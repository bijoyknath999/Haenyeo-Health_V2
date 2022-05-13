package com.rockwonitglobal.jejudiver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.BoxInsetLayout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;


public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000;
    private BoxInsetLayout rootLayout;
    private String diverid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        rootLayout = findViewById(R.id.splashScreen);

        diverid = Tools.getID("diverid",SplashActivity.this);

        System.out.println(""+diverid);
    }

    private void initFunctionality() {
        rootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!diverid.equals("0"))
                {
                    Intent intent = new Intent(SplashActivity.this, UniversalActivity.class);
                    startActivity(intent);
                    finish();
                }
                else
                {
                    Intent intent = new Intent(SplashActivity.this, RegisterActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }, SPLASH_DURATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initFunctionality();
    }
}