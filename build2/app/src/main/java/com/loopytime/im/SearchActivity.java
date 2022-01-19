package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.Utils;
import com.loopytime.model.ContactsData;
import com.loopytime.model.SearchData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.Manifest.permission.READ_CONTACTS;
import static com.loopytime.helper.Utils.getURLForResource;
import static com.loopytime.im.SearchActivity.RecyclerViewAdapter.VIEW_TYPE_CHANNELS;
import static com.loopytime.im.SearchActivity.RecyclerViewAdapter.VIEW_TYPE_CHANNEL_HEADER;
import static com.loopytime.im.SearchActivity.RecyclerViewAdapter.VIEW_TYPE_CHATS;
import static com.loopytime.im.SearchActivity.RecyclerViewAdapter.VIEW_TYPE_CHATS_HEADER;
import static com.loopytime.im.SearchActivity.RecyclerViewAdapter.VIEW_TYPE_GROUPS;
import static com.loopytime.im.SearchActivity.RecyclerViewAdapter.VIEW_TYPE_GROUP_HEADER;

public class SearchActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    TextView title;
    ImageView backbtn, searchbtn, optionbtn, cancelbtn;
    RecyclerView recyclerView;
    RecyclerViewAdapter recyclerViewAdapter;
    DatabaseHandler dbhelper;
    EditText searchView;
    RelativeLayout searchLay, mainLay;
    LinearLayout buttonLayout;
    List<SearchData> filteredList;
    List<SearchData> searchList = new ArrayList<>();
    List<SearchData> chatList = new ArrayList<>();
    List<SearchData> groupList = new ArrayList<>();
    List<SearchData> channelList = new ArrayList<>();
    String userId;
    LinearLayoutManager linearLayoutManager;
    ProgressDialog progressDialog;
    static ApiInterface apiInterface;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    int chatCount = 0, groupCount = 0, channelCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = SearchActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        title = findViewById(R.id.title);
        backbtn = findViewById(R.id.backbtn);
        searchbtn = findViewById(R.id.searchbtn);
        optionbtn = findViewById(R.id.optionbtn);
        recyclerView = findViewById(R.id.recyclerView);
        searchView = findViewById(R.id.searchView);
        buttonLayout = findViewById(R.id.buttonLayout);
        cancelbtn = findViewById(R.id.cancelbtn);
        searchLay = findViewById(R.id.searchLay);
        mainLay = findViewById(R.id.mainLay);

        if (ApplicationClass.isRTL()) {
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        title.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        searchbtn.setVisibility(View.VISIBLE);
        optionbtn.setVisibility(View.GONE);

        title.setText(getString(R.string.select_contact));
        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));

        dbhelper = DatabaseHandler.getInstance(this);
        backbtn.setOnClickListener(this);
        searchbtn.setOnClickListener(this);
        optionbtn.setOnClickListener(this);
        cancelbtn.setOnClickListener(this);
        searchbtn.performClick();

        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        /*searchView.setFilters(new InputFilter[] {
                new InputFilter.AllCaps() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        return String.valueOf(source).toLowerCase().replace(" ", "");
                    }
                }
        });*/

        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return false;
            }
        });

        SearchData data = new SearchData();

        ArrayList<HashMap<String, String>> chatList = dbhelper.getAllRecentsMessages(this);
        chatCount = chatList.size();
        if (chatCount > 0) {
            data = new SearchData();
            data.viewType = VIEW_TYPE_CHATS_HEADER;
            searchList.add(data);/*First item - Contact Header*/
            for (HashMap<String, String> hashMap : dbhelper.getAllRecentsMessages(this)) {
                data = new SearchData();
                data.viewType = VIEW_TYPE_CHATS;
                data.user_id = hashMap.get(Constants.TAG_USER_ID);
                data.user_name = hashMap.get(Constants.TAG_USER_NAME);
                data.user_image = hashMap.get(Constants.TAG_USER_IMAGE);
                data.unreadCount = hashMap.get(Constants.TAG_UNREAD_COUNT);
                if (hashMap.get(Constants.TAG_MESSAGE) != null) {
                    data.message = hashMap.get(Constants.TAG_MESSAGE);
                } else if (hashMap.get(Constants.TAG_ABOUT) != null) {
                    data.message = hashMap.get(Constants.TAG_ABOUT);
                } else {
                    data.message = "";
                }
                searchList.add(data);
            }
        }

        List<HashMap<String, String>> groupList = dbhelper.getGroupRecentMessages(this);
        groupCount = groupList.size();
        if (groupCount > 0) {
            data = new SearchData();
            data.viewType = VIEW_TYPE_GROUP_HEADER;
            searchList.add(data); /*First item - Group Header*/
            for (HashMap<String, String> hashMap : groupList) {
                data = new SearchData();
                data.viewType = VIEW_TYPE_GROUPS;
                data.groupId = hashMap.get(Constants.TAG_GROUP_ID);
                data.groupName = hashMap.get(Constants.TAG_GROUP_NAME);
                data.groupImage = hashMap.get(Constants.TAG_UNREAD_COUNT);
                data.message = hashMap.get(Constants.TAG_MESSAGE) != null ? hashMap.get(Constants.TAG_MESSAGE) : "";
                searchList.add(data);
            }

        }

        ArrayList<HashMap<String, String>> channelList = dbhelper.getChannelRecentMessages(getApplicationContext());
        channelCount = channelList.size();
        if (channelCount > 0) {
            data = new SearchData();
            data.viewType = VIEW_TYPE_CHANNEL_HEADER;
            searchList.add(data); /*First item - Channel Header*/
            for (HashMap<String, String> hashMap : channelList) {
                data = new SearchData();
                data.viewType = VIEW_TYPE_CHANNELS;
                data.channelId = hashMap.get(Constants.TAG_CHANNEL_ID);
                data.channelName = hashMap.get(Constants.TAG_CHANNEL_NAME);
                data.channelImage = hashMap.get(Constants.TAG_CHANNEL_IMAGE);
                data.message = (hashMap.get(Constants.TAG_MESSAGE) != null && !TextUtils.isEmpty(hashMap.get(Constants.TAG_MESSAGE)))
                        ? hashMap.get(Constants.TAG_MESSAGE) : hashMap.get(Constants.TAG_CHANNEL_DES);
                searchList.add(data);
            }
        }

        filteredList = new ArrayList<>();
        filteredList.addAll(searchList);

        recyclerViewAdapter = new RecyclerViewAdapter(this);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerViewAdapter.notifyDataSetChanged();

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
            case R.id.searchbtn:
                title.setVisibility(View.GONE);
                searchLay.setVisibility(View.VISIBLE);
                buttonLayout.setVisibility(View.GONE);
                ApplicationClass.showKeyboard(this, searchView);
                break;
            case R.id.cancelbtn:
                searchView.setText("");
                break;
        }
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter implements Filterable {

        public static final int VIEW_TYPE_CHATS_HEADER = 1;
        public static final int VIEW_TYPE_CHATS = 2;
        public static final int VIEW_TYPE_GROUP_HEADER = 3;
        public static final int VIEW_TYPE_GROUPS = 4;
        public static final int VIEW_TYPE_CHANNEL_HEADER = 5;
        public static final int VIEW_TYPE_CHANNELS = 6;

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
                filteredList = new ArrayList<>();
                chatList = new ArrayList<>();
                channelList = new ArrayList<>();
                groupList = new ArrayList<>();
                final FilterResults results = new FilterResults();
                if (constraint.length() == 0) {
                    filteredList.addAll(searchList);
                } else {
                    final String filterPattern = constraint.toString().toLowerCase().trim();

                    for (SearchData data : searchList) {
                        if (data.viewType == VIEW_TYPE_CHATS_HEADER) {
                            chatList.add(data);
                        } else if (data.viewType == VIEW_TYPE_CHATS) {
                            if (data.user_name.toLowerCase().startsWith(filterPattern)) {
                                chatList.add(data);
                            }
                        } else if (data.viewType == VIEW_TYPE_GROUP_HEADER) {
                            groupList.add(data);
                        } else if (data.viewType == VIEW_TYPE_GROUPS) {
                            if (data.groupName.toLowerCase().startsWith(filterPattern)) {
                                groupList.add(data);
                            }
                        } else if (data.viewType == VIEW_TYPE_CHANNEL_HEADER) {
                            channelList.add(data);
                        } else if (data.viewType == VIEW_TYPE_CHANNELS) {
                            if (data.channelName.toLowerCase().startsWith(filterPattern)) {
                                channelList.add(data);
                            }
                        }
                    }

                    if (chatList.size() > 1) {
                        filteredList.addAll(chatList);
                    }
                    if (groupList.size() > 1) {
                        filteredList.addAll(groupList);
                    }
                    if (channelList.size() > 1) {
                        filteredList.addAll(channelList);
                    }

                }

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                this.mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = null;

            if (viewType == VIEW_TYPE_CHATS_HEADER) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_search_header, parent, false);
                return new RecyclerViewAdapter.HeaderViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_CHATS) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_item, parent, false);
                return new RecyclerViewAdapter.MyViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_GROUP_HEADER) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_search_header, parent, false);
                return new RecyclerViewAdapter.HeaderViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_GROUPS) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_item, parent, false);
                return new RecyclerViewAdapter.MyViewHolder(itemView);
            } else if (viewType == VIEW_TYPE_CHANNEL_HEADER) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_search_header, parent, false);
                return new RecyclerViewAdapter.HeaderViewHolder(itemView);
            } else {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_item, parent, false);
                return new RecyclerViewAdapter.MyViewHolder(itemView);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            if (getItemViewType(position) == VIEW_TYPE_CHATS_HEADER) {
                ((HeaderViewHolder) holder).txtHeader.setText(getString(R.string.chat));
            } else if (getItemViewType(position) == VIEW_TYPE_GROUP_HEADER) {
                ((HeaderViewHolder) holder).txtHeader.setText(getString(R.string.group));
            } else if (getItemViewType(position) == VIEW_TYPE_CHANNEL_HEADER) {
                ((HeaderViewHolder) holder).txtHeader.setText(getString(R.string.channels));
            } else {
                final SearchData data = filteredList.get(position);
                if (data.chatTime != null) {
                    ((MyViewHolder) holder).time.setText(Utils.getFormattedDate(context, Long.parseLong(data.chatTime.replace(".0", ""))));
                }
                ((MyViewHolder) holder).message.setText(data.message != null ? data.message : "");

                if (getItemViewType(position) == VIEW_TYPE_CHATS) {
                    if (ContextCompat.checkSelfPermission(SearchActivity.this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        ((MyViewHolder) holder).name.setText(data.user_name);
                    } else {
                        ((MyViewHolder) holder).name.setText(data.phone_no);
                    }

                    if (data.user_id != null) {
                        ContactsData.Result result = dbhelper.getContactDetail(data.user_id);
                        DialogActivity.setProfileImage(result, ((MyViewHolder) holder).profileimage, context);
                    } else {
                        Glide.with(context).load(R.drawable.temp)
                                .apply(new RequestOptions().placeholder(R.drawable.temp).error(R.drawable.temp))
                                .into(((MyViewHolder) holder).profileimage);
                    }

                } else if ((getItemViewType(position) == VIEW_TYPE_GROUPS)) {
                    ((MyViewHolder) holder).name.setText(data.groupName);
                    Glide.with(context).load(Constants.GROUP_IMG_PATH + filteredList.get(position).user_image)
                            .apply(new RequestOptions().placeholder(R.drawable.ic_group_square).error(R.drawable.ic_group_square))
                            .into(((MyViewHolder) holder).profileimage);

                } else if (getItemViewType(position) == VIEW_TYPE_CHANNELS) {
                    ((MyViewHolder) holder).name.setText("" + filteredList.get(position).channelName);
                    Glide.with(context).load(Constants.CHANNEL_IMG_PATH + filteredList.get(position).channelImage)
                            .apply(new RequestOptions().placeholder(R.drawable.ic_channel_square).error(R.drawable.ic_channel_square))
                            .into(((MyViewHolder) holder).profileimage);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (filteredList.get(position).viewType == VIEW_TYPE_CHATS_HEADER) {
                return VIEW_TYPE_CHATS_HEADER;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_CHATS) {
                return VIEW_TYPE_CHATS;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_GROUP_HEADER) {
                return VIEW_TYPE_GROUP_HEADER;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_GROUPS) {
                return VIEW_TYPE_GROUPS;
            } else if (filteredList.get(position).viewType == VIEW_TYPE_CHANNEL_HEADER) {
                return VIEW_TYPE_CHANNEL_HEADER;
            } else {
                return VIEW_TYPE_CHANNELS;
            }
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay;
            RelativeLayout unseenLay;
            TextView name, message, time, unseenCount;
            ImageView tickimage, typeicon;
            CircleImageView profileimage;
            View profileview;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                message = view.findViewById(R.id.message);
                time = view.findViewById(R.id.time);
                name = view.findViewById(R.id.name);
                profileimage = view.findViewById(R.id.profileimage);
                tickimage = view.findViewById(R.id.tickimage);
                typeicon = view.findViewById(R.id.typeicon);
                unseenLay = view.findViewById(R.id.unseenLay);
                unseenCount = view.findViewById(R.id.unseenCount);
                profileview = view.findViewById(R.id.profileview);

                parentlay.setOnClickListener(this);
                profileimage.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
                        if (filteredList.get(getAdapterPosition()).user_id != null) {
                            Intent i = new Intent(SearchActivity.this, ChatActivity.class);
                            i.putExtra(Constants.TAG_USER_ID, filteredList.get(getAdapterPosition()).user_id);
                            startActivity(i);
                        } else if (filteredList.get(getAdapterPosition()).groupId != null) {
                            Intent i = new Intent(SearchActivity.this, GroupChatActivity.class);
                            i.putExtra(Constants.TAG_GROUP_ID, filteredList.get(getAdapterPosition()).groupId);
                            startActivity(i);
                        } else if (filteredList.get(getAdapterPosition()).channelId != null) {
                            Intent i = new Intent(SearchActivity.this, ChannelChatActivity.class);
                            i.putExtra(Constants.TAG_CHANNEL_ID, filteredList.get(getAdapterPosition()).channelId);
                            startActivity(i);
                        }
                        break;
                    case R.id.profileimage:
                        if (getItemViewType() != VIEW_TYPE_CHANNELS) {
                            openUserDialog(profileview, filteredList.get(getAdapterPosition()));
                        }
                        break;
                }
            }
        }

        private void openUserDialog(View view, SearchData data) {
            Intent i = new Intent(SearchActivity.this, DialogActivity.class);
            if (data.user_id != null) {
                i.putExtra(Constants.TAG_USER_ID, data.user_id);
                i.putExtra(Constants.TAG_USER_NAME, data.user_name);
                i.putExtra(Constants.TAG_USER_IMAGE, data.user_image);
            } else if (data.groupId != null) {
                i.putExtra(Constants.TAG_GROUP_ID, data.groupId);
                i.putExtra(Constants.TAG_GROUP_NAME, data.groupName);
                i.putExtra(Constants.TAG_GROUP_IMAGE, data.groupImage);
            }
            //Pair<View, String> bodyPair = Pair.create(view, getURLForResource(R.drawable.temp));
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(SearchActivity.this, view, getURLForResource(R.drawable.temp));
            startActivity(i, options.toBundle());
        }

        public class HeaderViewHolder extends RecyclerView.ViewHolder {

            TextView txtHeader;

            public HeaderViewHolder(View view) {
                super(view);
                txtHeader = view.findViewById(R.id.txtHeader);
            }
        }
    }
}
