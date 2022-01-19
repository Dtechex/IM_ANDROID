package com.loopytime.im;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.loopytime.external.RandomString;
import com.loopytime.helper.FileUploadService;
import com.loopytime.helper.ImageCompression;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.StorageManager;
import com.loopytime.model.ChannelMessage;
import com.loopytime.model.ChannelResult;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupMessage;
import com.loopytime.model.MessagesData;
import com.loopytime.model.SearchData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.im.ForwardActivity.RecyclerViewAdapter.VIEW_TYPE_CHANNELS;
import static com.loopytime.im.ForwardActivity.RecyclerViewAdapter.VIEW_TYPE_CHANNEL_HEADER;
import static com.loopytime.im.ForwardActivity.RecyclerViewAdapter.VIEW_TYPE_CHATS;
import static com.loopytime.im.ForwardActivity.RecyclerViewAdapter.VIEW_TYPE_CHATS_HEADER;
import static com.loopytime.im.ForwardActivity.RecyclerViewAdapter.VIEW_TYPE_CONTACTS;
import static com.loopytime.im.ForwardActivity.RecyclerViewAdapter.VIEW_TYPE_CONTACTS_HEADER;
import static com.loopytime.im.ForwardActivity.RecyclerViewAdapter.VIEW_TYPE_GROUPS;
import static com.loopytime.im.ForwardActivity.RecyclerViewAdapter.VIEW_TYPE_GROUP_HEADER;
import static com.loopytime.utils.Constants.TAG_GROUP;

/**
 * Created by hitasoft on 9/8/18.
 */

public class ForwardActivity extends BaseActivity implements View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();
    TextView title;
    ImageView backbtn, searchbtn, optionbtn, cancelbtn;
    RecyclerView recyclerView;
    EditText searchView;
    RelativeLayout searchLay;
    RelativeLayout mainLay;
    LinearLayout buttonLayout, btnNext;
    int chatCount = 0, groupCount = 0, channelCount = 0;
    String from = "", id = "";
    LinearLayoutManager linearLayoutManager;
    RecyclerViewAdapter recyclerViewAdapter;
    List<SearchData> filteredList;
    List<SearchData> searchList = new ArrayList<>();
    List<SearchData> chatList = new ArrayList<>();
    List<SearchData> groupList = new ArrayList<>();
    List<SearchData> channelList = new ArrayList<>();
    List<SearchData> selectedList = new ArrayList<>();
    List<SearchData> contactsList = new ArrayList<>();
    List<MessagesData> multipleMediaList = new ArrayList<>();
    List<SearchData> searchedData = new ArrayList<>();
    SearchData sdata;
    StorageManager storageManager;
    ContactsData.Result results;
    MessagesData externalData;
    ApiInterface apiInterface;
    boolean isMultiple = false;
    ArrayList<Uri> externalImageUris = new ArrayList<>();
    Uri externalImageUri;
    int uriSize = -1;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forward_activity);

        title = findViewById(R.id.title);
        backbtn = findViewById(R.id.backbtn);
        searchbtn = findViewById(R.id.searchbtn);
        optionbtn = findViewById(R.id.optionbtn);
        recyclerView = findViewById(R.id.recyclerView);
        searchView = findViewById(R.id.searchView);
        buttonLayout = findViewById(R.id.buttonLayout);
        cancelbtn = findViewById(R.id.cancelbtn);
        searchLay = findViewById(R.id.searchLay);
        mainLay = findViewById(R.id.mainLay);
        btnNext = findViewById(R.id.btnNext);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);

        title.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        searchbtn.setVisibility(View.VISIBLE);
        optionbtn.setVisibility(View.GONE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        searchbtn.setOnClickListener(this);
        backbtn.setOnClickListener(this);
        cancelbtn.setOnClickListener(this);
        btnNext.setOnClickListener(this);

        if (ApplicationClass.isRTL()) {
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        storageManager = StorageManager.getInstance(this);
        title.setText(getString(R.string.forward_to));
        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        sdata = new SearchData();

        if (getIntent().getExtras().getString("from") != null) {
            from = getIntent().getExtras().getString("from");
            id = getIntent().getExtras().getString("id");
        } else {
            externalData = new MessagesData();
            from = "external";
            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();


            if (Intent.ACTION_SEND.equals(action) && type != null) {
                sdata.lat = "";
                sdata.lon = "";
                sdata.contact_name = "";
                sdata.contact_phone_no = "";
                sdata.contact_country_code = "";
                if ("text/plain".equals(type)) {
                    sdata.message_type = "text";
                    sdata.message = handleSendText(intent);
                    searchedData.add(sdata);
                } else if (type.startsWith("image/") || type.startsWith("video/")) {
                    handleSendSingleMedia(intent);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
                Log.d(TAG, "onCreate: " + type);
                isMultiple = true;
                uriSize = handleSendMultipleImages(intent);
            }

        }
        Log.i(TAG, "onCreate: " + from);
        switch (from) {
            case "chat": {
                List<MessagesData> messagesData = (List<MessagesData>) getIntent().getSerializableExtra("data");
                for (MessagesData mdata : messagesData) {
                    SearchData sdata = new SearchData();
                    sdata.user_id = mdata.user_id;
                    sdata.user_name = mdata.user_name;
                    sdata.chat_id = mdata.chat_id;
                    sdata.message_id = mdata.message_id;
                    if (ApplicationClass.isStringNotNull(mdata.message_type)) {
                        if (mdata.message_type.equals("story")) {
                            sdata.message_type = "text";
                        } else {
                            sdata.message_type = mdata.message_type;
                        }
                    }
                    sdata.message = mdata.message;
                    sdata.attachment = mdata.attachment;
                    sdata.lat = mdata.lat;
                    sdata.lon = mdata.lon;
                    sdata.contact_name = mdata.contact_name;
                    sdata.contact_phone_no = mdata.contact_phone_no;
                    sdata.contact_country_code = mdata.contact_country_code;
                    sdata.chatTime = mdata.chat_time;
                    sdata.receiver_id = mdata.receiver_id;
                    sdata.sender_id = mdata.sender_id;
                    sdata.delivery_status = mdata.delivery_status;
                    sdata.thumbnail = mdata.thumbnail;
                    sdata.progress = mdata.progress;
                    searchedData.add(sdata);
                }
                break;
            }
            case "group": {
                List<GroupMessage> groupMessages = (List<GroupMessage>) getIntent().getSerializableExtra("data");
                for (GroupMessage mdata : groupMessages) {
                    SearchData sdata = new SearchData();
                    sdata.groupId = mdata.groupId;
                    sdata.groupName = mdata.groupName;
                    sdata.groupImage = mdata.groupImage;
                    sdata.message_id = mdata.messageId;
                    sdata.message_type = mdata.messageType;
                    sdata.message = mdata.message;
                    sdata.groupAdminId = mdata.groupAdminId;
                    sdata.memberId = mdata.memberId;
                    sdata.memberName = mdata.memberName;
                    sdata.memberNo = mdata.memberNo;
                    sdata.attachment = mdata.attachment;
                    sdata.chatTime = mdata.chatTime;
                    sdata.contact_name = mdata.contactName;
                    sdata.contact_phone_no = mdata.contactPhoneNo;
                    sdata.contact_country_code = mdata.contactCountryCode;
                    sdata.lat = mdata.lat;
                    sdata.lon = mdata.lon;
                    sdata.delivery_status = mdata.deliveryStatus;
                    sdata.thumbnail = mdata.thumbnail;
                    sdata.progress = mdata.progress;
                    searchedData.add(sdata);
                }
                break;
            }
            case "channel": {
                List<ChannelMessage> channelMessages = (List<ChannelMessage>) getIntent().getSerializableExtra("data");
                for (ChannelMessage mdata : channelMessages) {
                    SearchData sdata = new SearchData();
                    sdata.channelId = mdata.channelId;
                    sdata.channelName = mdata.channelName;
                    sdata.channelAdminId = mdata.channelAdminId;
                    sdata.message_id = mdata.messageId;
                    sdata.message_type = mdata.messageType;
                    sdata.message = mdata.message;
                    sdata.attachment = mdata.attachment;
                    sdata.lat = mdata.lat;
                    sdata.lon = mdata.lon;
                    sdata.contact_name = mdata.contactName;
                    sdata.contact_phone_no = mdata.contactPhoneNo;
                    sdata.contact_country_code = mdata.contactCountryCode;
                    sdata.chatTime = mdata.chatTime;
                    sdata.delivery_status = mdata.deliveryStatus;
                    sdata.thumbnail = mdata.thumbnail;
                    sdata.progress = mdata.progress;
                    searchedData.add(sdata);
                }
                break;
            }
        }

        SearchData data = new SearchData();

        List<SearchData> tempChat = new ArrayList<>();
        for (HashMap<String, String> hashMap : dbhelper.getAllRecentsMessages(this)) {
            if (!hashMap.get(Constants.TAG_USER_ID).equals(GetSet.getUserId()) && !hashMap.get(Constants.TAG_USER_ID).equals(id)) {
                data = new SearchData();
                data.viewType = VIEW_TYPE_CHATS;
                data.user_id = hashMap.get(Constants.TAG_USER_ID);
                data.user_name = hashMap.get(Constants.TAG_USER_NAME);
                data.user_image = hashMap.get(Constants.TAG_USER_IMAGE);
                data.phone_no = hashMap.get(Constants.TAG_PHONE_NUMBER);
                data.blockedbyme = hashMap.get(Constants.TAG_BLOCKED_BYME);
                data.blockedme = hashMap.get(Constants.TAG_BLOCKED_ME);
                tempChat.add(data);
            }
        }
        if (tempChat.size() > 0) {
            data = new SearchData();
            data.viewType = VIEW_TYPE_CHATS_HEADER;
            tempChat.add(0, data);/*First item - Contact Header*/
        }
        searchList.addAll(tempChat);

        List<SearchData> tempGroup = new ArrayList<>();
        for (HashMap<String, String> hashMap : dbhelper.getGroupRecentMessages(this)) {
            if (!hashMap.get(Constants.TAG_GROUP_ID).equals(id) && dbhelper.isMemberExist(GetSet.getUserId(), hashMap.get(Constants.TAG_GROUP_ID))) {
                data = new SearchData();
                data.viewType = VIEW_TYPE_GROUPS;
                data.groupId = hashMap.get(Constants.TAG_GROUP_ID);
                data.groupName = hashMap.get(Constants.TAG_GROUP_NAME);
                data.groupImage = hashMap.get(Constants.TAG_GROUP_IMAGE);
                tempGroup.add(data);
            }
        }
        if (tempGroup.size() > 0) {
            data = new SearchData();
            data.viewType = VIEW_TYPE_GROUP_HEADER;
            tempGroup.add(0, data);/*First item - Group Header*/
        }
        searchList.addAll(tempGroup);

        List<SearchData> tempChannel = new ArrayList<>();
        for (ChannelResult.Result result : dbhelper.getMyChannels(GetSet.getUserId())) {
            if (!id.equals(result.channelId)) {
                data = new SearchData();
                data.viewType = VIEW_TYPE_CHANNELS;
                data.channelId = result.channelId;
                data.channelName = result.channelName;
                data.channelImage = result.channelImage;
                tempChannel.add(data);
            }
        }
        if (tempChannel.size() > 0) {
            data = new SearchData();
            data.viewType = VIEW_TYPE_CHANNEL_HEADER;
            tempChannel.add(0, data);/*First item - Group Header*/
        }
        searchList.addAll(tempChannel);

        List<SearchData> tempContacts = new ArrayList<>();
        for (ContactsData.Result result : dbhelper.getAllContacts(this)) {
            if (!result.user_id.equals(GetSet.getUserId()) && !isUserChatAlready(result.user_id, tempChat) && !result.user_id.equals(id)) {
                data = new SearchData();
                data.viewType = VIEW_TYPE_CONTACTS;
                data.user_id = result.user_id;
                data.user_name = result.user_name;
                data.user_image = result.user_image;
                data.phone_no = result.phone_no;
                data.blockedme = result.blockedme;
                data.blockedbyme = result.blockedbyme;
                tempContacts.add(data);
            }
        }
        if (tempContacts.size() > 0) {
            data = new SearchData();
            data.viewType = VIEW_TYPE_CONTACTS_HEADER;
            tempContacts.add(0, data);/*First item - Contact Header*/
        }
        searchList.addAll(tempContacts);

        filteredList = new ArrayList<>();
        filteredList.addAll(searchList);

        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        recyclerViewAdapter = new RecyclerViewAdapter(this);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerViewAdapter.notifyDataSetChanged();

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    cancelbtn.setVisibility(View.VISIBLE);
                } else {
                    cancelbtn.setVisibility(View.GONE);
                }
                recyclerViewAdapter.getFilter().filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    String handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            return sharedText;
        }
        return "";
    }

    void handleSendSingleMedia(Intent intent) {
        externalImageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
    }

    void imageUpload(String type, Uri imageUri) {
        if (type.equals("image")) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
            String filePath = storageManager.saveBitmapToExtFilesDir(bitmap, timestamp + ".jpg");
            if (filePath != null) {
                ImageCompression imageCompression = new ImageCompression(ForwardActivity.this) {
                    @Override
                    protected void onPostExecute(String imagePath) {
                        try {
                            MessagesData mdata = new MessagesData();
                            mdata = getExternalData(type, imagePath, "");
                            Log.d(TAG, "onPostExecute: " + new Gson().toJson(mdata));
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
                finish();
            }
        } else if (type.equals("video")) {
            try {
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = this.getContentResolver().query(imageUri, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String videoPath = cursor.getString(columnIndex);
                cursor.close();
                Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
                if (thumb != null) {
                    String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
                    String imageStatus = storageManager.saveToSdCard(thumb, "sent", timestamp + ".jpg");
                    if (imageStatus.equals("success")) {
                        File file = storageManager.getImage("sent", timestamp + ".jpg");
                        String thumbnailPath = file.getAbsolutePath();
                        MessagesData mdata = getExternalData("video", thumbnailPath, videoPath);
                        byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(thumbnailPath));
                        uploadImage(bytes, thumbnailPath, mdata, videoPath);
                    }
                }
                /*if(cursor!=null){
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(ForwardActivity.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                    finish();
                }*/
            } catch (IOException ex) {
                ex.printStackTrace();
                Toast.makeText(ForwardActivity.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
            }
        }
    }

    int handleSendMultipleImages(Intent intent) {
        externalImageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        return externalImageUris.size();
    }

    private void uploadImage(byte[] imageBytes, final String imagePath, MessagesData mdata, final String filePath) {
        RequestBody requestFile = RequestBody.create(imageBytes, MediaType.parse("openImage/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("attachment", "openImage.jpg", requestFile);

        RequestBody userid = RequestBody.create(GetSet.getUserId(), MediaType.parse("multipart/form-data"));
        Call<HashMap<String, String>> call3 = apiInterface.upmychat(GetSet.getToken(), body, userid);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> data = response.body();
                Log.v(TAG, "uploadImageResponse: " + data);
                Log.v(TAG, "uploadImageResponse: " + imagePath);
                if (data.get(Constants.TAG_STATUS).equals("true")) {
                    File dir = new File(imagePath);
                    if (dir.exists()) {
                        if (mdata.message_type.equals("image")) {
                            Log.i(TAG, "onResponse: " + mdata.message_id);
                            dbhelper.updateMessageData(mdata.message_id, Constants.TAG_PROGRESS, "completed");
//                            dbhelper.updateMessageData(mdata.message_id, Constants.TAG_ATTACHMENT, data.get(Constants.TAG_USER_IMAGE));
//                            dbhelper.updateMessageData(mdata.message_id, Constants.TAG_THUMBNAIL, data.get(Constants.TAG_USER_IMAGE));
                            if (isMultiple) {
                                mdata.attachment = data.get(Constants.TAG_USER_IMAGE);
                                mdata.thumbnail = data.get(Constants.TAG_USER_IMAGE);
                                storageManager.moveFilesToSentPath(getApplicationContext(), StorageManager.TAG_SENT, imagePath, data.get(Constants.TAG_USER_IMAGE));
                                storageManager.moveFilesToSentPath(getApplicationContext(), StorageManager.TAG_THUMB, imagePath, data.get(Constants.TAG_USER_IMAGE));
                                multipleMediaList.add(mdata);
                            } else {
                                sdata.message = mdata.message;
                                sdata.message_type = mdata.message_type;
                                sdata.message_id = mdata.message_id;
                                sdata.attachment = data.get(Constants.TAG_USER_IMAGE);
                                storageManager.moveFilesToSentPath(getApplicationContext(), StorageManager.TAG_SENT, imagePath, data.get(Constants.TAG_USER_IMAGE));
                                storageManager.moveFilesToSentPath(getApplicationContext(), StorageManager.TAG_THUMB, imagePath, data.get(Constants.TAG_USER_IMAGE));
                            }
                        } else if (mdata.message_type.equals("video")) {
                            storageManager.moveFilesToSentPath(getApplicationContext(), StorageManager.TAG_SENT, imagePath, data.get(Constants.TAG_USER_IMAGE));
                            dbhelper.updateMessageData(mdata.message_id, Constants.TAG_THUMBNAIL, data.get(Constants.TAG_USER_IMAGE));
                            mdata.thumbnail = data.get(Constants.TAG_USER_IMAGE);
                            if (isMultiple) {
                                multipleMediaList.add(mdata);
                            } else {
                                sdata.message = mdata.message;
                                sdata.message_id = mdata.message_id;
                                sdata.message_type = mdata.message_type;
                                sdata.thumbnail = data.get(Constants.TAG_USER_IMAGE);
                                sdata.attachment = mdata.attachment;
                            }

                            Intent service = new Intent(ForwardActivity.this, FileUploadService.class);
                            Bundle b = new Bundle();
                            b.putSerializable("mdata", mdata);
                            b.putString("filepath", filePath);
                            b.putSerializable(Constants.TAG_THUMBNAIL, data.get(Constants.TAG_USER_IMAGE));
                            b.putString("chatType", "chat");
                            service.putExtras(b);
                            startService(service);
                        }
                    }
                    if (sdata != null) {
                        searchedData.add(sdata);
                    }
                    if (isMultiple && multipleMediaList.size() == externalImageUris.size()) {
                        emitData();
                    } else if (isMultiple) {
                        shareMultiple(multipleMediaList.size());
                    } else {
                        emitData();
                    }
                } else {
                    dbhelper.updateMessageData(mdata.message_id, Constants.TAG_PROGRESS, "error");
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                Log.v(TAG, "onFailure=" + "onFailure");
                call.cancel();
                dbhelper.updateMessageData(mdata.message_id, Constants.TAG_PROGRESS, "error");
            }
        });
    }


    private String getFileName(String url) {
        String imgSplit = url;
        int endIndex = imgSplit.lastIndexOf("/");
        if (endIndex != -1) {
            imgSplit = imgSplit.substring(endIndex + 1);
        }
        return imgSplit;
    }

    private MessagesData getExternalData(String type, String imagePath, String filePath) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        RandomString randomString = new RandomString(10);
        String messageId = GetSet.getUserId() + randomString.nextString();

        String msg = "";
        switch (type) {
            case "image":
                msg = getString(R.string.image);
                break;
            case "audio":
                msg = getFileName(filePath);
                break;
            case "video":
                msg = getString(R.string.video);
                break;
            case "document":
                msg = getFileName(filePath);
                break;
        }

        MessagesData data = new MessagesData();
        data.user_id = GetSet.getUserId();
        data.message_type = type;
        data.message = msg;
        data.message_id = messageId;
        data.chat_time = unixStamp;
        data.delivery_status = "";
        data.progress = "";

        if ("image".equals(type)) {
            data.thumbnail = imagePath;
            data.attachment = imagePath;
        } else {
            String[] parts = filePath.split("/");
            String picturePath = parts[parts.length - 1];
            data.thumbnail = imagePath;
            data.attachment = picturePath;
        }
        Log.i(TAG, "getExternalData: " + data.message_id);
        return data;
    }


    private boolean isUserChatAlready(String user_id, List<SearchData> tempChat) {
        for (SearchData searchData : tempChat) {
            if (user_id.equals(searchData.user_id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backbtn:

                if (searchLay.getVisibility() == View.VISIBLE) {
                    searchView.setText("");
                    searchLay.setVisibility(View.GONE);
                    title.setVisibility(View.VISIBLE);
                    buttonLayout.setVisibility(View.VISIBLE);
                    ApplicationClass.hideSoftKeyboard(this, searchView);
                } else {
                    finish();
                }
                break;
            case R.id.searchbtn:
                title.setVisibility(View.GONE);
                searchLay.setVisibility(View.VISIBLE);
                buttonLayout.setVisibility(View.GONE);
                ApplicationClass.showKeyboard(this, searchView);
                break;
            case R.id.cancelbtn:
                searchView.setText("");
                break;
            case R.id.btnNext:
                btnNext.setEnabled(false);
                progressDialog.show();
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    if (from.equals("external")) {
                        if (isMultiple) {
                            if (!externalImageUris.isEmpty()) {
                                shareMultiple(0);
                            }
                        } else if (externalImageUri != null) {
                            if (getMimeType(externalImageUri).startsWith("video/")) {
                                imageUpload("video", externalImageUri);
                            } else {
                                imageUpload("image", externalImageUri);
                            }

                        } else {
                            emitData();
                        }
                    } else {
                        emitData();
                    }

                }
                break;
        }
    }

    private void shareMultiple(int i) {
        Uri uri = externalImageUris.get(i);
        if (getMimeType(uri).startsWith("video/")) {
            imageUpload("video", uri);
        } else {
            imageUpload("image", uri);
        }
    }

    private void emitData() {
        try {
            Toast.makeText(this, getString(R.string.sending_message), Toast.LENGTH_LONG).show();

            for (SearchData searchData : selectedList) {
                if (searchData.viewType == VIEW_TYPE_CHATS || searchData.viewType == VIEW_TYPE_CONTACTS) {
                    if (isMultiple) {
                        for (MessagesData data : multipleMediaList) {
                            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                            String chatId = GetSet.getUserId() + searchData.user_id;
                            RandomString randomString = new RandomString(10);
//                            String messageId = GetSet.getUserId() + randomString.nextString();


                            if (!searchData.blockedme.equals("block")) {
                                JSONObject jobj = new JSONObject();
                                JSONObject message = new JSONObject();
                                message.put(Constants.TAG_USER_ID, GetSet.getUserId());
                                message.put(Constants.TAG_USER_NAME, GetSet.getUserName());
                                message.put(Constants.TAG_MESSAGE_TYPE, data.message_type);
                                message.put(Constants.TAG_CHAT_TIME, unixStamp);
                                message.put(Constants.TAG_CHAT_ID, chatId);
                                message.put(Constants.TAG_MESSAGE_ID, data.message_id);
                                message.put(Constants.TAG_RECEIVER_ID, searchData.user_id);
                                message.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                                message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_SINGLE);
                                message.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(data.message));
                                message.put(Constants.TAG_CONTACT_NAME, ApplicationClass.encryptMessage(data.contact_name));
                                message.put(Constants.TAG_CONTACT_PHONE_NO, ApplicationClass.encryptMessage(data.contact_phone_no));
                                message.put(Constants.TAG_CONTACT_COUNTRY_CODE, ApplicationClass.encryptMessage(data.contact_country_code));
                                message.put(Constants.TAG_ATTACHMENT, data.attachment);
                                message.put(Constants.TAG_THUMBNAIL, data.thumbnail);
                                message.put(Constants.TAG_LAT, ApplicationClass.encryptMessage(data.lat));
                                message.put(Constants.TAG_LON, ApplicationClass.encryptMessage(data.lon));
                                jobj.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                                jobj.put(Constants.TAG_RECEIVER_ID, searchData.user_id);
                                jobj.put("message_data", message);
                                Log.i(TAG, "emitDataChat: " + jobj);
                                socketConnection.startChat(jobj);
                            }

                            dbhelper.addMessageDatas(chatId, data.message_id, GetSet.getUserId(), GetSet.getUserName(), data.message_type,
                                    ApplicationClass.encryptMessage(data.message), ApplicationClass.encryptMessage(data.attachment), ApplicationClass.encryptMessage(data.lat),
                                    ApplicationClass.encryptMessage(data.lon), ApplicationClass.encryptMessage(data.contact_name), ApplicationClass.encryptMessage(data.contact_phone_no),
                                    ApplicationClass.encryptMessage(data.contact_country_code), unixStamp, searchData.user_id, GetSet.getUserId(), "", ApplicationClass.encryptMessage(data.thumbnail), ApplicationClass.encryptMessage(data.statusData));
                            dbhelper.updateMessageData(data.message_id, Constants.TAG_PROGRESS, "completed");
                            dbhelper.addRecentMessages(chatId, searchData.user_id, data.message_id, unixStamp, "0");

                            //                            if (id.equals(searchedData.user_id)) {
                            if (SocketConnection.chatCallbackListener != null) {
                                SocketConnection.chatCallbackListener.onReceiveChat(dbhelper.getSingleMessage(data.message_id));
                            }
                            if (SocketConnection.recentChatReceivedListener != null) {
                                SocketConnection.recentChatReceivedListener.onRecentChatReceived();
                            }
                            if (data.message_type.equals("image") && !storageManager.checkifImageExists("sent", data.attachment)) {
                                storageManager.saveImageToSentPath(storageManager.getImage("receive", data.attachment).getAbsolutePath(), data.attachment);
                            } else if ((data.message_type.equals("video") || data.message_type.equals("file") || data.message_type.equals("audio")) && !storageManager.checkifImageExists("sent", data.attachment)) {
                                storageManager.moveFilesToSentPath(ForwardActivity.this, data.message_type, storageManager.getFile(data.attachment, data.message_type, "receive").getAbsolutePath(), data.attachment);
                            }
                        }
                    } else {
                        for (SearchData sdata : searchedData) {
                            if (sdata.message_type != null) {
                                String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                                String chatId = GetSet.getUserId() + searchData.user_id;
                                RandomString randomString = new RandomString(10);

                                if (!searchData.blockedme.equals("block")) {
                                    JSONObject jobj = new JSONObject();
                                    JSONObject message = new JSONObject();
                                    message.put(Constants.TAG_USER_ID, GetSet.getUserId());
                                    message.put(Constants.TAG_USER_NAME, GetSet.getUserName());
                                    message.put(Constants.TAG_MESSAGE_TYPE, sdata.message_type);
                                    message.put(Constants.TAG_CHAT_TIME, unixStamp);
                                    message.put(Constants.TAG_CHAT_ID, chatId);
                                    message.put(Constants.TAG_MESSAGE_ID, sdata.message_id);
                                    message.put(Constants.TAG_RECEIVER_ID, searchData.user_id);
                                    message.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                                    message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_SINGLE);
                                    message.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(sdata.message));
                                    message.put(Constants.TAG_CONTACT_NAME, ApplicationClass.encryptMessage(sdata.contact_name));
                                    message.put(Constants.TAG_CONTACT_PHONE_NO, ApplicationClass.encryptMessage(sdata.contact_phone_no));
                                    message.put(Constants.TAG_CONTACT_COUNTRY_CODE, ApplicationClass.encryptMessage(sdata.contact_country_code));
                                    message.put(Constants.TAG_ATTACHMENT, sdata.attachment);
                                    message.put(Constants.TAG_THUMBNAIL, sdata.thumbnail);
                                    message.put(Constants.TAG_LAT, ApplicationClass.encryptMessage(sdata.lat));
                                    message.put(Constants.TAG_LON, ApplicationClass.encryptMessage(sdata.lon));
                                    jobj.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                                    jobj.put(Constants.TAG_RECEIVER_ID, searchData.user_id);
                                    jobj.put("message_data", message);
                                    Log.i(TAG, "emitDataChat: " + jobj);
                                    socketConnection.startChat(jobj);
                                }

                                dbhelper.addMessageDatas(chatId, sdata.message_id, GetSet.getUserId(), GetSet.getUserName(), sdata.message_type,
                                        ApplicationClass.encryptMessage(sdata.message), sdata.attachment, ApplicationClass.encryptMessage(sdata.lat),
                                        ApplicationClass.encryptMessage(sdata.lon), ApplicationClass.encryptMessage(sdata.contact_name), ApplicationClass.encryptMessage(sdata.contact_phone_no),
                                        ApplicationClass.encryptMessage(sdata.contact_country_code), unixStamp, searchData.user_id, GetSet.getUserId(), "", sdata.thumbnail,
                                        ApplicationClass.encryptMessage(sdata.statusData));
                                dbhelper.updateMessageData(sdata.message_id, Constants.TAG_PROGRESS, "completed");
                                dbhelper.addRecentMessages(chatId, searchData.user_id, sdata.message_id, unixStamp, "0");

                                //                            if (id.equals(searchedData.user_id)) {
                                if (SocketConnection.chatCallbackListener != null) {
                                    SocketConnection.chatCallbackListener.onReceiveChat(dbhelper.getSingleMessage(sdata.message_id));
                                }
                                if (SocketConnection.recentChatReceivedListener != null) {
                                    SocketConnection.recentChatReceivedListener.onRecentChatReceived();
                                }
                                if (sdata.message_type.equals("image") && !storageManager.checkifImageExists("sent", sdata.attachment)) {
                                    storageManager.saveImageToSentPath(storageManager.getImage("receive", sdata.attachment).getAbsolutePath(), sdata.attachment);
                                } else if ((sdata.message_type.equals("video") || sdata.message_type.equals("file") || sdata.message_type.equals("audio")) && !storageManager.checkifImageExists("sent", sdata.attachment)) {
                                    storageManager.moveFilesToSentPath(ForwardActivity.this, sdata.message_type, storageManager.getFile(sdata.attachment, sdata.message_type, "receive").getAbsolutePath(), sdata.attachment);
                                }
                            }
                        }
                    }
                    //                            }

                } else if (searchData.viewType == VIEW_TYPE_GROUPS) {
                    if (isMultiple) {
                        for (MessagesData data : multipleMediaList) {
                            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                            RandomString randomString = new RandomString(10);
                            String messageId = searchData.groupId + randomString.nextString();

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(Constants.TAG_GROUP_ID, searchData.groupId);
                            jsonObject.put(Constants.TAG_GROUP_NAME, searchData.groupName);
                            jsonObject.put(Constants.TAG_CHAT_TYPE, TAG_GROUP);
                            jsonObject.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                            jsonObject.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                            jsonObject.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
                            jsonObject.put(Constants.TAG_MESSAGE_TYPE, data.message_type);
                            jsonObject.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(data.message));
                            jsonObject.put(Constants.TAG_CONTACT_NAME, ApplicationClass.encryptMessage(data.contact_name));
                            jsonObject.put(Constants.TAG_CONTACT_PHONE_NO, ApplicationClass.encryptMessage(data.contact_phone_no));
                            jsonObject.put(Constants.TAG_CONTACT_COUNTRY_CODE, ApplicationClass.encryptMessage(data.contact_country_code));
                            jsonObject.put(Constants.TAG_ATTACHMENT, data.attachment);
                            jsonObject.put(Constants.TAG_THUMBNAIL, data.thumbnail);
                            jsonObject.put(Constants.TAG_LAT, ApplicationClass.encryptMessage(data.lat));
                            jsonObject.put(Constants.TAG_LON, ApplicationClass.encryptMessage(data.lon));
                            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
                            Log.i(TAG, "emitDataGroupChat: " + jsonObject);
                            socketConnection.startGroupChat(jsonObject);

                            dbhelper.addGroupMessages(messageId, searchData.groupId, GetSet.getUserId(), "", data.message_type,
                                    ApplicationClass.encryptMessage(data.message), data.attachment, ApplicationClass.encryptMessage(data.lat), ApplicationClass.encryptMessage(data.lon),
                                    ApplicationClass.encryptMessage(data.contact_name), ApplicationClass.encryptMessage(data.contact_phone_no), ApplicationClass.encryptMessage(data.contact_country_code),
                                    unixStamp, data.thumbnail, "read");
                            dbhelper.updateGroupMessageData(messageId, Constants.TAG_PROGRESS, "completed");
                            dbhelper.addGroupRecentMsgs(searchData.groupId, messageId, GetSet.getUserId(), unixStamp, "0");

                            //                            if (id.equals(searchedData.groupId)) {
                            if (SocketConnection.groupChatCallbackListener != null) {
                                SocketConnection.groupChatCallbackListener.onGroupChatReceive(dbhelper.getSingleGroupMessage(searchData.groupId, messageId));
                            }
                            if (SocketConnection.groupRecentReceivedListener != null) {
                                SocketConnection.groupRecentReceivedListener.onGroupRecentReceived();
                            }
                            if (data.message_type.equals("image") && !storageManager.checkifImageExists("sent", data.attachment)) {
                                storageManager.saveImageToSentPath(storageManager.getImage("receive", data.attachment).getAbsolutePath(), data.attachment);
                            } else if ((data.message_type.equals("video") || data.message_type.equals("file") || data.message_type.equals("audio")) && !storageManager.checkifImageExists("sent", data.attachment)) {
                                storageManager.moveFilesToSentPath(ForwardActivity.this, data.message_type, storageManager.getFile(data.attachment, data.message_type, "receive").getAbsolutePath(), data.attachment);
                            }
                        }
                    } else {
                        for (SearchData sdata : searchedData) {

                            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                            RandomString randomString = new RandomString(10);
                            String messageId = searchData.groupId + randomString.nextString();

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(Constants.TAG_GROUP_ID, searchData.groupId);
                            jsonObject.put(Constants.TAG_GROUP_NAME, searchData.groupName);
                            jsonObject.put(Constants.TAG_CHAT_TYPE, TAG_GROUP);
                            jsonObject.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                            jsonObject.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                            jsonObject.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
                            jsonObject.put(Constants.TAG_MESSAGE_TYPE, sdata.message_type);
                            jsonObject.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(sdata.message));
                            jsonObject.put(Constants.TAG_CONTACT_NAME, ApplicationClass.encryptMessage(sdata.contact_name));
                            jsonObject.put(Constants.TAG_CONTACT_PHONE_NO, ApplicationClass.encryptMessage(sdata.contact_phone_no));
                            jsonObject.put(Constants.TAG_CONTACT_COUNTRY_CODE, ApplicationClass.encryptMessage(sdata.contact_country_code));
                            jsonObject.put(Constants.TAG_ATTACHMENT, sdata.attachment);
                            jsonObject.put(Constants.TAG_THUMBNAIL, sdata.thumbnail);
                            jsonObject.put(Constants.TAG_LAT, ApplicationClass.encryptMessage(sdata.lat));
                            jsonObject.put(Constants.TAG_LON, ApplicationClass.encryptMessage(sdata.lon));

                            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
                            Log.i(TAG, "emitDataGroupChat: " + jsonObject);
                            socketConnection.startGroupChat(jsonObject);

                            dbhelper.addGroupMessages(messageId, searchData.groupId, GetSet.getUserId(), "", sdata.message_type,
                                    ApplicationClass.encryptMessage(sdata.message), sdata.attachment, ApplicationClass.encryptMessage(sdata.lat), ApplicationClass.encryptMessage(sdata.lon),
                                    ApplicationClass.encryptMessage(sdata.contact_name), ApplicationClass.encryptMessage(sdata.contact_phone_no), ApplicationClass.encryptMessage(sdata.contact_country_code),
                                    unixStamp, sdata.thumbnail, "read");
                            dbhelper.updateGroupMessageData(messageId, Constants.TAG_PROGRESS, "completed");
                            dbhelper.addGroupRecentMsgs(searchData.groupId, messageId, GetSet.getUserId(), unixStamp, "0");

                            //                            if (id.equals(searchedData.groupId)) {
                            if (SocketConnection.groupChatCallbackListener != null) {
                                SocketConnection.groupChatCallbackListener.onGroupChatReceive(dbhelper.getSingleGroupMessage(searchData.groupId, messageId));
                            }
                            if (SocketConnection.groupRecentReceivedListener != null) {
                                SocketConnection.groupRecentReceivedListener.onGroupRecentReceived();
                            }
                            if (sdata.message_type.equals("image") && !storageManager.checkifImageExists("sent", sdata.attachment)) {
                                storageManager.saveImageToSentPath(storageManager.getImage("receive", sdata.attachment).getAbsolutePath(), sdata.attachment);
                            } else if ((sdata.message_type.equals("video") || sdata.message_type.equals("file") || sdata.message_type.equals("audio")) && !storageManager.checkifImageExists("sent", sdata.attachment)) {
                                storageManager.moveFilesToSentPath(ForwardActivity.this, sdata.message_type, storageManager.getFile(sdata.attachment, sdata.message_type, "receive").getAbsolutePath(), sdata.attachment);
                            }
                        }
                    }
                    //                            }
                } else if (searchData.viewType == VIEW_TYPE_CHANNELS) {
                    if (isMultiple) {
                        for (MessagesData data : multipleMediaList) {
                            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                            RandomString randomString = new RandomString(10);
                            String messageId = searchData.channelId + randomString.nextString();

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(Constants.TAG_CHANNEL_ID, searchData.channelId);
                            jsonObject.put(Constants.TAG_CHANNEL_NAME, searchData.channelName);
                            jsonObject.put(Constants.TAG_ADMIN_ID, GetSet.getUserId());
                            jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CHANNEL);
                            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
                            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
                            jsonObject.put(Constants.TAG_MESSAGE_TYPE, data.message_type);
                            jsonObject.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(data.message));
                            jsonObject.put(Constants.TAG_CONTACT_NAME, ApplicationClass.encryptMessage(data.contact_name));
                            jsonObject.put(Constants.TAG_CONTACT_PHONE_NO, ApplicationClass.encryptMessage(data.contact_phone_no));
                            jsonObject.put(Constants.TAG_CONTACT_COUNTRY_CODE, ApplicationClass.encryptMessage(data.contact_country_code));
                            jsonObject.put(Constants.TAG_ATTACHMENT, data.attachment);
                            jsonObject.put(Constants.TAG_THUMBNAIL, data.thumbnail);
                            jsonObject.put(Constants.TAG_LAT, ApplicationClass.encryptMessage(data.lat));
                            jsonObject.put(Constants.TAG_LON, ApplicationClass.encryptMessage(data.lon));
                            Log.i(TAG, "emitDataChannelChat: " + jsonObject);
                            socketConnection.startChannelChat(jsonObject);

                            dbhelper.addChannelMessages(searchData.channelId, Constants.TAG_CHANNEL, messageId, data.message_type,
                                    ApplicationClass.encryptMessage(data.message), data.attachment, ApplicationClass.encryptMessage(data.lat), ApplicationClass.encryptMessage(data.lon),
                                    ApplicationClass.encryptMessage(data.contact_name), ApplicationClass.encryptMessage(data.contact_phone_no),
                                    ApplicationClass.encryptMessage(data.contact_country_code), unixStamp, data.thumbnail, "read");
                            dbhelper.updateChannelMessageData(messageId, Constants.TAG_PROGRESS, "completed");
                            dbhelper.addChannelRecentMsgs(searchData.channelId, messageId, unixStamp, "0");

                            //                            if (id.equals(searchedData.channelId)) {
                            if (SocketConnection.channelChatCallbackListener != null) {
                                SocketConnection.channelChatCallbackListener.onChannelChatReceive(dbhelper.getSingleChannelMessage(searchData.channelId, messageId));
                            }
                            if (SocketConnection.channelRecentReceivedListener != null) {
                                SocketConnection.channelRecentReceivedListener.onChannelRecentReceived();
                            }
                            if (data.message_type.equals("image") && !storageManager.checkifImageExists("sent", data.attachment)) {
                                storageManager.saveImageToSentPath(storageManager.getImage("receive", data.attachment).getAbsolutePath(), data.attachment);
                            } else if ((data.message_type.equals("video") || data.message_type.equals("file") || data.message_type.equals("audio")) && !storageManager.checkifImageExists("sent", data.attachment)) {
                                storageManager.moveFilesToSentPath(ForwardActivity.this, data.message_type, storageManager.getFile(data.attachment, data.message_type, "receive").getAbsolutePath(), data.attachment);
                            }
                        }
                    } else {
                        for (SearchData sdata : searchedData) {

                            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                            RandomString randomString = new RandomString(10);
                            String messageId = searchData.channelId + randomString.nextString();

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(Constants.TAG_CHANNEL_ID, searchData.channelId);
                            jsonObject.put(Constants.TAG_CHANNEL_NAME, searchData.channelName);
                            jsonObject.put(Constants.TAG_ADMIN_ID, GetSet.getUserId());
                            jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CHANNEL);
                            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
                            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
                            jsonObject.put(Constants.TAG_MESSAGE_TYPE, sdata.message_type);
                            jsonObject.put(Constants.TAG_MESSAGE, ApplicationClass.encryptMessage(sdata.message));
                            jsonObject.put(Constants.TAG_CONTACT_NAME, ApplicationClass.encryptMessage(sdata.contact_name));
                            jsonObject.put(Constants.TAG_CONTACT_PHONE_NO, ApplicationClass.encryptMessage(sdata.contact_phone_no));
                            jsonObject.put(Constants.TAG_CONTACT_COUNTRY_CODE, ApplicationClass.encryptMessage(sdata.contact_country_code));
                            jsonObject.put(Constants.TAG_ATTACHMENT, sdata.attachment);
                            jsonObject.put(Constants.TAG_THUMBNAIL, sdata.thumbnail);
                            jsonObject.put(Constants.TAG_LAT, ApplicationClass.encryptMessage(sdata.lat));
                            jsonObject.put(Constants.TAG_LON, ApplicationClass.encryptMessage(sdata.lon));
                            Log.i(TAG, "emitDataChannelChat: " + jsonObject);

                            socketConnection.startChannelChat(jsonObject);

                            dbhelper.addChannelMessages(searchData.channelId, Constants.TAG_CHANNEL, messageId, sdata.message_type,
                                    ApplicationClass.encryptMessage(sdata.message), sdata.thumbnail, ApplicationClass.encryptMessage(sdata.lat), ApplicationClass.encryptMessage(sdata.lon),
                                    ApplicationClass.encryptMessage(sdata.contact_name), ApplicationClass.encryptMessage(sdata.contact_phone_no),
                                    ApplicationClass.encryptMessage(sdata.contact_country_code), unixStamp, sdata.thumbnail, "read");
                            dbhelper.updateChannelMessageData(messageId, Constants.TAG_PROGRESS, "completed");
                            dbhelper.addChannelRecentMsgs(searchData.channelId, messageId, unixStamp, "0");

                            //                            if (id.equals(searchedData.channelId)) {
                            if (SocketConnection.channelChatCallbackListener != null) {
                                SocketConnection.channelChatCallbackListener.onChannelChatReceive(dbhelper.getSingleChannelMessage(searchData.channelId, messageId));
                            }
                            if (SocketConnection.channelRecentReceivedListener != null) {
                                SocketConnection.channelRecentReceivedListener.onChannelRecentReceived();
                            }
                            if (sdata.message_type.equals("image") && !storageManager.checkifImageExists("sent", sdata.attachment)) {
                                storageManager.saveImageToSentPath(storageManager.getImage("receive", sdata.attachment).getAbsolutePath(), sdata.attachment);
                            } else if ((sdata.message_type.equals("video") || sdata.message_type.equals("file") || sdata.message_type.equals("audio")) && !storageManager.checkifImageExists("sent", sdata.attachment)) {
                                storageManager.moveFilesToSentPath(ForwardActivity.this, sdata.message_type, storageManager.getFile(sdata.attachment, sdata.message_type, "receive").getAbsolutePath(), sdata.attachment);
                            }
                        }
                    }
                }
            }

            switch (from) {
                case "chat": {
                    Intent i = new Intent(ForwardActivity.this, ChatActivity.class);
                    setResult(RESULT_OK, i);
                    progressDialog.dismiss();
                    finish();
                    break;
                }
                case "group": {
                    Intent i = new Intent(ForwardActivity.this, GroupChatActivity.class);
                    setResult(RESULT_OK, i);
                    progressDialog.dismiss();
                    finish();
                    break;
                }
                case "channel": {
                    Intent i = new Intent(ForwardActivity.this, ChannelChatActivity.class);
                    setResult(RESULT_OK, i);
                    progressDialog.dismiss();
                    finish();
                    break;
                }
                case "external": {
                    progressDialog.dismiss();
                    finish();
                    break;
                }
            }
        } catch (Exception e) {
            progressDialog.dismiss();
            btnNext.setEnabled(true);
            e.printStackTrace();
        }
    }

    private void blockChatConfirmDialog(String userId) {
        final Dialog dialog = new Dialog(ForwardActivity.this);
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
                    Log.v(TAG, "block=" + jsonObject);
                    socketConnection.block(jsonObject);
                    dbhelper.updateBlockStatus(userId, Constants.TAG_BLOCKED_BYME, "unblock");
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

    public String getMimeType(Uri uri) {
        String mimeType = null;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = this.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        Log.d(TAG, "getMimeType: " + mimeType);
        return mimeType;
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter implements Filterable {

        public static final int VIEW_TYPE_CHATS_HEADER = 1;
        public static final int VIEW_TYPE_CHATS = 2;
        public static final int VIEW_TYPE_GROUP_HEADER = 3;
        public static final int VIEW_TYPE_GROUPS = 4;
        public static final int VIEW_TYPE_CHANNEL_HEADER = 5;
        public static final int VIEW_TYPE_CHANNELS = 6;
        public static final int VIEW_TYPE_CONTACTS_HEADER = 7;
        public static final int VIEW_TYPE_CONTACTS = 8;

        Context context;
        private RecyclerViewAdapter.SearchFilter mFilter;

        public RecyclerViewAdapter(Context context) {
            this.context = context;
            mFilter = new RecyclerViewAdapter.SearchFilter(RecyclerViewAdapter.this);
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = null;

            if (viewType == VIEW_TYPE_CHATS_HEADER) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_search_header, parent, false);
                return new RecyclerViewAdapter.HeaderViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_CHATS) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_blocked_contacts, parent, false);
                return new RecyclerViewAdapter.MyViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_GROUP_HEADER) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_search_header, parent, false);
                return new RecyclerViewAdapter.HeaderViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_GROUPS) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_blocked_contacts, parent, false);
                return new RecyclerViewAdapter.MyViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_CHANNEL_HEADER) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_search_header, parent, false);
                return new RecyclerViewAdapter.HeaderViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_CHANNELS) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_blocked_contacts, parent, false);
                return new RecyclerViewAdapter.MyViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_CONTACTS_HEADER) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_search_header, parent, false);
                return new RecyclerViewAdapter.HeaderViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_CONTACTS) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_blocked_contacts, parent, false);
                return new RecyclerViewAdapter.MyViewHolder(itemView);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            if (getItemViewType(position) == VIEW_TYPE_CHATS_HEADER) {
                ((HeaderViewHolder) holder).txtHeader.setText(getString(R.string.chat));
            } else if (getItemViewType(position) == VIEW_TYPE_GROUP_HEADER) {
                ((HeaderViewHolder) holder).txtHeader.setText(getString(R.string.group));
            } else if (getItemViewType(position) == VIEW_TYPE_CHANNEL_HEADER) {
                ((HeaderViewHolder) holder).txtHeader.setText(getString(R.string.channels));
            } else if (getItemViewType(position) == VIEW_TYPE_CONTACTS_HEADER) {
                ((HeaderViewHolder) holder).txtHeader.setText(getString(R.string.contacts));
            } else {
                final SearchData data = filteredList.get(position);

                if (selectedList.contains(filteredList.get(position))) {
                    ((MyViewHolder) holder).btnSelect.setChecked(true);
                } else {
                    ((MyViewHolder) holder).btnSelect.setChecked(false);
                }

                if (getItemViewType(position) == VIEW_TYPE_CHATS || getItemViewType(position) == VIEW_TYPE_CONTACTS) {
                    ((MyViewHolder) holder).name.setText(data.user_name);
                    if (data.user_id != null) {
                        ContactsData.Result result = dbhelper.getContactDetail(data.user_id);
                        DialogActivity.setProfileImage(result, ((MyViewHolder) holder).profileimage, context);
                    } else {
                        Glide.with(context).load(R.drawable.temp)
                                .apply(new RequestOptions().placeholder(R.drawable.temp).error(R.drawable.temp))
                                .into(((MyViewHolder) holder).profileimage);
                    }
                } else if ((getItemViewType(position) == VIEW_TYPE_GROUPS)) {
                    ((MyViewHolder) holder).name.setText(data.groupName);
                    Glide.with(context).load(Constants.GROUP_IMG_PATH + data.user_image)
                            .apply(new RequestOptions().placeholder(R.drawable.create_group).error(R.drawable.create_group))
                            .into(((MyViewHolder) holder).profileimage);

                } else if (getItemViewType(position) == VIEW_TYPE_CHANNELS) {
                    ((MyViewHolder) holder).name.setText("" + data.channelName);
                    Glide.with(context).load(Constants.CHANNEL_IMG_PATH + data.channelImage)
                            .apply(new RequestOptions().placeholder(R.drawable.temp).error(R.drawable.temp))
                            .into(((MyViewHolder) holder).profileimage);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (filteredList.get(position).viewType == VIEW_TYPE_CHATS_HEADER) {
                return VIEW_TYPE_CHATS_HEADER;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_CHATS) {
                return VIEW_TYPE_CHATS;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_GROUP_HEADER) {
                return VIEW_TYPE_GROUP_HEADER;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_GROUPS) {
                return VIEW_TYPE_GROUPS;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_CHANNEL_HEADER) {
                return VIEW_TYPE_CHANNEL_HEADER;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_CHANNELS) {
                return VIEW_TYPE_CHANNELS;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_CONTACTS_HEADER) {
                return VIEW_TYPE_CONTACTS_HEADER;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_CONTACTS) {
                return VIEW_TYPE_CONTACTS;
            }
            return 0;
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        public class SearchFilter extends Filter {
            private RecyclerViewAdapter mAdapter;

            private SearchFilter(RecyclerViewAdapter mAdapter) {
                super();
                this.mAdapter = mAdapter;
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                filteredList.clear();
                chatList.clear();
                channelList.clear();
                groupList.clear();
                contactsList.clear();
                final FilterResults results = new FilterResults();
                if (constraint.length() == 0) {
                    filteredList.addAll(searchList);
                } else {
                    final String filterPattern = constraint.toString().toLowerCase().trim();

                    for (SearchData data : searchList) {
                        if (data.viewType == VIEW_TYPE_CHATS_HEADER) {
                            chatList.add(data);
                        } else if (data.viewType == VIEW_TYPE_CHATS) {
                            if (data.user_name.toLowerCase().startsWith(filterPattern)) {
                                chatList.add(data);
                            }
                        } else if (data.viewType == VIEW_TYPE_GROUP_HEADER) {
                            groupList.add(data);
                        } else if (data.viewType == VIEW_TYPE_GROUPS) {
                            if (data.groupName.toLowerCase().startsWith(filterPattern)) {
                                groupList.add(data);
                            }
                        } else if (data.viewType == VIEW_TYPE_CHANNEL_HEADER) {
                            channelList.add(data);
                        } else if (data.viewType == VIEW_TYPE_CHANNELS) {
                            if (data.channelName.toLowerCase().startsWith(filterPattern)) {
                                channelList.add(data);
                            }
                        } else if (data.viewType == VIEW_TYPE_CONTACTS_HEADER) {
                            contactsList.add(data);
                        } else if (data.viewType == VIEW_TYPE_CONTACTS) {
                            if (data.user_name.toLowerCase().startsWith(filterPattern)) {
                                contactsList.add(data);
                            }
                        }
                    }

                    if (chatList.size() > 1) {
                        filteredList.addAll(chatList);
                    }
                    if (groupList.size() > 1) {
                        filteredList.addAll(groupList);
                    }
                    if (channelList.size() > 1) {
                        filteredList.addAll(channelList);
                    }
                    if (contactsList.size() > 1) {
                        filteredList.addAll(contactsList);
                    }
                }

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                this.mAdapter.notifyDataSetChanged();
            }
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay;
            TextView name;
            ImageView profileimage;
            View profileview;
            AppCompatRadioButton btnSelect;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                profileimage = view.findViewById(R.id.profileimage);
                name = view.findViewById(R.id.txtName);
                profileview = view.findViewById(R.id.profileview);
                btnSelect = view.findViewById(R.id.btnSelect);

                btnSelect.setVisibility(View.VISIBLE);
                parentlay.setOnClickListener(this);
                btnSelect.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
                    case R.id.btnSelect:
                        if (filteredList.get(getAdapterPosition()).viewType == VIEW_TYPE_CHATS) {
                            ContactsData.Result result = dbhelper.getContactDetail(filteredList.get(getAdapterPosition()).user_id);
                            if (result.blockedbyme.equals("block")) {
                                btnSelect.setChecked(false);
                                blockChatConfirmDialog(result.user_id);
                            } else {
                                if (!selectedList.contains(filteredList.get(getAdapterPosition()))) {
                                    selectedList.add(filteredList.get(getAdapterPosition()));
                                    btnSelect.setChecked(true);
                                } else {
                                    btnSelect.setChecked(false);
                                    selectedList.remove(filteredList.get(getAdapterPosition()));
                                }
                                notifyDataSetChanged();
                            }
                        } else {
                            if (!selectedList.contains(filteredList.get(getAdapterPosition()))) {
                                selectedList.add(filteredList.get(getAdapterPosition()));
                                btnSelect.setChecked(true);
                            } else {
                                btnSelect.setChecked(false);
                                selectedList.remove(filteredList.get(getAdapterPosition()));
                            }
                            notifyDataSetChanged();
                        }
                        break;
                }
            }
        }

        public class HeaderViewHolder extends RecyclerView.ViewHolder {

            TextView txtHeader;

            public HeaderViewHolder(View view) {
                super(view);
                txtHeader = view.findViewById(R.id.txtHeader);
            }
        }
    }

}
