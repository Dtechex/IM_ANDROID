package com.loopytime.im;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.loopytime.external.RandomString;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.ForegroundService;
import com.loopytime.helper.SocketConnection;
import com.loopytime.model.GroupData;
import com.loopytime.model.GroupUpdateResult;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.loopytime.utils.Constants.TAG_ADMIN;
import static com.loopytime.utils.Constants.TAG_GROUP_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER_ROLE;

public class DeleteAccountActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    ImageView btnBack;
    TextView txtTitle;
    LinearLayout btnNext;
    DatabaseHandler dbhelper;
    static ApiInterface apiInterface;
    SocketConnection socketConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);
        socketConnection = SocketConnection.getInstance(this);
        dbhelper = DatabaseHandler.getInstance(this);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = DeleteAccountActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();

        btnBack = findViewById(R.id.backbtn);
        txtTitle = findViewById(R.id.title);
        btnNext = findViewById(R.id.btnNext);
        btnBack.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        txtTitle.setText(getString(R.string.delete_account));

        if(ApplicationClass.isRTL()){
            btnBack.setRotation(180);
        } else {
            btnBack.setRotation(0);
        }

        btnBack.setOnClickListener(this);
        btnNext.setOnClickListener(this);
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backbtn:
                finish();
                break;
            case R.id.btnNext:
                List<GroupData> groupData = dbhelper.getGroups();

                for (GroupData groupDatum : groupData) {
                    try {
                        String groupId = groupDatum.groupId;
                        GroupData groupInfo = dbhelper.getGroupData(getApplicationContext(), groupId);
                        if (dbhelper.isGroupHaveAdmin(groupId) == 1 && groupInfo.groupAdminId.equalsIgnoreCase(GetSet.getUserId())) {
                            List<GroupData.GroupMembers> membersData = dbhelper.getGroupMembers(getApplicationContext(), groupId);
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
                                    message.put(Constants.TAG_GROUP_ID, groupId);
                                    message.put(Constants.TAG_GROUP_NAME, groupInfo.groupName);
                                    message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
                                    message.put(Constants.TAG_CHAT_TIME, unixStamp);
                                    message.put(Constants.TAG_MESSAGE_ID, messageId);
                                    message.put(Constants.TAG_ATTACHMENT, Constants.TAG_ADMIN);
                                    message.put(Constants.TAG_MEMBER_ID, groupMember.memberId);
                                    message.put(Constants.TAG_MEMBER_NAME, groupMember.memberName);
                                    message.put(Constants.TAG_MEMBER_NO, groupMember.memberNo);
                                    message.put(Constants.TAG_MESSAGE_TYPE, "admin");
                                    message.put(Constants.TAG_MESSAGE, "Admin");
                                    message.put(Constants.TAG_GROUP_ADMIN_ID, groupMember.memberId);
                                    socketConnection.startGroupChat(message);
                                    updateGroupData(jsonArray, groupId);
                                    break;
                                }
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                        RandomString randomString = new RandomString(10);
                        String messageId = groupDatum.groupId + randomString.nextString();

                        JSONObject message = new JSONObject();
                        message.put(Constants.TAG_GROUP_ID, groupDatum.groupId);
                        message.put(Constants.TAG_GROUP_NAME, groupDatum.groupName);
                        message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
                        message.put(Constants.TAG_CHAT_TIME, unixStamp);
                        message.put(Constants.TAG_MESSAGE_ID, messageId);
                        message.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                        message.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                        message.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                        message.put(Constants.TAG_MESSAGE_TYPE, "left");
                        message.put(Constants.TAG_MESSAGE, "1 participant left");
                        message.put(Constants.TAG_GROUP_ADMIN_ID, groupDatum.groupAdminId);
                        socketConnection.startGroupChat(message);

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(TAG_GROUP_ID, groupDatum.groupId);
                        jsonObject.put(TAG_MEMBER_ID, GetSet.getUserId());
                        socketConnection.exitFromGroup(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                deleteMyAccount();
                break;
        }
    }

    private void updateGroupData(JSONArray jsonArray, String groupId) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<GroupUpdateResult> call3 = apiInterface.updateGroup(GetSet.getToken(), groupId, jsonArray);
        call3.enqueue(new Callback<GroupUpdateResult>() {
            @Override
            public void onResponse(Call<GroupUpdateResult> call, Response<GroupUpdateResult> response) {
                            Log.i(TAG, "updateGroup: " + new Gson().toJson(response.body()));
            }

            @Override
            public void onFailure(Call<GroupUpdateResult> call, Throwable t) {
                Log.e(TAG, "updateGroup: " + t.getMessage());
                call.cancel();
            }
        });
    }

    private void deleteMyAccount() {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<HashMap<String, String>> call3 = apiInterface.deleteMyAccount(GetSet.getToken(), GetSet.getUserId());
//        Log.e(TAG, "GetSet.getToken(): " + GetSet.getToken());
//        Log.e(TAG, "GetSet.getUserId(): " + GetSet.getUserId());
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> deleteData = response.body();
                Log.i(TAG, "deleteMyAccount: " + deleteData);
                try {
                    if (deleteData.get(Constants.TAG_STATUS).equalsIgnoreCase(Constants.TRUE)) {
                        deleteDB();
                        Intent service = new Intent(DeleteAccountActivity.this, ForegroundService.class);
                        service.setAction("stop");

                        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                        finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                Log.e(TAG, "deleteMyAccount: " + t.getMessage());
                call.cancel();
            }
        });
    }

    private void deleteDB() {
        dbhelper.clearDB(getApplicationContext());
        GetSet.logout();
        editor.clear().commit();
    }

}
