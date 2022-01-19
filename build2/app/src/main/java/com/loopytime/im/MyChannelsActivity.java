package com.loopytime.im;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.model.ChannelResult;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyChannelsActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    TextView title, nullText;
    ImageView backbtn, searchbtn, optionbtn, cancelbtn, fab;
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;
    RecyclerViewAdapter channelAdapter;
    DatabaseHandler dbhelper;
    EditText searchView;
    RelativeLayout searchLay, mainLay;
    LinearLayout nullLay;
    LinearLayout buttonLayout, btnNext;
    static ApiInterface apiInterface;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    List<ChannelResult.Result> channelList = new ArrayList<>();
    List<ChannelResult.Result> filteredList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_contact);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = MyChannelsActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);

        title = findViewById(R.id.title);
        backbtn = findViewById(R.id.backbtn);
        searchbtn = findViewById(R.id.searchbtn);
        optionbtn = findViewById(R.id.optionbtn);
        recyclerView = findViewById(R.id.recyclerView);
        searchView = findViewById(R.id.searchView);
        buttonLayout = findViewById(R.id.buttonLayout);
        cancelbtn = findViewById(R.id.cancelbtn);
        searchLay = findViewById(R.id.searchLay);
        btnNext = findViewById(R.id.btnNext);
        mainLay = findViewById(R.id.mainLay);
        nullLay = findViewById(R.id.nullLay);
        nullText = findViewById(R.id.nullText);
        fab = findViewById(R.id.fab);

        if(ApplicationClass.isRTL()){
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        title.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        searchbtn.setVisibility(View.VISIBLE);
        optionbtn.setVisibility(View.GONE);

        title.setText(getString(R.string.mychannels));
        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.floating_channel));

        dbhelper = DatabaseHandler.getInstance(this);
        backbtn.setOnClickListener(this);
        searchbtn.setOnClickListener(this);
        optionbtn.setOnClickListener(this);
        cancelbtn.setOnClickListener(this);
        btnNext.setOnClickListener(this);

        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        channelList.addAll(dbhelper.getMyChannels(GetSet.getUserId()));
        filteredList.addAll(channelList);

        channelAdapter = new RecyclerViewAdapter(this);
        recyclerView.setAdapter(channelAdapter);
        channelAdapter.notifyDataSetChanged();

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
                channelAdapter.getFilter().filter(s.toString());
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
                if (searchLay.getVisibility() == View.VISIBLE) {
                    searchView.setText("");
                    searchLay.setVisibility(View.GONE);
                    title.setVisibility(View.VISIBLE);
                    buttonLayout.setVisibility(View.VISIBLE);
                    ApplicationClass.hideSoftKeyboard(this, searchView);
                } else {
                    finish();
                }
                break;
            case R.id.searchbtn:
                title.setVisibility(View.GONE);
                searchLay.setVisibility(View.VISIBLE);
                buttonLayout.setVisibility(View.GONE);
                ApplicationClass.showKeyboard(this, searchView);
                break;
            case R.id.optionbtn:

                break;
            case R.id.cancelbtn:
                searchView.setText("");
                break;
            case R.id.btnNext:
                Intent s = new Intent(getApplicationContext(), CreateChannelActivity.class);
                s.putExtra(Constants.TAG_USER_ID, GetSet.getUserId());
                startActivity(s);
                break;
        }
    }

    @Override
    protected void onResume() {
        btnNext.setVisibility(View.VISIBLE);
        nullText.setText(R.string.no_channels_yet_buddy);

        if (channelAdapter != null) {
            channelList.clear();
            channelList = dbhelper.getMyChannels(GetSet.getUserId());
            filteredList.clear();
            filteredList.addAll(channelList);
            channelAdapter.notifyDataSetChanged();
        }
        if (filteredList.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
        } else {
            nullLay.setVisibility(View.GONE);
        }
        super.onResume();
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> implements Filterable {

        private RecyclerViewAdapter.SearchFilter mFilter;
        Context context;

        public RecyclerViewAdapter(Context context) {
            mFilter = new SearchFilter(RecyclerViewAdapter.this);
            this.context = context;
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
                    filteredList.addAll(channelList);
                } else {
                    final String filterPattern = constraint.toString().toLowerCase().trim();
                    for (final ChannelResult.Result result : channelList) {
                        if (result.channelName != null) {
                            if (result.channelName.toLowerCase().startsWith(filterPattern)) {
                                filteredList.add(result);
                            }
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
            }
        }

        @Override
        public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item, parent, false);

            return new RecyclerViewAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final RecyclerViewAdapter.MyViewHolder holder, int position) {

            final ChannelResult.Result result = filteredList.get(position);
            holder.name.setText(result.channelName);
            holder.message.setText(result.channelDes);

            if (result.channelType.equalsIgnoreCase(Constants.TAG_PRIVATE)) {
                holder.privateImage.setVisibility(View.VISIBLE);
            } else {
                holder.privateImage.setVisibility(View.GONE);
            }

            Glide.with(context).load(Constants.CHANNEL_IMG_PATH + result.channelImage).thumbnail(0.5f)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                    .into(holder.profileimage);
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay, messageLay;
            RelativeLayout unseenLay;
            TextView name, message, time, unseenCount, typing;
            ImageView tickimage, typeicon, mute, privateImage;
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
                typing = view.findViewById(R.id.typing);
                messageLay = view.findViewById(R.id.messageLay);
                mute = view.findViewById(R.id.mute);
                privateImage = view.findViewById(R.id.privateImage);

                parentlay.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
                        Intent i = new Intent(context, ChannelChatActivity.class);
                        i.putExtra(Constants.TAG_CHANNEL_ID, filteredList.get(getAdapterPosition()).channelId);
                        startActivity(i);
                        break;
                }
            }
        }
    }
}
