package com.github.gfx.debugassert.example;

public class Foo {
    private Bar bar;

    public Foo(boolean value) {
        this.bar = new Bar();
        this.bar.value = value;
    }

    public static void f(boolean expr) {
        Foo foo = new Foo(expr);
        assert foo.bar.value : "expects foo.value to be true";

        int bar = 42;
        assert bar == 42;
    }
}