package com.loopytime.im;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.ForegroundService;
import com.loopytime.helper.LocaleManager;
import com.loopytime.helper.MaterialColor;
import com.loopytime.helper.MaterialColors;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.SocketConnection;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;


public abstract class BaseActivity extends AppCompatActivity implements NetworkReceiver.ConnectivityReceiverListener {

    private static final String TAG = "BaseActivity";
    NetworkReceiver networkReceiver;
    private boolean IS_NETWORK_CHANGED = false;
    DatabaseHandler dbhelper;
    SocketConnection socketConnection;
    MaterialColor color;

    void tintShape(Context mContext,int color,Drawable background){

        if (background instanceof ShapeDrawable) {
            ((ShapeDrawable)background).getPaint().setColor(color);
        } else if (background instanceof GradientDrawable) {
            ((GradientDrawable)background).setColor(color);
        } else if (background instanceof ColorDrawable) {
            ((ColorDrawable)background).setColor(color);
        }
    }
    public void setActionBarColor() {

        color = MaterialColors.CONVERSATION_PALETTE.get(getSharedPreferences("wall", Context.MODE_PRIVATE).getInt(MaterialColors.THEME, 0));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color.toStatusBarColor(this));
        }
        //  getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //   getSupportActionBar().setDisplayShowHomeEnabled(true);

        if(findViewById(R.id.toolbar)!=null)
            findViewById(R.id.toolbar).setBackgroundColor(color.toActionBarColor(this));
        if(findViewById(R.id.actionbar)!=null)
            findViewById(R.id.actionbar).setBackgroundColor(color.toActionBarColor(this));
        tintShape(this,color.toActionBarColor(this), ContextCompat.getDrawable(this,R.drawable.fablay_bg));
        tintShape(this,color.toActionBarColor(this), ContextCompat.getDrawable(this,R.drawable.gradient_round));
        tintShape(this,color.toActionBarColor(this), ContextCompat.getDrawable(this,R.drawable.gradient_round_withoutborder));
        /*if(findViewById(R.id.fab)!=null)
            findViewById(R.id.fab).setBackgroundColor(color.toActionBarColor(this));
        if(findViewById(R.id.fablocation)!=null)
            findViewById(R.id.fablocation).setBackgroundColor(color.toActionBarColor(this));
        if(findViewById(R.id.fabSearch)!=null)
            findViewById(R.id.fabSearch).setBackgroundColor(color.toActionBarColor(this));
        if(findViewById(R.id.send)!=null)
            findViewById(R.id.send).setBackgroundColor(color.toActionBarColor(this));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color.toStatusBarColor(this));
        }*/
    }

    private Thread.UncaughtExceptionHandler handleAppCrash =
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable e) {
                    final Writer result = new StringWriter();
                    final PrintWriter printWriter = new PrintWriter(result);
                    e.printStackTrace(printWriter);
                    printWriter.close();
                    StackTraceElement[] arr = e.getStackTrace();
                    StringBuilder report = new StringBuilder(e.toString() + "\n\n");
                    report.append("--------- Stack trace ---------\n\n");
                    for (int i = 0; i < arr.length; i++) {
                        report.append("    ").append(arr[i].toString()).append("\n");
                    }
                    report.append("-------------------------------\n\n");

                    report.append("--------- Cause ---------\n\n");
                    Throwable cause = e.getCause();
                    if (cause != null) {
                        report.append(cause.toString()).append("\n\n");
                        arr = cause.getStackTrace();
                        for (StackTraceElement stackTraceElement : arr) {
                            report.append("    ").append(stackTraceElement.toString()).append("\n");
                        }
                    }
                    report.append("-------------------------------\n\n");
                    sendEmail(report.toString());
                }
            };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(base));
        Log.d(TAG, "attachBaseContext");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }


        // register connection status listener
        ApplicationClass.getInstance().setConnectivityListener(this);
        Thread.setDefaultUncaughtExceptionHandler(handleAppCrash);


        dbhelper = DatabaseHandler.getInstance(this);
        socketConnection = SocketConnection.getInstance(this);

//        Timer timer = new Timer();
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                // your code here...
//                if (socketConnection != null)
//                    socketConnection.runTimerTask("ping");
//            }
//        };
//        timer.schedule(timerTask, 0L, 10000);

        networkReceiver = new NetworkReceiver();
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            int uiMode = overrideConfiguration.uiMode;
            overrideConfiguration.setTo(getBaseContext().getResources().getConfiguration());
            overrideConfiguration.uiMode = uiMode;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        Log.v("onNetwork", "base=" + isConnected);
        if (isConnected && !ForegroundService.IS_SERVICE_RUNNING && IS_NETWORK_CHANGED) {
            IS_NETWORK_CHANGED = false;
            Log.v("onNetwork", "service start");
            socketConnection = SocketConnection.getInstance(this);
            Intent service = new Intent(this, ForegroundService.class);
            service.setAction("start");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
        } else {
            IS_NETWORK_CHANGED = true;
        }

        onNetworkChange(isConnected);
    }

    public abstract void onNetworkChange(boolean isConnected);

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setActionBarColor();
            }
        },400);

        dbhelper = DatabaseHandler.getInstance(this);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void makeToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void showLog(String TAG, String message, String value) {
        Log.d(TAG, message + " :" + value);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void sendEmail(String crash) {
        try {

            String reportContetnt = "\n\n" + "DEVICE OS VERSION CODE: " + Build.VERSION.SDK_INT + "\n" +
                    "DEVICE VERSION CODE NAME: " + Build.VERSION.CODENAME + "\n" +
                    "DEVICE NAME: " + getDeviceName() + "\n" +
                    "VERSION CODE: " + BuildConfig.VERSION_CODE + "\n" +
                    "VERSION NAME: " + BuildConfig.VERSION_NAME + "\n" +
                    "PACKAGE NAME: " + BuildConfig.APPLICATION_ID + "\n" +
                    "BUILD TYPE: " + BuildConfig.BUILD_TYPE + "\n\n\n" +
                    crash;

            final Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:")); // only email apps should handle this
            emailIntent.putExtra(Intent.EXTRA_EMAIL,
                    new String[]{"ashish.nautiyal102@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Report");
            emailIntent.putExtra(Intent.EXTRA_TEXT, reportContetnt);
            try {
                //start email intent
                startActivity(Intent.createChooser(emailIntent, "Email"));
            } catch (Exception e) {
                //if any thing goes wrong for example no email client application or any exception
                //get and show exception message
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e(TAG, "sendEmail: " + e.getMessage());
        }
    }

    /**
     * Returns the consumer friendly device name
     */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        return manufacturer + " " + model;
    }
}
