package com.loopytime.im;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class SplashActivity extends BaseActivity {
    private static final String TAG = SplashActivity.class.getSimpleName();
    SharedPreferences pref;
    private static int SPLASH_TIME_OUT = 2000;
    ApiInterface apiInterface;
    boolean updateAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        pref = getApplicationContext().getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);

        checkUpdates();

        if (pref.getBoolean("isLogged", false)) {
            Log.v(TAG, "isLogged");
            Log.v(TAG, "" + pref.getString("userId", null));
            Log.v(TAG, "" + pref.getString("userName", null));
            Log.v(TAG, "" + pref.getString("phoneNumber", null));
            Log.v(TAG, "" + pref.getString("userImage", null));
            GetSet.setLogged(true);
            GetSet.setUserId(pref.getString("userId", null));
            GetSet.setUserName(pref.getString("userName", null));
            GetSet.setphonenumber(pref.getString("phoneNumber", null));
            GetSet.setcountrycode(pref.getString("countryCode", null));
            GetSet.setImageUrl(pref.getString("userImage", null));
            GetSet.setToken(pref.getString("token", null));
            GetSet.setAbout(pref.getString("about", null));
        }

        if (ContextCompat.checkSelfPermission(this, READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{READ_CONTACTS, WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            openActivity();
        }
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void checkUpdates() {
        Call<HashMap<String, String>> call3 = apiInterface.checkForUpdates();
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> data = response.body();
                Log.v(TAG, "checkUpdatesResponse: " + data);
                PackageInfo packageInfo = null;
                try {
                    packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                String versionName = packageInfo.versionName;
                int versionCode = packageInfo.versionCode;
                 Log.v(TAG, "checkUpdatesResponse se: " + data+versionCode);

                if (data.get(Constants.TAG_STATUS).equals("true") && !data.get("android_version").equals(String.valueOf(versionCode))) {
                    updateAvailable = true;
                    updateConfirmDialog(data);
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                call.cancel();
            }
        });
    }

    private void updateConfirmDialog(HashMap<String, String> data) {
        final Dialog dialog = new Dialog(SplashActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.default_popup);
        dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 85 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        TextView title = dialog.findViewById(R.id.title);
        TextView yes = dialog.findViewById(R.id.yes);
        TextView no = dialog.findViewById(R.id.no);
        yes.setText(getString(R.string.yes));
        no.setText(getString(R.string.no));
        title.setText(R.string.update_des);

        if (data.get("android_update").equals("1")) {
            no.setVisibility(View.GONE);
        } else {
            no.setVisibility(View.VISIBLE);
        }

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                openActNow();
            }
        });

        if (!SplashActivity.this.isDestroyed() && !dialog.isShowing())
            dialog.show();
    }

    private void openActivity() {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                if (!updateAvailable) {
                    openActNow();
                }
            }
        }, SPLASH_TIME_OUT);
    }

    private void openActNow() {
        if (GetSet.isLogged()) {
            Intent i = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(i);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null)
                notificationManager.cancelAll();
        } else {
            Intent j = new Intent(SplashActivity.this, WelcomeActivity.class);
            startActivity(j);
        }
        //startActivity(new Intent(SplashActivity.this, com.hitasoft.loopytime.hiddy.cameraX.cameraxbasic.MainActivity.class));

        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v("requestCode", "requestCode=" + requestCode);
        switch (requestCode) {
            case 100:
                boolean isContactEnabled = false;

                for (String permission : permissions) {
                    if (permission.equals(READ_CONTACTS)) {
                        if (ActivityCompat.checkSelfPermission(SplashActivity.this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            isContactEnabled = true;
                        }
                    }
                }

                if (!isContactEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
                            requestPermission(new String[]{READ_CONTACTS}, 100);
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.contact_permission_error, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + getApplication().getPackageName()));
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                } else {
                    openActivity();
                }

                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Window w = getWindow(); // in Activity's onCreate() for instance
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    private boolean checkPermissions() {
        int permissionContacts = ContextCompat.checkSelfPermission(SplashActivity.this,
                READ_CONTACTS);

        return permissionContacts == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(SplashActivity.this, permissions, requestCode);
    }
}
