package com.loopytime.helper;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.loopytime.external.RandomString;
import com.loopytime.im.ApplicationClass;
import com.loopytime.im.R;
import com.loopytime.im.WelcomeActivity;
import com.loopytime.model.AdminChannel;
import com.loopytime.model.AdminChannelMsg;
import com.loopytime.model.BlocksData;
import com.loopytime.model.CallData;
import com.loopytime.model.ChannelChatResult;
import com.loopytime.model.ChannelMessage;
import com.loopytime.model.ChannelResult;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupChatResult;
import com.loopytime.model.GroupData;
import com.loopytime.model.GroupInvite;
import com.loopytime.model.GroupMessage;
import com.loopytime.model.MessagesData;
import com.loopytime.model.RecentsData;
import com.loopytime.model.SaveMyContacts;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.READ_CONTACTS;
import static com.loopytime.utils.Constants.TAG_CONTACT_STATUS;
import static com.loopytime.utils.Constants.TAG_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER_ID;

/**
 * Created by hitasoft on 5/7/18.
 */

public class ForegroundService extends Service {

    private static final String TAG = ForegroundService.class.getSimpleName();
    public static boolean IS_SERVICE_RUNNING = false;
    Thread recentChatThread, getBlockThread, checkDevice, groupInvitesThread, recenGroupChatThread, saveContacts,
            recentCallThread, recentChannelChatThread, adminChannelThread, channelInvitesThread;
    ApiInterface apiInterface;
    DatabaseHandler dbhelper;
    SocketConnection socketConnection;
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    int count = 10;
    Context context = this;
    List<String> myContacts = new ArrayList<>();
    JsonArray contactsNumJson = new JsonArray();
    Gson gson = new GsonBuilder().create();

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");
        super.onCreate();
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = getString(R.string.notification_channel_foreground_service);
        CharSequence channelName = getString(R.string.app_name);
        int importance = NotificationManager.IMPORTANCE_LOW;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            notificationChannel.enableVibration(false);
            notificationChannel.setSound(null, null);
            mNotifyManager.createNotificationChannel(notificationChannel);
        }
        mBuilder = new NotificationCompat.Builder(this, channelId);
        mBuilder.setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.checking_new_messages))
                .setSmallIcon(R.drawable.notification);
        startForeground(1, mBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction().equals("start")) {
            IS_SERVICE_RUNNING = true;
            Log.v(TAG, "Received Start Foreground Intent ");
            //Toast.makeText(this, "Service Started!", Toast.LENGTH_SHORT).show();
            apiInterface = ApiClient.getClient().create(ApiInterface.class);
            dbhelper = DatabaseHandler.getInstance(this);
            socketConnection = SocketConnection.getInstance(this);

            getBlockThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    getblockstatus();
                }
            });
            getBlockThread.start();

            saveContacts = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (checkCallPermission()) {
                        new GetContactTask().execute();
                    }
                }
            });
            saveContacts.start();

            checkDevice = new Thread(new Runnable() {
                @Override
                public void run() {
                    checkDeviceInfo();
                }
            });
            checkDevice.start();

            recentChatThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    recentchats();
                }
            });
            recentChatThread.start();

            groupInvitesThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    getGroupInvites();
                }
            });
            groupInvitesThread.start();

            recenGroupChatThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    recentgroupchats();
                }
            });
            recenGroupChatThread.start();

            recentCallThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    recentCalls();
                }
            });
            recentCallThread.start();

            recentChannelChatThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    getRecentChannelChats();
                }
            });
            recentChannelChatThread.start();

            adminChannelThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    getAdminChannels();
                }
            });
            adminChannelThread.start();


            channelInvitesThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    getChannelInvites();
                }
            });
            channelInvitesThread.start();

        } else if (intent == null || intent.getAction().equals("stop")) {
            IS_SERVICE_RUNNING = false;
            Log.i(TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    private void stopService() {
        count--;
        Log.v("service", "count=" + count);
        if (count == 0) {
            IS_SERVICE_RUNNING = false;
            stopForeground(true);
            stopSelf();
        }
    }

    void recentchats() {
        Call<RecentsData> call3 = apiInterface.recentchats(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<RecentsData>() {
            @Override
            public void onResponse(Call<RecentsData> call, Response<RecentsData> response) {
                try {
                    Log.v(TAG, "recentChatsResponse: " + gson.toJson(response.body()));
                    RecentsData data = response.body();
                    if (data.status.equals("true")) {
                        ArrayList<MessagesData> result = data.result;
                        for (int i = 0; i < result.size(); i++) {
                            MessagesData mdata = result.get(i);
                            if (mdata.user_id != null) {
                                dbhelper.addMessageDatas(GetSet.getUserId() + mdata.user_id, mdata.message_id, mdata.user_id, "",
                                        mdata.message_type, mdata.message, mdata.attachment, mdata.lat, mdata.lon, mdata.contact_name, mdata.contact_phone_no,
                                        mdata.contact_country_code, mdata.chat_time, GetSet.getUserId(), mdata.user_id, "sent", mdata.thumbnail, mdata.statusData);

                                if (!dbhelper.isUserExist(mdata.user_id)) {
                                    getuserprofile(mdata);
                                } else {
                                    setMessagesnListener(mdata);
                                }
                            }
                        }
                        socketConnection.setRecentListener();
                    } else {

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopService();
            }

            @Override
            public void onFailure(Call<RecentsData> call, Throwable t) {
                Log.v("Contacts Failed", "TEST" + t.getMessage());
                call.cancel();
                stopService();
            }
        });
    }

    private void setMessagesnListener(MessagesData mdata) {
        try {
            int unseenCount = dbhelper.getUnseenMessagesCount(mdata.user_id);
            Log.v("unseenCount", "unseenCount=" + unseenCount);
            dbhelper.addRecentMessages(GetSet.getUserId() + mdata.user_id, mdata.user_id, mdata.message_id, mdata.chat_time, String.valueOf(unseenCount));

            // To acknowledge the message has been delivered
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.TAG_SENDER_ID, mdata.user_id);
            jsonObject.put(Constants.TAG_RECEIVER_ID, GetSet.getUserId());
            jsonObject.put(Constants.TAG_MESSAGE_ID, mdata.message_id);
            Log.v("chatreceivedFore", "=" + jsonObject);
            socketConnection.chatReceived(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void getuserprofile(final MessagesData mdata) {

        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<Map<String, String>> call3 = apiInterface.getuserprofile(GetSet.getToken(), GetSet.getphonenumber(), mdata.user_id);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                try {
                    Log.v("response", "response=" + gson.toJson(response.body()));
                    Map<String, String> userdata = response.body();
                    if (userdata.get(Constants.TAG_STATUS).equals("true")) {

                        String name = userdata.get(Constants.TAG_USER_NAME);
                        HashMap<String, String> map = ApplicationClass.getContactrNot(getApplicationContext(), userdata.get(Constants.TAG_PHONE_NUMBER));
                        if (map.get("isAlready").equals("true")) {
                            name = map.get(Constants.TAG_USER_NAME);
                        }
                        dbhelper.addContactDetails(name, userdata.get(Constants.TAG_ID), userdata.get(Constants.TAG_USER_NAME), userdata.get(Constants.TAG_PHONE_NUMBER), userdata.get(Constants.TAG_COUNTRY_CODE), userdata.get(Constants.TAG_USER_IMAGE),
                                userdata.get(Constants.TAG_PRIVACY_ABOUT), userdata.get(Constants.TAG_PRIVACY_LAST_SEEN), userdata.get(Constants.TAG_PRIVACY_PROFILE), userdata.get(Constants.TAG_ABOUT), userdata.get(Constants.TAG_CONTACT_STATUS));

                        setMessagesnListener(mdata);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.v("Contacts Failed", "TEST" + t.getMessage());
                call.cancel();
            }
        });

    }

    @SuppressLint("StaticFieldLeak")
    private class GetContactTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Uri uri = null;
            uri = ContactsContract.CommonDataKinds.Contactables.CONTENT_URI;
            ContentResolver cr = getContentResolver();
            Cursor cur = cr.query(uri, Constants.PROJECTION, Constants.SELECTION, Constants.SELECTION_ARGS, null);

            if (cur != null) {
                try {
                    final int numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    final int nameIndex = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    String countryCode = tm.getNetworkCountryIso();
                    while (cur.moveToNext()) {
                        String phoneNo = cur.getString(numberIndex).replace(" ", "");
                        String name = cur.getString(nameIndex);
                        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                        try {
                            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNo, countryCode.toUpperCase());
                            if (numberProto != null && !phoneNo.equals("") && phoneNo.length() > 6 && phoneUtil.isPossibleNumberForType(numberProto, PhoneNumberUtil.PhoneNumberType.MOBILE)) {
                                String tempNo = ("" + numberProto.getNationalNumber()).replaceAll("[^0-9]", "");
                                if (tempNo.startsWith("0")) {
                                    tempNo = tempNo.replaceFirst("^0+(?!$)", "");
                                }
                                if (!myContacts.contains(tempNo)) {
                                    myContacts.add(tempNo.replaceAll("[^0-9]", ""));
                                    contactsNumJson.add(tempNo.replaceAll("[^0-9]", ""));
                                }
                            }
                        } catch (NumberParseException e) {
                            e.printStackTrace();
                            phoneNo = phoneNo.replaceAll("[^0-9]", "");
                            if (isValidPhoneNumber(phoneNo)) {
                                if (phoneNo.startsWith("0")) {
                                    phoneNo = phoneNo.replaceFirst("^0+(?!$)", "");
                                }
                                if (!myContacts.contains(phoneNo))
                                myContacts.add(phoneNo.replaceAll("[^0-9]", ""));
                            }
                        }
                    }
                } finally {
                    cur.close();
                }
            }
            Log.e(TAG, "getContactList: " + myContacts.size());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            saveMyContacts();
        }
    }

    public void getMyContacts() {
        Uri uri = null;
        uri = ContactsContract.CommonDataKinds.Contactables.CONTENT_URI;
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(uri, Constants.PROJECTION, Constants.SELECTION, Constants.SELECTION_ARGS, null);

        if (cur != null) {
            try {
                final int nameIndex = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                final int numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                while (cur.moveToNext()) {
                    String phoneNo = cur.getString(numberIndex).replace(" ", "");
                    String name = cur.getString(nameIndex);

                    try {
                        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                        Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNo, Locale.getDefault().getCountry());
                        if (phoneNo != null && !phoneNo.equals("") && phoneNo.length() > 6 && phoneUtil.isPossibleNumberForType(numberProto, PhoneNumberUtil.PhoneNumberType.MOBILE)) {
                            String tempNo = ("" + numberProto.getNationalNumber());
                            tempNo = tempNo.replaceAll("[^0-9]", "");
                            if (tempNo.startsWith("0")) {
                                Log.v("MyContacts df ",tempNo);
                                tempNo = tempNo.replaceFirst("^0+(?!$)", "");
                            }
                            myContacts.add(tempNo.replaceAll("[^0-9]", ""));
                        }
                    } catch (NumberParseException e) {
                        if (isValidPhoneNumber(phoneNo)) {
                            if (phoneNo.startsWith("0")) {
                                Log.v("MyContacts dfss ",phoneNo);
                                phoneNo = phoneNo.replaceFirst("^0+(?!$)", "");
                            }
//                            Log.v("Name", "excep name=" + name);
                            myContacts.add(phoneNo.replaceAll("[^0-9]", ""));
                        }
                    }
                }
            } finally {
                cur.close();
            }
        }
    }

    void saveMyContacts() {
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_USER_ID, GetSet.getUserId());
        //map.put(Constants.TAG_CONTACTS, "" + myContacts);
        map.put(Constants.TAG_CONTACTS, contactsNumJson.toString().replaceAll(", 0",", "));
//        Log.v("ContactsSave", "saveMyContacts=" + myContacts);
        Call<SaveMyContacts> call = apiInterface.saveMyContacts(GetSet.getToken(), map);
        Log.e(TAG, "saveMyContacts: " + map.toString());
        call.enqueue(new Callback<SaveMyContacts>() {
            @Override
            public void onResponse(Call<SaveMyContacts> call, Response<SaveMyContacts> response) {
                updateMyContacts();
            }

            @Override
            public void onFailure(Call<SaveMyContacts> call, Throwable t) {
                Log.e(TAG, "saveMyContacts: " + t.getMessage());
                call.cancel();
                stopService();
            }
        });

    }

    public boolean isValidPhoneNumber(CharSequence target) {
        if (target.length() < 7 || target.length() > 15) {
            return false;
        } else {
            return android.util.Patterns.PHONE.matcher(target).matches();
        }
    }

    void updateMyContacts() {
        List<String> contacts = new ArrayList<>();
        contacts = dbhelper.getAllContactsNumber(this);
        for (String contact : contacts) {
            if (!myContacts.contains(contact)) {
                myContacts.add(contact.replaceAll("[^0-9]", ""));
                contactsNumJson.add(contact.replaceAll("[^0-9]", ""));
            }
        }
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_USER_ID, GetSet.getUserId());
        map.put(Constants.TAG_CONTACTS, contactsNumJson.toString().replaceAll(", 0",", "));
        map.put(Constants.TAG_PHONE_NUMBER, GetSet.getphonenumber());
        Log.v(TAG, "updateMyContacts=" + map.toString());
        Call<ContactsData> call3 = apiInterface.updatemycontacts(GetSet.getToken(), map);
        call3.enqueue(new Callback<ContactsData>() {
            @Override
            public void onResponse(Call<ContactsData> call, Response<ContactsData> response) {
                try {
                    Log.i(TAG, "updateMyContacts: " + gson.toJson(response.body()));
                    ContactsData data = response.body();
                    if (response.isSuccessful() && data.status.equals("true")) {
                        for (ContactsData.Result result : data.result) {

                            String name = result.user_name;
                            HashMap<String, String> map = ApplicationClass.getContactrNot(getApplicationContext(), result.phone_no);
                            if (map.get("isAlready").equals("true")) {
                                name = map.get(Constants.TAG_USER_NAME);
                            }
                            dbhelper.addContactDetails(name, result.user_id, result.user_name, result.phone_no, result.country_code, result.user_image, result.privacy_about,
                                    result.privacy_last_seen, result.privacy_profile_image, result.about, result.contactstatus);

                        }
                        socketConnection.setRecentListener();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopService();
            }

            @Override
            public void onFailure(Call<ContactsData> call, Throwable t) {
//                Log.e(TAG, "updatemycontacts: " + t.getMessage());
                call.cancel();
                stopService();
            }
        });

    }

    void getblockstatus() {
        Call<BlocksData> call3 = apiInterface.getblockstatus(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<BlocksData>() {
            @Override
            public void onResponse(Call<BlocksData> call, Response<BlocksData> response) {
                try {
                    Log.v("response", "response=" + gson.toJson(response.body()));
                    BlocksData data = response.body();
                    if (data.status != null && data.status.equals("true")) {
                        ArrayList<BlocksData.Blockedme> blockedme = data.blockedme;
                        if (blockedme.size() == 0) {
                            dbhelper.resetAllBlockStatus(Constants.TAG_BLOCKED_ME);
                        } else {
                            for (int i = 0; i < blockedme.size(); i++) {
                                BlocksData.Blockedme block = blockedme.get(i);
                                dbhelper.updateBlockStatus(block.user_id, Constants.TAG_BLOCKED_ME, "block");
                            }
                        }

                        ArrayList<BlocksData.Blockedbyme> blockedbyme = data.blockedbyme;
                        if (blockedbyme.size() == 0) {
                            dbhelper.resetAllBlockStatus(Constants.TAG_BLOCKED_BYME);
                        } else {
                            for (int i = 0; i < blockedbyme.size(); i++) {
                                BlocksData.Blockedbyme block = blockedbyme.get(i);
                                dbhelper.updateBlockStatus(block.buser_id, Constants.TAG_BLOCKED_BYME, "block");
                            }
                        }
                    } else {

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                stopService();
            }

            @Override
            public void onFailure(Call<BlocksData> call, Throwable t) {
                Log.v("Contacts Failed", "TEST" + t.getMessage());
                call.cancel();
                stopService();
            }
        });
    }

    void checkDeviceInfo() {
        final String deviceId = android.provider.Settings.Secure.getString(getApplicationContext().getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        Map<String, String> map = new HashMap<>();
        map.put("user_id", GetSet.getUserId());
        map.put("device_id", deviceId);
        Log.v("checkDeviceInfo", "Params- " + map);
        Log.v("checkDeviceInfo", "Params- " + GetSet.getToken());
        Call<Map<String, String>> call3 = apiInterface.deviceinfo(GetSet.getToken(), map);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                Map<String, String> data = response.body();
                Log.v("checkDeviceInfo:", "response- " + data);
                if (data.get(Constants.TAG_STATUS).equals("false")) {
                    GetSet.logout();
                    SharedPreferences settings = getSharedPreferences("SavedPref", Context.MODE_PRIVATE);
                    settings.edit().clear().commit();
                    Intent logout = new Intent(getApplicationContext(), WelcomeActivity.class);
                    logout.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(logout);
                }
                stopService();
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                call.cancel();
                stopService();
            }
        });

    }

    private void getGroupInvites() {
        final ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<GroupInvite> call3 = apiInterface.getGroupInvites(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<GroupInvite>() {
            @Override
            public void onResponse(Call<GroupInvite> call, Response<GroupInvite> response) {
                try {
                    Log.v("GroupInvite", "GroupInvite=" + gson.toJson(response.body()));
                    GroupInvite userdata = response.body();
                    if (userdata.status.equalsIgnoreCase(Constants.TRUE)) {

                        for (GroupData groupData : userdata.result) {
                            if (!dbhelper.isGroupExist(groupData.groupId)) {
                                dbhelper.createGroup(groupData.groupId, groupData.groupAdminId,
                                        groupData.groupName, groupData.createdAt, groupData.groupImage);

                                for (GroupData.GroupMembers groupMember : groupData.groupMembers) {
                                    if (!dbhelper.isUserExist(groupMember.memberId)) {
                                        String memberKey = groupData.groupId + groupMember.memberId;
                                        getUserData(memberKey, groupData.groupId, groupMember.memberId, groupMember.memberRole);
                                    } else {
                                        String memberKey = groupData.groupId + groupMember.memberId;
                                        dbhelper.createGroupMembers(memberKey, groupData.groupId, groupMember.memberId,
                                                groupMember.memberRole);
                                    }
                                }

                                String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                                RandomString randomString = new RandomString(10);
                                String messageId = groupData.groupId + randomString.nextString();

                                dbhelper.addGroupMessages(messageId, groupData.groupId, GetSet.getUserId(), groupData.groupAdminId, "create_group",
                                        "", "", "", "", "", "", "",
                                        groupData.createdAt, "", "");
                                int unseenCount = dbhelper.getUnseenGroupMessagesCount(groupData.groupId);
                                dbhelper.addGroupRecentMsgs(groupData.groupId, messageId, GetSet.getUserId(), unixStamp, "" + unseenCount);

                                if (!groupData.groupAdminId.equals(GetSet.getUserId())) {
                                    String unixStamp2 = String.valueOf(System.currentTimeMillis() / 1000L);
                                    String messageId2 = groupData.groupId + randomString.nextString();
                                    dbhelper.addGroupMessages(messageId2, groupData.groupId, GetSet.getUserId(), groupData.groupAdminId, "add_member",
                                            "", "", "", "",
                                            "", "", "", groupData.createdAt, "", "");
                                    unseenCount = dbhelper.getUnseenGroupMessagesCount(groupData.groupId);
                                    dbhelper.addGroupRecentMsgs(groupData.groupId, messageId, GetSet.getUserId(), unixStamp, "" + unseenCount);
                                }
                            }
                            try {
                                JSONObject jobj = new JSONObject();
                                jobj.put(Constants.TAG_GROUP_ID, groupData.groupId);
                                jobj.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                                socketConnection.joinGroup(jobj);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        socketConnection.setRecentGroupListener();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopService();
            }

            @Override
            public void onFailure(Call<GroupInvite> call, Throwable t) {
                Log.e(TAG, "getGroupInvites " + t.getMessage());
                call.cancel();
                stopService();
            }
        });
    }

    void recentgroupchats() {
        Call<GroupChatResult> call3 = apiInterface.getRecentGroupChats(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<GroupChatResult>() {
            @Override
            public void onResponse(Call<GroupChatResult> call, Response<GroupChatResult> response) {
                try {
                    Log.v(TAG, "recentGroupChats=" + gson.toJson(response.body()));
                    GroupChatResult data = response.body();
                    if (data.status.equals("true")) {
                        List<GroupMessage> result = data.result;
                        for (int g = 0; g < result.size(); g++) {
                            GroupMessage mdata = result.get(g);
                            if (mdata.memberId != null) {

                                switch (mdata.messageType) {
                                    case "subject":
                                        dbhelper.updateGroupData(mdata.groupId, Constants.TAG_GROUP_NAME, mdata.groupName);
                                        socketConnection.updateGroupInfo(mdata);
                                        break;
                                    case "group_image":
                                        dbhelper.updateGroupData(mdata.groupId, Constants.TAG_GROUP_IMAGE, mdata.attachment);
                                        socketConnection.updateGroupInfo(mdata);
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
                                        socketConnection.updateGroupInfo(mdata);
                                        break;
                                    case "left":
                                    case "remove_member":
                                        if (dbhelper.isUserExist(mdata.memberId))
                                            dbhelper.deleteFromGroup(mdata.groupId, mdata.memberId);
                                        socketConnection.updateGroupInfo(mdata);
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
                                }

                                if (mdata.memberId.equalsIgnoreCase(GetSet.getUserId()) && (mdata.messageType.equals("text") || mdata.messageType.equals("image") ||
                                        mdata.messageType.equals("audio") || mdata.messageType.equals("video") || mdata.messageType.equals("document") ||
                                        mdata.messageType.equals("location") || mdata.messageType.equals("contact"))) {


                                } else if (!mdata.memberId.equalsIgnoreCase(GetSet.getUserId()) && mdata.messageType.equals("admin")) {

                                } else if (mdata.messageType.equalsIgnoreCase("remove_member") && (!dbhelper.isUserExist(mdata.memberId))) {

                                } else {

                                    dbhelper.addGroupMessages(mdata.messageId, mdata.groupId, mdata.memberId, mdata.groupAdminId, mdata.messageType,
                                            mdata.message, mdata.attachment, mdata.lat, mdata.lon,
                                            mdata.contactName, mdata.contactPhoneNo, mdata.contactCountryCode, mdata.chatTime, mdata.thumbnail, "");

                                    int unseenCount = dbhelper.getUnseenGroupMessagesCount(mdata.groupId);
                                    dbhelper.addGroupRecentMsgs(mdata.groupId, mdata.messageId,
                                            mdata.memberId, mdata.chatTime, String.valueOf(unseenCount));
                                }

                            }
                        }
                        socketConnection.setRecentGroupListener();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopService();
            }

            @Override
            public void onFailure(Call<GroupChatResult> call, Throwable t) {
                Log.v("Contacts Failed", "TEST" + t.getMessage());
                call.cancel();
                stopService();
            }
        });
    }

    void recentCalls() {
        Call<CallData> call3 = apiInterface.recentcalls(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<CallData>() {
            @Override
            public void onResponse(Call<CallData> call, Response<CallData> response) {
                try {
                    Log.v(TAG, "recentCallsResponse=" + gson.toJson(response.body()));
                    CallData data = response.body();
                    if (data.status.equals("true")) {
                        List<CallData.Result> result = data.result;
                        for (int i = 0; i < result.size(); i++) {
                            CallData.Result mdata = result.get(i);
                            if (mdata.callerId != null) {
                                String isAlert = "1";
                                if (mdata.callStatus.equals("missed")) {
                                    isAlert = "0";
                                }
                                dbhelper.addRecentCall(mdata.callId, mdata.callerId, mdata.type, mdata.callStatus, mdata.createdAt, isAlert);
                                if (!dbhelper.isUserExist(mdata.callerId)) {
                                    getUserInfo(mdata.callerId);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopService();
            }

            @Override
            public void onFailure(Call<CallData> call, Throwable t) {
                Log.v("Contacts Failed", "TEST" + t.getMessage());
                call.cancel();
                stopService();
            }
        });
    }

    private void getRecentChannelChats() {
        Call<ChannelChatResult> call3 = apiInterface.recentChannelChats(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<ChannelChatResult>() {
            @Override
            public void onResponse(Call<ChannelChatResult> call, Response<ChannelChatResult> response) {
                Log.i(TAG, "getRecentChannelChats: " + gson.toJson(response.body()));
                if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                    for (ChannelMessage channelMessage : response.body().result) {
                        if (!dbhelper.isChannelExist(channelMessage.channelId)) {
                            getChannelInfo(channelMessage.channelId);
                        }
                        if (dbhelper.isChannelExist(channelMessage.channelId)) {
                            dbhelper.addChannelMessages(channelMessage.channelId, channelMessage.chatType, channelMessage.messageId,
                                    channelMessage.messageType, channelMessage.message, channelMessage.attachment, channelMessage.lat, channelMessage.lon,
                                    channelMessage.contactName, channelMessage.contactPhoneNo, channelMessage.contactCountryCode,
                                    channelMessage.chatTime, channelMessage.thumbnail != null ? channelMessage.thumbnail : "", "");

                            int unseenCount = dbhelper.getUnseenChannelMessagesCount(channelMessage.channelId);
                            dbhelper.addChannelRecentMsgs(channelMessage.channelId, channelMessage.messageId, channelMessage.chatTime, String.valueOf(unseenCount));
                        }
                    }
                    socketConnection.setRecentChannelChatListener();
                }
                stopService();
            }

            @Override
            public void onFailure(Call<ChannelChatResult> call, Throwable t) {
                Log.e(TAG, "getRecentChannelChats: " + t.getMessage());
                call.cancel();
                stopService();
            }
        });
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
                Log.i(TAG, "getChannelInfo: " + gson.toJson(response.body()));
                if (response.body().status != null && response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                    for (ChannelResult.Result result : response.body().result) {
                        dbhelper.addChannel(result.channelId, result.channelName, result.channelDes, result.channelImage,
                                result.channelType, result.channelAdminId, result.channelAdminName, result.totalSubscribers, result.createdAt, Constants.TAG_USER_CHANNEL,
                                "true", result.blockStatus, result.report);
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
        Call<AdminChannel> call3 = apiInterface.getAdminChannels(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<AdminChannel>() {
            @Override
            public void onResponse(Call<AdminChannel> call, Response<AdminChannel> response) {
                Log.v(TAG, "getAdminChannels: " + gson.toJson(response.body()));
                if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                    /*Get Channels from Admin*/
                    for (AdminChannel.Result result : response.body().result) {
                        dbhelper.addChannel(result.channelId, result.channelName, result.channelDes,
                                result.channelImage, Constants.TAG_PUBLIC, "", "", "",
                                result.createdTime, Constants.TAG_ADMIN_CHANNEL, "", "", "0");
                        if (!dbhelper.isChannelIdExistInMessages(result.channelId)) {
                            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                            RandomString randomString = new RandomString(10);
                            String messageId = result.channelId + randomString.nextString();
                            dbhelper.addChannelMessages(result.channelId, Constants.TAG_ADMIN_CHANNEL, messageId, "create_channel",
                                    "", "", "", "", "", "", "",
                                    unixStamp, "", "");

                            int unseenCount = dbhelper.getUnseenChannelMessagesCount(result.channelId);
                            dbhelper.addChannelRecentMsgs(result.channelId, messageId, unixStamp, "" + unseenCount);
                        }
                    }

                    /*Get the last recent message form db*/
                    HashMap<String, String> hashMap = dbhelper.getRecentChannelMsg();
                    if (hashMap.containsKey(Constants.TAG_CHANNEL_ID) && hashMap.get(Constants.TAG_CHANNEL_ID) != null) {
                        Log.i(TAG, "hashMap: " + hashMap.get(Constants.TAG_CHAT_TIME));
                        getMsgFromAdminChannels(hashMap.get(Constants.TAG_CHAT_TIME));
                    } else {
                        getMsgFromAdminChannels("" + System.currentTimeMillis() / 1000);
                    }
                }
                stopService();
            }

            @Override
            public void onFailure(Call<AdminChannel> call, Throwable t) {
                Log.e(TAG, "getAdminChannels: " + t.getMessage());
                call.cancel();
                stopService();
            }
        });
    }

    private void getMsgFromAdminChannels(String timeStamp) {
        Call<AdminChannelMsg> call3 = apiInterface.getMsgFromAdminChannels(GetSet.getToken(), timeStamp);
        call3.enqueue(new Callback<AdminChannelMsg>() {
            @Override
            public void onResponse(Call<AdminChannelMsg> call, Response<AdminChannelMsg> response) {
                if (response.body() != null && response.body().status.equalsIgnoreCase(Constants.TRUE) && response.body().result != null) {
                    Log.i(TAG, "getMsgFromAdminChannels: " + gson.toJson(response.body()));
                    for (AdminChannelMsg.Result result : response.body().result) {
                        if (dbhelper.isChannelExist(result.channelId)) {
                            String deleiveryStatus = "";
                            if (!dbhelper.isChannelMessageIdExist(result.messageId)) {

                                dbhelper.addChannelMessages(result.channelId, Constants.TAG_ADMIN_CHANNEL, result.messageId, result.messageType,
                                        result.message, result.attachment, "", "",
                                        "", "", "",
                                        result.chatTime, result.thumbnail, "");

                                int unseenCount = dbhelper.getUnseenChannelMessagesCount(result.channelId);
                                dbhelper.addChannelRecentMsgs(result.channelId, result.messageId, result.chatTime, "" + unseenCount);
                            }
                        }
                    }
                    socketConnection.setRecentChannelChatListener();
                }
            }

            @Override
            public void onFailure(Call<AdminChannelMsg> call, Throwable t) {
//                Log.e(TAG, "getMsgFromAdminChannels: " + t.getMessage());
                call.cancel();
            }
        });
    }

    private void getChannelInvites() {
        Call<ChannelResult> call3 = apiInterface.getRecentChannelInvites(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<ChannelResult>() {
            @Override
            public void onResponse(Call<ChannelResult> call, Response<ChannelResult> response) {
                Log.v(TAG, "getRecentChannelInvites: " + gson.toJson(response.body()));
                if (response.body() != null && response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                    for (ChannelResult.Result result : response.body().result) {
                        dbhelper.addChannel(result.channelId, result.channelName, result.channelDes,
                                result.channelImage, result.channelType, result.adminId,
                                result.channelAdminName, result.totalSubscribers, result.createdTime,
                                Constants.TAG_USER_CHANNEL, "", result.blockStatus, result.report);
                        if (!dbhelper.isChannelIdExistInMessages(result.channelId)) {
                            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                            RandomString randomString = new RandomString(10);
                            String messageId = result.channelId + randomString.nextString();
                            dbhelper.addChannelMessages(result.channelId, Constants.TAG_CHANNEL, messageId, "create_channel",
                                    "", "", "", "", "", "", "",
                                    unixStamp, "", "");

                            int unseenCount = dbhelper.getUnseenChannelMessagesCount(result.channelId);
                            dbhelper.addChannelRecentMsgs(result.channelId, messageId, unixStamp, "" + unseenCount);
                        }
                    }
                    socketConnection.setRecentChannelChatListener();
                }
                stopService();
            }

            @Override
            public void onFailure(Call<ChannelResult> call, Throwable t) {
                Log.e(TAG, "getRecentChannelInvites: " + t.getMessage());
                call.cancel();
                stopService();
            }
        });
    }

    private void getUserData(String memberKey, String groupId, String memberId, String memberRole) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<Map<String, String>> call3 = apiInterface.getuserprofile(GetSet.getToken(), GetSet.getphonenumber(), memberId);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                try {
                    Log.v(TAG, "getUserData: " + gson.toJson(response.body()));
                    Map<String, String> userdata = response.body();
                    if (userdata.get(Constants.TAG_STATUS).equals("true")) {

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

    private void getUserInfo(String memberId) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<Map<String, String>> call3 = apiInterface.getuserprofile(GetSet.getToken(), GetSet.getphonenumber(), memberId);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                try {
                    Log.v(TAG, "getUserInfo: " + gson.toJson(response.body()));
                    Map<String, String> userdata = response.body();
                    if (userdata.get(Constants.TAG_STATUS).equals("true")) {
                        String name = userdata.get(Constants.TAG_USER_NAME);
                        HashMap<String, String> map = ApplicationClass.getContactrNot(getApplicationContext(), userdata.get(Constants.TAG_PHONE_NUMBER));
                        if (map.get("isAlready").equals("true")) {
                            name = map.get(Constants.TAG_USER_NAME);
                        }
                        dbhelper.addContactDetails(name, userdata.get(Constants.TAG_ID), userdata.get(Constants.TAG_USER_NAME), userdata.get(Constants.TAG_PHONE_NUMBER), userdata.get(Constants.TAG_COUNTRY_CODE), userdata.get(Constants.TAG_USER_IMAGE),
                                userdata.get(Constants.TAG_PRIVACY_ABOUT), userdata.get(Constants.TAG_PRIVACY_LAST_SEEN), userdata.get(Constants.TAG_PRIVACY_PROFILE), userdata.get(Constants.TAG_ABOUT), userdata.get(Constants.TAG_CONTACT_STATUS));

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.v("Contacts Failed", "TEST" + t.getMessage());
                call.cancel();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "In onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case if services are bound (Bound Services).
        return null;
    }

    private boolean checkCallPermission() {
        int permissionContacts = ContextCompat.checkSelfPermission(context,
                READ_CONTACTS);
        return permissionContacts == PackageManager.PERMISSION_GRANTED;
    }
}
