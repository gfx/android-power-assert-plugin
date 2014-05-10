package com.github.gfx.powerassert.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

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
        assert findViewById(android.R.id.list).getVisibility() == View.VISIBLE;
    }

    @OnClick(R.id.button3)
    void onButton3Click() {
        assert new Function<LayoutInflater, Boolean>() {
            @Override
            public Boolean call(LayoutInflater arg) {
                return arg.getContext() != null;
            }
        }.call(getLayoutInflater());
    }

    interface Function<ArgT, ResultT> {
        ResultT call(ArgT arg);
    }
}
