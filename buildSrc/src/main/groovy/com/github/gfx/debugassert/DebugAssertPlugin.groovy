package com.github.gfx.debugassert

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
import javassist.expr.ExprEditor
import javassist.expr.FieldAccess
import javassist.expr.MethodCall
import javassist.expr.NewExpr
import org.apache.commons.lang3.StringEscapeUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

// see http://www.gradle.org/docs/current/userguide/custom_plugins.html

public class DebugAssertPlugin implements Plugin<Project> {
    private static final String kPowerAssertMessage = '$powerAssertMessage'

    public static List<String> DEPENDENCIES = ['org.apache.commons:commons-lang3:+']

    private Project project;

    private ClassPool classPool
    private CtClass stringBufferClass

    @Override
    void apply(Project project) {
        this.project = project;

        checkAndroidPlugin()

        classPool = ClassPool.default
        stringBufferClass = classPool.getCtClass("java.lang.StringBuilder")

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
                // TODO: lazy initialization?
                method.addLocalVariable(kPowerAssertMessage, stringBufferClass)
                method.insertBefore("$kPowerAssertMessage = new StringBuffer();")

                method.instrument(new EditAssertStatement(method))
            }
        }
        return modified;
    }

    private class EditAssertStatement extends ExprEditor {
        final CtMethod method;

        boolean inAssertStatement = false;

        EditAssertStatement(CtMethod method) {
            this.method = method;
        }

        @Override
        void edit(FieldAccess f) throws CannotCompileException {
            if (inAssertStatement) {
                buildFieldInformation(f)
            }

            if (f.static && f.fieldName == '$assertionsDisabled') {
                info "assert statement found at ${f.fileName}:${f.lineNumber}"
                inAssertStatement = true
            }
        }

        @Override
        void edit(NewExpr e) throws CannotCompileException {
            if (inAssertStatement && e.className == "java.lang.AssertionError") {
                injectVariableInformation(e)
                inAssertStatement = false;
            }
        }

        void buildFieldInformation(FieldAccess expr) {

        }

        CharSequence buildVariableTable(int lineNumber) {
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
                if (lines.lineNumber(vars.index(i)) <= lineNumber && name != kPowerAssertMessage) {
                    CtClass varType = Descriptor.toCtClass(vars.descriptor(i), classPool)

                    info "${name} ${varType.name}"

                    def exprToDump = name
                    if (!varType.isPrimitive()) {
                        exprToDump = "ToStringBuilder.reflectionToString((Object)${exprToDump})"
                    }

                    s.append("${kPowerAssertMessage}.append(${makeLiteral("${name}=")});\n")
                    s.append("${kPowerAssertMessage}.append(${exprToDump});\n");
                }
            }

            return s
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
            CharSequence localVars = buildVariableTable(expr.lineNumber)

            def messagePrefix = new StringBuilder()
            if (expr.constructor.parameterTypes.length > 0) {
                messagePrefix.append('$1+')
            }
            messagePrefix.append('"\\n"')

            def src = String.format('''
{
%1$s.insert(0, %2$s);
%3$s
$_ = $proceed((Object)%1$s);
}
''', kPowerAssertMessage, messagePrefix, localVars);
            info src
            expr.replace(src);
        }
    }

    private void info(message) {
        println "[DebugAssert] $message"
        project.logger.info "[DebugAssert] $message"
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
}
