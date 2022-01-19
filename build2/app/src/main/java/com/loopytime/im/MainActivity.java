package com.loopytime.im;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.DownloadFiles;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.StorageManager;
import com.loopytime.helper.Utils;
import com.loopytime.im.status.CameraKitActivity;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import hotchemi.android.rate.AppRate;
import hotchemi.android.rate.OnClickButtonListener;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.loopytime.im.CallFragment.callFragment;
import static com.loopytime.utils.Constants.RATING_SHOWN;
import static com.loopytime.utils.Constants.STATUS_WHATSAPP;
import static com.loopytime.utils.Constants.STATUS_WHATSAPP_COUNT;
import static com.loopytime.utils.Constants.STATUS_WHATSAPP_PATH;

public class MainActivity extends BaseActivity implements
        NavigationView.OnNavigationItemSelectedListener, View.OnClickListener,
        TabLayout.OnTabSelectedListener, SocketConnection.OnUpdateTabIndication, CallFragment.ClearLogFunction {

    private static final String TAG = MainActivity.class.getSimpleName();
    public CircleImageView userImage;
    public static boolean loadPost = false;
    Toolbar toolbar;
    private InterstitialAd mInterstitialAd;
    AdRequest adRequest;
    AppBarLayout appBarLayout;
    ViewPagerAdapter adapter;
    TabLayout tabLayout;
    ViewPager viewPager;
    ImageView navBtn, fab, searchBtn, closeDelete, deleteLog;
    DatabaseHandler dbhelper;
    DrawerLayout drawer;
    NavigationView navigationView;
    LinearLayout usrLayout, fabSearch;
    TextView userName, logCount;
    RelativeLayout deleteLay;
    private Utils utils;
    private StorageManager storageManager;

    public static List<Intent> POWERMANAGER_INTENTS = Arrays.asList(
            new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.sysfloatwindow.FloatWindowListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.floatwindow.FloatWindowListActivity")),
            new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.floatwindow.FloatWindowListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            new Intent().setComponent(new ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.activity.LandingPageActivity")),
            new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")).setData(android.net.Uri.parse("mobilemanager://function/entry/AutoStart"))
    );

    void setTheme() {
        findViewById(R.id.toolbar).setBackgroundColor(color.toActionBarColor(this));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            //w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }*/
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }*/
        super.onCreate(savedInstanceState);
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        //setStatusBarGradient(this);

        setContentView(R.layout.activity_main);
        openSecretChatFromNotification(getIntent());
        showRatings();
        MobileAds.initialize(this, getString(R.string.im_ad_app_id));
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.im_ad_interstantial_id));
        /*mInterstitialAd.loadAd(new AdRequest.Builder().addTestDevice("90DE0FF5D1BEB82ACBE8518D057B6FA5").build());*/
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
  /*      adRequest = new AdRequest.Builder().addTestDevice("90DE0FF5D1BEB82ACBE8518D057B6FA5")
                .build();*/
        //adRequest = new AdRequest.Builder()
        //      .build();

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                // if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
                //}
            }
        });
        toolbar = findViewById(R.id.toolbar);
        viewPager = findViewById(R.id.viewpager);
        navBtn = findViewById(R.id.navBtn);
        drawer = findViewById(R.id.drawer_layout);
        tabLayout = findViewById(R.id.tabs);
        navigationView = findViewById(R.id.nav_view);
        fab = findViewById(R.id.fab);
        searchBtn = findViewById(R.id.searchBtn);
        logCount = findViewById(R.id.logCount);
        closeDelete = findViewById(R.id.closeDelete);
        deleteLog = findViewById(R.id.deleteLog);
        deleteLay = findViewById(R.id.deleteLay);
        fabSearch = findViewById(R.id.fabSearch);
        utils = new Utils(this);
        utils.getNavigationBarHeight();
        utils.getStatusBarHeight();
        if (ApplicationClass.isRTL()) {
            navBtn.setRotation(180);
        } else {
            navBtn.setRotation(0);
        }
        setupViewPager(viewPager);
        viewPager.setOffscreenPageLimit(3);
        View header = navigationView.getHeaderView(0);
        dbhelper = DatabaseHandler.getInstance(this);
        SocketConnection.getInstance(this).setOnUpdateTabIndication(this);
        storageManager = StorageManager.getInstance(this);
        storageManager.deleteCacheDir();

        userImage = header.findViewById(R.id.userImage);
        usrLayout = header.findViewById(R.id.usrLayout);
        userName = header.findViewById(R.id.userName);

        tabLayout.addOnTabSelectedListener(this);
        navigationView.setNavigationItemSelectedListener(this);
        navBtn.setOnClickListener(this);
        searchBtn.setOnClickListener(this);
        usrLayout.setOnClickListener(this);
        deleteLog.setOnClickListener(this);
        closeDelete.setOnClickListener(this);

        updateTabIndication();

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer,
                null, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                Log.v("Drawer", "Drawer Opened");
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                Log.v("Drawer", "Drawer Closed");
            }
        };
        drawer.addDrawerListener(toggle);
        drawer.post(new Runnable() {
            @Override
            public void run() {
                toggle.syncState();
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tabLayout != null && tabLayout.getSelectedTabPosition() == 0) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_CONTACTS}, 101);
                    } else {
                        Intent s = new Intent(getApplicationContext(), SelectContact.class);
                        s.putExtra(Constants.TAG_USER_ID, GetSet.getUserId());
                        startActivity(s);
                    }
                }
                /*Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm.isIgnoringBatteryOptimizations(packageName))
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                else {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                }
                startActivity(intent);*/
            }
        });

        navigationView.post(new Runnable() {
            @Override
            public void run() {
                Resources r = getResources();
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int width = metrics.widthPixels;

                float screenWidth = width / r.getDisplayMetrics().density;
                float navWidth = screenWidth - 56;

                navWidth = Math.min(navWidth, 320);

                int newWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, navWidth, r.getDisplayMetrics());

                DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) navigationView.getLayoutParams();
                params.width = newWidth;
                navigationView.setLayoutParams(params);
            }
        });

        if (viewPager != null && getIntent().getStringExtra(Constants.IS_FROM) != null) {
            if (getIntent().getStringExtra(Constants.IS_FROM).equalsIgnoreCase("group")) {
                viewPager.setCurrentItem(1);
            } else if (getIntent().getStringExtra(Constants.IS_FROM).equalsIgnoreCase("channel")) {
                viewPager.setCurrentItem(2);
            }
        }

//        enableAutoStart();
        startPowerSaverIntent(this);
//        AutoStartPermissionHelper.getInstance().getAutoStartPermission(MainActivity.this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openSecretChatFromNotification(intent);
    }

    public static void startPowerSaverIntent(Context context) {
        SharedPreferences settings = context.getSharedPreferences("ProtectedApps", Context.MODE_PRIVATE);
        boolean skipMessage = settings.getBoolean("skipProtectedAppCheck", false);
        if (!skipMessage) {
            final SharedPreferences.Editor editor = settings.edit();
            boolean foundCorrectIntent = false;
            for (Intent intent : POWERMANAGER_INTENTS) {
                if (isCallable(context, intent)) {
                    foundCorrectIntent = true;
                    final AppCompatCheckBox dontShowAgain = new AppCompatCheckBox(context);
                    dontShowAgain.setText("Do not show again");
                    dontShowAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            editor.putBoolean("skipProtectedAppCheck", isChecked);
                            editor.apply();
                        }
                    });

                    new AlertDialog.Builder(context)
                            .setTitle(Build.MANUFACTURER + " Protected Apps")
                            .setMessage(String.format("%s requires to be enabled in 'Protected Apps' to function properly.%n", context.getString(R.string.app_name)))
                            .setView(dontShowAgain)
                            .setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        context.startActivity(intent);
                                    } catch (SecurityException se) {
                                        Log.e(TAG, "startPowerSaverIntent: " + se.getMessage());
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    break;
                }
            }
            if (!foundCorrectIntent) {
                editor.putBoolean("skipProtectedAppCheck", true);
                editor.apply();
            }
        }
    }

    private static boolean isCallable(Context context, Intent intent) {
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void enableAutoStart() {
        if (Build.BRAND.equalsIgnoreCase("xiaomi")) {
            new AlertDialog.Builder(MainActivity.this).setTitle("Enable AutoStart")
                    .setMessage(
                            "Please allow AppName to always run in the background,else our services can't be accessed.")
                    .setPositiveButton("ALLOW", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialog, @NonNull int which) {

                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.miui.securitycenter",
                                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                            startActivity(intent);
                        }
                    })
                    .show();
        } else if (Build.BRAND.equalsIgnoreCase("Letv")) {
            new AlertDialog.Builder(MainActivity.this).setTitle("Enable AutoStart")
                    .setMessage(
                            "Please allow AppName to always run in the background,else our services can't be accessed.")
                    .setPositiveButton("ALLOW", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialog, @NonNull int which) {

                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.letv.android.letvsafe",
                                    "com.letv.android.letvsafe.AutobootManageActivity"));
                            startActivity(intent);
                        }
                    })
                    .show();
        } else if (Build.BRAND.equalsIgnoreCase("Honor")) {
            new AlertDialog.Builder(MainActivity.this).setTitle("Enable AutoStart")
                    .setMessage(
                            "Please allow AppName to always run in the background,else our services can't be accessed.")
                    .setPositiveButton("ALLOW", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialog, @NonNull int which) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.huawei.systemmanager",
                                    "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                            startActivity(intent);
                        }
                    })
                    .show();
        } else if (Build.MANUFACTURER.equalsIgnoreCase("oppo")) {
            new AlertDialog.Builder(MainActivity.this).setTitle("Enable AutoStart")
                    .setMessage(
                            "Please allow AppName to always run in the background,else our services can't be accessed.")
                    .setPositiveButton("ALLOW", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialog, @NonNull int which) {
                            try {
                                Intent intent = new Intent();
                                intent.setClassName("com.coloros.safecenter",
                                        "com.coloros.safecenter.permission.startup.StartupAppListActivity");
                                startActivity(intent);
                            } catch (Exception e) {
                                try {
                                    Intent intent = new Intent();
                                    intent.setClassName("com.oppo.safe",
                                            "com.oppo.safe.permission.startup.StartupAppListActivity");
                                    startActivity(intent);
                                } catch (Exception ex) {
                                    try {
                                        Intent intent = new Intent();
                                        intent.setClassName("com.coloros.safecenter",
                                                "com.coloros.safecenter.startupapp.StartupAppListActivity");
                                        startActivity(intent);
                                    } catch (Exception exx) {

                                    }
                                }
                            }
                        }
                    })
                    .show();
        } else if (Build.MANUFACTURER.contains("vivo")) {
            new AlertDialog.Builder(MainActivity.this).setTitle("Enable AutoStart")
                    .setMessage("Please allow AppName to always run in the background.Our loopytime runs in background else our services can't be accesed.")
                    .setPositiveButton("ALLOW", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialog, @NonNull int which) {
                            try {
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName("com.iqoo.secure",
                                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
                                startActivity(intent);
                            } catch (Exception e) {
                                try {
                                    Intent intent = new Intent();
                                    intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
                                    startActivity(intent);
                                } catch (Exception ex) {
                                    try {
                                        Intent intent = new Intent();
                                        intent.setClassName("com.iqoo.secure",
                                                "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager");
                                        startActivity(intent);
                                    } catch (Exception exx) {
                                        ex.printStackTrace();
                                    }
                                }
                            }
                        }
                    })
                    .show();
        }
    }

    private void updateTabIndication() {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            View selected = tab.getCustomView();
            ImageView indication = selected.findViewById(R.id.indication);

            if (i == 0) {
                if (dbhelper.isRecentChatIndicationExist()) {
                    indication.setVisibility(View.VISIBLE);
                } else {
                    indication.setVisibility(View.GONE);
                }
            } else if (i == 1) {
                if (dbhelper.isRecentGroupIndicationExist()) {
                    indication.setVisibility(View.VISIBLE);
                } else {
                    indication.setVisibility(View.GONE);
                }
            } else if (i == 2) {
                if (dbhelper.isRecentChannelIndicationExist()) {
                    indication.setVisibility(View.VISIBLE);
                } else {
                    indication.setVisibility(View.GONE);
                }
            } else if (i == 3) {
                if (dbhelper.isMissedCallIndicationExist()) {
                    indication.setVisibility(View.VISIBLE);
                } else {
                    indication.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    public static void setStatusBarGradient(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            Drawable background = activity.getResources().getDrawable(R.drawable.gradient);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(activity.getResources().getColor(android.R.color.transparent));
            window.setNavigationBarColor(activity.getResources().getColor(android.R.color.transparent));
            window.setBackgroundDrawable(background);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), MainActivity.this);
        adapter.addFragment(new ChatFragment(), getString(R.string.chat));
        adapter.addFragment(new GroupFragment(), getString(R.string.group));
        adapter.addFragment(new ChannelFragment(), getString(R.string.channels));
        adapter.addFragment(CallFragment.newInstance(this), getString(R.string.calls));
        adapter.addFragment(new PostFragment(), getString(R.string.post));
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        // Iterate over all tabs and set the custom view
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            tab.setCustomView(adapter.getTabView(i, this));
        }
        adapter.setOnSelectView(this, tabLayout, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v("requestCode", "requestCode=" + requestCode);
        switch (requestCode) {
            case 101:
                int permContacts = ContextCompat.checkSelfPermission(MainActivity.this,
                        READ_CONTACTS);
                if (permContacts == PackageManager.PERMISSION_GRANTED) {
                    Intent s = new Intent(this, SelectContact.class);
                    s.putExtra(Constants.TAG_USER_ID, GetSet.getUserId());
                    startActivity(s);
                }
                break;
            case 100:
                boolean isContactEnabled = false;

                for (String permission : permissions) {
                    if (permission.equals(READ_CONTACTS)) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
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
                    onResumeFunction();
                }
                break;
        }
    }

    private void requestPermission(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(MainActivity.this, permissions, requestCode);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.navBtn:
                drawer.openDrawer(GravityCompat.START);
                break;
            case R.id.searchBtn:
                startActivity(new Intent(this, SearchActivity.class));
                break;
            case R.id.usrLayout:
                ApplicationClass.preventMultiClick(usrLayout);
                Intent p = new Intent(this, ProfileActivity.class);
                p.putExtra(Constants.TAG_USER_ID, GetSet.getUserId());
                startActivity(p);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.deleteLog:
                deleteLay.setVisibility(View.GONE);
                callFragment.deleteCallLog("delete");
                break;
            case R.id.closeDelete:
                deleteLay.setVisibility(View.GONE);
                callFragment.deleteCallLog("clear");
                break;

        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Log.v("onNavigation", "=" + item.getTitle());
        int id = item.getItemId();
        switch (id) {
            case R.id.yourrides_menu:
                Intent channel = new Intent(MainActivity.this, MyChannelsActivity.class);
                startActivity(channel);
                break;
            case R.id.theme:
                startActivityForResult(new Intent(MainActivity.this, ChooseTheme.class), THEME_RESULT_CODE);
                break;
//            case R.id.ranChats:
//                startActivity(new Intent(MainActivity.this, RandomChatListActivity.class));
//                break;
//            case R.id.locked:
//                if (getSharedPreferences("wall", Context.MODE_PRIVATE).getString("seq_pass", null) == null) {
//                    setPassword(false);
//                } else
//                    enterPassword();
//
//                break;
//            case R.id.noteSelf:
//                Bundle bundle = new Bundle();
//                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "4");
//                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "note to self ");
//                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Activity");
//                ApplicationClass.getInstance().mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
//                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
//                intent.putExtra("user_id", GetSet.getUserId());
//                startActivity(intent);
//                break;
            case R.id.wallet_menu:
                Intent account = new Intent(MainActivity.this, AccountActivity.class);
                startActivity(account);
                break;
            case R.id.invite_menu:
                Intent g = new Intent(Intent.ACTION_SEND);
                g.setType("text/plain");
                g.putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_message) + "https://play.google.com/store/apps/details?id=" +
                        getApplicationContext().getPackageName());
                startActivity(Intent.createChooser(g, "Share"));
                break;
            case R.id.help_menu:
                Intent help = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(help);
                break;
        }
        //  switchActivityByNavigation(id, item);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        adapter.setOnSelectView(this, tabLayout, tab.getPosition());
        if (tab.getPosition() == 0) {
            if (deleteLay.getVisibility() == View.VISIBLE) {
                fabSearch.setVisibility(View.VISIBLE);
            }
            fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.home_page_chat));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (drawer != null) {
                        drawer.closeDrawer(GravityCompat.START);
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_CONTACTS}, 101);
                    } else {
                        Intent s = new Intent(getApplicationContext(), SelectContact.class);
                        s.putExtra(Constants.TAG_USER_ID, GetSet.getUserId());
                        startActivity(s);
                    }
                }
            });
        } else if (tab.getPosition() == 1) {
            if (deleteLay.getVisibility() == View.VISIBLE) {
                fabSearch.setVisibility(View.VISIBLE);
            }
            fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.floating_group));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (drawer != null) {
                        drawer.closeDrawer(GravityCompat.START);
                    }
                    Intent s = new Intent(getApplicationContext(), NewGroupActivity.class);
                    s.putExtra(Constants.TAG_USER_ID, GetSet.getUserId());
                    startActivity(s);
                }
            });
        } else if (tab.getPosition() == 2) {
            if (deleteLay.getVisibility() == View.VISIBLE) {
                fabSearch.setVisibility(View.VISIBLE);
            }
            fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.floating_channel));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (drawer != null) {
                        drawer.closeDrawer(GravityCompat.START);
                    }
                    Intent s = new Intent(getApplicationContext(), CreateChannelActivity.class);
                    s.putExtra(Constants.TAG_USER_ID, GetSet.getUserId());
                    startActivity(s);
//                    Toast.makeText(MainActivity.this, "Coming Soon", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (tab.getPosition() == 3) {
            if (deleteLay.getVisibility() == View.VISIBLE) {
                fabSearch.setVisibility(View.GONE);
            }
            fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.floating_call));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (drawer != null) {
                        drawer.closeDrawer(GravityCompat.START);
                    }
                    Intent s = new Intent(getApplicationContext(), CallContactActivity.class);
                    startActivity(s);
                }
            });
        } else if (tab.getPosition() == 4) {
            if (deleteLay.getVisibility() == View.VISIBLE) {
                fabSearch.setVisibility(View.GONE);
            }
            fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add_white));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (drawer != null) {
                        drawer.closeDrawer(GravityCompat.START);
                    }
                    if (ContextCompat.checkSelfPermission(MainActivity.this, CAMERA) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(MainActivity.this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(MainActivity.this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{CAMERA, RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, 1010);
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "5");
                        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Post create open ");
                        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Activity");
                        ApplicationClass.getInstance().mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                        loadPost = true;
                        Intent i = new Intent(MainActivity.this, CameraKitActivity.class);
                        i.putExtra(Constants.IS_POST, true);
                        startActivity(i);

                    }
                }
            });
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        adapter.setUnSelectView(this, tabLayout, tab.getPosition());
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Override
    public void updateIndication() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateTabIndication();
            }
        });
    }

    @Override
    public void isDeleteVisible(boolean isDelete, int count) {
        if (isDelete) {
            fabSearch.setVisibility(View.GONE);
            deleteLay.setVisibility(View.VISIBLE);
            logCount.setText("" + count);
        } else {
            fabSearch.setVisibility(View.VISIBLE);
            deleteLay.setVisibility(View.GONE);
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();
        Context context;

        public ViewPagerAdapter(FragmentManager manager, Context context) {
            super(manager);
            this.context = context;
        }

        public View getTabView(int position, Context context) {
            // Given you have a custom layout in `res/layout/custom_tab.xml` with a TextView and ImageView
            View v = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);
            TextView tabName = (TextView) v.findViewById(R.id.tabName);
            tabName.setText(mFragmentTitleList.get(position));
            // ImageView indication = (ImageView) v.findViewById(R.id.indication);
            return v;
        }

        public void setOnSelectView(Context mContext, TabLayout tabLayout, int position) {
            TabLayout.Tab tab = tabLayout.getTabAt(position);
            View selected = tab.getCustomView();
            TextView tabName = selected.findViewById(R.id.tabName);
            tabName.setTextColor(mContext.getResources().getColor(R.color.primarytext));
        }

        public void setUnSelectView(Context mContext, TabLayout tabLayout, int position) {
            TabLayout.Tab tab = tabLayout.getTabAt(position);
            View selected = tab.getCustomView();
            TextView iv_text = selected.findViewById(R.id.tabName);
            iv_text.setTextColor(mContext.getResources().getColor(R.color.secondarytext));
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        Log.e(TAG, "onResume: " + GetSet.getUserName());
        onResumeFunction();
        /*if(checkPermissions()){
        } else {
            if (ContextCompat.checkSelfPermission(this, READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{READ_CONTACTS, WRITE_EXTERNAL_STORAGE}, 100);
            }
        }*/

    }

    private void onResumeFunction() {
        SocketConnection.getInstance(this).setOnUpdateTabIndication(this);
        updateTabIndication();
        userName.setText(GetSet.getUserName());
        Glide.with(MainActivity.this).load(Constants.USER_IMG_PATH + GetSet.getImageUrl())
                .apply(new RequestOptions().placeholder(R.drawable.temp).error(R.drawable.temp))
                .into(userImage);
    }

    private boolean checkPermissions() {
        int permissionContacts = ContextCompat.checkSelfPermission(MainActivity.this, READ_CONTACTS);

        return permissionContacts == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
        SocketConnection.getInstance(this).setOnUpdateTabIndication(null);
    }

    void showRatings() {
        downloadWhatVideo();
        if (getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE).getBoolean(RATING_SHOWN, false)) {
            return;
        }
        AppRate.with(this)
                .setInstallDays(0) // default 10, 0 means install day.
                .setLaunchTimes(3) // default 10
                .setRemindInterval(1) // default 1
                .setShowLaterButton(true) // default true
                .setDebug(false) // default false
                .setOnClickButtonListener(new OnClickButtonListener() { // callback listener.
                    @Override
                    public void onClickButton(int which) {
                        if (which == 0) {
                            getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE).edit().putBoolean(RATING_SHOWN, true).commit();
                            Utils.launchMarket(MainActivity.this);
                        }
                    }
                })
                .monitor();

        // Show a dialog if meets conditions
        AppRate.showRateDialogIfMeetsConditions(this);
    }

    void downloadWhatVideo() {
        if (getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE).getBoolean(STATUS_WHATSAPP, false)) {
            return;
        }
        if (getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE).getString(STATUS_WHATSAPP_PATH, null) == null) {
            DownloadFiles downloadFiles = new DownloadFiles(MainActivity.this) {
                @Override
                protected void onPostExecute(String downPath) {
                    if (downPath == null) {
                        Log.v("Download Failed", "Download Failed");
                    } else {
                        getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE).edit().putString(STATUS_WHATSAPP_PATH, downPath).commit();
                        //Toast.makeText(mContext, getString(R.string.downloaded), Toast.LENGTH_SHORT).show();
                    }
                }
            };
            downloadFiles.execute("http://indianmessenger.in:3040/getTikTok/tiktok.mp4", "video");
        } else {
            int count = getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE).getInt(STATUS_WHATSAPP_COUNT, 0);
            if (count > 0) {
                count--;
                getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE).edit().putInt(STATUS_WHATSAPP_COUNT, count).commit();
                return;
            }
            new AlertDialog.Builder(MainActivity.this).setTitle(R.string.shareToWhatsApp).setMessage(R.string.invite_message).setPositiveButton("Share", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    shareFileWhatsapp(getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE).getString(STATUS_WHATSAPP_PATH, null));
                    getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE).edit().putBoolean(STATUS_WHATSAPP, true);
                }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE).edit().putInt(STATUS_WHATSAPP_COUNT, 5).commit();
                }
            })
                    .create().show();
            ;

        }
    }

    void shareFileWhatsapp(String path) {
        File shareFile = new File(path);
        Uri imageUri = Uri.parse(path);// FileProvider.getUriForFile(this, getPackageName() + ".provider", shareFile);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setPackage("com.whatsapp");
        final String final_text = getString(R.string.invite_message);
        shareIntent.putExtra(Intent.EXTRA_TEXT, final_text);
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.setType("video/*");
        startActivity(shareIntent);
    }

    int THEME_RESULT_CODE = 134;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == THEME_RESULT_CODE) {

                Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }

        }
    }

    androidx.appcompat.widget.PopupMenu popupMenu;

    void popup(TextView v) {
        popupMenu = new androidx.appcompat.widget.PopupMenu(this, v);
        popupMenu.getMenu().add(0, 0, 0, "what is your favorite food?");
        popupMenu.getMenu().add(0, 1, 0, "what is your favorite game?");
        popupMenu.getMenu().add(0, 2, 0, "what is your favorite character?");
        popupMenu.getMenu().add(0, 2, 0, "what is your favorite place?");
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                v.setText(item.getTitle());
                return false;
            }
        });
    }

    void setPassword(boolean reset) {
        alertDialogBuilderUserInput = null;
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
        View mView = layoutInflaterAndroid.inflate(R.layout.set_new_password, null);
        EditText pass = mView.findViewById(R.id.et1);
        EditText ans = mView.findViewById(R.id.et2);
        TextView quest = mView.findViewById(R.id.quest);
        if (reset) {
            String q = getSharedPreferences("wall", Context.MODE_PRIVATE).getString("seq_ques", null);
            String qa = getSharedPreferences("wall", Context.MODE_PRIVATE).getString("seq_ans", null);
            ((TextView) mView.findViewById(R.id.title)).setText(R.string.reset_pass);
            ((TextView) mView.findViewById(R.id.tag)).setText(R.string.reset_pass_tag_new);
            quest.setText(q);
            ans.setText(qa);
        }

        mView.findViewById(R.id.quest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popup((TextView) v);
            }
        });
        mView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialogBuilderUserInput.dismiss();
            }
        });
        mView.findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(pass.getText()) || pass.getText().length() < 4) {
                    Toast.makeText(MainActivity.this, R.string.pass_err, Toast.LENGTH_SHORT).show();
                    pass.requestFocus();
                    return;
                }
                if (getString(R.string.sel_sec_quest).equalsIgnoreCase(quest.getText().toString())) {
                    Toast.makeText(MainActivity.this, R.string.sel_sec_quest, Toast.LENGTH_SHORT).show();
                    popup(quest);
                    return;
                }
                if (TextUtils.isEmpty(ans.getText())) {
                    Toast.makeText(MainActivity.this, R.string.ans_err, Toast.LENGTH_SHORT).show();
                    ans.requestFocus();
                    return;
                }
                getSharedPreferences("wall", Context.MODE_PRIVATE).edit().putString("seq_ques", quest.getText().toString()).commit();
                getSharedPreferences("wall", Context.MODE_PRIVATE).edit().putString("seq_ans", ans.getText().toString()).commit();
                getSharedPreferences("wall", Context.MODE_PRIVATE).edit().putString("seq_pass", pass.getText().toString()).commit();
                openActivity();
                alertDialogBuilderUserInput.dismiss();
            }
        });

        alertDialogBuilderUserInput = new AlertDialog.Builder(this).create();
        alertDialogBuilderUserInput.setView(mView);
        alertDialogBuilderUserInput.show();
    }

    void enterPassword() {
        alertDialogBuilderUserInput = null;
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
        View mView = layoutInflaterAndroid.inflate(R.layout.set_new_password, null);
        ((TextView) mView.findViewById(R.id.title)).setText(R.string.enter_pass);
        ((TextView) mView.findViewById(R.id.tag)).setText(R.string.enter_pass_tag);
        EditText pass = mView.findViewById(R.id.et1);
        ((ConstraintLayout.LayoutParams) mView.findViewById(R.id.cancel).getLayoutParams()).topMargin = 0;
        ((ConstraintLayout.LayoutParams) mView.findViewById(R.id.accept).getLayoutParams()).topMargin = 0;
        mView.findViewById(R.id.et2).setVisibility(View.GONE);
        mView.findViewById(R.id.forgot_password).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.quest).setVisibility(View.GONE);
        mView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialogBuilderUserInput.dismiss();
            }
        });
        mView.findViewById(R.id.forgot_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialogBuilderUserInput.dismiss();
                forgotPassword();

            }
        });
        mView.findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(pass.getText()) || pass.getText().length() < 4) {
                    Toast.makeText(MainActivity.this, R.string.pass_err, Toast.LENGTH_SHORT).show();
                    pass.requestFocus();
                    return;
                }


                if (getSharedPreferences("wall", Context.MODE_PRIVATE).getString("seq_pass", null).equalsIgnoreCase(pass.getText().toString())) {
                    openActivity();
                    alertDialogBuilderUserInput.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, R.string.wrong_pass, Toast.LENGTH_SHORT).show();
                }

            }
        });

        alertDialogBuilderUserInput = new AlertDialog.Builder(this).create();
        alertDialogBuilderUserInput.setView(mView);
        alertDialogBuilderUserInput.show();
    }


    void forgotPassword() {
        alertDialogBuilderUserInput = null;
        String q = getSharedPreferences("wall", Context.MODE_PRIVATE).getString("seq_ques", null);
        String qa = getSharedPreferences("wall", Context.MODE_PRIVATE).getString("seq_ans", null);
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
        View mView = layoutInflaterAndroid.inflate(R.layout.set_new_password, null);
        ((TextView) mView.findViewById(R.id.title)).setText(R.string.forgot_pass);
        ((TextView) mView.findViewById(R.id.tag)).setText(R.string.reset_pass_tag);
        EditText ans = mView.findViewById(R.id.et2);
        mView.findViewById(R.id.et1).setVisibility(View.GONE);
        ((TextView) mView.findViewById(R.id.quest)).setText(q);

        mView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialogBuilderUserInput.dismiss();
            }
        });
        mView.findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(ans.getText())) {
                    Toast.makeText(MainActivity.this, R.string.ans_err, Toast.LENGTH_SHORT).show();
                    ans.requestFocus();
                    return;
                }


                if (qa.equalsIgnoreCase(ans.getText().toString())) {
                    alertDialogBuilderUserInput.dismiss();
                    setPassword(true);

                } else {
                    Toast.makeText(MainActivity.this, R.string.wrong_ans, Toast.LENGTH_SHORT).show();
                }

            }
        });

        alertDialogBuilderUserInput = new AlertDialog.Builder(this).create();
        alertDialogBuilderUserInput.setView(mView);
        alertDialogBuilderUserInput.show();
    }

    AlertDialog alertDialogBuilderUserInput;

    void openSecretChatFromNotification(Intent i) {

        if (i.hasExtra("isSecret") && i.getBooleanExtra("isSecret", false)) {
            enterPassword();
        }
    }

    void openActivity() {
        Intent i = getIntent();
        if (i.hasExtra("isSecret") && i.getBooleanExtra("isSecret", false)) {
            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);

            intent.putExtra("user_id", i.getStringExtra("user_id"));
            intent.putExtra("notification", "true");
            startActivity(intent);
        } else {
            startActivity(new Intent(MainActivity.this, LockedChatListActivity.class));
        }
    }
}


