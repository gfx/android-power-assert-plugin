package com.github.gfx.debugassert

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.ApplicationVariant
import groovy.io.FileType
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtClass
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.StopExecutionException

// see http://www.gradle.org/docs/current/userguide/custom_plugins.html

public class DebugAssertPlugin implements Plugin<Project> {
    private Project project;

    @Override
    void apply(Project project) {
        this.project = project;

        checkAndroidPlugin()

        AppExtension android = project.android;
        android.applicationVariants.all { ApplicationVariant variant ->
            if (variant.buildType.name != "release") {
                variant.javaCompile.doLast {
                    enableDebugAssert(variant.javaCompile.destinationDir)
                }
                variant.testVariant.javaCompile.doLast {
                    enableDebugAssert(variant.testVariant.javaCompile.destinationDir)
                }
            }
        }
    }

    private String ucfirst(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1)
    }

    private Task createTask(ApkVariant variant) {
        Task task = project.tasks.create("enable${ucfirst(variant.name)}Assert")
        task.description = "Enables assert statements (equivalent to setting debug.assert true in Dalvik VM)"
        task.doLast {
            enableDebugAssert(variant.javaCompile.destinationDir)
        }

        return task
    }

    private void checkAndroidPlugin() {
        if (!project.plugins.hasPlugin(AppPlugin)) {
            throw new StopExecutionException('No android plugin detected')
        }
    }

    private void enableDebugAssert(File buildDir) {
        long t0 = System.currentTimeMillis()

        ClassPool classPool = ClassPool.getDefault()

        project.plugins.findPlugin("android").bootClasspath.each { androidJar ->
            classPool.appendClassPath(androidJar)
        }

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
        return modified;
    }

    private void info(String message) {
        println "[DebugAssert] $message"
        project.logger.info "[DebugAssert] $message"
    }

    private void growl(String title, String message) {
        info(message)
        project.logger.info("[$title] $message")
        def proc = ["osascript", "-e", "display notification \"${message}\" with title \"${title}\""].execute()
        if (proc.waitFor() != 0) {
            println "[WARNING] ${proc.err.text.trim()}"
        }
    }
}
