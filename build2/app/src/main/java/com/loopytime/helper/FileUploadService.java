package com.loopytime.helper;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.loopytime.im.R;
import com.loopytime.model.ChannelMessage;
import com.loopytime.model.GroupMessage;
import com.loopytime.model.MessagesData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by hitasoft on 14/7/18.
 */

public class FileUploadService extends IntentService implements ProgressRequestBody.UploadCallbacks {

    private static final String TAG = FileUploadService.class.getSimpleName();
    NotificationManagerCompat mNotifyManager;
    NotificationCompat.Builder mBuilder;
    DatabaseHandler dbhelper;
    SocketConnection socketConnection;
    StorageManager storageManager;
    MessagesData mdata;
    GroupMessage gdata;
    ChannelMessage chdata;
    String filepath, chatType, thumbnail = null;
    long startTime;
    long elapsedTime = 0L;

    public FileUploadService() {
        super("FileUploadService");
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");
        super.onCreate();
        //mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyManager = NotificationManagerCompat.from(this);
        String channelId = getString(R.string.notification_channel_foreground_service);
        CharSequence channelName = getString(R.string.app_name);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            mNotifyManager.createNotificationChannel(notificationChannel);
        }*/
        mBuilder = new NotificationCompat.Builder(this, channelId);
        mBuilder.setContentTitle(getString(R.string.app_name))
                .setContentText("Uploading..")
                .setSmallIcon(R.drawable.notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true);
        mNotifyManager.notify(2, mBuilder.build());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent");
        dbhelper = DatabaseHandler.getInstance(this);
        socketConnection = SocketConnection.getInstance(this);
        storageManager = StorageManager.getInstance(this);

        Bundle bundle = intent.getExtras();
        filepath = bundle.getString("filepath");
        chatType = bundle.getString("chatType");
        if (bundle.containsKey(Constants.TAG_THUMBNAIL)) {
            thumbnail = bundle.getString(Constants.TAG_THUMBNAIL);
        }
        if (chatType.equals("chat")) {
            mdata = (MessagesData) bundle.getSerializable("mdata");

            ProgressRequestBody fileBody = new ProgressRequestBody(new File(filepath), this);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("attachment", new File(filepath).getName(), fileBody);

            ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
            RequestBody userid = RequestBody.create(GetSet.getUserId(), MediaType.parse("multipart/form-data"));
            Call<Map<String, String>> call3 = apiInterface.upchat(GetSet.getToken(), filePart, userid);
            try {
                Response<Map<String, String>> response = call3.execute();
                if (response.isSuccessful()) {
                    try {
                        Log.v(TAG, "UploadSingleChatResponse: " + response.body());
                        Log.v(TAG, "filepath=" + filepath);
                        Map<String, String> userdata = response.body();
                        if (userdata.get(Constants.TAG_STATUS).equals("true")) {
                            try {
                                boolean fileStatus;
                                if (mdata.message_type.equals(Constants.TAG_IMAGE)) {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, mdata.message_type, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                } else if (mdata.message_type.equals(Constants.TAG_AUDIO)) {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, StorageManager.TAG_AUDIO_SENT, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                } else if (mdata.message_type.equals(Constants.TAG_VIDEO)) {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, StorageManager.TAG_VIDEO_SENT, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                } else {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, StorageManager.TAG_FILE_SENT, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                }
                                if (fileStatus) {
                                    JSONObject jobj = new JSONObject();
                                    JSONObject message = new JSONObject();
                                    message.put(Constants.TAG_USER_ID, mdata.user_id);
                                    message.put(Constants.TAG_USER_NAME, mdata.user_name);
                                    message.put(Constants.TAG_MESSAGE_TYPE, mdata.message_type);
                                    message.put(Constants.TAG_ATTACHMENT, userdata.get(Constants.TAG_USER_IMAGE));
                                    message.put(Constants.TAG_THUMBNAIL, thumbnail != null ? thumbnail : "");
                                    message.put(Constants.TAG_MESSAGE, mdata.message);
                                    message.put(Constants.TAG_CHAT_TIME, String.valueOf(System.currentTimeMillis() / 1000L));
                                    message.put(Constants.TAG_CHAT_ID, mdata.chat_id);
                                    message.put(Constants.TAG_MESSAGE_ID, mdata.message_id);
                                    message.put(Constants.TAG_RECEIVER_ID, mdata.receiver_id);
                                    message.put(Constants.TAG_SENDER_ID, mdata.user_id);
                                    message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_SINGLE);
                                    jobj.put(Constants.TAG_SENDER_ID, mdata.user_id);
                                    jobj.put(Constants.TAG_RECEIVER_ID, mdata.receiver_id);
                                    jobj.put("message_data", message);
                                    Log.v(TAG, "startChat=" + jobj);
                                    socketConnection.startChat(jobj);

                                    dbhelper.updateMessageData(mdata.message_id, Constants.TAG_ATTACHMENT, userdata.get(Constants.TAG_USER_IMAGE));
                                    dbhelper.updateMessageData(mdata.message_id, Constants.TAG_PROGRESS, "completed");

                                    mBuilder.setProgress(0, 0, false);
                                    mBuilder.setContentText(getString(R.string.file_uploaded));
                                    mNotifyManager.cancel("progress", 2);
                                    mNotifyManager.notify(2, mBuilder.build());

                                    socketConnection.setUploadingListen(chatType, mdata.message_id, userdata.get(Constants.TAG_USER_IMAGE), "completed", null);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                setErrorUpload();
                            }
                        } else {
                            setErrorUpload();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        setErrorUpload();
                    }
                } else {
                    setErrorUpload();
                }
            } catch (IOException e) {
                e.printStackTrace();
                setErrorUpload();
            }
        } else if (chatType.equals("group")) {
            gdata = (GroupMessage) bundle.getSerializable("mdata");
            Log.i(TAG, "onHandleIntent: " + filepath);
            ProgressRequestBody fileBody = new ProgressRequestBody(new File(filepath), this);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("group_attachment", new File(filepath).getName(), fileBody);

            ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
            RequestBody userid = RequestBody.create(GetSet.getUserId(), MediaType.parse("multipart/form-data"));
            Call<Map<String, String>> call3 = apiInterface.upGroupchat(GetSet.getToken(), filePart, userid);
            try {
                Response<Map<String, String>> response = call3.execute();
                if (response.isSuccessful()) {
                    try {
                        Map<String, String> userdata = response.body();
                        if (userdata.get(Constants.TAG_STATUS).equals("true")) {
                            try {
                                boolean fileStatus;
                                if (gdata.messageType.equals(Constants.TAG_IMAGE)) {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, gdata.messageType, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                } else if (gdata.messageType.equals(Constants.TAG_AUDIO)) {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, StorageManager.TAG_AUDIO_SENT, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                } else if (gdata.messageType.equals(Constants.TAG_VIDEO)) {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, StorageManager.TAG_VIDEO_SENT, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                } else {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, StorageManager.TAG_FILE_SENT, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                }

                                if (fileStatus) {
                                    JSONObject message = new JSONObject();
                                    message.put(Constants.TAG_GROUP_ID, gdata.groupId);
                                    message.put(Constants.TAG_GROUP_NAME, gdata.groupName);
                                    message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
                                    message.put(Constants.TAG_MESSAGE_TYPE, gdata.messageType);
                                    message.put(Constants.TAG_ATTACHMENT, userdata.get(Constants.TAG_USER_IMAGE));
                                    message.put(Constants.TAG_THUMBNAIL, thumbnail != null ? thumbnail : "");
                                    message.put(Constants.TAG_MESSAGE, gdata.message);
                                    message.put(Constants.TAG_CHAT_TIME, String.valueOf(System.currentTimeMillis() / 1000L));
                                    message.put(Constants.TAG_MESSAGE_ID, gdata.messageId);
                                    message.put(Constants.TAG_MEMBER_ID, gdata.memberId);
                                    message.put(Constants.TAG_MEMBER_NAME, gdata.memberName);
                                    message.put(Constants.TAG_MEMBER_NO, gdata.memberNo);
                                    Log.v("checkChat", "startchat=" + message);
                                    socketConnection.startGroupChat(message);

                                    dbhelper.updateGroupMessageData(gdata.messageId, Constants.TAG_ATTACHMENT, userdata.get(Constants.TAG_USER_IMAGE));
                                    dbhelper.updateGroupMessageData(gdata.messageId, Constants.TAG_PROGRESS, "completed");

                                    mBuilder.setProgress(0, 0, false);
                                    mBuilder.setContentText(getString(R.string.file_uploaded));
                                    mNotifyManager.cancel("progress", 2);
                                    mNotifyManager.notify(2, mBuilder.build());

                                    socketConnection.setUploadingListen(chatType, gdata.messageId, userdata.get(Constants.TAG_USER_IMAGE), "completed", null);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                setGroupErrorUpload();
                            }
                        } else {
                            setGroupErrorUpload();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        setGroupErrorUpload();
                    }
                } else {
                    setGroupErrorUpload();
                }
            } catch (IOException e) {
                e.printStackTrace();
                setGroupErrorUpload();
            }
        } else if (chatType.equals(Constants.TAG_CHANNEL)) {
            chdata = (ChannelMessage) bundle.getSerializable("mdata");

            ProgressRequestBody fileBody = new ProgressRequestBody(new File(filepath), this);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("channel_attachment", new File(filepath).getName(), fileBody);

            RequestBody channelid = RequestBody.create(MediaType.parse("multipart/form-data"), chdata.channelId);
            ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
            RequestBody userid = RequestBody.create(MediaType.parse("multipart/form-data"), GetSet.getUserId());
            Call<Map<String, String>> call3 = apiInterface.upChannelChat(GetSet.getToken(), filePart, channelid, userid);
            try {
                Response<Map<String, String>> response = call3.execute();
                if (response.isSuccessful()) {
                    try {
                        Log.v(TAG, "upChannelChat " + response.body());
                        Log.v(TAG, "filepath=" + filepath);
                        Map<String, String> userdata = response.body();
                        if (userdata.get(Constants.TAG_STATUS).equals("true")) {
                            try {
                                boolean fileStatus;
                                if (chdata.messageType.equals(Constants.TAG_IMAGE)) {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, chdata.messageType, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                } else if (chdata.messageType.equals(Constants.TAG_AUDIO)) {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, StorageManager.TAG_AUDIO_SENT, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                } else if (chdata.messageType.equals(Constants.TAG_VIDEO)) {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, StorageManager.TAG_VIDEO_SENT, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                } else {
                                    fileStatus = storageManager.moveFilesToSentPath(FileUploadService.this, StorageManager.TAG_FILE_SENT, filepath, userdata.get(Constants.TAG_USER_IMAGE));
                                }

                                if (fileStatus) {
                                    JSONObject message = new JSONObject();
                                    message.put(Constants.TAG_CHANNEL_ID, chdata.channelId);
                                    message.put(Constants.TAG_ADMIN_ID, chdata.channelAdminId);
                                    message.put(Constants.TAG_CHANNEL_NAME, chdata.channelName);
                                    message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CHANNEL);
                                    message.put(Constants.TAG_MESSAGE_TYPE, chdata.messageType);
                                    message.put(Constants.TAG_ATTACHMENT, userdata.get(Constants.TAG_USER_IMAGE));
                                    message.put(Constants.TAG_THUMBNAIL, thumbnail != null ? thumbnail : "");
                                    message.put(Constants.TAG_MESSAGE, chdata.message);
                                    message.put(Constants.TAG_CHAT_TIME, String.valueOf(System.currentTimeMillis() / 1000L));
                                    message.put(Constants.TAG_MESSAGE_ID, chdata.messageId);
                                    socketConnection.startChannelChat(message);

                                    dbhelper.updateChannelMessageData(chdata.messageId, Constants.TAG_ATTACHMENT, userdata.get(Constants.TAG_USER_IMAGE));
                                    dbhelper.updateChannelMessageData(chdata.messageId, Constants.TAG_PROGRESS, "completed");

                                    mBuilder.setProgress(0, 0, false);
                                    mBuilder.setContentText(getString(R.string.file_uploaded));
                                    mNotifyManager.cancel("progress", 2);
                                    mNotifyManager.notify(2, mBuilder.build());

                                    socketConnection.setUploadingListen(chatType, chdata.messageId, userdata.get(Constants.TAG_USER_IMAGE), "completed", null);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                setChannelErrorUpload();
                            }
                        } else {
                            setChannelErrorUpload();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        setChannelErrorUpload();
                    }
                } else {
                    setChannelErrorUpload();
                }
            } catch (IOException e) {
                e.printStackTrace();
                setChannelErrorUpload();
            }
        } else if (chatType.equals(StorageManager.TAG_STATUS)) {
            mdata = (MessagesData) bundle.getSerializable("mdata");

            ProgressRequestBody fileBody = new ProgressRequestBody(new File(filepath), this);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("attachment", new File(filepath).getName(), fileBody);

            ApiInterface apiInterface = ApiClient.getUploadClient().create(ApiInterface.class);
            RequestBody userid = RequestBody.create(GetSet.getUserId(), MediaType.parse("multipart/form-data"));
            Call<Map<String, String>> call3 = apiInterface.upchat(GetSet.getToken(), filePart, userid);
            try {
                Response<Map<String, String>> response = call3.execute();
                if (response.isSuccessful()) {
                    try {
                        Log.v(TAG, "uploadStatusResponse= " + response.body());
                        Log.v(TAG, "filepath= " + filepath);
                        Map<String, String> userdata = response.body();
                        if (userdata.get(Constants.TAG_STATUS).equals("true")) {
                            mBuilder.setProgress(0, 0, false);
                            mBuilder.setContentText(getString(R.string.file_uploaded));
                            mNotifyManager.cancel("progress", 2);
                            mNotifyManager.notify(2, mBuilder.build());

                            socketConnection.setUploadingListen(chatType, "", userdata.get(Constants.TAG_USER_IMAGE), "completed", storageManager.getFileName(filepath));
                        } else {
                            setStatuErrorUpload();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        setStatuErrorUpload();
                    }
                } else {
                    setStatuErrorUpload();
                }
            } catch (IOException e) {
                e.printStackTrace();
                setStatuErrorUpload();
            }
        }

        Log.v(TAG, "onHandleIntent END");
    }

    private void setErrorUpload() {
        dbhelper.updateMessageData(mdata.message_id, Constants.TAG_PROGRESS, "error");
        socketConnection.setUploadingListen(chatType, mdata.message_id, filepath, "error", null);
        mBuilder.setProgress(0, 0, false);
        mBuilder.setContentText(getString(R.string.file_upload_error));
        mNotifyManager.cancel("progress", 2);
        mNotifyManager.notify(2, mBuilder.build());
    }

    private void setGroupErrorUpload() {
        dbhelper.updateGroupMessageData(gdata.messageId, Constants.TAG_PROGRESS, "error");
        socketConnection.setUploadingListen(chatType, gdata.messageId, filepath, "error", null);
        mBuilder.setProgress(0, 0, false);
        mBuilder.setContentText(getString(R.string.file_upload_error));
        mNotifyManager.cancel("progress", 2);
        mNotifyManager.notify(2, mBuilder.build());
    }

    private void setChannelErrorUpload() {
        dbhelper.updateChannelMessageData(chdata.messageId, Constants.TAG_PROGRESS, "error");
        socketConnection.setUploadingListen(chatType, chdata.messageId, filepath, "error", null);
        mBuilder.setProgress(0, 0, false);
        mBuilder.setContentText(getString(R.string.file_upload_error));
        mNotifyManager.cancel("progress", 2);
        mNotifyManager.notify(2, mBuilder.build());
    }

    private void setStatuErrorUpload() {
        socketConnection.setUploadingListen(chatType, mdata.message_id, filepath, "error", null);
        mBuilder.setProgress(0, 0, false);
        mBuilder.setContentText(getString(R.string.file_upload_error));
        mNotifyManager.cancel("progress", 2);
        mNotifyManager.notify(2, mBuilder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "In onDestroy");
    }

    @Override
    public void onProgressUpdate(final int percentage) {
        if (elapsedTime > 500) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mBuilder.setProgress(100, percentage, false);
                    mBuilder.setContentText("Uploading..");
                    mNotifyManager.notify("progress", 2, mBuilder.build());

                    startTime = System.currentTimeMillis();
                    elapsedTime = 0;
                }
            });
            Log.v(TAG, "onProgressUpdate=" + percentage);
        } else
            elapsedTime = new Date().getTime() - startTime;
    }

    @Override
    public void onError() {
        Log.v(TAG, "onError");
        if (chatType.equals("chat")) {
            setErrorUpload();
        } else if (chatType.equals("group")) {
            setGroupErrorUpload();
        }
    }

}
