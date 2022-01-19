package com.loopytime.im;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.loopytime.external.RandomString;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.FileUploadService;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.StorageManager;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupData;
import com.loopytime.model.MessagesData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.READ_CONTACTS;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.utils.Constants.TAG_GROUP_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER;
import static com.loopytime.utils.Constants.TAG_MEMBER_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER_NO;
import static com.loopytime.utils.Constants.TAG_MEMBER_ROLE;
import static com.loopytime.utils.Constants.TAG_MY_CONTACTS;
import static com.loopytime.utils.Constants.TAG_NOBODY;
import static com.loopytime.utils.Constants.TRUE;

public class NewGroupActivity extends BaseActivity implements View.OnClickListener, SocketConnection.StatusUploadListener {

    private final String TAG = this.getClass().getSimpleName();
    TextView title, txtSubtitle, nullText;
    ImageView backbtn, searchbtn, optionbtn, cancelbtn;
    RecyclerView groupRecycler, contactRecycler;
    LinearLayoutManager linearLayoutManager;
    RecyclerViewAdapter recyclerViewAdapter;
    GroupAdapter groupAdapter;
    DatabaseHandler dbhelper;
    EditText searchView;
    RelativeLayout searchLay, selectAllLay;
    RelativeLayout mainLay;
    LinearLayout buttonLayout, btnNext;
    List<ContactsData.Result> groupList = new ArrayList<>();
    List<ContactsData.Result> contactList = new ArrayList<>();
    List<ContactsData.Result> filteredList = new ArrayList<>();
    List<String> userList = new ArrayList<>();

    ProgressDialog progressDialog;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String userId, groupId, groupName, groupAdminId, groupImage;
    LinearLayout nullLay;
    String from = "";
    HashMap<String, String> map = new HashMap<>();
    private boolean isSelectAll = false;
    RadioButton btnSelectAll;
    ApiInterface apiInterface, statusUploadInterface;
    StorageManager storageManager;
    private int resultCode = Activity.RESULT_CANCELED;
    private String sourceType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_group);
        pref = NewGroupActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);
        storageManager = StorageManager.getInstance(this);
        SocketConnection.getInstance(this).setStatusUploadListener(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        title = findViewById(R.id.title);
        txtSubtitle = findViewById(R.id.txtSubtitle);
        backbtn = findViewById(R.id.backbtn);
        searchbtn = findViewById(R.id.searchbtn);
        optionbtn = findViewById(R.id.optionbtn);
        groupRecycler = findViewById(R.id.groupRecycler);
        contactRecycler = findViewById(R.id.contactRecycler);
        searchView = findViewById(R.id.searchView);
        buttonLayout = findViewById(R.id.buttonLayout);
        cancelbtn = findViewById(R.id.cancelbtn);
        searchLay = findViewById(R.id.searchLay);
        mainLay = findViewById(R.id.mainLay);
        nullLay = findViewById(R.id.nullLay);
        nullText = findViewById(R.id.nullText);
        btnNext = findViewById(R.id.btnNext);
        selectAllLay = findViewById(R.id.selectAllLay);
        btnSelectAll = findViewById(R.id.btnSelectAll);

       /* btnSelectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                selectAllContact();
            }
        });*/

        title.setVisibility(View.VISIBLE);
        txtSubtitle.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        searchbtn.setVisibility(View.VISIBLE);
        optionbtn.setVisibility(View.GONE);

        if (ApplicationClass.isRTL()) {
        backbtn.setRotation(180);
            btnNext.setRotation(180);
        } else {
            backbtn.setRotation(0);
            btnNext.setRotation(0);
        }

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        statusUploadInterface = ApiClient.getUploadClient().create(ApiInterface.class);

        if (getIntent().getStringExtra(TAG_GROUP_ID) != null &&
                !getIntent().getStringExtra(TAG_GROUP_ID).equalsIgnoreCase("")) {
            groupId = getIntent().getStringExtra(TAG_GROUP_ID);
            GroupData groupData = dbhelper.getGroupData(this, groupId);
            groupName = groupData.groupName;
            groupImage = groupData.groupImage;
            groupAdminId = groupData.groupAdminId;
            title.setText(groupName);
        } else {
            if (getIntent().getExtras().getString(Constants.TAG_FROM) != null && !getIntent().getExtras().getString(Constants.TAG_FROM).equals("")) {
                from = getIntent().getExtras().getString(Constants.TAG_FROM);
            }

            if (getIntent().getExtras().getString(Constants.TAG_SOURCE_TYPE) != null && !getIntent().getExtras().getString(Constants.TAG_SOURCE_TYPE).equals("")) {
                sourceType = getIntent().getExtras().getString(Constants.TAG_SOURCE_TYPE);
            }
            if (from.equals("status")) {
                map = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.TAG_MESSAGE_DATA);
                title.setText(getString(R.string.forward_status));
                selectAllLay.setVisibility(View.VISIBLE);
                groupRecycler.setVisibility(View.GONE);
            } else {
                title.setText(getString(R.string.create_group));
                selectAllLay.setVisibility(View.GONE);
                groupRecycler.setVisibility(View.VISIBLE);
            }
        }

        btnSelectAll.setChecked(false);

        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));

        searchbtn.setOnClickListener(this);
        backbtn.setOnClickListener(this);
        cancelbtn.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        selectAllLay.setOnClickListener(this);
        btnSelectAll.setOnClickListener(this);

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

        initGroupList();
        initContactList();
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

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

    private void initContactList() {
        contactRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        contactRecycler.setHasFixedSize(true);

        contactList.addAll(dbhelper.getStoredContacts(this));
        Collections.sort(contactList, new Comparator<ContactsData.Result>() {
            public int compare(ContactsData.Result obj1, ContactsData.Result obj2) {
                // ## Ascending order
                return obj1.user_name.compareToIgnoreCase(obj2.user_name); // To compare string values
            }
        });
        filteredList = new ArrayList<>();
        if (groupId != null && !groupId.equalsIgnoreCase("")) {
            List<String> phoneList = new ArrayList<>();
            for (GroupData.GroupMembers members : dbhelper.getGroupMembers(this, groupId)) {
                String phoneNo = dbhelper.getContactDetail(members.memberId).phone_no;
                phoneList.add(phoneNo);
            }
            for (ContactsData.Result result : contactList) {
                if (!phoneList.contains(result.phone_no)) {
                    filteredList.add(result);
                }
            }
        } else {
            filteredList.addAll(contactList);
        }

        txtSubtitle.setText(" " + 0 + " " + getString(R.string.of) + " " +
                filteredList.size() + " " + getString(R.string.selected));

        if (recyclerViewAdapter == null) {
            recyclerViewAdapter = new RecyclerViewAdapter(this);
            contactRecycler.setAdapter(recyclerViewAdapter);
            recyclerViewAdapter.notifyDataSetChanged();
        } else {
            recyclerViewAdapter.notifyDataSetChanged();
        }

        nullText.setText(getString(R.string.no_contact));
        if (filteredList.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
        } else {
            nullLay.setVisibility(View.GONE);
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
            case R.id.backbtn:
                if (searchLay.getVisibility() == View.VISIBLE) {
                    searchView.setText("");
                    searchLay.setVisibility(View.GONE);
                    title.setVisibility(View.VISIBLE);
                    txtSubtitle.setVisibility(View.VISIBLE);
                    buttonLayout.setVisibility(View.VISIBLE);
                    setTxtSubtitle(userList.size());
                    ApplicationClass.hideSoftKeyboard(this, searchView);
                } else {
                    finish();
                }
                break;
            case R.id.searchbtn:
                title.setVisibility(View.GONE);
                txtSubtitle.setVisibility(View.GONE);
                searchLay.setVisibility(View.VISIBLE);
                buttonLayout.setVisibility(View.GONE);
                ApplicationClass.showKeyboard(this, searchView);
                break;
            case R.id.cancelbtn:
                searchView.setText("");
                break;
            case R.id.selectAllLay:
            case R.id.btnSelectAll:
                selectAllContact();
                break;
            case R.id.btnNext:
                if (from.equals("status")) {
                    if (!userList.isEmpty()) {
                        if (isNetworkConnected().equals(NOT_CONNECT)) {
                            networkSnack();
                        } else {
                            showLoading();
                            imageUpload();
                        }
                    } else {
                        makeToast(getString(R.string.please_choose_contact));
                    }
                } else {
                    createGroup();
                }
                break;

        }
    }

    private void selectAllContact() {
        isSelectAll = !isSelectAll;
        btnSelectAll.setChecked(isSelectAll);
        if (isSelectAll) {
            userList.clear();
            for (ContactsData.Result result : filteredList) {
                if (!result.blockedbyme.equals("block") &&
                        !result.blockedme.equals("block") && !userList.contains(result.user_id)) {
                    userList.add(result.user_id);
                }
            }
        } else {
            for (ContactsData.Result result : filteredList) {
                if (!result.blockedbyme.equals("block") &&
                        !result.blockedme.equals("block")) {
                    userList.remove(result.user_id);
                    groupList.remove(result);
                }
            }
        }
        setTxtSubtitle(userList.size());
        if (recyclerViewAdapter != null)
            recyclerViewAdapter.notifyDataSetChanged();
    }

    private void createGroup() {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else {
            if (groupId != null && !groupId.equalsIgnoreCase("")) {
                if (dbhelper.getGroupMemberSize(groupId) <= 50) {
                    if (groupList.size() > 0) {
                        JSONArray newMembers = new JSONArray();
                        JSONArray jsonArray = new JSONArray();
                        for (ContactsData.Result result : groupList) {
                            String memberKey = groupId + result.user_id;
                            dbhelper.createGroupMembers(memberKey, groupId, result.user_id,
                                    TAG_MEMBER);
                            JSONObject jsonObject = new JSONObject();
                            JSONObject jobj = new JSONObject();
                            try {
                                jsonObject.put(TAG_MEMBER_ID, result.user_id);
                                jsonObject.put(TAG_MEMBER_NO, result.phone_no);
                                jsonObject.put(TAG_MEMBER_ROLE, TAG_MEMBER);
                                newMembers.put(jsonObject);

                                jobj.put(TAG_MEMBER_ID, result.user_id);
                                jobj.put(TAG_MEMBER_ROLE, TAG_MEMBER);
                                jsonArray.put(jobj);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
                        Log.d(TAG, "modifyGroupmembers: " + jsonArray + "\n" + groupId);
                        Call<HashMap<String, String>> call3 = apiInterface.modifyGroupmembers(GetSet.getToken(), GetSet.getUserId(),
                                groupId, jsonArray);
                        call3.enqueue(new Callback<HashMap<String, String>>() {
                            @Override
                            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                                try {
                                    HashMap<String, String> groupData = response.body();
                                    Log.i(TAG, "modifyGroupmembers: " + groupData.toString());
                                    try {
                                        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                                        RandomString randomString = new RandomString(10);
                                        String messageId = groupId + randomString.nextString();

                                        JSONObject message = new JSONObject();
                                        message.put(Constants.TAG_GROUP_ID, groupId);
                                        message.put(Constants.TAG_GROUP_NAME, groupName);
                                        message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
                                        message.put(Constants.TAG_ATTACHMENT, newMembers);
                                        message.put(Constants.TAG_CHAT_TIME, unixStamp);
                                        message.put(Constants.TAG_MESSAGE_ID, messageId);
                                        message.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                                        message.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                                        message.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                                        message.put(Constants.TAG_MESSAGE_TYPE, "add_member");
                                        message.put(Constants.TAG_MESSAGE, getString(R.string.add) + " " + newMembers.length() + " " + getString(R.string.participant));
                                        message.put(Constants.TAG_GROUP_ADMIN_ID, GetSet.getUserId());

                                        socketConnection.startGroupChat(message);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                                Log.e(TAG, "modifyGroupmembers: " + t.getMessage());
                                call.cancel();
                                try {
                                    String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                                    RandomString randomString = new RandomString(10);
                                    String messageId = groupId + randomString.nextString();

                                    JSONObject message = new JSONObject();
                                    message.put(Constants.TAG_GROUP_ID, groupId);
                                    message.put(Constants.TAG_GROUP_NAME, groupName);
                                    message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
                                    message.put(Constants.TAG_ATTACHMENT, newMembers);
                                    message.put(Constants.TAG_CHAT_TIME, unixStamp);
                                    message.put(Constants.TAG_MESSAGE_ID, messageId);
                                    message.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                                    message.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                                    message.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                                    message.put(Constants.TAG_MESSAGE_TYPE, "add_member");
                                    message.put(Constants.TAG_MESSAGE, getString(R.string.add) + " " + newMembers.length() + " " + getString(R.string.participant));
                                    message.put(Constants.TAG_GROUP_ADMIN_ID, GetSet.getUserId());

                                    socketConnection.startGroupChat(message);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        finish();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.group_limit), Toast.LENGTH_SHORT).show();
                }
            } else {
                if (groupList.size() == 0) {
                    makeToast(getString(R.string.select_atleast_one_members));
                } else {
                    if (dbhelper.getGroupMemberSize(groupId) <= 50) {
                        Intent createGroup = new Intent(getApplicationContext(), CreateGroupActivity.class);
                        createGroup.putExtra(Constants.TAG_GROUP_LIST, (Serializable) groupList);
                        startActivity(createGroup);
                        finish();
                    } else {
                        Toast.makeText(this, getString(R.string.group_limit), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void createStatus(String localFileName) {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else {
            if (userList.size() == 0) {
                makeToast(getString(R.string.select_atleast_one_members));
            } else {
                if (localFileName == null) localFileName = "";
                try {
                    long storyTime = System.currentTimeMillis();
                    String expiryTime = String.valueOf((storyTime + Constants.STATUS_EXPIRY_TIME));
                    storyTime = storyTime / 1000;
                    RandomString randomString = new RandomString(10);
                    String storyId = GetSet.getUserId() + randomString.nextString();
                    JSONObject jobj = new JSONObject();
                    jobj.put(Constants.TAG_STORY_ID, storyId);
                    jobj.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                    jobj.put(Constants.TAG_MESSAGE, map.get(Constants.TAG_MESSAGE));
                    jobj.put(Constants.TAG_STORY_TYPE, map.get(Constants.TAG_TYPE));
                    jobj.put(Constants.TAG_ATTACHMENT, map.get(Constants.TAG_ATTACHMENT));
                    jobj.put(Constants.TAG_THUMBNAIL, map.get(Constants.TAG_THUMBNAIL));
                    jobj.put(Constants.TAG_STORY_DATE, getStatusDate());
                    jobj.put(Constants.TAG_STORY_TIME, "" + storyTime);
                    jobj.put(Constants.TAG_EXPIRY_TIME, expiryTime);
                    jobj.put(Constants.TAG_USER_ID, GetSet.getUserId());
                    jobj.put("device_type", "android");

                    JSONArray membersArray = getMemberArray();

                    jobj.put(Constants.TAG_STORY_MEMBERS, membersArray);
                    Log.v("startStatus", "startStatus=" + jobj);
                    socketConnection.createStory(jobj);

                    dbhelper.createStatus(storyId, "" + storyTime, map.get(Constants.TAG_TYPE), GetSet.getUserId(),
                            localFileName, ApplicationClass.encryptMessage(map.get(Constants.TAG_MESSAGE)), "1",
                            map.get(Constants.TAG_THUMBNAIL), expiryTime, membersArray.toString());
                    closeActivity();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public String getStatusDate() {
        try {
            DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            Date netDate = (new Date(System.currentTimeMillis() * 1000));
            return sdf.format(netDate);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "xx";
        }
    }

    private JSONArray getMemberArray() throws JSONException {
        JSONArray memberArray = new JSONArray();
        for (String id : userList) {
            JSONObject idObj = new JSONObject();
            idObj.put("member_id", id);
            memberArray.put(idObj);
        }
        return memberArray;
    }

    @Override
    public void onUploadListen(String attachment, String progress, String localFileName) {
        if (progress.equals("completed")) {
            map.put(Constants.TAG_ATTACHMENT, attachment);
            createStatus(localFileName);
            Toast.makeText(this, getString(R.string.file_uploaded), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.file_upload_error), Toast.LENGTH_SHORT).show();
        }
        closeActivity();
    }

    @Override
    public void onBackPressed() {
        backbtn.performClick();
    }

    private void closeActivity() {
        hideLoading();
        setResult(Activity.RESULT_OK);
        finish();
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> implements Filterable {

        List<ContactsData.Result> Items;
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

        public class SearchFilter extends Filter {
            private RecyclerViewAdapter mAdapter;

            private SearchFilter(RecyclerViewAdapter mAdapter) {
                super();
                this.mAdapter = mAdapter;
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                filteredList.clear();
                final FilterResults results = new FilterResults();
                if (constraint.length() == 0) {
                    filteredList.addAll(contactList);
                } else {
                    final String filterPattern = constraint.toString().toLowerCase().trim();
                    for (final ContactsData.Result result : contactList) {
                        if (result.user_name.toLowerCase().startsWith(filterPattern)) {
                            filteredList.add(result);
                        }
                    }
                }
                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                this.mAdapter.notifyDataSetChanged();
                if (filteredList.size() == 0) {
                    nullLay.setVisibility(View.VISIBLE);
                } else {
                    nullLay.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_blocked_contacts, parent, false);

            return new RecyclerViewAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final RecyclerViewAdapter.MyViewHolder holder, int position) {

            if (ContextCompat.checkSelfPermission(NewGroupActivity.this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                holder.name.setText(filteredList.get(position).user_name);
            } else {
                holder.name.setText(filteredList.get(position).phone_no);
            }

            if (filteredList.get(position).blockedme.equals("block")) {
                Glide.with(context).load(R.drawable.temp)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                        .into(holder.profileimage);
            } else {
                DialogActivity.setProfileImage(dbhelper.getContactDetail(filteredList.get(position).user_id), holder.profileimage, context);
            }

            if (userList.contains(filteredList.get(position).user_id)) {
                holder.btnSelect.setChecked(true);
            } else {
                holder.btnSelect.setChecked(false);
            }
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
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
                    case R.id.btnSelect:
                    case R.id.parentlay:
                        if (!filteredList.get(getAdapterPosition()).blockedbyme.equals("block") &&
                                !filteredList.get(getAdapterPosition()).blockedme.equals("block")) {
                            if (!userList.contains(filteredList.get(getAdapterPosition()).user_id)) {
                                userList.add(filteredList.get(getAdapterPosition()).user_id);
                                groupList.add(filteredList.get(getAdapterPosition()));
                                btnSelect.setChecked(true);
                            } else {
                                btnSelect.setChecked(false);
                                userList.remove(filteredList.get(getAdapterPosition()).user_id);
                                groupList.remove(filteredList.get(getAdapterPosition()));
                                isSelectAll = false;
                                btnSelectAll.setChecked(false);
                            }

                            if (filteredList.size() == userList.size()) {
                                btnSelectAll.setChecked(true);
                            } else {
                                btnSelectAll.setChecked(false);
                            }
                            notifyDataSetChanged();
                            if (groupAdapter != null) {
                                groupAdapter.notifyDataSetChanged();
                            }
                            setTxtSubtitle(userList.size());
                        } else {
                            btnSelect.setChecked(false);
                            openAlertDialog(filteredList.get(getAdapterPosition()));
                        }
                        break;
                    /*case R.id.btnSelect:
                        if (!userList.contains(filteredList.get(getAdapterPosition()).user_id)) {
                            userList.add(filteredList.get(getAdapterPosition()).user_id);
                            groupList.add(filteredList.get(getAdapterPosition()));
                            btnSelect.setChecked(true);
                        } else {
                            btnSelect.setChecked(false);
                            userList.remove(filteredList.get(getAdapterPosition()).user_id);
                            groupList.remove(filteredList.get(getAdapterPosition()));
                            isSelectAll = false;
                            btnSelectAll.setChecked(false);
                            selectAllContact();
                        }
                        notifyDataSetChanged();

                        if (groupAdapter != null) {
                            groupAdapter.notifyDataSetChanged();
                        }
                        setTxtSubtitle(userList.size());
                        break;
*/
                }
            }
        }
    }

    private void openAlertDialog(ContactsData.Result result) {
        final Dialog dialog = new Dialog(NewGroupActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.default_popup);
        dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 90 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView title = dialog.findViewById(R.id.title);
        TextView yes = dialog.findViewById(R.id.yes);
        TextView no = dialog.findViewById(R.id.no);
        yes.setText(R.string.okay);
        no.setText(getString(R.string.nope));
        title.setText(getString(R.string.could_not_add) + " " + ApplicationClass.getContactName(getApplicationContext(), result.phone_no, result.country_code,result.user_name));
        no.setVisibility(View.GONE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
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
                if (result.privacy_profile_image.equalsIgnoreCase(TAG_MY_CONTACTS)) {
                    if (result.contactstatus != null && result.contactstatus.equalsIgnoreCase(TRUE)) {
                        Glide.with(context).load(Constants.USER_IMG_PATH + result.user_image).thumbnail(0.5f)
                                .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                                .into(holder.profileimage);
                    } else {
                        Glide.with(context).load(R.drawable.temp).thumbnail(0.5f)
                                .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                                .into(holder.profileimage);
                    }

                } else if (result.privacy_profile_image.equalsIgnoreCase(TAG_NOBODY)) {
                    Glide.with(context).load(R.drawable.temp).thumbnail(0.5f)
                            .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                            .into(holder.profileimage);
                } else {
                    Glide.with(context).load(Constants.USER_IMG_PATH + result.user_image).thumbnail(0.5f)
                            .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                            .into(holder.profileimage);
                }
            }

            holder.txtName.setText(result.user_name);

        }

        @Override
        public int getItemCount() {
            return groupList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

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

                parentlay.setOnClickListener(this);
                btnRemove.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
                    case R.id.btnRemove:
                        if (userList.contains(groupList.get(getAdapterPosition()).user_id)) {
                            userList.remove(groupList.get(getAdapterPosition()).user_id);
                            groupList.remove(groupList.get(getAdapterPosition()));
                        }
                        notifyDataSetChanged();

                        if (recyclerViewAdapter != null) {
                            recyclerViewAdapter.notifyDataSetChanged();
                        }
                        setTxtSubtitle(userList.size());
                        break;
                }
            }
        }

    }

    public void setTxtSubtitle(int count) {
        txtSubtitle.setText(" " + count + " " + getString(R.string.of) + " " +
                filteredList.size() + " " + getString(R.string.selected));
    }


    void imageUpload() {
        String type = map.get(Constants.TAG_TYPE);
        try {
            MessagesData mdata;
            byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(map.get(Constants.TAG_ATTACHMENT)));
            if (type.equals("image")) {
                mdata = getExternalData(type, map.get(Constants.TAG_ATTACHMENT), "");
                uploadImage(bytes, mdata.attachment, mdata, "");
            } else {
                Log.i(TAG, "imageUpload: " + map.get(Constants.TAG_ATTACHMENT));
                Bitmap thumb = ThumbnailUtils.createVideoThumbnail(map.get(Constants.TAG_ATTACHMENT), MediaStore.Video.Thumbnails.MINI_KIND);
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
                String imageStatus = storageManager.saveToSdCard(thumb, "sent", timestamp + ".jpg");
                mdata = getExternalData(type, map.get(Constants.TAG_ATTACHMENT), map.get(Constants.TAG_THUMBNAIL));
                if (imageStatus.equals("success")) {
                    File file = storageManager.getImage("sent", timestamp + ".jpg");
                    String imagePath = file.getAbsolutePath();
                    bytes = FileUtils.readFileToByteArray(new File(imagePath));
                    uploadImage(bytes, imagePath, mdata, map.get(Constants.TAG_ATTACHMENT));
                }
            }
        } catch (IOException ex) {
            hideLoading();
            ex.printStackTrace();
        }
    }

    private void uploadImage(byte[] imageBytes, final String imagePath, final MessagesData mdata, final String filePath) {
        RequestBody requestFile = RequestBody.create(imageBytes, MediaType.parse("openImage/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("attachment", "openImage.jpg", requestFile);

        RequestBody userid = RequestBody.create(GetSet.getUserId(), MediaType.parse("multipart/form-data"));
        Call<HashMap<String, String>> call3 = statusUploadInterface.upmychat(GetSet.getToken(), body, userid);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> data = response.body();
                Log.v(TAG, "uploadImageResponse: " + data);
                Log.d(TAG, "onResponse: " + new Gson().toJson(mdata));
                if (data.get(Constants.TAG_STATUS).equals("true")) {
                    File dir = new File(imagePath);
                    String fileName = getString(R.string.app_name) + "_" + System.currentTimeMillis() + ".JPG";
                    if (dir.exists()) {
                        if (mdata.message_type.equals("image")) {
                            storageManager.moveFilesToSentPath(NewGroupActivity.this, StorageManager.TAG_SENT, imagePath, fileName);
                            map.put(Constants.TAG_ATTACHMENT, data.get(Constants.TAG_USER_IMAGE));
                            map.put(Constants.TAG_THUMBNAIL, "");
                            createStatus(fileName);
                        } else if (mdata.message_type.equals("video")) {
                            map.put(Constants.TAG_THUMBNAIL, data.get(Constants.TAG_USER_IMAGE));
                            mdata.thumbnail = data.get(Constants.TAG_USER_IMAGE);
                            Intent service = new Intent(NewGroupActivity.this, FileUploadService.class);
                            Bundle b = new Bundle();
                            b.putSerializable("mdata", mdata);
                            b.putString("filepath", filePath);
                            b.putString("chatType", "status");
                            service.putExtras(b);
                            startService(service);
                        }
                    }
                } else {
                    // dbhelper.updateMessageData(mdata.message_id, Constants.TAG_PROGRESS, "error");
                    closeActivity();
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                Log.e(TAG, "uploadImageOnFailure: " + t.getMessage());
                call.cancel();
                closeActivity();
                //dbhelper.updateMessageData(mdata.message_id, Constants.TAG_PROGRESS, "error");
            }
        });
    }

    private MessagesData getExternalData(String type, String imagePath, String filePath) {
        String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        RandomString randomString = new RandomString(10);
        String messageId = GetSet.getUserId() + randomString.nextString();


        MessagesData data = new MessagesData();
        data.user_id = GetSet.getUserId();
        data.message_type = type;
        data.message = map.get(Constants.TAG_MESSAGE);
        data.message_id = messageId;
        data.chat_time = unixStamp;
        data.delivery_status = "";
        data.progress = "";

        switch (type) {
            case "video":
                data.thumbnail = imagePath;
                data.attachment = filePath;
                break;
            case "image":
                data.thumbnail = "";
                data.attachment = imagePath;
                break;
            default:
                data.thumbnail = "";
                data.attachment = filePath;
                break;
        }
        return data;
    }

    private void showLoading() {
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideLoading() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
