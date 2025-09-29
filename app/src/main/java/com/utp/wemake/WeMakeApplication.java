package com.utp.wemake;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class WeMakeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
