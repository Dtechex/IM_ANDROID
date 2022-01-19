package com.loopytime.helper;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import android.util.Log;

import com.loopytime.im.ApplicationClass;

public class MyJobIntentService extends JobIntentService {

    public static String TAG = "MyJobIntentService";
    /* Give the Job a Unique Id */
    private static final int JOB_ID = 1000;

    public static void enqueueWork(Context ctx, Intent intent) {
        enqueueWork(ctx, MyJobIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        /* your code here */
        /* reset the alarm */
        Log.e(TAG, "onHandleWork: " + System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            AlarmReceiver.setAlarm(true, getApplicationContext());
            ApplicationClass.setAlarm(this.getApplicationContext());
        }
//        stopSelf();
    }

}