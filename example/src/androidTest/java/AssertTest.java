import android.test.AndroidTestCase;

import com.github.gfx.debugassert.example.Foo;

public class AssertTest extends AndroidTestCase {
    public void testAssert() throws Exception {
        try {
            int num = 10;
            assert num == 20;
            fail("must not be reached");
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }

    public void testAssertEnabledInAppClasses() throws Exception {
        try {
            Foo.f(false);
            fail("must not be reached");
        } catch (AssertionError e) {
            assertNotNull(e);
        }
    }
}
