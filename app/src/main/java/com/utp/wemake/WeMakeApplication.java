package com.utp.wemake;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.utp.wemake.repository.ImageRepository;

public class WeMakeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ImageRepository.initialize(getApplicationContext());
        FirebaseApp.initializeApp(this);
    }
}
