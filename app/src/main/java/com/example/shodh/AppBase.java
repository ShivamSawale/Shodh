package com.example.shodh;


import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

public class AppBase extends Application {

    private static AppBase instance;

    @Override
    public void onCreate() {
        super.onCreate();
        initApplication();

    }

    private void initApplication() {
        instance = this;
    }

    public static synchronized AppBase getInstance() {
        return instance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
