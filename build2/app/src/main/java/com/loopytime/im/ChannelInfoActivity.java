package com.loopytime.im;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.Utils;
import com.loopytime.model.ChannelResult;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.utils.Constants.TAG_USER_ID;

public class ChannelInfoActivity extends BaseActivity implements View.OnClickListener {

    static ApiInterface apiInterface;
    Toolbar toolbar;
    ImageView channelImageView, btnBack, btnMenu, privateImage, closeBtn, imageView, arrow;
    TextView txtChannelName, txtCreatedAt, txtAbout, txtParticipants, txtMembersCount, mediaCount;
    SwitchCompat btnMute;
    CollapsingToolbarLayout collapse_toolbar;
    AppBarLayout appBarLayout;
    LinearLayoutManager linearLayoutManager;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    DatabaseHandler dbhelper;
    CoordinatorLayout mainLay;
    String channelId, adminId;
    ChannelResult.Result channelData;
    LinearLayout muteLayout, mediaLayout;
    RelativeLayout subscribersLayout, imageViewLay, mediaLay;
    RecyclerView mediaList;
    private String TAG = this.getClass().getSimpleName();
    public static boolean isChannelUnsubscribe = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_info);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);

        if (getIntent().getStringExtra(Constants.TAG_CHANNEL_ID) != null) {
            channelId = getIntent().getStringExtra(Constants.TAG_CHANNEL_ID);
            channelData = dbhelper.getChannelInfo(channelId);
        }
        toolbar = findViewById(R.id.toolbar);
        collapse_toolbar = findViewById(R.id.collapse_toolbar);
        appBarLayout = findViewById(R.id.appbar);
        channelImageView = findViewById(R.id.channelImage);
        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);
        txtChannelName = findViewById(R.id.txtChannelName);
        txtCreatedAt = findViewById(R.id.txtCreatedAt);
        txtMembersCount = findViewById(R.id.txtMembersCount);
        txtParticipants = findViewById(R.id.txtParticipants);
        mainLay = findViewById(R.id.mainLay);
        btnMute = findViewById(R.id.btnMute);
        txtAbout = findViewById(R.id.txtAbout);
        muteLayout = findViewById(R.id.muteLayout);
        privateImage = findViewById(R.id.privateImage);
        subscribersLayout = findViewById(R.id.subscribersLayout);
        imageViewLay = findViewById(R.id.imageViewLay);
        closeBtn = findViewById(R.id.closeBtn);
        imageView = findViewById(R.id.imageView);
        mediaLayout = findViewById(R.id.mediaLayout);
        mediaList = findViewById(R.id.mediaList);
        mediaLay = findViewById(R.id.mediaLay);
        mediaCount = findViewById(R.id.mediaCount);
        arrow = findViewById(R.id.arrow);

        btnBack.setOnClickListener(this);
        btnMute.setOnClickListener(this);
        btnMenu.setOnClickListener(this);
        txtMembersCount.setOnClickListener(this);
        channelImageView.setOnClickListener(this);
        closeBtn.setOnClickListener(this);
        mediaLay.setOnClickListener(this);

        if (ApplicationClass.isRTL()) {
            btnBack.setRotation(180);
            arrow.setRotation(180);
        } else {
            btnBack.setRotation(0);
            arrow.setRotation(0);
        }

        if (Utils.isUserAdminInChannel(channelData)) {
            muteLayout.setVisibility(View.GONE);
        } else {
            muteLayout.setVisibility(View.VISIBLE);
        }

        setMediaAdapter();

        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        collapse_toolbar.getLayoutParams().height = (getResources().getDisplayMetrics().heightPixels * 60 / 100);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    // Collapsed
                    btnBack.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
                    btnMenu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
                } else if (verticalOffset == 0) {
                    // Expanded
                    btnBack.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white));
                    btnMenu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white));
                } else {
                    // Somewhere in between
                }
            }
        });

    }

    private void setMediaAdapter() {
        List<MediaModelData> mediaData = new ArrayList<>();
        try {
            mediaData = dbhelper.getMedia(channelId, Constants.TAG_CHANNEL, getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mediaData.isEmpty()) {
            mediaLayout.setVisibility(View.GONE);
        } else {
            // mediaCount.setText("" + mediaData.size());
            LinearLayoutManager layoutManager = new LinearLayoutManager(ChannelInfoActivity.this, RecyclerView.HORIZONTAL, false);
            mediaList.setLayoutManager(layoutManager);
            MediaShareAdapter adapter = new MediaShareAdapter(mediaData, ChannelInfoActivity.this, mediaData.size());
            mediaList.setAdapter(adapter);
        }

    }

    private void initChannel() {
        if (NetworkReceiver.isConnected()) {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(channelId);
            Call<ChannelResult> call = apiInterface.getChannelInfo(GetSet.getToken(), jsonArray);
            call.enqueue(new Callback<ChannelResult>() {
                @Override
                public void onResponse(Call<ChannelResult> call, Response<ChannelResult> response) {
                    if (response.isSuccessful()) {
                        Log.i(TAG, "onResponse: " + new Gson().toJson(response.body()));
                        if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                            for (ChannelResult.Result result : response.body().result) {
                                if (channelId.equalsIgnoreCase(result.channelId)) {
                                    if (dbhelper.isChannelExist(channelId)) {
                                        dbhelper.updateChannelInfo(result.channelId, result.channelName, result.channelDes, result.channelImage,
                                                result.channelType != null ? result.channelType : Constants.TAG_PUBLIC, result.channelAdminId != null ? result.channelAdminId : "", result.channelAdminName, result.totalSubscribers, result.blockStatus);
                                        setUI(dbhelper.getChannelInfo(result.channelId));
                                    } else {
                                        setUI(result);
                                    }
                                }

                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<ChannelResult> call, Throwable t) {
                    Log.e(TAG, "getChannelInfo: " + t.getMessage());
                    call.cancel();
                }
            });
        } else {
            makeToast(getString(R.string.no_internet_connection));
        }
    }

    private void setUI(ChannelResult.Result channelData) {

        String createdBy = "";
        if (channelData.channelCategory.equalsIgnoreCase(Constants.TAG_USER_CHANNEL)) {
            subscribersLayout.setVisibility(View.VISIBLE);
            if (channelData.channelAdminId != null && channelData.channelAdminId.equalsIgnoreCase(GetSet.getUserId())) {
                createdBy = getString(R.string.created_by) + " " + getString(R.string.you) + " " +
                        "at " + Utils.getCreatedFormatDate(getApplicationContext(), Long.parseLong(channelData.createdTime));
            } else {
                createdBy = getString(R.string.created_by) + " " + channelData.channelAdminName + " " +
                        "at " + Utils.getCreatedFormatDate(getApplicationContext(), Long.parseLong(channelData.createdTime));
            }
            btnMenu.setVisibility(View.VISIBLE);
//            if(channelData.blockStatus == null || !channelData.blockStatus.equals("1")) {
//                btnMenu.setVisibility(View.VISIBLE);
//            } else {
//                btnMenu.setVisibility(View.GONE);
//            }

        } else {
            subscribersLayout.setVisibility(View.GONE);
            btnMenu.setVisibility(View.VISIBLE);
            createdBy = getString(R.string.created_by) + " " + getString(R.string.admin) + " " +
                    "at " + Utils.getCreatedFormatDate(getApplicationContext(), Long.parseLong(channelData.createdTime));
        }
        txtCreatedAt.setText(createdBy);
        if (channelData.channelType.equalsIgnoreCase(Constants.TAG_PRIVATE)) {
            privateImage.setVisibility(View.VISIBLE);
        } else {
            privateImage.setVisibility(View.GONE);
        }
        txtChannelName.setText(channelData.channelName);

        txtAbout.setText(Utils.fromHtml(channelData.channelDes));

        Glide.with(getApplicationContext()).load(Constants.CHANNEL_IMG_PATH + channelData.channelImage)
                .apply(new RequestOptions().error(R.drawable.ic_channel_banner).placeholder(R.drawable.ic_channel_banner))
                .into(channelImageView);

        if (channelData.totalSubscribers != null && !channelData.totalSubscribers.equalsIgnoreCase("0")) {
            txtMembersCount.setVisibility(View.VISIBLE);
            txtParticipants.setText(channelData.totalSubscribers + " " + getString(R.string.subscribers));
        } else {
            txtMembersCount.setVisibility(View.GONE);
            txtParticipants.setText("0" + " " + getString(R.string.subscribers));
        }

        if (channelData.muteNotification != null && channelData.muteNotification.equals("true")) {
            btnMute.setChecked(true);
        } else {
            btnMute.setChecked(false);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnBack:
                finish();
                break;
            case R.id.btnMute:
                if (btnMute.isChecked()) {
                    dbhelper.updateChannelData(channelId, Constants.TAG_MUTE_NOTIFICATION, "true");
                } else {
                    dbhelper.updateChannelData(channelId, Constants.TAG_MUTE_NOTIFICATION, "");
                }
                break;
            case R.id.txtMembersCount:
                if (isNetworkConnected().equals(NOT_CONNECT)) {
                    networkSnack();
                } else {
                    Intent subscribers = new Intent(getApplicationContext(), SubscribersActivity.class);
                    subscribers.putExtra(Constants.TAG_CHANNEL_ID, channelId);
                    startActivity(subscribers);
                }
                break;
            case R.id.btnMenu:
                Display display = this.getWindowManager().getDefaultDisplay();
                final ArrayList<String> values = new ArrayList<>();
                ChannelResult.Result results = dbhelper.getChannelInfo(channelId);
                if (Utils.isUserAdminInChannel(results)) {
                    values.add(getString(R.string.edit_channel));
                    values.add(getString(R.string.invite_subscribers));
//                    values.add(getString(R.string.leave_channel));
                } else {
                    if (results.blockStatus == null || !results.blockStatus.equals("1")) {
                        if (results.muteNotification.equals("true")) {
                            values.add(getString(R.string.unmute_notification));
                        } else {
                            values.add(getString(R.string.mute_notification));
                        }

                        if (results.channelCategory.equalsIgnoreCase(Constants.TAG_USER_CHANNEL)) {
                            if (results.channelType.equalsIgnoreCase(Constants.TAG_PUBLIC)) {
                                values.add(getString(R.string.invite_subscribers));
                            }

                            values.add(getString(R.string.unsubscribe_channel));
//                            values.add(getString(R.string.report));

                            if (results.report.equals("0")) {
                                values.add(getString(R.string.report));
                            } else {
                                values.add(getString(R.string.undo_report));
                            }
                        }
                    } else {
                        values.add(getString(R.string.unsubscribe_channel));
                    }
                }
                //  values.add(getString(R.string.exit_channel));
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        R.layout.option_item, android.R.id.text1, values);
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = layoutInflater.inflate(R.layout.option_layout, null);
                final PopupWindow popup = new PopupWindow(ChannelInfoActivity.this);
                popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popup.setContentView(layout);
                popup.setWidth(display.getWidth() * 60 / 100);
                popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                popup.setFocusable(true);

                ImageView pinImage = layout.findViewById(R.id.pinImage);
                if (ApplicationClass.isRTL()) {
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
                        if (values.get(position).equalsIgnoreCase(getString(R.string.edit_channel))) {
                            if (Utils.isNetworkConnected(ChannelInfoActivity.this).equals(NOT_CONNECT)) {
                                Utils.networkSnack(mainLay, ChannelInfoActivity.this);
                            } else {
                                Intent intent = new Intent(getApplicationContext(), CreateChannelActivity.class);
                                intent.putExtra(Constants.TAG_CHANNEL_ID, channelId);
                                startActivity(intent);
                            }
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.invite_subscribers))) {
                            if (Utils.isNetworkConnected(ChannelInfoActivity.this).equals(NOT_CONNECT)) {
                                Utils.networkSnack(mainLay, ChannelInfoActivity.this);
                            } else {
                                Intent subscribers = new Intent(getApplicationContext(), NewChannelActivity.class);
                                subscribers.putExtra(Constants.IS_EDIT, true);
                                subscribers.putExtra(Constants.TAG_CHANNEL_ID, channelId);
                                startActivity(subscribers);
                            }
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.unsubscribe_channel))) {
                            if (Utils.isNetworkConnected(ChannelInfoActivity.this).equals(NOT_CONNECT)) {
                                Utils.networkSnack(mainLay, ChannelInfoActivity.this);
                            } else {
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put(Constants.TAG_USER_ID, GetSet.getUserId());
                                    jsonObject.put(Constants.TAG_CHANNEL_ID, results.channelId);
                                    socketConnection.unsubscribeChannel(jsonObject, results.channelId, results.totalSubscribers);
                                    isChannelUnsubscribe = true;
                                    finish();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.mute_notification))) {
                            btnMute.setChecked(true);
                            dbhelper.updateChannelData(channelId, Constants.TAG_MUTE_NOTIFICATION, "true");
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.unmute_notification))) {
                            btnMute.setChecked(false);
                            dbhelper.updateChannelData(channelId, Constants.TAG_MUTE_NOTIFICATION, "");
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.report))) {
                            openReportDialog();
                        } else if (values.get(position).equalsIgnoreCase(getString(R.string.undo_report))) {
                            reportChannel(channelId, "", true);
                        }
                    }
                });
                break;
            case R.id.channelImage:
                ApplicationClass.openImage(ChannelInfoActivity.this,
                        Constants.CHANNEL_IMG_PATH + channelData.channelImage, Constants.TAG_CHANNEL, channelImageView);
                break;
            case R.id.mediaLay:
                Intent mediaIntent = new Intent(ChannelInfoActivity.this, MediaDetailActivity.class);
                mediaIntent.putExtra(TAG_USER_ID, channelId);
                mediaIntent.putExtra(Constants.TAG_FROM, Constants.TAG_CHANNEL);
                startActivity(mediaIntent);
                break;
            case R.id.closeBtn:
                onBackPressed();
                break;
        }
    }

    private void openReportDialog() {
        Dialog reportDialog = new Dialog(ChannelInfoActivity.this);
        reportDialog.setCancelable(true);
        if (reportDialog.getWindow() != null) {
            reportDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            reportDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        reportDialog.setContentView(R.layout.dialog_report);

        RecyclerView reportRecyclerView = reportDialog.findViewById(R.id.reportRecyclerView);

        List<String> reports = new ArrayList<>();

        reports.add(getString(R.string.spam));
        reports.add(getString(R.string.abuse));
        reports.add(getString(R.string.in_appropriate));
        reports.add(getString(R.string.adult_content));

        LinearLayoutManager layoutManager = new LinearLayoutManager(ChannelInfoActivity.this, RecyclerView.VERTICAL, false);
        reportRecyclerView.setLayoutManager(layoutManager);

        ReportAdapter adapter = new ReportAdapter(reports, reportDialog);
        reportRecyclerView.setAdapter(adapter);

        reportDialog.show();
    }

    private void reportChannel(String channelId, String description, boolean isAlreadyReported) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(Constants.TAG_USER_ID, GetSet.getUserId());
        hashMap.put(Constants.TAG_CHANNEL_ID, channelId);
        hashMap.put(Constants.TAG_REPORT, description);

        Call<HashMap<String, String>> call = apiInterface.reportChannel(GetSet.getToken(), hashMap);
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                Log.i(TAG, "ReportChannel Response: " + response.body());
                if (isAlreadyReported) {
                    dbhelper.updateChannelData(channelId, Constants.TAG_REPORT, "0");
                    makeToast(getString(R.string.undo_report));
                } else {
                    dbhelper.updateChannelData(channelId, Constants.TAG_REPORT, "1");
                    makeToast(getString(R.string.reported_successfully));
                }
//                if (response.body().get(Constants.TAG_STATUS).equalsIgnoreCase(Constants.TRUE)) {
//                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                call.cancel();
                Log.e(TAG, "Report Channel onFailure: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        if (dbhelper.isChannelExist(channelId)) {
            channelData = dbhelper.getChannelInfo(channelId);
            btnMute.setVisibility(View.VISIBLE);
            setUI(channelData);
            initChannel();
        } else {
            initChannel();
        }
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.MyViewHolder> {

        Dialog dialog;
        List<String> reportList;

        public ReportAdapter(List<String> reportList, Dialog dialog) {
            this.reportList = reportList;
            this.dialog = dialog;
        }

        @NonNull
        @Override
        public ReportAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.report_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ReportAdapter.MyViewHolder holder, int position) {
            holder.texItem.setText(reportList.get(position));
        }

        @Override
        public int getItemCount() {
            return reportList.size();
        }

        private class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView texItem;

            MyViewHolder(@NonNull View itemView) {
                super(itemView);
                texItem = itemView.findViewById(R.id.itemText);

                texItem.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.itemText) {
                    reportChannel(channelId, reportList.get(getAdapterPosition()), false);
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }
            }
        }
    }

}
