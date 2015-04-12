package com.github.gfx.android.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);
    }

    @OnClick(R.id.button1)
    void onButton1Click() {
        Foo.f(false);
    }

    @OnClick(R.id.button2)
    void onButton2Click() {
        assert findViewById(R.id.never).isFocused();
    }

    @OnClick(R.id.button3)
    void onButton3Click() {
        assert new Function<LayoutInflater, Boolean>() {
            @Override
            public Boolean call(LayoutInflater arg) {
                return arg.getContext() == null;
            }
        }.call(getLayoutInflater()) : "complex assertions";
    }

    interface Function<ArgT, ResultT> {
        ResultT call(ArgT arg);
    }
}
