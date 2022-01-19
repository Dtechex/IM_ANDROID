package com.loopytime.im;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.devlomi.record_view.OnRecordListener;
import com.devlomi.record_view.RecordButton;
import com.devlomi.record_view.RecordView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.loopytime.external.EndlessRecyclerOnScrollListener;
import com.loopytime.external.ImagePicker;
import com.loopytime.external.ProgressWheel;
import com.loopytime.external.RandomString;
import com.loopytime.external.RecyclerItemClickListener;
import com.loopytime.external.videotrimmer.utils.GoogleVoiceTypingDisabledException;
import com.loopytime.external.videotrimmer.utils.Screen;
import com.loopytime.external.videotrimmer.utils.Speech;
import com.loopytime.external.videotrimmer.utils.SpeechDelegate;
import com.loopytime.external.videotrimmer.utils.SpeechRecognitionNotAvailable;
import com.loopytime.external.videotrimmer.utils.SpeechUtil;
import com.loopytime.external.videotrimmer.view.SpeechProgressView;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.DownloadFiles;
import com.loopytime.helper.FileUploadService;
import com.loopytime.helper.ImageCompression;
import com.loopytime.helper.ImageDownloader;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.StorageManager;
import com.loopytime.helper.Utils;
import com.loopytime.model.AdminChannel;
import com.loopytime.model.AdminChannelMsg;
import com.loopytime.model.ChannelMessage;
import com.loopytime.model.ChannelResult;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.shadow.apache.commons.lang3.LocaleUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;
import droidninja.filepicker.models.sort.SortingTypes;
import droidninja.filepicker.utils.Orientation;
import jp.wasabeef.glide.transformations.BlurTransformation;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.im.ChannelChatActivity.MessageListAdapter.VIEW_TYPE_DATE;
import static com.loopytime.utils.Constants.TAG_GROUP_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER_ID;
import static com.loopytime.utils.Constants.TRUE;

public class ChannelChatActivity extends BaseActivity implements View.OnClickListener,
        TextWatcher, SocketConnection.ChannelChatCallbackListener, DeleteAdapter.deleteListener, SpeechDelegate {
    public static final int ACTIVITY_RECORD_SOUND = 0;
    public static String tempChannelId = "";
    EditText editText;
    String channelId;
    TextToSpeech ttobj;
    private final int PERMISSIONS_REQUEST = 19876;
    FloatingActionButton voiceFab;
    View bottomLayout, fabLay, linearLayout;
    boolean isBound = false;
    FloatingWidgetService myService;
    public boolean isSppechEnable = false;
    boolean isPermissionCall = false;
    int retry = 1;

    List<ChannelMessage> messagesList = new ArrayList<>();
    String TAG = this.getClass().getSimpleName();
    RecyclerView recyclerView;
    TextView username, online, txtMembers, txtBlocked, audioTime;
    RelativeLayout chatUserLay, mainLay, attachmentsLay, imageViewLay, bottomLay, forwordLay;
    ImageView attachbtn, optionbtn, backbtn, send, audioCallBtn, videoCallBtn, cameraBtn, closeBtn,
            galleryBtn, fileBtn, audioBtn, locationBtn, contactBtn, imageView, forwordBtn, copyBtn, deleteBtn;
    CircleImageView userimage;
    Display display;
    boolean visible, stopLoading = false, meTyping, chatLongPressed = false;
    int totalMsg;
    SocketConnection socketConnection;
    LinearLayoutManager linearLayoutManager;
    MessageListAdapter messageListAdapter;
    DatabaseHandler dbhelper;
    StorageManager storageManager;
    ApiInterface apiInterface;
    ArrayList<String> pathsAry = new ArrayList<>();
    Handler handler = new Handler();
    Runnable runnable;
    EndlessRecyclerOnScrollListener endlessRecyclerOnScrollListener;
    ChannelResult.Result channelData;
    ArrayList<ChannelMessage> selectedChatPos = new ArrayList<>();
    String channelAdminId = null;
    String recordVoicePath = null;
    MediaRecorder mediaRecorder;
    RecordView recordView;
    RecordButton recordButton;
    MediaPlayer mediaPlayer = null;
    boolean firstFlag = true;
    boolean checkFirst = true;
    LinearLayout editLay, excryptText;
    int playingPosition = -1;
    private boolean isFromNotification;
    private long time;
    private SeekBar seekBar;
    private MediaPlayer player = new MediaPlayer();
    private Handler seekHandler = new Handler();
    private Runnable moveSeekBarThread = new Runnable() {
        public void run() {
            if (player.isPlaying()) {

                long currentDuration = player.getCurrentPosition();
                audioTime.setText(milliSecondsToTimer(currentDuration));
                int mediaPos_new = player.getCurrentPosition();
                int mediaMax_new = player.getDuration();
                seekBar.setMax(mediaMax_new);
                seekBar.setProgress(mediaPos_new);
                seekHandler.postDelayed(this, 100);
            }
        }
    };

    public static boolean isVideo(String mimeType) {
        Log.v("mimeType", "mimeType=" + mimeType);
        return mimeType != null && mimeType.startsWith("video");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        if (getIntent().getStringExtra("notification") != null) {
            Constants.isChannelChatOpened = true;
            isFromNotification = true;
            /*NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancelAll();
            }*/
        }
        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        detailsIntent.setPackage("com.google.android.googlequicksearchbox");
        sendOrderedBroadcast(
                detailsIntent, null, new LanguageDetailsChecker(), null, 1234, null, null);
        setSpeekProgress();
        if (Constants.channelContext != null && Constants.isChannelChatOpened) {
            ((Activity) Constants.channelContext).finish();
        }
        Constants.channelContext = this;
        getWindow().setBackgroundDrawableResource(R.drawable.chat_bg);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        send = findViewById(R.id.send);
        editText = findViewById(R.id.editText);
        chatUserLay = findViewById(R.id.chatUserLay);
        userimage = findViewById(R.id.userImg);
        username = findViewById(R.id.userName);
        txtMembers = findViewById(R.id.txtMembers);
        online = findViewById(R.id.online);
        attachbtn = findViewById(R.id.attachbtn);
        bottomLayout = findViewById(R.id.speech);
        bottomLayout.setVisibility(View.GONE);
        recordButton = findViewById(R.id.record_button);
        voiceFab = findViewById(R.id.fabSpeak);
        voiceFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "11");
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "voice typing Channel");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Activity");
                ApplicationClass.getInstance().mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                speak();
            }
        });
        fabLay = findViewById(R.id.fabLay);
        linearLayout = findViewById(R.id.linearLayout);
        recordView = findViewById(R.id.record_view);
        bottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }});

        audioCallBtn = findViewById(R.id.audioCallBtn);
        videoCallBtn = findViewById(R.id.videoCallBtn);
        optionbtn = findViewById(R.id.optionbtn);
        backbtn = findViewById(R.id.backbtn);
        bottomLay = findViewById(R.id.bottom);
        mainLay = findViewById(R.id.mainLay);
        attachmentsLay = findViewById(R.id.attachmentsLay);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        fileBtn = findViewById(R.id.fileBtn);
        audioBtn = findViewById(R.id.audioBtn);
        locationBtn = findViewById(R.id.locationBtn);
        contactBtn = findViewById(R.id.contactBtn);
        imageViewLay = findViewById(R.id.imageViewLay);
        imageView = findViewById(R.id.imageView);
        closeBtn = findViewById(R.id.closeBtn);
        forwordLay = findViewById(R.id.forwordLay);
        forwordBtn = findViewById(R.id.forwordBtn);
        copyBtn = findViewById(R.id.copyBtn);
        deleteBtn = findViewById(R.id.deleteBtn);
        txtBlocked = findViewById(R.id.txtBlocked);
        editLay = findViewById(R.id.editLay);
        recordView = findViewById(R.id.record_view);
        recordButton = findViewById(R.id.record_button);
        excryptText = findViewById(R.id.excryptText);

        if (ApplicationClass.isRTL()) {
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        socketConnection = SocketConnection.getInstance(this);
        SocketConnection.getInstance(this).setChannelChatCallbackListener(this);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        dbhelper = DatabaseHandler.getInstance(this);
        storageManager = StorageManager.getInstance(this);
        display = getWindowManager().getDefaultDisplay();

        channelId = getIntent().getStringExtra(Constants.TAG_CHANNEL_ID);
        channelData = dbhelper.getChannelInfo(channelId);

        /*NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(channelData.channelName, 0);
            notificationManager.cancel("New Channel", 0);
        }*/

        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        // set visibility status
        chatUserLay.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        audioCallBtn.setVisibility(View.GONE);
        videoCallBtn.setVisibility(View.GONE);
        optionbtn.setVisibility(View.VISIBLE);
        txtMembers.setVisibility(View.VISIBLE);

        totalMsg = dbhelper.getChannelMessagesCount(channelId);
        Log.v("totalMsg", "totalMsg=" + totalMsg);

        messagesList.addAll(getMessagesAry(dbhelper.getChannelMessages(channelId, "0", "20"), null));
        showEncryptionText();
        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        messageListAdapter = new MessageListAdapter(this, messagesList);
        recyclerView.setAdapter(messageListAdapter);

        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.scrollToPosition(0);
            }
        });

        DividerItemDecoration divider = new DividerItemDecoration(recyclerView.getContext(),
                linearLayoutManager.getOrientation());
        divider.setDrawable(getResources().getDrawable(R.drawable.emptychat_divider));
        recyclerView.addItemDecoration(divider);

        send.setOnClickListener(this);
        backbtn.setOnClickListener(this);
        attachbtn.setOnClickListener(this);
        optionbtn.setOnClickListener(this);
        userimage.setOnClickListener(this);
        audioCallBtn.setOnClickListener(this);
        videoCallBtn.setOnClickListener(this);
        cameraBtn.setOnClickListener(this);
        galleryBtn.setOnClickListener(this);
        fileBtn.setOnClickListener(this);
        audioBtn.setOnClickListener(this);
        locationBtn.setOnClickListener(this);
        contactBtn.setOnClickListener(this);
        editText.addTextChangedListener(this);
        chatUserLay.setOnClickListener(this);
        closeBtn.setOnClickListener(this);
        copyBtn.setOnClickListener(this);
        forwordBtn.setOnClickListener(this);
        deleteBtn.setOnClickListener(this);

        setVoiceRecorder();
        whileViewChat();

        endlessRecyclerOnScrollListener = new EndlessRecyclerOnScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.v("current_page", "current_page=" + page + "&totalItems=" + totalItemsCount);
                final List<ChannelMessage> tmpList = new ArrayList<>(dbhelper.getChannelMessages(channelId, String.valueOf(page * 20), "20"));
                if (tmpList.size() == 0 && !stopLoading) {
                    stopLoading = true;
                    messagesList.addAll(getMessagesAry(tmpList, messagesList.get(messagesList.size() - 1)));
                } else {
                    messagesList.addAll(getMessagesAry(tmpList, null));
                }
                showEncryptionText();
                Log.v("current_page", "messagesList=" + messagesList.size());
                recyclerView.post(new Runnable() {
                    public void run() {
                        messageListAdapter.notifyDataSetChanged();
                    }
                });
            }
        };
        recyclerView.addOnScrollListener(endlessRecyclerOnScrollListener);

        recyclerView.addOnItemTouchListener(chatItemClick(this, recyclerView));
    }

    private void setVoiceRecorder() {

        recordView.setCounterTimeColor(Color.parseColor("#a3a3a3"));
        recordView.setSmallMicColor(ContextCompat.getColor(ChannelChatActivity.this, R.color.colorAccent));
        recordView.setSlideToCancelTextColor(Color.parseColor("#a3a3a3"));
        recordView.setLessThanSecondAllowed(false);
        recordView.setSlideToCancelText(getString(R.string.slide_to_cancel));
        recordView.setCustomSounds(0, 0, 0);

        recordButton.setRecordView(recordView);


        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else {
            recordView.setOnRecordListener(new OnRecordListener() {
                @Override
                public void onStart() {
                    if (ContextCompat.checkSelfPermission(ChannelChatActivity.this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(ChannelChatActivity.this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        recordView.setOnRecordListener(null);
                        ActivityCompat.requestPermissions(ChannelChatActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, 111);
                    } else {
                        startVoice();
                    }
                }

                @Override
                public void onCancel() {
                    editText.requestFocus();
//                Toast.makeText(ChatActivity.this, "onCancel"+time, Toast.LENGTH_SHORT).show();
                    Log.d("RecordView", "onCancel");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ApplicationClass.hideSoftKeyboard(ChannelChatActivity.this, recordView);
                            editLay.setVisibility(View.VISIBLE);
                            recordView.setVisibility(View.GONE);
                        }
                    }, 1000);

                }

                @Override
                public void onFinish(long recordTime) {
                    if (recordTime > 1000) {

                        editText.requestFocus();
                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                            networkSnack();
                        } else {
                            if (null != mediaRecorder) {
                                try {
                                    mediaRecorder.stop();
                                } catch (RuntimeException ex) {
                                }
                            }

                            editLay.setVisibility(View.VISIBLE);
                            recordView.setVisibility(View.GONE);

                            ChannelMessage mdata = updateDBList("audio", "", recordVoicePath/*,String.valueOf(getAudioTime(Time))*/);
                            Intent service = new Intent(ChannelChatActivity.this, FileUploadService.class);
                            Bundle b = new Bundle();
                            b.putSerializable("mdata", mdata);
                            b.putString("filepath", recordVoicePath);
                            b.putString("chatType", Constants.TAG_CHANNEL);
                            service.putExtras(b);
                            startService(service);
                            Log.d("RecordView", "onFinish");
                        }
                    } else {
                        stopMedia();
                        editLay.setVisibility(View.VISIBLE);
                        recordView.setVisibility(View.GONE);
                        ApplicationClass.hideSoftKeyboard(ChannelChatActivity.this, recordView);

                        Toast.makeText(ChannelChatActivity.this, getString(R.string.less_than_second), Toast.LENGTH_SHORT).show();
                    }

                }

                @Override
                public void onLessThanSecond() {
                    stopMedia();
                    editLay.setVisibility(View.VISIBLE);
                    recordView.setVisibility(View.GONE);
                    ApplicationClass.hideSoftKeyboard(ChannelChatActivity.this, recordView);

                    Toast.makeText(ChannelChatActivity.this, getString(R.string.less_than_second), Toast.LENGTH_SHORT).show();
                    Log.d("RecordView", "onLessThanSecond");
                }

            });
        }
    }

    private void startVoice() {
        if (visible) {
            TransitionManager.beginDelayedTransition(bottomLay);
            visible = !visible;
            attachmentsLay.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        stopMedia();
        Log.d("RecordView", "onStart");
        editLay.setVisibility(View.GONE);
        recordView.setVisibility(View.VISIBLE);
        if (ContextCompat.checkSelfPermission(ChannelChatActivity.this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(ChannelChatActivity.this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ChannelChatActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, 111);
        } else if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else {
            String fileName = (getString(R.string.app_name) + "_" + System.currentTimeMillis() + ".MP3").replaceAll(" ", "");

            storageManager.createDirectory(StorageManager.TAG_AUDIO_SENT);

            recordVoicePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + storageManager.getFolderPath(StorageManager.TAG_AUDIO_SENT) + fileName;
            MediaRecorderReady();
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                time = System.currentTimeMillis() / (1000);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void MediaRecorderReady() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(recordVoicePath);
    }

    private void getChannelInfo(String channelId) {
        channelData = dbhelper.getChannelInfo(channelId);
        setUI(channelData);
        if (NetworkReceiver.isConnected()) {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(channelId);
            Call<ChannelResult> call = apiInterface.getChannelInfo(GetSet.getToken(), jsonArray);
            call.enqueue(new Callback<ChannelResult>() {
                @Override
                public void onResponse(Call<ChannelResult> call, Response<ChannelResult> response) {
                    try {
                        Log.i(TAG, "getChannelInfo: " + new JSONObject("" + response.body()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                        for (ChannelResult.Result result : response.body().result) {
                            dbhelper.updateChannelInfo(result.channelId, result.channelName, result.channelDes, result.channelImage,
                                    result.channelType != null ? result.channelType : Constants.TAG_PUBLIC, result.channelAdminId != null ? result.channelAdminId : "", result.channelAdminName, result.totalSubscribers, result.blockStatus);
                            if (channelId.equalsIgnoreCase(result.channelId)) {
                                channelData = dbhelper.getChannelInfo(result.channelId);
                                setUI(channelData);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<ChannelResult> call, Throwable t) {
                    Log.e(TAG, "getChannelInfo: " + t.getMessage());
                    call.cancel();
                    channelData = dbhelper.getChannelInfo(channelId);
                    setUI(channelData);
                }
            });
        }
    }

    @Override
    public void onNetworkChange(boolean isConnected) {
//        if (isConnected) {
//            online.setVisibility(View.VISIBLE);
//        } else {
//            online.setVisibility(View.GONE);
//        }
    }

    private List<ChannelMessage> getMessagesAry(List<ChannelMessage> tmpList, ChannelMessage lastData) {
        List<ChannelMessage> msgList = new ArrayList<>();
        if (tmpList.size() == 0 && lastData != null) {
            ChannelMessage channelMessage = new ChannelMessage();
            channelMessage.messageType = "date";
            channelMessage.chatTime = lastData.chatTime;
            msgList.add(channelMessage);
            Log.v("diff", "diff pos=ss" + "&msg=" + lastData.message);
        } else {
            for (int i = 0; i < tmpList.size(); i++) {
                Calendar cal1 = Calendar.getInstance();
                cal1.setTimeInMillis(Long.parseLong(tmpList.get(i).chatTime) * 1000L);

                if (i + 1 < tmpList.size()) {
                    Calendar cal2 = Calendar.getInstance();
                    cal2.setTimeInMillis(Long.parseLong(tmpList.get(i + 1).chatTime) * 1000L);

                    boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);

                    if (sameDay) {
                        msgList.add(tmpList.get(i));
                        Log.v("diff", "same pos=" + i + "&msg=" + tmpList.get(i).message);
                    } else {
                        msgList.add(tmpList.get(i));
                        ChannelMessage channelMessage = new ChannelMessage();
                        channelMessage.messageType = "date";
                        channelMessage.chatTime = tmpList.get(i).chatTime;
                        msgList.add(channelMessage);
                        Log.v("diff", "diff pos=" + i + "&msg=" + tmpList.get(i).message);
                    }
                } else {
                    msgList.add(tmpList.get(i));
                }
            }
        }
        return msgList;
    }

    private void whileViewChat() {
        dbhelper.updateChannelMessageReadStatus(channelId);
        dbhelper.resetUnseenChannelMessagesCount(channelId);
        dbhelper.updateChannelReadData(channelId, Constants.TAG_DELIVERY_STATUS, "read");
    }

    @Override
    public void onChannelChatReceive(ChannelMessage result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "onChannelChatReceive: " + new Gson().toJson(result));
                result.message = ApplicationClass.decryptMessage(result.message);
                result.attachment = ApplicationClass.decryptMessage(result.attachment);
                result.lat = ApplicationClass.decryptMessage(result.lat);
                result.lon = ApplicationClass.decryptMessage(result.lon);
                result.contactName = ApplicationClass.decryptMessage(result.contactName);
                result.contactPhoneNo = ApplicationClass.decryptMessage(result.contactPhoneNo);
                result.contactCountryCode = ApplicationClass.decryptMessage(result.contactCountryCode);

                if (result.channelId.equalsIgnoreCase(channelId)) {

                    whileViewChat();
                    if (!result.messageType.equals(Constants.TAG_ISDELETE)) {
                        messagesList.add(0, result);
                        updatePosition();
                        showEncryptionText();
                        if (messageListAdapter != null) {
                            messageListAdapter.notifyItemInserted(0);
                            recyclerView.smoothScrollToPosition(0);
                        }
                        if (result.messageType.equalsIgnoreCase("subject") || result.messageType.equalsIgnoreCase("channel_image")) {
                            username.setText(result.channelName);
                            Glide.with(ChannelChatActivity.this).load(Constants.CHANNEL_IMG_PATH + result.attachment)
                                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                                    .into(userimage);
                        }
                    } else {
                        for (ChannelMessage data : messagesList) {
                            if (data.messageId != null && result.messageId != null &&
                                    data.messageId.equals(result.messageId)) {
                                data.messageType = result.messageType;
                                messageListAdapter.notifyItemChanged(messagesList.indexOf(data));
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

//    @Override
//    public void onChannelBlocked(String channelId) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                channelData = dbhelper.getChannelInfo(channelId);
//                setUI(channelData);
//            }
//        });
//    }

    @Override
    public void onAdminChatReceive(AdminChannelMsg.Result result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (result.channelId.equalsIgnoreCase(channelId)) {
                    ChannelMessage channelMessage = new ChannelMessage();
                    channelMessage.channelId = result.channelId;
                    channelMessage.channelName = channelData.channelName != null ? channelData.channelName : "";
                    channelMessage.chatTime = result.chatTime;
                    channelMessage.message = result.message;
                    channelMessage.messageId = result.messageId;
                    channelMessage.messageType = result.messageType;
                    channelMessage.thumbnail = result.thumbnail;
                    channelMessage.attachment = result.attachment;
                    messagesList.add(0, channelMessage);
                    updatePosition();
                    showEncryptionText();
                    if (messageListAdapter != null) {
                        messageListAdapter.notifyItemInserted(0);
                        recyclerView.smoothScrollToPosition(0);
                    }

                    whileViewChat();
                }

            }
        });
    }

    @Override
    public void onUploadListen(final String message_id, final String attachment, final String progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < messagesList.size(); i++) {
                    if (message_id.equals(messagesList.get(i).messageId)) {
                        Log.v("checkChat", "onPostExecute");
                        messagesList.get(i).attachment = attachment;
                        messagesList.get(i).progress = progress;
                        messageListAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void onGetUpdateFromDB() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int currentCount = dbhelper.getChannelMessagesCount(channelId);
                if (totalMsg != currentCount) {
                    messagesList.clear();
                    if (endlessRecyclerOnScrollListener != null) {
                        endlessRecyclerOnScrollListener.resetState();
                    }
                    messagesList.addAll(getMessagesAry(dbhelper.getChannelMessages(channelId, "0", "20"), null));
                    showEncryptionText();
                    messageListAdapter.notifyDataSetChanged();
                    recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.scrollToPosition(0);
                        }
                    });

                    whileViewChat();
                } else if (isFromNotification) {
                    messagesList.clear();
                    if (endlessRecyclerOnScrollListener != null) {
                        endlessRecyclerOnScrollListener.resetState();
                    }
                    messagesList.addAll(getMessagesAry(dbhelper.getChannelMessages(channelId, "0", "20"), null));
                    showEncryptionText();
                    messageListAdapter.notifyDataSetChanged();
                    recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.scrollToPosition(0);
                        }
                    });

                    whileViewChat();

                }
            }
        });
    }

    @Override
    public void onChannelBlocked(String channelId) {
        channelData = dbhelper.getChannelInfo(channelId);
    }

    @Override
    public void onChannelDeleted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        if (charSequence.length() > 0) {
            send.setVisibility(View.VISIBLE);
            recordButton.setVisibility(View.GONE);
        } else {
            send.setVisibility(View.GONE);
            recordButton.setVisibility(View.VISIBLE);
        }
        if (runnable != null)
            handler.removeCallbacks(runnable);
        if (!meTyping) {
            meTyping = true;
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        runnable = new Runnable() {
            public void run() {
                meTyping = false;
            }
        };
        handler.postDelayed(runnable, 500);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void getMsgFromAdminChannels() {
        String timeStamp;
        if (messagesList.size() > 0) {
            timeStamp = messagesList.get(0).chatTime;
        } else {
            timeStamp = "" + (System.currentTimeMillis() / 1000);
        }
        Log.i(TAG, "timeStamp: " + timeStamp);
        Call<AdminChannelMsg> call3 = apiInterface.getMsgFromAdminChannels(GetSet.getToken(), timeStamp);
        call3.enqueue(new Callback<AdminChannelMsg>() {
            @Override
            public void onResponse(Call<AdminChannelMsg> call, Response<AdminChannelMsg> response) {
                if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                    for (AdminChannelMsg.Result result : response.body().result) {
                        Log.e(TAG, "getMsgFromAdminChannels: " + result.messageId);
                        ChannelMessage channelMessage = new ChannelMessage();
                        channelMessage.channelId = result.channelId;
                        channelMessage.channelName = channelData.channelName != null ? channelData.channelName : "";
                        channelMessage.chatTime = result.chatTime;
                        channelMessage.message = result.message;
                        channelMessage.messageId = result.messageId;
                        channelMessage.messageType = result.messageType;
                        messagesList.add(0, channelMessage);
                        updatePosition();
                        showEncryptionText();
                        if (messageListAdapter != null) {
                            messageListAdapter.notifyItemInserted(0);
                            recyclerView.smoothScrollToPosition(0);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<AdminChannelMsg> call, Throwable t) {
                Log.e(TAG, "getMsgFromAdminChannels: " + t.getMessage());
                call.cancel();
            }
        });
    }

    public RecyclerItemClickListener chatItemClick(Context mContext, final RecyclerView recyclerView) {
        return new RecyclerItemClickListener(mContext, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (chatLongPressed) {

                    if (chatLongPressed) {
                        if (playingPosition == position) {
                            stopMedia();
                        }
                        int chatType = recyclerView.getAdapter().getItemViewType(position);
                        if (chatType != VIEW_TYPE_DATE /*&& !messagesList.get(position).messageType.equals(Constants.TAG_ISDELETE)*/) {
                            if (selectedChatPos.contains(messagesList.get(position))) {
                                selectedChatPos.remove(messagesList.get(position));
                            } else {
                                if (copyBtn.getVisibility() == View.VISIBLE) {
                                    copyBtn.setVisibility(View.GONE);
                                }
                                selectedChatPos.add(messagesList.get(position));
                            }

                            messageListAdapter.notifyItemChanged(position);
                        }

                        for (ChannelMessage messagesData : selectedChatPos) {
                            if (!isForwardable(messagesData)) {
                                forwordBtn.setVisibility(View.GONE);
                                break;
                            } else {
                                forwordBtn.setVisibility(View.VISIBLE);
                            }
                        }

                        if (selectedChatPos.isEmpty()) {
                            chatLongPressed = false;
                            chatUserLay.setVisibility(View.VISIBLE);
                            forwordLay.setVisibility(View.GONE);
                            if (copyBtn.getVisibility() == View.VISIBLE) {
                                copyBtn.setVisibility(View.GONE);
                            }
                        }
                    }
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (!chatLongPressed) {
                    if (playingPosition == position) {
                        stopMedia();
                    }
                    if (recyclerView.getAdapter().getItemViewType(position) != VIEW_TYPE_DATE
                        /*&& !messagesList.get(position).messageType.equals(Constants.TAG_ISDELETE)*/) {
                        chatLongPressed = true;
                        selectedChatPos.add(messagesList.get(position));
                        chatUserLay.setVisibility(View.GONE);
                        forwordLay.setVisibility(View.VISIBLE);
                        if (isForwardable(messagesList.get(position))) {
                            forwordBtn.setVisibility(View.VISIBLE);
                        } else {
                            forwordBtn.setVisibility(View.GONE);
                        }
                        if (messagesList.get(position).messageType.equals("text")) {
                            copyBtn.setVisibility(View.VISIBLE);
                        } else {
                            copyBtn.setVisibility(View.GONE);
                        }
                        messageListAdapter.notifyItemChanged(position);
                    }
                }
            }
        });
    }

    private boolean isForwardable(ChannelMessage mData) {
        if (mData.messageType.equals(Constants.TAG_ISDELETE)) {
            return false;
        } else if ((mData.messageType.equals("video") || mData.messageType.equals("file") ||
                mData.messageType.equals("audio"))) {
            if (channelAdminId != null && channelAdminId.equals(GetSet.getUserId()) && !mData.progress.equals("completed")) {
                return false;
            } else
                return channelAdminId == null || channelAdminId.equals(GetSet.getUserId()) || storageManager.checkifFileExists(mData.attachment, mData.messageType, "receive");
        } else if (mData.messageType.equals("image") && ApplicationClass.isStringNotNull(mData.progress) &&
                !mData.progress.equals("completed")) {
            if (channelAdminId != null && channelAdminId.equals(GetSet.getUserId())) {
                return false;
            } else
                return channelAdminId == null || channelAdminId.equals(GetSet.getUserId()) || storageManager.checkifImageExists("receive", mData.attachment);
        } else {
            return true;
        }
    }

    private void emitImage(ChannelMessage mdata, String imageUrl) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.TAG_CHANNEL_ID, channelId);
            jsonObject.put(Constants.TAG_CHANNEL_NAME, channelData.channelName);
            jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CHANNEL);
            jsonObject.put(Constants.TAG_MESSAGE_ID, mdata.messageId);
            jsonObject.put(Constants.TAG_MESSAGE_TYPE, mdata.messageType);
            jsonObject.put(Constants.TAG_MESSAGE, mdata.message);
            jsonObject.put(Constants.TAG_ATTACHMENT, imageUrl);
            jsonObject.put(Constants.TAG_CHAT_TIME, mdata.chatTime);
            jsonObject.put(Constants.TAG_ADMIN_ID, channelData.channelAdminId);
            socketConnection.startChannelChat(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void emitLocation(String type, String lat, String lon) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        RandomString randomString = new RandomString(10);
        String messageId = channelId + randomString.nextString();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.TAG_CHANNEL_ID, channelId);
            jsonObject.put(Constants.TAG_CHANNEL_NAME, channelData.channelName);
            jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CHANNEL);
            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
            jsonObject.put(Constants.TAG_MESSAGE_TYPE, type);
            jsonObject.put(Constants.TAG_MESSAGE, "Location");
            jsonObject.put(Constants.TAG_LAT, lat);
            jsonObject.put(Constants.TAG_LON, lon);
            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
            jsonObject.put(Constants.TAG_ADMIN_ID, channelData.channelAdminId);
            socketConnection.startChannelChat(jsonObject);

            dbhelper.addChannelMessages(channelId, Constants.TAG_CHANNEL, messageId, type,
                    getString(R.string.location), "", lat, lon,
                    "", "", "",
                    unixStamp, "", "read");

            dbhelper.addChannelRecentMsgs(channelId, messageId, unixStamp, "0");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        ChannelMessage data = new ChannelMessage();
        data.messageId = messageId;
        data.channelId = channelId;
        data.messageType = type;
        data.message = getString(R.string.location);
        data.lat = lat;
        data.lon = lon;
        data.chatTime = unixStamp;
        data.deliveryStatus = "";
        messagesList.add(0, data);
        updatePosition();
        showEncryptionText();
        messageListAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);
    }

    private void emitContact(String type, String name, String phone, String countrycode) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        RandomString randomString = new RandomString(10);
        String messageId = channelId + randomString.nextString();
        try {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.TAG_CHANNEL_ID, channelId);
            jsonObject.put(Constants.TAG_CHANNEL_NAME, channelData.channelName);
            jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CHANNEL);
            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
            jsonObject.put(Constants.TAG_MESSAGE_TYPE, type);
            jsonObject.put(Constants.TAG_MESSAGE, getString(R.string.contact));
            jsonObject.put(Constants.TAG_CONTACT_NAME, name);
            jsonObject.put(Constants.TAG_CONTACT_PHONE_NO, phone);
            jsonObject.put(Constants.TAG_CONTACT_COUNTRY_CODE, countrycode);
            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
            jsonObject.put(Constants.TAG_ADMIN_ID, channelData.channelAdminId);
            socketConnection.startChannelChat(jsonObject);
            dbhelper.addChannelMessages(channelId, Constants.TAG_CHANNEL, messageId, type,
                    getString(R.string.contact), "", "", "", name, phone, countrycode,
                    unixStamp, "", "read");

            dbhelper.addChannelRecentMsgs(channelId, messageId, unixStamp, "0");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        ChannelMessage data = new ChannelMessage();
        data.messageType = type;
        data.message = getString(R.string.contact);
        data.contactName = name;
        data.contactPhoneNo = phone;
        data.contactCountryCode = countrycode;
        data.messageId = messageId;
        data.chatTime = unixStamp;
        data.deliveryStatus = "";
        messagesList.add(0, data);
        updatePosition();
        showEncryptionText();
        messageListAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);
    }

    private void deleteChatConfirmDialog() {
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
        yes.setText(getString(R.string.im_sure));
        no.setText(getString(R.string.nope));
        title.setText(R.string.really_delete_chat_history);
        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                dbhelper.deleteChannelMessages(channelId);
                messagesList.clear();
                messageListAdapter.notifyDataSetChanged();
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

    private void exitConfirmDialog() {
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
        yes.setText(getString(R.string.im_sure));
        no.setText(getString(R.string.nope));
        title.setText(R.string.really_exit_group);
        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                try {
                    String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                    RandomString randomString = new RandomString(10);
                    String messageId = channelId + randomString.nextString();

                    JSONObject message = new JSONObject();
                    message.put(Constants.TAG_CHANNEL_ID, channelId);
                    message.put(Constants.TAG_CHANNEL_NAME, channelData.channelName);
                    message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CHANNEL);
                    message.put(Constants.TAG_CHAT_TIME, unixStamp);
                    message.put(Constants.TAG_MESSAGE_ID, messageId);
                    message.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                    message.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                    message.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                    message.put(Constants.TAG_MESSAGE_TYPE, getString(R.string.left));
                    message.put(Constants.TAG_MESSAGE, getString(R.string.one_participant_left));
                    message.put(Constants.TAG_ADMIN_ID, GetSet.getUserId());
                    socketConnection.startGroupChat(message);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(TAG_GROUP_ID, channelId);
                    jsonObject.put(TAG_MEMBER_ID, GetSet.getUserId());
                    socketConnection.exitFromGroup(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                bottomLay.setVisibility(View.GONE);
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

    private void deleteMessageConfirmDialog(ChannelMessage mData) {
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
        title.setText(R.string.really_delete_msg);
        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                dbhelper.deleteChannelMessageFromId(mData.messageId);
                messagesList.remove(mData);
                Toast.makeText(ChannelChatActivity.this, getString(R.string.message_deleted), Toast.LENGTH_SHORT).show();
                selectedChatPos.clear();
                messageListAdapter.notifyDataSetChanged();
                chatUserLay.setVisibility(View.VISIBLE);
                forwordLay.setVisibility(View.GONE);
                chatLongPressed = false;
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

    public String getFormattedDate(Context context, long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis * 1000L);

        Calendar now = Calendar.getInstance();

        final String dateTimeFormatString = "d MMMM yyyy";
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            return getString(R.string.today);
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            return getString(R.string.yesterday);
        } else {
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        }
    }

    private void uploadImage(byte[] imageBytes, final String imagePath, final ChannelMessage mdata, final String filePath) {
        RequestBody requestFile = RequestBody.create(imageBytes, MediaType.parse("openImage/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("channel_attachment", "openImage.jpg", requestFile);

        RequestBody channelid = RequestBody.create(channelId, MediaType.parse("multipart/form-data"));
        RequestBody user_id = RequestBody.create(GetSet.getUserId(), MediaType.parse("multipart/form-data"));
        Call<HashMap<String, String>> call3 = apiInterface.uploadChannelChat(GetSet.getToken(), body, channelid, user_id);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> data = response.body();
                Log.i(TAG, "uploadChannelChat " + data);
                if (data.get(Constants.TAG_STATUS).equals(TRUE)) {
                    File dir = new File(imagePath);
                    if (dir.exists()) {
                        if (mdata.messageType.equals("image")) {
                            dbhelper.updateChannelMessageData(mdata.messageId, Constants.TAG_PROGRESS, "completed");
                            if (messageListAdapter != null) {
                                for (int i = 0; i < messagesList.size(); i++) {
                                    if (mdata.messageId.equals(messagesList.get(i).messageId)) {
                                        messagesList.get(i).progress = "completed";
                                        messageListAdapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                            }
                            emitImage(mdata, data.get(Constants.TAG_USER_IMAGE));
                        } else if (mdata.messageType.equals("video")) {
                            if (messageListAdapter != null) {
                                for (int i = 0; i < messagesList.size(); i++) {
                                    if (mdata.messageId.equals(messagesList.get(i).messageId)) {
                                        messagesList.get(i).thumbnail = mdata.thumbnail;
                                        messageListAdapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                            }

                            Intent service = new Intent(ChannelChatActivity.this, FileUploadService.class);
                            Bundle b = new Bundle();
                            b.putSerializable("mdata", mdata);
                            b.putString("filepath", filePath);
                            b.putSerializable(Constants.TAG_THUMBNAIL, data.get(Constants.TAG_USER_IMAGE));
                            b.putString("chatType", Constants.TAG_CHANNEL);
                            service.putExtras(b);
                            startService(service);
                        }
                    }
                } else {
                    dbhelper.updateChannelMessageData(mdata.messageId, Constants.TAG_PROGRESS, "error");
                    if (messageListAdapter != null) {
                        for (int i = 0; i < messagesList.size(); i++) {
                            if (mdata.messageId.equals(messagesList.get(i).messageId)) {
                                messagesList.get(i).progress = "error";
                                messageListAdapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                Log.e(TAG, "uploadChannelChat " + t.getMessage());
                call.cancel();
            }
        });
    }

    private void networkSnack() {
        Snackbar snackbar = Snackbar
                .make(mainLay, getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    private ChannelMessage updateDBList(String type, String imagePath, String filePath) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        RandomString randomString = new RandomString(10);
        String messageId = channelId + randomString.nextString();

        String msg = "";
        if (type.equals("image")) {
            msg = getString(R.string.image);
        } else if (type.equals("audio")) {
            msg = getFileName(filePath);
        } else if (type.equals("video")) {
            msg = getString(R.string.video);
        } else if (type.equals("document")) {
            msg = getFileName(filePath);
        }

        ChannelMessage channelMessage = new ChannelMessage();
        channelMessage.channelId = channelId;
        channelMessage.channelName = channelData.channelName;
        channelMessage.channelAdminId = channelData.channelAdminId != null ? channelData.channelAdminId : channelData.adminId;
        channelMessage.chatType = channelData.channelCategory;
        channelMessage.messageType = type;
        channelMessage.message = msg;
        channelMessage.messageId = messageId;
        channelMessage.chatTime = unixStamp;
        channelMessage.deliveryStatus = "";
        channelMessage.progress = "";

        if (type.equals("video")) {
            channelMessage.thumbnail = imagePath;
            channelMessage.attachment = filePath;
            dbhelper.addChannelMessages(channelId, Constants.TAG_CHANNEL, messageId, type,
                    ApplicationClass.encryptMessage(msg), ApplicationClass.encryptMessage(filePath), "", "", "",
                    "", "", unixStamp, ApplicationClass.encryptMessage(imagePath), "read");

        } else if (type.equals("image")) {
            channelMessage.thumbnail = "";
            channelMessage.attachment = imagePath;
            dbhelper.addChannelMessages(channelId, Constants.TAG_CHANNEL, messageId, type,
                    ApplicationClass.encryptMessage(msg), ApplicationClass.encryptMessage(imagePath), "", "", "",
                    "", "", unixStamp, ApplicationClass.encryptMessage(imagePath), "read");
        } else {
            channelMessage.thumbnail = "";
            channelMessage.attachment = filePath;
            dbhelper.addChannelMessages(channelId, Constants.TAG_CHANNEL, messageId, type,
                    ApplicationClass.encryptMessage(msg), ApplicationClass.encryptMessage(filePath), "", "", "",
                    "", "", unixStamp, ApplicationClass.encryptMessage(imagePath), "read");
        }

        dbhelper.addChannelRecentMsgs(channelId, messageId, unixStamp, "0");

        messagesList.add(0, channelMessage);
        updatePosition();
        showEncryptionText();
        messageListAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);

        return channelMessage;
    }

    private String getFileName(String url) {
        String imgSplit = url;
        int endIndex = imgSplit.lastIndexOf("/");
        if (endIndex != -1) {
            imgSplit = imgSplit.substring(endIndex + 1);
        }
        return imgSplit;
    }

    /**
     * Function for Sent a message to Socket
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            int permissionCamera = ContextCompat.checkSelfPermission(ChannelChatActivity.this,
                    CAMERA);
            int permissionAudio = ContextCompat.checkSelfPermission(ChannelChatActivity.this,
                    RECORD_AUDIO);

            if (permissionCamera == PackageManager.PERMISSION_GRANTED &&
                    permissionAudio == PackageManager.PERMISSION_GRANTED) {
              /*  Intent video = new Intent(ChatActivity.this, CallActivity.class);
                video.putExtra("from", "send");
                video.putExtra("type", "audio");
                video.putExtra("data", data);
                startActivity(video);*/
            }
        } else if (requestCode == 101) {
            int permissionCamera = ContextCompat.checkSelfPermission(ChannelChatActivity.this,
                    CAMERA);
            int permissionAudio = ContextCompat.checkSelfPermission(ChannelChatActivity.this,
                    RECORD_AUDIO);

            if (permissionCamera == PackageManager.PERMISSION_GRANTED &&
                    permissionAudio == PackageManager.PERMISSION_GRANTED) {
               /* Intent video = new Intent(ChatActivity.this, CallActivity.class);
                video.putExtra("from", "send");
                video.putExtra("type", "video");
                video.putExtra("data", data);
                startActivity(video);*/
            }
        } else if (requestCode == 102) {
            int permissionStorage = ContextCompat.checkSelfPermission(ChannelChatActivity.this, WRITE_EXTERNAL_STORAGE);

            if (permissionStorage == PackageManager.PERMISSION_GRANTED) {
                ImagePicker.pickImage(this, getString(R.string.select_your_image));
            }
        } else if (requestCode == 111) {

            int permissionAudio = ContextCompat.checkSelfPermission(ChannelChatActivity.this,
                    RECORD_AUDIO);
            int permissionStorage = ContextCompat.checkSelfPermission(ChannelChatActivity.this,
                    WRITE_EXTERNAL_STORAGE);

            setVoiceRecorder();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1 && requestCode == 234) {
            if (isNetworkConnected().equals(NOT_CONNECT)) {
                networkSnack();
            } else {
                Log.v(TAG, "camera");
                Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
                String filepath = storageManager.saveBitmapToExtFilesDir(bitmap, timestamp + ".jpg");
                if (filepath != null) {
                    Log.i(TAG, "onActivityResult: " + filepath);
                    ImageCompression imageCompression = new ImageCompression(ChannelChatActivity.this) {
                        @Override
                        protected void onPostExecute(String imagePath) {
                            try {
                                ChannelMessage mdata = updateDBList("image", imagePath, "");
                                byte[] bytes = FileUtils.readFileToByteArray(new File(imagePath));
                                uploadImage(bytes, imagePath, mdata, "");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    };
                    imageCompression.execute(filepath);
                } else {
                    Toast.makeText(this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                }
            }
        } else if (resultCode == -1 && requestCode == 150) {
            if (isNetworkConnected().equals(NOT_CONNECT)) {
                networkSnack();
            } else {
                pathsAry = new ArrayList<>();
                pathsAry.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA));
                if (pathsAry.size() > 0) {
                    String filepath = pathsAry.get(0);
                    Log.i(TAG, "onActivityResult: " + filepath);
                    if (ApplicationClass.isVideoFile(filepath)) {
                        try {
                            Log.v("checkChat", "videopath=" + filepath);
                            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(filepath, MediaStore.Video.Thumbnails.MINI_KIND);
                            if (thumb != null) {
                                String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
                                String imageStatus = storageManager.saveToSdCard(thumb, "sent", timestamp + ".jpg");
                                if (imageStatus.equals("success")) {
                                    File file = storageManager.getImage("sent", timestamp + ".jpg");
                                    String imagePath = file.getAbsolutePath();
                                    ChannelMessage mdata = updateDBList("video", imagePath, filepath);
                                    byte[] bytes = FileUtils.readFileToByteArray(new File(imagePath));
                                    uploadImage(bytes, imagePath, mdata, filepath);
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        ImageCompression imageCompression = new ImageCompression(ChannelChatActivity.this) {
                            @Override
                            protected void onPostExecute(String imagePath) {
                                try {
                                    Log.v("checkChat", "imagepath=" + imagePath);
                                    ChannelMessage mdata = updateDBList("image", imagePath, "");
                                    byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(imagePath));
                                    Log.e(TAG, "onActivityResult: " + imagePath);
                                    uploadImage(bytes, imagePath, mdata, "");
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        };
                        imageCompression.execute(filepath);
                    }
                } else {
                    Toast.makeText(this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                }
            }
        } else if (resultCode == -1 && requestCode == 151) {
            if (isNetworkConnected().equals(NOT_CONNECT)) {
                networkSnack();
            } else {
                pathsAry = new ArrayList<>();
                pathsAry.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS));
                if (pathsAry.size() > 0) {
                    Log.v(TAG, "File");
                    String filepath = pathsAry.get(0);
                    Log.i(TAG, "selectedImageFile: " + filepath);
                    try {
                        ChannelMessage mdata = updateDBList("document", "", filepath);
                        Intent service = new Intent(ChannelChatActivity.this, FileUploadService.class);
                        Bundle b = new Bundle();
                        b.putSerializable("mdata", mdata);
                        b.putString("filepath", filepath);
                        b.putString("chatType", Constants.TAG_CHANNEL);
                        service.putExtras(b);
                        startService(service);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                }
            }
        } else if (resultCode == -1 && requestCode == 152) {
            if (isNetworkConnected().equals(NOT_CONNECT)) {
                networkSnack();
            } else {
                pathsAry = new ArrayList<>();
                pathsAry.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS));
                if (pathsAry.size() > 0) {
                    Log.v(TAG, "Audio");
                    String filepath = pathsAry.get(0);
                    Log.i(TAG, "selectedImageFile: " + filepath);
                    try {
                        ChannelMessage mdata = updateDBList("audio", "", filepath);
                        Intent service = new Intent(ChannelChatActivity.this, FileUploadService.class);
                        Bundle b = new Bundle();
                        b.putSerializable("mdata", mdata);
                        b.putString("filepath", filepath);
                        b.putString("chatType", Constants.TAG_CHANNEL);
                        service.putExtras(b);
                        startService(service);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                }
            }
        } else if (resultCode == -1 && requestCode == 200) {
            String lat = data.getStringExtra("lat");
            String lon = data.getStringExtra("lon");
            if (isNetworkConnected().equals(NOT_CONNECT)) {
                networkSnack();
            } else {
                emitLocation("location", lat, lon);
            }
        } else if (resultCode == -1 && requestCode == 13) {
            try {
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    Uri uri = data.getData();
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    if (cursor.moveToFirst()) {
                        int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        String phoneNo = cursor.getString(phoneIndex);
                        String name = cursor.getString(nameIndex);

                        Log.v("Name & Contact", name + "," + phoneNo);

                        emitContact("contact", name, phoneNo, "");
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (resultCode == RESULT_OK && requestCode == 556) {
            username.setText(channelData.channelName);
        } else if (resultCode == RESULT_OK && requestCode == 222) {
            selectedChatPos.clear();
            messageListAdapter.notifyDataSetChanged();
            chatUserLay.setVisibility(View.VISIBLE);
            forwordLay.setVisibility(View.GONE);
            chatLongPressed = false;
        }
    }

    public String firstThree(String str) {
        return str.length() < 3 ? str : str.substring(0, 3);
    }

    private void setUI(ChannelResult.Result channelData) {
        if (channelData.channelCategory != null && channelData.channelCategory.equalsIgnoreCase(Constants.TAG_ADMIN_CHANNEL)) {
            Glide.with(getApplicationContext()).load(Constants.CHANNEL_IMG_PATH + channelData.channelImage)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.profile_square).error(R.drawable.profile_square))
                    .into(userimage);
        } else {
            if (Utils.isChannelAdmin(channelData, GetSet.getUserId())) {
                bottomLay.setVisibility(View.VISIBLE);
            } else {
                if(myService!=null)
                myService.counterFab.setVisibility(View.GONE);
                bottomLay.setVisibility(View.GONE);
            }
//            if(channelData.blockStatus != null && channelData.blockStatus.equalsIgnoreCase("1")) {
//                txtBlocked.setVisibility(View.VISIBLE);
//                chatUserLay.setEnabled(false);
//            } else {
//                chatUserLay.setEnabled(true);
//                txtBlocked.setVisibility(View.GONE);
//            }
            Glide.with(getApplicationContext()).load(Constants.CHANNEL_IMG_PATH + channelData.channelImage)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.ic_channel_square).error(R.drawable.ic_channel_square))
                    .into(userimage);
            channelAdminId = channelData.channelAdminId;
        }
        username.setText(channelData.channelName);
        txtMembers.setText("" + channelData.channelDes);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v("onDestroy", "onDestroy");
        if (Constants.isChannelChatOpened) {
            Constants.isChannelChatOpened = false;
        }
        SocketConnection.getInstance(this).setChannelChatCallbackListener(null);
    }

    private void updatePosition() {
       /* if(player != null){
            if(player.isPlaying()){
                playingPosition = playingPosition+1;
            }
        }*/
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send:
                ApplicationClass.preventMultiClick(send);
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else if (editText.getText().toString().trim().length() > 0) {
                    String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                    String textMsg = editText.getText().toString().trim();
                    RandomString randomString = new RandomString(10);
                    String messageId = channelId + randomString.nextString();
                    try {
                        String type = "text";
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(Constants.TAG_CHANNEL_ID, channelId);
                        jsonObject.put(Constants.TAG_CHANNEL_NAME, channelData.channelName);
                        jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CHANNEL);
                        jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
                        jsonObject.put(Constants.TAG_MESSAGE_TYPE, type);
                        jsonObject.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(textMsg));
                        jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
                        jsonObject.put(Constants.TAG_ADMIN_ID, GetSet.getUserId());
                        socketConnection.startChannelChat(jsonObject);

                        dbhelper.addChannelMessages(channelId, Constants.TAG_CHANNEL, messageId, type,
                                ApplicationClass.encryptMessage(textMsg), "", "", "", "", "",
                                "", unixStamp, "", "read");

                        dbhelper.addChannelRecentMsgs(channelId, messageId, unixStamp, "0");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    ChannelMessage channelMessage = new ChannelMessage();
                    channelMessage.chatType = channelData.channelCategory;
                    channelMessage.messageType = "text";
                    channelMessage.message = textMsg;
                    channelMessage.messageId = messageId;
                    channelMessage.chatTime = unixStamp;
                    channelMessage.deliveryStatus = "";
                    messagesList.add(0, channelMessage);
                    updatePosition();
                    showEncryptionText();
                    messageListAdapter.notifyItemInserted(0);
                    recyclerView.smoothScrollToPosition(0);
                    editText.setText("");
                } else {
                    editText.setError(getString(R.string.please_enter_your_message));
                }
                break;
            case R.id.backbtn:
                onBackPressed();
                break;
            case R.id.optionbtn:
                ApplicationClass.preventMultiClick(optionbtn);
                stopMedia();
                Display display = this.getWindowManager().getDefaultDisplay();
                final ArrayList<String> values = new ArrayList<>();
                ChannelResult.Result results = dbhelper.getChannelInfo(channelId);
                if (Utils.isUserAdminInChannel(results)) {
                    if (results.blockStatus == null || !results.blockStatus.equals("1")) {
                        values.add(getString(R.string.clear_chat));
                        values.add(getString(R.string.invite_subscribers));
                    }
                    values.add(getString(R.string.delete_channel));
                } else {
                    if (results.blockStatus == null || !results.blockStatus.equals("1")) {
                        if (results.muteNotification.equals("true")) {
                            values.add(getString(R.string.unmute_notification));
                        } else {
                            values.add(getString(R.string.mute_notification));
                        }
                        values.add(getString(R.string.clear_chat));

                        if (results.channelCategory.equalsIgnoreCase(Constants.TAG_USER_CHANNEL)) {
                            if (results.channelType.equalsIgnoreCase(Constants.TAG_PUBLIC)) {
                                values.add(getString(R.string.invite_subscribers));
                            }

                            values.add(getString(R.string.unsubscribe_channel));
//                            values.add(getString(R.string.report));

                            if (results.report.equals("0")) {
                                values.add(getString(R.string.report));
                            } else {
                                values.add(getString(R.string.undo_report));
                            }
                        }
                    } else {
                        values.add(getString(R.string.unsubscribe_channel));
                    }

                }

                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        R.layout.option_item, android.R.id.text1, values);
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = layoutInflater.inflate(R.layout.option_layout, null);
                final PopupWindow popup = new PopupWindow(ChannelChatActivity.this);
                popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popup.setContentView(layout);
                popup.setWidth(display.getWidth() * 60 / 100);
                popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                popup.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                popup.setFocusable(true);

                ImageView pinImage = layout.findViewById(R.id.pinImage);
                if (ApplicationClass.isRTL()) {
                    layout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.grow_from_topleft_to_bottomright));
                    pinImage.setRotation(180);
                    popup.showAtLocation(mainLay, Gravity.TOP | Gravity.START, ApplicationClass.dpToPx(this, 10), ApplicationClass.dpToPx(this, 63));
                } else {
                    layout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.grow_from_topright_to_bottomleft));
                    pinImage.setRotation(0);
                    popup.showAtLocation(mainLay, Gravity.TOP | Gravity.END, ApplicationClass.dpToPx(this, 10), ApplicationClass.dpToPx(this, 63));
                }

                final ListView lv = layout.findViewById(R.id.listView);
                lv.setAdapter(adapter);
                popup.showAsDropDown(v);

                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        popup.dismiss();
                        if (values.get(position).equalsIgnoreCase(getString(R.string.mute_notification))) {
                            dbhelper.updateChannelData(channelId, Constants.TAG_MUTE_NOTIFICATION, "true");
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.unmute_notification))) {
                            dbhelper.updateChannelData(channelId, Constants.TAG_MUTE_NOTIFICATION, "");
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.clear_chat))) {
                            deleteChatConfirmDialog();
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.invite_subscribers))) {
                            Intent subscribers = new Intent(getApplicationContext(), NewChannelActivity.class);
                            subscribers.putExtra(Constants.IS_EDIT, true);
                            subscribers.putExtra(Constants.TAG_CHANNEL_ID, channelId);
                            startActivity(subscribers);
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.unsubscribe_channel))) {
                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                networkSnack();
                            } else {
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put(Constants.TAG_USER_ID, GetSet.getUserId());
                                    jsonObject.put(Constants.TAG_CHANNEL_ID, results.channelId);
                                    socketConnection.unsubscribeChannel(jsonObject, results.channelId, results.totalSubscribers);
                                    finish();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.delete_channel))) {
                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                networkSnack();
                            } else {
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put(Constants.TAG_CHANNEL_ID, channelId);
                                    jsonObject.put(Constants.TAG_USER_ID, GetSet.getUserId());
                                    socketConnection.leaveChannel(jsonObject, channelId);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.report))) {
                            openReportDialog();
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.undo_report))) {
                            reportChannel(channelId, "", true);
                        }
                    }
                });
                break;
            case R.id.attachbtn:
                ApplicationClass.preventMultiClick(attachbtn);
                TransitionManager.beginDelayedTransition(mainLay);
                visible = !visible;
                attachmentsLay.setVisibility(visible ? View.VISIBLE : View.GONE);
                break;
            case R.id.userImg:
                break;
            case R.id.cameraBtn:
                ApplicationClass.preventMultiClick(cameraBtn);
                stopMedia();
                if (ContextCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{CAMERA}, 100);
                } else if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 100);
                } else if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    ApplicationClass.onShareExternal = true;
                    ImagePicker.pickImageCameraOnly(this, 234);
                }
                break;
            case R.id.galleryBtn:
                ApplicationClass.preventMultiClick(galleryBtn);
                stopMedia();
                if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 100);
                } else if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
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
                            .pickPhoto(this, 150);
                }
                break;
            case R.id.fileBtn:
                ApplicationClass.preventMultiClick(fileBtn);
                stopMedia();
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    FilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .enableDocSupport(true)
                            .setActivityTitle(getString(R.string.please_select_document))
                            .showTabLayout(true)
                            .setActivityTheme(R.style.MainTheme)
                            .pickFile(this, 151);
                }
                break;
            case R.id.audioBtn:
                ApplicationClass.preventMultiClick(audioBtn);
                stopMedia();
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    String[] aud = {".mp3", ".wav", ".flac", ".3gp", ".ogg"};
                    FilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .setActivityTheme(R.style.MainTheme)
                            .setActivityTitle(getString(R.string.please_select_audio))
                            .addFileSupport("MP3", aud)
                            .enableDocSupport(false)
                            .enableSelectAll(true)
                            .showTabLayout(false)
                            .sortDocumentsBy(SortingTypes.name)
                            .withOrientation(Orientation.UNSPECIFIED)
                            .pickFile(this, 152);

                }
                break;
            case R.id.locationBtn:
                ApplicationClass.preventMultiClick(locationBtn);
                stopMedia();
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    Intent location = new Intent(this, LocationActivity.class);
                    location.putExtra("from", "share");
                    startActivityForResult(location, 200);
                }
                break;
            case R.id.contactBtn:
                ApplicationClass.preventMultiClick(contactBtn);
                stopMedia();
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    ApplicationClass.onShareExternal = true;
                    Intent intentc = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                    startActivityForResult(intentc, 13);
                }
                break;
            case R.id.chatUserLay:
                ApplicationClass.preventMultiClick(chatUserLay);
                stopMedia();
                Intent profile = new Intent(ChannelChatActivity.this, ChannelInfoActivity.class);
                profile.putExtra(Constants.TAG_CHANNEL_ID, channelId);
                startActivity(profile);
                break;
            case R.id.forwordBtn:
                ApplicationClass.preventMultiClick(forwordBtn);
                stopMedia();
                Intent f = new Intent(ChannelChatActivity.this, ForwardActivity.class);
                f.putExtra("from", "channel");
                f.putExtra("id", channelId);
                f.putExtra("data", selectedChatPos);
                startActivityForResult(f, 222);
                break;
            case R.id.copyBtn:
                if (selectedChatPos.size() > 0) {
                    String msg = selectedChatPos.get(0).message;
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Copied Message", msg);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, getString(R.string.message_copied), Toast.LENGTH_SHORT).show();
                    selectedChatPos.clear();
                    messageListAdapter.notifyDataSetChanged();
                    chatUserLay.setVisibility(View.VISIBLE);
                    forwordLay.setVisibility(View.GONE);
                    chatLongPressed = false;
                }
                break;
            case R.id.deleteBtn:
                ApplicationClass.preventMultiClick(deleteBtn);
                stopMedia();
                openDeleteDialog();
                //deleteMessageConfirmDialog(selectedChatPos.get(0));
                break;
        }

    }

    private void openDeleteDialog() {
        boolean canEveryOneVisible = true;
        Dialog deleteDialog = new Dialog(ChannelChatActivity.this);
        deleteDialog.setCancelable(true);
        if (deleteDialog.getWindow() != null) {
            deleteDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        deleteDialog.setContentView(R.layout.dialog_report);

        RecyclerView deleteRecyclerView = deleteDialog.findViewById(R.id.reportRecyclerView);

        List<String> deleteTexts = new ArrayList<>();

        if (channelData.channelCategory.equals("admin_channel") || ((channelData.channelCategory.equals("user_channel") && !channelData.channelAdminId.equals(GetSet.getUserId())))) {
            canEveryOneVisible = false;
        } else {
            for (ChannelMessage message : selectedChatPos) {
                if (ApplicationClass.isExceedsOneHour(message.chatTime) || message.messageType.equalsIgnoreCase(Constants.TAG_ISDELETE)) {
                    canEveryOneVisible = false;
                    break;
                }
            }
        }


        deleteTexts.add(getString(R.string.delete_for_me));
        deleteTexts.add(getString(R.string.cancel));

        if (canEveryOneVisible) {
            deleteTexts.add(getString(R.string.delete_for_everyone));
            LinearLayoutManager layoutManager = new LinearLayoutManager(ChannelChatActivity.this, RecyclerView.VERTICAL, false);
            deleteRecyclerView.setLayoutManager(layoutManager);
        } else {
            GridLayoutManager layoutManager = new GridLayoutManager(ChannelChatActivity.this, 2);
            deleteRecyclerView.setLayoutManager(layoutManager);
        }

        DeleteAdapter adapter = new DeleteAdapter(deleteTexts, deleteDialog, ChannelChatActivity.this);
        deleteRecyclerView.setAdapter(adapter);

        deleteDialog.show();
    }

    private void showEncryptionText() {
        /*if(messagesList.isEmpty()){
            excryptText.setVisibility(View.VISIBLE);
        } else {
            excryptText.setVisibility(View.GONE);
        }*/
        excryptText.setVisibility(View.GONE);
    }

    private void reportChannel(String channelId, String description, boolean isAlreadyReported) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(Constants.TAG_USER_ID, GetSet.getUserId());
        hashMap.put(Constants.TAG_CHANNEL_ID, channelId);
        if (isAlreadyReported) {
            hashMap.put(Constants.TAG_REPORT, "");
            hashMap.put(Constants.TAG_STATUS, "delete");
        } else {
            hashMap.put(Constants.TAG_REPORT, description);
            hashMap.put(Constants.TAG_STATUS, "new");
        }


        Call<HashMap<String, String>> call = apiInterface.reportChannel(GetSet.getToken(), hashMap);
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                Log.i(TAG, "ReportChannel Response: " + response.body());
                if (response.body().get(Constants.TAG_STATUS).equalsIgnoreCase(Constants.TRUE)) {
                    if (isAlreadyReported) {
                        dbhelper.updateChannelData(channelId, Constants.TAG_REPORT, "0");
                        makeToast(getString(R.string.undo_report));
                    } else {
                        dbhelper.updateChannelData(channelId, Constants.TAG_REPORT, "1");
                        makeToast(getString(R.string.reported_successfully));
                    }
                } else {
                    makeToast(getString(R.string.something_wrong));
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                call.cancel();
                Log.e(TAG, "Report Channel onFailure: " + t.getMessage());
                makeToast(getString(R.string.something_wrong));
            }
        });
    }

    private void openReportDialog() {
        Dialog reportDialog = new Dialog(ChannelChatActivity.this);
        reportDialog.setCancelable(true);
        if (reportDialog.getWindow() != null) {
            reportDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            reportDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        reportDialog.setContentView(R.layout.dialog_report);

        RecyclerView reportRecyclerView = reportDialog.findViewById(R.id.reportRecyclerView);

        List<String> reports = new ArrayList<>();

        reports.add(getString(R.string.spam));
        reports.add(getString(R.string.abuse));
        reports.add(getString(R.string.in_appropriate));
        reports.add(getString(R.string.adult_content));

        LinearLayoutManager layoutManager = new LinearLayoutManager(ChannelChatActivity.this, RecyclerView.VERTICAL, false);
        reportRecyclerView.setLayoutManager(layoutManager);

        ReportAdapter adapter = new ReportAdapter(reports, reportDialog);
        reportRecyclerView.setAdapter(adapter);

        reportDialog.show();
    }

    @Override
    public void deletetype(String type) {
        for (ChannelMessage mData : selectedChatPos) {
            if (type.equals("me")) {
                dbhelper.deleteChannelMessageFromId(mData.messageId);
                messagesList.remove(mData);
                messageListAdapter.notifyDataSetChanged();
                dbhelper.deleteChannelRecentMessages(channelId);
            } else {
                String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);

                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_CHANNEL_ID, channelId);
                    jsonObject.put(Constants.TAG_CHANNEL_NAME, channelData.channelName);
                    jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CHANNEL);
                    jsonObject.put(Constants.TAG_MESSAGE_ID, mData.messageId);
                    jsonObject.put(Constants.TAG_MESSAGE_TYPE, Constants.TAG_ISDELETE);
                    jsonObject.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(getString(R.string.message_deleted)));
                    jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
                    jsonObject.put(Constants.TAG_ADMIN_ID, GetSet.getUserId());
                    socketConnection.startChannelChat(jsonObject);
                    mData.messageType = Constants.TAG_ISDELETE;
                    mData.isDelete = "1";

                    dbhelper.updateChannelMessageData(mData.messageId, Constants.TAG_MESSAGE_TYPE, Constants.TAG_ISDELETE);

                    messageListAdapter.notifyItemChanged(messagesList.indexOf(mData));

                    /*if (messagesList.indexOf(mData) == 0) {
                        dbhelper.addChannelRecentMsgs(channelId, mData.messageId, unixStamp, "0");
                    }*/
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        showEncryptionText();
        selectedChatPos.clear();
        chatUserLay.setVisibility(View.VISIBLE);
        forwordLay.setVisibility(View.GONE);
        chatLongPressed = false;
    }

    private String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    void stopMedia() {
        if (player != null) {
            if (player.isPlaying() && playingPosition != -1) {
                View itemView;
                itemView = linearLayoutManager.findViewByPosition(playingPosition);
                if (itemView != null) {
                    ImageView play = itemView.findViewById(R.id.icon);
                    TextView time = itemView.findViewById(R.id.duration);
                    SeekBar seek = itemView.findViewById(R.id.song_seekbar);
                    if (play != null && time != null && seek != null) {
                        time.setText(milliSecondsToTimer(player.getDuration()));
                        seek.setProgress(0);
                        seek = null;
                        play.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.drawable.pause_icon_white));
                    }
                }
                player.pause();
            }
            player.stop();
            player.reset();
            playingPosition = -1;
        }
    }

    private long mediaDuration(int position, String type) {
        MediaPlayer player;
        File file = storageManager.getFile(messagesList.get(position).attachment,
                messagesList.get(position).messageType, type);
        Uri voiceURI = FileProvider.getUriForFile(getApplicationContext(),
                BuildConfig.APPLICATION_ID + ".provider", file);

        player = MediaPlayer.create(getApplicationContext(), voiceURI);
        if (player != null) {
            return player.getDuration();
        } else {
            return 0;
        }
    }

    public class MessageListAdapter extends RecyclerView.Adapter {
        public static final int VIEW_TYPE_DATE = 9;
        public static final int VIEW_TYPE_VOICE_SENT = 10;
        public static final int VIEW_TYPE_VOICE_RECEIVE = 11;
        public static final int VIEW_TYPE_DELETE_SENT = 12;
        public static final int VIEW_TYPE_DELETE_RECEIVE = 13;
        private static final int VIEW_TYPE_MESSAGE_SENT = 1;
        private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
        private static final int VIEW_TYPE_IMAGE_SENT = 3;
        private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
        private static final int VIEW_TYPE_CONTACT_SENT = 5;
        private static final int VIEW_TYPE_CONTACT_RECEIVED = 6;
        private static final int VIEW_TYPE_FILE_SENT = 7;
        private static final int VIEW_TYPE_FILE_RECEIVED = 8;
        private Context mContext;
        private List<ChannelMessage> mMessageList;

        public MessageListAdapter(Context context, List<ChannelMessage> messageList) {
            mContext = context;
            mMessageList = messageList;
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }

        // Determines the appropriate ViewType according to the sender of the message.
        @Override
        public int getItemViewType(int position) {
            final ChannelMessage message = mMessageList.get(position);

            if (channelData.channelAdminId != null && channelData.channelAdminId.equalsIgnoreCase(GetSet.getUserId())) {
                switch (message.messageType) {
                    case "text":
                        return VIEW_TYPE_MESSAGE_SENT;
                    case "image":
                    case "video":
                    case "location":
                        return VIEW_TYPE_IMAGE_SENT;
                    case "contact":
                        return VIEW_TYPE_CONTACT_SENT;
                    case "date":
                    case "subject":
                    case "create_channel":
                    case "channel_image":
                    case "channel_des":
                        return VIEW_TYPE_DATE;
                    case "audio":
                        return VIEW_TYPE_VOICE_SENT;
                    case Constants.TAG_ISDELETE:
                        return VIEW_TYPE_DELETE_SENT;
                    default:
                        return VIEW_TYPE_FILE_SENT;
                }
            } else {
                switch (message.messageType) {
                    case "text":
                        return VIEW_TYPE_MESSAGE_RECEIVED;
                    case "image":
                    case "video":
                    case "location":
                        return VIEW_TYPE_IMAGE_RECEIVED;
                    case "contact":
                        return VIEW_TYPE_CONTACT_RECEIVED;
                    case "date":
                    case "subject":
                    case "create_channel":
                    case "channel_image":
                    case "channel_des":
                        return VIEW_TYPE_DATE;
                    case "audio":
                        return VIEW_TYPE_VOICE_RECEIVE;
                    case Constants.TAG_ISDELETE:
                        return VIEW_TYPE_DELETE_RECEIVE;
                    default:
                        return VIEW_TYPE_FILE_RECEIVED;
                }
            }
        }

        // Inflates the appropriate layout according to the ViewType.
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;

            switch (viewType) {
                case VIEW_TYPE_MESSAGE_SENT:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_text_bubble_sent, parent, false);
                    return new SentMessageHolder(view);
                case VIEW_TYPE_MESSAGE_RECEIVED:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_text_bubble_receive, parent, false);
                    return new ReceivedMessageHolder(view);
                case VIEW_TYPE_IMAGE_SENT:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_image_bubble_sent, parent, false);
                    return new SentImageHolder(view);
                case VIEW_TYPE_IMAGE_RECEIVED:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_image_bubble_receive, parent, false);
                    return new ReceivedImageHolder(view);
                case VIEW_TYPE_CONTACT_SENT:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_contact_bubble_sent, parent, false);
                    return new SentContactHolder(view);
                case VIEW_TYPE_CONTACT_RECEIVED:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_contact_bubble_receive, parent, false);
                    return new ReceivedContactHolder(view);
                case VIEW_TYPE_FILE_SENT:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_file_bubble_sent, parent, false);
                    return new SentFileHolder(view);
                case VIEW_TYPE_FILE_RECEIVED:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_file_bubble_received, parent, false);
                    return new ReceivedFileHolder(view);
                case VIEW_TYPE_DATE:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_date_layout, parent, false);
                    return new DateHolder(view);
                case VIEW_TYPE_VOICE_SENT:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_voice_sent, parent, false);
                    return new SentVoiceHolder(view);
                case VIEW_TYPE_VOICE_RECEIVE:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_voice_receive, parent, false);
                    return new ReceiveVoiceHolder(view);
                case VIEW_TYPE_DELETE_SENT:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_delete_bubble_sent, parent, false);
                    return new DeleteMsgSent(view);
                case VIEW_TYPE_DELETE_RECEIVE:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.chat_delete_bubble_receive, parent, false);
                    return new DeleteMsgReceived(view);
            }


            return null;
        }

        // Passes the message object to a ViewHolder so that the contents can be bound to UI.
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final ChannelMessage message = mMessageList.get(position);
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MESSAGE_SENT:
                    ((SentMessageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_MESSAGE_RECEIVED:
                    ((ReceivedMessageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_IMAGE_SENT:
                    ((SentImageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_IMAGE_RECEIVED:
                    ((ReceivedImageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_FILE_SENT:
                    ((SentFileHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_FILE_RECEIVED:
                    ((ReceivedFileHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_CONTACT_SENT:
                    ((SentContactHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_CONTACT_RECEIVED:
                    ((ReceivedContactHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_DATE:
                    ((DateHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_VOICE_SENT:
                    ((SentVoiceHolder) holder).bind(message, position);
                    break;
                case VIEW_TYPE_VOICE_RECEIVE:
                    ((ReceiveVoiceHolder) holder).bind(message, position);
                    break;
                case VIEW_TYPE_DELETE_SENT:
                    ((DeleteMsgSent) holder).bind(message);
                    break;
                case VIEW_TYPE_DELETE_RECEIVE:
                    ((DeleteMsgReceived) holder).bind(message);
            }
        }

        private void playMedia(final Context context, int position, final String from) {
            ImageView play;
            TextView time;
            SeekBar seek;
            View itemView;
            boolean isAudioPlay = false;
            if (playingPosition != position) {

                if (player.isPlaying() || playingPosition != -1) {
                    itemView = linearLayoutManager.findViewByPosition(playingPosition);
                    if (itemView != null) {

                        play = itemView.findViewById(R.id.icon);
                        time = itemView.findViewById(R.id.duration);
                        seek = itemView.findViewById(R.id.song_seekbar);
                        if (play != null && time != null && seek != null) {
                            TextView finalTime = time;
                            ImageView finalPlay1 = play;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    finalTime.setText(milliSecondsToTimer(player.getDuration()));
                                    finalPlay1.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pause_icon_white));
                                }
                            });

                        }
                    }
                    player.pause();
                    seekBar.setProgress(0);
                    seekBar = null;
                }
                playingPosition = position;
                itemView = linearLayoutManager.findViewByPosition(playingPosition);
                play = itemView.findViewById(R.id.icon);
                time = itemView.findViewById(R.id.duration);
                seek = itemView.findViewById(R.id.song_seekbar);
                play.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.play_icon_white));
                isAudioPlay = true;
                if (player != null) {
                    player.stop();
                    player.reset();
                }
                if (seekBar != null) {
                    seekBar.setProgress(0);
                    seekBar = null;
                }

                File file = storageManager.getFile(mMessageList.get(position).attachment,
                        mMessageList.get(position).messageType, from);
                Uri voiceURI = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", file);

                seek.setTag(playingPosition);
                seekBar = seek;
                audioTime = time;
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    player.setDataSource(getApplicationContext(), voiceURI);
                    player.prepare();
                    if ((seekBar.getProgress() == 0)) {
                        player.start();
                    } else {
                        player.seekTo(seekBar.getProgress());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                seekHandler.removeCallbacks(moveSeekBarThread);
            } else {
                itemView = linearLayoutManager.findViewByPosition(playingPosition);
                play = itemView.findViewById(R.id.icon);
                time = itemView.findViewById(R.id.duration);
                seek = itemView.findViewById(R.id.song_seekbar);
                seekBar = seek;
                audioTime = time;
                if (player.isPlaying()) {
                    isAudioPlay = false;
                    play.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pause_icon_white));
                    player.pause();
                } else {
                    isAudioPlay = true;
                    play.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.play_icon_white));
                    player.start();
                }
            }

            boolean finalIsAudioPlay = isAudioPlay;
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {

                        if (seekBar.getTag().toString().equals("" + playingPosition)) {
                            if (player.isPlaying()) {
                                player.pause();
                            }
                            player.seekTo(progress);
                            seekBar.setProgress(progress);
                            long currentDuration = player.getCurrentPosition();
                            audioTime.setText(milliSecondsToTimer(currentDuration));
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (seekBar.getTag().toString().equals("" + playingPosition)) {
                        if (!player.isPlaying() && finalIsAudioPlay) {
                            player.start();
                        }
                        player.seekTo(seekBar.getProgress());
                    }
                }
            });

            seekHandler.postDelayed(moveSeekBarThread, 100);

            ImageView finalPlay = play;
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    finalPlay.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pause_icon_white));
                    seekBar.setProgress(0);
                }
            });

        }

        private String milliSecondsToTimer(long milliseconds) {
            String finalTimerString = "";
            String secondsString = "";

            // Convert total duration into time
            int hours = (int) (milliseconds / (1000 * 60 * 60));
            int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
            int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
            // Add hours if there
            if (hours > 0) {
                finalTimerString = hours + ":";
            }

            // Prepending 0 to seconds if it is one digit
            if (seconds < 10) {
                secondsString = "0" + seconds;
            } else {
                secondsString = "" + seconds;
            }

            finalTimerString = finalTimerString + minutes + ":" + secondsString;

            // return timer string
            return finalTimerString;
        }

        public int progressToTimer(int progress, int totalDuration) {
            int currentDuration = 0;
            totalDuration = totalDuration / 1000;
            currentDuration = (int) ((((double) progress) / 100) * totalDuration);

            // return current duration in milliseconds
            return currentDuration * 1000;
        }

        public int getProgressPercentage(long currentDuration, long totalDuration) {
            Double percentage = (double) 0;

            long currentSeconds = (int) (currentDuration / 1000);
            long totalSeconds = (int) (totalDuration / 1000);

            // calculating percentage
            percentage = (((double) currentSeconds) / totalSeconds) * 100;

            // return percentage
            return percentage.intValue();
        }

        private class SentMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText;

            SentMessageHolder(View itemView) {
                super(itemView);

                messageText = itemView.findViewById(R.id.text_message_body);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(final ChannelMessage message) {
                messageText.setText(message.message
                        + Html.fromHtml(" &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"));
                Linkify.addLinks(messageText, Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
            }
        }

        private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText, nameText;

            ReceivedMessageHolder(View itemView) {
                super(itemView);

                nameText = itemView.findViewById(R.id.text_message_sender);
                messageText = itemView.findViewById(R.id.text_message_body);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(ChannelMessage message) {
                nameText.setVisibility(View.GONE);
                if (message.messageType.equals("create_channel") || message.messageType.equals("new_invite")) {
                    messageText.setText(Html.fromHtml("Welcome"
                            + " &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"));
                } else {
                    messageText.setText(message.message
                            + Html.fromHtml(" &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"));
                    Linkify.addLinks(messageText, Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS);
                }
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime.replace(".0", ""))));
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
            }
        }

        private class SentImageHolder extends RecyclerView.ViewHolder {
            TextView timeText;
            ImageView uploadimage, downloadicon;
            RelativeLayout progresslay;
            ProgressWheel progressbar;

            SentImageHolder(View itemView) {
                super(itemView);

                uploadimage = itemView.findViewById(R.id.uploadimage);
                timeText = itemView.findViewById(R.id.text_message_time);
                progresslay = itemView.findViewById(R.id.progresslay);
                progressbar = itemView.findViewById(R.id.progressbar);
                downloadicon = itemView.findViewById(R.id.downloadicon);
            }

            void bind(final ChannelMessage message) {
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                switch (message.messageType) {
                    case "image":
                        downloadicon.setImageResource(R.drawable.upload);
                        switch (message.progress) {
                            case "": {
                                progresslay.setVisibility(View.VISIBLE);
                                progressbar.setVisibility(View.VISIBLE);
                                progressbar.spin();
                                File file = storageManager.getImage("sent", getFileName(message.attachment));
                                Log.i(TAG, "progress: "+message.attachment);
                                if (file != null && file.exists()) {
                                    Log.v(TAG, "progress=" + file.getAbsolutePath());
                                    Glide.with(mContext).load(Uri.fromFile(file)).thumbnail(0.5f)
                                            .into(uploadimage);
                                }
                                break;
                            }
                            case "completed": {
                                progresslay.setVisibility(View.GONE);
                                progressbar.setVisibility(View.GONE);
                                progressbar.stopSpinning();
                                File file = storageManager.getImage("sent", getFileName(ApplicationClass.decryptMessage(message.attachment)));
                                Log.i(TAG, "completed: "+message.attachment);
                                if (file != null) {
                                    Log.v(TAG, "completed=" + file.getAbsolutePath());
                                    Glide.with(mContext).load(Uri.fromFile(file)).thumbnail(0.5f)
                                            .into(uploadimage);
                                } else {
                                    File thumbnail = storageManager.getImage(StorageManager.TAG_THUMB, getFileName(message.thumbnail));
                                    if (thumbnail != null) {
                                        Glide.with(mContext).load(Uri.fromFile(thumbnail)).thumbnail(0.5f)
                                                .transform(new BlurTransformation())
                                                .into(uploadimage);
                                    }
                                }
                                break;
                            }
                            case "error": {
                                progresslay.setVisibility(View.VISIBLE);
                                progressbar.setVisibility(View.VISIBLE);
                                progressbar.stopSpinning();
                                File file = storageManager.getImage("sent", getFileName(message.attachment));
                                Log.i(TAG, "error: "+message.attachment);
                                if (file != null) {
                                    Log.v(TAG, "error=" + file.getAbsolutePath());
                                    Glide.with(mContext).load(Uri.fromFile(file)).thumbnail(0.5f)
                                            .into(uploadimage);
                                }
                                break;
                            }
                        }

                        uploadimage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!chatLongPressed) {
                                    if (message.progress.equals("error")) {
                                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                                            networkSnack();
                                        } else {
                                            try {
                                                progressbar.setVisibility(View.VISIBLE);
                                                progressbar.spin();
                                                dbhelper.updateChannelMessageData(message.messageId, Constants.TAG_PROGRESS, "");
                                                message.progress = "";
                                                byte[] bytes = FileUtils.readFileToByteArray(new File(message.attachment));
                                                uploadImage(bytes, message.attachment, message, "");
                                            } catch (IOException ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    } else if (message.progress.equals("completed")) {
                                        Log.i(TAG, "channelchatonClickfile: "+ message.attachment);
                                        if (storageManager.checkifImageExists("sent",  getFileName(ApplicationClass.decryptMessage(message.attachment)))) {
                                            File file = storageManager.getImage("sent", getFileName(ApplicationClass.decryptMessage(message.attachment)));
                                            Log.i(TAG, "channelchatonClickfile1: "+file.getAbsolutePath());
                                            if (file != null) {
                                                ApplicationClass.openImage(mContext, file.getAbsolutePath(), Constants.TAG_MESSAGE, imageView);
                                            }
                                        } else {
                                            Log.i(TAG, "channelchatonClick4: "+message.attachment);
                                            Log.i(TAG, "onClick: "+fileList());
                                            Toast.makeText(ChannelChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }
                        });
                        break;
                    case "location":
                        progresslay.setVisibility(View.GONE);
                        Glide.with(mContext).load(ApplicationClass.getMapUrl(message.lat, message.lon, mContext)).thumbnail(0.5f)
                                .into(uploadimage);

                        uploadimage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!chatLongPressed) {
                                    Intent i = new Intent(ChannelChatActivity.this, LocationActivity.class);
                                    i.putExtra("from", "view");
                                    i.putExtra("lat", message.lat);
                                    i.putExtra("lon", message.lon);
                                    startActivity(i);
                                }
                            }
                        });
                        break;
                    case "video":
                        progresslay.setVisibility(View.VISIBLE);
                        switch (message.progress) {
                            case "": {
                                progressbar.setVisibility(View.VISIBLE);
                                progressbar.spin();
                                downloadicon.setImageResource(R.drawable.upload);
                                File file = storageManager.getImage("sent", getFileName(message.thumbnail));
                                if (file != null) {
                                    Log.v(TAG, "file=" + file.getAbsolutePath());
                                    Glide.with(mContext).load(Uri.fromFile(file)).thumbnail(0.5f)
                                            .into(uploadimage);
                                }
                                break;
                            }
                            case "completed": {
                                progressbar.setVisibility(View.GONE);
                                progressbar.stopSpinning();
                                downloadicon.setImageResource(R.drawable.play);
                                File thumbnail = storageManager.getImage("sent", getFileName(message.thumbnail));
                                String filePath = storageManager.getFilePath(StorageManager.TAG_VIDEO_SENT, message.attachment);
                                if (filePath != null && !TextUtils.isEmpty(filePath)) {
                                    Glide.with(mContext).load("" + thumbnail.getAbsolutePath()).thumbnail(0.5f)
                                            .into(uploadimage);
                                } else {
                                    Glide.with(mContext).load("" + thumbnail.getAbsolutePath())
                                            .transform(new BlurTransformation())
                                            .thumbnail(0.5f)
                                            .into(uploadimage);
                                }
                                break;
                            }
                            case "error": {
                                progressbar.setVisibility(View.VISIBLE);
                                progressbar.stopSpinning();
                                downloadicon.setImageResource(R.drawable.upload);
                                File file = storageManager.getImage("sent", getFileName(message.thumbnail));
                                if (file != null) {
                                    Log.v(TAG, "file=" + file.getAbsolutePath());
                                    Glide.with(mContext).load(Uri.fromFile(file)).thumbnail(0.5f)
                                            .into(uploadimage);
                                }
                                break;
                            }
                            default:

                                break;
                        }

                        uploadimage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!chatLongPressed) {
                                    if (message.progress.equals("error")) {
                                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                                            networkSnack();
                                        } else {
                                            try {
                                                Bitmap thumb = ThumbnailUtils.createVideoThumbnail(message.attachment, MediaStore.Video.Thumbnails.MINI_KIND);
                                                if (thumb != null) {
                                                    progressbar.setVisibility(View.VISIBLE);
                                                    progressbar.spin();
                                                    dbhelper.updateChannelMessageData(message.messageId, Constants.TAG_PROGRESS, "");
                                                    message.progress = "";
                                                    String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
                                                    String imageStatus = storageManager.saveToSdCard(thumb, "sent", timestamp + ".jpg");
                                                    if (imageStatus.equals("success")) {
                                                        File file = storageManager.getImage("sent", timestamp + ".jpg");
                                                        String imagePath = file.getAbsolutePath();
                                                        byte[] bytes = FileUtils.readFileToByteArray(new File(imagePath));
                                                        uploadImage(bytes, imagePath, message, message.attachment);
                                                    }
                                                }
                                            } catch (IOException ex) {
                                                ex.printStackTrace();
                                                Toast.makeText(ChannelChatActivity.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    } else if (message.progress.equals("completed")) {
                                        if (storageManager.checkifFileExists(message.attachment, message.messageType, "sent")) {
                                            try {
                                                Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_VIEW);
                                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                File file = storageManager.getFile(message.attachment, message.messageType, "sent");
                                                Uri photoURI = FileProvider.getUriForFile(mContext,
                                                        BuildConfig.APPLICATION_ID + ".provider", file);

                                                MimeTypeMap mime = MimeTypeMap.getSingleton();
                                                String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                                String type = mime.getMimeTypeFromExtension(ext);

                                                intent.setDataAndType(photoURI, type);

                                                startActivity(intent);
                                            } catch (ActivityNotFoundException e) {
                                                Toast.makeText(ChannelChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                                e.printStackTrace();
                                            }
                                        } else {
                                            Log.i(TAG, "channelchatonClick: "+message);
                                            Toast.makeText(ChannelChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }
                        });
                        break;
                }
            }
        }

        private class ReceivedImageHolder extends RecyclerView.ViewHolder {
            TextView timeText, nameText;
            ImageView uploadimage, downloadicon;
            RelativeLayout progresslay, videoprogresslay;
            ProgressWheel progressbar, videoprogressbar;

            ReceivedImageHolder(View itemView) {
                super(itemView);

                uploadimage = itemView.findViewById(R.id.uploadimage);
                progresslay = itemView.findViewById(R.id.progresslay);
                timeText = itemView.findViewById(R.id.text_message_time);
                progressbar = itemView.findViewById(R.id.progressbar);
                downloadicon = itemView.findViewById(R.id.downloadicon);
                nameText = itemView.findViewById(R.id.text_message_sender);
                videoprogresslay = itemView.findViewById(R.id.videoprogresslay);
                videoprogressbar = itemView.findViewById(R.id.videoprogressbar);
            }

            void bind(final ChannelMessage message) {
                nameText.setVisibility(View.GONE);
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                switch (message.messageType) {
                    case "image":
                        videoprogresslay.setVisibility(View.GONE);
                        downloadicon.setImageResource(R.drawable.download);
                        if (storageManager.checkifImageExists("receive", message.attachment)) {
                            progresslay.setVisibility(View.GONE);
                            File file = storageManager.getImage("thumb", message.attachment);
                            if (file != null) {
                                Glide.with(mContext).load(file).thumbnail(0.5f)
                                        .into(uploadimage);
                            }
                        } else {
                            progresslay.setVisibility(View.VISIBLE);
                            progressbar.setVisibility(View.VISIBLE);
                            progressbar.stopSpinning();
                            Glide.with(mContext).load(Constants.CHANNEL_IMG_PATH + message.attachment).thumbnail(0.5f)
                                    .apply(RequestOptions.overrideOf(18, 18))
                                    .into(uploadimage);
                        }
                        timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));

                        uploadimage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!chatLongPressed) {
                                    if (storageManager.checkifImageExists("receive", message.attachment)) {
                                        File file = storageManager.getImage("receive", message.attachment);
                                        if (file != null) {
                                            videoprogresslay.setVisibility(View.GONE);
                                            Log.v(TAG, "file=" + file.getAbsolutePath());
                                            ApplicationClass.openImage(mContext, file.getAbsolutePath(), Constants.TAG_MESSAGE, imageView);
                                        }
                                    } else {
                                        if (ContextCompat.checkSelfPermission(ChannelChatActivity.this, WRITE_EXTERNAL_STORAGE)
                                                != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(ChannelChatActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, 100);
                                        } else {
                                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                                networkSnack();
                                            } else {
                                                ImageDownloader imageDownloader = new ImageDownloader(ChannelChatActivity.this) {
                                                    @Override
                                                    protected void onPostExecute(Bitmap imgBitmap) {
                                                        if (imgBitmap == null) {
                                                            Log.e(TAG, "bitmapFailed");
                                                            Toast.makeText(mContext, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Log.v(TAG, "onBitmapLoaded");
                                                            try {
                                                                String status = storageManager.saveThumbNail(imgBitmap, message.attachment);
                                                                if (status.equals("success")) {
                                                                    File thumbFile = storageManager.getImage("thumb", message.attachment);
                                                                    if (thumbFile != null) {
                                                                        Log.v("file", "file=" + thumbFile.getAbsolutePath());
                                                                        Glide.with(mContext).load(thumbFile).thumbnail(0.5f)
                                                                                .into(uploadimage);
                                                                        progresslay.setVisibility(View.GONE);
                                                                        progressbar.stopSpinning();
                                                                        videoprogresslay.setVisibility(View.GONE);
                                                                    }
                                                                } else {
                                                                    Toast.makeText(mContext, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                                                }
                                                            } catch (NullPointerException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    protected void onProgressUpdate(String... progress) {
                                                        // progressbar.setProgress(Integer.parseInt(progress[0]));
                                                    }
                                                };
                                                imageDownloader.execute(Constants.CHANNEL_IMG_PATH + message.attachment, "receive");
                                                progressbar.setVisibility(View.VISIBLE);
                                                progressbar.spin();
                                            }
                                        }
                                    }
                                }
                            }
                        });
                        break;
                    case "location":
                        progresslay.setVisibility(View.GONE);
                        Glide.with(mContext).load(ApplicationClass.getMapUrl(message.lat, message.lon, mContext)).thumbnail(0.5f)
                                .into(uploadimage);
                        timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                        uploadimage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!chatLongPressed) {
                                    Intent i = new Intent(ChannelChatActivity.this, LocationActivity.class);
                                    i.putExtra("from", "view");
                                    i.putExtra("lat", message.lat);
                                    i.putExtra("lon", message.lon);
                                    startActivity(i);
                                }
                            }
                        });
                        break;
                    case "video":
                        progresslay.setVisibility(View.VISIBLE);
                        progressbar.setVisibility(View.GONE);
                        downloadicon.setImageResource(R.drawable.play);
                        timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                        if (storageManager.checkifFileExists(message.attachment, message.messageType, "receive") &&
                                storageManager.checkifImageExists("thumb", message.thumbnail)) {
                            Log.v(TAG, "video-if");
                            videoprogresslay.setVisibility(View.GONE);
                            File file = storageManager.getImage("thumb", message.thumbnail);
                            if (file != null) {
                                Log.v(TAG, "file=" + file.getAbsolutePath());
                                Glide.with(mContext).load(Uri.fromFile(file)).thumbnail(0.5f)
                                        .into(uploadimage);
                            }
                        } else {
                            Glide.with(mContext).load(Constants.CHANNEL_IMG_PATH + message.thumbnail)
                                    .listener(new RequestListener<Drawable>() {
                                                  @Override
                                                  public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {

                                                      return false;
                                                  }

                                                  @Override
                                                  public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                      uploadimage.setImageDrawable(resource);
                                                      return true;
                                                  }
                                              }
                                    ).into(uploadimage);
                            videoprogresslay.setVisibility(View.VISIBLE);
                            videoprogressbar.setVisibility(View.VISIBLE);
                            videoprogressbar.stopSpinning();
                        }
                        uploadimage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!chatLongPressed) {
                                    if (storageManager.checkifFileExists(message.attachment, message.messageType, "receive") &&
                                            storageManager.checkifImageExists("thumb", message.thumbnail)) {
                                        try {
                                            Intent intent = new Intent();
                                            intent.setAction(Intent.ACTION_VIEW);
                                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            File file = storageManager.getFile(message.attachment, message.messageType, "receive");
                                            Uri photoURI = FileProvider.getUriForFile(mContext,
                                                    BuildConfig.APPLICATION_ID + ".provider", file);

                                            MimeTypeMap mime = MimeTypeMap.getSingleton();
                                            String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                            String type = mime.getMimeTypeFromExtension(ext);

                                            intent.setDataAndType(photoURI, type);

                                            startActivity(intent);
                                        } catch (ActivityNotFoundException e) {
                                            Toast.makeText(ChannelChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                            e.printStackTrace();
                                        }
                                    } else {
                                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                                            networkSnack();
                                        } else {
                                            ImageDownloader imageDownloader = new ImageDownloader(ChannelChatActivity.this) {
                                                @Override
                                                protected void onPostExecute(Bitmap imgBitmap) {
                                                    if (imgBitmap == null) {
                                                        Log.v("bitmapFailed", "bitmapFailed");
                                                        Toast.makeText(mContext, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                                        videoprogresslay.setVisibility(View.GONE);
                                                        videoprogressbar.setVisibility(View.GONE);
                                                        videoprogressbar.stopSpinning();
                                                    } else {
                                                        Log.v("onBitmapLoaded", "onBitmapLoaded");
                                                        try {
//                                                        String status = storageManager.saveThumbNail(imgBitmap, message.thumbnail);
//                                                        if (status.equals("success")) {
                                                            final File thumbFile = storageManager.getImage("thumb", message.thumbnail);
                                                            if (thumbFile != null) {
                                                                Log.v("file", "file=" + thumbFile.getAbsolutePath());

                                                                DownloadFiles downloadFiles = new DownloadFiles(ChannelChatActivity.this) {
                                                                    @Override
                                                                    protected void onPostExecute(String downPath) {
                                                                        videoprogresslay.setVisibility(View.GONE);
                                                                        videoprogressbar.setVisibility(View.GONE);
                                                                        videoprogressbar.stopSpinning();
                                                                        if (downPath == null) {
                                                                            Log.v("Download Failed", "Download Failed");
                                                                            Toast.makeText(mContext, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                                                        } else {
                                                                            Glide.with(mContext).load(Uri.fromFile(thumbFile)).thumbnail(0.5f)
                                                                                    .into(uploadimage);
                                                                            //  Toast.makeText(mContext, getString(R.string.downloaded), Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                };
                                                                downloadFiles.execute(Constants.CHANNEL_IMG_PATH + message.attachment, message.messageType);
                                                            }
//                                                        } else {
//                                                            Toast.makeText(mContext, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
//                                                            videoprogresslay.setVisibility(View.VISIBLE);
//                                                            videoprogressbar.setVisibility(View.VISIBLE);
//                                                            videoprogressbar.stopSpinning();
//                                                        }
                                                        } catch (NullPointerException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }

                                                @Override
                                                protected void onProgressUpdate(String... progress) {
                                                    // progressbar.setProgress(Integer.parseInt(progress[0]));
                                                }
                                            };
                                            imageDownloader.execute(Constants.CHANNEL_IMG_PATH + message.thumbnail, "thumb");
                                            videoprogresslay.setVisibility(View.VISIBLE);
                                            videoprogressbar.setVisibility(View.VISIBLE);
                                            videoprogressbar.spin();
                                        }
                                    }
                                }
                            }
                        });
                        break;
                }
            }
        }

        private class SentFileHolder extends RecyclerView.ViewHolder {
            TextView filename, timeText, file_type_tv;
            ImageView icon, uploadicon;
            RelativeLayout file_body_lay;
            ProgressWheel progressbar;

            SentFileHolder(View itemView) {
                super(itemView);

                filename = itemView.findViewById(R.id.filename);
                timeText = itemView.findViewById(R.id.text_message_time);
                icon = itemView.findViewById(R.id.icon);
                file_body_lay = itemView.findViewById(R.id.file_body_lay);
                progressbar = itemView.findViewById(R.id.progressbar);
                uploadicon = itemView.findViewById(R.id.uploadicon);
                file_type_tv = itemView.findViewById(R.id.file_type_tv);
            }

            void bind(final ChannelMessage message) {
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                if (message.messageType.equals("file")) {
                    icon.setImageResource(R.drawable.icon_file_unknown);
                    file_type_tv.setVisibility(View.VISIBLE);
                    file_type_tv.setText(firstThree(FilenameUtils.getExtension(message.attachment)));
                } else if (message.messageType.equals("audio")) {
                    icon.setImageResource(R.drawable.mp3);
                    file_type_tv.setVisibility(View.GONE);
                }

                switch (message.progress) {
                    case "":
                        progressbar.setVisibility(View.VISIBLE);
                        progressbar.spin();
                        uploadicon.setVisibility(View.VISIBLE);
                        filename.setText(getString(R.string.uploading));
                        break;
                    case "completed":
                        progressbar.setVisibility(View.GONE);
                        progressbar.stopSpinning();
                        uploadicon.setVisibility(View.GONE);
                        filename.setText(message.message);
                        break;
                    case "error":
                        progressbar.setVisibility(View.VISIBLE);
                        progressbar.stopSpinning();
                        uploadicon.setVisibility(View.VISIBLE);
                        filename.setText(getString(R.string.retry));
                        break;
                }

                file_body_lay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!chatLongPressed) {
                            if (message.progress.equals("error")) {
                                if (isNetworkConnected().equals(NOT_CONNECT)) {
                                    networkSnack();
                                } else {
                                    try {
                                        progressbar.setVisibility(View.VISIBLE);
                                        progressbar.spin();
                                        uploadicon.setVisibility(View.VISIBLE);
                                        filename.setText("Uploading..");
                                        dbhelper.updateChannelMessageData(message.messageId, Constants.TAG_PROGRESS, "");
                                        message.progress = "";
                                        Intent service = new Intent(ChannelChatActivity.this, FileUploadService.class);
                                        Bundle b = new Bundle();
                                        b.putSerializable("mdata", message);
                                        b.putString("filepath", message.attachment);
                                        b.putString("chatType", Constants.TAG_CHANNEL);
                                        service.putExtras(b);
                                        startService(service);
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            } else if (message.progress.equals("completed")) {
                                if (storageManager.checkifFileExists(message.attachment, message.messageType, "sent")) {
                                    try {
                                        Intent intent = new Intent();
                                        intent.setAction(android.content.Intent.ACTION_VIEW);
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        File file = storageManager.getFile(message.attachment, message.messageType, "sent");
                                        Uri photoURI = FileProvider.getUriForFile(mContext,
                                                BuildConfig.APPLICATION_ID + ".provider", file);

                                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                                        String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                        String type = mime.getMimeTypeFromExtension(ext);

                                        intent.setDataAndType(photoURI, type);

                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                        makeToast(getString(R.string.no_application));
                                        e.printStackTrace();
                                    }
                                } else {
                                    Log.i(TAG, "channelchatonClick1: "+message);
                                    makeToast(getString(R.string.no_media));
                                }
                            }
                        }
                    }
                });
            }
        }

        private class ReceivedFileHolder extends RecyclerView.ViewHolder {
            TextView filename, timeText, nameText, file_type_tv;
            ImageView icon, downloadicon;
            RelativeLayout file_body_lay;
            ProgressWheel progressbar;

            ReceivedFileHolder(View itemView) {
                super(itemView);

                filename = itemView.findViewById(R.id.filename);
                timeText = itemView.findViewById(R.id.text_message_time);
                icon = itemView.findViewById(R.id.icon);
                file_body_lay = itemView.findViewById(R.id.file_body_lay);
                downloadicon = itemView.findViewById(R.id.downloadicon);
                progressbar = itemView.findViewById(R.id.progressbar);
                nameText = itemView.findViewById(R.id.text_message_sender);
                file_type_tv = itemView.findViewById(R.id.file_type_tv);
            }

            void bind(final ChannelMessage message) {
                filename.setText(message.message);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                nameText.setVisibility(View.GONE);
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                if (message.messageType.equals("document")) {
                    icon.setImageResource(R.drawable.icon_file_unknown);
                    file_type_tv.setVisibility(View.VISIBLE);
                    file_type_tv.setText(firstThree(FilenameUtils.getExtension(message.attachment)));
                } else if (message.messageType.equals("audio")) {
                    file_type_tv.setVisibility(View.GONE);
                    icon.setImageResource(R.drawable.mp3);
                }

                if (storageManager.checkifFileExists(message.attachment, message.messageType, "receive")) {
                    downloadicon.setVisibility(View.GONE);
                    progressbar.setVisibility(View.GONE);
                } else {
                    downloadicon.setVisibility(View.VISIBLE);
                    progressbar.setVisibility(View.VISIBLE);
                    progressbar.stopSpinning();
                }

                file_body_lay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!chatLongPressed) {
                            if (storageManager.checkifFileExists(message.attachment, message.messageType, "receive")) {
                                try {
                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_VIEW);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    File file = storageManager.getFile(message.attachment, message.messageType, "receive");
                                    Uri photoURI = FileProvider.getUriForFile(mContext,
                                            BuildConfig.APPLICATION_ID + ".provider", file);

                                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                                    String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                    String type = mime.getMimeTypeFromExtension(ext);

                                    intent.setDataAndType(photoURI, type);

                                    startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(ChannelChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            } else {
                                if (isNetworkConnected().equals(NOT_CONNECT)) {
                                    networkSnack();
                                } else {
                                    DownloadFiles downloadFiles = new DownloadFiles(ChannelChatActivity.this) {
                                        @Override
                                        protected void onPostExecute(String downPath) {
                                            progressbar.setVisibility(View.GONE);
                                            progressbar.stopSpinning();
                                            downloadicon.setVisibility(View.GONE);
                                            if (downPath == null) {
                                                Log.v("Download Failed", "Download Failed");
                                                Toast.makeText(mContext, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                            } else {
                                                //Toast.makeText(mContext, getString(R.string.downloaded), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    };
                                    downloadFiles.execute(Constants.CHANNEL_IMG_PATH + message.attachment, message.messageType);
                                    progressbar.setVisibility(View.VISIBLE);
                                    progressbar.spin();
                                    downloadicon.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                });
            }
        }

        private class SentContactHolder extends RecyclerView.ViewHolder {
            TextView username, phoneno, timeText;

            SentContactHolder(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                phoneno = itemView.findViewById(R.id.phoneno);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(ChannelMessage message) {
                username.setText(message.contactName);
                phoneno.setText(message.contactPhoneNo);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
            }
        }

        private class ReceivedContactHolder extends RecyclerView.ViewHolder {
            TextView username, phoneno, timeText, addcontact, nameText;

            ReceivedContactHolder(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                phoneno = itemView.findViewById(R.id.phoneno);
                timeText = itemView.findViewById(R.id.text_message_time);
                addcontact = itemView.findViewById(R.id.addcontact);
                nameText = itemView.findViewById(R.id.text_message_sender);
            }

            void bind(final ChannelMessage message) {
                username.setText(message.contactName);
                phoneno.setText(message.contactPhoneNo);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                nameText.setVisibility(View.GONE);
                nameText.setText("");
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                addcontact.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!chatLongPressed) {
                            Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
                            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, message.contactPhoneNo);
                            intent.putExtra(ContactsContract.Intents.Insert.NAME, message.contactName);
                            startActivity(intent);
                        }
                    }
                });
            }
        }

        private class DateHolder extends RecyclerView.ViewHolder {
            TextView timeText;

            DateHolder(View itemView) {
                super(itemView);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(final ChannelMessage message) {
                if (message.messageType.equalsIgnoreCase("create_channel")) {
                    if (Utils.isUserAdminInChannel(channelData)) {
                        timeText.setText(R.string.you_created_this_channel);
                    } else {
                        timeText.setText(R.string.you_added_in_to_this_channel);
                    }
                } else if (message.messageType.equalsIgnoreCase("subject")) {
                    timeText.setText(R.string.channel_subject_changed);
                } else if (message.messageType.equalsIgnoreCase("channel_image")) {
                    timeText.setText(R.string.channel_image_changed);
                } else if (message.messageType.equalsIgnoreCase("channel_des")) {
                    timeText.setText(R.string.channel_info_changed);
                } else {
                    timeText.setText(Utils.getFormattedDate(mContext, Long.parseLong(message.chatTime)));
                }
            }
        }

        private class DeleteMsgSent extends RecyclerView.ViewHolder {
            TextView timeText;

            DeleteMsgSent(View itemView) {
                super(itemView);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(final ChannelMessage message) {
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
            }
        }

        private class DeleteMsgReceived extends RecyclerView.ViewHolder {
            TextView timeText;

            DeleteMsgReceived(View itemView) {
                super(itemView);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(final ChannelMessage message) {
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                /*nameText.setVisibility(View.VISIBLE);
                nameText.setText(ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(message.memberId)));*/
            }
        }

        private class SentVoiceHolder extends RecyclerView.ViewHolder {

            ImageView icon, tickimage, uploadicon;
            TextView duration, msg_time, filename;
            SeekBar seekbar;
            RelativeLayout body_lay;
            ProgressWheel progressbar;
            Context context;

            SentVoiceHolder(View itemView) {
                super(itemView);
                context = itemView.getContext();
                icon = itemView.findViewById(R.id.icon);
                tickimage = itemView.findViewById(R.id.tickimage);
                duration = itemView.findViewById(R.id.duration);
                msg_time = itemView.findViewById(R.id.text_message_time);
                body_lay = itemView.findViewById(R.id.body_lay);
                progressbar = itemView.findViewById(R.id.progressbar);
                uploadicon = itemView.findViewById(R.id.uploadicon);
                filename = itemView.findViewById(R.id.filename);
                seekbar = itemView.findViewById(R.id.song_seekbar);

            }

            void bind(final ChannelMessage message, int pos) {
                itemView.setTag("pos" + pos);
                seekbar.setTag("tag" + pos);
                msg_time.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));

                duration.setVisibility(View.INVISIBLE);
//            duration.setText(message.duration);
                icon.setImageResource(R.drawable.pause_icon_white);

                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                switch (message.progress) {
                    case "":
                        progressbar.setVisibility(View.VISIBLE);
                        progressbar.spin();
                        uploadicon.setVisibility(View.VISIBLE);
                        seekbar.setVisibility(View.GONE);
//                    duration.setVisibility(View.GONE);
                        filename.setText(R.string.uploading);
                        break;
                    case "completed":
                        progressbar.setVisibility(View.GONE);
                        progressbar.stopSpinning();
                        uploadicon.setVisibility(View.GONE);
                        seekbar.setVisibility(View.VISIBLE);
                        duration.setVisibility(View.VISIBLE);
                        icon.setVisibility(View.VISIBLE);
                        filename.setVisibility(View.GONE);
                        break;
                    case "error":
                        progressbar.setVisibility(View.VISIBLE);
                        progressbar.stopSpinning();
                        uploadicon.setVisibility(View.VISIBLE);
                        seekbar.setVisibility(View.GONE);
//                    duration.setVisibility(View.GONE);
                        icon.setVisibility(View.GONE);
                        filename.setText(R.string.retry);
                        break;
                }
//            seekbar.setVisibility(View.INVISIBLE);
//            duration.setVisibility(View.INVISIBLE);

                if (message.deliveryStatus != null) {
                    switch (message.deliveryStatus) {
                        case "read":
                            tickimage.setVisibility(View.VISIBLE);
                            tickimage.setImageResource(R.drawable.double_tick);
                            break;
                        case "sent":
                            tickimage.setVisibility(View.VISIBLE);
                            tickimage.setImageResource(R.drawable.double_tick_unseen);
                            break;
                        default:
                            tickimage.setVisibility(View.VISIBLE);
                            tickimage.setImageResource(R.drawable.single_tick);
                            break;
                    }
                }


                body_lay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!chatLongPressed) {
                            if (message.progress.equals("error")) {
                                if (isNetworkConnected().equals(NOT_CONNECT)) {
                                    networkSnack();
                                } else {
                                    try {
                                        progressbar.setVisibility(View.VISIBLE);
                                        progressbar.spin();
                                        uploadicon.setVisibility(View.VISIBLE);
                                        filename.setText(getString(R.string.uploading));
                                        dbhelper.updateMessageData(message.messageId, Constants.TAG_PROGRESS, "");
                                        message.progress = "";
                                        Intent service = new Intent(ChannelChatActivity.this, FileUploadService.class);
                                        Bundle b = new Bundle();
                                        b.putSerializable("mdata", message);
                                        b.putString("filepath", message.attachment);
                                        b.putString("chatType", "chat");
                                        service.putExtras(b);
                                        startService(service);
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }
                        }

                    }
                });

                if (storageManager.checkifFileExists(message.attachment, message.messageType, "sent")) {
                    duration.setText(milliSecondsToTimer(mediaDuration(getAdapterPosition(), "sent")));
                } else {
                    duration.setVisibility(View.GONE);
                }

                //sentVoiceHolder

                icon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!chatLongPressed) {
                            duration.setVisibility(View.VISIBLE);
                            if (storageManager.checkifFileExists(message.attachment, message.messageType, "sent")) {
                                playMedia(context, getAdapterPosition(), "sent");
                            } else {
                                Log.i(TAG, "channelchatonClick2: "+message);
                                Toast.makeText(ChannelChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                            }
                        }

                    }

                });

            }

        }

        private class ReceiveVoiceHolder extends RecyclerView.ViewHolder {

            ImageView icon, tickimage, downloadIcon;
            TextView duration, msg_time, file_name;
            SeekBar seekbar;
            RelativeLayout body_lay;
            ProgressWheel progressbar;
            Context context;

            ReceiveVoiceHolder(View itemView) {
                super(itemView);
                context = itemView.getContext();
                icon = itemView.findViewById(R.id.icon);
                duration = itemView.findViewById(R.id.duration);
                msg_time = itemView.findViewById(R.id.text_message_time);
                seekbar = itemView.findViewById(R.id.song_seekbar);
                progressbar = itemView.findViewById(R.id.progressbar);
                downloadIcon = itemView.findViewById(R.id.downloadicon);
                file_name = itemView.findViewById(R.id.filename);
                body_lay = itemView.findViewById(R.id.body_lay);
            }

            void bind(final ChannelMessage message, int pos) {
                itemView.setTag("poss" + pos);
                msg_time.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                seekbar.getProgressDrawable().setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.MULTIPLY);

                duration.setVisibility(View.INVISIBLE);
//            duration.setText(message.duration);
                icon.setImageResource(R.drawable.pause_icon_white);

                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                if (storageManager.checkifFileExists(message.attachment, message.messageType, "receive")) {
                    downloadIcon.setVisibility(View.GONE);
                    progressbar.setVisibility(View.GONE);
                    icon.setEnabled(true);
                    duration.setVisibility(View.VISIBLE);
                    duration.setText(milliSecondsToTimer(mediaDuration(getAdapterPosition(), "receive")));
                    seekbar.setEnabled(true);
                } else {
                    downloadIcon.setVisibility(View.VISIBLE);
                    progressbar.setVisibility(View.VISIBLE);
                    duration.setVisibility(View.GONE);
                    icon.setEnabled(false);
                    seekbar.setEnabled(false);
                    progressbar.stopSpinning();
                }


                body_lay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!chatLongPressed) {
                            if (storageManager.checkifFileExists(message.attachment, message.messageType, "receive")) {
                                body_lay.setEnabled(false);
                            } else {
                                if (isNetworkConnected().equals(NOT_CONNECT)) {
                                    networkSnack();
                                } else {
                                    DownloadFiles downloadFiles = new DownloadFiles(ChannelChatActivity.this) {
                                        @Override
                                        protected void onPostExecute(String downPath) {
                                            progressbar.setVisibility(View.GONE);
                                            progressbar.stopSpinning();
                                            downloadIcon.setVisibility(View.GONE);
                                            if (downPath == null) {
                                                Log.v("Download Failed", "Download Failed");
                                                Toast.makeText(getApplicationContext(), getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                            } else {
                                                duration.setVisibility(View.VISIBLE);
                                                duration.setText(milliSecondsToTimer(mediaDuration(getAdapterPosition(), "receive")));
                                                //Toast.makeText(mContext, getString(R.string.downloaded), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    };
                                    downloadFiles.execute(Constants.CHAT_IMG_PATH + message.attachment, message.messageType);
                                    progressbar.setVisibility(View.VISIBLE);
                                    progressbar.spin();
                                    downloadIcon.setVisibility(View.VISIBLE);
                                    icon.setEnabled(true);
                                    duration.setEnabled(true);
                                    seekbar.setEnabled(true);

                                }
                            }
                        }
                    }
                });

                //ReceiveVoiceHolder
                icon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!chatLongPressed) {
                            duration.setVisibility(View.VISIBLE);
                            if (storageManager.checkifFileExists(message.attachment, message.messageType, "receive")) {
                                playMedia(context, getAdapterPosition(), "receive");
                            } else {
                                Log.i(TAG, "channelchatonClick3: "+message+storageManager);
                                Toast.makeText(ChannelChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                            }
                        }

                    }

                });

            }
        }

    }

    class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.MyViewHolder> {

        Dialog dialog;
        List<String> reportList;

        public ReportAdapter(List<String> reportList, Dialog dialog) {
            this.reportList = reportList;
            this.dialog = dialog;
        }

        @NonNull
        @Override
        public ReportAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.report_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ReportAdapter.MyViewHolder holder, int position) {
            holder.texItem.setText(reportList.get(position));
        }

        @Override
        public int getItemCount() {
            return reportList.size();
        }

        private class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView texItem;

            MyViewHolder(@NonNull View itemView) {
                super(itemView);
                texItem = itemView.findViewById(R.id.itemText);

                texItem.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.itemText) {
                    reportChannel(channelId, reportList.get(getAdapterPosition()), false);
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isPermissionCall)
            askForSystemOverlayPermission();
        else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isPermissionCall = false;
                    askForSystemOverlayPermission();
                }
            }, 4000);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                voiceFab.setRippleColor(color.toConversationColor(ChannelChatActivity.this));
                voiceFab.setBackgroundTintList(new ColorStateList(new int[][]{
                        new int[]{android.R.attr.state_pressed},
                        new int[]{color.toConversationPColor(ChannelChatActivity.this)},


                }, new int[]{
                        color.toConversationPColor(ChannelChatActivity.this),
                        color.toConversationColor(ChannelChatActivity.this),
                }));
            }
        },600);
        Speech.init(this, getPackageName());
        initTextToSpeech();
        if (ChannelInfoActivity.isChannelUnsubscribe) {
            ChannelInfoActivity.isChannelUnsubscribe = false;
            finish();
        } else {

            tempChannelId = channelId;
            if (!("" + channelData.channelAdminId).equalsIgnoreCase(GetSet.getUserId())) {
                bottomLay.setVisibility(View.GONE);
            }
            ApplicationClass.onShareExternal = false;
            if (channelData.channelCategory.equalsIgnoreCase(Constants.TAG_ADMIN_CHANNEL)) {
                AdminChannel.Result adminData = dbhelper.getAdminChannelInfo(channelId);
                Glide.with(getApplicationContext()).load(Constants.CHANNEL_IMG_PATH + adminData.channelImage)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.profile_square).error(R.drawable.profile_square))
                        .into(userimage);
                username.setText(adminData.channelName);
                txtMembers.setText("" + adminData.channelDes);
            } else {
                getChannelInfo(channelId);
            }
        }
    }
    @Override
    public void onPause() {
        tempChannelId = "";
        editText.setError(null);
        super.onPause();
        if (bottomLayout.getVisibility() == View.VISIBLE)
            onBackPressed();
        Speech.getInstance().shutdown();
        if (isBound) {
            try {
                unbindService(myConnection);
            } catch (Exception e) {

            }

        }
        if (ttobj != null) {
            ttobj.stop();
            ttobj.shutdown();
        }
    }

    @Override
    public void onBackPressed() {
        stopMedia();
        if (bottomLayout.getVisibility() == View.VISIBLE) {
            disableWakeLock();
            isSppechEnable = false;
            bottomLayout.setVisibility(View.GONE);
            ((View)recyclerView.getParent()).setPadding(Screen.dp(0),Screen.dp(0),Screen.dp(0),Screen.dp(0) );
            //((ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams()).bottomMargin = Screen.dp(0);
            linearLayout.setVisibility(View.GONE);
            fabLay.setVisibility(View.VISIBLE);
            if(myService!=null)
            myService.counterFab.setVisibility(View.VISIBLE);
            if (Speech.getInstance().isListening())
                Speech.getInstance().stopListening();
            return;
        }
        if (selectedChatPos.size() > 0) {
            selectedChatPos.clear();
            messageListAdapter.notifyDataSetChanged();
            chatUserLay.setVisibility(View.VISIBLE);
            forwordLay.setVisibility(View.GONE);
            chatLongPressed = false;
        } else {
            if (isFromNotification) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra(Constants.IS_FROM, "channel");
                startActivity(intent);
                finish();
            } else {
                finish();
            }
        }
    }

    private void askForSystemOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            isPermissionCall = true;
            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 878);
        } else {
            //
            Intent intent = new Intent(this, FloatingWidgetService.class);
            //  startService(new Intent(ChatActivity.this, FloatingWidgetService.class));
            if (Utils.isChannelAdmin(channelData, GetSet.getUserId())) {
            bindService(intent, myConnection, Context.BIND_AUTO_CREATE);}
        }
    }

    int bottomBarHeight =0;
    void showBottomSheet() {
        bottomLayout.setVisibility(View.VISIBLE);
        ((View)recyclerView.getParent()).setPadding(Screen.dp(0),Screen.dp(0),Screen.dp(0),Screen.dp(200) - Screen.dp(72));
        recyclerView.scrollToPosition(0);
        // ((ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams()).bottomMargin = Screen.dp(200) ;
        if(myService!=null)
        myService.counterFab.setVisibility(View.GONE);
        voiceFab.performClick();

    }

    private ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            FloatingWidgetService.MyLocalBinder binder = (FloatingWidgetService.MyLocalBinder) service;
            myService = binder.getService();
            System.out.println("fjgbj bounded connected");
            myService.setOnFabClickListener(new FloatingWidgetService.FabClickListener() {
                @Override
                public void OnFabClicked() {
                    enableWakeLock();
                    isSppechEnable = true;
                    showBottomSheet();

                }
            });

            isBound = true;
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
    private SpeechProgressView progress;

    protected PowerManager powerManager;
    protected PowerManager.WakeLock wakeLock;
    protected int field = 0x00000020;

    public void enableWakeLock() {
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(field, getLocalClassName());

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    @Override
    public void onStartOfSpeech() {

    }

    @Override
    public void onSpeechRmsChanged(float value) {

    }
    String sppechText = "";
    @Override
    public void onSpeechPartialResults(List<String> results) {
        for (String partial : results) {
            sppechText = sppechText + (partial + " ");
        }
    }
    void setSpeekProgress(){
        progress = findViewById(R.id.progress);
        int[] colors = {
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.darker_gray),
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.holo_orange_dark),
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
        };
        progress.setColors(colors);

    }

    @Override
    public void onSpeechResult(String result) {

        //     Toast.makeText(this, "lekem" + result + " ss " , Toast.LENGTH_SHORT).show();
        if (TextUtils.isEmpty(result) || TextUtils.isEmpty(result.replaceAll(" ", ""))) {
            result = sppechText.trim();
        }
        if (!TextUtils.isEmpty(result)) {
            //       Toast.makeText(this, "sendin  " + result, Toast.LENGTH_SHORT).show();
            editText.setText(result);
            send.performClick();
            //messenger().sendMessage(chatFragment.peer, result);
            retry = 1;
        } else {
            retry--;
            if (retry == 0) {
                retry = 1;
            }
            else {
                speech("If you missed retry after beep", false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onRecordAudioPermissionGranted();
                    }
                }, 1500);
            }
        }

        sppechText = "";
        /*speechButton.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.GONE);
        if(!TextUtils.isEmpty(result))
            messenger().sendMessage(peer, result);
        res.findViewById(R.id.container).setPadding(0, 0, 0, 0);
        speech.setVisibility(View.GONE);*/
        if (Speech.getInstance().isListening())
            Speech.getInstance().stopListening();
        fabLay.setVisibility(View.VISIBLE);

        linearLayout.setVisibility(View.GONE);
    }
    public void disableWakeLock() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }


    void initTextToSpeech(){
        ttobj = new TextToSpeech(ChannelChatActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    setLanguage();
                    System.out.println("xxxspekkking");
                    ttobj.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            System.out.println("xxxspekkking start");
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            System.out.println("xxxspekkking enjk " + utteranceId + " " + mostRecentUtteranceID);
                            if (utteranceId.equalsIgnoreCase(mostRecentUtteranceID)) {


                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        speech("Please say your reply after beep", false);
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                retry = 3;
                                                speak();


                                            }
                                        }, 2000);

                                    }
                                });


                            }
                        }

                        @Override
                        public void onError(String utteranceId) {

                        }
                    });
                }

            }
        });
    }


    String mostRecentUtteranceID;

    public void speech(String txt, boolean autoReply) {
        if (!isSppechEnable || (Speech.getInstance() != null && Speech.getInstance().isListening())) {
            return;
        }
        if (ttobj != null && ttobj.isSpeaking())

        {

            ttobj.stop();
        }
        if (TextUtils.isEmpty(txt))

        {
            return;
        }
        if (autoReply) {
            mostRecentUtteranceID = (new Random().nextInt() % 9999999) + "";
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, mostRecentUtteranceID);
            System.out.println("xxxspekkking" + mostRecentUtteranceID);
            ttobj.speak(txt, TextToSpeech.QUEUE_ADD, params);
        } else {
            mostRecentUtteranceID = null;
            ttobj.speak(txt, TextToSpeech.QUEUE_ADD, null);
        }
    }

    public void speak() {
        if (Speech.getInstance().isListening()) {
            Speech.getInstance().stopListening();
        } else {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
            if (ContextCompat.checkSelfPermission(ChannelChatActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

                onRecordAudioPermissionGranted();

            } else {
                ActivityCompat.requestPermissions(ChannelChatActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST);
            }

        }
    }
    public class LanguageDetailsChecker extends BroadcastReceiver {
        private List<String> supportedLanguages;

        private String languagePreference;
        LanguageAdapter indiaAdapter, otherAdapter;
        RecyclerView indiaList, otherList;

        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle results = getResultExtras(true);
            System.out.println("languagePreference " + results.keySet().size());

            if (results.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)) {

                languagePreference =
                        results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
                if (getSharedPreferences("wall", Context.MODE_PRIVATE).getString("speech_lang", null) == null) {
                    getSharedPreferences("wall", Context.MODE_PRIVATE).edit().putString("speech_lang", languagePreference).commit();
                    setLanguage();
                }

                setDefault(getSharedPreferences("wall", Context.MODE_PRIVATE).getString("speech_lang", "en"));
                System.out.println("languagePreference " + languagePreference);
            }
            if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {

                supportedLanguages =
                        results.getStringArrayList(
                                RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
                ArrayList<String> langCodesIN = new ArrayList<>();
                ArrayList<String> langCodes = new ArrayList<>();
                for (String lang : supportedLanguages) {
                    if (lang.contains("-IN")) {
                        langCodesIN.add(lang);
                    } else {
                        langCodes.add(lang);
                    }

                }
                indiaAdapter = new LanguageAdapter(langCodesIN);
                indiaAdapter.setOnLanguageSelection(new LanguageAdapter.OnLanguageSelection() {
                    @Override
                    public void onLanguageSelected(String lng) {
                        getSharedPreferences("wall", Context.MODE_PRIVATE).edit().putString("speech_lang", lng).commit();
                        setDefault(lng);
                        setLanguage();
                    }
                });
                otherAdapter = new LanguageAdapter(langCodes);
                otherAdapter.setOnLanguageSelection(new LanguageAdapter.OnLanguageSelection() {
                    @Override
                    public void onLanguageSelected(String lng) {
                        getSharedPreferences("wall", Context.MODE_PRIVATE).edit().putString("speech_lang", lng).commit();
                        setDefault(lng);

                    }
                });
                indiaList = findViewById(R.id.myLangList);
                otherList = findViewById(R.id.allLangList);
                indiaList.addItemDecoration(new DividerItemDecoration(ChannelChatActivity.this, RecyclerView.VERTICAL));
                otherList.addItemDecoration(new DividerItemDecoration(ChannelChatActivity.this, RecyclerView.VERTICAL));
                indiaList.setLayoutManager(new LinearLayoutManager(ChannelChatActivity.this));
                otherList.setLayoutManager(new LinearLayoutManager(ChannelChatActivity.this));
                indiaList.setAdapter(indiaAdapter);
                otherList.setAdapter(otherAdapter);
                System.out.println("languagePreference " + supportedLanguages.get(0));


            }
            if (results.containsKey("SUPPORTED_LANGUAGE_NAMES")) {


            }
        }
    }

    void setLanguage() {
        Locale mLocale = Locale.getDefault();
        String toLangs = getSharedPreferences("wall", Context.MODE_PRIVATE).getString("speech_lang", "en");
        if (toLangs.contains("-")) {
            toLangs = toLangs.substring(0, toLangs.indexOf("-"));
        }
        if (toLangs.equalsIgnoreCase("zh-Hans"))
            ttobj.setLanguage(Locale.SIMPLIFIED_CHINESE);
        else if (toLangs.equalsIgnoreCase("zh-Hant"))
            ttobj.setLanguage(Locale.TRADITIONAL_CHINESE);
        else
            ttobj.setLanguage(LocaleUtils.toLocale(toLangs));
    }

    void setDefault(String ln) {
        Locale loc = null;
        if (ln.contains("-")) {
            loc = new Locale("", ln.substring(ln.indexOf("-") + 1, ln.length()));
            ln = ln.substring(0, ln.indexOf("-"));

        }//Locale loc = new Locale("","IN");
        String co = loc != null ? loc.getDisplayCountry() : "";
        co = TextUtils.isEmpty(co) ? "" : co.trim();
        co = TextUtils.isEmpty(co) ? "" : " - " + co;
        ((TextView) findViewById(R.id.defaultLang)).setText(LocaleUtils.toLocale(ln).getDisplayName() + co);
    }

    private void onRecordAudioPermissionGranted() {
        fabLay.setVisibility(View.GONE);

        linearLayout.setVisibility(View.VISIBLE);

        try {
            System.out.println("xxxspekkking enjk statlis");
            Speech.getInstance().startListening(progress, ChannelChatActivity.this);

        } catch (SpeechRecognitionNotAvailable exc) {
            System.out.println("xxxspekkking enjk grnt ex " + exc.getLocalizedMessage());
            fabLay.setVisibility(View.VISIBLE);

            linearLayout.setVisibility(View.GONE);
            showSpeechNotSupportedDialog();

        } catch (GoogleVoiceTypingDisabledException exc) {
            fabLay.setVisibility(View.VISIBLE);
            System.out.println("xxxspekkking enjk doodo " + exc.getLocalizedMessage());
            linearLayout.setVisibility(View.GONE);
            showEnableGoogleVoiceTyping();
        }
    }


    private void showEnableGoogleVoiceTyping() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChannelChatActivity.this);
        builder.setMessage("typing")
                .setCancelable(false)
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                })
                .show();
    }

    private void showSpeechNotSupportedDialog() {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(ChannelChatActivity.this);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(ChannelChatActivity.this);
        builder.setMessage("speech not available")
                .setCancelable(false)
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
    }
}
