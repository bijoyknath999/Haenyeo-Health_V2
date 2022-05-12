package com.rockwonitglobal.jejudiver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.BoxInsetLayout;

import android.content.Intent;
import android.os.Bundle;


public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000;
    private BoxInsetLayout rootLayout;
    private int diverid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        rootLayout = findViewById(R.id.splashScreen);

        diverid = Tools.getID("diverid",SplashActivity.this);
    }

    private void initFunctionality() {
        rootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, UniversalActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initFunctionality();
    }
}