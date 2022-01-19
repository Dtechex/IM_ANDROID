package com.loopytime.im.status;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.loopytime.external.keyboard.KeyboardHeightObserver;
import com.loopytime.external.keyboard.KeyboardHeightProvider;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.StorageManager;
import com.loopytime.im.ApplicationClass;
import com.loopytime.im.BaseActivity;
import com.loopytime.im.NewGroupActivity;
import com.loopytime.im.R;
import com.loopytime.utils.Constants;

import java.util.HashMap;

public class OwnStoryActivity extends BaseActivity implements View.OnClickListener, KeyboardHeightObserver {

    private static final String TAG = OwnStoryActivity.class.getSimpleName();
    private ImageView cameraImage, galleryImage;
    private RelativeLayout mainLay, titleLay, closeLayout, bottomLay, editLay;
    private EditText editText;
    private ImageView send;
    private String filePath, sourceType;
    private StorageManager storageManager;
    private SharedPreferences pref;
    private KeyboardHeightProvider keyboardHeightProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_own_story);
        storageManager = StorageManager.getInstance(this);
        pref = getSharedPreferences("SavedPref", MODE_PRIVATE);
        keyboardHeightProvider = new KeyboardHeightProvider(this);
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
        if (ApplicationClass.hasNavigationBar()) {
            Log.i(TAG, "initBottomPadding: " + bottomMargin);
            initBottomPadding(ApplicationClass.dpToPx(OwnStoryActivity.this, bottomMargin + 5));
        } else {
            initBottomPadding(ApplicationClass.dpToPx(OwnStoryActivity.this, 10));
        }
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
        ApplicationClass.showSnack(OwnStoryActivity.this, findViewById(R.id.mainLay), isConnected);
    }

    @Override
    public void onKeyboardHeightChanged(int height, int orientation) {
        // color the keyboard height view, this will remain visible when you close the keyboard
        if (height > 0) {
            if (ApplicationClass.hasNavigationBar()) {
                int bottomLayHeight = editLay.getHeight();
                initBottomPadding(height + bottomLayHeight + 10);
            } else {
                initBottomPadding(height);
            }
        } else {
            if (ApplicationClass.hasNavigationBar()) {
                initBottomPadding(pref.getInt(Constants.TAG_NAV_HEIGHT, 0) + ApplicationClass.dpToPx(OwnStoryActivity.this, 10));
            } else {
                initBottomPadding(ApplicationClass.dpToPx(OwnStoryActivity.this, 10));
            }
        }
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
                    ApplicationClass.showSnack(OwnStoryActivity.this, findViewById(R.id.mainLay), false);
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

}
