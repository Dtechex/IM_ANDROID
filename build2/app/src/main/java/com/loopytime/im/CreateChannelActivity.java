package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.snackbar.Snackbar;
import com.loopytime.external.ImagePicker;
import com.loopytime.external.RandomString;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.ImageCompression;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.StorageManager;
import com.loopytime.model.ChannelResult;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.utils.Constants.TRUE;

public class CreateChannelActivity extends BaseActivity implements View.OnClickListener, SocketConnection.ChannelCallbackListener {

    private String TAG = this.getClass().getSimpleName();
    ImageView backbtn, searchbtn, optionbtn, cancelbtn, fab;
    LinearLayout buttonLayout, channelTypeLayout;
    CircleImageView userImage, noimage;
    TextView title;
    EditText edtChannelName, edtChannelDes;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    DatabaseHandler dbhelper;
    StorageManager storageManager;
    RadioButton btnPublic, btnPrivate;
    RelativeLayout publicLay, privateLay, mainLay;
    String channelImage = "";
    SocketConnection socketConnection;
    ChannelResult.Result channelData;
    String channelId;
    boolean isImageChanged = false, isEdit = false;
    private static ApiInterface apiInterface;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_channel);

        socketConnection = SocketConnection.getInstance(this);
        SocketConnection.getInstance(this).setChannelCallbackListener(this);
        pref = CreateChannelActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        dbhelper = DatabaseHandler.getInstance(this);
        storageManager = StorageManager.getInstance(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        userImage = findViewById(R.id.userImage);
        noimage = findViewById(R.id.noimage);

        title = findViewById(R.id.title);
        edtChannelName = findViewById(R.id.edtChannelName);
        edtChannelDes = findViewById(R.id.edtChannelDes);
        backbtn = findViewById(R.id.backbtn);
        searchbtn = findViewById(R.id.searchbtn);
        optionbtn = findViewById(R.id.optionbtn);
        buttonLayout = findViewById(R.id.buttonLayout);
        channelTypeLayout = findViewById(R.id.channelTypeLayout);
        btnPublic = findViewById(R.id.btnPublic);
        btnPrivate = findViewById(R.id.btnPrivate);
        publicLay = findViewById(R.id.publicLay);
        privateLay = findViewById(R.id.privateLay);
        fab = findViewById(R.id.fab);
        mainLay = findViewById(R.id.mainLay);

        if(ApplicationClass.isRTL()){
            backbtn.setRotation(180);
            fab.setRotation(180);
        } else {
            backbtn.setRotation(0);
            fab.setRotation(0);
        }

        title.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        searchbtn.setVisibility(View.GONE);
        optionbtn.setVisibility(View.GONE);

        edtChannelName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        edtChannelDes.setFilters(new InputFilter[]{new InputFilter.LengthFilter(250)});

        if (getIntent().getStringExtra(Constants.TAG_CHANNEL_ID) != null) {
            channelId = getIntent().getStringExtra(Constants.TAG_CHANNEL_ID);
            channelData = dbhelper.getChannelInfo(channelId);
            isEdit = true;
        }

        title.setText(channelId == null ? getString(R.string.create_channel) : getString(R.string.edit_channel));
        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));

        if (isEdit) {
            edtChannelName.setText("" + channelData.channelName);
            edtChannelDes.setText("" + channelData.channelDes);
            channelImage = channelData.channelImage;
            Glide.with(getApplicationContext()).load(Constants.CHANNEL_IMG_PATH + channelImage)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            noimage.setVisibility(View.VISIBLE);
                            userImage.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            noimage.setVisibility(View.GONE);
                            userImage.setVisibility(View.VISIBLE);
                            return false;
                        }
                    }).into(userImage);

            channelTypeLayout.setVisibility(View.GONE);

            btnPublic.setChecked(channelData.channelType.equalsIgnoreCase(Constants.TAG_PUBLIC));
            btnPrivate.setChecked(channelData.channelType.equalsIgnoreCase(Constants.TAG_PRIVATE));
        }
        fab.setOnClickListener(this);
        userImage.setOnClickListener(this);
        noimage.setOnClickListener(this);
        backbtn.setOnClickListener(this);
        btnPublic.setOnClickListener(this);
        btnPrivate.setOnClickListener(this);
        privateLay.setOnClickListener(this);
        publicLay.setOnClickListener(this);

    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.userImage:
            case R.id.noimage:
                if (ContextCompat.checkSelfPermission(CreateChannelActivity.this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(CreateChannelActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, 101);
                } else {
                    if (isNetworkConnected().equals(NOT_CONNECT)) {
                        networkSnack();
                    } else {
                        ImagePicker.pickImage(this, getString(R.string.select_your_image));
                    }
                }
                break;
            case R.id.btnPublic:
                btnPrivate.setChecked(false);
                btnPublic.setChecked(true);
                break;
            case R.id.btnPrivate:
                btnPrivate.setChecked(true);
                btnPublic.setChecked(false);
                break;
            case R.id.publicLay:
                btnPrivate.setChecked(false);
                btnPublic.setChecked(true);
                break;
            case R.id.privateLay:
                btnPublic.setChecked(false);
                btnPrivate.setChecked(true);
                break;
            case R.id.backbtn:
                finish();
                break;
            case R.id.fab:
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    if (TextUtils.isEmpty(edtChannelName.getText().toString().trim())) {
                        makeToast(getString(R.string.enter_channel_name));
                    } else if (TextUtils.isEmpty(edtChannelDes.getText().toString().trim())) {
                        makeToast(getString(R.string.enter_channel_description));
                    } else {
                        if (!isEdit) {
                            if (progressDialog != null && !progressDialog.isShowing())
                                progressDialog.show();
                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put(Constants.TAG_USER_ID, GetSet.getUserId());
                                jsonObject.put(Constants.TAG_CHANNEL_NAME, "" + edtChannelName.getText());
                                jsonObject.put(Constants.TAG_CHANNEL_DES, "" + edtChannelDes.getText());
                                jsonObject.put(Constants.TAG_CHANNEL_TYPE, btnPublic.isChecked() ? Constants.TAG_PUBLIC : Constants.TAG_PRIVATE);
                                socketConnection.createChannel(jsonObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            if (!channelData.channelName.equals(edtChannelName.getText().toString()) ||
                                    !channelData.channelDes.equals(edtChannelDes.getText().toString()) ||
                                    isImageChanged){
                                updateChannel();
                            }
                        }
                    }
                }
                break;
        }
    }

    private void updateChannel() {
        if (progressDialog != null && !progressDialog.isShowing()) progressDialog.show();
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_CHANNEL_ID, channelId);
        map.put(Constants.TAG_CHANNEL_NAME, "" + edtChannelName.getText());
        map.put(Constants.TAG_CHANNEL_DES, "" + edtChannelDes.getText());
        map.put(Constants.TAG_CHANNEL_TYPE, btnPublic.isChecked() ? Constants.TAG_PUBLIC : Constants.TAG_PRIVATE);
        Call<HashMap<String, String>> call = apiInterface.updateChannel(GetSet.getToken(), map);
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> hashMap = response.body();
//                Log.i(TAG, "updateChannel: " + hashMap);
                dbhelper.addChannel(hashMap.get(Constants.TAG_ID), "" + hashMap.get(Constants.TAG_CHANNEL_NAME), "" + hashMap.get(Constants.TAG_CHANNEL_DES),
                        hashMap.get(Constants.TAG_CHANNEL_IMAGE), hashMap.get(Constants.TAG_CHANNEL_TYPE),
                        hashMap.get(Constants.TAG_CHANNEL_ADMIN_ID), GetSet.getUserName(), hashMap.get(Constants.TAG_TOTAL_SUBSCRIBERS),
                        hashMap.get(Constants.TAG_CREATED_TIME), Constants.TAG_USER_CHANNEL, "", channelData.blockStatus,channelData.report);
                if (isImageChanged) {
                    try {
                        byte[] bytes = FileUtils.readFileToByteArray(new File(channelImage));
                        uploadImage(bytes, channelId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (progressDialog != null && progressDialog.isShowing())
                        progressDialog.dismiss();
                    if (!channelData.channelName.equals(edtChannelName.getText().toString())){
                        sendChannelModified("subject");
                    }
                    if (!channelData.channelDes.equals(edtChannelDes.getText().toString())){
                        sendChannelModified("channel_des");
                    }
                    finish();
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                call.cancel();
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
//                Log.e(TAG, "updateChannel: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1 && requestCode == 234) {
            try {
                Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
                String imageStatus = storageManager.saveToSdCard(bitmap, "profile", timestamp + ".jpg");
                if (imageStatus.equals("success")) {
                    File file = storageManager.getImage("profile", timestamp + ".jpg");
                    String filepath = file.getAbsolutePath();
//                    Log.i(TAG, "selectedImageFile: " + filepath);
                    ImageCompression imageCompression = new ImageCompression(CreateChannelActivity.this) {
                        @Override
                        protected void onPostExecute(String imagePath) {
                            channelImage = imagePath;
                            isImageChanged = true;
                            Glide.with(getApplicationContext()).load(channelImage)
                                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                                    .listener(new RequestListener<Drawable>() {
                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                            noimage.setVisibility(View.VISIBLE);
                                            userImage.setVisibility(View.GONE);
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                            noimage.setVisibility(View.GONE);
                                            userImage.setVisibility(View.VISIBLE);
                                            return false;
                                        }
                                    }).into(userImage);
                        }
                    };
                    imageCompression.execute(filepath);
                } else {
                    Toast.makeText(this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImage(byte[] imageBytes, final String channelId) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.show();
        }
        RequestBody requestFile = RequestBody.create(MediaType.parse("openImage/*"), imageBytes);
        MultipartBody.Part body = MultipartBody.Part.createFormData("channel_attachment", "openImage.jpg", requestFile);

        RequestBody channelID = RequestBody.create(MediaType.parse("multipart/form-data"), channelId);
        Call<HashMap<String, String>> call3 = apiInterface.uploadChannelImage(GetSet.getToken(), body, channelID);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                HashMap<String, String> data = response.body();
//                Log.i(TAG, "uploadChannelImage: " + data);
                if (data.get(Constants.TAG_STATUS).equals(TRUE)) {
                    channelImage = data.get(Constants.TAG_CHANNEL_IMAGE);
                    dbhelper.updateChannelData(channelId, Constants.TAG_CHANNEL_IMAGE, channelImage);

                    if (isEdit) {
                        sendChannelModified("channel_image");
                        if (!channelData.channelName.equals(edtChannelName.getText().toString())){
                            sendChannelModified("subject");
                        }
                        if (!channelData.channelDes.equals(edtChannelDes.getText().toString())){
                            sendChannelModified("channel_des");
                        }
                        finish();
                    } else {
                        sendChannelModified("channel_image");
                        finish();
                        Intent intent = new Intent(getApplicationContext(), ChannelCreatedActivity.class);
                        intent.putExtra(Constants.TAG_CHANNEL_ID, channelId);
                        startActivity(intent);
                    }
                }

                finish();
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
//                Log.e(TAG, "uploadChannelImage: " + t.getMessage());
                call.cancel();
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        });
    }

    private void sendChannelModified(String type) {
        try {
            ChannelResult.Result channelData = dbhelper.getChannelInfo(channelId);

            String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
            RandomString randomString = new RandomString(10);
            String messageId = channelId + randomString.nextString();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.TAG_CHANNEL_ID, channelId);
            jsonObject.put(Constants.TAG_CHANNEL_NAME, channelData.channelName);
            jsonObject.put(Constants.TAG_CHAT_TYPE, Constants.TAG_CHANNEL);
            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
            jsonObject.put(Constants.TAG_MESSAGE_TYPE, type);
            jsonObject.put(Constants.TAG_MESSAGE, channelData.channelDes);
            jsonObject.put(Constants.TAG_ATTACHMENT, channelData.channelImage);
            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
            socketConnection.startChannelChat(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v("requestCode", "requestCode=" + requestCode);
        switch (requestCode) {
            case 100:
                int permissionContacts = ContextCompat.checkSelfPermission(CreateChannelActivity.this,
                        READ_CONTACTS);
                int permissionStorage = ContextCompat.checkSelfPermission(CreateChannelActivity.this,
                        WRITE_EXTERNAL_STORAGE);

                if (permissionContacts == PackageManager.PERMISSION_GRANTED && permissionStorage == PackageManager.PERMISSION_GRANTED) {

                }
                break;
            case 101:
                int permStorage = ContextCompat.checkSelfPermission(CreateChannelActivity.this,
                        WRITE_EXTERNAL_STORAGE);
                if (permStorage == PackageManager.PERMISSION_GRANTED) {
                    ImagePicker.pickImage(this, getString(R.string.select_your_image));
                }
                break;
        }
    }

    @Override
    public void onChannelCreated(JSONObject jsonObject) {
        try {
            channelId = jsonObject.getString(Constants.TAG_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isImageChanged) {
                    byte[] bytes = new byte[0];
                    try {
                        bytes = FileUtils.readFileToByteArray(new File(channelImage));
                        uploadImage(bytes, channelId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (progressDialog != null && !progressDialog.isShowing())
                        progressDialog.dismiss();
                    Intent intent = new Intent(getApplicationContext(), ChannelCreatedActivity.class);
                    intent.putExtra(Constants.TAG_CHANNEL_ID, channelId);
                    startActivity(intent);
                    finish();
                }

            }
        });
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
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        SocketConnection.getInstance(this).setChannelCallbackListener(null);
        super.onDestroy();
    }
}
