package com.loopytime.im;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.loopytime.external.RandomString;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.Utils;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupData;
import com.loopytime.model.GroupResult;
import com.loopytime.model.GroupUpdateResult;
import com.loopytime.model.MediaModelData;
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

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.helper.Utils.getURLForResource;
import static com.loopytime.utils.Constants.TAG_ADMIN;
import static com.loopytime.utils.Constants.TAG_GROUP_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER;
import static com.loopytime.utils.Constants.TAG_MEMBER_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER_ROLE;
import static com.loopytime.utils.Constants.TAG_USER_ID;

public class GroupInfoActivity extends BaseActivity implements View.OnClickListener, SocketConnection.NewAdminCreatedListener{

    private String TAG = this.getClass().getSimpleName();
    Toolbar toolbar;
    ImageView groupImageView, btnBack, btnAdd, btnEdit, btnMenu, closeBtn, imageView, arrow;
    TextView txtGroupName, txtCreatedAt, btnViewAll, txtEventCount, txtParticipants, btnAddMember, mediaCount;
    RecyclerView fileRecycler, eventRecycler, contactRecycler,mediaList;
    SwitchCompat btnMute;
    CollapsingToolbarLayout collapse_toolbar;
    AppBarLayout appBarLayout;
    GroupAdapter contactAdapter;
    FileAdapter fileAdapter;
    EventAdapter eventAdapter;
    List<GroupData.GroupMembers> membersList = new ArrayList<>();
    List<String> eventList = new ArrayList<>();
    List<String> fileList = new ArrayList<>();
    LinearLayoutManager linearLayoutManager;
    ProgressDialog progressDialog;
    static ApiInterface apiInterface;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    DatabaseHandler dbhelper;
    String groupId, groupName, groupAdminId, groupImage;
    CoordinatorLayout mainLay;
    RelativeLayout imageViewLay,mediaLay;
    GroupData groupData;
    LinearLayout mediaLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);
        SocketConnection.getInstance(this).setNewAdminCreatedListener(this);
        if (getIntent().getStringExtra(TAG_GROUP_ID) != null) {
            groupId = getIntent().getStringExtra(TAG_GROUP_ID);
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        toolbar = findViewById(R.id.toolbar);
        collapse_toolbar = findViewById(R.id.collapse_toolbar);
        appBarLayout = findViewById(R.id.appbar);
        groupImageView = findViewById(R.id.groupImage);
        btnBack = findViewById(R.id.btnBack);
        btnAdd = findViewById(R.id.btnAdd);
        btnEdit = findViewById(R.id.btnEdit);
        btnMenu = findViewById(R.id.btnMenu);
        txtGroupName = findViewById(R.id.txtGroupName);
        txtCreatedAt = findViewById(R.id.txtCreatedAt);
        btnViewAll = findViewById(R.id.btnViewAll);
        txtEventCount = findViewById(R.id.txtEventCount);
        txtParticipants = findViewById(R.id.txtParticipants);
        btnAddMember = findViewById(R.id.btnAddMember);
        fileRecycler = findViewById(R.id.fileRecycler);
        eventRecycler = findViewById(R.id.eventRecycler);
        contactRecycler = findViewById(R.id.contactRecycler);
        mainLay = findViewById(R.id.mainLay);
        btnMute = findViewById(R.id.btnMute);
        imageViewLay = findViewById(R.id.imageViewLay);
        closeBtn = findViewById(R.id.closeBtn);
        imageView = findViewById(R.id.imageView);
        mediaLayout = findViewById(R.id.mediaLayout);
        mediaList = findViewById(R.id.mediaList);
        mediaLay = findViewById(R.id.mediaLay);
        mediaCount = findViewById(R.id.mediaCount);
        arrow = findViewById(R.id.arrow);

        if (ApplicationClass.isRTL()) {
            btnBack.setRotation(180);
            arrow.setRotation(180);
        } else {
            btnBack.setRotation(0);
            arrow.setRotation(0);
        }

        setMediaAdapter();

        btnBack.setOnClickListener(this);
        btnMute.setOnClickListener(this);
        btnAdd.setOnClickListener(this);
        btnAddMember.setOnClickListener(this);
        btnEdit.setOnClickListener(this);
        btnMenu.setOnClickListener(this);
        groupImageView.setOnClickListener(this);
        closeBtn.setOnClickListener(this);
        mediaLay.setOnClickListener(this);

        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        collapse_toolbar.getLayoutParams().height = (getResources().getDisplayMetrics().heightPixels * 60 / 100);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    // Collapsed
                    btnBack.setColorFilter(ContextCompat.getColor(GroupInfoActivity.this, R.color.primarytext));
                    btnEdit.setColorFilter(ContextCompat.getColor(GroupInfoActivity.this, R.color.primarytext));
                    btnMenu.setColorFilter(ContextCompat.getColor(GroupInfoActivity.this, R.color.primarytext));
                    btnAdd.setColorFilter(ContextCompat.getColor(GroupInfoActivity.this, R.color.primarytext));
                } else if (verticalOffset == 0) {
                    // Expanded
                    btnBack.setColorFilter(ContextCompat.getColor(GroupInfoActivity.this, R.color.white));
                    btnEdit.setColorFilter(ContextCompat.getColor(GroupInfoActivity.this, R.color.white));
                    btnMenu.setColorFilter(ContextCompat.getColor(GroupInfoActivity.this, R.color.white));
                    btnAdd.setColorFilter(ContextCompat.getColor(GroupInfoActivity.this, R.color.white));
                } else {
                    // Somewhere in between
                }
            }
        });

//        initMedia();
//        initEvents();
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    protected void onResume() {
        initGroup();
        initContact();
        super.onResume();
    }

    private void setMediaAdapter(){
        List<MediaModelData> mediaData = new ArrayList<>();

        try {
            mediaData = dbhelper.getMedia(groupId,Constants.TAG_GROUP,getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(mediaData.isEmpty()){
            mediaLayout.setVisibility(View.GONE);
        } else {
            /*mediaCount.setText("" + mediaData.size());*/
            LinearLayoutManager layoutManager = new LinearLayoutManager(GroupInfoActivity.this, RecyclerView.HORIZONTAL, false);
            mediaList.setLayoutManager(layoutManager);
            MediaShareAdapter adapter = new MediaShareAdapter(mediaData, GroupInfoActivity.this, mediaData.size());
            mediaList.setAdapter(adapter);
        }

    }

    private void getGroupInfo(String groupId) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(groupId);
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<GroupResult> call3 = apiInterface.getGroupInfo(GetSet.getToken(), jsonArray.toString());
        call3.enqueue(new Callback<GroupResult>() {
            @Override
            public void onResponse(Call<GroupResult> call, Response<GroupResult> response) {
                try {
                    Log.i(TAG, "getGroupInfo: " + new Gson().toJson(response.body()));
                    GroupResult userdata = response.body();
                    if (userdata.status.equalsIgnoreCase(Constants.TRUE)) {
                        for (GroupData groupData : userdata.result) {

                            for (GroupData.GroupMembers groupMember : groupData.groupMembers) {
                                Log.i(TAG, "getGroupInfo: " + new Gson().toJson(groupMember));
                            }

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<GroupResult> call, Throwable t) {
                Log.v("getGroupInfo Failed", "TEST" + t.getMessage());
                call.cancel();
            }
        });
    }

    private void initGroup() {
//        getGroupInfo(groupId);
        groupData = dbhelper.getGroupData(this, groupId);
        groupName = groupData.groupName;
        groupAdminId = groupData.groupAdminId;
        groupImage = groupData.groupImage;
        txtGroupName.setText(groupName);
        String createdBy = "";
        if (groupData.groupAdminId.equalsIgnoreCase(GetSet.getUserId())) {
            createdBy = getString(R.string.you);
        } else {
            ContactsData.Result result = dbhelper.getContactDetail(groupData.groupAdminId);
            createdBy = ApplicationClass.getContactName(GroupInfoActivity.this, result.phone_no,result.country_code,result.phone_no);
        }

        if (groupData.muteNotification.equals("true")) {
            btnMute.setChecked(true);
        } else {
            btnMute.setChecked(false);
        }

        txtCreatedAt.setText(getString(R.string.created_by) + " " + createdBy + " " + getString(R.string.at) + " " + Utils.getCreatedFormatDate(this, Long.parseLong(groupData.createdAt)));

        Glide.with(GroupInfoActivity.this).load(Constants.GROUP_IMG_PATH + groupData.groupImage)
                .apply(new RequestOptions().error(R.drawable.ic_group_banner).placeholder(R.drawable.ic_group_banner))
                .into(groupImageView);
    }

    private void initMedia() {
        fileList.add("");
        fileRecycler.setLayoutManager(linearLayoutManager);

        if (fileAdapter == null) {
            fileAdapter = new FileAdapter(this, fileList);
            fileRecycler.setAdapter(fileAdapter);
            fileAdapter.notifyDataSetChanged();
        } else {
            fileAdapter.notifyDataSetChanged();
        }
    }

    public void initEvents() {
        eventList.add("");
        eventRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        txtEventCount.setText("" + eventList.size());
        if (eventAdapter == null) {
            eventAdapter = new EventAdapter(this, eventList);
            eventRecycler.setAdapter(eventAdapter);
            eventAdapter.notifyDataSetChanged();
        } else {
            eventAdapter.notifyDataSetChanged();
        }
    }

    private void initContact() {
        membersList.clear();
        membersList.addAll(dbhelper.getGroupMembers(getApplicationContext(), groupId));
        contactRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        txtParticipants.setText("" + membersList.size() + " " + getString(R.string.participants));
        if (contactAdapter == null) {
            contactAdapter = new GroupAdapter(this, membersList);
            contactRecycler.setAdapter(contactAdapter);
            contactAdapter.notifyDataSetChanged();
        } else {
            contactAdapter.notifyDataSetChanged();
        }

        if (isAdmin()) {
            btnAdd.setVisibility(View.VISIBLE);
            btnAddMember.setVisibility(View.VISIBLE);
        } else {
            btnAdd.setVisibility(View.GONE);
            btnAddMember.setVisibility(View.GONE);
        }
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
            case R.id.btnBack:
                finish();
                break;
            case R.id.btnMute:
                if (btnMute.isChecked()) {
                    dbhelper.updateMuteGroup(groupId, "true");
                } else {
                    dbhelper.updateMuteGroup(groupId, "");
                }
                break;
            case R.id.btnAdd:
            case R.id.btnAddMember:
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    if (dbhelper.getGroupMemberSize(groupId) <= 50) {
                        if (dbhelper.isMemberExist(GetSet.getUserId(), groupId)) {
                            Intent i = new Intent(GroupInfoActivity.this, NewGroupActivity.class);
                            i.putExtra(TAG_GROUP_ID, groupId);
                            startActivity(i);
                        } else {
                            exitSnack();
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.group_limit), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btnEdit:
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    if (dbhelper.isMemberExist(GetSet.getUserId(), groupId)) {
                        Intent intent = new Intent(getApplicationContext(), EditGroupActivity.class);
                        intent.putExtra(TAG_GROUP_ID, groupId);
                        startActivity(intent);
                    } else {
                        exitSnack();
                    }
                }
                break;
            case R.id.btnMenu:
                Display display = this.getWindowManager().getDefaultDisplay();
                final ArrayList<String> values = new ArrayList<>();
                if (dbhelper.isMemberExist(GetSet.getUserId(), groupId)) {
                    values.add(getString(R.string.edit_group));
                    values.add(getString(R.string.exit_group));
                } else {
                    values.add(getString(R.string.delete_group));
                }

                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        R.layout.option_item, android.R.id.text1, values);
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = layoutInflater.inflate(R.layout.option_layout, null);
                final PopupWindow popup = new PopupWindow(GroupInfoActivity.this);
                popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popup.setContentView(layout);
                popup.setWidth(display.getWidth() * 60 / 100);
                popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                popup.setFocusable(true);

                ImageView pinImage = layout.findViewById(R.id.pinImage);
                if(ApplicationClass.isRTL()){
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
                popup.showAsDropDown(lv);

                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        popup.dismiss();
                        if (position == 0) {
                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                networkSnack();
                            } else {
                                if (dbhelper.isMemberExist(GetSet.getUserId(), groupId)) {
                                    Intent intent = new Intent(getApplicationContext(), EditGroupActivity.class);
                                    intent.putExtra(TAG_GROUP_ID, groupId);
                                    startActivity(intent);
                                } else {
                                    dbhelper.deleteMembers(groupId);
                                    dbhelper.deleteGroupMessages(groupId);
                                    dbhelper.deleteGroup(groupId);
                                    finish();
                                }
                            }
                        } else if (position == 1) {
                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                networkSnack();
                            } else {
                                exitConfirmDialog();
                            }
                        }
                    }
                });

                break;
            case R.id.groupImage:
                ApplicationClass.openImage(GroupInfoActivity.this,Constants.GROUP_IMG_PATH + groupData.groupImage,
                        Constants.TAG_GROUP,groupImageView);
                break;
            case R.id.mediaLay:
                Intent mediaIntent = new Intent(GroupInfoActivity.this,MediaDetailActivity.class);
                mediaIntent.putExtra(TAG_USER_ID,groupId);
                mediaIntent.putExtra(Constants.TAG_FROM,Constants.TAG_GROUP);
                startActivity(mediaIntent);
                break;
            case R.id.closeBtn:
                onBackPressed();
                break;
        }
    }

    private void exitSnack() {
        Snackbar snackbar = Snackbar
                .make(mainLay, getString(R.string.not_group_member), Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
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
                    if(memberData.memberId.equalsIgnoreCase(GetSet.getUserId())){
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
                    message.put(Constants.TAG_MESSAGE_TYPE, "left");
                    message.put(Constants.TAG_MESSAGE, getString(R.string.one_participant_left));
                    message.put(Constants.TAG_GROUP_ADMIN_ID, GetSet.getUserId());
                    socketConnection.startGroupChat(message);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(TAG_GROUP_ID, groupId);
                    jsonObject.put(TAG_MEMBER_ID, GetSet.getUserId());
                    socketConnection.exitFromGroup(jsonObject);

                    finish();
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

    @Override
    public void onNewAdminCreated() {
        if (contactAdapter != null) {
            membersList.clear();
            membersList.addAll(dbhelper.getGroupMembers(getApplicationContext(), groupId));
            contactAdapter.notifyDataSetChanged();
        }
    }

    public class FileAdapter extends RecyclerView.Adapter<FileAdapter.MyViewHolder> {

        List<String> contactList;
        Context context;

        public FileAdapter(Context context, List<String> contactList) {
            this.context = context;
            this.contactList = contactList;
        }

        @Override
        public FileAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_files, parent, false);

            return new FileAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final FileAdapter.MyViewHolder holder, int position) {

            String result = contactList.get(position);

            Glide.with(context).load(Constants.USER_IMG_PATH + "")
                    .apply(new RequestOptions().placeholder(R.drawable.temp).error(R.drawable.temp))
                    .into(holder.fileImage);

        }

        @Override
        public int getItemCount() {
            return fileList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            RelativeLayout imageLayout;
            ImageView fileImage, playIcon;

            public MyViewHolder(View view) {
                super(view);

                imageLayout = view.findViewById(R.id.imageLayout);
                fileImage = view.findViewById(R.id.fileImage);
                playIcon = view.findViewById(R.id.playIcon);

                playIcon.setOnClickListener(this);
                imageLayout.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.imageLayout:

                        break;
                    case R.id.playIcon:

                        break;

                }
            }
        }
    }

    public class EventAdapter extends RecyclerView.Adapter<EventAdapter.MyViewHolder> {

        List<String> eventList;
        Context context;

        public EventAdapter(Context context, List<String> eventList) {
            this.context = context;
            this.eventList = eventList;
        }

        @Override
        public EventAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_blocked_contacts, parent, false);

            return new EventAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final EventAdapter.MyViewHolder holder, int position) {

            holder.txtName.setText("");
            holder.txtAbout.setText("");

            Glide.with(context).load(Constants.USER_IMG_PATH + "")
                    .apply(new RequestOptions().placeholder(R.drawable.temp).error(R.drawable.temp))
                    .into(holder.profileimage);

        }

        @Override
        public int getItemCount() {
            return eventList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay;
            TextView txtName, txtAbout;
            CircleImageView profileimage;
            View profileview;
            AppCompatRadioButton btnSelect;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                profileimage = view.findViewById(R.id.profileimage);
                txtName = view.findViewById(R.id.txtName);
                txtAbout = view.findViewById(R.id.txtAbout);
                profileview = view.findViewById(R.id.profileview);

                txtAbout.setVisibility(View.VISIBLE);
                parentlay.setOnClickListener(this);
                profileview.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:

                        break;
                    case R.id.profileview:

                        break;

                }
            }
        }
    }

    private boolean isAdmin() {
        boolean isAdmin = false;
        for (GroupData.GroupMembers members : membersList) {
            if (members.memberId.equalsIgnoreCase(GetSet.getUserId())) {
                isAdmin = members.memberRole.equalsIgnoreCase(TAG_ADMIN);
                break;
            }
        }
        return isAdmin;
    }

    public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.MyViewHolder> {

        List<GroupData.GroupMembers> contactList;
        Context context;

        public GroupAdapter(Context context, List<GroupData.GroupMembers> contactList) {
            this.context = context;
            this.contactList = contactList;
        }

        @Override
        public GroupAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_blocked_contacts, parent, false);

            return new GroupAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final GroupAdapter.MyViewHolder holder, final int position) {

            final GroupData.GroupMembers memberResult = contactList.get(position);
            final ContactsData.Result memberData = dbhelper.getContactDetail(memberResult.memberId);

            if (memberResult.memberId.equalsIgnoreCase(GetSet.getUserId())) {
                holder.txtUserName.setVisibility(View.GONE);
                holder.txtName.setText(R.string.you);
                Glide.with(context).load(Constants.USER_IMG_PATH + GetSet.getImageUrl()).thumbnail(0.5f)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                        .into(holder.profileimage);

                if (memberResult.memberRole != null) {
                    if (memberResult.memberRole.equalsIgnoreCase(TAG_ADMIN)) {
                        holder.txtAdmin.setVisibility(View.VISIBLE);
                    } else {
                        holder.txtAdmin.setVisibility(View.GONE);
                    }
                }
            } else {
                ContactsData.Result result = dbhelper.getContactDetail(memberData.user_id);
                holder.txtName.setText(result.user_name);
                if(ApplicationClass.isStringNotNull(result.saved_name)){
                    holder.txtUserName.setVisibility(View.GONE);
                    holder.txtUserName.setText("");
                } else {
                    holder.txtUserName.setVisibility(View.VISIBLE);
                    holder.txtUserName.setText(result.user_name);
                }

                if (memberResult.memberRole != null) {
                    if (memberResult.memberRole.equalsIgnoreCase(TAG_ADMIN)) {
                        holder.txtAdmin.setVisibility(View.VISIBLE);
                    } else {
                        holder.txtAdmin.setVisibility(View.GONE);
                    }
                }

                if (memberData.blockedme.equals("block")) {
                    Glide.with(context).load(R.drawable.temp)
                            .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                            .into(holder.profileimage);
                } else {
                    DialogActivity.setProfileImage(result, holder.profileimage, context);
                    DialogActivity.setAboutUs(result, holder.txtAbout);

                }
            }
        }

        @Override
        public int getItemCount() {
            return contactList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay;
            TextView txtName, txtAbout, txtAdmin, txtUserName;
            CircleImageView profileimage;
            View profileview;
            AppCompatRadioButton btnSelect;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                profileimage = view.findViewById(R.id.profileimage);
                txtName = view.findViewById(R.id.txtName);
                txtAbout = view.findViewById(R.id.txtAbout);
                txtAdmin = view.findViewById(R.id.txtAdmin);
                txtUserName = view.findViewById(R.id.txtUserName);
                profileview = view.findViewById(R.id.profileview);

                parentlay.setOnClickListener(this);
                profileimage.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                GroupData.GroupMembers memberResult = contactList.get(getAdapterPosition());
                ContactsData.Result memberData = dbhelper.getContactDetail(memberResult.memberId);
                switch (view.getId()) {
                    case R.id.parentlay:
                        if (!memberResult.memberId.equalsIgnoreCase(GetSet.getUserId())) {
                            if (isAdmin()) {
                                openMessageDialog(context, memberData, memberResult, true);
                            } else {
                                openMessageDialog(context, memberData, memberResult, false);
                            }
                        }
                        break;
                    case R.id.profileimage:
                        if (!memberResult.memberId.equalsIgnoreCase(GetSet.getUserId())) {
                            openUserDialog(profileview, memberData.user_id, memberData.user_name, memberData.user_image, context);
                        }
                        break;
                }
            }
        }

        private void openUserDialog(View view, String userId, String memberName, String memberPicture, Context context) {
            Intent i = new Intent(context, DialogActivity.class);
            i.putExtra(Constants.TAG_USER_ID, userId);
            i.putExtra(Constants.TAG_USER_NAME, memberName);
            i.putExtra(Constants.TAG_USER_IMAGE, memberPicture);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(((GroupInfoActivity) context), view, getURLForResource(R.drawable.temp));
            startActivity(i, options.toBundle());
        }

        private void openMessageDialog(final Context context, final ContactsData.Result result, final GroupData.GroupMembers groupMember, boolean isVisible) {
            View bottomView = getLayoutInflater().inflate(R.layout.dialog_group_member, null);
            BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setContentView(bottomView);

            TextView txtMessage = dialog.findViewById(R.id.txtMessage);
            TextView txtView = dialog.findViewById(R.id.txtView);
            TextView txtMakeAdmin = dialog.findViewById(R.id.txtMakeAdmin);
            TextView txtRemove = dialog.findViewById(R.id.txtRemove);
            TextView txtContact = dialog.findViewById(R.id.txtContact);

            if (isVisible) {
                txtMakeAdmin.setVisibility(View.VISIBLE);
                txtRemove.setVisibility(View.VISIBLE);
            } else {
                txtMakeAdmin.setVisibility(View.GONE);
                txtRemove.setVisibility(View.GONE);
            }
            txtMessage.setText(getString(R.string.message) + " " + ApplicationClass.getContactName(context, result.phone_no,result.country_code,result.phone_no));
            txtView.setText(getString(R.string.view) + " " + ApplicationClass.getContactName(context, result.phone_no,result.country_code,result.phone_no));
            if (groupMember.memberRole.equalsIgnoreCase(TAG_ADMIN)) {
                txtMakeAdmin.setText(getString(R.string.remove) + " " + ApplicationClass.getContactName(context, result.phone_no,result.country_code,result.phone_no) + " " + getString(R.string.from_admin));
            } else {
                txtMakeAdmin.setText(getString(R.string.make) + " " + ApplicationClass.getContactName(context, result.phone_no,result.country_code,result.phone_no) + " " + getString(R.string.as_admin));
            }
            txtRemove.setText(getString(R.string.remove) + " " + ApplicationClass.getContactName(context, result.phone_no,result.country_code,result.phone_no));

            HashMap<String, String> map = ApplicationClass.getContactrNot(context, result.phone_no);
            if (map.get("isAlready").equals("true")) {
                txtContact.setVisibility(View.GONE);
            } else {
                txtContact.setVisibility(View.VISIBLE);
            }

            txtMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(GroupInfoActivity.this, ChatActivity.class);
                    i.putExtra(TAG_USER_ID, result.user_id);
                    startActivity(i);
                    dialog.dismiss();
                }
            });

            txtView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent profile = new Intent(GroupInfoActivity.this, ProfileActivity.class);
                    profile.putExtra(Constants.TAG_USER_ID, result.user_id);
                    startActivity(profile);
                    dialog.dismiss();
                }
            });
            txtMakeAdmin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isNetworkConnected().equals(NOT_CONNECT)) {
                        networkSnack();
                        dialog.dismiss();
                    } else {
                        if (groupMember.memberRole.equalsIgnoreCase(TAG_ADMIN)) {
                            groupMember.memberRole = TAG_MEMBER;
                        } else {
                            groupMember.memberRole = TAG_ADMIN;
                        }

                        JSONArray jsonArray = new JSONArray();
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(TAG_MEMBER_ID, groupMember.memberId);
                            jsonObject.put(TAG_MEMBER_ROLE, groupMember.memberRole);
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
                            message.put(Constants.TAG_ATTACHMENT, groupMember.memberRole);
                            message.put(Constants.TAG_MEMBER_ID, result.user_id);
                            message.put(Constants.TAG_MEMBER_NAME, result.user_name);
                            message.put(Constants.TAG_MEMBER_NO, result.phone_no);
                            message.put(Constants.TAG_MESSAGE_TYPE, "admin");
                            message.put(Constants.TAG_MESSAGE, getString(R.string.admin));
                            message.put(Constants.TAG_GROUP_ADMIN_ID, GetSet.getUserId());
                            socketConnection.startGroupChat(message);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        notifyDataSetChanged();
                        updateGroupData(jsonArray);
                        dialog.dismiss();
                    }
                }
            });

            txtRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isNetworkConnected().equals(NOT_CONNECT)) {
                        networkSnack();
                        dialog.dismiss();
                    } else {
                        contactList.remove(groupMember);
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
                            message.put(Constants.TAG_MEMBER_ID, result.user_id);
                            message.put(Constants.TAG_MEMBER_NAME, result.user_name);
                            message.put(Constants.TAG_MEMBER_NO, result.phone_no);
                            message.put(Constants.TAG_MESSAGE_TYPE, "remove_member");
                            message.put(Constants.TAG_MESSAGE, getString(R.string.one_participant_removed));
                            message.put(Constants.TAG_GROUP_ADMIN_ID, GetSet.getUserId());
                            socketConnection.startGroupChat(message);

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(TAG_GROUP_ID, groupId);
                            jsonObject.put(TAG_MEMBER_ID, result.user_id);
                            socketConnection.exitFromGroup(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (contactAdapter != null) {
                            notifyDataSetChanged();
                                txtParticipants.setText("" + contactList.size() + " " + getString(R.string.participants));
                            //  updateGroupData(channelList);
                        }
                        dialog.dismiss();
                    }
                }
            });

            txtContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
                    intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                    intent.putExtra("finishActivityOnSaveCompleted", true);
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, result.phone_no);
                    intent.putExtra(ContactsContract.Intents.Insert.NAME, "");
                    startActivityForResult(intent, 556);
                }
            });

            dialog.show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketConnection.getInstance(this).setNewAdminCreatedListener(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 556) {

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
