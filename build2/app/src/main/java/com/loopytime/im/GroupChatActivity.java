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
import com.bumptech.glide.request.RequestOptions;
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
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.StorageManager;
import com.loopytime.helper.Utils;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupData;
import com.loopytime.model.GroupMessage;
import com.loopytime.model.GroupUpdateResult;
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
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.im.GroupChatActivity.MessageListAdapter.VIEW_TYPE_DATE;
import static com.loopytime.utils.Constants.TAG_ADMIN;
import static com.loopytime.utils.Constants.TAG_GROUP;
import static com.loopytime.utils.Constants.TAG_GROUP_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER;
import static com.loopytime.utils.Constants.TAG_MEMBER_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER_NO;
import static com.loopytime.utils.Constants.TAG_MEMBER_ROLE;
import static com.loopytime.utils.Constants.TRUE;

public class GroupChatActivity extends BaseActivity implements View.OnClickListener, SocketConnection.GroupChatCallbackListener,
        TextWatcher, DeleteAdapter.deleteListener, SpeechDelegate {
    public static final int ACTIVITY_RECORD_SOUND = 0;
    public static String tempGroupId = "";
    EditText editText;
    FloatingActionButton voiceFab;
    String groupId, groupName;
    TextToSpeech ttobj;
    private final int PERMISSIONS_REQUEST = 19876;
    View bottomLayout, fabLay, linearLayout;
    List<GroupMessage> messagesList = new ArrayList<>();
    String TAG = this.getClass().getSimpleName();
    RecyclerView recyclerView;
    FloatingWidgetService myService;
    boolean isBound = false;
    public boolean isSppechEnable = false;
    boolean isPermissionCall = false;
    int retry = 1;
    TextView username, online, txtMembers, audioTime;
    RelativeLayout chatUserLay, mainLay, attachmentsLay, imageViewLay, bottomLay, forwordLay;
    ImageView attachbtn, optionbtn, backbtn, send, audioCallBtn, videoCallBtn, cameraBtn,
            galleryBtn, fileBtn, audioBtn, locationBtn, contactBtn, imageView, forwordBtn, copyBtn, closeBtn, deleteBtn;
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
    GroupData groupData;
    ArrayList<GroupMessage> selectedChatPos = new ArrayList<>();
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
    private Dialog permissionDialog;
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

    public static String getFormattedDate(Context context, long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis * 1000L);

        Calendar now = Calendar.getInstance();

        final String dateTimeFormatString = "d MMMM yyyy";
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            return context.getString(R.string.today);
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            return context.getString(R.string.yesterday);
        } else {
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        }
    }

    public static boolean isVideo(String mimeType) {
        Log.v("mimeType", "mimeType=" + mimeType);
        return mimeType != null && mimeType.startsWith("video");
    }

    public static GroupMessage getMessages(DatabaseHandler dbhelper, Context mContext, GroupMessage groupMessage) {
        if (groupMessage.messageType != null) {
            switch (groupMessage.messageType) {
                case "text":
                case "image":
                case "video":
                case "file":
                case "location":
                case "contact":
                case "audio":
                    groupMessage.message = groupMessage.message != null ? groupMessage.message : "";
                    break;
                case "create_group":
                    if (groupMessage.groupAdminId.equals(GetSet.getUserId())) {
                        groupMessage.message = mContext.getString(R.string.you_created_the_group);
                    } else {
                        if (dbhelper.isUserExist(groupMessage.groupAdminId)) {
                            groupMessage.message = ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupMessage.groupAdminId), dbhelper.getContactCountryCode(groupMessage.groupAdminId),dbhelper.getContactPhone(groupMessage.groupAdminId)) + " " + mContext.getString(R.string.created_the_group);
                        } else {
                            groupMessage.message = mContext.getString(R.string.group_created);
                        }
                    }
                    break;
                case "add_member":
                    if (groupMessage.attachment.equals("")) {
                        if (dbhelper.isUserExist(groupMessage.groupAdminId)) {
                            groupMessage.message = ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupMessage.groupAdminId), dbhelper.getContactCountryCode(groupMessage.groupAdminId),dbhelper.getContactPhone(groupMessage.groupAdminId)) + " " + mContext.getString(R.string.added_you);
                        } else {
                            groupMessage.message = mContext.getString(R.string.you_were_added);
                        }
                    } else {
                        try {
                            JSONArray jsonArray = new JSONArray(groupMessage.attachment);
                            ArrayList<String> members = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                if (jsonObject.getString(TAG_MEMBER_ID).equals(GetSet.getUserId())) {
                                    members.add(mContext.getString(R.string.you));
                                } else if (dbhelper.isUserExist(jsonObject.getString(Constants.TAG_MEMBER_ID))) {
                                    members.add(ApplicationClass.getContactName(mContext, jsonObject.getString(TAG_MEMBER_NO), dbhelper.getContactCountryCode(jsonObject.getString(Constants.TAG_MEMBER_ID)),jsonObject.getString(TAG_MEMBER_NO)));
                                }
                            }
                            String memberstr = members.toString().replaceAll("[\\[\\]]|(?<=,)\\s+", "");
                            if (groupMessage.memberId.equals(GetSet.getUserId())) {
                                groupMessage.message = mContext.getString(R.string.you_added) + " " + memberstr;
                            } else {
                                groupMessage.message = ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupMessage.groupAdminId), dbhelper.getContactCountryCode(groupMessage.groupAdminId),dbhelper.getContactPhone(groupMessage.groupAdminId)) + " " + mContext.getString(R.string.added) + " " + memberstr;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case "group_image":
                case "subject":
                    if (groupMessage.memberId.equalsIgnoreCase(GetSet.getUserId())) {
                        groupMessage.message = mContext.getString(R.string.you) + " " + groupMessage.message;
                    } else {
                        groupMessage.message = ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupMessage.memberId), dbhelper.getContactCountryCode(groupMessage.memberId), dbhelper.getContactPhone(groupMessage.memberId)) + " " + groupMessage.message;
                    }
                    break;
                case "left":
                    if (groupMessage.memberId.equalsIgnoreCase(GetSet.getUserId())) {
                        groupMessage.message = mContext.getString(R.string.you_left);
                    } else {
                        groupMessage.message = ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupMessage.memberId), dbhelper.getContactCountryCode(groupMessage.memberId), dbhelper.getContactPhone(groupMessage.memberId)) + " " + mContext.getString(R.string.left);
                    }
                    break;
                case "remove_member":
                    if (groupMessage.groupAdminId.equals(GetSet.getUserId())) {
                        groupMessage.message = mContext.getString(R.string.you_removed) + " " + ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupMessage.memberId), dbhelper.getContactCountryCode(groupMessage.memberId), dbhelper.getContactPhone(groupMessage.memberId));
                    } else {
                        if (groupMessage.memberId.equals(GetSet.getUserId())) {
                            groupMessage.message = ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupMessage.groupAdminId), dbhelper.getContactCountryCode(groupMessage.groupAdminId), dbhelper.getContactPhone(groupMessage.groupAdminId) )+ " " + mContext.getString(R.string.removed_you);
                        } else {
                            groupMessage.message = ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupMessage.groupAdminId), dbhelper.getContactCountryCode(groupMessage.groupAdminId), dbhelper.getContactPhone(groupMessage.groupAdminId)) + " " + mContext.getString(R.string.removed) + " " +
                                    ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupMessage.memberId), dbhelper.getContactCountryCode(groupMessage.memberId),dbhelper.getContactPhone(groupMessage.memberId));
                        }
                    }
                    break;
                case "admin":
                    if (groupMessage.attachment.equals(TAG_MEMBER)) {
                        groupMessage.message = mContext.getString(R.string.you_are_no_longer_as_admin);
                    } else {
                        groupMessage.message = mContext.getString(R.string.you_are_now_an_admin);
                    }
                    break;
                case "date":
                    groupMessage.message = Utils.getFormattedDate(mContext, Long.parseLong(groupMessage.chatTime));
                    break;
                case "change_number":
                    if (!groupMessage.memberId.equals(GetSet.getUserId())) {
                        groupMessage.message = ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupMessage.memberId), dbhelper.getContactCountryCode(groupMessage.memberId),dbhelper.getContactPhone(groupMessage.memberId)) + " " + groupMessage.message;
                    }
                    break;
            }
        } else {
            groupMessage.message = "";
        }

        groupMessage.message = ApplicationClass.decryptMessage(groupMessage.message);

        return groupMessage;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_chat);

        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        detailsIntent.setPackage("com.google.android.googlequicksearchbox");
        sendOrderedBroadcast(
                detailsIntent, null, new GroupChatActivity.LanguageDetailsChecker(), null, 1234, null, null);
        setSpeekProgress();
        if (getIntent().getStringExtra("notification") != null) {
            Constants.isGroupChatOpened = true;
            isFromNotification = true;
        }
        if (Constants.groupContext != null && Constants.isGroupChatOpened) {
            ((Activity) Constants.groupContext).finish();
        }
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        Constants.groupContext = this;
        getWindow().setBackgroundDrawableResource(R.drawable.chat_bg);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        send = findViewById(R.id.send);
        recordButton = findViewById(R.id.record_button);
        voiceFab = findViewById(R.id.fabSpeak);
        voiceFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "10");
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "voice typing Group");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Activity");
                ApplicationClass.getInstance().mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                speak();
            }
        });
        editText = findViewById(R.id.editText);
        chatUserLay = findViewById(R.id.chatUserLay);
        userimage = findViewById(R.id.userImg);
        username = findViewById(R.id.userName);
        txtMembers = findViewById(R.id.txtMembers);
        online = findViewById(R.id.online);
        attachbtn = findViewById(R.id.attachbtn);
        bottomLayout = findViewById(R.id.speech);
        bottomLayout.setVisibility(View.GONE);
        fabLay = findViewById(R.id.fabLay);
        linearLayout = findViewById(R.id.linearLayout);
        recordView = findViewById(R.id.record_view);
        bottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        setVoiceRecorder();
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
        closeBtn = imageViewLay.findViewById(R.id.closeBtn);
        imageView = findViewById(R.id.imageView);
        forwordLay = findViewById(R.id.forwordLay);
        forwordBtn = findViewById(R.id.forwordBtn);
        copyBtn = findViewById(R.id.copyBtn);
        deleteBtn = findViewById(R.id.deleteBtn);
        editLay = findViewById(R.id.editLay);


        excryptText = findViewById(R.id.excryptText);

        if (ApplicationClass.isRTL()) {
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        socketConnection = SocketConnection.getInstance(this);
        SocketConnection.getInstance(this).setGroupChatCallbackListener(this);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        dbhelper = DatabaseHandler.getInstance(this);
        storageManager = StorageManager.getInstance(this);
        display = getWindowManager().getDefaultDisplay();

        groupId = getIntent().getStringExtra(TAG_GROUP_ID);
        if (dbhelper.getGroupData(this, groupId) != null) {
            groupData = dbhelper.getGroupData(this, groupId);
            groupName = groupData.groupName;
            tempGroupId = groupId;

           /* NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(groupData.groupName, 0);
                notificationManager.cancel("New Group", 0);
            }*/

        } else {
            finish();
        }

        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        // set visibility status
        chatUserLay.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        audioCallBtn.setVisibility(View.GONE);
        videoCallBtn.setVisibility(View.GONE);
        optionbtn.setVisibility(View.VISIBLE);
        txtMembers.setVisibility(View.VISIBLE);

        username.setText(groupData.groupName);
        setGroupMembers(groupId);
        Glide.with(GroupChatActivity.this).load(Constants.GROUP_IMG_PATH + groupData.groupImage)
                .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.create_group).error(R.drawable.create_group))
                .into(userimage);

        totalMsg = dbhelper.getGroupMessagesCount(groupId);
        Log.v("totalMsg", "totalMsg=" + totalMsg);

        messagesList.addAll(getMessagesAry(dbhelper.getGroupMessages(groupId, "0", "20", getApplicationContext()), null));
        showEncryptionText();
        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(linearLayoutManager);
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setItemViewCacheSize(100);
//        recyclerView.setDrawingCacheEnabled(true);
//        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

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
        copyBtn.setOnClickListener(this);
        forwordBtn.setOnClickListener(this);
        closeBtn.setOnClickListener(this);
        deleteBtn.setOnClickListener(this);

        setVoiceRecorder();

        whileViewChat();

        if (!dbhelper.isMemberExist(GetSet.getUserId(), groupId)) {
            bottomLay.setVisibility(View.GONE);
        }

        endlessRecyclerOnScrollListener = new EndlessRecyclerOnScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.v("current_page", "current_page=" + page + "&totalItems=" + totalItemsCount);
                final List<GroupMessage> tmpList = new ArrayList<>(dbhelper.getGroupMessages(groupId, String.valueOf(page * 20), "20", getApplicationContext()));
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

    private void setGroupMembers(String groupId) {
        List<GroupData.GroupMembers> memberList = dbhelper.getThreeMembers(this, groupId);
        StringBuilder members = new StringBuilder();
        String prefix = "";
        for (GroupData.GroupMembers groupMembers : memberList) {
            members.append(prefix);
            prefix = ", ";
            if (groupMembers.memberId.equalsIgnoreCase(GetSet.getUserId())) {
                members.append(getString(R.string.you));
            } else {
                ContactsData.Result user = dbhelper.getContactDetail(groupMembers.memberId);
                members.append(user.user_name);
            }
        }
        txtMembers.setText("" + members);

    }

    private void setVoiceRecorder() {

        recordView.setCounterTimeColor(Color.parseColor("#a3a3a3"));
        recordView.setSmallMicColor(ContextCompat.getColor(GroupChatActivity.this, R.color.colorAccent));
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
                    if (ContextCompat.checkSelfPermission(GroupChatActivity.this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(GroupChatActivity.this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        recordView.setOnRecordListener(null);
                        ActivityCompat.requestPermissions(GroupChatActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, 111);
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
                            ApplicationClass.hideSoftKeyboard(GroupChatActivity.this, recordView);
                            editLay.setVisibility(View.VISIBLE);
                            recordView.setVisibility(View.GONE);
                        }
                    }, 1000);

                }

                @Override
                public void onFinish(long recordTime) {
                    if (recordTime > 1500) {
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

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    editLay.setVisibility(View.VISIBLE);
                                    recordView.setVisibility(View.GONE);

                                    GroupMessage mdata = updateDBList("audio", "", recordVoicePath/*,String.valueOf(getAudioTime(Time))*/);
                                    Intent service = new Intent(GroupChatActivity.this, FileUploadService.class);
                                    Bundle b = new Bundle();
                                    b.putSerializable("mdata", mdata);
                                    b.putString("filepath", recordVoicePath);
                                    b.putString("chatType", "group");
                                    service.putExtras(b);
                                    startService(service);
                                }
                            }, 200);
                        }
                    } else {
                        stopMedia();
                        editLay.setVisibility(View.VISIBLE);
                        recordView.setVisibility(View.GONE);
                        ApplicationClass.hideSoftKeyboard(GroupChatActivity.this, recordView);

                        Toast.makeText(GroupChatActivity.this, getString(R.string.less_than_second), Toast.LENGTH_SHORT).show();
                        Log.d("RecordView", "onLessThanSecond");
                    }

                }

                @Override
                public void onLessThanSecond() {
                    stopMedia();
                    editLay.setVisibility(View.VISIBLE);
                    recordView.setVisibility(View.GONE);
                    ApplicationClass.hideSoftKeyboard(GroupChatActivity.this, recordView);

                    Toast.makeText(GroupChatActivity.this, getString(R.string.less_than_second), Toast.LENGTH_SHORT).show();
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
        if (ContextCompat.checkSelfPermission(GroupChatActivity.this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(GroupChatActivity.this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(GroupChatActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, 111);

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
        Log.v("onNetwork", "GroupChat=" + isConnected);
//        if (isConnected) {
//            online.setVisibility(View.VISIBLE);
//        } else {
//            online.setVisibility(View.GONE);
//        }
    }

    private List<GroupMessage> getMessagesAry(List<GroupMessage> tmpList, GroupMessage lastData) {
        List<GroupMessage> msgList = new ArrayList<>();
        if (tmpList.size() == 0 && lastData != null) {
            GroupMessage groupMessage = new GroupMessage();
            groupMessage.messageType = "date";
            groupMessage.message = Utils.getFormattedDate(this, Long.parseLong(lastData.chatTime));
            msgList.add(groupMessage);
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
                        GroupMessage groupMessage = new GroupMessage();
                        groupMessage.messageType = "date";
                        groupMessage.message = Utils.getFormattedDate(this, Long.parseLong(tmpList.get(i).chatTime));
                        msgList.add(groupMessage);
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
    public void onGroupChatReceive(final GroupMessage mdata) {
        Log.i(TAG, "onGroupChatReceive: " + mdata);
        mdata.message = ApplicationClass.decryptMessage(mdata.message);
        mdata.attachment = ApplicationClass.decryptMessage(mdata.attachment);
        mdata.lat = ApplicationClass.decryptMessage(mdata.lat);
        mdata.lon = ApplicationClass.decryptMessage(mdata.lon);
        mdata.contactName = ApplicationClass.decryptMessage(mdata.contactName);
        mdata.contactPhoneNo = ApplicationClass.decryptMessage(mdata.contactPhoneNo);
        mdata.contactCountryCode = ApplicationClass.decryptMessage(mdata.contactCountryCode);
        runOnUiThread(new Runnable() {
            public void run() {
                if (mdata.groupId.equalsIgnoreCase(groupId)) {
                    GroupMessage groupMessage = mdata;
                    groupMessage = GroupChatActivity.getMessages(dbhelper, getApplicationContext(), mdata);


                    switch (mdata.messageType) {
                        case "text":
                        case "image":
                        case "video":
                        case "file":
                        case "location":
                        case "contact":
                        case "audio":
                        case "group_image":
                        case "document":
                        case "subject":
                            messagesList.add(0, groupMessage);
                            updatePosition();
                            showEncryptionText();
                            messageListAdapter.notifyItemInserted(0);
                            recyclerView.smoothScrollToPosition(0);
                            groupData = dbhelper.getGroupData(GroupChatActivity.this, groupId);
                            username.setText(groupData.groupName);
                            Glide.with(GroupChatActivity.this).load(Constants.GROUP_IMG_PATH + groupData.groupImage)
                                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                                    .into(userimage);
                            break;
                        case "remove_member":
                        case "left":
                            messagesList.add(0, groupMessage);
                            showEncryptionText();
                            updatePosition();
                            messageListAdapter.notifyItemInserted(0);
                            recyclerView.smoothScrollToPosition(0);
                            if (mdata.memberId.equals(GetSet.getUserId())) {
                                bottomLay.setVisibility(View.GONE);
                            } else {
                                bottomLay.setVisibility(View.VISIBLE);
                            }
                            setGroupMembers(groupId);
                            break;
                        case "add_member":
                            messagesList.add(0, groupMessage);
                            updatePosition();
                            showEncryptionText();
                            messageListAdapter.notifyItemInserted(0);
                            recyclerView.smoothScrollToPosition(0);
                            try {
                                JSONArray jsonArray = new JSONArray(mdata.attachment);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    if (jsonObject.getString(TAG_MEMBER_ID).equals(GetSet.getUserId())) {
                                        bottomLay.setVisibility(View.VISIBLE);
                                        setGroupMembers(groupId);
                                        recyclerView.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                recyclerView.scrollToPosition(0);
                                            }
                                        });
                                        break;
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                        case Constants.TAG_ISDELETE:

                            for (GroupMessage data : messagesList) {
                                if (data.messageId.equals(mdata.messageId)) {
                                    data.messageType = mdata.messageType;
                                    messageListAdapter.notifyItemChanged(messagesList.indexOf(data));
                                    break;
                                }
                            }
                            break;
                    }

                    whileViewChat();

                }
            }
        });
    }

    private void makeAdmin(String groupId) {
        GroupData groupData = dbhelper.getGroupData(getApplicationContext(), groupId);
        if (dbhelper.isGroupHaveAdmin(groupData.groupId) == 1 && groupData.groupAdminId.equalsIgnoreCase(GetSet.getUserId())) {
            List<GroupData.GroupMembers> membersData = dbhelper.getGroupMembers(getApplicationContext(), groupData.groupId);
            for (GroupData.GroupMembers groupMember : membersData) {
                if (!groupMember.memberId.equals(GetSet.getUserId())) {
                    JSONArray jsonArray = new JSONArray();
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(TAG_MEMBER_ID, groupMember.memberId);
                        jsonObject.put(TAG_MEMBER_ROLE, TAG_ADMIN);
                        jsonArray.put(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                    RandomString randomString = new RandomString(10);
                    String messageId = groupId + randomString.nextString();

                    JSONObject message = new JSONObject();
                    try {
                        message.put(Constants.TAG_GROUP_ADMIN_ID, groupMember.memberId);
                        message.put(Constants.TAG_GROUP_ID, groupId);
                        message.put(Constants.TAG_GROUP_NAME, groupData.groupName);
                        message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
                        message.put(Constants.TAG_CHAT_TIME, unixStamp);
                        message.put(Constants.TAG_MESSAGE_ID, messageId);
                        message.put(Constants.TAG_ATTACHMENT, Constants.TAG_ADMIN);
                        message.put(Constants.TAG_MEMBER_ID, groupMember.memberId);
                        message.put(Constants.TAG_MEMBER_NAME, groupMember.memberName);
                        message.put(Constants.TAG_MEMBER_NO, groupMember.memberNo);
                        message.put(Constants.TAG_MESSAGE_TYPE, "admin");
                        message.put(Constants.TAG_MESSAGE, "Admin");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    socketConnection.startGroupChat(message);
                    updateGroupData(jsonArray);
                    break;
                }
            }
        }
    }

    private void updateGroupData(JSONArray jsonArray) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<GroupUpdateResult> call3 = apiInterface.updateGroup(GetSet.getToken(), groupId, jsonArray);
        call3.enqueue(new Callback<GroupUpdateResult>() {
            @Override
            public void onResponse(Call<GroupUpdateResult> call, Response<GroupUpdateResult> response) {
                try {
                    Log.i(TAG, "updateGroup: " + new Gson().toJson(response.body()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<GroupUpdateResult> call, Throwable t) {
                Log.e(TAG, "updateGroup: " + t.getMessage());
                call.cancel();
            }
        });
    }

    private void whileViewChat() {
        dbhelper.updateGroupMessageReadStatus(groupId);
        dbhelper.resetUnseenGroupMessagesCount(groupId);
    }

    @Override
    public void onListenGroupTyping(final JSONObject data) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (dbhelper.isMemberExist(GetSet.getUserId(), groupId)) {
                    try {
                        Log.e(TAG, "onListenGroupTyping: " + data.toString());
                        String memberId = data.getString(Constants.TAG_MEMBER_ID);
                        String group_id = data.getString(Constants.TAG_GROUP_ID);
                        if (!memberId.equalsIgnoreCase(GetSet.getUserId()) &&
                                group_id.equalsIgnoreCase(groupId)) {
                            ContactsData.Result result = dbhelper.getContactDetail(memberId);
                            if (data.get("type").equals("typing")) {
                                txtMembers.setText(ApplicationClass.getContactName(GroupChatActivity.this, result.phone_no, result.country_code,result.phone_no) + " is " + getString(R.string.typing));
                            } else if (data.get("type").equals("recording")) {
                                txtMembers.setText(ApplicationClass.getContactName(GroupChatActivity.this, result.phone_no, result.country_code,result.phone_no) + " is " + getString(R.string.recording));
                            }

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setGroupMembers(groupId);
                        }
                    }, 1000);
                }
            }
        });
    }

    @Override
    public void onMemberExited(final JSONObject data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    setGroupMembers(data.getString(Constants.TAG_GROUP_ID));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setGroupMembers(groupId);
                        }
                    }, 1000);
                } catch (JSONException e) {
                    e.printStackTrace();
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

                int currentCount = dbhelper.getGroupMessagesCount(groupId);
                if (totalMsg != currentCount) {
                    messagesList.clear();
                    if (endlessRecyclerOnScrollListener != null) {
                        endlessRecyclerOnScrollListener.resetState();
                    }
                    messagesList.addAll(getMessagesAry(dbhelper.getGroupMessages(groupId, "0", "20", getApplicationContext()), null));
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
    public void onUpdateGroupInfo(GroupMessage groupMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (GroupChatActivity.this.groupId.equals(groupMessage.groupId)) {
                    switch (groupMessage.messageType) {
                        case "group_image":
                        case "subject":
                            groupData = dbhelper.getGroupData(GroupChatActivity.this, groupMessage.groupId);
                            username.setText(groupData.groupName);
                            Glide.with(GroupChatActivity.this).load(Constants.GROUP_IMG_PATH + groupData.groupImage)
                                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                                    .into(userimage);
                            break;
                        case "remove_member":
                        case "left":
                            if (groupMessage.memberId.equals(GetSet.getUserId())) {
                                bottomLay.setVisibility(View.GONE);
                            } else {
                                bottomLay.setVisibility(View.VISIBLE);
                            }
                            setGroupMembers(groupMessage.groupId);
                            break;
                        case "add_member":
                            try {
                                JSONArray jsonArray = new JSONArray(groupMessage.attachment);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    if (jsonObject.getString(TAG_MEMBER_ID).equals(GetSet.getUserId())) {
                                        bottomLay.setVisibility(View.VISIBLE);
                                        setGroupMembers(groupMessage.groupId);
                                        recyclerView.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                recyclerView.scrollToPosition(0);
                                            }
                                        });
                                        break;
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
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
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.TAG_GROUP_ID, groupId);
            jsonObject.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
            jsonObject.put("type", "typing");
            socketConnection.groupTyping(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        runnable = new Runnable() {
            public void run() {
                meTyping = false;
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_GROUP_ID, groupId);
                    jsonObject.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                    jsonObject.put("type", "untyping");
                    socketConnection.groupTyping(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, 500);
    }

    @Override
    public void deletetype(String type) {
        for (int i = 0; i < selectedChatPos.size(); i++) {
            GroupMessage mData = selectedChatPos.get(i);
            if (type.equals("me")) {
                dbhelper.deleteGroupMessageFromId(mData.messageId);
                messagesList.remove(mData);
                messageListAdapter.notifyDataSetChanged();
            } else {
                String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);

                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_GROUP_ID, groupId);
                    jsonObject.put(Constants.TAG_GROUP_NAME, groupName);
                    jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_ISDELETE);
                    jsonObject.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                    jsonObject.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                    jsonObject.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                    jsonObject.put(Constants.TAG_MESSAGE_ID, mData.messageId);
                    jsonObject.put(Constants.TAG_MESSAGE_TYPE, Constants.TAG_ISDELETE);
                    jsonObject.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(getString(R.string.this_message_was_deleted)));
                    jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);

                    socketConnection.startGroupChat(jsonObject);

                    mData.messageType = Constants.TAG_ISDELETE;
                    mData.isDelete = "1";
                    messageListAdapter.notifyItemChanged(messagesList.indexOf(mData));
                    dbhelper.updateGroupMessageData(mData.messageId, Constants.TAG_MESSAGE_TYPE, Constants.TAG_ISDELETE);

                    whileViewChat();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            // Toast.makeText(GroupChatActivity.this, getString(R.string.message_deleted), Toast.LENGTH_SHORT).show();
        }
        if (type.equals("me")) {
            if (messagesList.isEmpty()) {
                dbhelper.deleteRecentChat(groupId);
            } else {
                GroupMessage data = messagesList.get(0);
                dbhelper.addGroupRecentMsgs(groupId, data.messageId, GetSet.getUserId(), data.chatTime, "0");
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
        ApplicationClass.resumeExternalAudio(this);
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

    private void setSectionMessage(Context mContext, GroupMessage message, TextView timeText) {
        timeText.setText(message.message);
        if (message.messageType.equalsIgnoreCase("change_number")) {

            timeText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    HashMap<String, String> map = ApplicationClass.getContactrNot(getApplicationContext(), message.contactPhoneNo);
                    if (map.get("isAlready").equals("false")) {
                        showAlertDialog(message);
                    } else {
                        makeToast(getString(R.string.contact_already_exists));
                    }
                }
            });
        } else {
            timeText.setOnClickListener(null);
        }
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

                    for (GroupMessage messagesData : selectedChatPos) {
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

    public void showAlertDialog(GroupMessage message) {
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
        title.setText(R.string.do_you_want_to_add_contact);
        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
                intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, message.contactPhoneNo);
                intent.putExtra(ContactsContract.Intents.Insert.NAME, message.contactName);
                startActivity(intent);
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

    private boolean isForwardable(GroupMessage mData) {
        if (mData.messageType.equals(Constants.TAG_ISDELETE)) {
            return false;
        } else if ((mData.messageType.equals("video") || mData.messageType.equals("document") ||
                mData.messageType.equals("audio"))) {
            if (mData.memberId.equals(GetSet.getUserId()) && !mData.progress.equals("completed")) {
                return false;
            } else
                return mData.memberId.equals(GetSet.getUserId()) || storageManager.checkifFileExists(mData.attachment, mData.messageType, "receive");
        } else if (mData.messageType.equals("image") && !mData.progress.equals("completed")) {
            if (mData.memberId.equals(GetSet.getUserId()) && !mData.progress.equals("completed")) {
                return false;
            } else
                return mData.memberId.equals(GetSet.getUserId()) || storageManager.checkifImageExists("receive", mData.attachment);
        } else {
            return true;
        }
    }

    private void emitImage(GroupMessage mdata, String imageUrl) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.TAG_GROUP_ID, groupId);
            jsonObject.put(Constants.TAG_GROUP_NAME, groupName);
            jsonObject.put(Constants.TAG_CHAT_TYPE, TAG_GROUP);
            jsonObject.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
            jsonObject.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
            jsonObject.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
            jsonObject.put(Constants.TAG_MESSAGE_ID, mdata.messageId);
            jsonObject.put(Constants.TAG_MESSAGE_TYPE, mdata.messageType);
            jsonObject.put(Constants.TAG_MESSAGE, mdata.message);
            jsonObject.put(Constants.TAG_ATTACHMENT, imageUrl);
            jsonObject.put(Constants.TAG_CHAT_TIME, mdata.chatTime);

            socketConnection.startGroupChat(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void emitLocation(String type, String lat, String lon) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        RandomString randomString = new RandomString(10);
        String messageId = groupId + randomString.nextString();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.TAG_GROUP_ID, groupId);
            jsonObject.put(Constants.TAG_GROUP_NAME, groupName);
            jsonObject.put(Constants.TAG_CHAT_TYPE, TAG_GROUP);
            jsonObject.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
            jsonObject.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
            jsonObject.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
            jsonObject.put(Constants.TAG_MESSAGE_TYPE, type);
            jsonObject.put(Constants.TAG_MESSAGE, getString(R.string.location));
            jsonObject.put(Constants.TAG_LAT, lat);
            jsonObject.put(Constants.TAG_LON, lon);
            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
            socketConnection.startGroupChat(jsonObject);

            dbhelper.addGroupMessages(messageId, groupId, GetSet.getUserId(), "", type,
                    getString(R.string.location), "", lat, lon,
                    "", "", "",
                    unixStamp, "", "read");

            dbhelper.addGroupRecentMsgs(groupId, messageId, GetSet.getUserId(), unixStamp, "0");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        GroupMessage data = new GroupMessage();
        data.messageId = messageId;
        data.groupId = groupId;
        data.memberId = GetSet.getUserId();
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

    public String firstThree(String str) {
        return str.length() < 3 ? str : str.substring(0, 3);
    }

    private void emitContact(String type, String name, String phone, String countrycode) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        RandomString randomString = new RandomString(10);
        String messageId = groupId + randomString.nextString();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.TAG_GROUP_ID, groupId);
            jsonObject.put(Constants.TAG_GROUP_NAME, groupName);
            jsonObject.put(Constants.TAG_CHAT_TYPE, TAG_GROUP);
            jsonObject.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
            jsonObject.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
            jsonObject.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
            jsonObject.put(Constants.TAG_MESSAGE_TYPE, type);
            jsonObject.put(Constants.TAG_MESSAGE, getString(R.string.contact));
            jsonObject.put(Constants.TAG_CONTACT_NAME, name);
            jsonObject.put(Constants.TAG_CONTACT_PHONE_NO, phone);
            jsonObject.put(Constants.TAG_CONTACT_COUNTRY_CODE, countrycode);
            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);

            socketConnection.startGroupChat(jsonObject);

            dbhelper.addGroupMessages(messageId, groupId, GetSet.getUserId(), "", type,
                    getString(R.string.contact), "", "", "", name, phone, countrycode,
                    unixStamp, "", "read");

            dbhelper.addGroupRecentMsgs(groupId, messageId, GetSet.getUserId(), unixStamp, "0");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        GroupMessage data = new GroupMessage();
        data.memberId = GetSet.getUserId();
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
                dbhelper.deleteGroupMessages(groupId);
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

    private void openDeleteDialog() {
        boolean canEveryOneVisible = true;
        Dialog deleteDialog = new Dialog(GroupChatActivity.this);
        deleteDialog.setCancelable(true);
        if (deleteDialog.getWindow() != null) {
            deleteDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        deleteDialog.setContentView(R.layout.dialog_report);

        RecyclerView deleteRecyclerView = deleteDialog.findViewById(R.id.reportRecyclerView);

        List<String> deleteTexts = new ArrayList<>();

        for (GroupMessage message : selectedChatPos) {
            if (ApplicationClass.isExceedsOneHour(message.chatTime) ||
                    !message.memberId.equalsIgnoreCase(GetSet.getUserId()) ||
                    message.messageType.equalsIgnoreCase(Constants.TAG_ISDELETE)) {
                canEveryOneVisible = false;
                break;
            }
        }

        deleteTexts.add(getString(R.string.delete_for_me));
        deleteTexts.add(getString(R.string.cancel));

        if (canEveryOneVisible) {
            deleteTexts.add(getString(R.string.delete_for_everyone));
            LinearLayoutManager layoutManager = new LinearLayoutManager(GroupChatActivity.this, RecyclerView.VERTICAL, false);
            deleteRecyclerView.setLayoutManager(layoutManager);
        } else {
            GridLayoutManager layoutManager = new GridLayoutManager(GroupChatActivity.this, 2);
            deleteRecyclerView.setLayoutManager(layoutManager);
        }
        DeleteAdapter adapter = new DeleteAdapter(deleteTexts, deleteDialog, GroupChatActivity.this);
        deleteRecyclerView.setAdapter(adapter);

        deleteDialog.show();
    }

    private void deleteMessageConfirmDialog(GroupMessage mData) {
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
                dbhelper.deleteGroupMessageFromId(mData.messageId);
                messagesList.remove(mData);
                Toast.makeText(GroupChatActivity.this, getString(R.string.message_deleted), Toast.LENGTH_SHORT).show();
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

                if (dbhelper.isGroupHaveAdmin(groupId) == 1) {
                    GroupData.GroupMembers memberData = dbhelper.getAdminFromMembers(groupId);
                    if (memberData.memberId.equalsIgnoreCase(GetSet.getUserId())) {
                        Log.v(TAG, "No admin");
                        for (GroupData.GroupMembers members : dbhelper.getGroupMembers(getApplicationContext(), groupId)) {
                            if (!members.memberId.equals(GetSet.getUserId())) {
                                JSONArray jsonArray = new JSONArray();
                                try {
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put(TAG_MEMBER_ID, members.memberId);
                                    jsonObject.put(TAG_MEMBER_ROLE, TAG_ADMIN);
                                    jsonArray.put(jsonObject);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                try {
                                    String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                                    RandomString randomString = new RandomString(10);
                                    String messageId = groupId + randomString.nextString();

                                    JSONObject message = new JSONObject();
                                    message.put(Constants.TAG_GROUP_ID, groupId);
                                    message.put(Constants.TAG_GROUP_NAME, groupName);
                                    message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
                                    message.put(Constants.TAG_CHAT_TIME, unixStamp);
                                    message.put(Constants.TAG_MESSAGE_ID, messageId);
                                    message.put(Constants.TAG_ATTACHMENT, "1");
                                    message.put(Constants.TAG_MEMBER_ID, members.memberId);
                                    message.put(Constants.TAG_MEMBER_NAME, "");
                                    message.put(Constants.TAG_MEMBER_NO, members.memberNo);
                                    message.put(Constants.TAG_MESSAGE_TYPE, "admin");
                                    message.put(Constants.TAG_MESSAGE, getString(R.string.admin));
                                    message.put(Constants.TAG_GROUP_ADMIN_ID, GetSet.getUserId());
                                    socketConnection.startGroupChat(message);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                updateGroupData(jsonArray);
                                break;
                            }
                        }
                    }
                }

                try {
                    String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                    RandomString randomString = new RandomString(10);
                    String messageId = groupId + randomString.nextString();

                    JSONObject message = new JSONObject();
                    message.put(Constants.TAG_GROUP_ID, groupId);
                    message.put(Constants.TAG_GROUP_NAME, groupName);
                    message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
                    message.put(Constants.TAG_CHAT_TIME, unixStamp);
                    message.put(Constants.TAG_MESSAGE_ID, messageId);
                    message.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                    message.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                    message.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                    message.put(Constants.TAG_MESSAGE_TYPE, getString(R.string.left));
                    message.put(Constants.TAG_MESSAGE, getString(R.string.one_participant_left));
                    message.put(Constants.TAG_GROUP_ADMIN_ID, groupData.groupAdminId);
                    socketConnection.startGroupChat(message);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(TAG_GROUP_ID, groupId);
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

    private void uploadImage(byte[] imageBytes, final String imagePath, final GroupMessage mdata, final String filePath) {
        RequestBody requestFile = RequestBody.create(imageBytes, MediaType.parse("openImage/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("group_attachment", "openImage.jpg", requestFile);

        RequestBody userid = RequestBody.create(GetSet.getUserId(), MediaType.parse("multipart/form-data"));
        Call<HashMap<String, String>> call3 = apiInterface.uploadGroupChat(GetSet.getToken(), body, userid);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> data = response.body();
                Log.v(TAG, "uploadImageresponse=" + data);
                if (data.get(Constants.TAG_STATUS).equals(TRUE)) {
                    File dir = new File(imagePath);
                    if (dir.exists()) {
                        if (mdata.messageType.equals("image")) {
                            dbhelper.updateGroupMessageData(mdata.messageId, Constants.TAG_PROGRESS, "completed");
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

                            Intent service = new Intent(GroupChatActivity.this, FileUploadService.class);
                            Bundle b = new Bundle();
                            b.putSerializable("mdata", mdata);
                            b.putString("filepath", filePath);
                            b.putSerializable(Constants.TAG_THUMBNAIL, data.get(Constants.TAG_USER_IMAGE));
                            b.putString("chatType", "group");
                            service.putExtras(b);
                            startService(service);
                        }

                    }
                } else {
                    dbhelper.updateGroupMessageData(mdata.messageId, Constants.TAG_PROGRESS, "error");
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
                Log.v(TAG, "onFailure=" + "onFailure");
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

    private GroupMessage updateDBList(String type, String imagePath, String filePath) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        RandomString randomString = new RandomString(10);
        String messageId = groupId + randomString.nextString();

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

        GroupMessage groupMessage = new GroupMessage();
        groupMessage.groupId = groupId;
        groupMessage.groupName = groupName;
        groupMessage.memberId = GetSet.getUserId();
        groupMessage.memberName = GetSet.getUserName();
        groupMessage.memberNo = GetSet.getphonenumber();
        groupMessage.messageType = type;
        groupMessage.message = msg;
        groupMessage.messageId = messageId;
        groupMessage.chatTime = unixStamp;
        groupMessage.deliveryStatus = "";
        groupMessage.progress = "";

        if (type.equals("video")) {
            groupMessage.thumbnail = imagePath;
            groupMessage.attachment = filePath;
            dbhelper.addGroupMessages(messageId, groupId, GetSet.getUserId(), "",
                    type, ApplicationClass.encryptMessage(msg), ApplicationClass.encryptMessage(filePath), "", "", "", "",
                    "", unixStamp, ApplicationClass.encryptMessage(imagePath), "read");
        } else if (type.equals("image")) {
            groupMessage.thumbnail = "";
            groupMessage.attachment = imagePath;
            dbhelper.addGroupMessages(messageId, groupId, GetSet.getUserId(), "",
                    type, ApplicationClass.encryptMessage(msg), ApplicationClass.encryptMessage(imagePath), "", "", "", "",
                    "", unixStamp, ApplicationClass.encryptMessage(imagePath), "read");
        } else {
            groupMessage.thumbnail = "";
            groupMessage.attachment = filePath;
            dbhelper.addGroupMessages(messageId, groupId, GetSet.getUserId(), "",
                    type, ApplicationClass.encryptMessage(msg), ApplicationClass.encryptMessage(filePath), "", "", "", "",
                    "", unixStamp, "", "read");
        }

        dbhelper.addGroupRecentMsgs(groupId, messageId, GetSet.getUserId(), unixStamp, "0");

        messagesList.add(0, groupMessage);
        updatePosition();
        showEncryptionText();
        messageListAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);

        return groupMessage;
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
            int permissionCamera = ContextCompat.checkSelfPermission(GroupChatActivity.this,
                    CAMERA);
            int permissionAudio = ContextCompat.checkSelfPermission(GroupChatActivity.this,
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
            int permissionCamera = ContextCompat.checkSelfPermission(GroupChatActivity.this,
                    CAMERA);
            int permissionAudio = ContextCompat.checkSelfPermission(GroupChatActivity.this,
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
            int permissionStorage = ContextCompat.checkSelfPermission(GroupChatActivity.this, WRITE_EXTERNAL_STORAGE);

            if (permissionStorage == PackageManager.PERMISSION_GRANTED) {
                ImagePicker.pickImage(this, getString(R.string.select_your_image));
            }
        } else if (requestCode == 106) {
            int permissionCamera = ContextCompat.checkSelfPermission(GroupChatActivity.this,
                    CAMERA);
            int permissionStorage = ContextCompat.checkSelfPermission(GroupChatActivity.this,
                    WRITE_EXTERNAL_STORAGE);

            if (permissionCamera == PackageManager.PERMISSION_GRANTED &&
                    permissionStorage == PackageManager.PERMISSION_GRANTED) {
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    ApplicationClass.onShareExternal = true;
                    ImagePicker.pickImageCameraOnly(this, 234);
                }
            }
        } else if (requestCode == 107) {
            int permissionCamera = ContextCompat.checkSelfPermission(GroupChatActivity.this,
                    CAMERA);
            int permissionStorage = ContextCompat.checkSelfPermission(GroupChatActivity.this,
                    WRITE_EXTERNAL_STORAGE);

            if (permissionCamera == PackageManager.PERMISSION_GRANTED &&
                    permissionStorage == PackageManager.PERMISSION_GRANTED) {
                if (isNetworkConnected().equals(NOT_CONNECT)) {
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
            } else {
                makeToast(getString(R.string.storage_permission_error));
            }
        } else if (requestCode == 108) {
            int permissionStorage = ContextCompat.checkSelfPermission(GroupChatActivity.this,
                    WRITE_EXTERNAL_STORAGE);

            if (permissionStorage == PackageManager.PERMISSION_GRANTED) {
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
            } else {
                makeToast(getString(R.string.storage_permission_error));
            }
        } else if (requestCode == 109) {
            int permissionStorage = ContextCompat.checkSelfPermission(GroupChatActivity.this,
                    WRITE_EXTERNAL_STORAGE);

            if (permissionStorage == PackageManager.PERMISSION_GRANTED) {
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
            } else {
                makeToast(getString(R.string.storage_permission_error));
            }
        } else if (requestCode == 110) {
            int permissionContacts = ContextCompat.checkSelfPermission(GroupChatActivity.this,
                    READ_CONTACTS);

            if (permissionContacts == PackageManager.PERMISSION_GRANTED) {
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    ApplicationClass.onShareExternal = true;
                    Intent intentc = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                    startActivityForResult(intentc, 13);
                }
            } else {
                makeToast(getString(R.string.storage_permission_error));
            }
        } else if (requestCode == 111) {

            int permissionAudio = ContextCompat.checkSelfPermission(GroupChatActivity.this,
                    RECORD_AUDIO);
            int permissionStorage = ContextCompat.checkSelfPermission(GroupChatActivity.this,
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
                    ImageCompression imageCompression = new ImageCompression(GroupChatActivity.this) {
                        @Override
                        protected void onPostExecute(String imagePath) {
                            try {
                                GroupMessage mdata = updateDBList("image", imagePath, "");
                                byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(imagePath));
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
                                    GroupMessage mdata = updateDBList("video", imagePath, filepath);
                                    byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(imagePath));
                                    uploadImage(bytes, imagePath, mdata, filepath);
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        ImageCompression imageCompression = new ImageCompression(GroupChatActivity.this) {
                            @Override
                            protected void onPostExecute(String imagePath) {
                                try {
                                    Log.v("checkChat", "imagepath=" + imagePath);
                                    GroupMessage mdata = updateDBList("image", imagePath, "");
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
                        GroupMessage mdata = updateDBList("document", "", filepath);
                        Intent service = new Intent(GroupChatActivity.this, FileUploadService.class);
                        Bundle b = new Bundle();
                        b.putSerializable("mdata", mdata);
                        b.putString("filepath", filepath);
                        b.putString("chatType", "group");
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
                        GroupMessage mdata = updateDBList("audio", "", filepath);
                        Intent service = new Intent(GroupChatActivity.this, FileUploadService.class);
                        Bundle b = new Bundle();
                        b.putSerializable("mdata", mdata);
                        b.putString("filepath", filepath);
                        b.putString("chatType", "group");
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
            username.setText(groupName);
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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                voiceFab.setRippleColor(color.toConversationColor(GroupChatActivity.this));
                voiceFab.setBackgroundTintList(new ColorStateList(new int[][]{
                        new int[]{android.R.attr.state_pressed},
                        new int[]{color.toConversationPColor(GroupChatActivity.this)},


                }, new int[]{
                        color.toConversationPColor(GroupChatActivity.this),
                        color.toConversationColor(GroupChatActivity.this),
                }));
            }
        },600);
        Speech.init(this, getPackageName());
        initTextToSpeech();
        if (getIntent().getStringExtra(TAG_GROUP_ID) != null)
            groupId = getIntent().getStringExtra(TAG_GROUP_ID);
        tempGroupId = groupId;
        if (dbhelper.getGroupData(this, groupId) != null) {
            groupData = dbhelper.getGroupData(this, groupId);
            groupName = groupData.groupName;
            username.setText(groupData.groupName);
            Glide.with(GroupChatActivity.this).load(Constants.GROUP_IMG_PATH + groupData.groupImage)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.create_group).error(R.drawable.create_group))
                    .into(userimage);
            setGroupMembers(groupId);
        } else {
            finish();
        }
        ApplicationClass.onShareExternal = false;
    }

    @Override
    public void onPause() {
        tempGroupId = "";
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
    protected void onDestroy() {
        super.onDestroy();
        Log.v("onDestroy", "onDestroy");
        if (Constants.isGroupChatOpened) {
            Constants.isGroupChatOpened = false;
        }
        SocketConnection.getInstance(this).setGroupChatCallbackListener(null);
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
        if (selectedChatPos.size() > 0) {
            selectedChatPos.clear();
            messageListAdapter.notifyDataSetChanged();
            chatUserLay.setVisibility(View.VISIBLE);
            forwordLay.setVisibility(View.GONE);
            chatLongPressed = false;
        } else {
            if (isFromNotification) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
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
                } else if (dbhelper.isMemberExist(GetSet.getUserId(), groupId)) {
                    if (editText.getText().toString().trim().length() > 0) {
                        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                        String textMsg = ApplicationClass.encryptMessage(editText.getText().toString().trim());
                        RandomString randomString = new RandomString(10);
                        String messageId = groupId + randomString.nextString();
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(Constants.TAG_GROUP_ID, groupId);
                            jsonObject.put(Constants.TAG_GROUP_NAME, groupName);
                            jsonObject.put(Constants.TAG_CHAT_TYPE, TAG_GROUP);
                            jsonObject.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                            jsonObject.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                            jsonObject.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
                            jsonObject.put(Constants.TAG_MESSAGE_TYPE, "text");
                            jsonObject.put(Constants.TAG_MESSAGE, textMsg);
                            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);

                            socketConnection.startGroupChat(jsonObject);

                            dbhelper.addGroupMessages(messageId, groupId, GetSet.getUserId(), "", "text",
                                    textMsg, "", "", "", "", "",
                                    "", unixStamp, "", "read");

                            dbhelper.addGroupRecentMsgs(groupId, messageId, GetSet.getUserId(), unixStamp, "0");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        GroupMessage groupMessage = new GroupMessage();
                        groupMessage.memberId = GetSet.getUserId();
                        groupMessage.messageType = "text";
                        groupMessage.message = ApplicationClass.decryptMessage(textMsg);
                        groupMessage.messageId = messageId;
                        groupMessage.chatTime = unixStamp;
                        groupMessage.deliveryStatus = "";
                        messagesList.add(0, groupMessage);
                        updatePosition();
                        showEncryptionText();
                        messageListAdapter.notifyItemInserted(0);
                        recyclerView.smoothScrollToPosition(0);
                        editText.setText("");
                    } else {
                        editText.setError(getString(R.string.please_enter_your_message));
                    }
                } else {
                    makeToast(getString(R.string.you_are_no_longer_member_in_this_group));
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
                GroupData results = dbhelper.getGroupData(GroupChatActivity.this, groupId);

                if (dbhelper.isMemberExist(GetSet.getUserId(), groupId)) {
                    if (results.muteNotification.equals("true")) {
                        values.add(getString(R.string.unmute_notification));
                    } else {
                        values.add(getString(R.string.mute_notification));
                    }
                    values.add(getString(R.string.clear_chat));
                    values.add(getString(R.string.exit_group));
                } else {
                    values.add(getString(R.string.delete_group));
                }

                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        R.layout.option_item, android.R.id.text1, values);
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = layoutInflater.inflate(R.layout.option_layout, null);
                final PopupWindow popup = new PopupWindow(GroupChatActivity.this);
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
                        if (position == 0) {
                            if (dbhelper.isMemberExist(GetSet.getUserId(), groupId)) {
                                if (isNetworkConnected().equals(NOT_CONNECT)) {
                                    networkSnack();
                                } else {
                                    if (values.get(position).equalsIgnoreCase(getString(R.string.mute_notification))) {
                                        dbhelper.updateMuteGroup(groupId, "true");
                                        values.set(position, getString(R.string.unmute_notification));
                                    } else {
                                        dbhelper.updateMuteGroup(groupId, "");
                                        values.set(position, getString(R.string.mute_notification));
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                            } else {
                                dbhelper.deleteMembers(groupId);
                                dbhelper.deleteGroupMessages(groupId);
                                dbhelper.deleteGroup(groupId);
                                finish();
                            }
                        } else if (position == 1) {
                            deleteChatConfirmDialog();
                        } else if (position == 2) {
                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                networkSnack();
                            } else {
                                exitConfirmDialog();
                            }
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
                if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, CAMERA}, 106);
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
                if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, CAMERA}, 107);
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
                if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 108);
                } else {
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
                    } else {
                        ApplicationClass.onShareExternal = true;
                        Intent intentc = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                        startActivityForResult(intentc, 13);
                    }
                }
                break;
            case R.id.chatUserLay:
                ApplicationClass.preventMultiClick(chatUserLay);
                stopMedia();
                Intent profile = new Intent(GroupChatActivity.this, GroupInfoActivity.class);
                profile.putExtra(Constants.TAG_GROUP_ID, groupId);
                startActivity(profile);
                break;
            case R.id.forwordBtn:
                ApplicationClass.preventMultiClick(forwordBtn);
                stopMedia();
                Intent f = new Intent(GroupChatActivity.this, ForwardActivity.class);
                f.putExtra("from", "group");
                f.putExtra("id", groupId);
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
            case R.id.closeBtn:
                onBackPressed();
                break;
            case R.id.deleteBtn:
                ApplicationClass.preventMultiClick(deleteBtn);
                openDeleteDialog();
                //deleteMessageConfirmDialog(selectedChatPos.get(0));
                break;
        }
    }

    private boolean isAdmin() {
        boolean isAdmin = false;
        for (GroupData.GroupMembers members : groupData.groupMembers) {
            if (members.memberId.equalsIgnoreCase(GetSet.getUserId())) {
                isAdmin = members.memberRole.equalsIgnoreCase(TAG_ADMIN);
                break;
            }
        }
        return isAdmin;
    }

    private void updatePosition() {
        /*if(player != null){
            if(player.isPlaying()){
                playingPosition = playingPosition+1;
            }
        }*/
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
        ttobj = new TextToSpeech(GroupChatActivity.this, new TextToSpeech.OnInitListener() {
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
            if (ContextCompat.checkSelfPermission(GroupChatActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

                onRecordAudioPermissionGranted();

            } else {
                ActivityCompat.requestPermissions(GroupChatActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST);
            }

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
        private List<GroupMessage> mMessageList;

        public MessageListAdapter(Context context, List<GroupMessage> messageList) {
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
            final GroupMessage message = mMessageList.get(position);

            if (message.memberId != null && message.memberId.equals(GetSet.getUserId())) {
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
                    case "create_group":
                    case "add_member":
                    case "group_image":
                    case "subject":
                    case "left":
                    case "remove_member":
                    case "admin":
                    case "change_number":
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
                    case "create_group":
                    case "add_member":
                    case "group_image":
                    case "subject":
                    case "left":
                    case "remove_member":
                    case "admin":
                    case "change_number":
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
            final GroupMessage message = mMessageList.get(position);
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
                    ApplicationClass.pauseExternalAudio(context);
                    player.setDataSource(getApplicationContext(), voiceURI);
                    player.prepare();
                    player.start();
                } catch (Exception e) {
                    ApplicationClass.resumeExternalAudio(context);
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
                    play.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.play_icon_white));
                    player.start();
                    ApplicationClass.pauseExternalAudio(context);
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

        private class SentMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText;

            SentMessageHolder(View itemView) {
                super(itemView);

                messageText = itemView.findViewById(R.id.text_message_body);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(final GroupMessage message) {
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

            void bind(GroupMessage message) {
                nameText.setVisibility(View.VISIBLE);
                nameText.setText(ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(message.memberId), dbhelper.getContactCountryCode(message.memberId),dbhelper.getContactPhone(message.memberId)));
                messageText.setText(message.message
                        + Html.fromHtml(" &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"));
                Linkify.addLinks(messageText, Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS);
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

            void bind(final GroupMessage message) {
                Log.i(TAG, "bind: " + new Gson().toJson(message));
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
                                File file = storageManager.getImage("sent", getFileName(message.attachment));
                                if (file != null) {
                                    Log.v(TAG, "checkChat=" + file.getAbsolutePath());
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
                                    if (message.progress.equals("error")) {
                                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                                            networkSnack();
                                        } else {
                                            try {
                                                progressbar.setVisibility(View.VISIBLE);
                                                progressbar.spin();
                                                dbhelper.updateGroupMessageData(message.messageId, Constants.TAG_PROGRESS, "");
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
                                                ApplicationClass.openImage(mContext, file.getAbsolutePath(), Constants.TAG_MESSAGE, imageView);
                                            }
                                        } else {
                                            Toast.makeText(GroupChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
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
                                    Intent i = new Intent(GroupChatActivity.this, LocationActivity.class);
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
                                                    dbhelper.updateGroupMessageData(message.messageId, Constants.TAG_PROGRESS, "");
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
                                                Toast.makeText(GroupChatActivity.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
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
                                                Toast.makeText(GroupChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                                e.printStackTrace();
                                            }
                                        } else {
                                            Toast.makeText(GroupChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
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

            void bind(final GroupMessage message) {
                nameText.setVisibility(View.VISIBLE);
                nameText.setText(ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(message.memberId), dbhelper.getContactCountryCode(message.memberId),dbhelper.getContactPhone(message.memberId)));
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                switch (message.messageType) {
                    case "image":
                        videoprogresslay.setVisibility(View.GONE);
                        downloadicon.setImageResource(R.drawable.download);
                        if (storageManager.checkifImageExists("thumb", message.attachment)) {
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
                            Glide.with(mContext).load(Constants.GROUP_IMG_PATH + message.attachment).thumbnail(0.5f)
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
                                            Log.v(TAG, "file=" + file.getAbsolutePath());
                                            ApplicationClass.openImage(mContext, file.getAbsolutePath(), Constants.TAG_MESSAGE, imageView);
                                        }
                                    } else {
                                        if (ContextCompat.checkSelfPermission(GroupChatActivity.this, WRITE_EXTERNAL_STORAGE)
                                                != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(GroupChatActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, 100);
                                        } else {
                                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                                networkSnack();
                                            } else {
                                                ImageDownloader imageDownloader = new ImageDownloader(GroupChatActivity.this) {
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
                                                imageDownloader.execute(Constants.GROUP_IMG_PATH + message.attachment, "receive");
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
                                    Intent i = new Intent(GroupChatActivity.this, LocationActivity.class);
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
                            Glide.with(mContext).load(Constants.GROUP_IMG_PATH + message.thumbnail).thumbnail(0.5f)
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
                                            Toast.makeText(GroupChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                            e.printStackTrace();
                                        }
                                    } else {
                                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                                            networkSnack();
                                        } else {
                                            ImageDownloader imageDownloader = new ImageDownloader(GroupChatActivity.this) {
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

                                                                DownloadFiles downloadFiles = new DownloadFiles(GroupChatActivity.this) {
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
                                                                downloadFiles.execute(Constants.GROUP_IMG_PATH + message.attachment, message.messageType);
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
                                            imageDownloader.execute(Constants.GROUP_IMG_PATH + message.thumbnail, "thumb");
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

            void bind(final GroupMessage message) {
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
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
                                        filename.setText(getString(R.string.uploading));
                                        dbhelper.updateGroupMessageData(message.messageId, Constants.TAG_PROGRESS, "");
                                        message.progress = "";
                                        Intent service = new Intent(GroupChatActivity.this, FileUploadService.class);
                                        Bundle b = new Bundle();
                                        b.putSerializable("mdata", message);
                                        b.putString("filepath", message.attachment);
                                        b.putString("chatType", "group");
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
                                        Toast.makeText(GroupChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                } else {
                                    Toast.makeText(GroupChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
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

            void bind(final GroupMessage message) {
                filename.setText(message.message);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                nameText.setVisibility(View.VISIBLE);
                nameText.setText(ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(message.memberId), dbhelper.getContactCountryCode(message.memberId),dbhelper.getContactPhone(message.memberId)));
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
                                    intent.setAction(android.content.Intent.ACTION_VIEW);
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
                                    Toast.makeText(GroupChatActivity.this, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            } else {

                                DownloadFiles downloadFiles = new DownloadFiles(GroupChatActivity.this) {
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
                                downloadFiles.execute(Constants.GROUP_IMG_PATH + message.attachment, message.messageType);
                                progressbar.setVisibility(View.VISIBLE);
                                progressbar.spin();
                                downloadicon.setVisibility(View.VISIBLE);
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

            void bind(GroupMessage message) {
                username.setText(message.contactName);
                phoneno.setText(message.contactPhoneNo);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
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

            void bind(final GroupMessage message) {
                username.setText(message.contactName);
                phoneno.setText(message.contactPhoneNo);
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                nameText.setVisibility(View.VISIBLE);
                nameText.setText(ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(message.memberId), dbhelper.getContactCountryCode(message.memberId),dbhelper.getContactPhone(message.memberId)));
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

            void bind(final GroupMessage message) {
                setSectionMessage(mContext, message, timeText);
            }
        }

        private class DeleteMsgSent extends RecyclerView.ViewHolder {
            TextView timeText;

            DeleteMsgSent(View itemView) {
                super(itemView);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(final GroupMessage message) {
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
            }
        }

        private class DeleteMsgReceived extends RecyclerView.ViewHolder {
            TextView timeText, nameText;

            DeleteMsgReceived(View itemView) {
                super(itemView);
                timeText = itemView.findViewById(R.id.text_message_time);
                nameText = itemView.findViewById(R.id.text_message_sender);
            }

            void bind(final GroupMessage message) {
                if (selectedChatPos.contains(message)) {
                    itemView.setSelected(true);
                } else {
                    itemView.setSelected(false);
                }
                timeText.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));
                nameText.setVisibility(View.VISIBLE);
                nameText.setText(ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(message.memberId), dbhelper.getContactCountryCode(message.memberId),dbhelper.getContactPhone(message.memberId)));
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

            void bind(final GroupMessage message, int pos) {
                itemView.setTag("pos" + pos);
                seekbar.setTag("tag" + pos);
                msg_time.setText(ApplicationClass.getTime(Long.parseLong(message.chatTime)));

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
                if (storageManager.checkifFileExists(message.attachment, message.messageType, "sent")) {
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
                                        filename.setText(getString(R.string.uploading));
                                        dbhelper.updateMessageData(message.messageId, Constants.TAG_PROGRESS, "");
                                        message.progress = "";
                                        Intent service = new Intent(GroupChatActivity.this, FileUploadService.class);
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
                            if (storageManager.checkifFileExists(message.attachment, message.messageType, "sent")) {
                                playMedia(context, getAdapterPosition(), "sent");
                            } else {
                                Toast.makeText(GroupChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
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

            void bind(final GroupMessage message, int pos) {
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
                                    DownloadFiles downloadFiles = new DownloadFiles(GroupChatActivity.this) {
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
                                Toast.makeText(GroupChatActivity.this, getString(R.string.no_media), Toast.LENGTH_SHORT).show();
                            }
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
                indiaList.addItemDecoration(new DividerItemDecoration(GroupChatActivity.this, RecyclerView.VERTICAL));
                otherList.addItemDecoration(new DividerItemDecoration(GroupChatActivity.this, RecyclerView.VERTICAL));
                indiaList.setLayoutManager(new LinearLayoutManager(GroupChatActivity.this));
                otherList.setLayoutManager(new LinearLayoutManager(GroupChatActivity.this));
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
            Speech.getInstance().startListening(progress, GroupChatActivity.this);

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
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this);
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
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(GroupChatActivity.this);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this);
        builder.setMessage("speech not available")
                .setCancelable(false)
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
    }


}
