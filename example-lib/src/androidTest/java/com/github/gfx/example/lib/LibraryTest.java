package com.github.gfx.example.lib;

import android.test.AndroidTestCase;

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
}
