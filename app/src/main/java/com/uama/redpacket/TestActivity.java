package com.uama.redpacket;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;


public class TestActivity extends Activity {
    Handler handler=new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.check_open).setVisibility(View.GONE);
            }
        },3000);
        finish();
        new Handler().removeCallbacksAndMessages(null);
    }
}
