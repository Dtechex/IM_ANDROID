package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.OnLoadMoreListener;
import com.loopytime.model.ChannelResult;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;

public class AllChannelsActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    TextView title, nullText;
    Toolbar toolbar;
    EditText searchView;
    RelativeLayout searchLay;
    LinearLayout nullLay;
    LinearLayout buttonLayout, headerLayout;
    FrameLayout mainLay;
    ImageView backbtn, searchbtn, optionbtn, cancelbtn;
    DatabaseHandler dbhelper;
    static ApiInterface apiInterface;
    RecyclerViewAdapter recyclerViewAdapter;
    RecyclerView recyclerView;
    List<ChannelResult.Result> channelList = new ArrayList<>();
    List<ChannelResult.Result> filteredList = new ArrayList<>();
    ProgressDialog progressDialog;
    int page = 0, limit = 20;
    boolean visible;
    LinearLayoutManager mLayoutManager;
    private boolean isTouched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_group);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        dbhelper = DatabaseHandler.getInstance(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        toolbar = findViewById(R.id.actionbar);
        title = toolbar.findViewById(R.id.title);
        searchView = toolbar.findViewById(R.id.searchView);
        cancelbtn = toolbar.findViewById(R.id.cancelbtn);
        searchbtn = toolbar.findViewById(R.id.searchbtn);
        optionbtn = toolbar.findViewById(R.id.optionbtn);
        backbtn = toolbar.findViewById(R.id.backbtn);
        searchLay = toolbar.findViewById(R.id.searchLay);
        buttonLayout = findViewById(R.id.buttonLayout);
        recyclerView = findViewById(R.id.recyclerView);
        nullLay = findViewById(R.id.nullLay);
        headerLayout = findViewById(R.id.headerLayout);
        mainLay = findViewById(R.id.mainLay);
        nullText = findViewById(R.id.nullText);

        headerLayout.setVisibility(View.GONE);
        toolbar.setVisibility(View.VISIBLE);
        title.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        searchbtn.setVisibility(View.VISIBLE);
        optionbtn.setVisibility(View.VISIBLE);

        if(ApplicationClass.isRTL()){
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        title.setText(R.string.all_channels);
        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));

        backbtn.setOnClickListener(this);
        searchbtn.setOnClickListener(this);
        optionbtn.setOnClickListener(this);
        cancelbtn.setOnClickListener(this);


        recyclerView.setLayoutManager(mLayoutManager);


        getAllChannels((!TextUtils.isEmpty("" + searchView.getText()) ? "" + searchView.getText() : "all"), page, limit);

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                page = 0;
                if (s.length() > 0) {
                    cancelbtn.setVisibility(View.VISIBLE);
                    getAllChannels("" + s, page, limit);
                } else {
                    cancelbtn.setVisibility(View.GONE);
                    getAllChannels("all", page, limit);
                }
//                recyclerViewAdapter.getFilter().filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        nullText.setText(getString(R.string.no_channels_yet_buddy));

        recyclerView.addOnItemTouchListener(touchListener);

    }

    private void getAllChannels(String searchString, int offSet, int limit) {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else {
            recyclerViewAdapter = null;
            recyclerView.removeOnItemTouchListener(touchListener);
            isTouched = false;
            Call<ChannelResult> call = apiInterface.getAllPublicChannels(GetSet.getToken(), GetSet.getUserId(), searchString, "" + offSet, "" + limit);
            call.enqueue(new Callback<ChannelResult>() {
                @Override
                public void onResponse(Call<ChannelResult> call, Response<ChannelResult> response) {
                    Log.i(TAG, "getAllChannels: " + new Gson().toJson(response.body()));
                    channelList = new ArrayList<>();
                    filteredList = new ArrayList<>();
                    if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                        if (response.body().result.size() > 0) {
                            nullLay.setVisibility(View.GONE);
                            page = page + limit;
                            channelList = response.body().result;
                            filteredList = response.body().result;
                        }
                    }

                    if (recyclerViewAdapter == null) {
                        recyclerViewAdapter = new RecyclerViewAdapter(AllChannelsActivity.this, filteredList, recyclerView);
                        recyclerView.setHasFixedSize(true);
                        recyclerView.setAdapter(recyclerViewAdapter);
                        setLoadMoreListener();
                        recyclerViewAdapter.notifyDataSetChanged();
                    } else {
                        recyclerViewAdapter.setChannelList(filteredList);
                        recyclerViewAdapter.refreshAdapter();
                        setLoadMoreListener();
                    }

                    if (channelList.size() == 0) {
                        nullLay.setVisibility(View.VISIBLE);
                    } else {
                        nullLay.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(Call<ChannelResult> call, Throwable t) {
                    call.cancel();
                    Log.e(TAG, "getAllChannels: " + t.getMessage());
                }
            });
        }
    }

    private void setLoadMoreListener() {
        recyclerViewAdapter.setOnLoadMoreListener(loadMoreListener);
    }

    OnLoadMoreListener loadMoreListener = new OnLoadMoreListener() {
        @Override
        public void onLoadMore() {
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    if (filteredList.size() > 0 && isTouched) {
                        getLoadMoreChannels(!TextUtils.isEmpty("" + searchView.getText()) ? ("" + searchView.getText()) : "all", page, limit);
                    }
                }
            });
        }
    };

    RecyclerView.OnItemTouchListener touchListener = new RecyclerView.OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            isTouched = true;
            Log.e(TAG, "onInterceptTouchEvent: "+isTouched );
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    };

    private void getLoadMoreChannels(String searchString, int offSet, int limit) {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else {
            filteredList.add(null);
            recyclerViewAdapter.notifyItemInserted(filteredList.size() - 1);
            Call<ChannelResult> call = apiInterface.getAllPublicChannels(GetSet.getToken(), GetSet.getUserId(), searchString, "" + offSet, "" + limit);
            call.enqueue(new Callback<ChannelResult>() {
                @Override
                public void onResponse(Call<ChannelResult> call, Response<ChannelResult> response) {
                    Log.i(TAG, "getLoadMoreChannels: " + new Gson().toJson(response.body()));
                    //   remove progress item
                    filteredList.remove(filteredList.size() - 1);
                    recyclerViewAdapter.notifyItemRemoved(filteredList.size());
                    if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                        if (response.body().result.size() > 0) {
                            page = page + limit;
                            List<ChannelResult.Result> tempList = filteredList;
                            tempList.addAll(response.body().result);
                            filteredList = tempList;
                            channelList = tempList;
                            recyclerViewAdapter.setChannelList(filteredList);
                            recyclerViewAdapter.setLoaded();
                        }
                    } else {
                        recyclerViewAdapter.notifyDataSetChanged();
                        if (page > 20)
                            makeToast(getString(R.string.no_more_channels_available));
                    }
                }

                @Override
                public void onFailure(Call<ChannelResult> call, Throwable t) {
                    call.cancel();
                    Log.e(TAG, "getLoadMoreChannels: " + t.getMessage());
                    //   remove progress item
                    filteredList.remove(filteredList.size() - 1);
                    recyclerViewAdapter.notifyItemRemoved(filteredList.size());
                }
            });
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
                Display display = this.getWindowManager().getDefaultDisplay();
                ArrayList<String> values = new ArrayList<>();
                values.add(getString(R.string.refresh));

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        R.layout.option_item, android.R.id.text1, values);
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = layoutInflater.inflate(R.layout.option_layout, null);
                final PopupWindow popup = new PopupWindow(AllChannelsActivity.this);
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
                popup.showAsDropDown(view);

                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        popup.dismiss();
                        if (position == 0) {
                            if (NetworkReceiver.isConnected()) {
                                page = 0;
                                getAllChannels((!TextUtils.isEmpty("" + searchView.getText()) ? "" + searchView.getText() : "all"), 0, limit);
                            } else {
                                makeToast(getString(R.string.no_internet_connection));
                            }
                        }
                    }
                });
                break;
            case R.id.cancelbtn:
                searchView.setText("");
                break;
        }
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter implements Filterable {

        private final int ITEM_VIEW_TYPE_ITEM = 0;
        private final int ITEM_VIEW_TYPE_FOOTER = 1;
        Context context;
        private int visibleThreshold = 5;
        private int lastVisibleItem, totalItemCount;
        private boolean loading;
        // Listener or the Interface defined in STEP 4
        private OnLoadMoreListener mOnLoadMoreListener;
        private RecyclerViewAdapter.SearchFilter mFilter;
        private List<ChannelResult.Result> filteredList = new ArrayList<>();
        private List<ChannelResult.Result> channelList = new ArrayList<>();

        public RecyclerViewAdapter(Context context, List<ChannelResult.Result> filteredList, RecyclerView recyclerView) {
            this.context = context;
            mFilter = new SearchFilter(RecyclerViewAdapter.this);
            this.filteredList = filteredList;
            this.channelList = filteredList;

            if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {

                final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView
                        .getLayoutManager();


                recyclerView
                        .addOnScrollListener(new RecyclerView.OnScrollListener() {
                            @Override
                            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                                    isTouched = true;

                                }
                            }

                            @Override
                            public void onScrolled(RecyclerView recyclerView,
                                                   int dx, int dy) {
                                super.onScrolled(recyclerView, dx, dy);

                                totalItemCount = linearLayoutManager.getItemCount();
                                lastVisibleItem = linearLayoutManager
                                        .findLastVisibleItemPosition();
                                if (!loading
                                        && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                                    // End has been reached
                                    // Do something
                                    if (mOnLoadMoreListener != null) {
                                        mOnLoadMoreListener.onLoadMore();
                                    }
                                    loading = true;
                                }
                            }
                        });
            }
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        public void setChannelList(List<ChannelResult.Result> channelList) {
            this.filteredList = channelList;
            this.channelList = channelList;
        }

        public void refreshAdapter() {
            notifyDataSetChanged();
        }

        public class SearchFilter extends Filter {
            private RecyclerViewAdapter mAdapter;

            private SearchFilter(RecyclerViewAdapter mAdapter) {
                super();
                this.mAdapter = mAdapter;
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults results = new FilterResults();
                if (constraint.length() == 0) {
                    filteredList = channelList;
                } else {
                    List<ChannelResult.Result> tempList = new ArrayList<>();
                    final String filterPattern = constraint.toString().toLowerCase().trim();
                    for (final ChannelResult.Result result : channelList) {
                        if (result.channelName != null) {
                            if (result.channelName.toLowerCase().startsWith(filterPattern)) {
                                tempList.add(result);
                            }
                        }
                    }
                    filteredList = tempList;
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
            RecyclerView.ViewHolder viewHolder = null;

            if (viewType == ITEM_VIEW_TYPE_ITEM) {
                View v = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.chat_item, parent, false);
                viewHolder = new MyViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_loading, parent, false);
                viewHolder = new ProgressViewHolder(v);
            }
            return viewHolder;

        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            if (holder instanceof MyViewHolder) {
                final ChannelResult.Result result = filteredList.get(position);
                ((MyViewHolder) holder).name.setText(result.channelName);
                ((MyViewHolder) holder).message.setText(result.channelDes);

                Glide.with(context).load(Constants.CHANNEL_IMG_PATH + result.channelImage).thumbnail(0.5f)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                        .into(((MyViewHolder) holder).profileimage);
            } else {
                ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
            }

        }

        public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
            this.mOnLoadMoreListener = onLoadMoreListener;
        }

        // This method is used to remove ProgressBar when data is loaded
        public void setLoaded() {
            loading = false;
        }

        @Override
        public int getItemViewType(int position) {
            if (filteredList.get(position) != null) {
                return ITEM_VIEW_TYPE_ITEM;
            } else {
                return ITEM_VIEW_TYPE_FOOTER;
            }
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay, messageLay;
            RelativeLayout unseenLay;
            TextView name, message, time, unseenCount, typing;
            ImageView tickimage, typeicon, mute;
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

                parentlay.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
                        String channelId = filteredList.get(getAdapterPosition()).channelId;
                        Intent i = new Intent();
                        if (dbhelper.isChannelExist(channelId)) {
                            ChannelResult.Result channelData = dbhelper.getChannelInfo(channelId);
                            if (channelData != null && !channelData.channelAdminId.equalsIgnoreCase(GetSet.getUserId())
                                    && channelData.subscribeStatus.equalsIgnoreCase("")) {
                                i = new Intent(context, ChannelRequestActivity.class);
                            } else {
                                i = new Intent(context, ChannelChatActivity.class);
                            }
                        } else {
                            i = new Intent(context, ChannelRequestActivity.class);
                            i.putExtra(Constants.TAG_ADMIN_ID, filteredList.get(getAdapterPosition()).channelAdminId);
                            i.putExtra(Constants.TAG_CHANNEL_NAME, filteredList.get(getAdapterPosition()).channelName);
                            i.putExtra(Constants.TAG_CHANNEL_DES, filteredList.get(getAdapterPosition()).channelDes);
                            i.putExtra(Constants.TAG_CHANNEL_IMAGE, filteredList.get(getAdapterPosition()).channelImage);
                            i.putExtra(Constants.TAG_CHANNEL_TYPE, filteredList.get(getAdapterPosition()).channelType);
                            i.putExtra(Constants.TAG_TOTAL_SUBSCRIBERS, filteredList.get(getAdapterPosition()).totalSubscribers);
                        }
                        i.putExtra(Constants.TAG_CHANNEL_ID, filteredList.get(getAdapterPosition()).channelId);
                        startActivity(i);
                        break;
                }
            }
        }

        class ProgressViewHolder extends RecyclerView.ViewHolder {
            public ProgressBar progressBar;

            public ProgressViewHolder(View itemView) {
                super(itemView);
                progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
            }
        }
    }

}
