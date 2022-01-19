package com.loopytime.helper;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.loopytime.apprtc.util.AppRTCUtils;
import com.loopytime.im.ApplicationClass;
import com.loopytime.im.CallActivity;
import com.loopytime.im.ChannelChatActivity;
import com.loopytime.im.ChatActivity;
import com.loopytime.im.GroupChatActivity;
import com.loopytime.im.MainActivity;
import com.loopytime.im.R;
import com.loopytime.model.AdminChannel;
import com.loopytime.model.AdminChannelMsg;
import com.loopytime.model.ChannelMessage;
import com.loopytime.model.ChannelResult;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupData;
import com.loopytime.model.GroupMessage;
import com.loopytime.model.MessagesData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.loopytime.utils.Constants.TAG_ADMIN_ID;
import static com.loopytime.utils.Constants.TAG_CHANNEL_ID;
import static com.loopytime.utils.Constants.TAG_CHANNEL_NAME;
import static com.loopytime.utils.Constants.TAG_CONTACT_STATUS;
import static com.loopytime.utils.Constants.TAG_GROUP_ADMIN_ID;
import static com.loopytime.utils.Constants.TAG_GROUP_ID;
import static com.loopytime.utils.Constants.TAG_GROUP_NAME;
import static com.loopytime.utils.Constants.TAG_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER_ID;
import static com.loopytime.utils.Constants.TAG_MY_CONTACTS;
import static com.loopytime.utils.Constants.TAG_NOBODY;
import static com.loopytime.utils.Constants.TRUE;

/**
 * Created by Hitasoft on 03/11/16.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String LABEL_REPLY = "Reply";
    public static final String KEY_PRESSED_ACTION = "KEY_PRESSED_ACTION";
    public static final String REPLY_ACTION = "com.hitherejoe.notifi.util.ACTION_MESSAGE_REPLY";
    public static final int REPLY_INTENT_ID = 0;
    public static final String GROUP_KEY = "hiddy_Group";
    private static final String TAG = "MyFirebaseMsgService";
    DatabaseHandler dbhelper;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    ApiInterface apiInterface;
    StorageManager storageManager;

    /*public static void start(Context context) {
        Intent startServiceIntent = new Intent(context, MyFirebaseInstanceIDService.class);
        context.startService(startServiceIntent);

        Intent notificationServiceIntent = new Intent(context, MyFirebaseMessagingService.class);
        context.startService(notificationServiceIntent);
    }*/

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Log.d(TAG, "onNewToken: " + s);
        storeToken(s);
    }

    private void storeToken(String token) {
        //saving the token on shared preferences
        SharedPrefManager.getInstance(getApplicationContext()).saveDeviceToken(token);

        //get the logined user details from preference
        ApplicationClass.pref = getApplicationContext().getSharedPreferences("SavedPref", MODE_PRIVATE);
        ApplicationClass.editor = ApplicationClass.pref.edit();

        if (ApplicationClass.pref.getBoolean("isLogged", false)) {
            GetSet.setLogged(true);
            GetSet.setUserId(ApplicationClass.pref.getString("userId", null));
            addDeviceId();
        }
    }

    private void addDeviceId() {
        final String token = SharedPrefManager.getInstance(getApplicationContext()).getDeviceToken();
        final String deviceId = android.provider.Settings.Secure.getString(getApplicationContext().getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        Map<String, String> map = new HashMap<>();
        map.put("user_id", GetSet.getUserId());
        map.put("device_token", token);
        map.put("device_type", "1");
        map.put("device_id", deviceId);
        Log.v("addDeviceId:", "Params- " + map);
        Call<Map<String, String>> call3 = apiInterface.pushsignin(GetSet.getToken(), map);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                Map<String, String> data = response.body();
                Log.v("addDeviceId:", "response- " + data);

            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                call.cancel();

            }
        });

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        dbhelper = DatabaseHandler.getInstance(this);
        storageManager = StorageManager.getInstance(this);
        pref = getApplicationContext().getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        if (pref.getBoolean("isLogged", false)) {
            GetSet.setUserId(pref.getString("userId", null));
            GetSet.setToken(pref.getString("token", null));
        }

        if (remoteMessage.getData().size() > 0) {
            Log.e(TAG, "Data Payload: " + remoteMessage.getData().toString());
            long sendTime = remoteMessage.getSentTime();
            try {
                String strData = remoteMessage.getData().toString().replace("messsage_data=", "\"message_data\":");
                JSONObject remdata = new JSONObject(strData);
                Log.e(TAG, "Data Payload: strData " + remdata);
                JSONObject data = remdata.getJSONObject("message_data");
                String chatType = data.optString(Constants.TAG_CHAT_TYPE, "");
                if (chatType.equals(Constants.TAG_SINGLE)) {
                    String sender_id = data.optString("sender_id", "");
                    if (!ApplicationClass.onAppForegrounded) {
                        MessagesData mdata = getMessagesByType(new JSONObject().put("message_data", data));
                        chatReceived(mdata);
                    }
                    if (dbhelper.isUserExist(sender_id)) {
                        ContactsData.Result results = dbhelper.getContactDetail(sender_id);
                        if (results != null && !results.mute_notification.equals("true")) {//User Not Blocked Notifications
                            if (ChatActivity.tempUserId != null && !ChatActivity.tempUserId.equalsIgnoreCase(sender_id))
                                showSmallNotification(data);

                        }
                    }
                } else if (chatType.equals(Constants.TAG_GROUP)) {
                    String groupId = data.optString(Constants.TAG_GROUP_ID, "");
                    String memberId = data.optString(Constants.TAG_MEMBER_ID, "");
                    String msgType = data.optString(Constants.TAG_MESSAGE_TYPE, "");
                    if (!ApplicationClass.onAppForegrounded && dbhelper.isGroupExist(groupId)) {
                        getGroupMessagesByType(new JSONObject().put("message_data", data));
                    }
                    if (!memberId.equals(GetSet.getUserId()) && dbhelper.isGroupExist(groupId) &&
                            !msgType.equals("admin") && !msgType.equals("subject") && !msgType.equals("group_image") &&
                            !msgType.equals("add_member") && !msgType.equals("remove_member") && !msgType.equals("left")) {
                        GroupData results = dbhelper.getGroupData(getApplicationContext(), groupId);
                        if (results != null && !results.muteNotification.equals("true")) {//Group Not Blocked Notifications
                            if (GroupChatActivity.tempGroupId != null && !GroupChatActivity.tempGroupId.equalsIgnoreCase(groupId))
                                showSmallNotification(data);
                        }
                    }
                } else if (chatType.equals(Constants.TAG_CALL)) {
                    String userId = data.optString("caller_id", "");
                    if (dbhelper.isUserExist(userId)) {
                        onCallReceive(data, sendTime);
                    } else {
                        getUserInfo(userId, data, sendTime);
                    }
                } else if (chatType.equals(Constants.TAG_CHANNEL)) {
                    String channelId = data.optString(Constants.TAG_CHANNEL_ID, "");
                    String msgType = data.optString(Constants.TAG_MESSAGE_TYPE, "");
                    if (!ApplicationClass.onAppForegrounded) {
                        getChannelMessagesByType(new JSONObject().put("message_data", data));
                    }
                    if (dbhelper.isChannelExist(channelId) && !msgType.equals("subject") && !msgType.equals("channel_image") &&
                            !msgType.equals("channel_des")) {
                        ChannelResult.Result results = dbhelper.getChannelInfo(channelId);
                        data.put(Constants.TAG_CHANNEL_NAME, "");
                        if (results != null && !results.muteNotification.equals("true")) {
                            data.put(Constants.TAG_CHANNEL_NAME, results.channelName);
                            if (ChannelChatActivity.tempChannelId != null && !ChannelChatActivity.tempChannelId.equalsIgnoreCase(channelId))
                                showSmallNotification(data);
                        }
                    }
                } else if (chatType.equalsIgnoreCase(Constants.TAG_GROUP_INVITATION)) {
                    String adminId = data.optString(Constants.TAG_ADMIN_ID, "");
                    if (!adminId.equalsIgnoreCase(GetSet.getUserId()))
                        showSmallNotification(data);
                } else if (chatType.equalsIgnoreCase(Constants.TAG_CHANNEL_INVITATION)) {
                    showSmallNotification(data);
                } else if (chatType.equalsIgnoreCase(Constants.TAG_ISDELETE)) {
                    showSmallNotification(data);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public MessagesData getMessagesByType(JSONObject data) {
        MessagesData mdata = new MessagesData();
        try {
            JSONObject jobj = data.getJSONObject("message_data");
            mdata.user_id = jobj.optString(Constants.TAG_SENDER_ID, "");
            mdata.message_type = jobj.optString(Constants.TAG_MESSAGE_TYPE, "");
            mdata.message = jobj.optString(Constants.TAG_MESSAGE, "");
            mdata.message_id = jobj.optString(Constants.TAG_MESSAGE_ID, "");
            mdata.attachment = jobj.optString(Constants.TAG_ATTACHMENT, "");
            mdata.chat_time = jobj.optString(Constants.TAG_CHAT_TIME, "");
            mdata.receiver_id = jobj.optString(Constants.TAG_RECEIVER_ID, "");
            mdata.sender_id = jobj.optString(Constants.TAG_SENDER_ID, "");
            mdata.lat = jobj.optString(Constants.TAG_LAT, "");
            mdata.lon = jobj.optString(Constants.TAG_LON, "");
            mdata.contact_name = jobj.optString(Constants.TAG_CONTACT_NAME, "");
            mdata.contact_phone_no = jobj.optString(Constants.TAG_CONTACT_PHONE_NO, "");
            mdata.contact_country_code = jobj.optString(Constants.TAG_CONTACT_COUNTRY_CODE, "");
            mdata.thumbnail = jobj.optString(Constants.TAG_THUMBNAIL, "");
            mdata.statusData = jobj.optString(Constants.TAG_STATUS_DATA, "");

            if (mdata.message_type != null && mdata.message_type.equals(Constants.TAG_ISDELETE)) {
                dbhelper.updateMessageData(mdata.message_id, Constants.TAG_MESSAGE_TYPE, Constants.TAG_ISDELETE);
                if (ApplicationClass.isStringNotNull(mdata.attachment)) {
                    storageManager.checkDeleteFile(mdata.attachment, mdata.message_type, "receive");
                    dbhelper.updateMessageData(mdata.message_id, Constants.TAG_ATTACHMENT, "");
                    dbhelper.updateMessageData(mdata.message_id, Constants.TAG_THUMBNAIL, "");
                }

            } else {
                dbhelper.addMessageDatas(GetSet.getUserId() + mdata.user_id, mdata.message_id, mdata.user_id, "",
                        mdata.message_type, mdata.message, mdata.attachment, mdata.lat, mdata.lon,
                        mdata.contact_name, mdata.contact_phone_no,
                        mdata.contact_country_code, mdata.chat_time, GetSet.getUserId(), mdata.user_id, "sent", mdata.thumbnail, mdata.statusData);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mdata;
    }

    void chatReceived(final MessagesData mdata) {
        Map<String, String> params = new HashMap<>();
        params.put("user_id", GetSet.getUserId());
        params.put("sender_id", mdata.sender_id);
        params.put("receiver_id", mdata.receiver_id);
        params.put("message_id", mdata.message_id);
        params.put("chat_id", mdata.receiver_id + mdata.sender_id);
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<Map<String, String>> call3 = apiInterface.chatreceived(GetSet.getToken(), params);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                try {
                    Log.v("chatReceived", "response=" + new Gson().toJson(response.body()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.v("chatReceived", "TEST" + t.getMessage());
                call.cancel();
            }
        });
    }

    private void getUserInfo(String memberId, JSONObject data, long sendTime) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<Map<String, String>> call3 = apiInterface.getuserprofile(GetSet.getToken(), GetSet.getphonenumber(), memberId);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                try {
                    Log.v(TAG, "getUserInfo: " + new Gson().toJson(response.body()));
                    Map<String, String> userdata = response.body();
                    if (userdata.get(Constants.TAG_STATUS).equals("true")) {
                        String name = userdata.get(Constants.TAG_USER_NAME);
                        HashMap<String, String> map = ApplicationClass.getContactrNot(getApplicationContext(), userdata.get(Constants.TAG_PHONE_NUMBER));
                        if (map.get("isAlready").equals("true")) {
                            name = map.get(Constants.TAG_USER_NAME);
                        }
                        dbhelper.addContactDetails(name, userdata.get(TAG_ID), userdata.get(Constants.TAG_USER_NAME), userdata.get(Constants.TAG_PHONE_NUMBER), userdata.get(Constants.TAG_COUNTRY_CODE), userdata.get(Constants.TAG_USER_IMAGE),
                                userdata.get(Constants.TAG_PRIVACY_ABOUT), userdata.get(Constants.TAG_PRIVACY_LAST_SEEN), userdata.get(Constants.TAG_PRIVACY_PROFILE), userdata.get(Constants.TAG_ABOUT), userdata.get(TAG_CONTACT_STATUS));

                        onCallReceive(data, sendTime);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.v("getUserInfo Failed", "TEST" + t.getMessage());
                call.cancel();
            }
        });
    }

    private void onCallReceive(JSONObject data, long sendTime) {
        String userId = data.optString("caller_id", "");
        String type = data.optString(Constants.TAG_TYPE, "");
        String callId = data.optString("call_id", "");
        String unixStamp = data.optString("created_at", "");
        String call_type = data.optString("call_type", "");
        String roomId = data.optString("room_id", "");
        String platform = data.optString(Constants.TAG_PLATFORM, "");
        long diffInMs = System.currentTimeMillis() - sendTime;
        long diffSeconds = diffInMs / 1000;
        Log.v("FCM", "sendTime=" + sendTime);
        Log.v("FCM", "diffInSec=" + diffSeconds);
        Log.v("FCM", "now=" + System.currentTimeMillis());

        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int isPhoneCallOn = telephony.getCallState();

        if (call_type.equalsIgnoreCase("created")) {
            dbhelper.addRecentCall(callId, userId, type, "incoming", unixStamp, "1");
            ContactsData.Result results = dbhelper.getContactDetail(userId);
            if (diffSeconds < 30 && !results.blockedbyme.equals(Constants.TAG_BLOCK) &&
                    !CallActivity.isInCall && isPhoneCallOn == 0) {
                CallActivity.isInCall = true;
                AppRTCUtils appRTCUtils = new AppRTCUtils(getApplicationContext());
                Intent intent = appRTCUtils.connectToRoom(userId, "receive", type);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("call_id", callId);
                intent.putExtra("room_id", roomId);
                intent.putExtra(Constants.TAG_PLATFORM, platform);
                startActivity(intent);
            }
        } else if (call_type.equalsIgnoreCase("ended")) {
            if (CallActivity.callActivity != null && CallActivity.userid.equals(userId)) {
                if (CallActivity.isCallAttend) {
                    CallActivity.toastText = getString(R.string.call_ended);
                } else {
                    CallActivity.toastText = getString(R.string.call_declined);
                }
                CallActivity.callActivity.finish();
            }
        }
    }

    public void showSmallNotification(JSONObject jsonObject) {
        try {
            Intent intent = null;
            String message = jsonObject.optString("message", "");
            String userName = "Hiddy", userId = "";
            CharSequence channelName = getString(R.string.app_name);
            int count = 0;


            NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel notificationChannel = new NotificationChannel(getString(R.string.notification_channel_id), channelName, importance);
                mNotifyManager.createNotificationChannel(notificationChannel);
            }

            count = addUnreadSingleNotification() + addUnreadGroupNotification() + addUnreadChannelNotification();

            if (jsonObject.get(Constants.TAG_CHAT_TYPE).equals(Constants.TAG_GROUP)) {
                String group_id = jsonObject.optString(Constants.TAG_GROUP_ID, "");
                String phone_no = jsonObject.optString(Constants.TAG_MEMBER_NO, "");
                userName = jsonObject.optString(Constants.TAG_GROUP_NAME, "");
                String name = ApplicationClass.getContactName(this, phone_no, "",phone_no);
                message = name + " : " + ApplicationClass.decryptMessage(message);
                intent = new Intent(getApplicationContext(), GroupChatActivity.class);
                intent.putExtra("group_id", group_id);
                intent.putExtra("notification", "true");
//                showOldNotify(intent,userName,message);
                // count = addUnreadGroupNotification();
            } else if (jsonObject.get(Constants.TAG_CHAT_TYPE).equals(Constants.TAG_CHANNEL)) {
                userName = jsonObject.optString(Constants.TAG_CHANNEL_NAME, "");
                String channel_id = jsonObject.optString(Constants.TAG_CHANNEL_ID, "");
                message = ApplicationClass.decryptMessage(jsonObject.optString("message", ""));

                intent = new Intent(getApplicationContext(), ChannelChatActivity.class);
                intent.putExtra("channel_id", channel_id);
                intent.putExtra("notification", "true");
                //  showOldNotify(intent,userName,message);
            } else if (jsonObject.get(Constants.TAG_CHAT_TYPE).equals(Constants.TAG_GROUP_INVITATION)) {
                userName = getLocaleString(R.string.new_group);
                String group_id = jsonObject.optString(Constants.ID, "");
                message = getLocaleString(R.string.you_added_in_group) + " " + jsonObject.optString(Constants.TAG_TITLE, "");
                intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("group_id", group_id);
                intent.putExtra(Constants.IS_FROM, "group");

                showOldNotify(intent, userName, message);
            } else if (jsonObject.get(Constants.TAG_CHAT_TYPE).equals(Constants.TAG_CHANNEL_INVITATION)) {
                userName = getLocaleString(R.string.new_channel_received);
                String channel_id = jsonObject.optString(Constants.ID, "");
                message = getLocaleString(R.string.invitation_received);
                intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("channel_id", channel_id);
                intent.putExtra(Constants.IS_FROM, "channel");
                showOldNotify(intent, userName, message);
            }


            if (count > 0) {
                int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
                if (intent == null) {
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                }
                intent.putExtra("notification", "true");
                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);


                Notification summaryNotification =
                        new NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                                .setChannelId(getString(R.string.notification_channel_id))
                                .setContentTitle(getString(R.string.app_name))
                                .setContentText("" + count + " New messages")
                                .setSmallIcon(R.drawable.notification)
                                .setContentIntent(resultPendingIntent)
                                .setStyle(new NotificationCompat.InboxStyle()
                                        .setSummaryText("" + count + " unread conversation"))
                                .setGroup(GROUP_KEY)
                                .setGroupSummary(true)
                                .build();
                showNotification(summaryNotification, 0, "hiddy");
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showOldNotify(Intent intent, String userName, String message) {
        String appName = getString(R.string.app_name);
        Random random = new Random();
        int m = random.nextInt(9999 - 1000) + 1000;
        int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
        long when = System.currentTimeMillis();

        intent.putExtra("notification", "true");
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = getString(R.string.notification_channel_id);
        CharSequence channelName = getString(R.string.app_name);

        NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            mNotifyManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId);
        mBuilder.setContentTitle(userName)
                .setChannelId(channelId)
                .setContentText(message).setTicker(appName).setWhen(when)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(resultPendingIntent)
                .setSmallIcon(R.drawable.notification)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setGroup(GROUP_KEY)
                .setAutoCancel(true);
        mNotifyManager.notify(userName, 0, mBuilder.build());
    }

    private int addUnreadSingleNotification() {
        List<String> userListId = dbhelper.getUnseenMessageUser();
        int count = 0;
        for (String s : userListId) {
            boolean isSecret = getSharedPreferences("wall", Context.MODE_PRIVATE).contains(s);
            NotificationCompat.Builder notification = createSingleNotificationBuilder();
            Intent intent = new Intent(getApplicationContext(), isSecret ? MainActivity.class : ChatActivity.class);

            intent.putExtra("user_id", s);
            intent.putExtra("isSecret",isSecret);
            intent.putExtra("notification", "true");
            notification.setContentIntent(setIntent(intent));


            String userName = "", imgUrl = "";
            long notifyId = 0;
            if (!s.equals(GetSet.getUserId())) {
                ContactsData.Result result = dbhelper.getContactDetail(s);
                if (result.phone_no != null && !result.mute_notification.equals("true")) {
                    notifyId = Long.valueOf(result.phone_no);

                    if (!result.user_name.equals("")) {
                        Person userPerson = getPerson(s, isSecret);

                        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(userPerson);

                        List<MessagesData> unSeenMessage = dbhelper.getUnseenMessage(s, getApplicationContext());
                        if (!unSeenMessage.isEmpty()) {

                            for (MessagesData messagesData : unSeenMessage) {
                                style.addMessage(getMessage(isSecret?"You have a new  message":messagesData.message, Long.parseLong(messagesData.chat_time), userPerson));
                            }
                            notification.setNumber(unSeenMessage.size());
                            notification.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);
                            notification.setWhen(Long.parseLong(unSeenMessage.get(unSeenMessage.size() - 1).chat_time) * 1000);

                            notification.setStyle(style);
                            showNotification(notification.build(), (int) notifyId, "");

                            count = count + 1;
                        }
                    }
                }

            }
        }
        return count;
    }

    private int addUnreadChannelNotification() {
        int count = 0;
        ArrayList<HashMap<String, String>> recentMsg = dbhelper.getChannelRecentMessages(getApplicationContext());
        if (recentMsg.isEmpty()) {
            return count;
        } else {
            for (HashMap<String, String> map : recentMsg) {
                if (map.get(Constants.TAG_UNREAD_COUNT) != null && Integer.parseInt(map.get(Constants.TAG_UNREAD_COUNT)) > 0
                        && !map.get(Constants.TAG_MUTE_NOTIFICATION).equals("true")) {
                    if (map.get(TAG_CHANNEL_ID) != null) {
                        NotificationCompat.Builder notification = createSingleNotificationBuilder();
                        String channelId = map.get(TAG_CHANNEL_ID);
                        Intent intent = new Intent(getApplicationContext(), ChannelChatActivity.class);
                        intent.putExtra("channel_id", channelId);
                        intent.putExtra("notification", "true");
                        BigInteger notify = new BigInteger(channelId.replaceAll("[^\\d.]", ""));
                        int notifyId = notify.intValue();
                        ChannelResult.Result channelInfo = dbhelper.getChannelInfo(channelId);
                        String name = "";
                        if (map.get(Constants.TAG_CHANNEL_CATEGORY).equalsIgnoreCase(Constants.TAG_ADMIN_CHANNEL)) {
                            name = getApplicationContext().getString(R.string.admin);
                        } else {
                            name = channelInfo.channelName;
                        }

                        if (name == null || TextUtils.isEmpty(name)) {
                            name = getString(R.string.admin);
                        }


//                        if (dbhelper.getChannelUnreadCount(channelId)) {
                        List<ChannelMessage> unSeenMessage = dbhelper.getChannelUnreadMessages(channelId);
                        Person userPerson = new Person.Builder()
                                .setName(name)
                                .setIcon(IconCompat.createWithBitmap(getCircleBitmap(Constants.CHANNEL_IMG_PATH + channelInfo.channelImage)))
                                .build();
                        if (unSeenMessage.size() > 0) {
                            NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(userPerson);
                            style.setGroupConversation(true);
                            style.setConversationTitle(channelInfo.channelName);
                            for (ChannelMessage channelMessage : unSeenMessage) {
                                if (!TextUtils.isEmpty(channelMessage.message) && userPerson.getName() != null && !TextUtils.isEmpty(userPerson.getName())) {
                                    style.addMessage(getMessage(channelMessage.message, Long.parseLong(channelMessage.chatTime), userPerson));
                                }
                            }
                            notification.setNumber(unSeenMessage.size());
                            notification.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);
                            notification.setWhen(Long.parseLong(unSeenMessage.get(unSeenMessage.size() - 1).chatTime) * 1000);
                            notification.setContentIntent(setIntent(intent));
                            notification.setStyle(style);
                            showNotification(notification.build(), (int) notifyId, "");
                            count = count + 1;
                        }
//                        }
                    }
                }
            }
        }

        return count;
    }

    private int addUnreadGroupNotification() {
        int count = 0;
        NotificationCompat.Builder notification = createSingleNotificationBuilder();
        Intent intent = new Intent(getApplicationContext(), GroupChatActivity.class);
        intent.putExtra("notification", "true");
        ArrayList<HashMap<String, String>> recentMsg = dbhelper.getGroupRecentMessages(getApplicationContext());
        if (recentMsg.isEmpty()) {
            return count;
        } else {
            for (HashMap<String, String> map : recentMsg) {
                if (map.get(TAG_GROUP_ID) != null /*&& ApplicationClass.isStringNotNull(map.get(Constants.TAG_MUTE_NOTIFICATION))*/
                        && !map.get(Constants.TAG_MUTE_NOTIFICATION).equals("true")) {
                    if (map.get(Constants.TAG_UNREAD_COUNT) != null && Integer.parseInt(map.get(Constants.TAG_UNREAD_COUNT)) > 0) {
                        List<GroupMessage> unreadMsg = dbhelper.getGroupUnreadMessages(map.get(TAG_GROUP_ID), getApplicationContext());
                        if (!unreadMsg.isEmpty() && unreadMsg.size() > 0) {
                            count = count + 1;
                            String groupId = map.get(TAG_GROUP_ID);
                            intent.putExtra("group_id", groupId);
                            BigInteger notify = new BigInteger(groupId.replaceAll("[^\\d.]", ""));
                            int notifyId = notify.intValue();

                            Person groupPerson = new Person.Builder()
                                    .setName(map.get(TAG_GROUP_NAME))
                                    .build();
                            NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(groupPerson);
                            style.setGroupConversation(true);
                            style.setConversationTitle(map.get(TAG_GROUP_NAME) + " " + getUnreadMessageString(unreadMsg.size()));
                            notification.setStyle(style);
                            for (GroupMessage message : unreadMsg) {
                                ContactsData.Result userData = getUserData(message.memberId);
                                Person userPerson = getPerson(userData.user_id, false);
                                //style = new NotificationCompat.MessagingStyle(userPerson);
                                style.addMessage(getMessage(message.message, Long.parseLong(message.chatTime), userPerson));

                            }
                            notification.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);
                            notification.setContentIntent(setIntent(intent));
                            notification.setNumber(unreadMsg.size());
                            notification.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);
                            notification.setWhen(Long.parseLong(unreadMsg.get(unreadMsg.size() - 1).chatTime) * 1000);
                            showNotification(notification.build(), (int) notifyId, map.get(TAG_GROUP_NAME));
                        }

                    }
                }
            }
        }
        return count;
    }

    private String getUnreadMessageString(int count) {
        return getApplicationContext().getString(R.string.unread_message_count, count);
    }

    private NotificationCompat.MessagingStyle getMessageStyle(String userId, String message, String chatTime) {
        Person userPerson = getPerson(userId, false);
        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(userPerson);
        style.addMessage(getMessage(message, Long.parseLong(chatTime), userPerson));
        return style;
    }

    private Bitmap getCircleBitmap(String imgUrl) {
        Bitmap bitmap;
        if (imgUrl != null && !imgUrl.equals("")) {
            bitmap = getUserBitMap(imgUrl);
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.drawable.temp);
            }
        } else {
            bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                    R.drawable.temp);
        }
        int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);

        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    private Intent getMessageReplyIntent(String label) {
        return new Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(REPLY_ACTION)
                .putExtra(KEY_PRESSED_ACTION, label);
    }

    public NotificationCompat.Builder createSingleNotificationBuilder() {


        return new NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                .setChannelId(getString(R.string.notification_channel_id))
                .setTicker(getString(R.string.app_name))
                .setSmallIcon(R.drawable.notification)
                .setGroup(GROUP_KEY)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setAutoCancel(true);
    }

    private PendingIntent setIntent(Intent intent) {
        int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);

        return PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private ContactsData.Result getUserData(String userId) {
        ContactsData.Result result = dbhelper.getContactDetail(userId);

        if (result.privacy_profile_image != null && result.privacy_profile_image.equalsIgnoreCase(TAG_MY_CONTACTS)) {
            if (result.contactstatus != null && result.contactstatus.equalsIgnoreCase(TRUE) && !result.user_image.equals("")) {
                result.user_image = Constants.USER_IMG_PATH + result.user_image;
            } else {
                result.user_image = "";
            }

        } else if (result.privacy_profile_image != null && result.privacy_profile_image.equalsIgnoreCase(TAG_NOBODY)) {
            result.user_image = "";
        } else {
            if (!result.user_image.equals(""))
                result.user_image = Constants.USER_IMG_PATH + result.user_image;
            else
                result.user_image = "";
        }
        return result;
    }

    public Person getPerson(String userId, boolean isSecret) {
        ContactsData.Result userData = getUserData(userId);
        return new Person.Builder()
                .setName(isSecret?"Unknown Contact":userData.user_name)
                .setIcon(IconCompat.createWithBitmap(isSecret?( BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.drawable.temp)):getCircleBitmap(Constants.USER_IMG_PATH + userData.user_image)))
                .build();
    }

    public NotificationCompat.MessagingStyle.Message getMessage(String message, Long time, Person sender/*,String imgUrl*/) {
        return new NotificationCompat.MessagingStyle.Message(message, time, sender);
    }

    private NotificationCompat.InboxStyle inboxStyle(List<GroupMessage> messages) {
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (GroupMessage message : messages) {
            if (messages.indexOf(message) > 6) {
                break;
            }
            style.addLine(message.memberName + ":" + message.message);
        }
        return style;
    }

    public Bitmap getUserBitMap(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return BitmapFactory.decodeResource(getApplicationContext().getResources(),
                    R.drawable.temp);
        }
    }


    private void showNotification(Notification notification, int id, String tag) {
        if (!ApplicationClass.onAppForegrounded) {
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(id, notification);
        // NotificationManagerCompat.from(this).notify(id, notification);
    }

    public String getLocaleString(int stringId) {
        Configuration configuration = new Configuration(getApplicationContext().getResources().getConfiguration());
        configuration.setLocale(new Locale(pref.getString(Constants.TAG_LANGUAGE_CODE, Constants.TAG_DEFAULT_LANGUAGE_CODE)));
        return getApplicationContext().createConfigurationContext(configuration).getResources().getString(stringId);
    }

    private GroupMessage getGroupMessagesByType(JSONObject data) {
        GroupMessage mdata = new GroupMessage();
        try {
            JSONObject jobj = data.getJSONObject("message_data");
            mdata.groupId = jobj.optString(TAG_GROUP_ID, "");
            mdata.groupName = jobj.optString(TAG_GROUP_NAME, "");
            mdata.memberId = jobj.optString(Constants.TAG_MEMBER_ID, "");
            mdata.memberName = jobj.optString(Constants.TAG_MEMBER_NAME, "");
            mdata.memberNo = jobj.optString(Constants.TAG_MEMBER_NO, "");
            mdata.messageType = jobj.optString(Constants.TAG_MESSAGE_TYPE, "");
            mdata.message = jobj.optString(Constants.TAG_MESSAGE, "");
            mdata.messageId = jobj.optString(Constants.TAG_MESSAGE_ID, "");
            mdata.chatTime = jobj.optString(Constants.TAG_CHAT_TIME, "");
            mdata.attachment = jobj.optString(Constants.TAG_ATTACHMENT, "");
            mdata.thumbnail = jobj.optString(Constants.TAG_THUMBNAIL, "");
            mdata.lat = jobj.optString(Constants.TAG_LAT, "");
            mdata.lon = jobj.optString(Constants.TAG_LON, "");
            mdata.contactName = jobj.optString(Constants.TAG_CONTACT_NAME, "");
            mdata.contactPhoneNo = jobj.optString(Constants.TAG_CONTACT_PHONE_NO, "");
            mdata.contactCountryCode = jobj.optString(Constants.TAG_CONTACT_COUNTRY_CODE, "");
            mdata.groupAdminId = jobj.optString(TAG_GROUP_ADMIN_ID, "");

            switch (mdata.messageType) {
                case "subject":
                    dbhelper.updateGroupData(mdata.groupId, Constants.TAG_GROUP_NAME, mdata.groupName);
                    break;
                case "group_image":
                    dbhelper.updateGroupData(mdata.groupId, Constants.TAG_GROUP_IMAGE, mdata.attachment);
                    break;
                case "add_member":
                    if (!mdata.attachment.equals("")) {
                        JSONArray jsonArray = new JSONArray(mdata.attachment);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String memberId = jsonObject.getString(Constants.TAG_MEMBER_ID);
                            String memberRole = jsonObject.getString(Constants.TAG_MEMBER_ROLE);
                            if (!dbhelper.isUserExist(memberId)) {
                                String memberKey = mdata.groupId + jsonObject.getString(TAG_MEMBER_ID);
                                getUserData(memberKey, mdata.groupId, memberId, memberRole);
                            } else {
                                String memberKey = mdata.groupId + jsonObject.getString(TAG_MEMBER_ID);
                                dbhelper.updateGroupMembers(memberKey, mdata.groupId, memberId, memberRole);
                            }
                        }
                    }
                    break;
                case "left":
                case "remove_member":
                    if (dbhelper.isUserExist(mdata.memberId))
                        dbhelper.deleteFromGroup(mdata.groupId, mdata.memberId);
                    break;
                case "admin":
                    if (!dbhelper.isUserExist(mdata.memberId)) {
                        String memberKey = mdata.groupId + mdata.memberId;
                        getUserData(memberKey, mdata.groupId, mdata.memberId, mdata.attachment);
                    } else {
                        String memberKey = mdata.groupId + mdata.memberId;
                        dbhelper.updateGroupMembers(memberKey, mdata.groupId, mdata.memberId, mdata.attachment);
                    }
                    break;
                case "change_number":
                    dbhelper.updateContactInfo(mdata.memberId, Constants.TAG_COUNTRY_CODE, mdata.contactCountryCode);
                    dbhelper.updateContactInfo(mdata.memberId, Constants.TAG_PHONE_NUMBER, mdata.contactPhoneNo);
                    break;
                case Constants.TAG_ISDELETE:
                    dbhelper.updateGroupMessageData(mdata.messageId, Constants.TAG_MESSAGE_TYPE, Constants.TAG_ISDELETE);
                    if (ApplicationClass.isStringNotNull(mdata.attachment)) {
                        storageManager.checkDeleteFile(mdata.attachment, mdata.messageType, "receive");
                        dbhelper.updateMessageData(mdata.messageId, Constants.TAG_ATTACHMENT, "");
                        dbhelper.updateMessageData(mdata.messageId, Constants.TAG_THUMBNAIL, "");
                    }
                    break;
                default:
                    dbhelper.addGroupMessages(mdata.messageId, mdata.groupId, mdata.memberId, mdata.groupAdminId, mdata.messageType,
                            mdata.message, mdata.attachment, mdata.lat, mdata.lon, mdata.contactName, mdata.contactPhoneNo,
                            mdata.contactCountryCode, mdata.chatTime, mdata.thumbnail, "");

                    int unseenCount = dbhelper.getUnseenGroupMessagesCount(mdata.groupId);
                    dbhelper.addGroupRecentMsgs(mdata.groupId, mdata.messageId,
                            mdata.memberId, mdata.chatTime, String.valueOf(unseenCount));
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mdata;
    }

    private void getUserData(String memberKey, String groupId, String memberId, String memberRole) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<Map<String, String>> call3 = apiInterface.getuserprofile(GetSet.getToken(), GetSet.getphonenumber(), memberId);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                try {
                    Log.v(TAG, "getUserData: " + new Gson().toJson(response.body()));
                    Map<String, String> userdata = response.body();
                    if (response.isSuccessful() && userdata.get(Constants.TAG_STATUS).equals("true")) {
                        String name = userdata.get(Constants.TAG_USER_NAME);
                        HashMap<String, String> map = ApplicationClass.getContactrNot(getApplicationContext(), userdata.get(Constants.TAG_PHONE_NUMBER));
                        if (map.get("isAlready").equals("true")) {
                            name = map.get(Constants.TAG_USER_NAME);
                        }
                        dbhelper.addContactDetails(name, userdata.get(TAG_ID), userdata.get(Constants.TAG_USER_NAME), userdata.get(Constants.TAG_PHONE_NUMBER), userdata.get(Constants.TAG_COUNTRY_CODE), userdata.get(Constants.TAG_USER_IMAGE),
                                userdata.get(Constants.TAG_PRIVACY_ABOUT), userdata.get(Constants.TAG_PRIVACY_LAST_SEEN), userdata.get(Constants.TAG_PRIVACY_PROFILE), userdata.get(Constants.TAG_ABOUT), userdata.get(TAG_CONTACT_STATUS));
                        dbhelper.createGroupMembers(memberKey, groupId, memberId, memberRole);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.v(TAG, "getUserData Failed" + t.getMessage());
                call.cancel();
            }
        });
    }

    private void getChannelMessagesByType(JSONObject data) {
        ChannelMessage mdata = new ChannelMessage();
        AdminChannelMsg.Result chData = new AdminChannelMsg().new Result();
        String messageFrom;
        try {
            JSONObject jobj = data.getJSONObject("message_data");
            if (jobj.has(Constants.TAG_MESSAGE_ID)) {
                messageFrom = "channel";
                mdata.channelId = jobj.optString(TAG_CHANNEL_ID, "");
                mdata.channelAdminId = jobj.optString(TAG_ADMIN_ID, "");
                mdata.channelName = jobj.optString(TAG_CHANNEL_NAME, "");
                mdata.messageType = jobj.optString(Constants.TAG_MESSAGE_TYPE, "");
                mdata.chatType = jobj.optString(Constants.TAG_CHAT_TYPE, "");
                mdata.message = jobj.optString(Constants.TAG_MESSAGE, "");
                mdata.messageId = jobj.optString(Constants.TAG_MESSAGE_ID, "");
                mdata.chatTime = jobj.optString(Constants.TAG_CHAT_TIME, "");
                mdata.attachment = jobj.optString(Constants.TAG_ATTACHMENT, "");
                mdata.thumbnail = jobj.optString(Constants.TAG_THUMBNAIL, "");
                mdata.lat = jobj.optString(Constants.TAG_LAT, "");
                mdata.lon = jobj.optString(Constants.TAG_LON, "");
                mdata.contactName = jobj.optString(Constants.TAG_CONTACT_NAME, "");
                mdata.contactPhoneNo = jobj.optString(Constants.TAG_CONTACT_PHONE_NO, "");
                mdata.contactCountryCode = jobj.optString(Constants.TAG_CONTACT_COUNTRY_CODE, "");
            } else {
                messageFrom = "admin_channel";
                chData.channelId = jobj.optString(TAG_CHANNEL_ID, "");
                chData.messageType = jobj.optString(Constants.TAG_MESSAGE_TYPE, "");
                chData.message = jobj.optString(Constants.TAG_MESSAGE, "");
                chData.messageId = jobj.optString(Constants.TAG_ID, "");
                chData.messageDate = jobj.optString("message_date", "");
                chData.chatTime = jobj.optString("message_at", "");
                chData.attachment = jobj.optString(Constants.TAG_ATTACHMENT, "");
                chData.thumbnail = jobj.optString(Constants.TAG_THUMBNAIL, "");
                chData.chatType = jobj.optString(Constants.TAG_CHAT_TYPE, "");
                chData.progress = jobj.optString(Constants.TAG_PROGRESS, "");
            }
            if (messageFrom.equals("channel")) {
                if (!dbhelper.isChannelExist(mdata.channelId)) {
                    getChannelInfo(mdata.channelId);
                }
            } else {
                if (!dbhelper.isChannelExist(chData.channelId)) {
                    getAdminChannels();
                }
            }

            if (messageFrom.equals("channel")) {
                switch (mdata.messageType) {
                    case "subject":
                        dbhelper.updateChannelData(mdata.channelId, Constants.TAG_CHANNEL_NAME, mdata.channelName);
                        break;
                    case "channel_image":
                        dbhelper.updateChannelData(mdata.channelId, Constants.TAG_CHANNEL_IMAGE, mdata.attachment);
                        break;
                    case "channel_des":
                        dbhelper.updateChannelData(mdata.channelId, Constants.TAG_CHANNEL_DES, mdata.message);
                        break;
                    case Constants.TAG_ISDELETE:
                        dbhelper.updateChannelData(mdata.channelId, Constants.TAG_MESSAGE_TYPE, Constants.TAG_ISDELETE);
                        if (ApplicationClass.isStringNotNull(mdata.attachment)) {
                            storageManager.checkDeleteFile(mdata.attachment, mdata.messageType, "receive");
                            dbhelper.updateMessageData(mdata.messageId, Constants.TAG_ATTACHMENT, "");
                            dbhelper.updateMessageData(mdata.messageId, Constants.TAG_THUMBNAIL, "");
                        }
                        break;
                    default:
                        dbhelper.addChannelMessages(mdata.channelId, mdata.chatType, mdata.messageId, mdata.messageType, mdata.message,
                                mdata.attachment, mdata.lat, mdata.lon, mdata.contactName, mdata.contactPhoneNo,
                                mdata.contactCountryCode, mdata.chatTime, mdata.thumbnail, "");
                        int unseenCount = dbhelper.getUnseenChannelMessagesCount(mdata.channelId);
                        dbhelper.addChannelRecentMsgs(mdata.channelId, mdata.messageId,
                                mdata.chatTime, String.valueOf(unseenCount));
                        break;
                }
            } else {
                switch (chData.messageType) {
                    case "subject":
//                        dbhelper.updateChannelData(chData.channelId, Constants.TAG_CHANNEL_NAME, chData.channelName);
                        break;
                    case "channel_image":
                        dbhelper.updateChannelData(chData.channelId, Constants.TAG_CHANNEL_IMAGE, chData.attachment);
                        break;
                    case "channel_des":
                        dbhelper.updateChannelData(chData.channelId, Constants.TAG_CHANNEL_DES, chData.message);
                        break;
                    case Constants.TAG_ISDELETE:
                        dbhelper.updateChannelData(chData.channelId, Constants.TAG_MESSAGE_TYPE, Constants.TAG_ISDELETE);
                        if (ApplicationClass.isStringNotNull(chData.attachment)) {
                            storageManager.checkDeleteFile(chData.attachment, chData.messageType, "receive");
                            dbhelper.updateMessageData(chData.messageId, Constants.TAG_ATTACHMENT, "");
                            dbhelper.updateMessageData(chData.messageId, Constants.TAG_THUMBNAIL, "");
                        }
                        break;
                    default:
                        if (dbhelper.isChannelExist(chData.channelId)) {
                            dbhelper.addChannelMessages(chData.channelId, chData.chatType, chData.messageId, chData.messageType,
                                    ApplicationClass.encryptMessage(chData.message), chData.attachment, "", "", "", "", "",
                                    chData.chatTime, chData.thumbnail, "");

                            int unseenCount = dbhelper.getUnseenChannelMessagesCount(chData.channelId);
                            dbhelper.addChannelRecentMsgs(chData.channelId, chData.messageId, chData.chatTime, "" + unseenCount);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getChannelInfo(String channelId) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(channelId);
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
//        Log.i(TAG, "initChannel: " + jsonArray);
        Call<ChannelResult> call = apiInterface.getChannelInfo(GetSet.getToken(), jsonArray);
        call.enqueue(new Callback<ChannelResult>() {
            @Override
            public void onResponse(Call<ChannelResult> call, Response<ChannelResult> response) {
                Log.i(TAG, "getChannelInfo: " + new Gson().toJson(response.body()));
                if (response.body().status != null && response.body().status.equalsIgnoreCase(TRUE)) {
                    for (ChannelResult.Result result : response.body().result) {
                        dbhelper.addChannel(result.channelId, result.channelName, result.channelDes, result.channelImage,
                                result.channelType, result.channelAdminId, result.channelAdminName,
                                result.totalSubscribers, result.createdAt, Constants.TAG_USER_CHANNEL,
                                "", result.blockStatus, result.report);
                    }
                }
            }

            @Override
            public void onFailure(Call<ChannelResult> call, Throwable t) {
                Log.e(TAG, "getChannelInfo: " + t.getMessage());
                call.cancel();
            }
        });
    }

    private void getAdminChannels() {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<AdminChannel> call3 = apiInterface.getAdminChannels(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<AdminChannel>() {
            @Override
            public void onResponse(Call<AdminChannel> call, Response<AdminChannel> response) {
                Log.v(TAG, "getAdminChannels: " + new Gson().toJson(response.body()));
                if (response.isSuccessful() && response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                    /*Get Channels from Admin*/
                    for (AdminChannel.Result result : response.body().result) {
                        if (!dbhelper.isChannelExist(result.channelId)) {
                            dbhelper.addChannel(result.channelId, result.channelName, result.channelDes,
                                    "", Constants.TAG_PUBLIC, "", "",
                                    "", result.createdTime, Constants.TAG_ADMIN_CHANNEL, "", "", "0");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<AdminChannel> call, Throwable t) {
                Log.e(TAG, "getAdminChannels: " + t.getMessage());
                call.cancel();
            }
        });
    }
}