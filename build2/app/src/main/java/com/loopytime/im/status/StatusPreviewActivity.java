package com.loopytime.im.status;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.loopytime.external.keyboard.KeyboardHeightObserver;
import com.loopytime.external.keyboard.KeyboardHeightProvider;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.ProgressRequestBody2;
import com.loopytime.helper.StorageManager;
import com.loopytime.im.ApplicationClass;
import com.loopytime.im.BaseActivity;
import com.loopytime.im.NewGroupActivity;
import com.loopytime.im.R;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;

public class StatusPreviewActivity extends BaseActivity implements View.OnClickListener, KeyboardHeightObserver {

    private static final String TAG = StatusPreviewActivity.class.getSimpleName();
    private ImageView cameraImage, galleryImage;
    private RelativeLayout mainLay, titleLay, closeLayout, bottomLay, editLay;
    private EditText editText;
    private ImageView send;
    private String filePath, sourceType;
    private StorageManager storageManager;
    private SharedPreferences pref;
    private KeyboardHeightProvider keyboardHeightProvider;
    private int bottomNavHeight,  bottomMargin = 0;

    View.OnApplyWindowInsetsListener applyWindowListener = new View.OnApplyWindowInsetsListener() {
        @Override
        public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
            bottomNavHeight = view.getPaddingBottom() + windowInsets.getSystemWindowInsetBottom() + ApplicationClass.dpToPx(StatusPreviewActivity.this, 10);
            return windowInsets.consumeSystemWindowInsets();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setContentView(R.layout.activity_status_preview);
        storageManager = StorageManager.getInstance(this);
        pref = getSharedPreferences("SavedPref", MODE_PRIVATE);
        keyboardHeightProvider = new KeyboardHeightProvider(this);
        bottomMargin = ApplicationClass.dpToPx(this, 2);

        findViews();

        if (ApplicationClass.isRTL()) {
            send.setRotation(180);
        } else {
            send.setRotation(0);
        }

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        if (getIntent().hasExtra(Constants.TAG_ATTACHMENT)) {
            filePath = getIntent().getExtras().getString(Constants.TAG_ATTACHMENT);
            sourceType = getIntent().getExtras().getString(Constants.TAG_FROM);
        }

        if (filePath != null) {
            if (sourceType.equals(Constants.TAG_CAMERA)) {
                cameraImage.setVisibility(View.VISIBLE);
                galleryImage.setVisibility(View.GONE);
                loadStatus(cameraImage);
            } else {
                cameraImage.setVisibility(View.GONE);
                galleryImage.setVisibility(View.VISIBLE);
                loadStatus(galleryImage);
            }
        }
    }

    public void loadStatus(ImageView imageView) {
        Glide.with(getApplicationContext()).load(filePath)
                .into(imageView);
    }

    private void findViews() {
        mainLay = (RelativeLayout) findViewById(R.id.mainLay);
        cameraImage = (ImageView) findViewById(R.id.camera_image);
        galleryImage = (ImageView) findViewById(R.id.gallery_image);
        titleLay = (RelativeLayout) findViewById(R.id.title_lay);
        closeLayout = (RelativeLayout) findViewById(R.id.close_layout);
        bottomLay = (RelativeLayout) findViewById(R.id.bottom_lay);
        editText = (EditText) findViewById(R.id.editText);
        editLay = findViewById(R.id.editLay);
        send = (ImageView) findViewById(R.id.send);
        mainLay.setOnApplyWindowInsetsListener(applyWindowListener);

        // make sure to start the keyboard height provider after the onResume
        // of this activity. This is because a popup window must be initialised
        // and attached to the activity root view.
        mainLay.post(new Runnable() {
            public void run() {
                keyboardHeightProvider.start();
            }
        });
        closeLayout.setOnClickListener(this);
        send.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        keyboardHeightProvider.setKeyboardHeightObserver(this);
        initMargins();
    }

    @Override
    public void onPause() {
        super.onPause();
        keyboardHeightProvider.setKeyboardHeightObserver(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        keyboardHeightProvider.close();
    }

    private void initMargins() {
        int topMargin = pref.getInt(Constants.TAG_STATUS_HEIGHT, 0);
        int bottomMargin = pref.getInt(Constants.TAG_NAV_HEIGHT, 0);
        initTopLayMargins(topMargin);
       /* if (ApplicationClass.hasNavigationBar()) {
            Log.i(TAG, "initBottomPadding: " + bottomMargin);
            initBottomPadding(ApplicationClass.dpToPx(StatusPreviewActivity.this, bottomMargin + 5));
        } else {
            initBottomPadding(ApplicationClass.dpToPx(StatusPreviewActivity.this, 10));
        }*/
    }

    private void initTopLayMargins(int topMargin) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = topMargin + 5;
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        titleLay.setLayoutParams(layoutParams);
    }

    private void initBottomPadding(int bottomPadding) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bottomLay.setPadding(0, 0, 0, bottomPadding);
        bottomLay.setLayoutParams(params);
    }

    @Override
    public void onNetworkChange(boolean isConnected) {
        ApplicationClass.showSnack(StatusPreviewActivity.this, findViewById(R.id.mainLay), isConnected);
    }

    @Override
    public void onKeyboardHeightChanged(int height, int orientation) {
        // color the keyboard height view, this will remain visible when you close the keyboard
        if (height > 0) {
            if (ApplicationClass.hasNavigationBar()) {
                initBottomPadding(height + ApplicationClass.dpToPx(StatusPreviewActivity.this, 50));
            } else {
                initBottomPadding(height + ApplicationClass.dpToPx(StatusPreviewActivity.this, 10));
            }
        } else if (height < 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    initBottomPadding(bottomNavHeight + ApplicationClass.dpToPx(StatusPreviewActivity.this, 10));
                }
            }, 100);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    initBottomPadding(bottomNavHeight + ApplicationClass.dpToPx(StatusPreviewActivity.this, 10));
                }
            }, 100);
        }
    }

    private void setToImmersiveMode() {
        // set to immersive
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record_button:
                // Handle clicks for recordButton
                break;
            case R.id.close_layout:
                onBackPressed();
                break;
            case R.id.send:
                if (NetworkReceiver.isConnected()) {
                    if (getIntent().getBooleanExtra(Constants.IS_POST, false)) {
                        uplodMedia(GetSet.getUserId(), editText.getText().toString(), 0, GetSet.getUserName(), GetSet.getphonenumber(),filePath, "img_");
                        return;
                    }
                    HashMap<String, String> map = new HashMap<>();
                    map.put(Constants.TAG_MESSAGE, editText.getText().toString());
                    map.put(Constants.TAG_ATTACHMENT, filePath);

                    map.put(Constants.TAG_TYPE, "image");
                    Intent intent = new Intent(this, NewGroupActivity.class);
                    intent.putExtra(Constants.TAG_FROM, StorageManager.TAG_STATUS);
                    intent.putExtra(Constants.TAG_SOURCE_TYPE, sourceType);
                    intent.putExtra(Constants.TAG_MESSAGE_DATA, map);
                    startActivityForResult(intent, Constants.STATUS_IMAGE_CODE);
                } else {
                    ApplicationClass.showSnack(StatusPreviewActivity.this, findViewById(R.id.mainLay), false);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.STATUS_IMAGE_CODE) {
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    void uplodMedia(String uid, String text2, int opt, String uname, String _phoneNumber, String pending_file, String type) {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
            return;
        }
        send.setVisibility(View.GONE);
        findViewById(R.id.progressbar).setVisibility(View.VISIBLE);
        if (pending_file.contains("external_files")) {
            File sdDir = Environment.getExternalStorageDirectory();
            pending_file = sdDir + pending_file.replace("external_files/", "");
        }
        System.out.println("ding dong tkkkkk nmn amzons" + type + " " + uid + " " + opt + " " + uname + " " + _phoneNumber + " " + text2);
        // pending_fileName=FileProvider.getUriForFile(AndroidContext.getContext(), AndroidContext.getContext().getApplicationContext().getPackageName() + ".my.package.name.provider", new File(pending_fileName)).getPath();
        MediaType MEDIA_TYPE_JPG = MediaType.parse("*/*");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("type", type)
                .addFormDataPart("uid", uid)
                .addFormDataPart("isPublic", opt == 0 ? "0" : "1")
                .addFormDataPart("text2", "")

                .addFormDataPart("isFeed", String.valueOf(1))
                .addFormDataPart("uname", uname)
                .addFormDataPart("uPhone", _phoneNumber)
                .addFormDataPart("upic", GetSet.getImageUrl() == null ? "" : GetSet.getImageUrl())
                .addFormDataPart("newPrivateFeed", "true")
                .addFormDataPart("sampleFile", new File(pending_file).getName(),
                        RequestBody.create(MEDIA_TYPE_JPG, new File(pending_file)))
                .build();

        System.out.println("ding dong tkkkkk nmn amzon ll" + pending_file);
        String finalPending_file = pending_file;
        ProgressRequestBody2 prb = new ProgressRequestBody2(requestBody, new ProgressRequestBody2.Listener() {
            @Override
            public void onProgress(int progress) {
                System.out.println("ding dong tkkkkknp reslalal; " + progress + "##" + new File(finalPending_file).length());
                //     reportProgress(progress);
            }

        });


        System.out.println("ding dong tkkkkknp amzonxxnmju");
        String url = Constants.NODE_URL + "set_statusHiddi";
        Request request = new Request.Builder()
                .url(url)
                .post(prb)
                .build();
        System.out.println("ding dong tkkkkknp amzonxx");
        ApplicationClass.getInstance().httpClient.newCall(request)
                .enqueue(new Callback() {
                             @Override
                             public void onFailure(Call call, IOException e) {
                                 runOnUiThread(new Runnable() {
                                     @Override
                                     public void run() {
                                         networkSnack();
                                     }
                                 });
                             }

                             @Override
                             public void onResponse(Call call, Response response) {
                                 String res = null;
                                 try {
                                     res = response.body().string();
                                     System.out.println("ding dong tkkkkknp res" + res);
                                     try {
                                         JSONObject obj = new JSONObject(res);

                                         if (obj.getBoolean("success")) {


                                         } else if (obj.has("blocked") && obj.getInt("blocked") > 0) {
                                             //                                   reportProgress(-1);
                                         }

                                         runOnUiThread(new Runnable() {
                                             @Override
                                             public void run() {
                                                 setResult(Activity.RESULT_OK);
                                                 finish();
                                             }
                                         });

                                     } catch (JSONException e) {
                                         e.printStackTrace();
                                         runOnUiThread(new Runnable() {
                                             @Override
                                             public void run() {
                                                 networkSnack();
                                             }
                                         });
                                         //                             reportError();
                                     }

                                 } catch (IOException e) {
                                     //reportError();
                                     runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                             networkSnack();
                                         }
                                     });
                                     e.printStackTrace();
                                 }

                             }
                         }
                );

    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    private void networkSnack() {
        send.setVisibility(View.VISIBLE);
        findViewById(R.id.progressbar).setVisibility(View.GONE);
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.mainLay), getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }
}
