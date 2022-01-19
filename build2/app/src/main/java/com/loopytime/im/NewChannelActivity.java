package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.model.ChannelResult;
import com.loopytime.model.ContactsData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.Manifest.permission.READ_CONTACTS;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.utils.Constants.TAG_MY_CONTACTS;
import static com.loopytime.utils.Constants.TAG_NOBODY;
import static com.loopytime.utils.Constants.TRUE;

public class NewChannelActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    TextView title, txtSubtitle,nullText, txtAddPeoples;
    ImageView backbtn, searchbtn, optionbtn, cancelbtn, fab;
    RecyclerView groupRecycler, contactRecycler;
    LinearLayoutManager linearLayoutManager;
    RecyclerViewAdapter recyclerViewAdapter;
    GroupAdapter groupAdapter;
    DatabaseHandler dbhelper;
    EditText searchView;
    RelativeLayout searchLay;
    RelativeLayout mainLay;
    LinearLayout buttonLayout, btnNext;
    List<ContactsData.Result> groupList = new ArrayList<>();
    List<ContactsData.Result> contactList = new ArrayList<>();
    List<ContactsData.Result> filteredList = new ArrayList<>();
    List<String> userList = new ArrayList<>();
    ProgressDialog progressDialog;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String channelId, channelName, channelDes, channelImage, channelType;
    boolean isEdit = false;
    SocketConnection socketConnection;
    ApiInterface apiInterface;
    LinearLayout nullLay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_group);

        socketConnection = SocketConnection.getInstance(this);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = NewChannelActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        isEdit = getIntent().getBooleanExtra(Constants.IS_EDIT, false);
        channelId = getIntent().getStringExtra(Constants.TAG_CHANNEL_ID);
        ChannelResult.Result channelData = dbhelper.getChannelInfo(channelId);
        channelName = channelData.channelName;
        channelDes = channelData.channelDes;
        channelType = channelData.channelType;
        channelImage = channelData.channelImage;

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
        fab = findViewById(R.id.fab);
        btnNext = findViewById(R.id.btnNext);
        nullLay = findViewById(R.id.nullLay);
        nullText = findViewById(R.id.nullText);
        txtAddPeoples = findViewById(R.id.txtAddPeoples);

        if(ApplicationClass.isRTL()){
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.floating_tick));
        fab.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white));

        title.setVisibility(View.VISIBLE);
        txtSubtitle.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        searchbtn.setVisibility(View.VISIBLE);
        optionbtn.setVisibility(View.GONE);

        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        searchbtn.setOnClickListener(this);
        backbtn.setOnClickListener(this);
        cancelbtn.setOnClickListener(this);
        btnNext.setOnClickListener(this);

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

        nullText.setText(getString(R.string.no_contact));
        title.setText(R.string.invite_subscribers);
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
        Collections.sort(contactList, new Comparator<ContactsData.Result>(){
            public int compare(ContactsData.Result obj1, ContactsData.Result obj2) {
                // ## Ascending order
                return obj1.user_name.compareToIgnoreCase(obj2.user_name); // To compare string values
            }
        });
        filteredList = new ArrayList<>();
        filteredList.addAll(contactList);

        txtSubtitle.setText(" " + 0 + " " + getString(R.string.of) + " " +
                filteredList.size() + " " + getString(R.string.selected));

        if (recyclerViewAdapter == null) {
            recyclerViewAdapter = new RecyclerViewAdapter(this);
            contactRecycler.setAdapter(recyclerViewAdapter);
            recyclerViewAdapter.notifyDataSetChanged();
        } else {
            recyclerViewAdapter.notifyDataSetChanged();
        }

        if (filteredList.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
            txtAddPeoples.setVisibility(View.GONE);
        } else {
            nullLay.setVisibility(View.GONE);
            txtAddPeoples.setVisibility(View.VISIBLE);
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

            case R.id.btnNext:
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put(Constants.TAG_CHANNEL_ID, channelId);
                        JSONArray userId = new JSONArray();
                        for (ContactsData.Result result : groupList) {
                            userId.put(result.user_id);
                        }
                        jsonObject.put(Constants.TAG_USER_ID, GetSet.getUserId());
                        jsonObject.put(Constants.TAG_INVITE_SUBSCRIBERS, "" + userId);
                        socketConnection.sendInvitesToSubscribers(jsonObject);
                        if (isEdit) {
                            finish();
                        } else {
                            Intent intent = new Intent(getApplicationContext(), ChannelChatActivity.class);
                            intent.putExtra(Constants.TAG_CHANNEL_ID, channelId);
                            startActivity(intent);
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;

        }
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
                if(filteredList.size() == 0) {
                    nullLay.setVisibility(View.VISIBLE);
                    contactRecycler.setVisibility(View.GONE);
                    txtAddPeoples.setVisibility(View.GONE);
                } else {
                    nullLay.setVisibility(View.GONE);
                    contactRecycler.setVisibility(View.VISIBLE);
                    txtAddPeoples.setVisibility(View.VISIBLE);
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

            if (ContextCompat.checkSelfPermission(NewChannelActivity.this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                holder.name.setText(filteredList.get(position).user_name);
            } else {
                holder.name.setText(filteredList.get(position).phone_no);
            }

            if (filteredList.get(position).blockedme.equals("block")) {
                holder.about.setVisibility(View.GONE);
                Glide.with(context).load(R.drawable.temp)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                        .into(holder.profileimage);
            } else {
                holder.about.setVisibility(View.VISIBLE);
                DialogActivity.setAboutUs(dbhelper.getContactDetail(filteredList.get(position).user_id), holder.about);
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
            TextView name, about;
            ImageView profileimage;
            View profileview;
            AppCompatRadioButton btnSelect;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                profileimage = view.findViewById(R.id.profileimage);
                name = view.findViewById(R.id.txtName);
                about = view.findViewById(R.id.txtAbout);
                profileview = view.findViewById(R.id.profileview);
                btnSelect = view.findViewById(R.id.btnSelect);

                btnSelect.setVisibility(View.VISIBLE);
                parentlay.setOnClickListener(this);
                btnSelect.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
                        if (!userList.contains(filteredList.get(getAdapterPosition()).user_id)) {
                            userList.add(filteredList.get(getAdapterPosition()).user_id);
                            groupList.add(filteredList.get(getAdapterPosition()));
                            btnSelect.setChecked(true);
                        } else {
                            btnSelect.setChecked(false);
                            userList.remove(filteredList.get(getAdapterPosition()).user_id);
                            groupList.remove(filteredList.get(getAdapterPosition()));
                        }
                        notifyDataSetChanged();
                        if (groupAdapter != null) {
                            groupAdapter.notifyDataSetChanged();
                        }
                        setTxtSubtitle(userList.size());
                        break;
                    case R.id.btnSelect:
                        if (!userList.contains(filteredList.get(getAdapterPosition()).user_id)) {
                            userList.add(filteredList.get(getAdapterPosition()).user_id);
                            groupList.add(filteredList.get(getAdapterPosition()));
                            btnSelect.setChecked(true);
                        } else {
                            btnSelect.setChecked(false);
                            userList.remove(filteredList.get(getAdapterPosition()).user_id);
                            groupList.remove(filteredList.get(getAdapterPosition()));
                        }
                        notifyDataSetChanged();

                        if (groupAdapter != null) {
                            groupAdapter.notifyDataSetChanged();
                        }
                        setTxtSubtitle(userList.size());
                        break;

                }
            }
        }
    }

    public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.MyViewHolder> {

        List<ContactsData.Result> groupList;
        Context context;

        public GroupAdapter(Context context, List<ContactsData.Result> groupList) {
            this.context = context;
            this.groupList = groupList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_select_member, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {

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
}