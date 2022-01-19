package com.loopytime.im.status;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.loopytime.external.videotrimmer.utils.FileUtils;
import com.loopytime.helper.StorageManager;
import com.loopytime.im.ApplicationClass;
import com.loopytime.im.BaseActivity;
import com.loopytime.im.R;
import com.loopytime.utils.Constants;
import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitEventCallback;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;
import droidninja.filepicker.utils.Orientation;

public class CameraKitActivity extends BaseActivity {

    private static final String TAG = CameraKitActivity.class.getSimpleName();
    private FrameLayout mainLay;
    private ConstraintLayout bottomLay;
    private LinearLayout timerLay;
    private CameraView cameraKitView;
    private AppCompatCheckBox btnFlash, btnSwitchCamera;
    private ImageView btnGallery, btnImage, btnVideo, btnCapture, btnStatus;
    private Button permissionsButton;
    private TextView txtRecordTimer, txtTapHold;

    private String statusType = Constants.TAG_IMAGE;
    private boolean isVideoStarted = false;
    private int cameraMethod = CameraKit.Constants.METHOD_STANDARD, flashMode = 0;
    private boolean cropOutput = false;
    private int deviceLevel;
    private CountDownTimer longClickTimer;
    private long clickTime, STATUS_DURATION = 30 * 1000;
    private SharedPreferences pref;
    private Handler timerHandler = new Handler();
    int recordStartSec;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            txtRecordTimer.setText(getTime(recordStartSec));
            recordStartSec = recordStartSec + 1000;
            timerHandler.postDelayed(this, 1000);
        }
    };
    private View.OnTouchListener cameraTouchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_kit);
        Window w = getWindow(); // in Activity's onCreate() for instance
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        pref = getSharedPreferences("SavedPref", MODE_PRIVATE);

        mainLay = findViewById(R.id.main_lay);
        cameraKitView = findViewById(R.id.camera_kit_view);
        btnFlash = findViewById(R.id.btn_flash);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        bottomLay = findViewById(R.id.bottom_lay);
        btnGallery = findViewById(R.id.btn_gallery);
        btnImage = findViewById(R.id.btn_image);
        btnVideo = findViewById(R.id.btn_video);
        btnCapture = findViewById(R.id.btn_capture);
        btnStatus = findViewById(R.id.btn_status);
        permissionsButton = findViewById(R.id.permissionsButton);
        timerLay = findViewById(R.id.timerLay);
        txtRecordTimer = findViewById(R.id.txt_record_timer);
        txtTapHold = findViewById(R.id.txt_tap_hold);

        cameraKitView.setMethod(cameraMethod);
        cameraKitView.setCropOutput(cropOutput);
        initListeners();
    }

    @Override
    public void onNetworkChange(boolean isConnected) {
        ApplicationClass.showSnack(CameraKitActivity.this, findViewById(R.id.main_lay), isConnected);
    }

    private void initListeners() {

        cameraKitView.setFacingChangeListener(new CameraView.OnFacingChangedListener() {
            @Override
            public void onFacingChanged() {
                btnSwitchCamera.performClick();
            }
        });

        btnSwitchCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    cameraKitView.setFacing(CameraKit.Constants.FACING_FRONT);
                } else {
                    cameraKitView.setFacing(CameraKit.Constants.FACING_BACK);
                }

                Animation scaling = AnimationUtils.loadAnimation(CameraKitActivity.this, R.anim.anim_spin);
                btnSwitchCamera.startAnimation(scaling);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (cameraKitView.isFacingBack()) {
                            btnFlash.setVisibility(View.VISIBLE);
                        } else {
                            btnFlash.setChecked(false);
                            btnFlash.setVisibility(View.INVISIBLE);
                        }
                    }
                }, 500);
            }
        });

        btnFlash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        flashMode = CameraKit.Constants.FLASH_ON;
                        cameraKitView.setFlash(flashMode);
                    } else if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) {
                        flashMode = CameraKit.Constants.FLASH_ON;
                        cameraKitView.setFlash(flashMode);
                    } else if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
                        flashMode = CameraKit.Constants.FLASH_ON;
                        cameraKitView.setFlash(flashMode);
                    } else if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) {
                        if (cameraKitView.isFacingFront()) {
                            flashMode = CameraKit.Constants.FLASH_TORCH;
                        } else {
                            flashMode = CameraKit.Constants.FLASH_ON;
                        }
                        cameraKitView.setFlash(flashMode);
                    }
                } else {
                    if (cameraKitView.getFlash() != CameraKit.Constants.FLASH_OFF) {
                        flashMode = CameraKit.Constants.FLASH_OFF;
                        cameraKitView.setFlash(flashMode);
                    }
                }
                Animation scaling = AnimationUtils.loadAnimation(CameraKitActivity.this, R.anim.anim_spin);
                btnFlash.startAnimation(scaling);
            }
        });

        permissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraKitView.requestPermissions(true, true);
            }
        });

        cameraTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        setClickCountDown();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        longClickTimer.cancel();
                        if (isVideoStarted) {
                            timerHandler.removeCallbacks(timerRunnable);
                            stopRecorder();
                            resetCaptureButton();
                        } else {
                            btnCapture.setOnTouchListener(null);
                            cameraKitView.setToggleFacing(false);
                            captureImage();
                        }
                        clickTime = 0;
                        return true;
                }
                return false;
            }
        };

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilePickerBuilder.getInstance()
                        .setMaxCount(1)
                        .setActivityTheme(R.style.MainTheme)
                        .setActivityTitle(getString(R.string.please_select_media))
                        .enableVideoPicker(true)
                        .enableImagePicker(true)
                        .enableCameraSupport(false)
                        .showGifs(false)
                        .showFolderView(false)
                        .enableSelectAll(false)
                        .withOrientation(Orientation.UNSPECIFIED)
                        .pickPhoto(CameraKitActivity.this, 150);
            }
        });

        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                statusType = Constants.TAG_IMAGE;
                btnCapture.setImageDrawable(ContextCompat.getDrawable(CameraKitActivity.this, R.drawable.camera_normal));
                btnVideo.setImageDrawable(ContextCompat.getDrawable(CameraKitActivity.this, R.drawable.video));
                btnImage.setImageDrawable(ContextCompat.getDrawable(CameraKitActivity.this, R.drawable.camera_selected));
            }
        });

        btnVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                statusType = Constants.TAG_VIDEO;
                btnCapture.setImageDrawable(ContextCompat.getDrawable(CameraKitActivity.this, R.drawable.camera_recorder));
                btnVideo.setImageDrawable(ContextCompat.getDrawable(CameraKitActivity.this, R.drawable.video_selected));
                btnImage.setImageDrawable(ContextCompat.getDrawable(CameraKitActivity.this, R.drawable.camera_unselected));
            }
        });

        btnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CameraKitActivity.this, TextStatus.class);
                intent.putExtra(Constants.IS_POST, getIntent().getBooleanExtra(Constants.IS_POST,false));
                startActivityForResult(intent, Constants.STATUS_TEXT_CODE);
            }
        });
    }

    private void captureImage() {
//        ApplicationClass.preventMultiClick(btnCapture);
        resetCaptureButton();
        statusType = Constants.TAG_IMAGE;
        cameraKitView.captureImage(new CameraKitEventCallback<CameraKitImage>() {
            @Override
            public void callback(CameraKitImage image) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (btnFlash.isChecked()) {
                            flashMode = CameraKit.Constants.FLASH_ON;
                        } else {
                            flashMode = CameraKit.Constants.FLASH_OFF;
                        }
                        cameraKitView.setFlash(flashMode);
                        timerLay.setVisibility(View.GONE);
//                                final Jpeg jpeg = new Jpeg(photo);
                        String fileName = Constants.FOLDER + "_" + System.currentTimeMillis() + ".JPG";
                        StorageManager storageManager = StorageManager.getInstance(CameraKitActivity.this);

                        try {
                            File mDestDir = storageManager.getExtFilesDir();
                            if (mDestDir != null) {
                                File mDestFile = new File(mDestDir.getPath() + File.separator + fileName);
                                try {
                                    FileOutputStream out = new FileOutputStream(mDestFile);
                                    image.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, out);
                                    out.flush();
                                    out.close();
                                    setActivity(mDestFile.getAbsolutePath(), Constants.TAG_CAMERA);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        btnCapture.setOnTouchListener(cameraTouchListener);
                    }
                }).start();

            }

            @Override
            public void onRecordStarted() {

            }

            @Override
            public void onRecordError() {
                btnCapture.setOnTouchListener(cameraTouchListener);
            }
        });
    }

    private void captureVideo() {
        cameraKitView.captureVideo(CameraKitActivity.this, new CameraKitEventCallback<CameraKitVideo>() {
            @Override
            public void callback(CameraKitVideo video) {
                btnCapture.setOnTouchListener(null);
                if (btnFlash.isChecked()) {
                    flashMode = CameraKit.Constants.FLASH_ON;
                    cameraKitView.setFlash(flashMode);
                } else {
                    flashMode = CameraKit.Constants.FLASH_OFF;
                    cameraKitView.setFlash(flashMode);
                }
                timerHandler.removeCallbacks(timerRunnable);
                resetCaptureButton();
                setViewVisibility(btnFlash, View.VISIBLE);
                setViewVisibility(btnSwitchCamera, View.VISIBLE);
                setViewVisibility(btnGallery, View.VISIBLE);
                setViewVisibility(btnStatus, View.VISIBLE);
                setViewVisibility(timerLay, View.GONE);
                String filePath = video.getVideoFile().getAbsolutePath();
                setActivity(filePath, Constants.TAG_CAMERA);
            }

            @Override
            public void onRecordStarted() {
                setViewVisibility(txtTapHold, View.GONE);
                setViewVisibility(btnGallery, View.GONE);
                setViewVisibility(btnStatus, View.GONE);
                setViewVisibility(btnFlash, View.GONE);
                setViewVisibility(btnSwitchCamera, View.GONE);
                if (btnFlash.isChecked()) {
                    flashMode = CameraKit.Constants.FLASH_TORCH;
                    cameraKitView.setFlash(flashMode);
                }
                startVideoRecordingTimer();
            }

            @Override
            public void onRecordError() {
                btnCapture.setOnTouchListener(cameraTouchListener);
                btnSwitchCamera.setVisibility(View.VISIBLE);
                if (cameraKitView.isFacingBack()) {
                    btnFlash.setVisibility(View.VISIBLE);
                } else {
                    btnFlash.setChecked(false);
                    btnFlash.setVisibility(View.INVISIBLE);
                }
                if (btnFlash.isChecked()) {
                    flashMode = CameraKit.Constants.FLASH_ON;
                    cameraKitView.setFlash(flashMode);
                } else {
                    flashMode = CameraKit.Constants.FLASH_OFF;
                    cameraKitView.setFlash(flashMode);
                }
                timerLay.setVisibility(View.GONE);
            }
        }, (int) STATUS_DURATION);
    }

    private void setClickCountDown() {
        Animation anim = new ScaleAnimation(
                1f, 1.2f, // Start and end values for the X axis scaling
                1f, 1.2f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        anim.setDuration(400);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                btnCapture.setImageDrawable(ContextCompat.getDrawable(CameraKitActivity.this, R.drawable.camera_recorder));
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        btnCapture.startAnimation(anim);

        longClickTimer = new CountDownTimer(STATUS_DURATION, 500) {
            public void onTick(long millisUntilFinished) {
                clickTime = STATUS_DURATION - millisUntilFinished;
                if (clickTime > 500 && !isVideoStarted) {
                    isVideoStarted = true;
                    statusType = Constants.TAG_VIDEO;
                    captureVideo();
                }
            }

            public void onFinish() {

            }
        };
        longClickTimer.start();
    }

    private void resetCaptureButton() {
        btnCapture.animate().cancel();
        Animation anim = new ScaleAnimation(
                1.2f, 1f, // Start and end values for the X axis scaling
                1.2f, 1f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        anim.setDuration(400);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                btnCapture.setImageDrawable(ContextCompat.getDrawable(CameraKitActivity.this, R.drawable.camera_normal));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        btnCapture.startAnimation(anim);
    }

    // function setup countdown timerHandler for video recording
    private void startVideoRecordingTimer() {
        recordStartSec = 0;
        timerLay.setVisibility(View.VISIBLE);
        timerRunnable.run();
    }

    private void stopRecorder() {
        isVideoStarted = false;
        txtTapHold.setVisibility(View.VISIBLE);
        cameraKitView.stopVideo();
        if (btnFlash.isChecked()) {
            flashMode = CameraKit.Constants.FLASH_ON;
            cameraKitView.setFlash(flashMode);
        } else {
            flashMode = CameraKit.Constants.FLASH_OFF;
            cameraKitView.setFlash(flashMode);
        }
    }

    private void setViewVisibility(View view, int visible) {
        view.setVisibility(visible);
    }

    private void setActivity(String filePath, String sourceFrom) {
        Intent intent;
        if (statusType == null || statusType.equals(Constants.TAG_IMAGE)) {
            intent = new Intent(CameraKitActivity.this, StatusPreviewActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra(Constants.TAG_FROM, sourceFrom);
            intent.putExtra(Constants.TAG_ATTACHMENT, filePath);
            intent.putExtra(Constants.IS_POST, getIntent().getBooleanExtra(Constants.IS_POST,false));
            startActivityForResult(intent, Constants.STATUS_IMAGE_CODE);
        } else {
            Uri uri = Uri.fromFile(new File(filePath));
            intent = new Intent(this, TrimmerActivity.class);
            intent.putExtra(Constants.IS_POST, getIntent().getBooleanExtra(Constants.IS_POST,false));
            intent.putExtra(Constants.TAG_ATTACHMENT, FileUtils.getPath(this, uri));
            intent.putExtra(Constants.TAG_FROM, sourceFrom);
            intent.putExtra(Constants.TAG_STORY_TIME, getMediaDuration(uri));
            startActivityForResult(intent, Constants.STATUS_VIDEO_CODE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        btnCapture.setOnTouchListener(cameraTouchListener);
        statusType = Constants.TAG_IMAGE;
        initMargins();
        cameraKitView.start();
        cameraKitView.setToggleFacing(true);
        checkDeviceLevel();
    }

    private void initMargins() {
        if (ApplicationClass.hasNavigationBar()) {
            int bottomMargin = pref.getInt(Constants.TAG_NAV_HEIGHT, 0);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.bottomMargin = bottomMargin;
            bottomLay.setLayoutParams(params);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        cameraKitView.stop();
        super.onDestroy();
    }

    private void checkDeviceLevel() {
        if (cameraKitView.isStarted()) {
            showLevelSupported();
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkDeviceLevel();
                }
            }, 500);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == 150) {
            ArrayList<String> pathsAry = new ArrayList<String>();
            if (data != null) {
                pathsAry.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA));
            }
            if (pathsAry.size() > 0) {
                String filepath = pathsAry.get(0);
                if (ApplicationClass.isVideoFile(filepath)) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(CameraKitActivity.this, Uri.fromFile(new File(filepath)));
                    String videoFormat = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                    Log.i(TAG, "onActivityResult: " + videoFormat);
                    if (!videoFormat.contains("webm")) {
                        statusType = Constants.TAG_VIDEO;
                        previewMedia(filepath, statusType);
                    } else {
                        ApplicationClass.showToast(CameraKitActivity.this, getString(R.string.video_format_not_supported), Toast.LENGTH_SHORT);
                    }
                } else {
                    statusType = Constants.TAG_IMAGE;
                    previewMedia(filepath, statusType);
                }
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == Constants.STATUS_TEXT_CODE) {
            finish();
        } else if (resultCode == Activity.RESULT_OK && requestCode == Constants.STATUS_VIDEO_CODE) {
            finish();
        } else if (resultCode == Activity.RESULT_OK && requestCode == Constants.STATUS_IMAGE_CODE) {
            finish();
        }
    }

    private void previewMedia(String filePath, String type) {
        if (type.equals(Constants.TAG_VIDEO)) {
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.MINI_KIND);
            if (thumb != null) {
                String fileName = (System.currentTimeMillis() / 1000L) + ".JPG";
                StorageManager storageManager = StorageManager.getInstance(this);
                String imageStatus = storageManager.saveToSdCard(thumb, Constants.TAG_SENT, fileName);
                if (("" + imageStatus).equals(Constants.TAG_SUCCESS)) {
                    setActivity(filePath, Constants.TAG_GALLERY);
                }
            }
        } else {
            setActivity(filePath, Constants.TAG_GALLERY);
        }
    }

    private int getMediaDuration(Uri uriOfFile) {
        MediaPlayer mp = MediaPlayer.create(this, uriOfFile);
        return mp.getDuration();
    }

    private String getTime(long time) {
        int sec = (int) (time / 1000);
        String seconds;
        if (sec >= 10) {
            seconds = "00:" + sec;
        } else {
            seconds = "00:0" + sec;
        }
        Log.i(TAG, "getTime: " + seconds);
        return seconds;
    }

    private void showLevelSupported() {
        CameraManager manager = null;
        CameraCharacteristics chars = null;
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            chars = manager.getCameraCharacteristics("" + cameraKitView.getFacing());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        deviceLevel = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            Log.i(TAG, "Level supported: legacy");
        } else if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) {
            Log.i(TAG, "Level supported: level 3");
        } else if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
            Log.i(TAG, "Level supported: full");
        } else if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) {
            Log.i(TAG, "Level supported: limited");
        }
    }

}
