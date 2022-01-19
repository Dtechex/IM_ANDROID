package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.loopytime.external.ImagePicker;
import com.loopytime.external.RandomString;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.model.GroupData;
import com.loopytime.model.GroupUpdateResult;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.utils.Constants.TAG_GROUP_ID;
import static com.loopytime.utils.Constants.TRUE;

public class EditGroupActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    static ApiInterface apiInterface;
    TextView title, txtCount;
    EditText edtGroupName;
    ImageView backbtn;
    CircleImageView userImage;
    ImageView noImage;
    LinearLayout btnNext;
    ProgressDialog progressDialog;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    DatabaseHandler dbhelper;
    SocketConnection socketConnection;
    RecyclerView groupRecycler;
    RelativeLayout imageLayout, participantLay, mainLay;
    String _Id, updatedImage;
    GroupData groupData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        socketConnection = SocketConnection.getInstance(this);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        _Id = getIntent().getStringExtra(TAG_GROUP_ID);

        groupData = dbhelper.getGroupData(getApplicationContext(), _Id);

        title = findViewById(R.id.title);
        txtCount = findViewById(R.id.txtCount);
        backbtn = findViewById(R.id.backbtn);
        edtGroupName = findViewById(R.id.edtGroupName);
        groupRecycler = findViewById(R.id.groupRecycler);
        imageLayout = findViewById(R.id.imageLayout);
        participantLay = findViewById(R.id.participantLay);
        userImage = findViewById(R.id.userImage);
        noImage = findViewById(R.id.noimage);
        btnNext = findViewById(R.id.btnNext);
        mainLay = findViewById(R.id.mainLay);

        if (ApplicationClass.isRTL()) {
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        groupRecycler.setVisibility(View.GONE);
        title.setText(getString(R.string.create_group));
        imageLayout.setOnClickListener(this);
        backbtn.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        edtGroupName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        participantLay.setVisibility(View.GONE);
        title.setText(R.string.enter_new_subject);
        edtGroupName.setText("" + groupData.groupName);

        Glide.with(getApplicationContext()).load(Constants.GROUP_IMG_PATH + groupData.groupImage)
                .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        noImage.setVisibility(View.VISIBLE);
                        userImage.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        noImage.setVisibility(View.GONE);
                        userImage.setVisibility(View.VISIBLE);
                        return false;
                    }
                }).into(userImage);

    }

    @Override
    public void onNetworkChange(boolean isConnected) {

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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imageLayout:
                if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 100);
                } else {
                    ImagePicker.pickImage(this, getString(R.string.select_your_image));
                }
                break;
            case R.id.backbtn:
                finish();
                break;
            case R.id.btnNext:
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    if (TextUtils.isEmpty("" + edtGroupName.getText().toString().trim())) {
                        makeToast(getString(R.string.enter_group_name));
                    } else {
                        if (updatedImage != null && !updatedImage.equalsIgnoreCase("")) {
                            try {
                                byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(updatedImage));
                                uploadImage(bytes);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (!groupData.groupName.equals(edtGroupName.getText().toString())) {
                            updateGroup("" + edtGroupName.getText().toString().trim());
                        } else {
                            finish();
                        }
                    }
                }
                break;
        }
    }

    private void updateGroup(final String groupName) {
        try {
            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
            RandomString randomString = new RandomString(10);
            String messageId = groupData.groupId + randomString.nextString();

            JSONObject message = new JSONObject();
            message.put(Constants.TAG_GROUP_ID, groupData.groupId);
            message.put(Constants.TAG_GROUP_NAME, groupName);
            message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
            message.put(Constants.TAG_ATTACHMENT, updatedImage);
            message.put(Constants.TAG_CHAT_TIME, unixStamp);
            message.put(Constants.TAG_MESSAGE_ID, messageId);
            message.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
            message.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
            message.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
            message.put(Constants.TAG_MESSAGE_TYPE, "subject");
            message.put(Constants.TAG_MESSAGE, getString(R.string.changed_the_subject_to) + " \"" + groupName + "\"");

            dbhelper.updateGroupData(groupData.groupId, Constants.TAG_GROUP_NAME, groupName);
            Log.v("checkChat", "startchat=" + message);
            socketConnection.startGroupChat(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Call<GroupUpdateResult> call3 = apiInterface.updateGroup(GetSet.getToken(), groupData.groupId, groupName);
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
        finish();
    }

    void uploadImage(byte[] imageBytes) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("openImage/*"), imageBytes);
        MultipartBody.Part body = MultipartBody.Part.createFormData("group_image", "openImage.jpg", requestFile);

        final RequestBody groupId = RequestBody.create(MediaType.parse("multipart/form-data"), groupData.groupId);
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<HashMap<String, String>> call3 = apiInterface.uploadGroupImage(GetSet.getToken(), body, groupId);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> data = response.body();
                Log.i(TAG, "uploadImage: " + data.toString());
                if (data.get(Constants.TAG_STATUS).equalsIgnoreCase(TRUE)) {
                    if (data.get(Constants.TAG_GROUP_IMAGE) != null) {
                        updatedImage = data.get(Constants.TAG_GROUP_IMAGE);

                        try {
                            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                            RandomString randomString = new RandomString(10);
                            String messageId = groupData.groupId + randomString.nextString();
                            String groupName = edtGroupName.getText().toString().trim();

                            JSONObject message = new JSONObject();
                            message.put(Constants.TAG_GROUP_ID, groupData.groupId);
                            message.put(Constants.TAG_GROUP_NAME, groupName);
                            message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
                            message.put(Constants.TAG_ATTACHMENT, updatedImage);
                            message.put(Constants.TAG_CHAT_TIME, unixStamp);
                            message.put(Constants.TAG_MESSAGE_ID, messageId);
                            message.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                            message.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                            message.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                            if (updatedImage != null && !updatedImage.equalsIgnoreCase("")) {
                                message.put(Constants.TAG_MESSAGE_TYPE, "group_image");
                                message.put(Constants.TAG_MESSAGE, getString(R.string.changed_group_icon));

                                dbhelper.updateGroupData(groupData.groupId, Constants.TAG_GROUP_IMAGE, updatedImage);
                            }
                            Log.v("checkChat", "startchat=" + message);
                            socketConnection.startGroupChat(message);

                            if (!groupData.groupName.equals(edtGroupName.getText().toString())) {
                                updateGroup("" + edtGroupName.getText().toString().trim());
                            } else {
                                finish();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                Log.e(TAG, "uploadImage " + t.getMessage());
                call.cancel();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1 && requestCode == 234) {
            updatedImage = ImagePicker.getImagePathFromResult(this, requestCode, resultCode, data);
//            Log.e(TAG, "onActivityResult: " + groupImageView);
            Glide.with(EditGroupActivity.this).load(updatedImage)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            noImage.setVisibility(View.VISIBLE);
                            userImage.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            noImage.setVisibility(View.GONE);
                            userImage.setVisibility(View.VISIBLE);
                            return false;
                        }
                    }).into(userImage);

        }
    }

}
