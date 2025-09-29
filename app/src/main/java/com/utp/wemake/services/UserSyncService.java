package com.utp.wemake.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utp.wemake.repository.MemberRepository;

public class UserSyncService extends Service {
    private static final String TAG = "UserSyncService";
    private MemberRepository memberRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        memberRepository = new MemberRepository();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        syncCurrentUser();
        return START_STICKY;
    }

    private void syncCurrentUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            memberRepository.createOrUpdateUser(currentUser, new MemberRepository.MemberCallback() {
                @Override
                public void onSuccess(com.utp.wemake.models.Member member) {
                    Log.d(TAG, "Usuario sincronizado: " + member.getName());
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error sincronizando usuario: " + e.getMessage());
                }
            });
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
