package com.github.gfx.android.powerassert

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import groovy.io.FileType
import groovy.transform.CompileStatic
import javassist.*
import javassist.bytecode.*
import javassist.expr.*
import org.apache.commons.lang3.StringEscapeUtils
import org.gradle.api.Project

@CompileStatic
class Empower {
    private static final String kPowerAssertMessage = '$powerAssertMessage'

    /**
     * Runtime libraries this plugin depends on
     */
    static List<String> DEPENDENCIES = ['org.apache.commons:commons-lang3:3.4']

    private final Project project;

    private final ClassPool classPool
    private final CtClass stringBuilderClass
    private final CtClass runtimeExceptionClass

    Empower(Project project) {
        this.project = project
        info("setup $project")

        classPool = new ClassPool(null);
        classPool.appendSystemPath();
        stringBuilderClass = classPool.getCtClass("java.lang.StringBuilder")
        runtimeExceptionClass = classPool.getCtClass("java.lang.RuntimeException")

        setupBootClasspath()
    }

    void trace(message) {
        if (PowerAssertPlugin.VERBOSE >= 2) {
            println "[$PowerAssertPlugin.TAG] $message"
        } else {
            project.logger.trace "[$PowerAssertPlugin.TAG] $message"
        }
    }

    void info(message) {
        if (PowerAssertPlugin.VERBOSE >= 1) {
            println "[$PowerAssertPlugin.TAG] $message"
        } else {
            project.logger.info "[$PowerAssertPlugin.TAG] $message"
        }
    }

    private void setupBootClasspath() {
        def extensions = project.extensions
        BaseExtension androidExtension = extensions.findByType(AppExtension) ?: extensions.findByType(LibraryExtension)
        addClassPaths(androidExtension.bootClasspath)
    }

    public void addClassPaths(Collection<File> libraries) {
        libraries.each { jar ->
            String canonicalPath = project.file(jar).absoluteFile.canonicalPath
            info "classPath: ${canonicalPath}"
            classPool.appendClassPath(canonicalPath)
        }
    }


    public void process(BaseVariant variant) {
        long t0 = System.currentTimeMillis()

        info "Processing variant=${variant.name}"

        // FIXME: it can't handle library source files
        def fetcher = new TargetLinesFetcher(variant.javaCompiler.source)

        classPool.importPackage("org.apache.commons.lang3.builder")
        classPool.importPackage("org.apache.commons.lang3")

        def buildDir = variant.javaCompiler.destinationDir
        def absoluteBuildDir = buildDir.canonicalPath
        info "buildDir=${absoluteBuildDir}"

        def allCount = 0;
        def processedCount = 0;

        getClassNamesInDirectory(buildDir).each { className ->
            final t1 = System.currentTimeMillis()
            CtClass c = classPool.getCtClass(className)
            if (modifyStatements(c, fetcher)) {
                processedCount++
                c.writeFile(absoluteBuildDir)
                info("Enables assertions in ${c.name} (elapsed ${System.currentTimeMillis() - t1}ms)")
            }
            allCount++
        }

        info("Processed $processedCount/$allCount classes, elapsed ${System.currentTimeMillis() - t0}ms")
    }

    private List<String> getClassNamesInDirectory(File dir) {
        def classNames = new ArrayList<String>()
        dir.eachFileRecurse(FileType.FILES) { File file ->
            if (file.name.endsWith(".class")) {
                def className = classFileToClassName(dir.canonicalPath, file)
                classNames.add(className)
            }
        }
        return classNames
    }

    private static String classFileToClassName(String buildDir, File classFile) {
        assert classFile.absolutePath.startsWith(buildDir)

        def path = classFile.absolutePath.substring(buildDir.length() + 1 /* for a path separator */)
        return path.substring(0, path.lastIndexOf(".class")).replace("/", ".")
    }

    private boolean modifyStatements(CtClass c, TargetLinesFetcher fetcher) {
        boolean modified = false;

        c.getClassInitializer()?.instrument(new ExprEditor() {
            @Override
            void edit(MethodCall m) throws CannotCompileException {
                if (m.className == "java.lang.Class" && m.methodName == "desiredAssertionStatus") {
                    m.replace('{ $_ = ($r)true; }') // equivalent to `setprop debug.asset true`
                    modified = true
                }
            }
        })

        if (modified && PowerAssertPlugin.empower) {
            c.getDeclaredMethods().each { CtMethod method ->
                if (!method.empty) {
                    method.addLocalVariable(kPowerAssertMessage, stringBuilderClass)
                    method.insertBefore("${kPowerAssertMessage} = null;")

                    method.instrument(new EditAssertStatement(method, fetcher))
                }
            }
        }
        return modified;
    }

    private class EditAssertStatement extends ExprEditor {
        final CtMethod method;
        final TargetLinesFetcher fetcher;

        boolean initialized = false;

        boolean inAssertStatement = false

        EditAssertStatement(CtMethod method, TargetLinesFetcher fetcher) {
            this.method = method
            this.fetcher = fetcher
        }

        @Override
        void edit(FieldAccess f) throws CannotCompileException {
            if (inAssertStatement) {
                def src = buildPowerAssertMessageInitialization(f)
                src += buildFieldInformation(f)
                trace src
                f.replace(src)
            }

            if (f.static && f.fieldName == '$assertionsDisabled') {
                info "assert statement found at ${f.fileName}:${f.lineNumber}"
                inAssertStatement = true
                initialized = false
            }
        }

        @Override
        void edit(MethodCall m) throws CannotCompileException {
            if (inAssertStatement) {
                def src = buildMethodResultInformation(m)
                if (src != null) {
                    src = buildPowerAssertMessageInitialization(m) + src
                    trace src
                    m.replace(src)
                }
            }
        }

        @Override
        void edit(NewExpr e) throws CannotCompileException {
            if (inAssertStatement && e.className == "java.lang.AssertionError") {
                def src = buildPowerAssertMessageInitialization(e)
                src += buildPowerAssertMessageInjection(e)
                trace src
                e.replace(src);
                inAssertStatement = false;
            }
        }

        // source code builders:

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

        String buildPowerAssertMessageInitialization(Expr expr) {
            if (initialized) {
                return ''
            }
            initialized = true

            def lines = fetcher.getLines(expr.enclosingClass, expr.fileName, expr.lineNumber)

            return String.format('''{
if (%1$s == null) {
    %1$s = new StringBuilder();
} else {
    %1$s.setLength(0);
}
%1$s.append(%2$s);
}''', kPowerAssertMessage, makeLiteral("\n\n" + lines + "\n\n"))
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

                    def exprToDump = inspectExpr(name, varType)

                    // _s is a local StringBuilder for this `new AssertionError()` expression
                    s.append("_s.append(${makeLiteral("${name}=")}); /* local variable */\n")
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

                if (type.isArray() && type.getComponentType().isPrimitive()) {
                    // FIXME: Javassist can't cast a primitive array to Object
                    return "ToStringBuilder.reflectionToString((Object)ArrayUtils.toObject(${expr}), ToStringStyle.SHORT_PREFIX_STYLE)"
                } else if (type.name == "java.lang.String") {
                    return makeStringLiteral
                } else if (type.name == "java.lang.Object") {
                    return "(${expr} != null && ${expr}.getClass() == java.lang.String.class) ? ${makeStringLiteral} : ${inspect}"
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
        String buildPowerAssertMessageInjection(NewExpr expr) {
            CharSequence localVars = buildVariableTable(expr)

            def messagePrefix = new StringBuilder()
            if (expr.constructor.parameterTypes.length > 0) {
                messagePrefix.append('$1+')
            }
            messagePrefix.append('"\\n"')

            def src = String.format('''{
StringBuilder _s = new StringBuilder(%2$s);
_s.append("\\n\\n");
%3$s
_s.append("\\n\\n");
_s.append((Object)%1$s);

$_ = $proceed((Object)_s);
}''', kPowerAssertMessage, messagePrefix, localVars);

            return src
        }
    }
}
