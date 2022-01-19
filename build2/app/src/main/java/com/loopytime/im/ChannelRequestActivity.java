package com.loopytime.im;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.loopytime.external.RandomString;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.SocketConnection;
import com.loopytime.model.ChannelResult;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChannelRequestActivity extends BaseActivity implements View.OnClickListener {

    private String TAG = this.getClass().getSimpleName();
    Toolbar toolbar;
    CircleImageView channelImageView;
    ImageView backbtn, searchbtn, optionbtn, cancelbtn;
    TextView txtChannelName, txtChannelDes, txtMembersCount, btnJoin, btnDeney;
    LinearLayout buttonLayout;
    DatabaseHandler dbhelper;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    static ApiInterface apiInterface;
    ChannelResult.Result channelData;
    String channelId;
    SocketConnection socketConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_request);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = ChannelRequestActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);
        socketConnection = SocketConnection.getInstance(this);

        channelImageView = findViewById(R.id.channelImageView);
        backbtn = findViewById(R.id.backbtn);
        searchbtn = findViewById(R.id.searchbtn);
        optionbtn = findViewById(R.id.optionbtn);
        cancelbtn = findViewById(R.id.cancelbtn);
        txtChannelName = findViewById(R.id.txtChannelName);
        txtChannelDes = findViewById(R.id.txtChannelDes);
        txtMembersCount = findViewById(R.id.txtMembersCount);
        btnJoin = findViewById(R.id.btnJoin);
        btnDeney = findViewById(R.id.btnDeney);
        buttonLayout = findViewById(R.id.buttonLayout);

        if (ApplicationClass.isRTL()) {
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        searchbtn.setVisibility(View.GONE);
        optionbtn.setVisibility(View.GONE);
        cancelbtn.setVisibility(View.GONE);

        if (getIntent().getStringExtra(Constants.TAG_CHANNEL_ID) != null) {
            channelId = getIntent().getStringExtra(Constants.TAG_CHANNEL_ID);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel("New Channel", 0);
            }
            if (dbhelper.isChannelExist(channelId)) {
                getChannelData(channelId);
                updateReadStatus();
            } else {
                channelData = new ChannelResult().new Result();
                channelData.channelId = channelId;
                channelData.channelName = getIntent().getStringExtra(Constants.TAG_CHANNEL_NAME);
                channelData.channelAdminId = getIntent().getStringExtra(Constants.TAG_CHANNEL_ADMIN_ID);
                channelData.channelDes = getIntent().getStringExtra(Constants.TAG_CHANNEL_DES);
                channelData.channelImage = getIntent().getStringExtra(Constants.TAG_CHANNEL_IMAGE);
                channelData.channelType = getIntent().getStringExtra(Constants.TAG_CHANNEL_TYPE);
                channelData.totalSubscribers = getIntent().getStringExtra(Constants.TAG_TOTAL_SUBSCRIBERS);
                initChannel(channelData);
            }
        }

        btnJoin.setOnClickListener(this);
        btnDeney.setOnClickListener(this);
        backbtn.setOnClickListener(this);
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void initChannel(ChannelResult.Result channelData) {
        Glide.with(getApplicationContext()).load(Constants.CHANNEL_IMG_PATH + (channelData.channelImage != null ? channelData.channelImage : "")).thumbnail(0.5f)
                .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(this, 70)))
                .into(channelImageView);

        txtChannelName.setText(channelData.channelName);
        txtChannelDes.setText(channelData.channelDes);
        String temp = (channelData.totalSubscribers != null ? channelData.totalSubscribers : "0");
        temp = temp + " " + getString(R.string.subscribers);
        txtMembersCount.setText(temp);
    }

    private void updateReadStatus() {
        dbhelper.updateChannelMessageReadStatus(channelId);
        dbhelper.resetUnseenChannelMessagesCount(channelId);
        dbhelper.updateChannelReadData(channelId, Constants.TAG_DELIVERY_STATUS, "read");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backbtn:
                finish();
                break;

            case R.id.btnJoin:
                if (dbhelper.isChannelExist(channelId)) {
                    subscribeChannel();
                    dbhelper.updateChannelData(channelId, Constants.TAG_SUBSCRIBE_STATUS, Constants.TRUE);
                    finish();
                    Intent channel = new Intent(getApplicationContext(), ChannelChatActivity.class);
                    channel.putExtra(Constants.TAG_CHANNEL_ID, channelId);
                    startActivity(channel);
                } else {
                    getChannelInfo(channelId);
                }
                break;

            case R.id.btnDeney:
                if (dbhelper.isChannelExist(channelId)) {
                    dbhelper.deleteChannel(channelId);
                    dbhelper.deleteChannelMessages(channelId);
                    dbhelper.deleteChannelRecentMessages(channelId);
                }
                finish();

                break;
        }
    }

    private void subscribeChannel() {
        JSONObject jsonobject = new JSONObject();
        try {
            jsonobject.put(Constants.TAG_USER_ID, GetSet.getUserId());
            jsonobject.put(Constants.TAG_CHANNEL_ID, channelId);
            socketConnection.subscribeChannel(jsonobject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getChannelInfo(String channelId) {
        if (NetworkReceiver.isConnected()) {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(channelId);
            Call<ChannelResult> call = apiInterface.getChannelInfo(GetSet.getToken(), jsonArray);
            call.enqueue(new Callback<ChannelResult>() {
                @Override
                public void onResponse(Call<ChannelResult> call, Response<ChannelResult> response) {
                    Log.i(TAG, "getChannelInfo: " + new Gson().toJson(response.body()));
                    if (response.isSuccessful() && response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                        for (ChannelResult.Result result : response.body().result) {
                            dbhelper.addChannel(result.channelId, result.channelName, result.channelDes, result.channelImage, result.channelType,
                                    result.channelAdminId, result.channelAdminName, result.totalSubscribers, result.createdTime,
                                    Constants.TAG_USER_CHANNEL, Constants.TRUE, result.blockStatus, result.report);
                            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                            RandomString randomString = new RandomString(10);
                            String messageId = result.channelId + randomString.nextString();
                            dbhelper.addChannelMessages(result.channelId, Constants.TAG_CHANNEL, messageId, "create_channel",
                                    "", "", "", "", "", "", "",
                                    unixStamp, "", "");

                            int unseenCount = dbhelper.getUnseenChannelMessagesCount(result.channelId);
                            dbhelper.addChannelRecentMsgs(result.channelId, messageId, unixStamp, "" + unseenCount);
                        }
                        subscribeChannel();
                        finish();
                        Intent channel = new Intent(getApplicationContext(), ChannelChatActivity.class);
                        channel.putExtra(Constants.TAG_CHANNEL_ID, channelId);
                        startActivity(channel);
                    }
                }

                @Override
                public void onFailure(Call<ChannelResult> call, Throwable t) {
                    Log.e(TAG, "getChannelInfo: " + t.getMessage());
                    call.cancel();
                }
            });
        }
    }

    private void getChannelData(String channelId) {
        if (NetworkReceiver.isConnected()) {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(channelId);
            Call<ChannelResult> call = apiInterface.getChannelInfo(GetSet.getToken(), jsonArray);
            call.enqueue(new Callback<ChannelResult>() {
                @Override
                public void onResponse(Call<ChannelResult> call, Response<ChannelResult> response) {
                    Log.i(TAG, "getChannelInfo: " + new Gson().toJson(response.body()));
                    if (response.isSuccessful() && response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                        for (ChannelResult.Result result : response.body().result) {
                            dbhelper.updateChannelInfo(result.channelId, result.channelName, result.channelDes, result.channelImage, result.channelType,
                                    result.channelAdminId, result.channelAdminName, result.totalSubscribers, "");
                            channelData = dbhelper.getChannelInfo(channelId);
                            initChannel(channelData);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ChannelResult> call, Throwable t) {
                    Log.e(TAG, "getChannelInfo: " + t.getMessage());
                    call.cancel();
                    channelData = dbhelper.getChannelInfo(channelId);
                    initChannel(channelData);
                }
            });
        } else {
            channelData = dbhelper.getChannelInfo(channelId);
            initChannel(channelData);
        }
    }
}
