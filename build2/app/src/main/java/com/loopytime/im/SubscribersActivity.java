package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.OnLoadMoreListener;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.Utils;
import com.loopytime.model.ChannelResult;
import com.loopytime.model.ContactsData;
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

/**
 * Created by hitasoft on 20/6/18.
 */

public class SubscribersActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    TextView title, nullText;
    ImageView backbtn, searchbtn, optionbtn, cancelbtn;
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;
    RecyclerViewAdapter recyclerViewAdapter;
    DatabaseHandler dbhelper;
    EditText searchView;
    RelativeLayout searchLay, mainLay;
    LinearLayout nullLay;
    LinearLayout buttonLayout;
    List<ContactsData.Result> contactList = new ArrayList<>();
    List<ContactsData.Result> filteredList = new ArrayList<>();
    ProgressDialog progressDialog;
    static ApiInterface apiInterface;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String channelId;
    int page = 0, limit = 20;
    private boolean isTouched = false;
    private ChannelResult.Result channelData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_contact);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = SubscribersActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);

        channelId = getIntent().getStringExtra(Constants.TAG_CHANNEL_ID);
        channelData = dbhelper.getChannelInfo(channelId);
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
        nullLay = findViewById(R.id.nullLay);
        nullText = findViewById(R.id.nullText);

        title.setVisibility(View.GONE);
        backbtn.setVisibility(View.VISIBLE);
        searchbtn.setVisibility(View.GONE);
        optionbtn.setVisibility(View.VISIBLE);
        searchView.setVisibility(View.GONE);

        if(ApplicationClass.isRTL()){
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        title.setText(R.string.subscribers);
        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));

        dbhelper = DatabaseHandler.getInstance(this);
        backbtn.setOnClickListener(this);
        searchbtn.setOnClickListener(this);
        optionbtn.setOnClickListener(this);
        cancelbtn.setOnClickListener(this);

        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        nullText.setText(R.string.no_subscribers_found);

        getSubscribers(page, limit);

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

    private void getSubscribers(int offSet, int limit) {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else {
            progressDialog.show();
            Call<ContactsData> call = apiInterface.getChannelSubscribers(GetSet.getToken(), channelId, GetSet.getphonenumber(), "" + offSet, "" + limit);
            call.enqueue(new Callback<ContactsData>() {
                @Override
                public void onResponse(Call<ContactsData> call, Response<ContactsData> response) {
                    if (progressDialog.isShowing())
                        progressDialog.dismiss();
                    contactList = new ArrayList<>();
                    filteredList = new ArrayList<>();
                    Log.i(TAG, "getSubscribers: " + new Gson().toJson(response.body()));
                    if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                        if (response.body().result.size() > 0) {
                            page = page + limit;
                            contactList = response.body().result;
                            filteredList = response.body().result;
                        }
                    }

                    if (recyclerViewAdapter == null) {
                        recyclerViewAdapter = new RecyclerViewAdapter(getApplicationContext(), filteredList, recyclerView);
                        recyclerView.setHasFixedSize(true);
                        recyclerView.setAdapter(recyclerViewAdapter);
                        setLoadMoreListener();
                        recyclerViewAdapter.notifyDataSetChanged();
                    } else {
                        setLoadMoreListener();
                        recyclerViewAdapter.setContactList(filteredList);
                        recyclerViewAdapter.refreshAdapter();
                    }

                    if (contactList.size() == 0) {
                        nullLay.setVisibility(View.VISIBLE);
                    } else {
                        nullLay.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(Call<ContactsData> call, Throwable t) {
                    call.cancel();
                    Log.e(TAG, "getSubscribers: " + t.getMessage());
                    if (progressDialog.isShowing())
                        progressDialog.dismiss();
                }
            });
        }
    }

    private void setLoadMoreListener() {

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                isTouched = true;
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        recyclerViewAdapter.setLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (filteredList.size() > 0 && isTouched) {
                            getLoadMoreSubscribers(page, limit);
                        }
                    }
                });
            }
        });
    }

    private void getLoadMoreSubscribers(int offSet, int limit) {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
        } else {
            filteredList.add(null);
            recyclerViewAdapter.notifyItemInserted(filteredList.size() - 1);
            Call<ContactsData> call = apiInterface.getChannelSubscribers(GetSet.getToken(), channelId, GetSet.getphonenumber(), "" + offSet, "" + limit);
            call.enqueue(new Callback<ContactsData>() {
                @Override
                public void onResponse(Call<ContactsData> call, Response<ContactsData> response) {
                    filteredList.remove(filteredList.size() - 1);
                    recyclerViewAdapter.notifyItemRemoved(filteredList.size());
                    Log.i(TAG, "getLoadMoreSubscribers: " + new Gson().toJson(response.body()));
                    if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                        if (response.body().result.size() > 0) {
                            page = page + limit;
                            List<ContactsData.Result> tempList = filteredList;
                            tempList.addAll(response.body().result);
                            contactList = tempList;
                            filteredList = tempList;
                            recyclerViewAdapter.setContactList(filteredList);
                            recyclerViewAdapter.setLoaded();
                        }
                    } else {
                        recyclerViewAdapter.notifyDataSetChanged();
                        makeToast(getString(R.string.no_more_subscribers_available));
                    }
                }

                @Override
                public void onFailure(Call<ContactsData> call, Throwable t) {
                    call.cancel();
                    Log.e(TAG, "getLoadMoreSubscribers: " + t.getMessage());
                    //   remove progress item
                    filteredList.remove(filteredList.size() - 1);
                    recyclerViewAdapter.notifyItemRemoved(filteredList.size());
                }
            });
        }
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

    public class RecyclerViewAdapter extends RecyclerView.Adapter implements Filterable {

        private final int ITEM_VIEW_TYPE_ITEM = 0;
        private final int ITEM_VIEW_TYPE_FOOTER = 1;
        private OnLoadMoreListener mOnLoadMoreListener;
        private int visibleThreshold = 1;
        private int lastVisibleItem, totalItemCount;
        private boolean loading;
        Context context;
        private List<ContactsData.Result> filteredList = new ArrayList<>();
        private List<ContactsData.Result> subscribersList = new ArrayList<>();

        private SearchFilter mFilter;

        public RecyclerViewAdapter(Context context, List<ContactsData.Result> filteredList, RecyclerView recyclerView) {
            this.context = context;
            mFilter = new SearchFilter(RecyclerViewAdapter.this);
            this.filteredList = filteredList;
            this.subscribersList = filteredList;

            if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {

                final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView
                        .getLayoutManager();


                recyclerView
                        .addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        public void setContactList(List<ContactsData.Result> contactList) {
            this.filteredList = contactList;
            this.subscribersList = contactList;
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
                filteredList.clear();
                final FilterResults results = new FilterResults();
                if (constraint.length() == 0) {
                    filteredList.addAll(subscribersList);
                } else {
                    final String filterPattern = constraint.toString().toLowerCase().trim();
                    for (final ContactsData.Result result : subscribersList) {
                        if (result.user_name != null) {
                            if (result.user_name.toLowerCase().startsWith(filterPattern)) {
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
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = null;

            RecyclerView.ViewHolder viewHolder = null;

            if (viewType == ITEM_VIEW_TYPE_ITEM) {
                View v = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.contact_list_item, parent, false);
                viewHolder = new RecyclerViewAdapter.MyViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_loading, parent, false);
                viewHolder = new RecyclerViewAdapter.ProgressViewHolder(v);
            }
            return viewHolder;

        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            if (holder instanceof MyViewHolder) {
                if (!Utils.isChannelAdmin(channelData, filteredList.get(position).user_id)) {
                    ((MyViewHolder) holder).parentlay.setVisibility(View.VISIBLE);
                    if (filteredList.get(position).user_id.equalsIgnoreCase(GetSet.getUserId())) {
                        ((MyViewHolder) holder).name.setText(getString(R.string.you));
                    } else {
                        /*HashMap<String, String> map = ApplicationClass.getContactrNot(context, filteredList.get(position).phone_no);
                        if (map.get("isAlready").equals("true")) {
                            ((MyViewHolder) holder).name.setText(ApplicationClass.getContactName(context, filteredList.get(position).phone_no, filteredList.get(position).country_code));
                        } else {
                        }*/
                        ((MyViewHolder) holder).name.setText(filteredList.get(position).user_name);
                    }

                    ((MyViewHolder) holder).about.setVisibility(View.GONE);
                    if (filteredList.get(position).user_id.equalsIgnoreCase(GetSet.getUserId())) {
                        Glide.with(context).load(Constants.USER_IMG_PATH + GetSet.getImageUrl()).thumbnail(0.5f)
                                .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                                .into(((MyViewHolder) holder).profileimage);
                        ((MyViewHolder) holder).about.setText(filteredList.get(position).about != null ? filteredList.get(position).about : "");
                    } else {
                        DialogActivity.setProfileImage(filteredList.get(position), ((MyViewHolder) holder).profileimage, context);
//                        DialogActivity.setAboutUs(filteredList.get(position), ((MyViewHolder) holder).about);
                    }
                } else {
                    ((MyViewHolder) holder).parentlay.setVisibility(View.GONE);
                }
            } else {
                ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (filteredList.get(position) != null) {
                return ITEM_VIEW_TYPE_ITEM;
            } else {
                return ITEM_VIEW_TYPE_FOOTER;
            }
        }


        public void setLoadMoreListener(OnLoadMoreListener loadMoreListener) {
            this.mOnLoadMoreListener = loadMoreListener;
        }

        // This method is used to remove ProgressBar when data is loaded
        public void setLoaded() {
            loading = false;
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay;
            TextView name, about;
            CircleImageView profileimage;
            View profileview;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                profileimage = view.findViewById(R.id.profileimage);
                name = view.findViewById(R.id.name);
                about = view.findViewById(R.id.about);
                profileview = view.findViewById(R.id.profileview);

                parentlay.setOnClickListener(this);
                profileimage.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
//                        Intent i = new Intent(SubscribersActivity.this, ChatActivity.class);
//                        i.putExtra("user_id", filteredList.get(getAdapterPosition() - 1).user_id);
//                        startActivity(i);
                        break;
                    case R.id.profileimage:
//                        openUserDialog(profileview, filteredList.get(getAdapterPosition() - 1));
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

    private void openUserDialog(View view, ContactsData.Result data) {
        Intent i = new Intent(SubscribersActivity.this, DialogActivity.class);
        i.putExtra(Constants.TAG_USER_ID, data.user_id);
        i.putExtra(Constants.TAG_USER_NAME, data.user_name);
        i.putExtra(Constants.TAG_USER_IMAGE, data.user_image);
        i.putExtra(Constants.TAG_BLOCKED_ME, data.blockedme);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(SubscribersActivity.this, view, getURLForResource(R.drawable.temp));
        startActivity(i, options.toBundle());
    }

    public static String getURLForResource(int resourceId) {
        return Uri.parse("android.resource://com.hitasoft.loopytime.hiddy/" + resourceId).toString();
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
                final PopupWindow popup = new PopupWindow(SubscribersActivity.this);
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
                            page = 0;
                            getSubscribers(page, limit);

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
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketConnection.getInstance(this).setSelectContactListener(null);
    }
}
