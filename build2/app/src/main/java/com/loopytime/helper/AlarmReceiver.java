package com.loopytime.helper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    public static String CUSTOM_INTENT = "com.hitasoft.intent.action.ALARM";
    public static String TAG = "AlarmReceiver";
    public static PendingIntent pendingIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        /* enqueue the job */
        pendingIntent = getPendingIntent(context);
        Log.e(TAG, "onReceive: " + System.currentTimeMillis());
        MyJobIntentService.enqueueWork(context, intent);
    }

    public static void cancelAlarm(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        /* cancel any pending alarm */
        alarm.cancel(pendingIntent);
    }

    public static void setAlarm(Boolean force, Context context) {
        Log.e(TAG, "setAlarm: " + System.currentTimeMillis());
        cancelAlarm(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // EVERY N MINUTES
        long delay = (2000);
        long when = System.currentTimeMillis();
        if (!force) {
            when += delay;
        }

        // Hopefully your alarm will have a lower frequency than this!
        /*if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    System.currentTimeMillis(),
                    2000, pendingIntent);
        }*/

        /* fire the broadcast */
        int SDK_INT = Build.VERSION.SDK_INT;
        if (SDK_INT < Build.VERSION_CODES.KITKAT)
            alarmManager.set(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        else if (SDK_INT < Build.VERSION_CODES.M)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        }
    }

    public static PendingIntent getPendingIntent(Context context) {
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.setAction(CUSTOM_INTENT);

        return PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
