import android.test.AndroidTestCase;

import com.github.gfx.debugassert.example.Foo;

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
            assertNotNull(e.getMessage().contains("false"));
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
    }

    class HasKV {
        KV<String, String> entry;

        HasKV(KV<String, String> entry) {
            this.entry = entry;
        }
    }

    public void testFieldAccess() throws Exception {
        try {
            HasKV second = new HasKV(new KV<>("foo", "bar"));

            assert second.entry.key.equals("foo");
            assert second.entry.value.equals("zzz");
        } catch (AssertionError e) {
            assert e.getMessage().contains("key=foo");
            assert e.getMessage().contains("value=bar");
        }
    }

    public void testNPE() throws Exception {
        try {
            HasKV second = new HasKV(null);

            assert second.entry.key.equals("foo");
            fail("not reached");
        } catch (RuntimeException e) {
            assert e.getMessage().contains("AssertTest$KV.key");
            assert e.getMessage().contains("null");
        }
    }
}
