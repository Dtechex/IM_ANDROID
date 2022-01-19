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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
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
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.devlomi.record_view.OnRecordListener;
import com.devlomi.record_view.RecordButton;
import com.devlomi.record_view.RecordView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopytime.apprtc.util.AppRTCUtils;
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
import com.loopytime.im.status.SingleStoryActivity;
import com.loopytime.model.ContactsData;
import com.loopytime.model.MessagesData;
import com.loopytime.model.StatusDatas;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;
import com.makeramen.roundedimageview.RoundedImageView;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.shadow.apache.commons.lang3.LocaleUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

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
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WAKE_LOCK;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.im.ChatActivity.MessageListAdapter.VIEW_TYPE_DATE;
import static com.loopytime.utils.Constants.TAG_MY_CONTACTS;
import static com.loopytime.utils.Constants.TAG_NOBODY;
import static com.loopytime.utils.Constants.TAG_USER_ID;
import static com.loopytime.utils.Constants.TRUE;

public class ChatActivity extends BaseActivity implements View.OnClickListener,
        SocketConnection.ChatCallbackListener, TextWatcher, DeleteAdapter.deleteListener, SpeechDelegate {
    private static final String TAG = ChatActivity.class.getSimpleName();
    public static final int ACTIVITY_RECORD_SOUND = 0;
    public static String tempUserId = "";
    public static ArrayList<RecyclerView.ViewHolder> listHolder = new ArrayList<>();
    EditText editText;
    TextToSpeech ttobj;
    boolean isPermissionCall = false;
    String userId;
    int retry = 1;
    FloatingActionButton voiceFab;
    List<MessagesData> messagesList = new ArrayList<>();
    RecyclerView recyclerView;
    private final int PERMISSIONS_REQUEST = 19876;
    TextView username, online, audioTime;
    RelativeLayout chatUserLay, mainLay, attachmentsLay, imageViewLay, bottomLay, forwordLay;
    ImageView attachbtn, optionbtn, backbtn, send, audioCallBtn, videoCallBtn, cameraBtn,
            galleryBtn, fileBtn, audioBtn, locationBtn, contactBtn, imageView, closeBtn, forwordBtn, copyBtn, deleteBtn;
    CircleImageView userimage;
    Display display;
    boolean visible, stopLoading = false, meTyping, chatLongPressed = false, isViewVisible = false, isRecording = false;
    int totalMsg;
    SocketConnection socketConnection;
    LinearLayoutManager linearLayoutManager;
    MessageListAdapter messageListAdapter;
    DatabaseHandler dbhelper;
    StorageManager storageManager;
    ApiInterface apiInterface;
    ArrayList<String> pathsAry = new ArrayList<>();
    Timer onlineTimer = new Timer();
    Handler handler = new Handler();
    Runnable runnable;
    ContactsData.Result results;
    EndlessRecyclerOnScrollListener endlessRecyclerOnScrollListener;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    ArrayList<MessagesData> selectedChatPos = new ArrayList<>();
    String recordVoicePath = null;
    MediaRecorder mediaRecorder;
    RecordView recordView;
    RecordButton recordButton;

    LinearLayout editLay, excryptText;
    int playingPosition = -1;
    private boolean isFromNotification = false;
    private Dialog permissionDialog;
    private Gson gson;
    View bottomLayout, fabLay, linearLayout;
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

    public static String getAudioTime(long time) {
        SimpleDateFormat timeFormatter = new SimpleDateFormat("m:ss", Locale.getDefault());
        time *= 1000;
        timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return timeFormatter.format(new Date(time));
    }

    public static String getMimeType(String fileUrl) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);
        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        detailsIntent.setPackage("com.google.android.googlequicksearchbox");
        sendOrderedBroadcast(
                detailsIntent, null, new LanguageDetailsChecker(), null, 1234, null, null);
        gson = new GsonBuilder().create();
        if (getIntent().getStringExtra("notification") != null) {
            Constants.isChatOpened = true;
            isFromNotification = true;
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancelAll();
            }
        }
        if (Constants.chatContext != null && Constants.isChatOpened) {
            ((Activity) Constants.chatContext).finish();
        }
        getWindow().setBackgroundDrawableResource(R.drawable.chat_bg);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        pref = ChatActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        Constants.chatContext = this;
        setSpeekProgress();
        recyclerView = findViewById(R.id.recyclerView);
        send = findViewById(R.id.send);
        editText = findViewById(R.id.editText);
        chatUserLay = findViewById(R.id.chatUserLay);
        userimage = findViewById(R.id.userImg);
        username = findViewById(R.id.userName);
        online = findViewById(R.id.online);
        attachbtn = findViewById(R.id.attachbtn);
        audioCallBtn = findViewById(R.id.audioCallBtn);
        videoCallBtn = findViewById(R.id.videoCallBtn);
        optionbtn = findViewById(R.id.optionbtn);
        backbtn = findViewById(R.id.backbtn);
        bottomLay = findViewById(R.id.bottom);
        mainLay = findViewById(R.id.mainLay);
        attachmentsLay = findViewById(R.id.attachmentsLay);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        voiceFab = findViewById(R.id.fabSpeak);
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
        editLay = findViewById(R.id.editLay);
        recordView = findViewById(R.id.record_view);
        recordButton = findViewById(R.id.record_button);
        excryptText = findViewById(R.id.excryptText);
        voiceFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "9");
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "voice typing ");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Activity");
                ApplicationClass.getInstance().mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                speak();
            }
        });
        if (ApplicationClass.isRTL()) {
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        socketConnection = SocketConnection.getInstance(this);
        SocketConnection.getInstance(this).setChatCallbackListener(this);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        dbhelper = DatabaseHandler.getInstance(this);
        storageManager = StorageManager.getInstance(this);
        display = getWindowManager().getDefaultDisplay();
        userId = getIntent().getExtras().getString("user_id");
        tempUserId = userId;

        // set visibility status
        chatUserLay.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        audioCallBtn.setVisibility(View.VISIBLE);
        videoCallBtn.setVisibility(View.VISIBLE);
        optionbtn.setVisibility(View.VISIBLE);
        bottomLayout = findViewById(R.id.speech);
        bottomLayout.setVisibility(View.GONE);
        fabLay = findViewById(R.id.fabLay);
        linearLayout = findViewById(R.id.linearLayout);
        bottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        Log.v("userId", "userId=" + userId);
        results = dbhelper.getContactDetail(userId);

        setVoiceRecorder();


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(results.user_name, 0);
        }

        username.setText(results.user_name);
        if (!results.blockedme.equals("block")) {
            DialogActivity.setProfileImage(dbhelper.getContactDetail(userId), userimage, this);
            online.setVisibility(View.VISIBLE);
        } else {
            Glide.with(ChatActivity.this).load(R.drawable.temp)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                    .into(userimage);
            online.setVisibility(View.GONE);
        }
        if (results.blockedbyme.equals("block")) {
            online.setVisibility(View.GONE);
        }

        totalMsg = dbhelper.getMessagesCount(GetSet.getUserId() + userId);
        Log.v("totalMsg", "totalMsg=" + totalMsg);

        try {
            messagesList.addAll(getMessagesAry(dbhelper.getMessages(GetSet.getUserId() + userId, "0", "20"), null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        showEncryptionText();
        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        messageListAdapter = new MessageListAdapter(this, messagesList);
        recyclerView.setAdapter(messageListAdapter);

        DividerItemDecoration divider = new DividerItemDecoration(recyclerView.getContext(),
                linearLayoutManager.getOrientation());
        divider.setDrawable(getResources().getDrawable(R.drawable.emptychat_divider));
        recyclerView.addItemDecoration(divider);

        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.scrollToPosition(0);
            }
        });
       // backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));

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
if(!userId.equalsIgnoreCase(GetSet.getUserId())) {
    onlineTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() {
            //Function call every second
            if (!results.blockedme.equals("block") && !results.blockedbyme.equals("block")) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_USER_ID, GetSet.getUserId());
                    jsonObject.put(Constants.TAG_CONTACT_ID, userId);
                    Log.v("online", "online=" + jsonObject);
                    socketConnection.online(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }, 0, 2000);
}
        whileViewChat();


        endlessRecyclerOnScrollListener = new EndlessRecyclerOnScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.v("current_page", "current_page=" + page + "&totalItems=" + totalItemsCount);
                final List<MessagesData> tmpList = new ArrayList<>();
                try {
                    tmpList.addAll(dbhelper.getMessages(GetSet.getUserId() + userId, String.valueOf(page * 20), "20"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

        if (SocketConnection.onUpdateTabIndication != null) {
            SocketConnection.onUpdateTabIndication.updateIndication();
        }

        recyclerView.addOnItemTouchListener(chatItemClick(this, recyclerView));
    }

    private void showEncryptionText() {
        if (messagesList.isEmpty()) {
            excryptText.setVisibility(View.VISIBLE);
        } else {
            excryptText.setVisibility(View.GONE);
        }
    }

    private void setVoiceRecorder() {

        recordView.setCounterTimeColor(Color.parseColor("#a3a3a3"));
        recordView.setSmallMicColor(ContextCompat.getColor(ChatActivity.this, R.color.colorAccent));
        recordView.setSlideToCancelTextColor(Color.parseColor("#a3a3a3"));
        recordView.setLessThanSecondAllowed(false);
        recordView.setSlideToCancelText(getString(R.string.slide_to_cancel));
        recordView.setCustomSounds(0, 0, 0);

        recordButton.setRecordView(recordView);

        /*if(ApplicationClass.isRTL()){
            float cancelBound = ApplicationClass.pxToDp(getApplicationContext(),ApplicationClass.getWidth(getApplicationContext())) -
                    ApplicationClass.dpToPx(getApplicationContext(), (int) recordView.getCancelBounds());
            recordView.setCancelBounds(cancelBound);
        }

        Log.d(TAG, "setVoiceRecorder: " + recordView.getCancelBounds() + "\n"+ApplicationClass.dpToPx(getApplicationContext(),8)
                + "\n" );*/

        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else if (results.blockedbyme.equals("block")) {
            blockChatConfirmDialog("unblock", "sent");
        } else {
            recordView.setOnRecordListener(new OnRecordListener() {
                @Override
                public void onStart() {
                    if (ContextCompat.checkSelfPermission(ChatActivity.this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(ChatActivity.this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        recordView.setOnRecordListener(null);
                        ActivityCompat.requestPermissions(ChatActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, 111);
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
                            ApplicationClass.hideSoftKeyboard(ChatActivity.this, recordView);
                            editLay.setVisibility(View.VISIBLE);
                            recordView.setVisibility(View.GONE);
                            if (!results.blockedme.equals("block") && !results.blockedbyme.equals("block")) {
                                runnable = new Runnable() {
                                    public void run() {
                                        meTyping = false;
                                        try {
                                            JSONObject jsonObject = new JSONObject();
                                            jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                                            jsonObject.put(Constants.TAG_RECEIVER_ID, userId);
                                            jsonObject.put(Constants.TAG_CHAT_ID, userId + GetSet.getUserId());
                                            jsonObject.put("type", "untyping");
                                            //  socketConnection.typing(jsonObject);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };
                                handler.postDelayed(runnable, 1000);
                            }
                        }
                    }, 1000);

                }

                @Override
                public void onFinish(long recordTime) {
                    if (recordTime > 1000) {
                        editText.requestFocus();
                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                            networkSnack();
                        } else if (results.blockedbyme.equals("block")) {
                            blockChatConfirmDialog("unblock", "sent");
                        } else {
                            if (null != mediaRecorder) {
                                try {
                                    mediaRecorder.stop();
                                } catch (RuntimeException ignored) {
                                }
                            }

                            if (!results.blockedme.equals("block") && !results.blockedbyme.equals("block")) {
                                runnable = new Runnable() {
                                    public void run() {
                                        meTyping = false;
                                        try {
                                            JSONObject jsonObject = new JSONObject();
                                            jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                                            jsonObject.put(Constants.TAG_RECEIVER_ID, userId);
                                            jsonObject.put(Constants.TAG_CHAT_ID, userId + GetSet.getUserId());
                                            jsonObject.put("type", "untyping");
                                            socketConnection.typing(jsonObject);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };
                                handler.postDelayed(runnable, 1000);
                            }

                            editLay.setVisibility(View.VISIBLE);
                            recordView.setVisibility(View.GONE);

                            MessagesData mdata = updateDBList("audio", "", recordVoicePath/*,String.valueOf(getAudioTime(Time))*/);

                            Intent service = new Intent(ChatActivity.this, FileUploadService.class);
                            Bundle b = new Bundle();
                            b.putSerializable("mdata", mdata);
                            b.putString("filepath", recordVoicePath);
                            b.putString("chatType", "chat");
                            service.putExtras(b);
                            startService(service);
                            Log.d("RecordView", "onFinish");
                        }
                    } else {
                        stopMedia();
                        editLay.setVisibility(View.VISIBLE);
                        recordView.setVisibility(View.GONE);
                        ApplicationClass.hideSoftKeyboard(ChatActivity.this, recordView);

                        Toast.makeText(ChatActivity.this, getString(R.string.less_than_second), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onLessThanSecond() {
                    stopMedia();
                    editLay.setVisibility(View.VISIBLE);
                    recordView.setVisibility(View.GONE);
                    ApplicationClass.hideSoftKeyboard(ChatActivity.this, recordView);

                    Toast.makeText(ChatActivity.this, getString(R.string.less_than_second), Toast.LENGTH_SHORT).show();
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
        if (ContextCompat.checkSelfPermission(ChatActivity.this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(ChatActivity.this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ChatActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, 111);

        } else if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else if (results.blockedbyme.equals("block")) {
            blockChatConfirmDialog("unblock", "sent");
        } else {
            if (!results.blockedme.equals("block") && !results.blockedbyme.equals("block")) {
                if (runnable != null)
                    handler.removeCallbacks(runnable);
                if (!meTyping) {
                    meTyping = true;
                }
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                    jsonObject.put(Constants.TAG_RECEIVER_ID, userId);
                    jsonObject.put(Constants.TAG_CHAT_ID, userId + GetSet.getUserId());
                    jsonObject.put("type", "recording");
                    // socketConnection.typing(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            String fileName = (getString(R.string.app_name) + "_" + System.currentTimeMillis() + ".MP3").replaceAll(" ", "");

            storageManager.createDirectory(StorageManager.TAG_AUDIO_SENT);

            recordVoicePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + storageManager.getFolderPath(StorageManager.TAG_AUDIO_SENT) + fileName;
            MediaRecorderReady();
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
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

    @Override
    public void onNetworkChange(boolean isConnected) {
        Log.v("onNetwork", "chat=" + isConnected);
        if (online != null) {
            if (isConnected) {
                online.setVisibility(View.VISIBLE);
            } else {
                online.setVisibility(View.GONE);
            }
        }
    }

    private List<MessagesData> getMessagesAry(List<MessagesData> tmpList, MessagesData lastData) {
        List<MessagesData> msgList = new ArrayList<>();
        if (tmpList.size() == 0 && lastData != null) {
            MessagesData mdata = new MessagesData();
            mdata.message_type = "date";
            mdata.chat_time = lastData.chat_time;
            msgList.add(mdata);
            Log.v("diff", "diff pos=ss" + "&msg=" + lastData.message);
        } else {
            for (int i = 0; i < tmpList.size(); i++) {
                Calendar cal1 = Calendar.getInstance();
                cal1.setTimeInMillis(Long.parseLong(tmpList.get(i).chat_time) * 1000L);

                if (i + 1 < tmpList.size()) {
                    Calendar cal2 = Calendar.getInstance();
                    cal2.setTimeInMillis(Long.parseLong(tmpList.get(i + 1).chat_time) * 1000L);

                    boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);

                    if (sameDay) {
                        msgList.add(tmpList.get(i));
                        Log.v("diff", "same pos=" + i + "&msg=" + tmpList.get(i).message);
                    } else {
                        msgList.add(tmpList.get(i));
                        MessagesData mdata = new MessagesData();
                        mdata.message_type = "date";
                        mdata.chat_time = tmpList.get(i).chat_time;
                        msgList.add(mdata);
                        Log.v("diff", "diff pos=" + i + "&msg=" + tmpList.get(i).message);
                    }
                } else {
                    msgList.add(tmpList.get(i));
                }
            }
        }
        return msgList;
    }

    @Override
    public void onReceiveChat(final MessagesData mdata) {
        if(userId.equalsIgnoreCase(GetSet.getUserId())) {
            return;
        }
        runOnUiThread(() -> {
            Log.v(TAG, "onReceiveChat");
            mdata.message = ApplicationClass.decryptMessage(mdata.message);
            mdata.attachment = ApplicationClass.decryptMessage(mdata.attachment);
            mdata.lat = ApplicationClass.decryptMessage(mdata.lat);
            mdata.lon = ApplicationClass.decryptMessage(mdata.lon);
            mdata.contact_name = ApplicationClass.decryptMessage(mdata.contact_name);
            mdata.contact_phone_no = ApplicationClass.decryptMessage(mdata.contact_phone_no);
            mdata.contact_country_code = ApplicationClass.decryptMessage(mdata.contact_country_code);
            mdata.statusData = ApplicationClass.decryptMessage(mdata.statusData);
            if (mdata.user_id.equals(userId) || mdata.receiver_id.equals(userId)) {
                whileViewChat();
                if (!mdata.message_type.equals(Constants.TAG_ISDELETE)) {
                    messagesList.add(0, mdata);
                    messageListAdapter.notifyItemInserted(0);
                    recyclerView.smoothScrollToPosition(0);
                    showEncryptionText();
                } else {
                    for (MessagesData data : messagesList) {
                        if (ApplicationClass.isStringNotNull(mdata.message_id) && ApplicationClass.isStringNotNull(data.message_id) && data.message_id.equals(mdata.message_id)) {
                            data.message_type = mdata.message_type;
                            messageListAdapter.notifyItemChanged(messagesList.indexOf(data));
                            dbhelper.updateMessageReadStatus(GetSet.getUserId() + userId, GetSet.getUserId());
                            break;
                        }
                    }
                }
            }

        });
    }

    @Override
    public void onEndChat(final String message_id, final String sender_id, final String receiverId) {
        runOnUiThread(() -> {
            Log.v("onEndChat", "onEndChat");
            for (int i = 0; i < messagesList.size(); i++) {
                if (messagesList.get(i).message_id != null &&
                        messagesList.get(i).message_id.equals(message_id)) {
                    messagesList.get(i).delivery_status = "sent";
                    break;
                }
            }
            messageListAdapter.notifyDataSetChanged();
        });
    }

    private void whileViewChat() {
        Log.e(TAG, "whileViewChat: " + GetSet.getUserId() + userId);
        dbhelper.updateMessageReadStatus(GetSet.getUserId() + userId, GetSet.getUserId());
        dbhelper.resetUnseenMessagesCount(userId);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_SENDER_ID, userId);
                    jsonObject.put(Constants.TAG_RECEIVER_ID, GetSet.getUserId());
                    jsonObject.put(Constants.TAG_CHAT_ID, userId + GetSet.getUserId());
                    Log.v(TAG, "chatViewed: " + jsonObject);
                    socketConnection.chatViewed(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, 1000);
    }

    @Override
    public void onViewChat(final String chat_id, final String sender_id, final String receiverId) {
        runOnUiThread(new Runnable() {
            public void run() {
                Log.v("onViewChat", "onViewChat");
                if (chat_id.equals(GetSet.getUserId() + userId)) {
                    for (int i = 0; i < messagesList.size(); i++) {
                        if (messagesList.get(i).delivery_status != null && messagesList.get(i).delivery_status.equals("sent")) {
                            messagesList.get(i).delivery_status = "read";
                        }
                    }
                    messageListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onlineStatus(final JSONObject data) {
        runOnUiThread(new Runnable() {
            public void run() {
                //Log.v("onlineStatus", "onlineStatus="+data);
                try {
                    String contactId = data.getString(Constants.TAG_CONTACT_ID);
                    if (contactId.equals(userId)) {
                        if (!results.blockedme.equals("block") && !results.blockedbyme.equals("block")) {
                            online.setVisibility(View.VISIBLE);
                            if (data.get("livestatus").equals("online")) {
                                online.setText(getString(R.string.online));
                                online.setSelected(false);
                            } else if (data.get("livestatus").equals("offline")) {
                                ContactsData.Result result = dbhelper.getContactDetail(contactId);
                                if (result.privacy_last_seen.equalsIgnoreCase(TAG_MY_CONTACTS)) {
                                    if (result.contactstatus.equalsIgnoreCase(TRUE)) {
                                        online.setText(getString(R.string.last_seen) + " " + Utils.getFormattedDate(ChatActivity.this, Long.parseLong(data.getString("lastseen"))));
                                        online.setSelected(true);
                                    } else {
                                        online.setText("");
                                        online.setSelected(false);
                                    }
                                } else if (result.privacy_last_seen.equalsIgnoreCase(TAG_NOBODY)) {
                                    online.setText("");
                                    online.setSelected(false);
                                } else {
                                    online.setText(getString(R.string.last_seen) + " " + Utils.getFormattedDate(ChatActivity.this, Long.parseLong(data.getString("lastseen"))));
                                    online.setSelected(true);
                                }
                            }
                        } else {
                            online.setVisibility(View.GONE);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onListenTyping(final JSONObject data) {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    String chatId = data.getString(Constants.TAG_CHAT_ID);
                    if (chatId.equals(GetSet.getUserId() + userId)) {
                        if (data.get("type").equals("typing")) {
                            online.setText(getString(R.string.typing));
                        } else if (data.get("type").equals("recording")) {
                            online.setText(getString(R.string.recording));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onBlockStatus(final JSONObject data) {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    String senderId = data.getString(Constants.TAG_SENDER_ID);
                    String receiverId = data.getString(Constants.TAG_RECEIVER_ID);
                    String type = data.getString(Constants.TAG_TYPE);
                    if (senderId.equals(userId)) {
                        results = dbhelper.getContactDetail(userId);
                        if (!results.blockedme.equals("block")) {
                            DialogActivity.setProfileImage(dbhelper.getContactDetail(userId), userimage, ChatActivity.this);
                            online.setVisibility(View.VISIBLE);
                        } else {
                            Glide.with(ChatActivity.this).load(R.drawable.temp)
                                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(ChatActivity.this, 70)))
                                    .into(userimage);
                            online.setVisibility(View.GONE);
                        }
                        if (results.blockedbyme.equals("block")) {
                            online.setVisibility(View.GONE);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onUserImageChange(final String user_id, final String user_image) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (user_id.equals(userId)) {
                    ContactsData.Result results = dbhelper.getContactDetail(userId);
                    if (!results.blockedme.equals("block") && !results.blockedbyme.equals("block")) {
                        DialogActivity.setProfileImage(dbhelper.getContactDetail(results.user_id), userimage, ChatActivity.this);
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
                int currentCount = dbhelper.getMessagesCount(GetSet.getUserId() + userId);
                if (totalMsg != currentCount) {
                    messagesList.clear();
                    if (endlessRecyclerOnScrollListener != null) {
                        endlessRecyclerOnScrollListener.resetState();
                    }
                    try {
                        messagesList.addAll(getMessagesAry(dbhelper.getMessages(GetSet.getUserId() + userId, "0", "20"), null));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                    try {
                        messagesList.addAll(getMessagesAry(dbhelper.getMessages(GetSet.getUserId() + userId, "0", "20"), null));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
    public void onUploadListen(final String message_id, final String attachment, final String progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < messagesList.size(); i++) {
                    if (message_id.equals(messagesList.get(i).message_id)) {
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
    public void onPrivacyChanged(final JSONObject jsonObject) {
//        Log.i(TAG, "onPrivacyChanged: " + jsonObject);
        try {
            if (jsonObject.getString(TAG_USER_ID).equalsIgnoreCase(userId)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DialogActivity.setProfileImage(dbhelper.getContactDetail(jsonObject.getString(TAG_USER_ID)), userimage, ChatActivity.this);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

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
        if (!results.blockedme.equals("block") && !results.blockedbyme.equals("block")) {
            if (runnable != null)
                handler.removeCallbacks(runnable);
            if (!meTyping) {
                meTyping = true;
            }
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                jsonObject.put(Constants.TAG_RECEIVER_ID, userId);
                jsonObject.put(Constants.TAG_CHAT_ID, userId + GetSet.getUserId());
                jsonObject.put("type", "typing");
                socketConnection.typing(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (!results.blockedme.equals("block") && !results.blockedbyme.equals("block")) {
            runnable = new Runnable() {
                public void run() {
                    meTyping = false;
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                        jsonObject.put(Constants.TAG_RECEIVER_ID, userId);
                        jsonObject.put(Constants.TAG_CHAT_ID, userId + GetSet.getUserId());
                        jsonObject.put("type", "untyping");
                        socketConnection.typing(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };
            handler.postDelayed(runnable, 1000);
        }
    }

    @Override
    public void deletetype(String type) {
        String chatId = GetSet.getUserId() + userId;
        for (int i = 0; i < selectedChatPos.size(); i++) {
            MessagesData mData = selectedChatPos.get(i);
            if (type.equals("me")) {
                dbhelper.deleteMessageFromId(mData.message_id);
                messageListAdapter.notifyItemRemoved(messagesList.indexOf(mData));
                messagesList.remove(mData);
            } else {
                String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);

                try {
                    if (!results.blockedme.equals("block")) {
                        JSONObject jobj = new JSONObject();
                        JSONObject message = new JSONObject();
                        message.put(Constants.TAG_USER_ID, GetSet.getUserId());
                        message.put(Constants.TAG_USER_NAME, GetSet.getUserName());
                        message.put(Constants.TAG_MESSAGE_TYPE, Constants.TAG_ISDELETE);
                        message.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(getString(R.string.message_deleted)));
                        message.put(Constants.TAG_ATTACHMENT, mData.attachment);
                        message.put(Constants.TAG_THUMBNAIL, ApplicationClass.encryptMessage(mData.thumbnail));
                        message.put(Constants.TAG_CHAT_TIME, unixStamp);
                        message.put(Constants.TAG_CHAT_ID, chatId);
                        message.put(Constants.TAG_MESSAGE_ID, mData.message_id);
                        message.put(Constants.TAG_RECEIVER_ID, userId);
                        message.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                        message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_SINGLE);
                        message.put(Constants.TAG_ISDELETE, "1");
                        jobj.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                        jobj.put(Constants.TAG_RECEIVER_ID, userId);
                        jobj.put("message_data", message);
                        Log.v("startchat", "startchat=" + jobj);
                        socketConnection.startChat(jobj);
                        mData.message_type = Constants.TAG_ISDELETE;
                        mData.isDelete = "1";
                        messageListAdapter.notifyItemChanged(messagesList.indexOf(mData));
                        dbhelper.updateMessageData(mData.message_id, Constants.TAG_MESSAGE_TYPE, Constants.TAG_ISDELETE);
                        dbhelper.updateMessageData(mData.message_id, Constants.TAG_ATTACHMENT, ApplicationClass.encryptMessage(""));
                        dbhelper.updateMessageData(mData.message_id, Constants.TAG_THUMBNAIL, ApplicationClass.encryptMessage(""));
                        whileViewChat();
                    }

                    //dbhelper.updateMessageData(mData.message_id, Constants.TAG_IS_DELETE, "1");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (type.equals("me")) {
            if (messagesList.isEmpty()) {
                dbhelper.deleteRecentChat(chatId);
            } else {
                MessagesData data = messagesList.get(0);
                dbhelper.addRecentMessages(chatId, userId, data.message_id, data.chat_time, "0");
            }
        }
        showEncryptionText();
        selectedChatPos.clear();
        chatUserLay.setVisibility(View.VISIBLE);
        forwordLay.setVisibility(View.GONE);
        chatLongPressed = false;
    }

    public RecyclerItemClickListener chatItemClick(Context mContext, final RecyclerView recyclerView) {
        return new RecyclerItemClickListener(mContext, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (chatLongPressed) {

                    if (playingPosition == position) {
                        stopMedia();
                    }
                    int chatType = recyclerView.getAdapter().getItemViewType(position);
                    if (chatType != VIEW_TYPE_DATE /*&& !messagesList.get(position).message_type.equals(Constants.TAG_ISDELETE)*/) {
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

                    for (MessagesData messagesData : selectedChatPos) {
                        if (!isForwardable(messagesData)) {
                            forwordBtn.setVisibility(View.GONE);
                            break;
                        } else {
                            forwordBtn.setVisibility(View.VISIBLE);
                        }
                    }


                    Log.d(TAG, "onItemClick: " + selectedChatPos);

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

            @Override
            public void onItemLongClick(View view, int position) {
                if (!chatLongPressed && !isRecording) {
                    if (playingPosition == position) {
                        stopMedia();
                    }
                    if (recyclerView.getAdapter().getItemViewType(position) != VIEW_TYPE_DATE /*&&
                         !messagesList.get(position).message_type.equals(Constants.TAG_ISDELETE)*/) {
                        chatLongPressed = true;
                        selectedChatPos.add(messagesList.get(position));
                        chatUserLay.setVisibility(View.GONE);
                        forwordLay.setVisibility(View.VISIBLE);

                        if (isForwardable(messagesList.get(position))) {
                            forwordBtn.setVisibility(View.VISIBLE);
                        } else {
                            forwordBtn.setVisibility(View.GONE);
                        }
                        if (messagesList.get(position).message_type.equals("text")) {
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

    private boolean isForwardable(MessagesData mData) {
        switch (mData.message_type) {
            case Constants.TAG_ISDELETE:
                return false;
            case "video":
            case "document":
            case "audio":
                if (mData.user_id.equals(GetSet.getUserId()) && !mData.progress.equals("completed")) {
                    return false;
                } else
                    return mData.user_id.equals(GetSet.getUserId()) || storageManager.checkifFileExists(mData.attachment, mData.message_type, "f");
            case "image":
                if (mData.user_id.equals(GetSet.getUserId()) && !mData.progress.equals("completed")) {
                    return false;
                } else
                    return mData.user_id.equals(GetSet.getUserId()) || storageManager.checkifImageExists("receive", mData.attachment);
            default:
                return true;
        }
    }

    private void emitImage(MessagesData mdata, String imageUrl) {
        try {
            JSONObject jobj = new JSONObject();
            JSONObject message = new JSONObject();
            message.put(Constants.TAG_USER_ID, GetSet.getUserId());
            message.put(Constants.TAG_USER_NAME, GetSet.getUserName());
            message.put(Constants.TAG_MESSAGE_TYPE, mdata.message_type);
            message.put(Constants.TAG_ATTACHMENT, imageUrl);
            message.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(mdata.message));
            message.put(Constants.TAG_CHAT_TIME, mdata.chat_time);
            message.put(Constants.TAG_CHAT_ID, mdata.chat_id);
            message.put(Constants.TAG_MESSAGE_ID, mdata.message_id);
            message.put(Constants.TAG_RECEIVER_ID, userId);
            message.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
            message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_SINGLE);
            message.put(Constants.TAG_ISDELETE, "0");
            jobj.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
            jobj.put(Constants.TAG_RECEIVER_ID, userId);
            jobj.put("message_data", message);
            Log.v("startchat", "startchat=" + jobj);
            socketConnection.startChat(jobj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private MessagesData updateDBList(String type, String imagePath, String filePath) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String chatId = GetSet.getUserId() + userId;
        RandomString randomString = new RandomString(10);
        String messageId = GetSet.getUserId() + randomString.nextString();

        String msg = "";
        if (type.equals("image")) {
            msg = getString(R.string.image);
        } else if (type.equals("audio")) {
            if (playingPosition != -1) {
                playingPosition = playingPosition + 1;
            }
            msg = getFileName(filePath);
        } else if (type.equals("video")) {
            msg = getString(R.string.video);
        } else if (type.equals("document")) {
            msg = getFileName(filePath);
        }

        MessagesData data = new MessagesData();
        data.user_id = GetSet.getUserId();
        data.message_type = type;
        data.message = msg;
        data.message_id = messageId;
        data.chat_time = unixStamp;
        data.isDelete = "0";
        data.delivery_status = "";
        data.progress = "";
        data.receiver_id = userId;

        switch (type) {
            case "video":
                data.thumbnail = imagePath;
                data.attachment = filePath;
                dbhelper.addMessageDatas(chatId, messageId, GetSet.getUserId(), GetSet.getUserName(),
                        type, ApplicationClass.encryptMessage(msg), ApplicationClass.encryptMessage(filePath),
                        ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""),
                        ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), unixStamp, userId, GetSet.getUserId(),
                        "", ApplicationClass.encryptMessage(imagePath), "");
                break;
            case "image":
                data.thumbnail = "";
                data.attachment = imagePath;
                dbhelper.addMessageDatas(chatId, messageId, GetSet.getUserId(), GetSet.getUserName(),
                        type, ApplicationClass.encryptMessage(msg), imagePath, ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""),
                        ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), unixStamp,
                        userId, GetSet.getUserId(), "", ApplicationClass.encryptMessage(imagePath), "");
                break;
            default:
                data.thumbnail = "";
                data.attachment = filePath;
                dbhelper.addMessageDatas(chatId, messageId, GetSet.getUserId(), GetSet.getUserName(),
                        type, ApplicationClass.encryptMessage(msg), filePath, ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""),
                        ApplicationClass.encryptMessage(""), unixStamp, userId, GetSet.getUserId(), "", ApplicationClass.encryptMessage(""), "");
                break;
        }
        dbhelper.addRecentMessages(chatId, userId, messageId, unixStamp, "0");

        messagesList.add(0, data);
        showEncryptionText();
        messageListAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);

        return data;
    }

    private void emitLocation(String type, String lat, String lon) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String chatId = GetSet.getUserId() + userId;
        RandomString randomString = new RandomString(10);
        String messageId = GetSet.getUserId() + randomString.nextString();
        try {
            if (!results.blockedme.equals("block")) {
                JSONObject jobj = new JSONObject();
                JSONObject message = new JSONObject();
                message.put(Constants.TAG_USER_ID, GetSet.getUserId());
                message.put(Constants.TAG_USER_NAME, GetSet.getUserName());
                message.put(Constants.TAG_MESSAGE_TYPE, type);
                message.put(Constants.TAG_MESSAGE, "Location");
                message.put(Constants.TAG_CHAT_TIME, unixStamp);
                message.put(Constants.TAG_CHAT_ID, chatId);
                message.put(Constants.TAG_LAT, ApplicationClass.encryptMessage(lat));
                message.put(Constants.TAG_LON, ApplicationClass.encryptMessage(lon));
                message.put(Constants.TAG_MESSAGE_ID, messageId);
                message.put(Constants.TAG_RECEIVER_ID, userId);
                message.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_SINGLE);
                jobj.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                jobj.put(Constants.TAG_RECEIVER_ID, userId);
                jobj.put("message_data", message);
                Log.v("startchat", "startchat=" + jobj);
                socketConnection.startChat(jobj);
            }

            dbhelper.addMessageDatas(chatId, messageId, GetSet.getUserId(), GetSet.getUserName(),
                    type, ApplicationClass.encryptMessage("Location"), "", ApplicationClass.encryptMessage(lat), ApplicationClass.encryptMessage(lon),
                    ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), unixStamp, userId,
                    GetSet.getUserId(), "", ApplicationClass.encryptMessage(""), "");

            dbhelper.addRecentMessages(chatId, userId, messageId, unixStamp, "0");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MessagesData data = new MessagesData();
        data.user_id = GetSet.getUserId();
        data.message_type = type;
        data.message = "Location";
        data.lat = lat;
        data.lon = lon;
        data.message_id = messageId;
        data.chat_time = unixStamp;
        data.isDelete = "0";
        data.delivery_status = "";
        messagesList.add(0, data);
        showEncryptionText();
        messageListAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);
    }

    private void emitContact(String type, String name, String phone, String countrycode) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String chatId = GetSet.getUserId() + userId;
        RandomString randomString = new RandomString(10);
        String messageId = GetSet.getUserId() + randomString.nextString();
        try {
            if (!results.blockedme.equals("block")) {
                JSONObject jobj = new JSONObject();
                JSONObject message = new JSONObject();
                message.put(Constants.TAG_USER_ID, GetSet.getUserId());
                message.put(Constants.TAG_USER_NAME, GetSet.getUserName());
                message.put(Constants.TAG_MESSAGE_TYPE, type);
                message.put(Constants.TAG_MESSAGE, "Contact");
                message.put(Constants.TAG_CHAT_TIME, unixStamp);
                message.put(Constants.TAG_CHAT_ID, chatId);
                message.put(Constants.TAG_CONTACT_NAME, ApplicationClass.encryptMessage(name));
                message.put(Constants.TAG_CONTACT_PHONE_NO, ApplicationClass.encryptMessage(phone));
                message.put(Constants.TAG_CONTACT_COUNTRY_CODE, ApplicationClass.encryptMessage(countrycode));
                message.put(Constants.TAG_MESSAGE_ID, messageId);
                message.put(Constants.TAG_RECEIVER_ID, userId);
                message.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_SINGLE);
                jobj.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                jobj.put(Constants.TAG_RECEIVER_ID, userId);
                jobj.put("message_data", message);
                Log.v("startchat", "startchat=" + jobj);
                socketConnection.startChat(jobj);
            }

            dbhelper.addMessageDatas(chatId, messageId, GetSet.getUserId(), GetSet.getUserName(),
                    type, ApplicationClass.encryptMessage("Contact"), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(name),
                    ApplicationClass.encryptMessage(phone), ApplicationClass.encryptMessage(countrycode), unixStamp,
                    userId, GetSet.getUserId(), "", ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""));

            dbhelper.addRecentMessages(chatId, userId, messageId, unixStamp, "0");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MessagesData data = new MessagesData();
        data.user_id = GetSet.getUserId();
        data.message_type = type;
        data.message = "Contact";
        data.contact_name = name;
        data.contact_phone_no = phone;
        data.contact_country_code = countrycode;
        data.message_id = messageId;
        data.chat_time = unixStamp;
        data.isDelete = "0";
        data.delivery_status = "";
        messagesList.add(0, data);
        showEncryptionText();
        messageListAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);
    }

    private void openDeleteDialog() {
        boolean canEveryOneVisible = true;
        Dialog deleteDialog = new Dialog(ChatActivity.this);
        deleteDialog.setCancelable(true);
        if (deleteDialog.getWindow() != null) {
            deleteDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        deleteDialog.setContentView(R.layout.dialog_report);

        RecyclerView deleteRecyclerView = deleteDialog.findViewById(R.id.reportRecyclerView);
        TextView title = deleteDialog.findViewById(R.id.title);

        title.setText(getString(R.string.really_delete_msg));

        List<String> deleteTexts = new ArrayList<>();

        for (MessagesData message : selectedChatPos) {
            if (ApplicationClass.isExceedsOneHour(message.chat_time) || !message.user_id.equalsIgnoreCase(GetSet.getUserId())
                    || message.message_type.equalsIgnoreCase(Constants.TAG_ISDELETE)) {
                canEveryOneVisible = false;
                break;
            }
        }

        deleteTexts.add(getString(R.string.delete_for_me));
        deleteTexts.add(getString(R.string.cancel));

        if (canEveryOneVisible) {
            deleteTexts.add(getString(R.string.delete_for_everyone));
            LinearLayoutManager layoutManager = new LinearLayoutManager(ChatActivity.this, RecyclerView.VERTICAL, false);
            deleteRecyclerView.setLayoutManager(layoutManager);
        } else {
            GridLayoutManager layoutManager = new GridLayoutManager(ChatActivity.this, 2);
            deleteRecyclerView.setLayoutManager(layoutManager);
        }
        DeleteAdapter adapter = new DeleteAdapter(deleteTexts, deleteDialog, ChatActivity.this);
        deleteRecyclerView.setAdapter(adapter);

        deleteDialog.show();
    }

    private void deleteChatConfirmDialog() {
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
        title.setText(R.string.really_delete_chat_history);
        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEncryptionText();
                playingPosition = -1;
                dialog.dismiss();
                dbhelper.deleteAllChats(GetSet.getUserId() + userId);
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

    private void deleteMessageConfirmDialog(MessagesData mData) {
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
                dbhelper.deleteMessageFromId(mData.message_id);
                messagesList.remove(mData);
                Toast.makeText(ChatActivity.this, getString(R.string.message_deleted), Toast.LENGTH_SHORT).show();
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

    private void blockChatConfirmDialog(final String type, String from) {
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

        if (from.equals("popup")) {
            yes.setText(getString(R.string.im_sure));
            no.setText(getString(R.string.nope));
            if (type.equals(Constants.TAG_BLOCK)) {
                title.setText(R.string.really_block_chat);
            } else {
                title.setText(R.string.really_unblock_chat);
            }
        } else {
            yes.setText(getString(R.string.unblock));
            no.setText(getString(R.string.cancel));
            title.setText(R.string.unblock_message);
        }

        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                    jsonObject.put(Constants.TAG_RECEIVER_ID, userId);
                    jsonObject.put(Constants.TAG_TYPE, type);
                    Log.v("block", "block=" + jsonObject);
                    socketConnection.block(jsonObject);
                    dbhelper.updateBlockStatus(userId, Constants.TAG_BLOCKED_BYME, type);
                    results = dbhelper.getContactDetail(userId);
                    online.setVisibility(View.GONE);
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

    public String getFormattedDateTime(Context context, long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance(TimeZone.getDefault());
        smsTime.setTimeInMillis(smsTimeInMilis * 1000L);

        Calendar now = Calendar.getInstance();

        final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "EEEE, MMMM d, h:mm aa";
        final long HOURS = 60 * 60 * 60;
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            return getString(R.string.today) + " " + DateFormat.format(timeFormatString, smsTime);
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            return getString(R.string.yesterday) + DateFormat.format(timeFormatString, smsTime);
        } else if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        } else {
            return DateFormat.format("MMMM dd yyyy, h:mm aa", smsTime).toString();
        }
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

    private void uploadImage(byte[] imageBytes, final String imagePath, final MessagesData mdata, final String filePath) {
        RequestBody requestFile = RequestBody.create(imageBytes, MediaType.parse("openImage/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("attachment", "openImage.jpg", requestFile);

        RequestBody userid = RequestBody.create(GetSet.getUserId(), MediaType.parse("multipart/form-data"));
        Log.d(TAG, "uploadImage: " + body + "\n" + userid);
        Call<HashMap<String, String>> call3 = apiInterface.upmychat(GetSet.getToken(), body, userid);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> data = response.body();
                Log.v(TAG, "uploadImageResponse: " + data);
                if (data.get(Constants.TAG_STATUS).equals("true")) {
                    File dir = new File(imagePath);
                    if (dir.exists()) {
                        if (mdata.message_type.equals("image")) {
                            dbhelper.updateMessageData(mdata.message_id, Constants.TAG_PROGRESS, "completed");
                            if (messageListAdapter != null) {
                                for (int i = 0; i < messagesList.size(); i++) {
                                    if (mdata.message_id.equals(messagesList.get(i).message_id)) {
                                        messagesList.get(i).progress = "completed";
                                        messageListAdapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                            }

                            if (!results.blockedme.equals("block")) {
                                emitImage(mdata, data.get(Constants.TAG_USER_IMAGE));
                                // Toast.makeText(ChatActivity.this, data.get(Constants.TAG_MESSAGE), Toast.LENGTH_SHORT).show();
                            }
                        } else if (mdata.message_type.equals("video")) {
                            if (messageListAdapter != null) {
                                for (int i = 0; i < messagesList.size(); i++) {
                                    if (mdata.message_id.equals(messagesList.get(i).message_id)) {
                                        messagesList.get(i).thumbnail = mdata.thumbnail;
                                        messageListAdapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                            }

                            Intent service = new Intent(ChatActivity.this, FileUploadService.class);
                            Bundle b = new Bundle();
                            b.putSerializable("mdata", mdata);
                            b.putSerializable(Constants.TAG_THUMBNAIL, data.get(Constants.TAG_USER_IMAGE));
                            b.putString("filepath", filePath);
                            b.putString("chatType", "chat");
                            service.putExtras(b);
                            startService(service);
                        }
                    }
                } else {
                    dbhelper.updateMessageData(mdata.message_id, Constants.TAG_PROGRESS, "error");
                    if (messageListAdapter != null) {
                        for (int i = 0; i < messagesList.size(); i++) {
                            if (mdata.message_id.equals(messagesList.get(i).message_id)) {
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
                Log.v(TAG, "onFailure=" + "onFailure");
                call.cancel();
                dbhelper.updateMessageData(mdata.message_id, Constants.TAG_PROGRESS, "error");
                if (messageListAdapter != null) {
                    for (int i = 0; i < messagesList.size(); i++) {
                        if (mdata.message_id.equals(messagesList.get(i).message_id)) {
                            messagesList.get(i).progress = "error";
                            messageListAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }
        });
    }

    private String getFileName(String url) {
        String imgSplit = url;
        Log.i(TAG, "getFileName: " + imgSplit);
        int endIndex = imgSplit.lastIndexOf("/");
        if (endIndex != -1) {
            imgSplit = imgSplit.substring(endIndex + 1);
        }
        return imgSplit;
    }

    public String firstThree(String str) {
        return str.length() < 3 ? str : str.substring(0, 3);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            int permissionCamera = ContextCompat.checkSelfPermission(ChatActivity.this,
                    CAMERA);
            int permissionAudio = ContextCompat.checkSelfPermission(ChatActivity.this,
                    RECORD_AUDIO);
            int permissionWakeLock = ContextCompat.checkSelfPermission(ChatActivity.this,
                    WAKE_LOCK);

            if (permissionCamera == PackageManager.PERMISSION_GRANTED &&
                    permissionAudio == PackageManager.PERMISSION_GRANTED &&
                    permissionWakeLock == PackageManager.PERMISSION_GRANTED) {
              /*  Intent video = new Intent(ChatActivity.this, CallActivity.class);
                video.putExtra("from", "send");
                video.putExtra("type", "audio");
                video.putExtra("data", data);
                startActivity(video);*/
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(CAMERA) &&
                            shouldShowRequestPermissionRationale(WAKE_LOCK) &&
                            shouldShowRequestPermissionRationale(RECORD_AUDIO)) {
                        requestPermission(new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 100);
                    } else {
//                        openPermissionDialog("Camera, Record Audio");
                        makeToast(getString(R.string.call_permission_error));
                    }
                }
            }
        } else if (requestCode == 101) {
            int permissionCamera = ContextCompat.checkSelfPermission(ChatActivity.this,
                    CAMERA);
            int permissionAudio = ContextCompat.checkSelfPermission(ChatActivity.this,
                    RECORD_AUDIO);
            int permissionWakeLock = ContextCompat.checkSelfPermission(ChatActivity.this,
                    WAKE_LOCK);

            if (permissionCamera == PackageManager.PERMISSION_GRANTED &&
                    permissionAudio == PackageManager.PERMISSION_GRANTED &&
                    permissionWakeLock == PackageManager.PERMISSION_GRANTED) {
               /* Intent video = new Intent(ChatActivity.this, CallActivity.class);
                video.putExtra("from", "send");
                video.putExtra("type", "video");
                video.putExtra("data", data);
                startActivity(video);*/
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(CAMERA) &&
                            shouldShowRequestPermissionRationale(WAKE_LOCK) &&
                            shouldShowRequestPermissionRationale(RECORD_AUDIO)) {
                        requestPermission(new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 101);
                    } else {
//                        openPermissionDialog("Camera, Record Audio,Phone State");
                        makeToast(getString(R.string.call_permission_error));
                    }
                }
            }
        } else if (requestCode == 102) {
            int permissionStorage = ContextCompat.checkSelfPermission(ChatActivity.this, WRITE_EXTERNAL_STORAGE);

            if (permissionStorage == PackageManager.PERMISSION_GRANTED) {
                ImagePicker.pickImage(this, getString(R.string.select_your_image));
            } else {
                makeToast(getString(R.string.storage_permission_error));
            }
        } else if (requestCode == 106) {
            int permissionCamera = ContextCompat.checkSelfPermission(ChatActivity.this,
                    CAMERA);
            int permissionStorage = ContextCompat.checkSelfPermission(ChatActivity.this,
                    WRITE_EXTERNAL_STORAGE);

            if (permissionCamera == PackageManager.PERMISSION_GRANTED &&
                    permissionStorage == PackageManager.PERMISSION_GRANTED) {
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
                } else {
                    ApplicationClass.onShareExternal = true;
                    ImagePicker.pickImageCameraOnly(this, 234);
                }
            } else {
                makeToast(getString(R.string.storage_permission_error));
            }
        } else if (requestCode == 107) {
            int permissionCamera = ContextCompat.checkSelfPermission(ChatActivity.this,
                    CAMERA);
            int permissionStorage = ContextCompat.checkSelfPermission(ChatActivity.this,
                    WRITE_EXTERNAL_STORAGE);

            if (permissionCamera == PackageManager.PERMISSION_GRANTED &&
                    permissionStorage == PackageManager.PERMISSION_GRANTED) {
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
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
            } else {
                makeToast(getString(R.string.storage_permission_error));
            }
        } else if (requestCode == 108) {
            int permissionStorage = ContextCompat.checkSelfPermission(ChatActivity.this,
                    WRITE_EXTERNAL_STORAGE);

            if (permissionStorage == PackageManager.PERMISSION_GRANTED) {
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
                } else {
                    FilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .enableDocSupport(true)
                            .setActivityTitle("Please select document")
                            .showTabLayout(true)
                            .setActivityTheme(R.style.MainTheme)
                            .pickFile(this, 151);
                }
            } else {
                makeToast(getString(R.string.storage_permission_error));
            }
        } else if (requestCode == 109) {
            int permissionStorage = ContextCompat.checkSelfPermission(ChatActivity.this,
                    WRITE_EXTERNAL_STORAGE);

            if (permissionStorage == PackageManager.PERMISSION_GRANTED) {
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
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
            } else {
                makeToast(getString(R.string.storage_permission_error));
            }
        } else if (requestCode == 110) {
            int permissionContacts = ContextCompat.checkSelfPermission(ChatActivity.this,
                    READ_CONTACTS);

            if (permissionContacts == PackageManager.PERMISSION_GRANTED) {
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
                } else {
                    ApplicationClass.onShareExternal = true;
                    Intent intentc = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                    startActivityForResult(intentc, 13);
                }
            }
        } else if (requestCode == 111) {

            int permissionAudio = ContextCompat.checkSelfPermission(ChatActivity.this,
                    RECORD_AUDIO);
            int permissionStorage = ContextCompat.checkSelfPermission(ChatActivity.this,
                    WRITE_EXTERNAL_STORAGE);

            setVoiceRecorder();
        }
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
                String filePath = storageManager.saveBitmapToExtFilesDir(bitmap, timestamp + ".jpg");
                Log.i(TAG, "onActivityResult: " + filePath);
                if (filePath != null) {
                    ImageCompression imageCompression = new ImageCompression(ChatActivity.this) {
                        @Override
                        protected void onPostExecute(String imagePath) {
                            try {
                                MessagesData mdata = updateDBList(Constants.TAG_IMAGE, imagePath, "");
                                byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(imagePath));
                                uploadImage(bytes, imagePath, mdata, "");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    };
                    imageCompression.execute(filePath);
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
                    Log.v(TAG, "File");
                    String filepath = pathsAry.get(0);
                    Log.i(TAG, "selectedFile: " + filepath);
                    if (ApplicationClass.isVideoFile(filepath)) {
                        try {
                            Log.v(TAG, "videoPath: " + filepath);
                            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(filepath, MediaStore.Video.Thumbnails.MINI_KIND);
                            if (thumb != null) {
                                String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
                                String imageStatus = storageManager.saveToSdCard(thumb, "sent", timestamp + ".jpg");
                                if (imageStatus.equals("success")) {
                                    File file = storageManager.getImage("sent", timestamp + ".jpg");
                                    String imagePath = file.getAbsolutePath();
                                    MessagesData mdata = updateDBList("video", imagePath, filepath);
                                    byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(imagePath));
                                    uploadImage(bytes, imagePath, mdata, filepath);
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            Toast.makeText(ChatActivity.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        ImageCompression imageCompression = new ImageCompression(ChatActivity.this) {
                            @Override
                            protected void onPostExecute(String imagePath) {
                                try {
                                    Log.v(TAG, "ImageCompression: " + imagePath);
                                    MessagesData mdata = updateDBList("image", imagePath, "");
                                    byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(imagePath));
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
                        MessagesData mdata = updateDBList("document", "", filepath);
                        Intent service = new Intent(ChatActivity.this, FileUploadService.class);
                        Bundle b = new Bundle();
                        b.putSerializable("mdata", mdata);
                        b.putString("filepath", filepath);
                        b.putString("chatType", "chat");
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
                        MessagesData mdata = updateDBList("audio", "", filepath);
                        Intent service = new Intent(ChatActivity.this, FileUploadService.class);
                        Bundle b = new Bundle();
                        b.putSerializable("mdata", mdata);
                        b.putString("filepath", filepath);
                        b.putString("chatType", "chat");
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
            username.setText(results.user_name);
        } else if (resultCode == RESULT_OK && requestCode == 222) {
            selectedChatPos.clear();
            messageListAdapter.notifyDataSetChanged();
            chatUserLay.setVisibility(View.VISIBLE);
            forwordLay.setVisibility(View.GONE);
            chatLongPressed = false;
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
            System.out.println("fjgbj bounded" + bindService(intent, myConnection, Context.BIND_AUTO_CREATE));
        }
    }

int bottomBarHeight =0;
    void showBottomSheet() {
        bottomLayout.setVisibility(View.VISIBLE);
        ((View)recyclerView.getParent()).setPadding(Screen.dp(0),Screen.dp(0),Screen.dp(0),Screen.dp(200) - Screen.dp(72));
        recyclerView.scrollToPosition(0);
       // ((ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams()).bottomMargin = Screen.dp(200) ;
        myService.counterFab.setVisibility(View.GONE);
        voiceFab.performClick();

    }
    FloatingWidgetService myService;
    boolean isBound = false;
    public boolean isSppechEnable = false;
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
        Speech.init(this, getPackageName());
        initTextToSpeech();
        ApplicationClass.onShareExternal = false;
        userId = getIntent().getExtras().getString("user_id");
        tempUserId = userId;
        results = dbhelper.getContactDetail(userId);
        username.setText(results.user_name);
        if (!results.blockedme.equals("block")) {
            DialogActivity.setProfileImage(dbhelper.getContactDetail(userId), userimage, ChatActivity.this);
            online.setVisibility(View.VISIBLE);
        } else {
            Glide.with(ChatActivity.this).load(R.drawable.temp)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                    .into(userimage);
            online.setVisibility(View.GONE);
        }
        if (results.blockedbyme.equals("block")) {
            online.setVisibility(View.GONE);
        }
        if (!SingleStoryActivity.isMessageSent.equals("")) {
            messagesList.add(0, dbhelper.getSingleMessage(SingleStoryActivity.isMessageSent));
            showEncryptionText();
            messageListAdapter.notifyItemInserted(0);
            recyclerView.smoothScrollToPosition(0);
            SingleStoryActivity.isMessageSent = "";
        }
new Handler().postDelayed(new Runnable() {
    @Override
    public void run() {
        voiceFab.setRippleColor(color.toConversationColor(ChatActivity.this));
        voiceFab.setBackgroundTintList(new ColorStateList(new int[][]{
                new int[]{android.R.attr.state_pressed},
                new int[]{color.toConversationPColor(ChatActivity.this)},


        }, new int[]{
                color.toConversationPColor(ChatActivity.this),
                color.toConversationColor(ChatActivity.this),
        }));
    }
},600);

    }

    @Override
    public void onPause() {
        tempUserId = "";
        editText.setError(null);
        stopMedia();
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
    protected void onDestroy() {
        if (Constants.isChatOpened) {
            Constants.isChatOpened = false;
        }
        SocketConnection.getInstance(this).setChatCallbackListener(null);
        if (onlineTimer != null) {
            onlineTimer.cancel();
        }
        stopMedia();
        super.onDestroy();
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
        ApplicationClass.resumeExternalAudio(this);
    }

    private long mediaDuration(int position, String type) {
        MediaPlayer player;
        File file = storageManager.getFile(messagesList.get(position).attachment,
                messagesList.get(position).message_type, type);
        Uri voiceURI = FileProvider.getUriForFile(getApplicationContext(),
                BuildConfig.APPLICATION_ID + ".provider", file);

        player = MediaPlayer.create(getApplicationContext(), voiceURI);
        if (player != null) {
            return player.getDuration();
        } else {
            return 0;
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
            myService.counterFab.setVisibility(View.VISIBLE);
            if (Speech.getInstance().isListening())
                Speech.getInstance().stopListening();
            return;
        }
        /*if (bottomLayout.getVisibility() == View.VISIBLE) {
            disableWakeLock();
            isSppechEnable = false;
            bottomLayout.setVisibility(View.GONE);
            ((ViewGroup.MarginLayoutParams) chatFragmentCont.getLayoutParams()).bottomMargin = Screen.dp(0);
            linearLayout.setVisibility(View.GONE);
            fabLay.setVisibility(View.VISIBLE);
            myService.counterFab.setVisibility(View.VISIBLE);
            if (Speech.getInstance().isListening())
                Speech.getInstance().stopListening();
            return;
        }*/
        if (selectedChatPos.size() > 0) {
            selectedChatPos.clear();
            messageListAdapter.notifyDataSetChanged();
            chatUserLay.setVisibility(View.VISIBLE);
            forwordLay.setVisibility(View.GONE);
            chatLongPressed = false;
        } else {
            if (isFromNotification) {
                finish();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                finish();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send:
                ApplicationClass.preventMultiClick(send);
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
                } else {
                    if (editText.getText().toString().trim().length() > 0) {
                        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                        String textMsg = editText.getText().toString().trim();
                        /*String encryptedMsg = "";
                        try {
                            CryptLib cryptLib = new CryptLib();
                            encryptedMsg = cryptLib.encryptPlainTextWithRandomIV(textMsg,"123");
                        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                            Log.e(TAG, "onClick: "+e.getMessage());
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }*/
                        String chatId = GetSet.getUserId() + userId;
                        RandomString randomString = new RandomString(10);
                        String messageId = GetSet.getUserId() + randomString.nextString();
                        try {
                            if (!results.blockedme.equals("block")) {
                                JSONObject jobj = new JSONObject();
                                JSONObject message = new JSONObject();
                                message.put(Constants.TAG_USER_ID, GetSet.getUserId());
                                message.put(Constants.TAG_USER_NAME, GetSet.getUserName());
                                message.put(Constants.TAG_MESSAGE_TYPE, "text");
                                message.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(textMsg));
                                message.put(Constants.TAG_CHAT_TIME, unixStamp);
                                message.put(Constants.TAG_CHAT_ID, chatId);
                                message.put(Constants.TAG_MESSAGE_ID, messageId);
                                message.put(Constants.TAG_RECEIVER_ID, userId);
                                message.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                                message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_SINGLE);
                                jobj.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                                jobj.put(Constants.TAG_RECEIVER_ID, userId);
                                jobj.put("message_data", message);
                                Log.v("startchat", "startchat=" + jobj);
                                socketConnection.startChat(jobj);
                            }

                            dbhelper.addMessageDatas(chatId, messageId, GetSet.getUserId(), GetSet.getUserName(),
                                    "text", ApplicationClass.encryptMessage(textMsg), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""),
                                    ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""),
                                    unixStamp, userId, GetSet.getUserId(), "", ApplicationClass.encryptMessage(""), ApplicationClass.encryptMessage(""));

                            dbhelper.addRecentMessages(chatId, userId, messageId, unixStamp, "0");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        MessagesData data = new MessagesData();
                        data.user_id = GetSet.getUserId();
                        data.message_type = "text";
                        data.message = textMsg;
                        data.message_id = messageId;
                        data.chat_time = unixStamp;
                        data.isDelete = "0";
                        data.delivery_status = "";
                        messagesList.add(0, data);
                        messageListAdapter.notifyItemInserted(0);
                        recyclerView.smoothScrollToPosition(0);
                        showEncryptionText();
                        editText.setText("");
                    } else {
                        editText.setError(getString(R.string.please_enter_your_message));
                    }
                }
                break;
            case R.id.backbtn:
                onBackPressed();
                break;
            case R.id.optionbtn:
                ApplicationClass.preventMultiClick(optionbtn);
                stopMedia();
                results = dbhelper.getContactDetail(userId);
                Display display = this.getWindowManager().getDefaultDisplay();
                final ArrayList<String> values = new ArrayList<>();
                if (results.mute_notification.equals("true")) {
                    values.add(getString(R.string.unmute_notification));
                } else {
                    values.add(getString(R.string.mute_notification));
                }
                if (results.blockedbyme.equals("block")) {
                    values.add(getString(R.string.unblock));
                } else {
                    values.add(getString(R.string.block));
                }
                values.add(getString(R.string.clear_chat));
                if (results.favourited.equals("true")) {
                    values.add(getString(R.string.remove_favourite));
                } else {
                    values.add(getString(R.string.mark_favourite));
                }
                if (!ApplicationClass.isStringNotNull(results.saved_name)) {
                    values.add(getString(R.string.add_contact));
                }

                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        R.layout.option_item, android.R.id.text1, values);
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = layoutInflater.inflate(R.layout.option_layout, null);
                final PopupWindow popup = new PopupWindow(ChatActivity.this);
                popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popup.setContentView(layout);
                popup.setWidth(display.getWidth() * 50 / 100);
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
                        if (position == 0) {
                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                networkSnack();
                            } else {
                                if (values.get(position).equalsIgnoreCase(getString(R.string.mute_notification))) {
                                    dbhelper.updateMuteUser(userId, "true");
                                    values.set(position, getString(R.string.unmute_notification));
                                } else {
                                    dbhelper.updateMuteUser(userId, "");
                                    values.set(position, getString(R.string.mute_notification));
                                }
                                results = dbhelper.getContactDetail(userId);
                                adapter.notifyDataSetChanged();
                            }
                        } else if (position == 1) {
                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                networkSnack();
                            } else {
                                String type = "";
                                if (results.blockedbyme.equals("block")) {
                                    type = "unblock";
                                } else {
                                    type = "block";
                                }
                                blockChatConfirmDialog(type, "popup");
                            }
                        } else if (position == 2) {
                            deleteChatConfirmDialog();
                        } else if (position == 3) {
                            if (results.favourited.equals("true")) {
                                dbhelper.updateFavUser(userId, "false");
                                Toast.makeText(ChatActivity.this, getString(R.string.removed_favourites), Toast.LENGTH_SHORT).show();
                            } else {
                                dbhelper.updateFavUser(userId, "true");
                                Toast.makeText(ChatActivity.this, getString(R.string.marked_favourite), Toast.LENGTH_SHORT).show();
                            }
                        } else if (position == 4) {
                            Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
                            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                            intent.putExtra("finishActivityOnSaveCompleted", true);
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, results.phone_no);
                            intent.putExtra(ContactsContract.Intents.Insert.NAME, "");
                            startActivityForResult(intent, 556);
                        }
                    }
                });
                break;
            case R.id.attachbtn:
                ApplicationClass.preventMultiClick(attachbtn);
                TransitionManager.beginDelayedTransition(bottomLay);
                visible = !visible;
                attachmentsLay.setVisibility(visible ? View.VISIBLE : View.GONE);
                break;
            case R.id.userImg:
                break;
            case R.id.cameraBtn:
                ApplicationClass.preventMultiClick(cameraBtn);
                stopMedia();
                if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, CAMERA}, 106);
                } else if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
                } else {
                    ApplicationClass.onShareExternal = true;
                    ImagePicker.pickImageCameraOnly(this, 234);
                }
                break;
            case R.id.galleryBtn:
                ApplicationClass.preventMultiClick(galleryBtn);
                stopMedia();
                if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, CAMERA}, 107);
                } else if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
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
                if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 108);
                } else {
                    if (isNetworkConnected().equals(NOT_CONNECT)) {
                        networkSnack();
                    } else if (results.blockedbyme.equals("block")) {
                        blockChatConfirmDialog("unblock", "sent");
                    } else {
                        FilePickerBuilder.getInstance()
                                .setMaxCount(1)
                                .enableDocSupport(true)
                                .setActivityTitle(getString(R.string.please_select_document))
                                .showTabLayout(true)
                                .setActivityTheme(R.style.MainTheme)
                                .pickFile(this, 151);
                    }
                }
                break;
            case R.id.audioBtn:
                ApplicationClass.preventMultiClick(audioBtn);
                stopMedia();
                if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 109);
                } else {
                    if (isNetworkConnected().equals(NOT_CONNECT)) {
                        networkSnack();
                    } else if (results.blockedbyme.equals("block")) {
                        blockChatConfirmDialog("unblock", "sent");
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
                }
                break;
            case R.id.locationBtn:
                ApplicationClass.preventMultiClick(locationBtn);
                stopMedia();
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
                } else {
                    Intent location = new Intent(this, LocationActivity.class);
                    location.putExtra("from", "share");
                    startActivityForResult(location, 200);
                }
                break;
            case R.id.contactBtn:
                ApplicationClass.preventMultiClick(contactBtn);
                stopMedia();
                if (ContextCompat.checkSelfPermission(this, READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{READ_CONTACTS}, 110);
                } else {
                    if (isNetworkConnected().equals(NOT_CONNECT)) {
                        networkSnack();
                    } else if (results.blockedbyme.equals("block")) {
                        blockChatConfirmDialog("unblock", "sent");
                    } else {
                        ApplicationClass.onShareExternal = true;
                        Intent intentc = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                        startActivityForResult(intentc, 13);
                    }
                }
                break;
            case R.id.closeBtn:
                stopMedia();
                break;
            case R.id.audioCallBtn:
                ApplicationClass.preventMultiClick(audioCallBtn);
                stopMedia();
                if (ContextCompat.checkSelfPermission(ChatActivity.this, CAMERA) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(ChatActivity.this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(ChatActivity.this, WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ChatActivity.this, new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 100);
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
                } else {
                    if (NetworkReceiver.isConnected()) {
                        ApplicationClass.preventMultiClick(audioCallBtn);
                        AppRTCUtils appRTCUtils = new AppRTCUtils(this);
                        Intent video = appRTCUtils.connectToRoom(userId, Constants.TAG_SEND, Constants.TAG_AUDIO);
                        startActivity(video);
                    } else {
                        makeToast(getString(R.string.no_internet_connection));
                    }
                }
                break;
            case R.id.videoCallBtn:
                ApplicationClass.preventMultiClick(videoCallBtn);
                stopMedia();
                if (ContextCompat.checkSelfPermission(ChatActivity.this, CAMERA) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(ChatActivity.this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(ChatActivity.this, WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ChatActivity.this, new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 101);
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
                } else {
                    if (NetworkReceiver.isConnected()) {
                        ApplicationClass.preventMultiClick(videoCallBtn);
                        AppRTCUtils appRTCUtils = new AppRTCUtils(this);
                        Intent video = appRTCUtils.connectToRoom(userId, Constants.TAG_SEND, Constants.TAG_VIDEO);
                        startActivity(video);
                    } else {
                        makeToast(getString(R.string.no_internet_connection));
                    }
                }
                break;
            case R.id.chatUserLay:
                ApplicationClass.preventMultiClick(chatUserLay);
                stopMedia();
                Intent profile = new Intent(ChatActivity.this, ProfileActivity.class);
                profile.putExtra(Constants.TAG_USER_ID, userId);
                startActivity(profile);
                break;
            case R.id.forwordBtn:
                ApplicationClass.preventMultiClick(forwordBtn);
                stopMedia();
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    Intent f = new Intent(ChatActivity.this, ForwardActivity.class);
                    f.putExtra("from", "chat");
                    f.putExtra("id", userId);
                    f.putExtra("data", selectedChatPos);
                    startActivityForResult(f, 222);
                }
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

    private void requestPermission(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(ChatActivity.this, permissions, requestCode);
    }

    private void openPermissionDialog(String permissionList) {
        permissionDialog = new Dialog(ChatActivity.this);
        permissionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        permissionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        permissionDialog.setContentView(R.layout.default_popup);
        permissionDialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 85 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        permissionDialog.setCancelable(false);
        permissionDialog.setCanceledOnTouchOutside(false);

        TextView title = permissionDialog.findViewById(R.id.title);
        TextView yes = permissionDialog.findViewById(R.id.yes);
        TextView no = permissionDialog.findViewById(R.id.no);
        title.setText("This loopytime requires " + permissionList + " permissions to access the features. Please turn on");
        yes.setText(R.string.grant);
        no.setText(R.string.nope);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permissionDialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, 100);
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permissionDialog.isShowing())
                    permissionDialog.dismiss();
            }
        });
        permissionDialog.show();
    }

    private void updatePosition() {
        if (player != null) {
            if (playingPosition != -1) {
                // messageListAdapter.notifyItemMoved(playingPosition,playingPosition+1);
                playingPosition = playingPosition + 1;
            }
        }
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

    public class MessageListAdapter extends RecyclerView.Adapter {
        public static final int VIEW_TYPE_MESSAGE_SENT = 1;
        public static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
        public static final int VIEW_TYPE_IMAGE_SENT = 3;
        public static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
        public static final int VIEW_TYPE_CONTACT_SENT = 5;
        public static final int VIEW_TYPE_CONTACT_RECEIVED = 6;
        public static final int VIEW_TYPE_FILE_SENT = 7;
        public static final int VIEW_TYPE_FILE_RECEIVED = 8;
        public static final int VIEW_TYPE_DATE = 9;
        public static final int VIEW_TYPE_VOICE_SENT = 10;
        public static final int VIEW_TYPE_VOICE_RECEIVE = 11;
        public static final int VIEW_TYPE_DELETE_SENT = 12;
        public static final int VIEW_TYPE_DELETE_RECEIVE = 13;
        public static final int VIEW_TYPE_STATUS_REPLY_SENT = 14;
        public static final int VIEW_TYPE_STATUS_REPLY_RECEIVE = 15;

        private Context mContext = getApplicationContext();
        private List<MessagesData> mMessageList;

        public MessageListAdapter(Context context, List<MessagesData> messageList) {
            //mContext = context;
            mMessageList = messageList;
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }

        // Determines the appropriate ViewType according to the sender of the message.
        @Override
        public int getItemViewType(int position) {
            MessagesData message = mMessageList.get(position);
            if (message.user_id != null && message.user_id.equals(GetSet.getUserId())) {
                switch (message.message_type) {
                    case "text":
                        return VIEW_TYPE_MESSAGE_SENT;
                    case "image":
                    case "video":
                    case "location":
                        return VIEW_TYPE_IMAGE_SENT;
                    case "contact":
                        return VIEW_TYPE_CONTACT_SENT;
                    case "date":
                        return VIEW_TYPE_DATE;
                    case "audio":
                        return VIEW_TYPE_VOICE_SENT;
                    case "story":
                        return VIEW_TYPE_STATUS_REPLY_SENT;
                    case Constants.TAG_ISDELETE:
                        return VIEW_TYPE_DELETE_SENT;
                    default:
                        return VIEW_TYPE_FILE_SENT;
                }
            } else {
                switch (message.message_type) {
                    case "text":
                        return VIEW_TYPE_MESSAGE_RECEIVED;
                    case "image":
                    case "video":
                    case "location":
                        return VIEW_TYPE_IMAGE_RECEIVED;
                    case "contact":
                        return VIEW_TYPE_CONTACT_RECEIVED;
                    case "date":
                        return VIEW_TYPE_DATE;
                    case "audio":
                        return VIEW_TYPE_VOICE_RECEIVE;
                    case "story":
                        return VIEW_TYPE_STATUS_REPLY_RECEIVE;
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

            if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_text_bubble_sent, parent, false);
                return new SentMessageHolder(view);
            } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_text_bubble_receive, parent, false);
                return new ReceivedMessageHolder(view);
            } else if (viewType == VIEW_TYPE_IMAGE_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_image_bubble_sent, parent, false);
                return new SentImageHolder(view);
            } else if (viewType == VIEW_TYPE_IMAGE_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_image_bubble_receive, parent, false);
                return new ReceivedImageHolder(view);
            } else if (viewType == VIEW_TYPE_CONTACT_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_contact_bubble_sent, parent, false);
                return new SentContactHolder(view);
            } else if (viewType == VIEW_TYPE_CONTACT_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_contact_bubble_receive, parent, false);
                return new ReceivedContactHolder(view);
            } else if (viewType == VIEW_TYPE_FILE_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_file_bubble_sent, parent, false);
                return new SentFileHolder(view);
            } else if (viewType == VIEW_TYPE_FILE_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_file_bubble_received, parent, false);
                return new ReceivedFileHolder(view);
            } else if (viewType == VIEW_TYPE_DATE) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_date_layout, parent, false);
                return new DateHolder(view);
            } else if (viewType == VIEW_TYPE_VOICE_SENT) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_voice_sent, parent, false);
                return new SentVoiceHolder(view);
            } else if (viewType == VIEW_TYPE_VOICE_RECEIVE) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_voice_receive, parent, false);
                return new ReceiveVoiceHolder(view);
            } else if (viewType == VIEW_TYPE_DELETE_SENT) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_delete_bubble_sent, parent, false);
                return new DeleteMsgSent(view);
            } else if (viewType == VIEW_TYPE_DELETE_RECEIVE) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_delete_bubble_receive, parent, false);
                return new DeleteMsgReceived(view);
            } else if (viewType == VIEW_TYPE_STATUS_REPLY_SENT) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sent_status_reply, parent, false);
                return new SentStatusViewHolder(view);
            } else if (viewType == VIEW_TYPE_STATUS_REPLY_RECEIVE) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.received_status_reply, parent, false);
                return new ReceivedStatusViewHolder(view);
            }

            return null;
        }

        // Passes the message object to a ViewHolder so that the contents can be bound to UI.
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MessagesData message = mMessageList.get(position);

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
                    break;
                case VIEW_TYPE_STATUS_REPLY_SENT:
                    ((SentStatusViewHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_STATUS_REPLY_RECEIVE:
                    ((ReceivedStatusViewHolder) holder).bind(message);
                    break;
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
                        seekBar.setProgress(0);
                        seekBar = null;
                    }
                    player.pause();
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
                        mMessageList.get(position).message_type, from);
                Uri voiceURI = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", file);

                seek.setTag(playingPosition);
                seekBar = seek;
                audioTime = time;
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    player.setDataSource(getApplicationContext(), voiceURI);
                    ApplicationClass.pauseExternalAudio(context);
                    player.prepareAsync();
                    player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.start();
                        }
                    });
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
                    ApplicationClass.resumeExternalAudio(context);
                } else {
                    isAudioPlay = true;
                    ApplicationClass.pauseExternalAudio(context);
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

        private StatusDatas.Status getStatusData(String data) {
            StatusDatas.Status model = new StatusDatas().new Status();
            try {
                if (data != null && !TextUtils.isEmpty(data)) {
                    Log.i(TAG, "getStatusData: " + data);
                    JSONObject object = new JSONObject(data);
                    model.storyType = checkJSONobject(object, "story_type");
                    model.message = checkJSONobject(object, "message");
                    model.storyId = checkJSONobject(object, "story_id");
                    model.attachment = checkJSONobject(object, "attachment");
                    model.mThumbnail = checkJSONobject(object, "thumbnail");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return model;
        }

        private String checkJSONobject(JSONObject json, String key) {
            if (json.isNull(key))
                return "";
            else
                return json.optString(key, "").trim();
        }

        private class SentMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText;
            ImageView tickimage;

            SentMessageHolder(View itemView) {
                super(itemView);

                messageText = itemView.findViewById(R.id.text_message_body);
                timeText = itemView.findViewById(R.id.text_message_time);
                tickimage = itemView.findViewById(R.id.tickimage);
            }

            void bind(MessagesData message) {
                messageText.setText(message.message
                        + Html.fromHtml(" &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"));
                Linkify.addLinks(messageText, Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
                if(!userId.equalsIgnoreCase(GetSet.getUserId())) {
                switch (message.delivery_status) {
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
                }}

                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
            }
        }

        private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText;

            ReceivedMessageHolder(View itemView) {
                super(itemView);

                messageText = itemView.findViewById(R.id.text_message_body);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(MessagesData message) {

                messageText.setText(message.message + Html.fromHtml(
                        " &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"));
                Linkify.addLinks(messageText, Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time.replace(".0", ""))));

                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
            }
        }

        private class SentImageHolder extends RecyclerView.ViewHolder {
            TextView timeText;
            ImageView tickimage, uploadimage, downloadicon;
            RelativeLayout progresslay;
            ProgressWheel progressbar;

            SentImageHolder(View itemView) {
                super(itemView);

                uploadimage = itemView.findViewById(R.id.uploadimage);
                timeText = itemView.findViewById(R.id.text_message_time);
                tickimage = itemView.findViewById(R.id.tickimage);
                progresslay = itemView.findViewById(R.id.progresslay);
                progressbar = itemView.findViewById(R.id.progressbar);
                downloadicon = itemView.findViewById(R.id.downloadicon);
            }

            void bind(final MessagesData message) {
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }

                switch (message.message_type) {
                    case "image":
                        if (message.delivery_status.equals("read")) {
                            tickimage.setVisibility(View.VISIBLE);
                            tickimage.setImageResource(R.drawable.double_tick);
                            tickimage.setColorFilter(ContextCompat.getColor(mContext, R.color.colorAccent));
                        } else if (message.delivery_status.equals("sent")) {
                            tickimage.setVisibility(View.VISIBLE);
                            tickimage.setImageResource(R.drawable.double_tick_unseen);
                            tickimage.setColorFilter(ContextCompat.getColor(mContext, R.color.white));
                        } else if (message.progress.equals("completed")) {
                            tickimage.setVisibility(View.VISIBLE);
                            tickimage.setImageResource(R.drawable.single_tick);
                            tickimage.setColorFilter(ContextCompat.getColor(mContext, R.color.white));
                        } else {
                            tickimage.setVisibility(View.GONE);
                        }

                        downloadicon.setImageResource(R.drawable.upload);
                        switch (message.progress) {
                            case "": {
                                progresslay.setVisibility(View.VISIBLE);
                                progressbar.setVisibility(View.VISIBLE);
                                progressbar.spin();
                                File file = storageManager.getImage("sent", getFileName(message.attachment));
                                if (file != null) {
                                    Log.v(TAG, "checkChat=" + file.getAbsolutePath());
                                    Glide.with(mContext).load(Uri.fromFile(file)).thumbnail(0.5f)
                                            .into(uploadimage);
                                }
                                break;
                            }
                            case "completed": {
                                progresslay.setVisibility(View.GONE);
                                progressbar.setVisibility(View.GONE);
                                progressbar.stopSpinning();
                                if (storageManager.checkifImageExists(StorageManager.TAG_SENT, getFileName(message.attachment))) {
                                    File file = storageManager.getImage(StorageManager.TAG_SENT, getFileName(message.attachment));
                                    Log.v(TAG, "checkChat=" + file.getAbsolutePath());
                                    Glide.with(mContext).load(Uri.fromFile(file)).thumbnail(0.5f)
                                            .into(uploadimage);
                                } else {
                                    File thumbnail = storageManager.getImage(StorageManager.TAG_THUMB, getFileName(message.thumbnail));
                                    Log.i(TAG, "bind: " + message.thumbnail);
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
                                if (file != null) {
                                    Log.v(TAG, "checkChat=" + file.getAbsolutePath());
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
                                    stopMedia();
                                    if (message.progress.equals("error")) {
                                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                                            networkSnack();
                                        } else {
                                            try {
                                                progressbar.setVisibility(View.VISIBLE);
                                                progressbar.spin();
                                                dbhelper.updateMessageData(message.message_id, Constants.TAG_PROGRESS, "");
                                                message.progress = "";
                                                byte[] bytes = FileUtils.readFileToByteArray(new File(message.attachment));
                                                uploadImage(bytes, message.attachment, message, "");
                                            } catch (IOException ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    } else if (message.progress.equals("completed")) {
                                        if (storageManager.checkifImageExists("sent", getFileName(message.attachment))) {
                                            File file = storageManager.getImage("sent", getFileName(message.attachment));
                                            if (file != null) {
                                                Log.v(TAG, "file=" + file.getAbsolutePath());
                                                ApplicationClass.openImage(mContext, file.getAbsolutePath(), Constants.TAG_MESSAGE, imageView);
                                            }
                                        } else {
                                            Toast.makeText(ChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }
                        });
                        break;
                    case "location":
                        switch (message.delivery_status) {
                            case "read":
                                tickimage.setVisibility(View.VISIBLE);
                                tickimage.setImageResource(R.drawable.double_tick);
                                tickimage.setColorFilter(ContextCompat.getColor(mContext, R.color.colorAccent));
                                break;
                            case "sent":
                                tickimage.setVisibility(View.VISIBLE);
                                tickimage.setImageResource(R.drawable.double_tick_unseen);
                                tickimage.setColorFilter(ContextCompat.getColor(mContext, R.color.white));
                                break;
                            default:
                                tickimage.setVisibility(View.VISIBLE);
                                tickimage.setImageResource(R.drawable.single_tick);
                                tickimage.setColorFilter(ContextCompat.getColor(mContext, R.color.white));
                                break;
                        }
                        progresslay.setVisibility(View.GONE);
                        Glide.with(mContext).load(ApplicationClass.getMapUrl(message.lat, message.lon, mContext)).thumbnail(0.5f)
                                .into(uploadimage);

                        uploadimage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!chatLongPressed) {
                                    stopMedia();
                                    Intent i = new Intent(ChatActivity.this, LocationActivity.class);
                                    i.putExtra("from", "view");
                                    i.putExtra("lat", message.lat);
                                    i.putExtra("lon", message.lon);
                                    startActivity(i);
                                }
                            }
                        });
                        break;
                    case "video":
                        if (message.delivery_status.equals("read")) {
                            tickimage.setVisibility(View.VISIBLE);
                            tickimage.setImageResource(R.drawable.double_tick);
                            tickimage.setColorFilter(ContextCompat.getColor(mContext, R.color.colorAccent));
                        } else if (message.delivery_status.equals("sent")) {
                            tickimage.setVisibility(View.VISIBLE);
                            tickimage.setImageResource(R.drawable.double_tick_unseen);
                            tickimage.setColorFilter(ContextCompat.getColor(mContext, R.color.white));
                        } else if (message.progress.equals("completed")) {
                            tickimage.setVisibility(View.VISIBLE);
                            tickimage.setImageResource(R.drawable.single_tick);
                            tickimage.setColorFilter(ContextCompat.getColor(mContext, R.color.white));
                        } else {
                            tickimage.setVisibility(View.GONE);
                        }

                        progresslay.setVisibility(View.VISIBLE);
                        switch (message.progress) {
                            case "": {
                                progressbar.setVisibility(View.VISIBLE);
                                progressbar.spin();
                                downloadicon.setImageResource(R.drawable.upload);
                                File file = storageManager.getImage("sent", getFileName(message.thumbnail));
                                if (file != null) {
                                    Glide.with(mContext).load(file.getAbsolutePath()).thumbnail(0.5f)
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
                                File thumbnail = storageManager.getImage("sent", getFileName(message.thumbnail));
                                if (thumbnail != null) {
                                    Glide.with(mContext).load(thumbnail.getAbsolutePath())
                                            .thumbnail(0.5f)
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
                                                    dbhelper.updateMessageData(message.message_id, Constants.TAG_PROGRESS, "");
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
                                                Toast.makeText(ChatActivity.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    } else if (message.progress.equals("completed")) {
                                        if (storageManager.checkifFileExists(message.attachment, message.message_type, "sent")) {
                                            try {
                                                Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_VIEW);
                                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                File file = storageManager.getFile(message.attachment, message.message_type, "sent");
                                                Uri photoURI = FileProvider.getUriForFile(mContext,
                                                        BuildConfig.APPLICATION_ID + ".provider", file);

                                                MimeTypeMap mime = MimeTypeMap.getSingleton();
                                                String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                                String type = mime.getMimeTypeFromExtension(ext);

                                                intent.setDataAndType(photoURI, type);
                                                stopMedia();

                                                startActivity(intent);
                                            } catch (ActivityNotFoundException e) {
                                                Toast.makeText(ChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                                e.printStackTrace();
                                            }
                                        } else {
                                            Toast.makeText(ChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
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
            TextView timeText;
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
                videoprogresslay = itemView.findViewById(R.id.videoprogresslay);
                videoprogressbar = itemView.findViewById(R.id.videoprogressbar);
            }

            void bind(final MessagesData message) {
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }

                switch (message.message_type) {
                    case "image":
                        videoprogresslay.setVisibility(View.GONE);
                        downloadicon.setImageResource(R.drawable.download);
                        if (storageManager.checkifImageExists("thumb", message.attachment)) {
                            progresslay.setVisibility(View.GONE);
                            File file = storageManager.getImage("thumb", message.attachment);
                            ExifInterface exif = null;
                            int orientation = 0;
                            if (file != null) {
                                Glide.with(mContext).load(file).thumbnail(0.5f)
                                        .into(uploadimage);
                            }
                        } else {
                            progresslay.setVisibility(View.VISIBLE);
                            progressbar.setVisibility(View.VISIBLE);
                            progressbar.stopSpinning();
                            Glide.with(mContext).load(Constants.CHAT_IMG_PATH + message.attachment).thumbnail(0.5f)
                                    .apply(RequestOptions.overrideOf(18, 18))
                                    .into(uploadimage);
                        }

                        timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));

                        uploadimage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!chatLongPressed) {
                                    if (storageManager.checkifImageExists("thumb", message.attachment)) {
                                        File file = storageManager.getImage("thumb", message.attachment);
                                        if (file != null) {
                                            Log.v(TAG, "file=" + file.getAbsolutePath());
                                            videoprogresslay.setVisibility(View.GONE);
                                            stopMedia();
                                            ApplicationClass.openImage(mContext, file.getAbsolutePath(), Constants.TAG_MESSAGE, imageView);
                                        }
                                    } else {
                                        if (ContextCompat.checkSelfPermission(ChatActivity.this, WRITE_EXTERNAL_STORAGE)
                                                != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(ChatActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, 100);
                                        } else {
                                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                                networkSnack();
                                            } else {
                                                ImageDownloader imageDownloader = new ImageDownloader(ChatActivity.this) {
                                                    @Override
                                                    protected void onPostExecute(Bitmap imgBitmap) {
                                                        if (imgBitmap == null) {
                                                            Log.v("bitmapFailed", "bitmapFailed");
                                                            Toast.makeText(mContext, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Log.v("onBitmapLoaded", "onBitmapLoaded");
                                                            try {
                                                                String status = storageManager.saveThumbNail(imgBitmap, message.attachment);
                                                                if (status.equals("success")) {
                                                                    File thumbFile = storageManager.getImage("thumb", message.attachment);
                                                                    Glide.with(mContext).load(thumbFile).thumbnail(0.5f)
                                                                            .into(uploadimage);
                                                                    progresslay.setVisibility(View.GONE);
                                                                    progressbar.stopSpinning();
                                                                    videoprogresslay.setVisibility(View.GONE);
//                                                                }
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
                                                        //progressbar.setProgress(Integer.parseInt(progress[0]));
                                                    }
                                                };
                                                imageDownloader.execute(Constants.CHAT_IMG_PATH + message.attachment, "receive");
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
                        videoprogresslay.setVisibility(View.GONE);
                        Glide.with(mContext).load(ApplicationClass.getMapUrl(message.lat, message.lon, mContext)).thumbnail(0.5f)
                                .into(uploadimage);
                        timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
                        uploadimage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!chatLongPressed) {
                                    stopMedia();
                                    Intent i = new Intent(ChatActivity.this, LocationActivity.class);
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
                        timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
                        if (storageManager.checkifFileExists(message.attachment, message.message_type, "receive") &&
                                storageManager.checkifImageExists("thumb", message.thumbnail)) {
                            Log.v("dddd", "video-if");
                            videoprogresslay.setVisibility(View.GONE);
                            File file = storageManager.getImage("thumb", message.thumbnail);
                            if (file != null) {
                                Log.v(TAG, "file=" + file.getAbsolutePath());
                                Glide.with(mContext).load(Uri.fromFile(file)).thumbnail(0.5f)
                                        .into(uploadimage);
                            }
                        } else {
                            Log.v("dddd", "video-else=" + message.thumbnail);
                            Glide.with(mContext).load(Constants.CHAT_IMG_PATH + message.thumbnail).thumbnail(0.5f)
                                    .apply(RequestOptions.overrideOf(18, 18))
                                    .into(uploadimage);
                            videoprogresslay.setVisibility(View.VISIBLE);
                            videoprogressbar.setVisibility(View.VISIBLE);
                            videoprogressbar.stopSpinning();
                        }

                        uploadimage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!chatLongPressed) {
                                    if (storageManager.checkifFileExists(message.attachment, message.message_type, "receive") &&
                                            storageManager.checkifImageExists("thumb", message.thumbnail)) {
                                        try {
                                            Intent intent = new Intent();
                                            intent.setAction(Intent.ACTION_VIEW);
                                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            File file = storageManager.getFile(message.attachment, message.message_type, "receive");
                                            Uri photoURI = FileProvider.getUriForFile(mContext,
                                                    BuildConfig.APPLICATION_ID + ".provider", file);

                                            MimeTypeMap mime = MimeTypeMap.getSingleton();
                                            String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                            String type = mime.getMimeTypeFromExtension(ext);

                                            intent.setDataAndType(photoURI, type);
                                            stopMedia();

                                            startActivity(intent);
                                        } catch (ActivityNotFoundException e) {
                                            Toast.makeText(ChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                            e.printStackTrace();
                                        }
                                    } else {
                                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                                            networkSnack();
                                        } else {
                                            ImageDownloader imageDownloader = new ImageDownloader(ChatActivity.this) {
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

                                                                DownloadFiles downloadFiles = new DownloadFiles(ChatActivity.this) {
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
                                                                downloadFiles.execute(Constants.CHAT_IMG_PATH + message.attachment, message.message_type);
                                                            }
//                                                        } else {
//                                                            Toast.makeText(mContext, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
//                                                            videoprogresslay.setVisibility(View.GONE);
//                                                            videoprogressbar.setVisibility(View.GONE);
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
                                            imageDownloader.execute(Constants.CHAT_IMG_PATH + message.thumbnail, "thumb");
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
            ImageView tickimage, icon, uploadicon;
            RelativeLayout file_body_lay;
            ProgressWheel progressbar;

            SentFileHolder(View itemView) {
                super(itemView);

                filename = itemView.findViewById(R.id.filename);
                timeText = itemView.findViewById(R.id.text_message_time);
                tickimage = itemView.findViewById(R.id.tickimage);
                icon = itemView.findViewById(R.id.icon);
                file_body_lay = itemView.findViewById(R.id.file_body_lay);
                progressbar = itemView.findViewById(R.id.progressbar);
                uploadicon = itemView.findViewById(R.id.uploadicon);
                file_type_tv = itemView.findViewById(R.id.file_type_tv);
            }

            void bind(final MessagesData message) {
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
                if (message.delivery_status.equals("read")) {
                    tickimage.setVisibility(View.VISIBLE);
                    tickimage.setImageResource(R.drawable.double_tick);
                } else if (message.delivery_status.equals("sent")) {
                    tickimage.setVisibility(View.VISIBLE);
                    tickimage.setImageResource(R.drawable.double_tick_unseen);
                } else if (message.progress.equals("completed")) {
                    tickimage.setVisibility(View.VISIBLE);
                    tickimage.setImageResource(R.drawable.single_tick);
                } else {
                    tickimage.setVisibility(View.GONE);
                }

                if (message.message_type.equals("document")) {
                    icon.setImageResource(R.drawable.icon_file_unknown);
                    file_type_tv.setVisibility(View.VISIBLE);
                    file_type_tv.setText(firstThree(FilenameUtils.getExtension(message.attachment)));
                } else if (message.message_type.equals("audio")) {
                    icon.setImageResource(R.drawable.mp3);
                    file_type_tv.setVisibility(View.GONE);
                }

                switch (message.progress) {
                    case "":
                        progressbar.setVisibility(View.VISIBLE);
                        progressbar.spin();
                        uploadicon.setVisibility(View.VISIBLE);
                        filename.setText(R.string.uploading);
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
                        filename.setText(R.string.retry);
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
                                        filename.setText(getString(R.string.uploading));
                                        dbhelper.updateMessageData(message.message_id, Constants.TAG_PROGRESS, "");
                                        message.progress = "";
                                        Intent service = new Intent(ChatActivity.this, FileUploadService.class);
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
                            } else if (message.progress.equals("completed")) {
                                if (storageManager.checkifFileExists(message.attachment, message.message_type, "sent")) {
                                    try {
                                        Intent intent = new Intent();
                                        intent.setAction(Intent.ACTION_VIEW);
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        File file = storageManager.getFile(message.attachment, message.message_type, "sent");
                                        Uri photoURI = FileProvider.getUriForFile(mContext,
                                                BuildConfig.APPLICATION_ID + ".provider", file);

                                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                                        String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                        String type = mime.getMimeTypeFromExtension(ext);

                                        intent.setDataAndType(photoURI, type);

                                        stopMedia();

                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                        Toast.makeText(ChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                } else {
                                    Toast.makeText(ChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                });
            }
        }

        private class ReceivedFileHolder extends RecyclerView.ViewHolder {
            TextView filename, timeText, file_type_tv;
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
                file_type_tv = itemView.findViewById(R.id.file_type_tv);
            }

            void bind(final MessagesData message) {
                filename.setText(message.message);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                if (message.message_type.equals("document")) {
                    icon.setImageResource(R.drawable.icon_file_unknown);
                    file_type_tv.setVisibility(View.VISIBLE);
                    file_type_tv.setText(firstThree(FilenameUtils.getExtension(message.attachment)));
                } else if (message.message_type.equals("audio")) {
                    file_type_tv.setVisibility(View.GONE);
                    icon.setImageResource(R.drawable.mp3);
                }

                if (storageManager.checkifFileExists(message.attachment, message.message_type, "receive")) {
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
                            if (storageManager.checkifFileExists(message.attachment, message.message_type, "receive")) {
                                try {
                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_VIEW);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    File file = storageManager.getFile(message.attachment, message.message_type, "receive");
                                    Uri photoURI = FileProvider.getUriForFile(mContext,
                                            BuildConfig.APPLICATION_ID + ".provider", file);

                                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                                    String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                    String type = mime.getMimeTypeFromExtension(ext);

                                    intent.setDataAndType(photoURI, type);

                                    stopMedia();

                                    startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(ChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            } else {
                                if (isNetworkConnected().equals(NOT_CONNECT)) {
                                    networkSnack();
                                } else {
                                    DownloadFiles downloadFiles = new DownloadFiles(ChatActivity.this) {
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
                                    downloadFiles.execute(Constants.CHAT_IMG_PATH + message.attachment, message.message_type);
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
            ImageView tickimage;

            SentContactHolder(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                phoneno = itemView.findViewById(R.id.phoneno);
                tickimage = itemView.findViewById(R.id.tickimage);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(MessagesData message) {
                username.setText(message.contact_name);
                phoneno.setText(message.contact_phone_no);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                switch (message.delivery_status) {
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
        }

        private class ReceivedContactHolder extends RecyclerView.ViewHolder {
            TextView username, phoneno, timeText, addcontact;

            ReceivedContactHolder(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                phoneno = itemView.findViewById(R.id.phoneno);
                timeText = itemView.findViewById(R.id.text_message_time);
                addcontact = itemView.findViewById(R.id.addcontact);
            }

            void bind(final MessagesData message) {
                username.setText(message.contact_name);
                phoneno.setText(message.contact_phone_no);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                addcontact.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!chatLongPressed) {
                            stopMedia();
                            Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
                            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, message.contact_phone_no);
                            intent.putExtra(ContactsContract.Intents.Insert.NAME, message.contact_name);
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

            void bind(final MessagesData message) {
                timeText.setText(Utils.getFormattedDate(mContext, Long.parseLong(message.chat_time)));
            }
        }

        private class DeleteMsgSent extends RecyclerView.ViewHolder {
            TextView timeText;

            DeleteMsgSent(View itemView) {
                super(itemView);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(final MessagesData message) {
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
            }
        }

        private class DeleteMsgReceived extends RecyclerView.ViewHolder {
            TextView timeText;

            DeleteMsgReceived(View itemView) {
                super(itemView);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(final MessagesData message) {
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
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

            void bind(final MessagesData message, int pos) {


                msg_time.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
                duration.setVisibility(View.INVISIBLE);
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
                        filename.setText(R.string.uploading);
                        break;
                    case "completed":
                        progressbar.setVisibility(View.GONE);
                        progressbar.stopSpinning();
                        uploadicon.setVisibility(View.GONE);
                        seekbar.setVisibility(View.VISIBLE);
                        icon.setVisibility(View.VISIBLE);
                        filename.setVisibility(View.GONE);
                        duration.setVisibility(View.VISIBLE);
                        break;
                    case "error":
                        progressbar.setVisibility(View.VISIBLE);
                        progressbar.stopSpinning();
                        uploadicon.setVisibility(View.VISIBLE);
                        seekbar.setVisibility(View.GONE);
                        icon.setVisibility(View.GONE);
                        filename.setText(R.string.retry);
                        break;
                }

                switch (message.delivery_status) {
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

                if (storageManager.checkifFileExists(message.attachment, message.message_type, "sent")) {
                    duration.setText(milliSecondsToTimer(mediaDuration(getAdapterPosition(), "sent")));
                } else {
                    duration.setVisibility(View.GONE);
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
                                        duration.setVisibility(View.VISIBLE);
                                        filename.setText(getString(R.string.uploading));
                                        dbhelper.updateMessageData(message.message_id, Constants.TAG_PROGRESS, "");
                                        message.progress = "";
                                        Intent service = new Intent(ChatActivity.this, FileUploadService.class);
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

                //sentVoiceHolder

                icon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!chatLongPressed) {
                            duration.setVisibility(View.VISIBLE);
                            if (storageManager.checkifFileExists(message.attachment, message.message_type, "sent")) {
                                playMedia(context, getAdapterPosition(), "sent");
                            } else {
                                Toast.makeText(ChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
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

            void bind(final MessagesData message, int pos) {
                msg_time.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));
                seekbar.getProgressDrawable().setColorFilter(ContextCompat.getColor(context, R.color.white), PorterDuff.Mode.MULTIPLY);

                duration.setVisibility(View.INVISIBLE);

                icon.setImageResource(R.drawable.pause_icon_white);

                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                if (storageManager.checkifFileExists(message.attachment, message.message_type, "receive")) {
                    downloadIcon.setVisibility(View.GONE);
                    progressbar.setVisibility(View.GONE);
                    duration.setVisibility(View.VISIBLE);
                    duration.setText(milliSecondsToTimer(mediaDuration(getAdapterPosition(), "receive")));
                    icon.setEnabled(true);
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
                            if (storageManager.checkifFileExists(message.attachment, message.message_type, "receive")) {
                                body_lay.setEnabled(false);
                            } else {
                                if (isNetworkConnected().equals(NOT_CONNECT)) {
                                    networkSnack();
                                } else {
                                    DownloadFiles downloadFiles = new DownloadFiles(ChatActivity.this) {
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
                                    downloadFiles.execute(Constants.CHAT_IMG_PATH + message.attachment, message.message_type);
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

                icon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!chatLongPressed) {
                            duration.setVisibility(View.VISIBLE);
                            if (storageManager.checkifFileExists(message.attachment, message.message_type, "receive")) {
                                playMedia(context, getAdapterPosition(), "receive");
                            } else {
                                Toast.makeText(ChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

            }
        }

        private class SentStatusViewHolder extends RecyclerView.ViewHolder {

            TextView timeText, statusName, statusTypeName, textMessageBody;
            ImageView tickimage, statusTypeImage;
            RoundedImageView statusImage;
            RelativeLayout parentLay;

            SentStatusViewHolder(View itemView) {
                super(itemView);
                timeText = itemView.findViewById(R.id.text_message_time);
                tickimage = itemView.findViewById(R.id.tickimage);
                statusImage = itemView.findViewById(R.id.statusImage);
                statusName = itemView.findViewById(R.id.statusName);
                statusTypeImage = itemView.findViewById(R.id.statusTypeImage);
                statusTypeName = itemView.findViewById(R.id.statusTypeName);
                textMessageBody = itemView.findViewById(R.id.text_message_body);
                parentLay = itemView.findViewById(R.id.parentLay);
            }

            void bind(MessagesData message) {
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }

                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));

                switch (message.delivery_status) {
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

                results = dbhelper.getContactDetail(userId);
                statusName.setText(results.user_name + " . " + getString(R.string.status));

                textMessageBody.setText(message.message + Html.fromHtml(
                        " &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"));
                Linkify.addLinks(textMessageBody, Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS);
                StatusDatas.Status data = getStatusData(message.statusData);

                if (data.storyType.equals("video")) {
                    statusTypeImage.setImageResource(R.drawable.video);
                    statusTypeName.setText(R.string.video);
                    setstatusThumb(data.mThumbnail, statusImage);
                } else {
                    statusTypeImage.setImageResource(R.drawable.camera_status);
                    statusTypeName.setText(R.string.image);
                    setstatusThumb(data.attachment, statusImage);
                }


                statusImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!chatLongPressed && data.storyId != null &&
                                !TextUtils.isEmpty(data.storyId) && dbhelper.isStoryExists(data.storyId)) {
                            stopMedia();
                            StatusDatas datas = dbhelper.getSingleStatus(data.storyId);
                            Intent intent = new Intent(ChatActivity.this, SingleStoryActivity.class);
                            intent.putExtra(Constants.TAG_DATA, datas);
                            startActivity(intent);
                        } else {
                            ApplicationClass.showToast(ChatActivity.this, getString(R.string.no_media_found), Toast.LENGTH_SHORT);
                        }
                    }
                });

            }
        }


        private void setstatusThumb(final String thumb, final ImageView image) {
            if (storageManager.checkifImageExists("thumb", thumb)) {
                File file = storageManager.getImage("thumb", thumb);
                if (file != null) {
                    Log.d(TAG, "isImageExists: " + file.getAbsolutePath());
                    Glide.with(mContext).load(file).thumbnail(0.5f)
                            .transition(new DrawableTransitionOptions().crossFade())
                            .into(image);
                }
            } else {
                if (!ChatActivity.this.isDestroyed()) {
                    Glide.with(mContext).load(new ColorDrawable(ContextCompat.getColor(ChatActivity.this, R.color.secondarybg))).thumbnail(0.5f)
                            .transition(new DrawableTransitionOptions().crossFade())
                            .into(image);
                }
                ImageDownloader imageDownloader = new ImageDownloader(ChatActivity.this) {
                    @Override
                    protected void onPostExecute(Bitmap imgBitmap) {
                        if (imgBitmap == null) {
                        } else {
                            Log.v("onBitmapLoaded", "onBitmapLoaded");
                            try {
                                String status = storageManager.saveThumbNail(imgBitmap, thumb);
                                if (status.equals("success")) {
                                    File thumbFile = storageManager.getImage("thumb", thumb);
                                    Log.d(TAG, "isImageExists: " + thumbFile.getAbsolutePath());
                                    Glide.with(mContext).load(thumbFile).thumbnail(0.5f)
                                            .into(image);
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
                imageDownloader.execute(Constants.CHAT_IMG_PATH + thumb, "thumb");
            }
        }

        private class ReceivedStatusViewHolder extends RecyclerView.ViewHolder {

            TextView timeText, statusName, statusTypeName, textMessageBody;
            ImageView statusTypeImage;
            RoundedImageView statusImage;
            RelativeLayout parentLay;

            ReceivedStatusViewHolder(@NonNull View itemView) {
                super(itemView);
                timeText = itemView.findViewById(R.id.text_message_time);
                statusImage = itemView.findViewById(R.id.statusImage);
                statusName = itemView.findViewById(R.id.statusName);
                statusTypeImage = itemView.findViewById(R.id.statusTypeImage);
                statusTypeName = itemView.findViewById(R.id.statusTypeName);
                textMessageBody = itemView.findViewById(R.id.text_message_body);
                parentLay = itemView.findViewById(R.id.parentLay);
            }

            void bind(MessagesData message) {
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }

                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chat_time)));

                statusName.setText(getString(R.string.you) + " . " + getString(R.string.status));

                StatusDatas.Status data = getStatusData(message.statusData);

                textMessageBody.setText(message.message + Html.fromHtml(
                        " &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"));
                Linkify.addLinks(textMessageBody, Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS);

                if (data.storyType != null && data.storyType.equals("video")) {
                    statusTypeImage.setImageResource(R.drawable.video);
                    statusTypeName.setText(R.string.video);
                    setstatusThumb(data.mThumbnail, statusImage);
                } else {
                    statusTypeImage.setImageResource(R.drawable.camera_status);
                    setstatusThumb(data.attachment, statusImage);
                    statusTypeName.setText(R.string.image);
                }


                statusImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!chatLongPressed && data.storyId != null && dbhelper.isStoryExists(data.storyId)) {
                            stopMedia();
                            StatusDatas datas = dbhelper.getSingleStatus(data.storyId);
                            Intent intent;
                            intent = new Intent(ChatActivity.this, SingleStoryActivity.class);
                            intent.putExtra(Constants.TAG_DATA, datas);
                            startActivity(intent);
                        } else {
                            ApplicationClass.showToast(ChatActivity.this, getString(R.string.no_media_found), Toast.LENGTH_SHORT);
                        }
                    }
                });

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
                indiaList.addItemDecoration(new DividerItemDecoration(ChatActivity.this, RecyclerView.VERTICAL));
                otherList.addItemDecoration(new DividerItemDecoration(ChatActivity.this, RecyclerView.VERTICAL));
                indiaList.setLayoutManager(new LinearLayoutManager(ChatActivity.this));
                otherList.setLayoutManager(new LinearLayoutManager(ChatActivity.this));
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
            Speech.getInstance().startListening(progress, ChatActivity.this);

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
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
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
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(ChatActivity.this);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
        builder.setMessage("speech not available")
                .setCancelable(false)
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
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
        // Toast.makeText(this, "partial " + sppechText, Toast.LENGTH_SHORT).show();
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
        // myService.counterFab.setVisibility(View.VISIBLE);

        // sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void disableWakeLock() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }


void initTextToSpeech(){
    ttobj = new TextToSpeech(ChatActivity.this, new TextToSpeech.OnInitListener() {
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
            if (ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

                onRecordAudioPermissionGranted();

            } else {
                ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST);
            }

        }
    }


}
