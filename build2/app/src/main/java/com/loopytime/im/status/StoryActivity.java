
package com.loopytime.im.status;

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
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.ToxicBakery.viewpager.transforms.CubeInTransformer;
import com.ToxicBakery.viewpager.transforms.CubeOutTransformer;
import com.bumptech.glide.Glide;
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
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.loopytime.external.CustomEditText;
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
import com.loopytime.im.DialogActivity;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;

public class StoryActivity extends BaseActivity implements
        SocketConnection.StoryViewedFromSocket, KeyboardHeightObserver, StoryStatusView.StoriesListener {

    private static final String TAG = StoryActivity.class.getSimpleName();
    private static final ArrayList<TransformerItem> TRANSFORM_CLASSES;

    static {
        TRANSFORM_CLASSES = new ArrayList<TransformerItem>();
        TRANSFORM_CLASSES.add(new TransformerItem(CubeInTransformer.class));
        TRANSFORM_CLASSES.add(new TransformerItem(CubeOutTransformer.class));
    }

    public StoryStatusView storyStatusView;
    public File[] files;
    // Declaration of widgets
    PlayerView simpleExoPlayerView;
    DefaultControlDispatcher dispatcher;
    MediaSource videoSource;
    ExtractorsFactory extractorsFactory;
    com.google.android.exoplayer2.upstream.DataSource.Factory dataSourceFactory;
    SimpleExoPlayer player;
    ProgressBar imageProgressBar;
    ViewPager viewPager;
    CustomEditText messageEdit;
    TextView userTxt, timeTxt, seenCount, statusMsg;
    RelativeLayout edtLay, parentLayout;
    private FrameLayout contentLay;
    LinearLayout bottomLay;
    ConstraintLayout mainLay;
    CountDownTimer longClickTimer;
    String way = "";
    List<ContactsData.Result> statusList = new ArrayList<>();
    List<StatusDatas> statusDatas = new ArrayList<>();
    DatabaseHandler dbHelper;
    StorageManager storageManager;
    SocketConnection socketConnection;
    ViewPeopleAdapter viewPeopleAdapter;
    ArrayList<HashMap<String, String>> viewedContacts;
    boolean isOpenChoose = false;
    private ImageView statusImage, userImg, sent, statusView;
    private FrameLayout videoFrame;
    private String previewFilePath, filename;
    private int currentPlaybackPosition = 0, playpos = 0;
    private boolean isStoryPlaying = false, isCaching = true, isCreate = false, isMute = false,
            isReadMore = false, isPaused = false;
    private int counter = 0, pagerPosition = 0, pos = 0, posParent = 0;
    private long pressTime = 0L, limit = 500L;
    private SharedPreferences pref;
    private KeyboardHeightProvider keyboardHeightProvider;
    private ViewPagerAdapter viewPagerAdapter;
    int bottomNavHeight = 0, bottomMargin = 0;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setContentView(R.layout.pager_activity);
        isCreate = false;
        isMute = false;
        pref = getSharedPreferences("SavedPref", MODE_PRIVATE);
        keyboardHeightProvider = new KeyboardHeightProvider(this);
        dbHelper = DatabaseHandler.getInstance(this);
        storageManager = StorageManager.getInstance(this);
        socketConnection = SocketConnection.getInstance(this);
        mainLay = findViewById(R.id.mainLay);
        viewPager = findViewById(R.id.viewPager);
        contentLay = findViewById(R.id.content_lay);
        bottomMargin = ApplicationClass.dpToPx(this, 2);

        if (getIntent().getSerializableExtra(Constants.TAG_DATA) != null) {
            statusList = (List<ContactsData.Result>) getIntent().getSerializableExtra(Constants.TAG_DATA);
            pos = getIntent().getIntExtra(Constants.TAG_POSITION, -1);
            if (statusList.get(pos).user_id.equals(GetSet.getUserId()))
                way = "ownstory";
        }

        mainLay.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                bottomNavHeight = view.getPaddingBottom() + windowInsets.getSystemWindowInsetBottom() + bottomMargin;
                return windowInsets.consumeSystemWindowInsets();
            }
        });

        if (!way.equalsIgnoreCase("ownstory")) {
            if (statusList.get(0).user_id.equalsIgnoreCase(GetSet.getUserId())) {
                pagerPosition = pos - 1;
            } else pagerPosition = pos;
        }

        Log.e(TAG, "Position: " + pos + ", Way: " + way);
        viewPagerAdapter = new ViewPagerAdapter(this, statusList);
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setCurrentItem(pagerPosition);
        viewPager.setOffscreenPageLimit(statusList.size());

        try {
            viewPager.setPageTransformer(true, TRANSFORM_CLASSES.get(1).clazz.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.i(TAG, "onPageSelected: " + position);
                pagerPosition = position;
                if (statusList.get(0).user_id.equalsIgnoreCase(GetSet.getUserId())) {
                    position++;
                    pos = position;
                } else {
                    pos = position;
                }

                statusDatas = dbHelper.getSingleUserStatus(statusList.get(position).user_id);
                isCreate = false;
                counter = 0;

                storyStatusView.destroy();
                storyStatusView.setClearAnim();

                for (StatusDatas datas : statusDatas) {
                    if (datas.mIsSeen.equals("0")) {
                        counter = statusDatas.indexOf(datas);
                        break;
                    }
                }

                if (counter < 1) {
                    counter = 0;
                }

                releasePlayer();

                if (statusImage != null)
                    statusImage.setVisibility(View.VISIBLE);
                init(statusList.get(pos));

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.e(TAG, "onPageScrollStateChanged: " + state);
                // if(storyStatusView!=null)
                //  resumeAll();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (statusList.size() > 0) {
                    statusDatas = dbHelper.getSingleUserStatus(statusList.get(pos).user_id);
                    Log.i(TAG, "statusDatas.Size: " + statusDatas.size());
                    Log.i(TAG, "statusDatas.Size: " + pos);
                    init(statusList.get(pos));
                }
            }
        }, 500);
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    public void init(ContactsData.Result data) {

        View view = viewPager.findViewWithTag(viewPager.getCurrentItem() + "pos");

        statusImage = view.findViewById(R.id.image);
        ImageView close_img = view.findViewById(R.id.close);
        videoFrame = view.findViewById(R.id.video_frame);
        imageProgressBar = view.findViewById(R.id.imageProgressBar);
        messageEdit = view.findViewById(R.id.msg);
        userImg = view.findViewById(R.id.userImage);
        sent = view.findViewById(R.id.sent);
        userTxt = view.findViewById(R.id.user_name);
        statusMsg = view.findViewById(R.id.statusMsg);
        seenCount = view.findViewById(R.id.seenCount);
        timeTxt = view.findViewById(R.id.time);
        statusView = view.findViewById(R.id.statusView);
        edtLay = view.findViewById(R.id.edtLay);
        bottomLay = view.findViewById(R.id.bottomLay);
        parentLayout = view.findViewById(R.id.parentLay);
        storyStatusView = view.findViewById(R.id.storiesStatus);
        //Initialize simpleExoPlayerView
        simpleExoPlayerView = view.findViewById(R.id.exoplayer);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mainLay.post(new Runnable() {
                    public void run() {
                        keyboardHeightProvider.start();
                    }
                });
                initTopLayPadding(pref.getInt(Constants.TAG_STATUS_HEIGHT, 0));
                if (bottomLay != null) {
                    initBottomPadding(bottomNavHeight + bottomMargin);
                    bottomLay.setVisibility(View.VISIBLE);
                }
                if (edtLay != null)
                    edtLay.setVisibility(View.VISIBLE);

                if (storyStatusView != null) {
                    storyStatusView.setVisibility(View.VISIBLE);
                }
            }
        }, 200);

        if (ApplicationClass.isRTL()) {
            sent.setRotation(180);
        } else {
            sent.setRotation(0);
        }

        setPlayer();

        statusImage.setVisibility(View.VISIBLE);
        imageProgressBar.setIndeterminate(true);

        if (way.equalsIgnoreCase("ownstory")) {
            messageEdit.setVisibility(View.GONE);
            sent.setVisibility(View.GONE);
            setSeenCount(true);
        } else {
            messageEdit.setVisibility(View.VISIBLE);
            messageEdit.setError(null);
            sent.setVisibility(View.VISIBLE);
            setSeenCount(false);
        }

        counter = 0;
        for (StatusDatas datas : statusDatas) {
            if (datas.mIsSeen.equals("0")) {
                counter = statusDatas.indexOf(datas);
                break;
            }
        }

        view.findViewById(R.id.skip).setOnTouchListener(onTouchListener);
        view.findViewById(R.id.reverse).setOnTouchListener(onTouchListener);
        view.findViewById(R.id.center).setOnTouchListener(onTouchListener);

        view.findViewById(R.id.skip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        storyStatusView.skip();
                    }
                }, 100);
            }
        });

        view.findViewById(R.id.center).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        storyStatusView.skip();
                    }
                }, 100);
            }
        });

        view.findViewById(R.id.reverse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        storyStatusView.reverse();
                    }
                }, 100);
            }
        });

        close_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                releasePlayer();
                finish();
            }
        });

        statusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseAll();
                viewDialog();
            }
        });

        messageEdit.setOnTouchListener((view1, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                pauseAll();
            }
            return false;
        });

        messageEdit.setKeyImeChangeListener((keyCode, event) -> {
            if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
                isPaused = false;
                resumeAll();
            }
        });

        sent.setOnClickListener(v -> sendMessage());

        isReadMore = false;
        stopVideoView();
        setUserData(data);

        if (statusDatas.get(counter).mType.equalsIgnoreCase("video")) {
            statusImage.setVisibility(View.GONE);
        } else {
            imageProgressBar.setIndeterminate(true);
            imageProgressBar.setVisibility(View.VISIBLE);
        }
        storyStatusView.setStoriesCount(statusDatas.size());
        storyStatusView.setStoriesListener(StoryActivity.this);
        storyStatusView.setStoryDuration(Integer.MAX_VALUE);
        storyStatusView.setPos(counter);
        startStories();
        pauseView();
        setStatus(statusDatas.get(counter).mType, statusDatas.get(counter).mAttachment);
    }

    private void addReadMore(final String text, final TextView textView) {
        SpannableString ss = new SpannableString(text.substring(0, 70) + " " + getString(R.string.read_more) + " ");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                isReadMore = true;
                addReadLess(text, textView);
                playVideo();
                pauseAll();
                if (player != null) {
                    player.setPlayWhenReady(false);
                }
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(ContextCompat.getColor(StoryActivity.this, R.color.colorAccent));
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
                /*if(player!=null){
                    player.setPlayWhenReady(true);
                }*/
                playVideo();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(ContextCompat.getColor(StoryActivity.this, R.color.colorAccent));
            }
        };
        ss.setSpan(clickableSpan, ss.length() - 10, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setSeenCount(boolean isVisible) {
        if (isVisible) {
            statusView.setVisibility(View.VISIBLE);
            seenCount.setVisibility(View.VISIBLE);
            viewedContacts = dbHelper.getViewedContact(getApplicationContext(), statusDatas.get(counter).mStatusId);
            seenCount.setText(" " + viewedContacts.size());
        } else {
            statusView.setVisibility(View.GONE);
            seenCount.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNext() {
        counter++;
        Log.d(TAG, "onNext: " + counter);
        Media();
    }

    @Override
    public void onPrev() {
        if (counter - 1 < 0) {
            if (pagerPosition != 0) {
                pagerPosition--;
                viewPager.setCurrentItem(pagerPosition);
                Log.d(TAG, "onPrev: ");
            } else {
                if (statusDatas.get(counter).mType.contains("video")) {
                    releasePlayer();
                    setPlayer();
                }
            }
            setStatus(statusDatas.get(counter).mType, statusDatas.get(counter).mAttachment);
            return;
        }
        pauseView();
        counter--;
        Log.d(TAG, "onPrev: " + counter);

        Media();
    }

    @Override
    public void onComplete() {
        if ((statusDatas.size() - 1) == counter) {
            pagerPosition++;
            isReadMore = false;
            if (pagerPosition == viewPager.getAdapter().getCount()) {
                releasePlayer();
                finish();
            } else {
                if (storyStatusView != null) {
                    storyStatusView.destroy();
                    storyStatusView.setStoriesListener(null);
                }
                viewPager.setCurrentItem(pagerPosition);
            }
        }
    }

    public void Media() {
        isReadMore = false;
        pauseView();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        releasePlayer();
        setPlayer();
        stopVideoView();
        storyStatusView.setStoryDuration(Integer.MAX_VALUE);
        timeTxt.setText(ApplicationClass.getStatusTime(StoryActivity.this, Long.parseLong(statusDatas.get(counter).mStatusTime), false));

        statusImage.setImageResource(0);
        statusImage.setVisibility(View.VISIBLE);
        imageProgressBar.setVisibility(View.VISIBLE);

        if (statusDatas.get(counter).mSenderId.equalsIgnoreCase(GetSet.getUserId())) {
            way = "ownstory";
        }

        edtLay.setVisibility(View.VISIBLE);
        if (way.equalsIgnoreCase("ownstory")) {
            messageEdit.setVisibility(View.GONE);
            sent.setVisibility(View.GONE);
            setSeenCount(true);
        } else {
            messageEdit.setVisibility(View.VISIBLE);
            messageEdit.setError(null);
            sent.setVisibility(View.VISIBLE);
            setSeenCount(false);
        }

        if (statusDatas.get(counter).mType.contains("video")) {
            statusImage.setVisibility(View.GONE);
        } else {
            imageProgressBar.setIndeterminate(true);
            imageProgressBar.setVisibility(View.VISIBLE);
        }
        setStatus(statusDatas.get(counter).mType, statusDatas.get(counter).mAttachment);

    }

    public void setPlayer() {
        // Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(StoryActivity.this).build();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory();
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

    public void releasePlayer() {
        if (simpleExoPlayerView != null) {
            simpleExoPlayerView.getPlayer().setPlayWhenReady(false);
            simpleExoPlayerView.getPlayer().stop();
            simpleExoPlayerView.getPlayer().release();

            if (player != null) {
                player.clearVideoSurface();
                player.release();
            }
            if (videoSource != null) {
                videoSource = null;
            }
        }
    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    private void networkSnack() {
        Snackbar snackbar = Snackbar
                .make(mainLay, getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    private void setStatus(String type, String name) {
        isStoryPlaying = false;
        if (!statusDatas.get(counter).mMessage.equals("")) {
            statusMsg.setVisibility(View.VISIBLE);
            statusMsg.setText(statusDatas.get(counter).mMessage);
            statusMsg.post(new Runnable() {
                @Override
                public void run() {
                    if (statusMsg != null && statusMsg.length() > 70) {
                        addReadMore(statusMsg.getText().toString(), statusMsg);
                    }
                }
            });
        } else {
            statusMsg.setVisibility(View.GONE);
        }

        if (way.equalsIgnoreCase("ownstory")) {
            if (type.equalsIgnoreCase("video")) {
                if (storageManager.checkStatusExists(name, type, StorageManager.TAG_VIDEO_SENT)) {
                    try {
                        File file = new File(storageManager.getFilePath(StorageManager.TAG_VIDEO_SENT, name));
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(StoryActivity.this, Uri.fromFile(file));
                        long METADATA_KEY_DURATION = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                        retriever.release();
                        storyStatusView.setStoryDuration(METADATA_KEY_DURATION);
                        playVideoInLocal(file.getAbsolutePath());
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(StoryActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(StoryActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                }
            } else {
                if (storageManager.checkStatusExists(name, type, StorageManager.TAG_SENT)) {
                    File file = new File(storageManager.getFilePath(StorageManager.TAG_SENT, name));
                    storyStatusView.setStoryDuration(Constants.storyDuration);
                    Log.v(TAG, "file=" + file.getAbsolutePath());
                    Glide.with(getApplicationContext()).load(Uri.fromFile(file)).thumbnail(0.5f)
                            .into(statusImage);
                    imageProgressBar.setVisibility(View.GONE);
                    resumeStories();
                } else {
                    Toast.makeText(StoryActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                    storyStatusView.skip();
                }
            }
        } else {
            if (type.equalsIgnoreCase("video")) {
                if (storageManager.checkStatusExists(name, type, Constants.TAG_STATUS)) {
                    Log.i(TAG, "setStatus: " + name);
                    File file = new File(storageManager.getFilePath(Constants.TAG_STATUS, name));
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(StoryActivity.this, Uri.fromFile(file));
                    String hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
                    boolean isVideo = "yes".equals(hasVideo);
                    if (isVideo) {
                        try {
                            long METADATA_KEY_DURATION = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                            retriever.release();
                            storyStatusView.setStoryDuration(METADATA_KEY_DURATION);
                            playVideoInLocal(file.getAbsolutePath());
                            AddSeenToStory();
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(StoryActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    } else {
                        storageManager.deleteFile(file.getAbsolutePath());
                        downloadFile(name);
                    }
                } else {
                    if (isNetworkConnected().equals(NOT_CONNECT)) {
                        networkSnack();
                    } else {
                        try {
                            downloadFile(name);
                        } catch (Exception e) {
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
                        Glide.with(getApplicationContext()).load(file).thumbnail(0.5f)
                                .transition(new DrawableTransitionOptions().crossFade())
                                .into(statusImage);
                        AddSeenToStory();
                        imageProgressBar.setVisibility(View.GONE);
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(StoryActivity.this, WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(StoryActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, 100);
                    } else {
                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                            networkSnack();
                        } else {
                            pauseDelay();
                            ImageDownloader imageDownloader = new ImageDownloader(StoryActivity.this) {
                                @Override
                                protected void onPostExecute(Bitmap imgBitmap) {
                                    if (imgBitmap == null) {
                                        Log.v("bitmapFailed", "bitmapFailed");
                                        Toast.makeText(StoryActivity.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.v("onBitmapLoaded", "onBitmapLoaded");
                                        try {
                                            String status = storageManager.saveToSdCard(imgBitmap, Constants.TAG_STATUS, name);
                                            if (status.equals("success")) {
                                                storyStatusView.setStoryDuration(Constants.storyDuration);
                                                File thumbFile = new File(storageManager.getFilePath(Constants.TAG_STATUS, name));
                                                Glide.with(getApplicationContext()).load(thumbFile)
                                                        .into(statusImage);
                                                AddSeenToStory();
                                                if (storyStatusView.isPaused(counter)) {
                                                    resumeStories();
                                                } else {
                                                    storyStatusView.startStories();
                                                }
                                                imageProgressBar.setVisibility(View.GONE);
//                                                resumeInDownloadImage();
                                            } else {
                                                Toast.makeText(StoryActivity.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
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

    private void downloadFile(String fileName) {
        pauseDelay();
        if (!storyStatusView.isPaused(counter)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    storyStatusView.pause();
                }
            });
        }
        DownloadFiles downloadFiles = new DownloadFiles(StoryActivity.this) {
            @Override
            protected void onPostExecute(String downPath) {
                Log.i(TAG, "onPostExecute: " + downPath);
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(StoryActivity.this, Uri.fromFile(new File(downPath)));
                long METADATA_KEY_DURATION = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                retriever.release();
                storyStatusView.setStoryDuration(METADATA_KEY_DURATION);
                if (storyStatusView.isPaused(counter)) {
                    resumeStories();
                } else {
                    storyStatusView.startStories();
                }
                isStoryPlaying = true;
                playVideoInLocal(downPath);
                AddSeenToStory();
            }
        };
        downloadFiles.execute(Constants.CHAT_IMG_PATH + fileName, Constants.TAG_STATUS);
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

    public void playVideoInLocal(String url) {

        currentPlaybackPosition = 0;
        previewFilePath = url;
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
                                Log.e(TAG, "STATE_BUFFERING");
                            } else if (playbackState == Player.STATE_ENDED) {
                                Log.e(TAG, "STATE_ENDED");
                            } else if (playbackState == Player.STATE_IDLE) {  //state_idle will call when player stop and release
                                Log.e(TAG, "STATE_IDLE");
                            } else {
                                if (!isStoryPlaying) {
                                    isStoryPlaying = true;
                                    resumeStories();
                                }
                                imageProgressBar.setVisibility(View.INVISIBLE); //state else will call when player is completed
                                simpleExoPlayerView.setVisibility(View.VISIBLE);
                                statusImage.setVisibility(View.GONE);
                                edtLay.setVisibility(View.VISIBLE);
                                if (way.equalsIgnoreCase("ownstory")) {
                                    messageEdit.setVisibility(View.GONE);
                                    sent.setVisibility(View.GONE);
                                    setSeenCount(true);
                                } else {
                                    messageEdit.setVisibility(View.VISIBLE);
                                    messageEdit.setError(null);
                                    sent.setVisibility(View.VISIBLE);
                                    setSeenCount(false);
                                }
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
                    Log.e(TAG, "playVideoInLocal: " + e.getMessage());
                }
            }
        });

        isCreate = true;
    }

    void playVideo() {
        if (player != null) {
            if (!isReadMore) {
                resumeView();
                player.setPlayWhenReady(true);
            } else {
                pauseView();
                player.setPlayWhenReady(false);
            }
        }
    }

    public void setUserData(ContactsData.Result data) {
        userTxt.setText(data.user_name);
        timeTxt.setText(ApplicationClass.getStatusTime(StoryActivity.this, Long.parseLong(statusDatas.get(counter).mStatusTime), false));

        /*if(!statusDatas.get(counter).mMessage.equals("")){
            statusMsg.setVisibility(View.VISIBLE);
            statusMsg.setText(statusDatas.get(counter).mMessage);
            statusMsg.post(new Runnable() {
                @Override
                public void run() {
                    if (statusMsg != null && statusMsg.length() > 70) {
                        addReadMore(statusMsg.getText().toString(), statusMsg);
                    }
                }
            });
        }*/

        if (!data.blockedme.equals("block")) {
            DialogActivity.setProfileImage(data, userImg, getApplicationContext());
        } else {
            Glide.with(getApplicationContext()).load(R.drawable.temp)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                    .into(userImg);
        }
    }

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
    public void onStoryViewed(String statusId) {
        if (statusDatas.get(counter).mStatusId.equals(statusId)) {
            viewedContacts = dbHelper.getViewedContact(getApplicationContext(), statusId);
            viewPeopleAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDeleteStatus(String statusId) {
        dbHelper.deleteStatus(statusId);
        if (statusDatas.get(counter).mStatusId.equals(statusId)) {
            releasePlayer();
            finish();
        }
    }

    private void blockChatConfirmDialog(String userId) {
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
        } else if (statusList.get(pos).blockedbyme.equals("block")) {
            blockChatConfirmDialog(statusList.get(pos).user_id);
        } else {
            if (messageEdit.getText().toString().trim().length() > 0) {
                String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                String textMsg = messageEdit.getText().toString().trim();
                String chatId = GetSet.getUserId() + statusList.get(pos).user_id;
                RandomString randomString = new RandomString(10);
                String messageId = GetSet.getUserId() + randomString.nextString();
                try {
                    if (!statusList.get(pos).blockedme.equals("block")) {
                        JSONObject jobj = new JSONObject();
                        JSONObject message = new JSONObject();
                        message.put(Constants.TAG_USER_ID, GetSet.getUserId());
                        message.put(Constants.TAG_USER_NAME, GetSet.getUserName());
                        message.put(Constants.TAG_MESSAGE_TYPE, "story");
                        message.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(textMsg));
                        message.put(Constants.TAG_CHAT_TIME, unixStamp);
                        message.put(Constants.TAG_CHAT_ID, chatId);
                        message.put(Constants.TAG_MESSAGE_ID, messageId);
                        message.put(Constants.TAG_RECEIVER_ID, statusList.get(pos).user_id);
                        message.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                        message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_SINGLE);
                        jobj.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                        jobj.put(Constants.TAG_RECEIVER_ID, statusList.get(pos).user_id);


                        JSONObject statusObj = new JSONObject();
                        statusObj.put(Constants.TAG_ATTACHMENT, statusDatas.get(counter).mAttachment);
                        statusObj.put(Constants.TAG_THUMBNAIL, statusDatas.get(counter).mThumbnail);
                        statusObj.put(Constants.TAG_STORY_TYPE, statusDatas.get(counter).mType);
                        statusObj.put(Constants.TAG_MESSAGE, statusDatas.get(counter).mMessage);
                        statusObj.put(Constants.TAG_STORY_ID, statusDatas.get(counter).mStatusId);

                        message.put(Constants.TAG_STATUS_DATA, ApplicationClass.encryptMessage(statusObj.toString()));

                        jobj.put("message_data", message);

                        Log.v("startchat", "startchat=" + jobj + "\n" + statusObj);
                        socketConnection.startChat(jobj);
                        dbHelper.addMessageDatas(chatId, messageId, GetSet.getUserId(), GetSet.getUserName(),
                                "story", ApplicationClass.encryptMessage(textMsg), "", ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""),
                                ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""),
                                unixStamp, statusList.get(pos).user_id, GetSet.getUserId(), "", "", ApplicationClass.encryptMessage(statusObj.toString()));
                        dbHelper.addRecentMessages(chatId, statusList.get(pos).user_id, messageId, unixStamp, "0");

                        Toast.makeText(StoryActivity.this, getString(R.string.sending_message), Toast.LENGTH_SHORT).show();
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

    public void AddSeenToStory() {
        try {
            if (statusDatas.get(counter).mIsSeen.equals("0")) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                    jsonObject.put(Constants.TAG_RECEIVER_ID, statusDatas.get(counter).mSenderId);
                    jsonObject.put(Constants.TAG_STORY_ID, statusDatas.get(counter).mStatusId);
                    Log.v(TAG, "viewStory: " + jsonObject);
                    socketConnection.viewStory(jsonObject);
                    dbHelper.updateStatusSeen(statusDatas.get(counter).mStatusId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        keyboardHeightProvider.setKeyboardHeightObserver(this);
        if (player != null)
            player.setPlayWhenReady(true);
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
        // Very important !
        keyboardHeightProvider.close();
        storyStatusView.destroy();
        super.onDestroy();
        if (simpleExoPlayerView.getPlayer() != null) {
            simpleExoPlayerView.getPlayer().setPlayWhenReady(false);
            simpleExoPlayerView.getPlayer().stop();
            simpleExoPlayerView.getPlayer().release();
        }

    }

    public void stopVideoView() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                simpleExoPlayerView.setVisibility(View.INVISIBLE);
            }
        });
    }

    /**
     * To open status viewed dialog
     */

    private void viewDialog() {
        View bottomView = getLayoutInflater().inflate(R.layout.status_viewed_dialog, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(bottomView);

        RecyclerView viewedRecyclerView = bottomView.findViewById(R.id.viewedRecyclerView);
        ImageView deleteStory = bottomView.findViewById(R.id.deleteStory);
        TextView viewCount = bottomView.findViewById(R.id.viewCount);
        viewedContacts = dbHelper.getViewedContact(getApplicationContext(), statusDatas.get(counter).mStatusId);
        viewPeopleAdapter = new ViewPeopleAdapter(viewedContacts, getApplicationContext());
        viewedRecyclerView.setAdapter(viewPeopleAdapter);

        viewCount.setText(getString(R.string.viewed_by) + " " + viewedContacts.size());

        dialog.show();
        deleteStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                socketConnection.deleteStory(statusDatas.get(counter).mStatusId);
                dbHelper.deleteStatus(statusDatas.get(counter).mStatusId);
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

    @Override
    public void onKeyboardHeightChanged(int height, int orientation) {
        // color the keyboard height view, this will remain visible when you close the keyboard
        if (height > 0) {
            if (statusMsg != null) {
                statusMsg.setVisibility(View.GONE);
            }
            initBottomPadding(bottomNavHeight);
        } else if (height < 0) {
            if (statusMsg != null) {
                if (!TextUtils.isEmpty(statusMsg.getText())) {
                    statusMsg.setVisibility(View.VISIBLE);
                } else {
                    statusMsg.setVisibility(View.GONE);
                }
            }
            initBottomPadding(bottomNavHeight + bottomMargin);
        } else {
            if (!TextUtils.isEmpty(statusMsg.getText())) {
                statusMsg.setVisibility(View.VISIBLE);
            } else {
                statusMsg.setVisibility(View.GONE);
            }
            initBottomPadding(bottomNavHeight + bottomMargin);
        }
    }

    private void setToImmersiveMode() {
        // set to immersive
       /* getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);*/
    }

    private void initTopLayPadding(int topMargin) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ApplicationClass.dpToPx(StoryActivity.this, 2));
        layoutParams.topMargin = topMargin + ApplicationClass.dpToPx(StoryActivity.this, 5);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        storyStatusView.setLayoutParams(layoutParams);
    }

    private void initBottomPadding(int bottomPadding) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        if (bottomLay != null) {
            bottomLay.setPadding(0, 0, 0, bottomPadding);
            bottomLay.setLayoutParams(params);
        }
    }

    private static final class TransformerItem {

        final String title;
        final Class<? extends ViewPager.PageTransformer> clazz;

        public TransformerItem(Class<? extends ViewPager.PageTransformer> clazz) {
            this.clazz = clazz;
            title = clazz.getSimpleName();
        }

        @Override
        public String toString() {
            return title;
        }

    }

    public class ViewPagerAdapter extends PagerAdapter {

        List<ContactsData.Result> status;
        private Context mContext;

        public ViewPagerAdapter(Context context, List<ContactsData.Result> listDate) {
            mContext = context;
            status = listDate;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            if (way.equalsIgnoreCase("ownstory"))
                return 1;
            else {
                if (statusList.get(0).user_id.equalsIgnoreCase(GetSet.getUserId())) {
                    return status.size() - 1;
                } else return status.size();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.pager_item, container, false);
            view.setTag(position + "pos");

            isCreate = false;

            ImageView imageview = view.findViewById(R.id.image);
            imageview.setVisibility(View.VISIBLE);

            container.addView(view);
            return view;
        }
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
            holder.message.setText(ApplicationClass.getStatusTime(context, Long.parseLong(map.get(Constants.TAG_STATUS_TIME)), false));
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

    private String getFileName(String url) {
        String imgSplit = url;
        int endIndex = imgSplit.lastIndexOf("/");
        if (endIndex != -1) {
            imgSplit = imgSplit.substring(endIndex + 1, imgSplit.length());
        }
        return imgSplit;
    }

}