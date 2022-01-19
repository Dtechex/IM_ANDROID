package com.loopytime.im.status;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.DefaultControlDispatcher;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.loopytime.external.CustomEditText;
import com.loopytime.external.DelayBitmapTransformation;
import com.loopytime.external.RandomString;
import com.loopytime.external.keyboard.KeyboardHeightObserver;
import com.loopytime.external.keyboard.KeyboardHeightProvider;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.DownloadFiles;
import com.loopytime.helper.ImageDownloader;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.StorageManager;
import com.loopytime.im.ApplicationClass;
import com.loopytime.im.BaseActivity;
import com.loopytime.im.R;
import com.loopytime.model.ContactsData;
import com.loopytime.model.StatusDatas;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;

public class SingleStoryActivity extends BaseActivity implements
        SocketConnection.StoryViewedFromSocket, View.OnClickListener, KeyboardHeightObserver, StoryStatusView.StoriesListener {

    private static String TAG = SingleStoryActivity.class.getSimpleName();
    public static String isMessageSent = "";
    PlayerView simpleExoPlayerView;
    FrameLayout videoFrame;
    ImageView image, userImage, close, sent, statusView;
    ProgressBar imageProgressBar;
    View reverse, center, skip;
    LinearLayout actions, bottomLay;
    StoryStatusView storyStatusView;
    TextView userName, time, statusMsg, seenCount;
    CustomEditText messageEdit;
    RelativeLayout edtLay, parentLay;
    StatusDatas statusDatas = new StatusDatas();
    ContactsData.Result results;
    DefaultControlDispatcher dispatcher;
    MediaSource videoSource;
    ExtractorsFactory extractorsFactory;
    DataSource.Factory dataSourceFactory;
    SimpleExoPlayer player;
    String way = "";
    DatabaseHandler dbHelper;
    StorageManager storageManager;
    SocketConnection socketConnection;
    ViewPeopleAdapter viewPeopleAdapter;
    CountDownTimer longClickTimer;
    ArrayList<HashMap<String, String>> viewedContacts;
    private SharedPreferences pref;

    private long startVideoRecordingTime = 1000 * 60;
    private long clickTime;
    private boolean isVideoPlaying = true, isCaching = true, isCreate = false, isMute = false,
            isReadMore = false, flag = false, isPaused = false;
    private KeyboardHeightProvider keyboardHeightProvider;
    private long pressTime = 0L, limit = 500L;
    private boolean isStoryPlaying;

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pressTime = System.currentTimeMillis();
                    pauseAll();
                    return false;
                case MotionEvent.ACTION_UP:
                    long now = System.currentTimeMillis();
                    resumeAll();
                    return limit < now - pressTime;
            }
            return false;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setContentView(R.layout.pager_item);
        findViews();

        dbHelper = DatabaseHandler.getInstance(this);
        storageManager = StorageManager.getInstance(this);
        socketConnection = SocketConnection.getInstance(this);
        pref = getSharedPreferences("SavedPref", MODE_PRIVATE);
        keyboardHeightProvider = new KeyboardHeightProvider(this);

        if (getIntent().getSerializableExtra(Constants.TAG_DATA) != null) {
            statusDatas = (StatusDatas) getIntent().getSerializableExtra(Constants.TAG_DATA);
        }

        if (ApplicationClass.isRTL()) {
            sent.setRotation(180);
        } else {
            sent.setRotation(0);
        }

        results = dbHelper.getContactDetail(statusDatas.mSenderId);

        edtLay.setVisibility(View.VISIBLE);
        if (ApplicationClass.isStringNotNull(results.user_id)) {
            if (results.user_id.equalsIgnoreCase(GetSet.getUserId())) {
                way = "ownstory";
                messageEdit.setVisibility(View.GONE);
                sent.setVisibility(View.GONE);
                setSeenCount(true);
                userName.setText(getString(R.string.you));
            } else {
                messageEdit.setVisibility(View.VISIBLE);
                messageEdit.setError(null);
                sent.setVisibility(View.VISIBLE);
                setSeenCount(false);
                userName.setText(results.user_name);
            }

            setPlayer();

            storyStatusView.setVisibility(View.VISIBLE);
            bottomLay.setVisibility(View.VISIBLE);
            storyStatusView.setStoriesCount(1);
            if (ApplicationClass.hasNavigationBar()) {
                initBottomPadding(pref.getInt(Constants.TAG_NAV_HEIGHT, 0));
            }
            initTopLayPadding(pref.getInt(Constants.TAG_STATUS_HEIGHT, 0));
            Glide.with(getApplicationContext()).load(Constants.USER_IMG_PATH + results.user_image).thumbnail(0.5f)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(this, 70)))
                    .into(userImage);
            time.setText(ApplicationClass.getTime(Long.parseLong(statusDatas.mStatusTime)));
            stopVideoView();

            storyStatusView.setStoriesListener(SingleStoryActivity.this);
            storyStatusView.setStoryDuration(Integer.MAX_VALUE);
            storyStatusView.setPos(0);
            startStories();
            //pause functionality is working delay 100 seconds
            pauseView();
            isReadMore = false;

            if (!statusDatas.mMessage.equals("")) {
                statusMsg.setVisibility(View.VISIBLE);
                statusMsg.setText(statusDatas.mMessage);
                statusMsg.post(new Runnable() {
                    @Override
                    public void run() {
                        if (statusMsg != null && statusMsg.length() > 70) {
                            addReadMore(statusMsg.getText().toString(), statusMsg);
                        }
                    }
                });
                if (statusDatas.mType.equalsIgnoreCase("image")) {
                    storyStatusView.setStoryDuration(6000);
                }
            } else {
                statusMsg.setVisibility(View.GONE);
            }


            if (statusDatas.mType.equalsIgnoreCase("video")) {
                image.setVisibility(View.GONE);
            } else {
                imageProgressBar.setIndeterminate(true);
                imageProgressBar.setVisibility(View.VISIBLE);
            }
            setStatus(statusDatas.mType, statusDatas.mAttachment);
        } else {
            finish();
        }

        messageEdit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    pauseAll();
                }
                return false;
            }
        });

        messageEdit.setKeyImeChangeListener(new CustomEditText.KeyImeChange() {
            @Override
            public void onKeyIme(int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
                    isPaused = false;
                    resumeAll();
                }
            }
        });

        findViewById(R.id.skip).setOnTouchListener(onTouchListener);
        findViewById(R.id.reverse).setOnTouchListener(onTouchListener);
        findViewById(R.id.center).setOnTouchListener(onTouchListener);

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                storyStatusView.skip();

            }
        });

        center.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                storyStatusView.skip();
            }
        });

        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                storyStatusView.skip();
            }
        });
    }

    private void findViews() {
        simpleExoPlayerView = findViewById(R.id.exoplayer);
        videoFrame = findViewById(R.id.video_frame);
        image = findViewById(R.id.image);
        imageProgressBar = findViewById(R.id.imageProgressBar);
        reverse = findViewById(R.id.reverse);
        center = findViewById(R.id.center);
        skip = findViewById(R.id.skip);
        actions = findViewById(R.id.actions);
        storyStatusView = findViewById(R.id.storiesStatus);
        userImage = findViewById(R.id.userImage);
        userName = findViewById(R.id.user_name);
        close = findViewById(R.id.close);
        time = findViewById(R.id.time);
        messageEdit = findViewById(R.id.msg);
        sent = findViewById(R.id.sent);
        statusView = findViewById(R.id.statusView);
        bottomLay = findViewById(R.id.bottomLay);
        edtLay = findViewById(R.id.edtLay);
        parentLay = findViewById(R.id.parentLay);
        statusMsg = findViewById(R.id.statusMsg);
        seenCount = findViewById(R.id.seenCount);

        parentLay.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                initBottomPadding(view.getPaddingBottom() + windowInsets.getSystemWindowInsetBottom() + ApplicationClass.dpToPx(SingleStoryActivity.this, 10));
                return windowInsets.consumeSystemWindowInsets();
            }
        });
        close.setOnClickListener(this);
        sent.setOnClickListener(this);
        statusView.setOnClickListener(this);
        center.setOnClickListener(this);
    }

    public void setPlayer() {
        // Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        //Initialize the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        simpleExoPlayerView.setPlayer(player);

        // used for Repeat mode to video
        dispatcher = new DefaultControlDispatcher();

        // Produces DataSource instances through which media data is loaded.
        dataSourceFactory =
                new DefaultDataSourceFactory(this, Util.getUserAgent(this, "CloudinaryExoplayer"));

        // Produces Extractor instances for parsing the media data.
        extractorsFactory = new DefaultExtractorsFactory();

    }


    private void addReadMore(final String text, final TextView textView) {
        SpannableString ss = new SpannableString(text.substring(0, 70) + " " + getString(R.string.read_more) + " ");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                isReadMore = true;
                pauseAll();
                playVideo();
                addReadLess(text, textView);
                if (player != null) {
                    player.setPlayWhenReady(false);
                }

            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(ContextCompat.getColor(SingleStoryActivity.this, R.color.colorAccent));
            }
        };
        ss.setSpan(clickableSpan, ss.length() - 10, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void addReadLess(final String text, final TextView textView) {
        SpannableString ss = new SpannableString(text + " " + getString(R.string.read_less) + " ");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                isReadMore = false;
                resumeAll();
                addReadMore(text, textView);
                if (player != null) {
                    player.setPlayWhenReady(true);
                }
                playVideo();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(ContextCompat.getColor(SingleStoryActivity.this, R.color.colorAccent));
            }
        };
        ss.setSpan(clickableSpan, ss.length() - 10, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void setSeenCount(boolean isVisible) {
        if (isVisible) {
            statusView.setVisibility(View.VISIBLE);
            seenCount.setVisibility(View.VISIBLE);
            viewedContacts = dbHelper.getViewedContact(getApplicationContext(), statusDatas.mStatusId);
            seenCount.setText(" " + viewedContacts.size());
        } else {
            statusView.setVisibility(View.GONE);
            seenCount.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.close:
                releasePlayer();
                finish();
                break;
            case R.id.sent:
                sendMessage();
                break;
            case R.id.statusView:
                pauseAll();
                viewDialog();
                break;
            case R.id.center:

                break;
        }
    }

    public void stopVideoView() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                videoFrame.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void releasePlayer() {
        if (simpleExoPlayerView != null) {
            simpleExoPlayerView.getPlayer().setPlayWhenReady(false);
            simpleExoPlayerView.getPlayer().stop();
            simpleExoPlayerView.getPlayer().release();
            //  simpleExoPlayerView = null;
        }
    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    private void networkSnack() {
       /* Snackbar snackbar = Snackbar
                .make(mianLay, getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();*/
    }

    private void setStatus(String type, String name) {
        isStoryPlaying = false;
        if (way.equalsIgnoreCase("ownstory")) {
            if (type.equalsIgnoreCase("video")) {
                if (storageManager.checkStatusExists(name, type, StorageManager.TAG_VIDEO_SENT)) {
                    try {
                        File file = new File(storageManager.getFilePath(StorageManager.TAG_VIDEO_SENT, name));
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(SingleStoryActivity.this, Uri.fromFile(file));
                        long METADATA_KEY_DURATION = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                        storyStatusView.setStoryDuration(METADATA_KEY_DURATION);
                        playVideoInLocal(file.getAbsolutePath());
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(SingleStoryActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SingleStoryActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                }
            } else {
                if (storageManager.checkStatusExists(name, type, StorageManager.TAG_SENT)) {
                    File file = new File(storageManager.getFilePath(StorageManager.TAG_SENT, name));
                    if (file != null) {
                        storyStatusView.setStoryDuration(Constants.storyDuration);
                        Log.v(TAG, "file=" + file.getAbsolutePath());
                        RequestOptions requestOptions = new RequestOptions();
                        requestOptions.diskCacheStrategy(isCaching ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                                .centerCrop()
                                .skipMemoryCache(!isCaching)
                                .transform(new MultiTransformation(new CenterCrop(), new DelayBitmapTransformation(1000)));
                        Glide.with(getApplicationContext()).load(Uri.fromFile(file)).thumbnail(0.5f)
                                .transition(new DrawableTransitionOptions().crossFade())
                                .into(image);
                        imageProgressBar.setVisibility(View.GONE);
                        resumeStories();
                    }
                } else {
                    Toast.makeText(SingleStoryActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                    storyStatusView.skip();
                }
            }
        } else {
            if (type.equalsIgnoreCase("video")) {
                if (storageManager.checkStatusExists(name, type, Constants.TAG_STATUS)) {
                    try {
                        File file = new File(storageManager.getFilePath(Constants.TAG_STATUS, name));
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(SingleStoryActivity.this, Uri.fromFile(file));
                        long METADATA_KEY_DURATION = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                        storyStatusView.setStoryDuration(METADATA_KEY_DURATION);
                        playVideoInLocal(file.getAbsolutePath());
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(SingleStoryActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    if (isNetworkConnected().equals(NOT_CONNECT)) {
                        networkSnack();
                    } else {
                        try {
                            pauseView();
                            downloadFile(name);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                if (storageManager.checkStatusExists(name, type, Constants.TAG_STATUS)) {
                    File file = new File(storageManager.getFilePath(Constants.TAG_STATUS, name));
                    if (file.getAbsolutePath() != null && !TextUtils.isEmpty(file.getAbsolutePath())) {
                        storyStatusView.setStoryDuration(Constants.storyDuration);
                        resumeStories();
                        RequestOptions requestOptions = new RequestOptions();
                        requestOptions.diskCacheStrategy(isCaching ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                                .centerCrop()
                                .skipMemoryCache(!isCaching)
                                .transform(new MultiTransformation(new CenterCrop(), new DelayBitmapTransformation(1000)));
                        Glide.with(getApplicationContext()).load(file).thumbnail(0.5f)
                                .transition(new DrawableTransitionOptions().crossFade())
                                .into(image);
                        imageProgressBar.setVisibility(View.GONE);
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(SingleStoryActivity.this, WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(SingleStoryActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, 100);
                    } else {
                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                            networkSnack();
                        } else {
                            pauseDelay();
                            ImageDownloader imageDownloader = new ImageDownloader(SingleStoryActivity.this) {
                                @Override
                                protected void onPostExecute(Bitmap imgBitmap) {
                                    if (imgBitmap == null) {
                                        Log.v("bitmapFailed", "bitmapFailed");
                                        Toast.makeText(SingleStoryActivity.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.v("onBitmapLoaded", "onBitmapLoaded");
                                        try {
                                            String status = storageManager.saveToSdCard(imgBitmap, Constants.TAG_STATUS, name);
                                            if (status.equals("success")) {
                                                RequestOptions requestOptions = new RequestOptions();
                                                requestOptions.diskCacheStrategy(isCaching ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                                                        .centerCrop()
                                                        .skipMemoryCache(!isCaching)
                                                        .transform(new MultiTransformation(new CenterCrop(), new DelayBitmapTransformation(1000)));
                                                storyStatusView.setStoryDuration(Constants.storyDuration);
                                                if (storyStatusView.isPaused(0)) {
                                                    resumeStories();
                                                } else {
                                                    storyStatusView.startStories();
                                                }
                                                File thumbFile = new File(storageManager.getFilePath(Constants.TAG_STATUS, name));
                                                Glide.with(getApplicationContext()).load(thumbFile).thumbnail(0.5f)
                                                        .into(image);
                                                imageProgressBar.setVisibility(View.GONE);
                                            } else {
                                                Toast.makeText(SingleStoryActivity.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (NullPointerException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                protected void onProgressUpdate(String... progress) {
                                    //progressbar.setProgress(Integer.parseInt(progress[0]));
                                }
                            };
                            imageDownloader.execute(Constants.CHAT_IMG_PATH + name, Constants.TAG_STATUS);

                        }
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm.isAcceptingText()) {
            resumeAll();
            isPaused = false;
        } else {
            releasePlayer();
            finish();
        }
    }

    private void downloadFile(String fileName) {
        DownloadFiles downloadFiles = new DownloadFiles(SingleStoryActivity.this) {
            @Override
            protected void onPostExecute(String downPath) {
                playVideoInLocal(downPath);
            }
        };
        downloadFiles.execute(Constants.CHAT_IMG_PATH + fileName, Constants.TAG_STATUS);
    }

    private String getFileName(String url) {
        String imgSplit = url;
        int endIndex = imgSplit.lastIndexOf("/");
        if (endIndex != -1) {
            imgSplit = imgSplit.substring(endIndex + 1, imgSplit.length());
        }
        return imgSplit;
    }

    private void startStories() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isStoryPlaying = true;
                        storyStatusView.startStories();
                    }
                });
            }
        }, 100);
    }

    public void pauseDelay() {
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                storyStatusView.pause();
                            }
                        });
                    }
                }, 600);
    }

    private void playStories() {
        isStoryPlaying = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                storyStatusView.startStories();
            }
        }, 100);
    }

    private void resumeStories() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isStoryPlaying = true;
                        storyStatusView.resume();
                    }
                });
            }
        }, 100);
    }

    public void playVideoInLocal(String url) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {
                    Uri videoUri = Uri.parse(url);
                    videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri);
                    player.prepare(videoSource);
                    //    dispatcher.dispatchSetRepeatMode(simpleExoPlayerView.getPlayer(),2);4
                    player.addListener(new Player.EventListener() {
                        @Override
                        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

                        }

                        @Override
                        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

                        }

                        @Override
                        public void onLoadingChanged(boolean isLoading) {

                        }

                        @Override
                        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                            if (playbackState == Player.STATE_BUFFERING) {
                                Log.e("TAG", "STATE_BUFFERING");
                            } else if (playbackState == Player.STATE_ENDED) {
                                Log.e("TAG", "STATE_ENDED");
                            } else if (playbackState == Player.STATE_IDLE) {  //state_idle will call when player stop and release
                                Log.e("TAG", "STATE_IDLE");
                            } else {
                                isStoryPlaying = true;
                                imageProgressBar.setVisibility(View.INVISIBLE); //state else will call when player is completed
                                videoFrame.setVisibility(View.VISIBLE);
                                image.setVisibility(View.GONE);
                                edtLay.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onRepeatModeChanged(int repeatMode) {

                        }

                        @Override
                        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

                        }

                        @Override
                        public void onPlayerError(ExoPlaybackException error) {
                            /*If File is Invalid, then delete the old and download new file*/
                            if (("" + error.getMessage()).contains("UnrecognizedInputFormatException")) {
                                pauseView();
                                imageProgressBar.setIndeterminate(true);
                                imageProgressBar.setVisibility(View.VISIBLE);
//                                storageManager.deleteFile(url);
                                downloadFile(getFileName(url));
                            }
                        }

                        @Override
                        public void onPositionDiscontinuity(int reason) {

                        }

                        @Override
                        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

                        }

                        @Override
                        public void onSeekProcessed() {

                        }
                    });

                    playVideo();


                    if (!isMute) {
                        player.setVolume(1.0f);
                    } else {
                        player.setVolume(0);
                    }

                } catch (Exception e) {
                    Log.e("sourceException", "-" + e.toString());
                }
            }
        });

        isCreate = true;

    }

    void playVideo() {
        if (player != null) {
            if (!isReadMore) {
                resumeAll();
                player.setPlayWhenReady(true);
            } else {
                pauseAll();
                player.setPlayWhenReady(false);
            }
        }
    }

    /*public void setData(String userName, String userPath) {
        userTxt.setText(userName);
        timeTxt.setText(ApplicationClass.getTime(Long.parseLong(statusDatas.mStatusTime)));
    }*/

    public void pauseView() {
        //pause functionality is working delay 100 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (storyStatusView != null) {
                            storyStatusView.pause();
                        }
                    }
                });
            }
        }, 100);
    }

    public void resumeView() {
        //pause functionality is working delay 100 seconds
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (storyStatusView != null) {
                    storyStatusView.resume();
                }
            }
        });
    }

    public void pauseAll() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                storyStatusView.pause();
            }
        });

        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    public void resumeAll() {
        storyStatusView.resume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void onNext() {
        releasePlayer();
        finish();
    }


    @Override
    public void onPrev() {
        return;
    }

    @Override
    public void onComplete() {
        isReadMore = false;
        releasePlayer();
        finish();
    }

    private void blockChatConfirmDialog(String userId) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.default_popup);
        dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 90 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView title = dialog.findViewById(R.id.title);
        TextView yes = dialog.findViewById(R.id.yes);
        TextView no = dialog.findViewById(R.id.no);

        yes.setText(getString(R.string.unblock));
        no.setText(getString(R.string.cancel));
        title.setText(R.string.unblock_message);

        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                    jsonObject.put(Constants.TAG_RECEIVER_ID, userId);
                    jsonObject.put(Constants.TAG_TYPE, "unblock");
                    Log.v("block", "block=" + jsonObject);
                    socketConnection.block(jsonObject);
                    dbHelper.updateBlockStatus(userId, Constants.TAG_BLOCKED_BYME, "unblock");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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


    public void sendMessage() {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else if (results.blockedbyme.equals("block")) {
            blockChatConfirmDialog(results.user_id);
        } else {
            if (messageEdit.getText().toString().trim().length() > 0) {
                String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                String textMsg = messageEdit.getText().toString().trim();
                String chatId = GetSet.getUserId() + statusDatas.mSenderId;
                RandomString randomString = new RandomString(10);
                String messageId = GetSet.getUserId() + randomString.nextString();
                try {
                    if (!results.blockedme.equals("block")) {
                        JSONObject jobj = new JSONObject();
                        JSONObject message = new JSONObject();
                        message.put(Constants.TAG_USER_ID, GetSet.getUserId());
                        message.put(Constants.TAG_USER_NAME, GetSet.getUserName());
                        message.put(Constants.TAG_MESSAGE_TYPE, "story");
                        message.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(textMsg));
                        message.put(Constants.TAG_CHAT_TIME, unixStamp);
                        message.put(Constants.TAG_CHAT_ID, chatId);
                        message.put(Constants.TAG_MESSAGE_ID, messageId);
                        message.put(Constants.TAG_RECEIVER_ID, statusDatas.mSenderId);
                        message.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                        message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_SINGLE);
                        jobj.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                        jobj.put(Constants.TAG_RECEIVER_ID, statusDatas.mSenderId);


                        JSONObject statusObj = new JSONObject();
                        statusObj.put(Constants.TAG_ATTACHMENT, statusDatas.mAttachment);
                        statusObj.put(Constants.TAG_THUMBNAIL, statusDatas.mThumbnail);
                        statusObj.put(Constants.TAG_STORY_TYPE, statusDatas.mType);
                        statusObj.put(Constants.TAG_MESSAGE, statusDatas.mMessage);
                        statusObj.put(Constants.TAG_STORY_ID, statusDatas.mStatusId);

                        message.put(Constants.TAG_STATUS_DATA, ApplicationClass.encryptMessage(statusObj.toString()));

                        jobj.put("message_data", message);

                        Log.v("startchat", "startchat=" + jobj);
                        socketConnection.startChat(jobj);
                        dbHelper.addMessageDatas(chatId, messageId, GetSet.getUserId(), GetSet.getUserName(),
                                "story", ApplicationClass.encryptMessage(textMsg), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""),
                                ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""),
                                unixStamp, results.user_id, GetSet.getUserId(), "", ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(statusObj.toString()));
                        dbHelper.addRecentMessages(chatId, results.user_id, messageId, unixStamp, "0");
                        Toast.makeText(SingleStoryActivity.this, getString(R.string.sending_message), Toast.LENGTH_SHORT).show();
                        isMessageSent = messageId;
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

                messageEdit.setText("");
            } else {
                messageEdit.setError(getString(R.string.please_enter_your_message));
            }
        }

        ApplicationClass.hideSoftKeyboard(this, messageEdit);
        storyStatusView.resume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }

        messageEdit.setText("");
    }

    /**
     * To open status viewed dialog
     */

    private void viewDialog() {
        pauseAll();
        View bottomView = getLayoutInflater().inflate(R.layout.status_viewed_dialog, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(bottomView);

        RecyclerView viewedRecyclerView = bottomView.findViewById(R.id.viewedRecyclerView);
        ImageView deleteStory = bottomView.findViewById(R.id.deleteStory);
        TextView viewCount = bottomView.findViewById(R.id.viewCount);
        viewedContacts = dbHelper.getViewedContact(getApplicationContext(), statusDatas.mStatusId);
        viewPeopleAdapter = new ViewPeopleAdapter(viewedContacts, getApplicationContext());
        viewedRecyclerView.setAdapter(viewPeopleAdapter);

        viewCount.setText(getString(R.string.viewed_by) + " " + viewedContacts.size());

        dialog.show();
        deleteStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                socketConnection.deleteStory(statusDatas.mStatusId);
                dbHelper.deleteStatus(statusDatas.mStatusId);
                releasePlayer();
                finish();
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!isReadMore) {
                    resumeAll();
                }
            }
        });
    }

    @Override
    public void onStoryViewed(String storyId) {
        if (statusDatas.mStatusId.equals(storyId)) {
            viewedContacts = dbHelper.getViewedContact(getApplicationContext(), storyId);
            viewPeopleAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDeleteStatus(String statusId) {
        if (statusDatas.mStatusId.equals(statusId)) {
            releasePlayer();
            finish();
        }
    }

    @Override
    public void onKeyboardHeightChanged(int height, int orientation) {
        if (height > 0) {
            setToImmersiveMode();
        } else if (height < 0) {
            if (statusMsg != null) {
                statusMsg.setVisibility(View.VISIBLE);
            }
            initBottomPadding((-height) + ApplicationClass.dpToPx(SingleStoryActivity.this, 10));
        } else {
            if (statusMsg != null) {
                statusMsg.setVisibility(View.VISIBLE);
            }
            initBottomPadding(ApplicationClass.dpToPx(SingleStoryActivity.this, 10));
        }
    }

    private void setToImmersiveMode() {
        // set to immersive
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }


    /**
     * Adapter to see people who viewed the status
     */

    class ViewPeopleAdapter extends RecyclerView.Adapter<ViewPeopleAdapter.ViewHolder> {
        ArrayList<HashMap<String, String>> items;
        Context context;

        ViewPeopleAdapter(ArrayList<HashMap<String, String>> items, Context context) {
            this.items = items;
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.people_viewed_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, String> map = items.get(position);
            holder.name.setText(map.get(Constants.TAG_USER_NAME));
            holder.message.setText(ApplicationClass.getTime(Long.parseLong(map.get(Constants.TAG_STATUS_TIME))));
            Glide.with(context).load(Constants.USER_IMG_PATH + map.get(Constants.TAG_USER_IMAGE)).thumbnail(0.5f)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                    .into(holder.profileimage);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            CircleImageView profileimage;
            TextView name, message;
            Context context;

            ViewHolder(View itemView) {
                super(itemView);
                profileimage = itemView.findViewById(R.id.profileimage);
                name = itemView.findViewById(R.id.name);
                message = itemView.findViewById(R.id.message);
                context = itemView.getContext();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        keyboardHeightProvider.setKeyboardHeightObserver(this);
        initMargins();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                resumeView();
            }
        }, 200);
    }

    @Override
    public void onPause() {
        super.onPause();
        keyboardHeightProvider.setKeyboardHeightObserver(null);
        if (player != null)
            player.setPlayWhenReady(false);

        pauseView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ApplicationClass.hideSoftKeyboard(this);
    }

    @Override
    public void onDestroy() {
        keyboardHeightProvider.close();
        storyStatusView.destroy();
        super.onDestroy();
        keyboardHeightProvider.close();
        if (simpleExoPlayerView.getPlayer() != null) {
            simpleExoPlayerView.getPlayer().setPlayWhenReady(false);
            simpleExoPlayerView.getPlayer().stop();
            simpleExoPlayerView.getPlayer().release();
        }
    }

    private void initMargins() {
        int bottomMargin = pref.getInt(Constants.TAG_NAV_HEIGHT, 0);
        if (ApplicationClass.hasNavigationBar()) {
            initBottomPadding(bottomMargin + 5);
        } else {
            initBottomPadding(ApplicationClass.dpToPx(SingleStoryActivity.this, 10));
        }
    }

    private void initBottomPadding(int bottomPadding) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bottomLay.setPadding(0, 0, 0, bottomPadding);
        bottomLay.setLayoutParams(params);
    }

    private void initTopLayPadding(int topMargin) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ApplicationClass.dpToPx(SingleStoryActivity.this, 2));
        layoutParams.topMargin = topMargin + ApplicationClass.dpToPx(SingleStoryActivity.this, 5);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        storyStatusView.setLayoutParams(layoutParams);
    }

}
