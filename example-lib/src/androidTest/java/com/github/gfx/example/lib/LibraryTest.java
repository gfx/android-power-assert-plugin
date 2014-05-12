package com.github.gfx.example.lib;

import android.test.AndroidTestCase;

import com.github.gfx.example.lib.p.LibBar;

public class LibraryTest extends AndroidTestCase {
    public void testAssertInLibs() throws Exception {
        try {
            LibFoo.f(false);
            fail("not reached");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("expr"));
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

    public void testAssertInAntherPackage() throws Exception {
        try {
            LibBar.f(false);
            fail("not reached");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("expr"));
        }
    }

}
