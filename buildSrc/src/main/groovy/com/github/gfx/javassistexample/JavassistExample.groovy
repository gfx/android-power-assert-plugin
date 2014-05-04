package com.github.gfx.javassistexamp
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod

public class JavassistExample {
    public static void process(String buildDir) {
        ClassPool classes = ClassPool.getDefault()

        classes.appendClassPath("/usr/local/opt/android-sdk/platforms/android-19//android.jar")
        classes.appendClassPath(buildDir)

        CtClass c = classes.getCtClass("com.github.gfx.javassistexample.app.MainActivity")

        CtMethod m = c.getDeclaredMethod("onResume")
        m.insertAfter("android.util.Log.d(\"XXX\", \"hoge\");")

        c.writeFile(buildDir)

        growl("ClassDumper", "${c.frozen}")
    }

    static void growl(String title, String message) {
        def proc = ["osascript", "-e", "display notification \"${message}\" with title \"${title}\""].execute()
        if (proc.waitFor() != 0) {
            println "[WARNING] ${proc.err.text.trim()}"
        }
    }
}
