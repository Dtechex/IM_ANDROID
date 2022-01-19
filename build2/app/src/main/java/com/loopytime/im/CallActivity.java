package com.loopytime.im;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.loopytime.apprtc.AppRTCAudioManager;
import com.loopytime.apprtc.AppRTCClient;
import com.loopytime.apprtc.DirectRTCClient;
import com.loopytime.apprtc.PeerConnectionClient;
import com.loopytime.apprtc.UnhandledExceptionHandler;
import com.loopytime.apprtc.WebSocketRTCClient;
import com.loopytime.external.RandomString;
import com.loopytime.helper.CallNotificationService;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.model.CallData;
import com.loopytime.model.ContactsData;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.FileVideoCapturer;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WAKE_LOCK;

public class CallActivity extends BaseActivity implements PeerConnectionClient.PeerConnectionEvents,
        SensorEventListener, CallControlsFragment.OnCallEvents, AppRTCClient.SignalingEvents {
    private static final String TAG = CallActivity.class.getSimpleName();

    public static final String EXTRA_ROOMID = "org.appspot.apprtc.ROOMID";
    public static final String EXTRA_URLPARAMETERS = "org.appspot.apprtc.URLPARAMETERS";
    public static final String EXTRA_LOOPBACK = "org.appspot.apprtc.LOOPBACK";
    public static final String EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL";
    public static final String EXTRA_SCREENCAPTURE = "org.appspot.apprtc.SCREENCAPTURE";
    public static final String EXTRA_CAMERA2 = "org.appspot.apprtc.CAMERA2";
    public static final String EXTRA_VIDEO_WIDTH = "org.appspot.apprtc.VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "org.appspot.apprtc.VIDEO_HEIGHT";
    public static final String EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS";
    public static final String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED =
            "org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER";
    public static final String EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE";
    public static final String EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC";
    public static final String EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC";
    public static final String EXTRA_CAPTURETOTEXTURE_ENABLED = "org.appspot.apprtc.CAPTURETOTEXTURE";
    public static final String EXTRA_FLEXFEC_ENABLED = "org.appspot.apprtc.FLEXFEC";
    public static final String EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE";
    public static final String EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC";
    public static final String EXTRA_NOAUDIOPROCESSING_ENABLED =
            "org.appspot.apprtc.NOAUDIOPROCESSING";
    public static final String EXTRA_AECDUMP_ENABLED = "org.appspot.apprtc.AECDUMP";
    public static final String EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED =
            "org.appspot.apprtc.SAVE_INPUT_AUDIO_TO_FILE";
    public static final String EXTRA_OPENSLES_ENABLED = "org.appspot.apprtc.OPENSLES";
    public static final String EXTRA_DISABLE_BUILT_IN_AEC = "org.appspot.apprtc.DISABLE_BUILT_IN_AEC";
    public static final String EXTRA_DISABLE_BUILT_IN_AGC = "org.appspot.apprtc.DISABLE_BUILT_IN_AGC";
    public static final String EXTRA_DISABLE_BUILT_IN_NS = "org.appspot.apprtc.DISABLE_BUILT_IN_NS";
    public static final String EXTRA_DISABLE_WEBRTC_AGC_AND_HPF =
            "org.appspot.apprtc.DISABLE_WEBRTC_GAIN_CONTROL";
    public static final String EXTRA_DISPLAY_HUD = "org.appspot.apprtc.DISPLAY_HUD";
    public static final String EXTRA_TRACING = "org.appspot.apprtc.TRACING";
    public static final String EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE";
    public static final String EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME";
    public static final String EXTRA_VIDEO_FILE_AS_CAMERA = "org.appspot.apprtc.VIDEO_FILE_AS_CAMERA";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_WIDTH";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT";
    public static final String EXTRA_USE_VALUES_FROM_INTENT =
            "org.appspot.apprtc.USE_VALUES_FROM_INTENT";
    public static final String EXTRA_DATA_CHANNEL_ENABLED = "org.appspot.apprtc.DATA_CHANNEL_ENABLED";
    public static final String EXTRA_ORDERED = "org.appspot.apprtc.ORDERED";
    public static final String EXTRA_MAX_RETRANSMITS_MS = "org.appspot.apprtc.MAX_RETRANSMITS_MS";
    public static final String EXTRA_MAX_RETRANSMITS = "org.appspot.apprtc.MAX_RETRANSMITS";
    public static final String EXTRA_PROTOCOL = "org.appspot.apprtc.PROTOCOL";
    public static final String EXTRA_NEGOTIATED = "org.appspot.apprtc.NEGOTIATED";
    public static final String EXTRA_ID = "org.appspot.apprtc.ID";
    public static final String EXTRA_ENABLE_RTCEVENTLOG = "org.appspot.apprtc.ENABLE_RTCEVENTLOG";

    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;

    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

    // Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;
    private Uri roomUri;

    private static class ProxyVideoSink implements VideoSink {
        private VideoSink target;

        @Override
        synchronized public void onFrame(VideoFrame frame) {
            if (target == null) {
                Logging.d(TAG, "Dropping frame in proxy because target is null.");
                return;
            }

            target.onFrame(frame);
        }

        synchronized public void setTarget(VideoSink target) {
            this.target = target;
        }
    }

    private final ProxyVideoSink remoteProxyRenderer = new ProxyVideoSink();
    private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
    @Nullable
    private com.loopytime.apprtc.PeerConnectionClient peerConnectionClient;
    @Nullable
    private AppRTCClient appRtcClient;
    @Nullable
    private AppRTCClient.SignalingParameters signalingParameters;
    @Nullable
    private AppRTCAudioManager audioManager;
    @Nullable
    private SurfaceViewRenderer pipRenderer;
    @Nullable
    private SurfaceViewRenderer fullscreenRenderer;
    @Nullable
    private VideoFileRenderer videoFileRenderer;
    private final List<VideoSink> remoteSinks = new ArrayList<>();
    private Toast logToast;
    private boolean commandLineRun;
    private boolean activityRunning;
    private AppRTCClient.RoomConnectionParameters roomConnectionParameters;
    @Nullable
    private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    private FrameLayout videoCallView;
    private long callStartedTimeMs;
    private static Intent mediaProjectionPermissionResultData;
    private static int mediaProjectionPermissionResultCode;
    public boolean connected, callControlFragmentVisible = true, isError, micEnabled = true, screencaptureEnabled;
    // True if local view is in the fullscreen renderer.
    private boolean isSwappedFeeds;
    // Controls
    private CallControlsFragment controlsFragment;
    public static boolean isInCall, callPause = false;
    public static String type = "", chatid = "", name = "", imgUrl = "", from = "",
            toastText = "", userid = "", callid = "", platform = "";
    ImageView userImage, bgImg;
    TextView userName, callType, callTime;
    Display display;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    boolean isChannelReady = false;
    DatabaseHandler dbhelper;
    ContactsData.Result results;
    SocketConnection socketConnection;
    Vibrator vibrator;
    Ringtone ringtone;
    CountDownTimer countDownTimer, callEmitTimer;
    Timer callTimer = new Timer();
    Animation downAnimation, upAnimation;
    ToneGenerator toneGenerator;
    public boolean speaker = false, isCallPause = false;
    public static boolean isCallAttend = false, isCallConnected = false;
    private Dialog permissionDialog;
    public static CallActivity callActivity;
    public boolean isPermissionError = false;
    boolean isScreenOn;
    private PowerManager powerManager;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private static final String CPU_WAKELOCK = "MyApp::CPUWakelockTag";
    PowerManager.WakeLock wakeLock;
    Intent intent;
    private boolean isPeerReceived = false;
    private boolean isFrontCamera = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(this));
        setContentView(R.layout.activity_call);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "8");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Call Activity ");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Activity");
        ApplicationClass.getInstance().mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        connected = false;
        signalingParameters = null;

        pref = getApplicationContext().getSharedPreferences("ChatPref",
                MODE_PRIVATE);
        editor = pref.edit();
        isInCall = true;
        callActivity = this;
        dbhelper = DatabaseHandler.getInstance(this);
        socketConnection = SocketConnection.getInstance(this);
        intent = getIntent();

        roomUri = intent.getData();

        if (roomUri == null) {
            Log.e(TAG, "Didn't get any URL in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        type = intent.getExtras().getString(Constants.TAG_TYPE);
        from = intent.getExtras().getString(Constants.TAG_FROM);
        userid = intent.getExtras().getString(Constants.TAG_USER_ID);
        platform = intent.getExtras().getString(Constants.TAG_PLATFORM);
        if (from.equals("receive") && platform.equals("ios") && type.equals(Constants.TAG_VIDEO)) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("user_id", userid);
                jsonObject.put("call_type", Constants.TAG_PLATFORM);
                jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CALL);
                jsonObject.put(Constants.TAG_PLATFORM, "android");
                socketConnection.createCall(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (from == null) {
            finish();
        }
        results = dbhelper.getContactDetail(userid);

        initPowerOptions();
        initView();

        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(CallActivity.this, new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 101);
        } else {
            initRender();
        }
    }

    @TargetApi(17)
    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =
                (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics;
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    private void initView() {
        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(CallActivity.this);
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        isScreenOn = powerManager.isScreenOn();

        userImage = findViewById(R.id.userImage);
        userName = findViewById(R.id.userName);
        callType = findViewById(R.id.callType);
        pipRenderer = findViewById(R.id.local_video_view);
        fullscreenRenderer = findViewById(R.id.remote_video_view);
        videoCallView = findViewById(R.id.videoCallView);
        callTime = findViewById(R.id.callTime);
        bgImg = findViewById(R.id.bgImg);
        controlsFragment = new CallControlsFragment();

        upAnimation = AnimationUtils.loadAnimation(CallActivity.this, R.anim.slide_up);
        downAnimation = AnimationUtils.loadAnimation(CallActivity.this, R.anim.slide_down);

        // Show/hide call control fragment on view click.
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCallControlFragmentVisibility();
            }
        };


        // Swap feeds on pip view click.
        pipRenderer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                setSwappedFeeds(!isSwappedFeeds);
            }
        });

        fullscreenRenderer.setOnClickListener(listener);
        remoteSinks.add(remoteProxyRenderer);

        if (type.equals(Constants.TAG_VIDEO)) {
            userName.setVisibility(View.GONE);
        }

        if (from.equals("receive")) {
            try {
                chatid = intent.getExtras().getString("room_id");
                userName.setText(results.user_name);
                imgUrl = results.user_image;
            } catch (Exception e) {
                e.printStackTrace();
            }

            callid = intent.getExtras().getString("call_id");

            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (alert == null) {
                // alert is null, using backup
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                // I can't see this ever being null (as always have a default notification)
                // but just incase
                if (alert == null) {
                    // alert backup is null, using 2nd backup
                    alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL);
                }
            }
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            long[] pattern = {0, 100, 1000, 300, 200, 100, 500, 200, 100};
            switch (audio.getRingerMode()) {
                case AudioManager.RINGER_MODE_NORMAL:
                    ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
                    if (!ringtone.isPlaying())
                        ringtone.play();
                    vibrator.vibrate(pattern, 0);
                    break;
                case AudioManager.RINGER_MODE_SILENT:
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    vibrator.vibrate(pattern, 0);
                    break;
            }
            turnOnScreen();
        } else {
            chatid = GetSet.getUserId() + System.currentTimeMillis();
            userName.setText(results.user_name);
            imgUrl = results.user_image;
            //  Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            //  declineCall.startAnimation(shake);

            toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, 30000);
        }

        if (type.equals("audio")) {
            // String img = Constants.RESIZE_URL + CommonFunctions.getImageName(data.get(Constants.TAG_USERIMAGE)) + Constants.IMAGE_RES;

            Glide.with(CallActivity.this).load(R.drawable.call_bg)
                    .apply(RequestOptions.centerCropTransform().placeholder(R.drawable.call_bg).error(R.drawable.call_bg))
                    .into(bgImg);

            Glide.with(CallActivity.this).load(Constants.USER_IMG_PATH + imgUrl)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                    .into(userImage);
            callType.setText(results.user_name + " " + getString(R.string.audio_calling));
        } else if (type.equals("video")) {
            callType.setText(results.user_name + " " + getString(R.string.video_calling));
            userImage.setVisibility(View.GONE);
            if (audioManager.hasWiredHeadset()) {
                audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.WIRED_HEADSET);
                speaker = false;
            } else {
                audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
                speaker = true;
            }
        }

    }

    private void initRender() {
        final EglBase eglBase = EglBase.create();

        // Create video renderers.
        pipRenderer.init(eglBase.getEglBaseContext(), null);
        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        String saveRemoteVideoToFile = intent.getStringExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);
        // When saveRemoteVideoToFile is set we save the video from the remote to a file.
        if (saveRemoteVideoToFile != null) {
            int videoOutWidth = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
            int videoOutHeight = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
            try {
                videoFileRenderer = new VideoFileRenderer(
                        saveRemoteVideoToFile, videoOutWidth, videoOutHeight, eglBase.getEglBaseContext());
                remoteSinks.add(videoFileRenderer);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to open video file for output: " + saveRemoteVideoToFile, e);
            }
        }
        fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        pipRenderer.setZOrderMediaOverlay(true);
        pipRenderer.setEnableHardwareScaler(true /* enabled */);
        fullscreenRenderer.setEnableHardwareScaler(false /* enabled */);
        // Start with local feed in fullscreen and swap it to the pip when the call is connected.
        /*fullscreenRenderer.setMirror(true);
        pipRenderer.setMirror(false);*/
        setSwappedFeeds(true /* isSwappedFeeds */);

        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        boolean loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false);
        boolean tracing = intent.getBooleanExtra(EXTRA_TRACING, false);

        int videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0);
        int videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0);

        screencaptureEnabled = intent.getBooleanExtra(EXTRA_SCREENCAPTURE, false);
        // If capturing format is not specified for screencapture, use screen resolution.
        if (screencaptureEnabled && videoWidth == 0 && videoHeight == 0) {
            DisplayMetrics displayMetrics = getDisplayMetrics();
            videoWidth = displayMetrics.widthPixels;
            videoHeight = displayMetrics.heightPixels;
        }

        startTimer();

        PeerConnectionClient.DataChannelParameters dataChannelParameters = null;
        if (intent.getBooleanExtra(EXTRA_DATA_CHANNEL_ENABLED, false)) {
            dataChannelParameters = new PeerConnectionClient.DataChannelParameters(intent.getBooleanExtra(EXTRA_ORDERED, true),
                    intent.getIntExtra(EXTRA_MAX_RETRANSMITS_MS, -1),
                    intent.getIntExtra(EXTRA_MAX_RETRANSMITS, -1), intent.getStringExtra(EXTRA_PROTOCOL),
                    intent.getBooleanExtra(EXTRA_NEGOTIATED, false), intent.getIntExtra(EXTRA_ID, -1));
        }
        peerConnectionParameters =
                new PeerConnectionClient.PeerConnectionParameters(intent.getBooleanExtra(EXTRA_VIDEO_CALL, false), loopback,
                        tracing, videoWidth, videoHeight, intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
                        intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0), intent.getStringExtra(EXTRA_VIDEOCODEC),
                        intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
                        intent.getBooleanExtra(EXTRA_FLEXFEC_ENABLED, false),
                        intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0), intent.getStringExtra(EXTRA_AUDIOCODEC),
                        intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_AECDUMP_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AEC, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AGC, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_NS, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false),
                        intent.getBooleanExtra(EXTRA_ENABLE_RTCEVENTLOG, false), dataChannelParameters);
        commandLineRun = intent.getBooleanExtra(EXTRA_CMDLINE, false);
        int runTimeMs = intent.getIntExtra(EXTRA_RUNTIME, 0);

        Log.d(TAG, "VIDEO_FILE: '" + intent.getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA) + "'");

        // Create connection client. Use DirectRTCClient if room name is an IP otherwise use the
        // standard WebSocketRTCClient.
        if (loopback || !DirectRTCClient.IP_PATTERN.matcher(chatid).matches()) {
            appRtcClient = new WebSocketRTCClient(this);
        } else {
            Log.i(TAG, "Using DirectRTCClient because room name looks like an IP.");
            appRtcClient = new DirectRTCClient(this);
        }
        // Create connection parameters.
        String urlParameters = intent.getStringExtra(EXTRA_URLPARAMETERS);
        roomConnectionParameters =
                new AppRTCClient.RoomConnectionParameters(roomUri.toString(), chatid, loopback, urlParameters);
        // Send intent arguments to fragments.
        controlsFragment.setContext(this);
        Bundle controlBundle = new Bundle();
        controlBundle.putString(Constants.TAG_TYPE, type);
        controlBundle.putString(Constants.TAG_FROM, from);
        controlsFragment.setArguments(controlBundle);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.call_fragment_container, controlsFragment);
        ft.commit();

        // For command line execution run connection for <runTimeMs> and exit.
        if (commandLineRun && runTimeMs > 0) {
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            }, runTimeMs);
        }

        // Create peer connection client.
        peerConnectionClient = new PeerConnectionClient(
                ApplicationClass.getInstance().getApplicationContext(), eglBase, peerConnectionParameters, CallActivity.this);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        if (loopback) {
            options.networkIgnoreMask = 0;
        }
        peerConnectionClient.createPeerConnectionFactory(options);

        if (screencaptureEnabled) {
            startScreenCapture();
        } else {
            startCall();
        }
    }

    public void startTimer() {
        if (from.equals("send")) {
            try {
                RandomString randomString = new RandomString(10);
                String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                callid = GetSet.getUserId() + userid + randomString.nextString();

                try {
                    if (!results.blockedme.equals("block")) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("call_id", callid);
                        jsonObject.put("user_id", userid);
                        jsonObject.put("caller_id", GetSet.getUserId());
                        jsonObject.put("sender_id", GetSet.getUserId());
                        jsonObject.put("type", type);
                        jsonObject.put("call_status", "outgoing");
                        jsonObject.put("created_at", unixStamp);
                        jsonObject.put("call_type", "created");
                        jsonObject.put("room_id", chatid);
                        jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CALL);
                        jsonObject.put(Constants.TAG_PLATFORM, "android");
                        socketConnection.createCall(jsonObject);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                callEmitTimer = new CountDownTimer(30000, 1000) {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (connected) {
                            cancelCallEmitTimer();
                        }
                    }

                    @Override
                    public void onFinish() {
                        Log.v(TAG, "callEmitTimer=Ended");
                        if (callEmitTimer != null) {
                            cancelCallEmitTimer();
                            stopAudioManager();
                            try {
                                if (!results.blockedme.equals("block")) {
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("call_id", callid);
                                    jsonObject.put("user_id", userid);
                                    jsonObject.put("caller_id", GetSet.getUserId());
                                    jsonObject.put("sender_id", GetSet.getUserId());
                                    jsonObject.put("type", type);
                                    jsonObject.put("call_status", "outgoing");
                                    jsonObject.put("created_at", unixStamp);
                                    jsonObject.put("call_type", "ended");
                                    jsonObject.put("room_id", chatid);
                                    jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CALL);
                                    jsonObject.put(Constants.TAG_PLATFORM, "android");
                                    socketConnection.createCall(jsonObject);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Log.v(TAG, "callEmitTimer=cancel");
                            finish();
                        }
                    }
                };
                callEmitTimer.start();

                dbhelper.addRecentCall(callid, userid, type, "outgoing", unixStamp, "1");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void cancelCallEmitTimer() {
        if (callEmitTimer != null) {
            callEmitTimer.cancel();
            callEmitTimer = null;
        }
    }

    @TargetApi(21)
    private void startScreenCapture() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;
        mediaProjectionPermissionResultCode = resultCode;
        mediaProjectionPermissionResultData = data;
        startCall();
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this) && intent.getBooleanExtra(EXTRA_CAMERA2, true);
    }

    private boolean captureToTexture() {
        return intent.getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false);
    }

    private Handler answerHandler = new Handler();
    int delay = 1000; //milliseconds
    Runnable answerRunnable = new Runnable() {

        @Override
        public void run() {
            onCallAccept();
        }
    };

    private void onCallAccept() {
        if (signalingParameters != null && !signalingParameters.initiator && isPeerReceived) {
            /*Remove handler once call connected*/
            answerHandler.removeCallbacks(answerRunnable);
            // Create answer. Answer SDP will be sent to offering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createAnswer();
        } else {
            callType.setText(getString(R.string.connecting));
            answerHandler.postDelayed(answerRunnable, delay);
        }
    }

    private @Nullable
    VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private void setMirror(boolean isSwappedFeeds) {
        if (isSwappedFeeds) {
            fullscreenRenderer.setMirror(true);
            pipRenderer.setMirror(false);
        } else {
            fullscreenRenderer.setMirror(false);
            pipRenderer.setMirror(true);
        }
        /*fullscreenRenderer.setMirror(isSwappedFeeds);
        pipRenderer.setMirror(isSwappedFeeds);*/
    }

    @TargetApi(21)
    private @Nullable
    VideoCapturer createScreenCapturer() {
        if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            reportError(getString(R.string.capture_permission_description));
            return null;
        }
        return new ScreenCapturerAndroid(
                mediaProjectionPermissionResultData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                reportError(getString(R.string.capture_permission_error));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        activityRunning = true;
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null && !screencaptureEnabled) {
            peerConnectionClient.startVideoSource();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isCallPause = false;
        mSensorManager.registerListener(CallActivity.this, mProximity, SensorManager.SENSOR_DELAY_FASTEST);
        if (!NetworkReceiver.isConnected()) {
            /*toastText = getString(R.string.poor_internet_connection);
            logAndToast(toastText);
            disconnect();*/
        } else {
            if (peerConnectionClient != null && type.equals("video"))
                peerConnectionClient.startVideoSource();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel("hiddycall", 0);
            }
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel("hiddycall", 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(CallActivity.this);
            releaseWakeLock();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (isCallPause) {
            if (ringtone != null) {
                ringtone.stop();
                ringtone = null;
            }
            if (vibrator != null) {
                vibrator.cancel();
                vibrator = null;
            }
        }
        isCallPause = true;
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
    }

    // Activity interfaces
    @Override
    public void onStop() {
        super.onStop();
        activityRunning = false;
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (peerConnectionClient != null && !screencaptureEnabled) {
            peerConnectionClient.stopVideoSource();
        }
    }

    @Override
    protected void onDestroy() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        if (logToast != null) {
            logToast.cancel();
        }
        activityRunning = false;
        isInCall = false;
        userid = "";
        releaseWakeLock();
        stopAudioManager();
        disconnect();
        if (!NetworkReceiver.isConnected()) {
            toastText = getString(R.string.poor_internet_connection);
            logAndToast(toastText);
        }
        super.onDestroy();
    }


    private void initPowerOptions() {
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, CPU_WAKELOCK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            isScreenOn = powerManager.isInteractive();
        } else {
            isScreenOn = powerManager.isScreenOn();
        }
    }

    public void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                wakeLock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY);
            } else {
                wakeLock.release();
            }
        }
    }

    @Override
    public void onCallAccepted() {
        if (!isNetworkConnected().equals(NetworkUtil.NOT_CONNECT)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ringtone != null) {
                        ringtone.stop();
                        ringtone = null;
                    }
                    if (vibrator != null) {
                        vibrator.cancel();
                        vibrator = null;
                    }
                    isCallAttend = true;
                    isCallConnected = true;
                    onCallAccept();
                }
            });
        } else {
            ApplicationClass.showToast(CallActivity.this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT);
        }
    }

    // CallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        stopAudioManager();
        try {
            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
            if (!results.blockedme.equals("block")) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("call_id", callid);
                jsonObject.put("user_id", userid);
                jsonObject.put("caller_id", GetSet.getUserId());
                jsonObject.put("sender_id", GetSet.getUserId());
                jsonObject.put("type", type);
                jsonObject.put("call_status", "outgoing");
                jsonObject.put("created_at", unixStamp);
                jsonObject.put("call_type", "ended");
                jsonObject.put("room_id", chatid);
                jsonObject.put(Constants.TAG_PLATFORM, "android");
                jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CALL);
                socketConnection.createCall(jsonObject);

                if (isCallAttend) {
                    toastText = getString(R.string.call_ended);
                } else {
                    toastText = getString(R.string.call_cancelled);
                }
            }
            disconnect();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onToggleMic() {
        if (peerConnectionClient != null) {
            micEnabled = !micEnabled;
            peerConnectionClient.setAudioEnabled(micEnabled);
        }
        return micEnabled;
    }

    @Override
    public boolean onToggleSpeaker() {
        return setSpeaker(!speaker);
    }

    @Override
    public void onSwitchCamera() {
        peerConnectionClient.switchCamera();
        isFrontCamera = !isFrontCamera;
        if (!isFrontCamera) {
            fullscreenRenderer.setMirror(false);
            pipRenderer.setMirror(false);
        } else {
            setMirror(false);
        }
    }

    public boolean setSpeaker(boolean speakerOn) {
        if (speakerOn) {
            speaker = true;
            audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
        } else {
            speaker = false;
            if (audioManager.hasWiredHeadset()) {
                audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.WIRED_HEADSET);
            } else {
                audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE);
            }
        }
        return speaker;
    }

    // Helper functions.
    private void toggleCallControlFragmentVisibility() {
        if (!connected || !controlsFragment.isAdded()) {
            return;
        }
        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(controlsFragment);
        } else {
            ft.hide(controlsFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void startCall() {
        if (appRtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }
        callStartedTimeMs = System.currentTimeMillis();
        // Start room connection.
        appRtcClient.connectToRoom(roomConnectionParameters);

        if (audioManager.hasWiredHeadset()) {
            audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.WIRED_HEADSET);
            speaker = false;
        } else {
            if (type.equals(Constants.TAG_VIDEO)) {
                audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
                speaker = true;
            } else {
                audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE);
                speaker = false;
            }
        }
        if (from.equals(Constants.TAG_SEND)) {
            startAudioManager();
        }
    }

    private void showToast(String text) {
        if (getApplicationContext() != null) {
            Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (peerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
        setSwappedFeeds(false /* isSwappedFeeds */);
//        setMirror(true);
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        if (from.equals("receive")) {
            if (callid != null && !callid.equals("") && dbhelper.isCallIdExist(callid)) {
                CallData.Result result = dbhelper.getCallData(callid);
                if (isCallConnected && isCallAttend) {
                    dbhelper.addRecentCall(result.callId, result.userId, result.type, "incoming", result.createdAt, "1");
                } else {
                    dbhelper.addRecentCall(result.callId, result.userId, result.type, "missed", result.createdAt, "0");
                }
            }

        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel("hiddycall", 0);
        }
        socketConnection.close(chatid);
        SocketConnection.getInstance(this).setCallListener(null);
        remoteProxyRenderer.setTarget(null);
        localProxyVideoSink.setTarget(null);
        activityRunning = false;

        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }

        if (pipRenderer != null) {
            pipRenderer.release();
            pipRenderer = null;
        }
        if (fullscreenRenderer != null) {
            fullscreenRenderer.release();
            fullscreenRenderer = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (callTimer != null) {
            callTimer.cancel();
            callTimer = null;
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (ringtone != null) {
            ringtone.stop();
            ringtone = null;
        }
        cancelCallEmitTimer();
        if (CallFragment.callFragment != null) { // For refresh the call history
            CallFragment.callFragment.refreshAdapter();
        }
        finish();
    }

    private @Nullable
    VideoCapturer createVideoCapturer() {
        final VideoCapturer videoCapturer;
        String videoFileAsCamera = intent.getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA);
        if (videoFileAsCamera != null) {
            try {
                videoCapturer = new FileVideoCapturer(videoFileAsCamera);
            } catch (IOException e) {
                reportError(getString(R.string.camera_open_failed));
                return null;
            }
        } else if (screencaptureEnabled) {
            return createScreenCapturer();
        } else if (useCamera2()) {
            if (!captureToTexture()) {
                reportError(getString(R.string.camera2_texture_only_error));
                return null;
            }

            Logging.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            Logging.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            return null;
        }
        return videoCapturer;
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
        this.isSwappedFeeds = isSwappedFeeds;
        localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
        fullscreenRenderer.setMirror(isSwappedFeeds);
        pipRenderer.setMirror(!isSwappedFeeds);
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final AppRTCClient.SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters != null && peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        peerConnectionClient.createPeerConnection(
                localProxyVideoSink, remoteSinks, videoCapturer, signalingParameters);

        if (signalingParameters != null) {
            if (signalingParameters.initiator) {
                isChannelReady = true;
                // Create offer. Offer SDP will be sent to answering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createOffer();
            } else {
                if (params.offerSdp != null) {
                    peerConnectionClient.setRemoteDescription(params.offerSdp);
                    // Create answer. Answer SDP will be sent to offering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    isPeerReceived = true;
                    if (!from.equals(Constants.TAG_RECEIVE)) {
                        peerConnectionClient.createAnswer();
                    }
                }
                if (params.iceCandidates != null) {
                    // Add remote ICE candidates from room.
                    for (IceCandidate iceCandidate : params.iceCandidates) {
                        peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                    }
                }
            }
        }
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    if (signalingParameters.initiator) {
                        appRtcClient.sendOfferSdp(sdp);
                    } else {
                        appRtcClient.sendAnswerSdp(sdp);
                    }
                }
                if (peerConnectionParameters.videoMaxBitrate > 0) {
                    Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                    peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
                }
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    appRtcClient.sendLocalIceCandidate(candidate);
                }
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    appRtcClient.sendLocalIceCandidateRemovals(candidates);
                }
            }
        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isCallAttend = true;
            }
        });
    }

    @Override
    public void onIceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connected = false;
                disconnect();
            }
        });
    }

    @Override
    public void onConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (from.equals(Constants.TAG_RECEIVE)) {
                    startAudioManager();
                }
                connected = true;
                callType.setVisibility(View.GONE);
                if (type.equals("audio")) {
                    callTime.setVisibility(View.VISIBLE);
                    startCountDown("answer");
                } else {
                    updateVideoViews(type.equals(Constants.TAG_VIDEO));
                }
                callConnected();
            }

        });
    }

    private void startAudioManager() {
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
            // This method will be called each time the number of available audio
            // devices has changed.
            @Override
            public void onAudioDeviceChanged(
                    AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
                onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
            }
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connected = false;
                disconnect();
            }
        });
    }

    @Override
    public void onPeerConnectionClosed() {
    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError && connected) {

                }
            }
        });
    }

    @Override
    public void onPeerConnectionError(final String description) {
        Log.e(TAG, "onPeerConnectionError: " + description);
    }

    @Override
    public void onNetworkError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (platform != null && platform.equals("ios")) {
                    stopAudioManager();
                    disconnect();
                    toastText = getString(R.string.call_declined);
                    showToast(toastText);
                } else {
                    callType.setVisibility(View.VISIBLE);
                    callType.setText(getString(R.string.poor_connection_connecting));
                    callTime.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onConnectedToRoom(AppRTCClient.SignalingParameters params) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onConnectedToRoomInternal(params);
            }
        });
    }

    @Override
    public void onRemoteDescription(SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                    return;
                }
                if (toneGenerator != null) {
                    toneGenerator.stopTone();
                    toneGenerator = null;
                }
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer = null;
                }
                isPeerReceived = true;
                isCallConnected = true;
                peerConnectionClient.setRemoteDescription(sdp);
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                    return;
                }
                peerConnectionClient.addRemoteIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onRemoteIceCandidatesRemoved(IceCandidate[] candidates) {
        if (peerConnectionClient == null) {
            Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
            return;
        }
        peerConnectionClient.removeRemoteIceCandidates(candidates);
    }

    @Override
    public void onChannelClose() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                disconnect();
            }
        });
    }

    @Override
    public void onChannelError(String description) {
        reportError(description);
    }


    private void startCountDown(String callType) {
        if (callType.equals("answer")) {
            Log.v(TAG, "startCountDown=" + callType);
            long startTime = SystemClock.elapsedRealtime();
            callTimer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    //Function call every second
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.v(TAG, "onChronometerTick");
                            long hours = (((SystemClock.elapsedRealtime() - startTime) / 1000) / 60) / 60;
                            long minutes = ((SystemClock.elapsedRealtime() - startTime) / 1000) / 60;
                            long seconds = ((SystemClock.elapsedRealtime() - startTime) / 1000) % 60;
                            callTime.setText(twoDigitString(hours) + ":" + twoDigitString(minutes) + ":" + twoDigitString(seconds));
                        }
                    });
                }
            }, 0, 1000);
        } else {
            long time;
            if (callType.equals("waiting")) {
                time = 30000;
            } else {
                time = 15000;
            }
            countDownTimer = new CountDownTimer(time, 1000) {
                @SuppressLint("SetTextI18n")
                @Override
                public void onTick(long millisUntilFinished) {
                    if (!NetworkReceiver.isConnected()) {
                        if (!CallActivity.this.isFinishing())
                            finish();
                    } else if (isCallConnected && isCallAttend) {
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                            Log.v(TAG, "countDownTimer=cancel");
                        }
                    }
                }

                @Override
                public void onFinish() {
                    Log.v(TAG, "countDownTimer=Ended");
                    if (isCallConnected && isCallAttend) {
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                            stopAudioManager();
                            Log.v(TAG, "countDownTimer=cancel");
                        }
                    } else {
                        if (!CallActivity.this.isFinishing())
                            finish();
                    }
                }
            };

            countDownTimer.start();
        }
    }

    private String twoDigitString(long number) {
        if (number == 0) {
            return "00";
        } else if (number / 10 == 0) {
            return "0" + number;
        }
        return String.valueOf(number);
    }

    private void updateVideoViews(final boolean remoteVisible) {
        Log.v(TAG, "updateVideoViews=" + remoteVisible);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pipRenderer != null && fullscreenRenderer != null) {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) pipRenderer.getLayoutParams();
                    if (remoteVisible && params != null) {
                        params.width = getDisplayMetrics().widthPixels * 30 / 100;
                        params.height = getDisplayMetrics().heightPixels * 20 / 100;
//                        params.height = ApplicationClass.dpToPx(CallActivity.this, 120);
//                        params.width = ApplicationClass.dpToPx(CallActivity.this, 90);
                        params.setMargins(0, 0, ApplicationClass.dpToPx(CallActivity.this, 20), getDisplayMetrics().widthPixels * 30 / 100);
                        pipRenderer.setBackground(ContextCompat.getDrawable(CallActivity.this, R.drawable.stroke_white_sharp));
                        pipRenderer.setLayoutParams(params);
                    }
                }
            }

        });

    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        closeConfirmDialog();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getStringExtra("_ACTION_");
        if (action != null) {
            switch (action) {
                case "endcall":
                    stopService(new Intent(getBaseContext(), CallNotificationService.class));
                    if (!CallActivity.this.isFinishing())
                        finish();
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {

            boolean isPermissionEnabled = false;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    isPermissionEnabled = false;
                    break;
                } else {
                    isPermissionEnabled = true;
                }
            }

            if (!isPermissionEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(CAMERA) &&
                            shouldShowRequestPermissionRationale(RECORD_AUDIO) &&
                            shouldShowRequestPermissionRationale(WAKE_LOCK)) {
                        requestPermission(new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 101);
                    } else {
                        isPermissionError = true;
                        makeToast(getString(R.string.call_permission_error));
                        disconnect();
                    }
                }
            } else {
                initRender();
            }
        }
    }

    @Override
    public void onNetworkChange(boolean isConnected) {
        if (!isConnected) {
            ApplicationClass.showToast(CallActivity.this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT);
            finish();
        }
    }

    private void closeConfirmDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.default_popup);
        dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 90 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView title = dialog.findViewById(R.id.title);
        TextView yes = dialog.findViewById(R.id.yes);
        TextView no = dialog.findViewById(R.id.no);
        yes.setText(getString(R.string.im_sure));
        no.setText(getString(R.string.nope));
        title.setText(R.string.really_endcall);
        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (!CallActivity.this.isFinishing())
                    finish();
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    private boolean checkPermissions() {
        int permissionCamera = ContextCompat.checkSelfPermission(CallActivity.this,
                CAMERA);
        int permissionAudio = ContextCompat.checkSelfPermission(CallActivity.this,
                RECORD_AUDIO);
        int permissionWakeLock = ContextCompat.checkSelfPermission(CallActivity.this,
                WAKE_LOCK);
        return permissionCamera == PackageManager.PERMISSION_GRANTED &&
                permissionAudio == PackageManager.PERMISSION_GRANTED &&
                permissionWakeLock == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(CallActivity.this, permissions, requestCode);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (type.equals(Constants.TAG_AUDIO)) {
            if (sensorEvent.values[0] == 0) {
                //TODO Turn on screen
                turnOnScreen();
            } else {
                //TODO Turn off screen
                turnOffScreen();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void turnOnScreen() {
        releaseWakeLock();
        if (!isScreenOn) {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            }
            isScreenOn = true;
        }
    }

    public void turnOffScreen() {
        // turn off screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            releaseWakeLock();
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, CPU_WAKELOCK);
            wakeLock.acquire();
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        isScreenOn = false;
    }

    public void setWaiting() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cancelCallEmitTimer();
                startCountDown("waiting");
                if (callTime != null) {
                    callType.setText(String.format(getString(R.string.is_an_another_call), results.user_name));
                }
            }
        });
    }

    public void stopAudioManager() {
        if (audioManager != null) {
            /*Reset audio manager to previous state*/
            audioManager.stop();
            audioManager = null;
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (commandLineRun || !activityRunning) {
            Log.e(TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            disconnect();
          new AlertDialog.Builder(this)
                    .setTitle(getText(R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(getString(R.string.okay),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    disconnect();
                                }
                            })
                    .create()
                    .show();
        }
    }
}
