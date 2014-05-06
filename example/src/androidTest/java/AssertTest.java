import android.test.AndroidTestCase;

import com.github.gfx.debugassert.example.Foo;

public class AssertTest extends AndroidTestCase {
    public void testAssert() throws Exception {
        try {
            int num = 42;
            assert num == 20;
            fail("must not be reached");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("num"));
            assertTrue(e.getMessage().contains("42"));
        }
    }

    public void testAssertEnabledInAppClasses() throws Exception {
        try {
            Foo.f(false);
            fail("must not be reached");
        } catch (AssertionError e) {
            assertNotNull(e.getMessage().contains("false"));
        }
    }

    public void testAssertWithMessage() throws Exception {
        try {
            assert false : "assert message here";
            fail("must not be reached");
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
            fail("must not be reached");
        } catch (AssertionError e) {
            assertFalse(e.getMessage().contains("foo"));
            assertFalse(e.getMessage().contains("bar"));
        }
    }
}
