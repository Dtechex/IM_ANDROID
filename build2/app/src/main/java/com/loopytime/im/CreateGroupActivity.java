package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.model.ContactsData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.utils.Constants.TAG_ADMIN;
import static com.loopytime.utils.Constants.TAG_GROUP;
import static com.loopytime.utils.Constants.TAG_GROUP_ID;
import static com.loopytime.utils.Constants.TAG_GROUP_IMAGE;
import static com.loopytime.utils.Constants.TAG_GROUP_MEMBERS;
import static com.loopytime.utils.Constants.TAG_GROUP_NAME;
import static com.loopytime.utils.Constants.TAG_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER;
import static com.loopytime.utils.Constants.TAG_MEMBER_ABOUT;
import static com.loopytime.utils.Constants.TAG_MEMBER_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER_NAME;
import static com.loopytime.utils.Constants.TAG_MEMBER_NO;
import static com.loopytime.utils.Constants.TAG_MEMBER_PICTURE;
import static com.loopytime.utils.Constants.TAG_MEMBER_ROLE;
import static com.loopytime.utils.Constants.TAG_USER_ID;
import static com.loopytime.utils.Constants.TRUE;

public class CreateGroupActivity extends BaseActivity implements View.OnClickListener, SocketConnection.OnGroupCreatedListener {

    private final String TAG = this.getClass().getSimpleName();
    static ApiInterface apiInterface;
    TextView title, txtCount;
    EditText edtGroupName;
    ImageView backbtn;
    CircleImageView userImage;
    ImageView noImage;
    LinearLayout btnNext;
    List<ContactsData.Result> groupList = new ArrayList<>();
    ProgressDialog progressDialog;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    GroupAdapter groupAdapter;
    DatabaseHandler dbhelper;
    LinearLayoutManager linearLayoutManager;
    RecyclerView groupRecycler;
    RelativeLayout imageLayout, mainLay;
    SocketConnection socketConnection;
    private String groupImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
//super.setActionBar();
        socketConnection = SocketConnection.getInstance(this);
        SocketConnection.getInstance(this).setOnGroupCreatedListener(this);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        title = findViewById(R.id.title);
        txtCount = findViewById(R.id.txtCount);
        backbtn = findViewById(R.id.backbtn);
        edtGroupName = findViewById(R.id.edtGroupName);
        groupRecycler = findViewById(R.id.groupRecycler);
        imageLayout = findViewById(R.id.imageLayout);
        userImage = findViewById(R.id.userImage);
        noImage = findViewById(R.id.noimage);
        btnNext = findViewById(R.id.btnNext);
        mainLay = findViewById(R.id.mainLay);

        if(ApplicationClass.isRTL()){
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        title.setText(getString(R.string.create_group));
        imageLayout.setOnClickListener(this);
        backbtn.setOnClickListener(this);
        btnNext.setOnClickListener(this);

        if (getIntent().getSerializableExtra(Constants.TAG_GROUP_LIST) != null) {
            groupList = (List<ContactsData.Result>) getIntent().getSerializableExtra(Constants.TAG_GROUP_LIST);
        }

        txtCount.setText("" + groupList.size());
        initGroupList();

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

    private void initGroupList() {
        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        groupRecycler.setLayoutManager(linearLayoutManager);
        groupRecycler.setHasFixedSize(true);

        if (groupAdapter == null) {
            groupAdapter = new GroupAdapter(this, groupList);
            groupRecycler.setAdapter(groupAdapter);
            groupAdapter.notifyDataSetChanged();
        } else {
            groupAdapter.notifyDataSetChanged();
        }
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
                ApplicationClass.preventMultiClick(btnNext);
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    if (TextUtils.isEmpty(("" + edtGroupName.getText()).trim())) {
                        makeToast(getString(R.string.enter_group_name));
                    } else {
                        progressDialog.show();
                        createGroup("" + edtGroupName.getText());
                    }
                }
                break;

        }
    }

    private void createGroup(String groupName) {
        try {
            JSONObject jobj = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            jobj.put(TAG_USER_ID, GetSet.getUserId());
            jobj.put(TAG_GROUP_NAME, groupName);
            jobj.put(TAG_GROUP_IMAGE, "");

            /*Add as Admin*/
            JSONObject group = new JSONObject();
            group.put(TAG_MEMBER_ID, GetSet.getUserId());
            group.put(TAG_MEMBER_NAME, ApplicationClass.getContactName(this, GetSet.getphonenumber(),"",GetSet.getUserName()));
            group.put(TAG_MEMBER_PICTURE, GetSet.getImageUrl());
            group.put(TAG_MEMBER_NO, GetSet.getphonenumber());
            group.put(TAG_MEMBER_ABOUT, GetSet.getAbout());
            group.put(TAG_MEMBER_ROLE, TAG_ADMIN);
            jsonArray.put(group);

            for (ContactsData.Result result : groupList) {
                group = new JSONObject();
                group.put(TAG_MEMBER_ID, result.user_id);
                group.put(TAG_MEMBER_NAME, result.user_name);
                group.put(TAG_MEMBER_PICTURE, result.user_image);
                group.put(TAG_MEMBER_NO, result.phone_no);
                group.put(TAG_MEMBER_ABOUT, "");
                group.put(TAG_MEMBER_ROLE, TAG_MEMBER);
                jsonArray.put(group);
            }
            jobj.put(TAG_GROUP_MEMBERS, jsonArray);
            Log.d(TAG, "createGroup: "+jobj);
            socketConnection.createGroup(jobj);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1 && requestCode == 234) {
            groupImage = ImagePicker.getImagePathFromResult(this, requestCode, resultCode, data);
//            Log.e(TAG, "onActivityResult: " + groupImageView);
            Glide.with(CreateGroupActivity.this).load(groupImage)
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

    @Override
    public void onGroupCreated(final JSONObject data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        JSONObject jobj = new JSONObject();
                        jobj.put(Constants.TAG_GROUP_ID, data.getString(TAG_ID));
                        jobj.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                        socketConnection.joinGroup(jobj);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (groupImage != null) {
                        try {
                            byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(groupImage));
                            uploadImage(bytes, data.getString(TAG_ID), data.getString(TAG_GROUP_NAME));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();

                        finish();
                        Intent i = new Intent(CreateGroupActivity.this, GroupChatActivity.class);
                        i.putExtra(TAG_GROUP_ID, data.getString(TAG_ID));
                        startActivity(i);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void uploadImage(byte[] imageBytes, final String Id, final String groupName) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("openImage/*"), imageBytes);
        MultipartBody.Part body = MultipartBody.Part.createFormData("group_image", "openImage.jpg", requestFile);

        final RequestBody groupId = RequestBody.create(MediaType.parse("multipart/form-data"), Id);
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<HashMap<String, String>> call3 = apiInterface.uploadGroupImage(GetSet.getToken(), body, groupId);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> data = response.body();
                Log.i(TAG, "uploadImage: " + data.toString());
                if (data.get(Constants.TAG_STATUS).equalsIgnoreCase(TRUE)) {
                    if (data.get(Constants.TAG_GROUP_IMAGE) != null) {
                        dbhelper.updateGroupData(Id, Constants.TAG_GROUP_IMAGE,
                                data.get(Constants.TAG_GROUP_IMAGE));
                        RandomString randomString = new RandomString(10);
                        String messageId = GetSet.getUserId() + randomString.nextString();
                        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                        String textMsg = getString(R.string.changed_group_image);
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(Constants.TAG_GROUP_ID, Id);
                            jsonObject.put(Constants.TAG_GROUP_NAME, groupName);
                            jsonObject.put(Constants.TAG_CHAT_TYPE, TAG_GROUP);
                            jsonObject.put(Constants.TAG_ATTACHMENT, data.get(Constants.TAG_GROUP_IMAGE));
                            jsonObject.put(Constants.TAG_GROUP_ADMIN_ID, GetSet.getUserId());
                            jsonObject.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                            jsonObject.put(Constants.TAG_MESSAGE_ID, messageId);
                            jsonObject.put(Constants.TAG_MESSAGE_TYPE, "group_image");
                            jsonObject.put(Constants.TAG_MESSAGE, textMsg);
                            jsonObject.put(Constants.TAG_CHAT_TIME, unixStamp);
                            socketConnection.startGroupChat(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (progressDialog.isShowing())
                            progressDialog.dismiss();

                        finish();
                        Intent i = new Intent(CreateGroupActivity.this, GroupChatActivity.class);
                        i.putExtra(TAG_GROUP_ID, Id);
                        startActivity(i);
                    }
                }

            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                Log.e(TAG, "uploadImage " + t.getMessage());
                call.cancel();
                if (progressDialog.isShowing())
                    progressDialog.dismiss();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketConnection.getInstance(this).setOnGroupCreatedListener(null);
    }

    public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.MyViewHolder> {

        List<ContactsData.Result> groupList;
        Context context;

        public GroupAdapter(Context context, List<ContactsData.Result> groupList) {
            this.context = context;
            this.groupList = groupList;
        }

        @Override
        public GroupAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_select_member, parent, false);

            return new GroupAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final GroupAdapter.MyViewHolder holder, int position) {

            ContactsData.Result result = groupList.get(position);

            if (result.blockedme.equals("block")) {
                Glide.with(context).load(R.drawable.temp)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                        .into(holder.profileimage);
            } else {
                DialogActivity.setProfileImage(result, holder.profileimage, context);
            }

            holder.txtName.setText(result.user_name);
        }

        @Override
        public int getItemCount() {
            return groupList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {

            LinearLayout parentlay;
            AppCompatImageButton btnRemove;
            CircleImageView profileimage;
            TextView txtName;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                profileimage = view.findViewById(R.id.userImage);
                txtName = view.findViewById(R.id.txtName);
                btnRemove = view.findViewById(R.id.btnRemove);

                btnRemove.setVisibility(View.GONE);
            }

        }
    }

}
