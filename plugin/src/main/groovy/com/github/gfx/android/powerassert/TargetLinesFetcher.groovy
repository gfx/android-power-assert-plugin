package com.github.gfx.android.powerassert

import javassist.CtClass
import org.gradle.api.file.FileTree

public class TargetLinesFetcher {
    private final FileTree sourceTree

    private final Map<CtClass, String[]> cache = new WeakHashMap<>()

    public TargetLinesFetcher(FileTree sourceTree) {
        this.sourceTree = sourceTree
    }

    public String getLines(CtClass c, String baseName, int targetLineNumber) {
        int target = targetLineNumber - 1 // line number is 1-origin

        String[] lines = findLines(c, baseName)
        if (lines == null) {
            return "";
        }

        def s = new StringBuilder()
        if ((target - 1) >= 0) {
            s.append(String.format('%4d: %s\n', target - 1, lines[target - 1]))
        }
        s.append(String.format('%4d> %s\n', target, lines[target]))
        if ((target + 1) < lines.length) {
            s.append(String.format('%4d: %s\n', target + 1, lines[target + 1]))
        }
        return s.toString()
    }

    String[] findLines(CtClass c, String baseName) {
        String[] lines = cache.get(c)
        if (lines == null) {
            String sourceFile = c.name.replace(".", "/")
            sourceFile = sourceFile.substring(0, sourceFile.lastIndexOf("/") + 1)
            sourceFile += baseName

            def matched = sourceTree.filter { File f ->
                f.absolutePath.endsWith(sourceFile)
            }
            if (matched.empty) {
                return null;
            }
            File file = matched.singleFile
            lines = file.text.split('\n')
            cache.put(c, lines)
        }
        return lines;
    }
}
