package com.github.gfx.powerassert.example;

public class Foo {
    private boolean bar = false;

    private static Foo instance = new Foo(false);

    public Foo(boolean value) {
        this.bar = value;
    }

    public static void f(boolean expr) {
        assert expr : "expects foo.value to be true";
    }

    public static void g(boolean expr) {
        assert instance.bar;
    }
}