package com.github.gfx.powerassert.example;

import android.test.AndroidTestCase;

import com.github.gfx.powerassert.example.p.Baz;

public class AssertTest extends AndroidTestCase {
    public void testAssert() throws Exception {
        try {
            int num = 42;
            assert num == 20;
            fail("not reached");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("num"));
            assertTrue(e.getMessage().contains("42"));
        }
    }

    public void testAssertEnabledInAppClasses() throws Exception {
        try {
            Foo.f(false);
            fail("not reached");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("false"));
        }
    }

    public void testAssertEnabledInAppClasses_causedIllegalAccessError() throws Exception {
        try {
            Foo.g(false);
            fail("not reached");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("false"));
        }
    }

    public void testAssertWithMessage() throws Exception {
        try {
            assert false : "assert message here";
            fail("not reached");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("assert message here"));
        }
    }

    public void testLocalVariables() throws Exception {
        {
            String foo = "bar";
            assertNotNull(foo);
        }

        try {
            assert false;
            fail("not reached");
        } catch (AssertionError e) {
            assertFalse(e.getMessage().contains("foo"));
            assertFalse(e.getMessage().contains("bar"));
        }
    }

    class KV<K, V> {
        K key;
        V value;

        KV(K key, V value) {
            this.key = key;
            this.value = value;
        }


        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }

    class HasKV {
        KV<String, String> entry;

        HasKV(KV<String, String> entry) {
            this.entry = entry;
        }
    }

    public void testFieldAccess() throws Exception {
        try {
            HasKV second = new HasKV(new KV<>("foo\n", "bar\n"));

            assert second.entry.key.equals("foo\n");
            assert second.entry.value.equals("zzz");
            fail("not reached");
        } catch (AssertionError e) {
            assert e.getMessage().contains("value=\"bar\\n\"");
        }
    }

    public void testNPE() throws Exception {
        try {
            HasKV second = new HasKV(null);

            assert second.entry.key.equals("foo");
            fail("not reached");
        } catch (RuntimeException e) {
            assert e.getMessage().contains("AssertTest$HasKV.entry=<null>");
        }
    }

    public void testMethodCall() throws Exception {
        try {
            HasKV second = new HasKV(new KV<>("foo\n", "bar\n"));

            assert second.entry.getKey().equals("foo\n");
            assert second.entry.getValue().equals("zzz");
            fail("not reached");
        } catch (AssertionError e) {
            assert e.getMessage().contains("getValue()=\"bar\\n\"");
        }
    }

    public void testTargetSource() throws Exception {
        try {
            assert false : "Hello, world!";
            fail("not reached");
        } catch (AssertionError e) {
            assert e.getMessage().contains("assert false : \"Hello, world!\";");
        }
    }

    public void testIntArrayVariables() throws Exception {
        try {
            int[] array = {10, 20, 30};
            assert array.length == 0;
            fail("not reached");
        } catch (AssertionError e) {
            assert e.getMessage().contains("10");
            assert e.getMessage().contains("20");
            assert e.getMessage().contains("30");
        }
    }

    public void testStringArrayVariables() throws Exception {
        try {
            String[] array = {"10", "20", "30"};
            assert array.length == 0;
            fail("not reached");
        } catch (AssertionError e) {
            assert e.getMessage().contains("10");
            assert e.getMessage().contains("20");
            assert e.getMessage().contains("30");
        }
    }

    public void testAssertInAnotherPackage() throws Exception {
        try {
            Baz.f(false);
            fail("not reached");
        } catch (AssertionError e) {
            assert e.getMessage().contains("expr");
        }
    }

    public void testWithAnonClass() throws Exception {
        final int value = 42 + this.hashCode();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                assert value == 0;
            }
        };

        try {
            runnable.run();
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("value"));
        }
    }
}
