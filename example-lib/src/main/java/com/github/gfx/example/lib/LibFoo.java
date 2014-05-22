package com.github.gfx.example.lib;

public class LibFoo {
    public static void f(boolean expr) {
        assert expr;
    }

    public static boolean g(String s) {
        assert s != null;
        return s.contains("foo");
    }
}
