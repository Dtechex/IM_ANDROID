package com.loopytime.im;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

import com.facebook.stetho.Stetho;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.loopytime.external.FontsOverride;
import com.loopytime.helper.AlarmReceiver;
import com.loopytime.helper.CallNotificationService;
import com.loopytime.helper.CryptLib;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.ForegroundService;
import com.loopytime.helper.LocaleManager;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import static android.Manifest.permission.READ_CONTACTS;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.im.CallActivity.isInCall;

/**
 * Created by hitasoft on 24/1/18.
 */

//@AcraCore(buildConfigClass = BuildConfig.class, reportFormat = StringFormat.JSON)
//@AcraMailSender(mailTo = "crashlog@hitasoft.com", reportAsFile = false)
public class ApplicationClass extends Application implements LifecycleObserver {
    public  OkHttpClient httpClient;
    private static final String TAG = ApplicationClass.class.getSimpleName();
    public static SharedPreferences pref;
    public static SharedPreferences.Editor editor;
    public static boolean onShareExternal = false, onAppForegrounded = false;
    private static Snackbar snackbar = null;
    private static ApplicationClass mInstance;
    private static Toast toast = null;
    private static SocketConnection socketConnection;
    private static DatabaseHandler dbhelper;
    public Activity foregroundActivity;
    private Locale locale;

    public static synchronized ApplicationClass getInstance() {
        return mInstance;
    }

    // Showing network status in Snackbar
    public static void showSnack(final Context context, View view, boolean isConnected) {
        if (snackbar == null) {
            snackbar = Snackbar
                    .make(view, context.getString(R.string.network_failure), Snackbar.LENGTH_INDEFINITE)
                    .setAction("SETTINGS", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                            context.startActivity(intent);
                        }
                    });
            View sbView = snackbar.getView();
            TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
            textView.setTextColor(Color.WHITE);
        }

        if (!isConnected && !snackbar.isShown()) {
            snackbar.show();
        } else {
            snackbar.dismiss();
            snackbar = null;
        }
    }
    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }
    protected String userAgent;
    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
    }

    public static void showToast(final Context context, String text, int duration) {

        if (toast == null || toast.getView().getWindowVisibility() != View.VISIBLE) {
            toast = Toast.makeText(context, text, duration);
            toast.show();
        } else toast.cancel();
    }

    /**
     * To convert the given dp value to pixel
     **/
    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static void hideSoftKeyboard(Activity context, View view) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            //Find the currently focused view, so we can grab the correct window token from it.
            View view = activity.getCurrentFocus();
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = new View(activity);
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showKeyboard(Activity context, View view) {
        view.requestFocus();
        InputMethodManager keyboard = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.showSoftInput(view, 0);
    }

    public static float pxToDp(Context context, float px) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Returns {@code null} if this couldn't be determined.
     */
    @SuppressLint("PrivateApi")
    public static Boolean hasNavigationBar() {
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            IBinder serviceBinder = (IBinder) serviceManager.getMethod("getService", String.class).invoke(serviceManager, "window");
            Class<?> stub = Class.forName("android.view.IWindowManager$Stub");
            Object windowManagerService = stub.getMethod("asInterface", IBinder.class).invoke(stub, serviceBinder);
            Method hasNavigationBar = windowManagerService.getClass().getMethod("hasNavigationBar");
            return (boolean) hasNavigationBar.invoke(windowManagerService);
        } catch (ClassNotFoundException | ClassCastException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Log.w("YOUR_TAG_HERE", "Couldn't determine whether the device has a navigation bar", e);
            return false;
        }
    }

    public static Point getNavigationBarSize(Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize = getRealScreenSize(context);

        // navigation bar on the right
        if (appUsableSize.x < realScreenSize.x) {
            return new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
        }

        // navigation bar at the bottom
        if (appUsableSize.y < realScreenSize.y) {
            return new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
        }

        // navigation bar is not present
        return new Point();
    }

    public static Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                size.x = (Integer)     Display.class.getMethod("getRawWidth").invoke(display);
                size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (IllegalAccessException e) {} catch     (InvocationTargetException e) {} catch (NoSuchMethodException e) {}
        }

        return size;
    }

    public static void requestFocus(Activity activity, View view, boolean isEnabled) {
        if (view.requestFocus()) {
            if (isEnabled)
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            else
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    public static String getContactName(Context context, String phoneNumber, String countryCode,String name) {
        if (ContextCompat.checkSelfPermission(context, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = cr.query(uri, new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, ContactsContract.PhoneLookup._ID, null, null);
            if (cursor == null) {
                return null;
            }
//            else if(cursor.moveToFirst()) {
//                String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
//            }
            String contactName = name;
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            }

            if (!cursor.isClosed()) {
                cursor.close();
            }

            return contactName;
        } else {
            if(!TextUtils.isEmpty(name)){
                return name;
            }
            if (countryCode != null && !countryCode.equals("")) {
                return "+" + countryCode + "-" + phoneNumber;
            } else {
                return phoneNumber;
            }
        }
    }

    public static HashMap<String, String> getContactrNot(Context context, String phoneNumber) {
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_USER_NAME, phoneNumber);
        map.put("isAlready", "false");
        if (ContextCompat.checkSelfPermission(context, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor == null) {
                return map;
            }
            String contactName = phoneNumber;
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                map.put(Constants.TAG_USER_NAME, contactName);
                map.put("isAlready", "true");
            }

            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return map;
    }

    public static boolean hasContact(Context context, String phoneNumber) {
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_USER_NAME, phoneNumber);
        map.put("isAlready", "false");
        if (ContextCompat.checkSelfPermission(context, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor == null || cursor.getCount()==0) {
                return false;
            }


            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            return true;
        }
        return false;
    }

    /**
     * To convert timestamp to Time
     **/

    public static String getTime(long timeStamp) {
        try {
            DateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
            Date netDate = (new Date(timeStamp * 1000));
            return sdf.format(netDate);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "xx";
        }
    }

    public static String getChatTime(String pattern, long timeStamp) {
        try {
            DateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
            Date netDate = (new Date(timeStamp * 1000));
            return sdf.format(netDate);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "xx";
        }
    }

    public static String getFormattedDate(Context context, long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis * 1000L);

        Calendar now = Calendar.getInstance();

        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            return context.getString(R.string.today);
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            return context.getString(R.string.yesterday);
        } else {
            return getDate("d MMMM yyyy", smsTime.getTimeInMillis());
        }
    }

    private static String getDate(String format, long time) {
        java.text.DateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
        Date netDate = (new Date(time));
        return sdf.format(netDate);
    }


    public static Bitmap rotate(Bitmap bm) {
       /* int width = bm.getWidth();
        int height = bm.getHeight();

        int rotation = 0;

        if (width > height){
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                return bmOut;
            }
        }*/

        return bm;
    }

    /**
     * To convert timestamp to Time
     **/

    public static String getStatusTime(Context context, long timeStamp2, boolean isTodayVisible) {
        long timeStamp = timeStamp2 * 1000;
        String time = "";
        try {
            Date now = new Date();
            long seconds = TimeUnit.MILLISECONDS.toSeconds(now.getTime() - timeStamp);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - timeStamp);
            long hours = TimeUnit.MILLISECONDS.toHours(now.getTime() - timeStamp);
            long days = TimeUnit.MILLISECONDS.toDays(now.getTime() - timeStamp);


            Calendar smsTime = Calendar.getInstance();
            smsTime.setTimeInMillis(timeStamp * 1000L);

            Calendar Today = Calendar.getInstance();
            DateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
            Date netDate = (new Date(timeStamp * 1000));

            if (seconds < 60) {
                time = context.getString(R.string.just_now);
            } else if (minutes < 60) {
                time = context.getString(R.string.time_ago_minutes, minutes);
            } else if (Today.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
                time = context.getString(R.string.yesterday) + " " + sdf.format(netDate);
            } else if (isTodayVisible) {
                time = context.getString(R.string.today) + " " + sdf.format(netDate);
            } else {
                time = sdf.format(netDate);
            }
        } catch (Exception j) {
            j.printStackTrace();
        }

        return time;
    }

    public static void setAlarm(Context context) {
        // Intent to start the Broadcast Receiver
        Intent broadcastIntent = new Intent(context, AlarmReceiver.class);

        // The Pending Intent to pass in AlarmManager
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                broadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Setting up AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int SDK_INT = Build.VERSION.SDK_INT;
        long when = System.currentTimeMillis() + 5000;
        if (SDK_INT < Build.VERSION_CODES.KITKAT)
            alarmManager.set(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        else if (SDK_INT < Build.VERSION_CODES.M)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        }
//        alarmManager.set(AlarmManager.RTC_WAKEUP, when, pendingIntent);

    }

    public static void preventMultiClick(View view) {
        view.setEnabled(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setEnabled(true);
            }
        }, 1000);
    }

    public static boolean isRTL() {
        return Locale.getDefault().getLanguage().equals("ar");
    }

    public static String encryptMessage(String message) {
        String encryptedMsg = "";
        if (message != null && !message.equals("")) {
            try {
                CryptLib cryptLib = new CryptLib();
                encryptedMsg = cryptLib.encryptPlainTextWithRandomIV(message, Constants.MESSAGE_CRYPT_KEY);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return encryptedMsg;
    }

    public static String decryptMessage(String message) {
        if (message != null && !message.equals("")) {
            try {
                CryptLib cryptLib = new CryptLib();
                message = cryptLib.decryptCipherTextWithRandomIV(message, Constants.MESSAGE_CRYPT_KEY);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return message;
        }
        return "";
    }
    FirebaseAnalytics mFirebaseAnalytics;
    @Override
    public void onCreate() {
        super.onCreate();
        userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .build();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "1");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "appopen");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Application");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        /*ComponentName receiver = new ComponentName(this, AlarmReceiver.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);*/

//        setAlarm(this);
        pref = getApplicationContext().getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
//        changeLocale(getBaseContext());
        if (pref.getBoolean("isLogged", false)) {
            GetSet.setLogged(true);
            GetSet.setUserId(pref.getString("userId", null));
            GetSet.setUserName(pref.getString("userName", null));
            GetSet.setphonenumber(pref.getString("phoneNumber", null));
            GetSet.setcountrycode(pref.getString("countryCode", null));
            GetSet.setImageUrl(pref.getString("userImage", null));
            GetSet.setToken(pref.getString("token", null));
            GetSet.setAbout(pref.getString("about", null));
            GetSet.setPrivacyprofileimage(pref.getString("privacyprofileimage", null));
            GetSet.setPrivacylastseen(pref.getString("privacylastseen", null));
            GetSet.setPrivacyabout(pref.getString("privacyabout", null));
            GetSet.setBday(pref.getString("bday", null));
            GetSet.setGender(pref.getString("gender", null));
            GetSet.setCountry(pref.getString("country", null));
            GetSet.setCity(pref.getString("city", null));
            GetSet.setIsRandomOn(pref.getBoolean("isRandom",false));
        }
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        mInstance = this;
        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this);
        builder.setBuildConfigClass(BuildConfig.class).setReportFormat(StringFormat.JSON);
        builder.getPluginConfigurationBuilder(MailSenderConfigurationBuilder.class);
//        ACRA.init(this);
        FontsOverride.setDefaultFont(this, "MONOSPACE", "font_regular.ttf");
        Stetho.initializeWithDefaults(this);
        dbhelper = DatabaseHandler.getInstance(this);
        if (GetSet.isLogged()) {
            socketConnection = SocketConnection.getInstance(this);
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                foregroundActivity = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                foregroundActivity = null;
            }
        });
    }

    public void setRandomChatSettings(boolean isRandomOn,String bday, String gen,String country,String city ){
        editor.putString("bday", bday).apply();
        editor.putString("gender", gen).apply();
        editor.putString("country", country).apply();
        editor.putString("city", city).apply();
        editor.putBoolean("isRandom",isRandomOn).apply();
        GetSet.setBday(pref.getString("bday", null));
        GetSet.setGender(pref.getString("gender", null));
        GetSet.setCountry(pref.getString("country", null));
        GetSet.setCity(pref.getString("city", null));
        GetSet.setIsRandomOn(pref.getBoolean("isRandom",false));

    }

    public Activity getForegroundActivity() {
        return foregroundActivity;
    }
    public boolean useExtensionRenderers() {
        return com.google.android.exoplayer2.BuildConfig.FLAVOR.equals("withExtensions");
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onAppBackgrounded() {
        Log.d("MyApp", "App in background");
        onAppForegrounded = false;
        if (!onShareExternal && !isInCall) {
            if (socketConnection != null) {
                socketConnection.disconnect();
            }
            Intent service = new Intent(this, ForegroundService.class);
            service.setAction("stop");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }

            if (dbhelper != null) {
                dbhelper.close();
            }
        }

        if (isInCall) {
            startService(new Intent(getBaseContext(), CallNotificationService.class));
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onAppForegrounded() {
        Log.d("MyApp", "App in foreground");
        onAppForegrounded = true;
        dbhelper = DatabaseHandler.getInstance(this);
        if (GetSet.isLogged() && !isNetworkConnected().equals(NOT_CONNECT) && !isInCall) {
            socketConnection = SocketConnection.getInstance(this);
            Intent service = new Intent(this, ForegroundService.class);
            service.setAction("start");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    private void onAppAny() {
        Log.d("MyApp", "App in onAppAny");
    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(LocaleManager.setLocale(context));
        MultiDex.install(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleManager.setLocale(this);
    }

    public void setConnectivityListener(NetworkReceiver.ConnectivityReceiverListener listener) {
        NetworkReceiver.connectivityReceiverListener = listener;
    }

    public void changeLocale(Context context) {
        Configuration config = context.getResources().getConfiguration();
        String lang = pref
                .getString(Constants.TAG_LANGUAGE_CODE, Constants.TAG_DEFAULT_LANGUAGE_CODE);
        if (!(config.locale.getLanguage().equals(lang))) {
            locale = new Locale(lang);
            Locale.setDefault(locale);
            config.locale = locale;
            Log.e("ApplicationClass: ", lang);
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
    }

    public static boolean checkEnglish(String text) {
        return isRTL() && new Locale(text).getLanguage().equals("en");
    }

    public static boolean isExceedsOneHour(String time) {
        long diff = (System.currentTimeMillis() / 1000) - Long.parseLong(time);
        return (diff > 3600);
    }

    public static int getHeight(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return displayMetrics.heightPixels;
    }

    public static int getWidth(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return displayMetrics.widthPixels;
    }

    public static String getMapUrl(String lat, String lng, Context context) {
        int size = dpToPx(context, 170);
        return "https://maps.googleapis.com/maps/api/staticmap?center=" + lat + "," + lng
                + "&zoom=18&size=" + size + "x" + size + "&maptype=roadmap&key="
                + context.getString(R.string.google_api_key);
    }

    public static void openImage(Context context, String url, String type, ImageView view) {
        preventMultiClick(view);
        Intent intent = new Intent(context, ImageOpenActivity.class);
        intent.putExtra(Constants.TAG_USER_IMAGE, url);
        intent.putExtra(Constants.TAG_FROM, type);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void pauseExternalAudio(Context context) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (manager.isMusicActive()) {
            Constants.isExternalPlay = true;
            Intent i = new Intent("com.android.music.musicservicecommand");
            i.putExtra("command", "pause");
            context.sendBroadcast(i);
        }
    }

    public static void resumeExternalAudio(Context context) {
        if (Constants.isExternalPlay) {
            Constants.isExternalPlay = false;
            Intent i = new Intent("com.android.music.musicservicecommand");
            i.putExtra("command", "play");
            context.sendBroadcast(i);
        }
    }

    public static boolean isStringNotNull(String value) {
        return value != null && !TextUtils.isEmpty(value);
    }

    public static boolean isVideoFile(String fileUrl) {
        String mimeType = URLConnection.guessContentTypeFromName(fileUrl);
        return mimeType != null && mimeType.startsWith("video");
    }

    public static String getMimeType(String fileUrl) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    public static boolean isVideo(String mimeType) {
        Log.v("mimeType", "mimeType=" + mimeType);
        return mimeType != null && mimeType.startsWith("video");
    }

}
