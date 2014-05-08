package com.github.gfx.powerassert

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.DefaultBuildType
import com.android.builder.model.BuildType
import groovy.io.FileType
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.bytecode.*
import javassist.expr.*
import org.apache.commons.lang3.StringEscapeUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

// see http://www.gradle.org/docs/current/userguide/custom_plugins.html

public class PowerAssertPlugin implements Plugin<Project> {
    private static final String TAG = 'PowerAssert'
    private static final boolean VERBOSE = System.getenv('CI') || System.getenv('POWERASSERT_VERBOSE')

    private static final String kPowerAssertMessage = '$powerAssertMessage'

    /**
     * Runtime libraries this plugin depends on
     */
    public static List<String> DEPENDENCIES = ['org.apache.commons:commons-lang3:+']

    private Project project;

    private ClassPool classPool
    private CtClass stringBuilderClass
    private CtClass runtimeExceptionClass

    @Override
    void apply(Project project) {
        this.project = project;

        checkAndroidPlugin()

        classPool = ClassPool.default
        stringBuilderClass = classPool.getCtClass("java.lang.StringBuilder")
        runtimeExceptionClass = classPool.getCtClass("java.lang.RuntimeException")

        project.dependencies.androidTestCompile(DEPENDENCIES)

        AppExtension android = project.android

        android.buildTypes.all { DefaultBuildType buildType ->
            if (isAssertionsEnabled(buildType)) {
                project.dependencies."${buildType.name}Compile"(DEPENDENCIES)
            }
        }

        android.applicationVariants.all { ApplicationVariant variant ->
            if (isAssertionsEnabled(variant.buildType)) {
                variant.javaCompile.doLast {
                    enableDebugAssert(variant)
                }
                variant.testVariant.javaCompile.doLast {
                    enableDebugAssert(variant.testVariant)
                }
            }
        }
    }

    private void trace(message) {
        if (VERBOSE) {
            println "[$TAG] $message"
        } else {
            project.logger.trace "[$TAG] $message"
        }
    }

    private void info(message) {
        if (VERBOSE) {
            println "[$TAG] $message"
        } else {
            project.logger.info "[$TAG] $message"
        }
    }

    @SuppressWarnings("unused")
    private void growl(String title, String message) {
        info(message)
        project.logger.info("[$title] $message")
        def proc = ["osascript", "-e", "display notification \"${message}\" with title \"${title}\""].execute()
        if (proc.waitFor() != 0) {
            println "[WARNING] ${proc.err.text.trim()}"
        }
    }

    private boolean isAssertionsEnabled(BuildType variant) {
        return variant.name != "release"
    }

    private void checkAndroidPlugin() {
        if (!project.plugins.hasPlugin(AppPlugin)) {
            throw new GradleException('No android plugin detected')
        }
    }

    private void enableDebugAssert(ApkVariant variant) {
        info "setup ${variant.name}"
        Collection<File> libraries = variant.apkLibraries
        File buildDir = variant.javaCompile.destinationDir

        long t0 = System.currentTimeMillis()

        project.plugins.findPlugin("android").bootClasspath.each { String androidJar ->
            classPool.appendClassPath(androidJar)
        }

        def hasCommonsLang3 = false
        libraries.each { File jar ->
            info "appendClassPath: ${jar.absolutePath}"
            classPool.appendClassPath(jar.absolutePath)

            if (jar.name.startsWith("commons-lang3")) {
                hasCommonsLang3 = true
            }
        }
        if (!hasCommonsLang3) {
            String depsDecl = """
dependencies {
    ${variant.name}Compile ${this.class.simpleName}.DEPENDENCIES
}
"""
            throw new GradleException("commons-lang3 is required. Specify in dependencies: $depsDecl")
        }

        classPool.importPackage("org.apache.commons.lang3.builder")
        classPool.importPackage("org.apache.commons.lang3")

        String absoluteBuildDir = buildDir.canonicalPath
        info "buildDir=$absoluteBuildDir"

        classPool.appendClassPath(absoluteBuildDir)

        def allCount = 0;
        def processedCount = 0;

        def classFiles = new ArrayList<File>()
        buildDir.eachFileRecurse(FileType.FILES) { File file ->
            if (file.name.endsWith(".class")) {
                classFiles.add(file)
            }
        }

        classFiles.collect { File classFile ->
            assert classFile.absolutePath.startsWith(absoluteBuildDir)

            def path = classFile.absolutePath.substring(absoluteBuildDir.length() + 1 /* for a path separator */)
            path.substring(0, path.lastIndexOf(".class")).replace("/", ".")
        }.each { className ->
            CtClass c = classPool.getCtClass(className)
            if (modifyStatements(c)) {
                info("Enables assertions in ${c.name}")
                processedCount++
                c.writeFile(absoluteBuildDir)
            }
            allCount++
        }

        info("Processed $processedCount/$allCount classes, elapsed ${System.currentTimeMillis() - t0}ms")
    }

    private boolean modifyStatements(CtClass c) {
        boolean modified = false;

        c.getClassInitializer()?.instrument(new ExprEditor() {
            @Override
            void edit(MethodCall m) throws CannotCompileException {
                if (m.className == "java.lang.Class" && m.methodName == "desiredAssertionStatus") {
                    m.replace('{ $_ = ($r)true; }')
                    modified = true
                }
            }
        })

        if (modified) {
            c.getDeclaredMethods().each { CtMethod method ->
                method.addLocalVariable(kPowerAssertMessage, stringBuilderClass)
                method.insertBefore("${kPowerAssertMessage} = new StringBuilder();")

                method.instrument(new EditAssertStatement(method))
            }
        }
        return modified;
    }

    private class EditAssertStatement extends ExprEditor {
        final CtMethod method;

        boolean inAssertStatement = false

        EditAssertStatement(CtMethod method) {
            this.method = method
        }

        @Override
        void edit(FieldAccess f) throws CannotCompileException {
            if (inAssertStatement) {
                def src = buildFieldInformation(f)
                trace src
                f.replace(src)
            }

            if (f.static && f.fieldName == '$assertionsDisabled') {
                info "assert statement found at ${f.fileName}:${f.lineNumber}"
                inAssertStatement = true

                def src = String.format('''{
$_ = $proceed();
if (%1$s == null) {
    %1$s = new StringBuilder();
} else {
    %1$s.setLength(0);
}
}''', kPowerAssertMessage)
                trace src
                f.replace(src)
            }
        }

        @Override
        void edit(MethodCall m) throws CannotCompileException {
            if (inAssertStatement) {
                def src = buildMethodResultInformation(m)
                if (src != null) {
                    trace src
                    m.replace(src)
                }
            }
        }

        @Override
        void edit(NewExpr e) throws CannotCompileException {
            if (inAssertStatement && e.className == "java.lang.AssertionError") {
                injectVariableInformation(e)
                inAssertStatement = false;
            }
        }

        String buildFieldInformation(FieldAccess expr) {
            return String.format(
                '''{
try {
  $_ = $proceed($$);
} catch (NullPointerException e) {
  Exception ex = new NullPointerException(%1$s.toString());
  ex.setStackTrace(e.getStackTrace());
  throw ex;
}
%1$s.append(%2$s);
%1$s.append(%3$s);
%1$s.append("\\n");
}''',
                kPowerAssertMessage,
                makeLiteral("${expr.className}.${expr.fieldName}="),
                inspectExpr('$_', Descriptor.toCtClass(expr.signature, classPool))
            )
        }

        String buildMethodResultInformation(MethodCall expr) {
            def returnType = Descriptor.getReturnType(expr.signature, classPool)
            if (returnType == CtClass.voidType) {
                return null
            }
            return String.format(
                '''{
try {
  $_ = $proceed($$);
} catch (NullPointerException e) {
  Exception ex = new NullPointerException(%1$s.toString());
  ex.setStackTrace(e.getStackTrace());
  throw ex;
}
%1$s.append(%2$s);
%1$s.append(%3$s);
%1$s.append("\\n");
}''',
                kPowerAssertMessage,
                makeLiteral("${expr.className}.${expr.methodName}()="),
                inspectExpr('$_', returnType)
            )
        }

        CharSequence buildVariableTable(Expr expr) {
            MethodInfo methodInfo = method.getMethodInfo()

            CodeAttribute attrs = methodInfo.getCodeAttribute()
            if (attrs == null) {
                return '""'
            }

            LocalVariableAttribute vars = (LocalVariableAttribute) attrs.getAttribute(LocalVariableAttribute.tag)
            LineNumberAttribute lines = (LineNumberAttribute) attrs.getAttribute(LineNumberAttribute.tag)

            if (vars == null || lines == null) {
                return '""'
            }

            def s = new StringBuilder();

            for (int i = 0; i < vars.tableLength(); i++) {
                def name = vars.variableName(i)
                def startIndex = vars.startPc(i)
                def endIndex = startIndex + vars.codeLength(i)
                def index = expr.indexOfBytecode()
                if ((startIndex <= index && index < endIndex) && name != kPowerAssertMessage) {
                    CtClass varType = Descriptor.toCtClass(vars.descriptor(i), classPool)

                    info "${lines.lineNumber(vars.index(i))}: ${varType.simpleName} ${name}"

                    def exprToDump = inspectExpr(name, varType)

                    // _s is a local StringBuilder for this `new AssertionError()` expression
                    s.append("_s.append(${makeLiteral("${name}=")});\n")
                    s.append("_s.append(${exprToDump});\n");
                    s.append("_s.append(${makeLiteral('\n')});\n");
                }
            }

            return s
        }

        String inspectExpr(String expr, CtClass type) {
            if (type.isPrimitive()) {
                return expr;
            } else {
                def makeStringLiteral = String.format('("\\"" + StringEscapeUtils.escapeJava((String)%s) + "\\"")', expr)
                def inspect = String.format('ToStringBuilder.reflectionToString((Object)%s, ToStringStyle.SHORT_PREFIX_STYLE)', expr)

                if (type.name == "java.lang.String") {
                    return makeStringLiteral
                } else if (type.name == "java.lang.Object") {
                    return "${expr}.getClass() == java.lang.String.class ? ${makeStringLiteral} : ${inspect}"
                } else {
                    return inspect
                }
            }
        }

        String makeLiteral(String s) {
            return '"' + StringEscapeUtils.escapeJava(s) + '"'
        }

        /**
         * Inject power-assert helpers
         *
         * @param expr the `new AssertionError()` expression
         */
        void injectVariableInformation(NewExpr expr) {
            CharSequence localVars = buildVariableTable(expr)

            def messagePrefix = new StringBuilder()
            if (expr.constructor.parameterTypes.length > 0) {
                messagePrefix.append('$1+')
            }
            messagePrefix.append('"\\n"')

            def src = String.format('''{
StringBuilder _s = new StringBuilder(%2$s);
_s.append("LOCAL VARIABLES:\\n");
%3$s
_s.append("\\nTEMPORARY VALUES:\\n");
_s.append((Object)%1$s);

$_ = $proceed((Object)_s);
}''', kPowerAssertMessage, messagePrefix, localVars);
            trace src
            expr.replace(src);
        }
    }
}