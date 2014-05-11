package com.github.gfx.powerassert

import javassist.CtClass
import org.gradle.api.file.FileTree

public class TargetLinesFetcher {
    private final FileTree sourceTree

    private final Map<CtClass, String[]> cache = new WeakHashMap<>()

    public TargetLinesFetcher(FileTree sourceTree) {
        this.sourceTree = sourceTree
    }

    public String getLines(CtClass c, int targetLineNumber) {
        int target = targetLineNumber - 1 // line number is 1-origin

        String[] lines = cache.get(c)
        if (lines == null) {
            String sourceFile = c.name.replace(".", "/") + ".java"

            def matched = sourceTree.filter { File f ->
                f.absolutePath.endsWith(sourceFile)
            }
            assert !matched.empty
            File file = matched.singleFile
            lines = file.text.split('\n')
            cache.put(c, lines)
        }

        def s = ""
        if ((target - 1) >= 0) {
            s += String.format('%4d: %s\n', target - 1, lines[target - 1])
        }
        s += String.format('%4d> %s\n', target, lines[target])
        if ((target + 1) < lines.length) {
            s += String.format('%4d: %s\n', target, lines[target + 1])
        }
        return s
    }
}