package com.github.gfx.example.lib;

import android.test.AndroidTestCase;

import com.github.gfx.example.lib.p.LibBar;

@SuppressWarnings("Assert")
public class LibraryTest extends AndroidTestCase {
    public void testAssertInLibs() throws Exception {
        try {
            LibFoo.f(false);
            fail("not reached");
        } catch (AssertionError e) {
            assert e.getMessage().contains("expr");
        }
    }

    public void testAssertInTests() throws Exception {
        try {
            assert false : "foo";
            fail("not reached");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("foo"));
        }
    }

    public void testAssertInAnotherPackage() throws Exception {
        try {
            LibBar.f(false);
            fail("not reached");
        } catch (AssertionError e) {
            assert e.getMessage().contains("expr");
        }
    }

    public void testAssertInAnotherClassInAnotherPackage() throws Exception {
        try {
            assert new LibBar().g(false);
            fail("not reached");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("LibBar"));
        }
    }

    public void testRefToObjectInLibrary() throws Exception {
        try {
            assert LibFoo.g("bar");
        }
        catch (AssertionError e) {
            assert e.getMessage().contains("LibFoo");
        }
    }
}
