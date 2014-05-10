package com.github.gfx.powerassert.example;

import android.content.Intent;
import android.test.ActivityUnitTestCase;

public class MainActivityTest extends ActivityUnitTestCase<MainActivity> {
    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Intent intent = new Intent(getInstrumentation().getContext(), MainActivity.class);
        startActivity(intent, null, null);
    }

    public void testButton1Click() throws Exception {
        try {
            getActivity().onButton1Click();
            fail("not reached");
        } catch (AssertionError e) {
            assert e.getMessage().contains("assert");
        }
    }

    public void testButton2Click() throws Exception {
        try {
            getActivity().onButton2Click();
            assert false : "not reached";
            fail("not reached");
        } catch (NullPointerException e) {
            assert e.getMessage().contains("assert");
        }
    }

    public void testButton3Click() throws Exception {
        try {
            getActivity().onButton3Click();
            fail("not reached");
        } catch (AssertionError e) {
            assert e.getMessage().contains("assert");
        }
    }
}
