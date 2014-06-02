package com.github.gfx.powerassert.example;

 public abstract class Bar {
    public boolean value;

    public Bar(boolean value) {
        this.value = value;
    }

    abstract void f();

     void g() {
         assert false;
     }
}