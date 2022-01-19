package com.loopytime.im;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.loopytime.apprtc.util.AppRTCUtils;
import com.loopytime.external.TouchImageView;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.Utils;
import com.loopytime.model.ContactsData;
import com.loopytime.model.MediaModelData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WAKE_LOCK;
import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;
import static com.loopytime.utils.Constants.TAG_USER_ID;

public class ProfileActivity extends BaseActivity implements View.OnClickListener, SocketConnection.UserProfileListener {

    private static final String TAG = ProfileActivity.class.getSimpleName();
    TextView userName, about, txtMobileNumber, txtNumberType, mediaCount;
    ImageView userImage, backbtn, editbtn, btnMenu, closeBtn, arrow;
    CollapsingToolbarLayout collapse_toolbar;
    AppBarLayout appBarLayout;
    CoordinatorLayout mainLay;
    DatabaseHandler dbhelper;
    SocketConnection socketConnection;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    ContactsData.Result results;
    LinearLayout aboutLay, muteLay, parentMediaLay;
    RelativeLayout mobileLay, imageViewLay, mediaLay, encryptionLay;
    RecyclerView mediaList;
    List<MediaModelData> mediaData = new ArrayList<>();
    private String userId = "";
    private SwitchCompat btnMute;
    private ImageView btnMessage, btnCall, btnVideo;
    TouchImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        pref = ProfileActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();

        dbhelper = DatabaseHandler.getInstance(this);
        userName = findViewById(R.id.userName);
        about = findViewById(R.id.about);
        userImage = findViewById(R.id.userImage);
        collapse_toolbar = findViewById(R.id.collapse_toolbar);
        appBarLayout = findViewById(R.id.appbar);
        backbtn = findViewById(R.id.backbtn);
        editbtn = findViewById(R.id.editbtn);
        btnMenu = findViewById(R.id.btnMenu);
        txtMobileNumber = findViewById(R.id.txtMobileNumber);
        txtNumberType = findViewById(R.id.txtNumberType);
        btnMute = findViewById(R.id.btnMute);
        btnMessage = findViewById(R.id.btnMessage);
        btnCall = findViewById(R.id.btnCall);
        btnVideo = findViewById(R.id.btnVideo);
        mainLay = findViewById(R.id.mainLay);
        aboutLay = findViewById(R.id.aboutLay);
        muteLay = findViewById(R.id.muteLay);
        mobileLay = findViewById(R.id.mobileLay);
        imageViewLay = findViewById(R.id.imageViewLay);
        mediaLay = findViewById(R.id.mediaLay);
        closeBtn = findViewById(R.id.closeBtn);
        imageView = findViewById(R.id.imageView);
        mediaCount = findViewById(R.id.mediaCount);
        mediaList = findViewById(R.id.mediaList);
        parentMediaLay = findViewById(R.id.parentMediaLay);
        encryptionLay = findViewById(R.id.encryptionLay);
        arrow = findViewById(R.id.arrow);

        if (ApplicationClass.isRTL()) {
            backbtn.setRotation(180);
            arrow.setRotation(180);
        } else {
            backbtn.setRotation(0);
            arrow.setRotation(0);
        }

        socketConnection = SocketConnection.getInstance(this);
        SocketConnection.getInstance(this).setUserProfileListener(this);
        if (getIntent().getStringExtra(Constants.TAG_USER_ID) != null) {
            userId = getIntent().getStringExtra(Constants.TAG_USER_ID);
        }

        if (userId.equalsIgnoreCase(GetSet.getUserId())) {
            editbtn.setVisibility(View.VISIBLE);
            btnMenu.setVisibility(View.GONE);
            muteLay.setVisibility(View.GONE);
            //mobileLay.setVisibility(View.GONE);
            findViewById(R.id.callLayout).setVisibility(View.GONE);
            parentMediaLay.setVisibility(View.GONE);
            aboutLay.setVisibility(View.VISIBLE);

            userName.setText(GetSet.getUserName());
            Glide.with(ProfileActivity.this).load(Constants.USER_IMG_PATH + GetSet.getImageUrl()).thumbnail(0.5f)
                    .apply(new RequestOptions().placeholder(R.drawable.profile_banner).error(R.drawable.profile_banner))
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(userImage);

            txtMobileNumber.setText("+" + GetSet.getcountrycode()+ GetSet.getphonenumber());
            txtNumberType.setText(R.string.mobile);
            about.setText(GetSet.getAbout());
        } else {
            setMediaAdapter();
            setOtherUserProfile();
            getUserProfile();
        }

        collapse_toolbar.getLayoutParams().height = (getResources().getDisplayMetrics().heightPixels * 60 / 100);

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    // Collapsed
                    backbtn.setColorFilter(ContextCompat.getColor(ProfileActivity.this, R.color.primarytext));
                    editbtn.setColorFilter(ContextCompat.getColor(ProfileActivity.this, R.color.primarytext));
                    btnMenu.setColorFilter(ContextCompat.getColor(ProfileActivity.this, R.color.primarytext));
                } else if (verticalOffset == 0) {
                    // Expanded
                    backbtn.setColorFilter(ContextCompat.getColor(ProfileActivity.this, R.color.white));
                    editbtn.setColorFilter(ContextCompat.getColor(ProfileActivity.this, R.color.white));
                    btnMenu.setColorFilter(ContextCompat.getColor(ProfileActivity.this, R.color.white));
                } else {
                    // Somewhere in between
                }
            }
        });

        backbtn.setOnClickListener(this);
        editbtn.setOnClickListener(this);
        btnMenu.setOnClickListener(this);
        btnMute.setOnClickListener(this);
        btnCall.setOnClickListener(this);
        btnVideo.setOnClickListener(this);
        btnMessage.setOnClickListener(this);
        userImage.setOnClickListener(this);
        closeBtn.setOnClickListener(this);
        mediaLay.setOnClickListener(this);

    }

    private void setMediaAdapter() {
        try {
            mediaData = dbhelper.getMedia(userId, Constants.TAG_SINGLE, getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mediaData.isEmpty()) {
            parentMediaLay.setVisibility(View.GONE);
        } else {
            /*mediaCount.setText("" + mediaData.size());*/
            LinearLayoutManager layoutManager = new LinearLayoutManager(ProfileActivity.this, RecyclerView.HORIZONTAL, false);
            mediaList.setLayoutManager(layoutManager);
            MediaShareAdapter adapter = new MediaShareAdapter(mediaData, ProfileActivity.this, mediaData.size());
            mediaList.setAdapter(adapter);
        }

    }

    private void setOtherUserProfile() {
        btnMenu.setVisibility(View.VISIBLE);
        editbtn.setVisibility(View.GONE);
        muteLay.setVisibility(View.VISIBLE);
        mobileLay.setVisibility(View.VISIBLE);
        aboutLay.setVisibility(View.VISIBLE);
        encryptionLay.setVisibility(View.VISIBLE);

        results = dbhelper.getContactDetail(userId);
        userName.setText(ApplicationClass.getContactName(this, results.phone_no, results.country_code,results.user_name));

        if (results.mute_notification.equals("true")) {
            btnMute.setChecked(true);
        } else {
            btnMute.setChecked(false);
        }
        if (!results.blockedme.equals("block")) {
            ContactsData.Result result = dbhelper.getContactDetail(userId);
            DialogActivity.setProfileBanner(result, userImage, this);
            if (Utils.isAboutEnabled(result)) {
                aboutLay.setVisibility(View.VISIBLE);
                about.setText(results.about);
            } else {
                aboutLay.setVisibility(View.GONE);
                about.setText("");
            }
        } else {
            Glide.with(ProfileActivity.this).load(R.drawable.profile_banner).thumbnail(0.5f)
                    .apply(new RequestOptions().placeholder(R.drawable.profile_banner).error(R.drawable.profile_banner))
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(userImage);
            Glide.with(ProfileActivity.this).load(R.drawable.profile_banner).thumbnail(0.5f)
                    .apply(new RequestOptions().placeholder(R.drawable.profile_banner).error(R.drawable.profile_banner))
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(imageView);
            about.setText("");
        }
        txtMobileNumber.setText(!ApplicationClass.hasContact(this,results.phone_no)?results.user_name:"+" + results.country_code + " - " + results.phone_no);
        txtNumberType.setText(R.string.mobile);
    }

    void getUserProfile() {

        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<Map<String, String>> call3 = apiInterface.getuserprofile(GetSet.getToken(), GetSet.getphonenumber(), userId);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                try {
                    Log.v(TAG, "getUserProfileResponse: " + new JSONObject(response.body()));
                    Map<String, String> userdata = response.body();
                    if (userdata.get(Constants.TAG_STATUS).equals("true")) {
                        String name = results.saved_name;
                        HashMap<String, String> map = ApplicationClass.getContactrNot(getApplicationContext(), userdata.get(Constants.TAG_PHONE_NUMBER));
                        if (map.get("isAlready").equals("true")) {
                            name = map.get(Constants.TAG_USER_NAME);
                        }
                        if (dbhelper.isUserExist(userdata.get(Constants.TAG_ID))) {
                            ContactsData.Result results = dbhelper.getContactDetail(userId);
                            dbhelper.addContactDetails(name, userdata.get(Constants.TAG_ID), userdata.get(Constants.TAG_USER_NAME),
                                    userdata.get(Constants.TAG_PHONE_NUMBER), userdata.get(Constants.TAG_COUNTRY_CODE),
                                    userdata.get(Constants.TAG_USER_IMAGE), userdata.get(Constants.TAG_PRIVACY_ABOUT),
                                    userdata.get(Constants.TAG_PRIVACY_LAST_SEEN), userdata.get(Constants.TAG_PRIVACY_PROFILE),
                                    userdata.get(Constants.TAG_ABOUT), results.contactstatus);
                        } else {
                            dbhelper.addContactDetails(name,
                                    userdata.get(Constants.TAG_ID), userdata.get(Constants.TAG_USER_NAME),
                                    userdata.get(Constants.TAG_PHONE_NUMBER), userdata.get(Constants.TAG_COUNTRY_CODE),
                                    userdata.get(Constants.TAG_USER_IMAGE), userdata.get(Constants.TAG_PRIVACY_ABOUT),
                                    userdata.get(Constants.TAG_PRIVACY_LAST_SEEN), userdata.get(Constants.TAG_PRIVACY_PROFILE),
                                    userdata.get(Constants.TAG_ABOUT), "false");
                        }
                        setOtherUserProfile();
                    } else {

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.v("getuserprofile Failed", "TEST" + t.getMessage());
                call.cancel();
            }
        });

    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void networkSnack() {
        Snackbar snackbar = Snackbar
                .make(mainLay, getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backbtn:
                onBackPressed();
                break;
            case R.id.editbtn:
                Intent i = new Intent(ProfileActivity.this, ProfileInfo.class);
                i.putExtra("from", "edit");
                startActivity(i);
                break;
            case R.id.btnMenu:
                Display display = this.getWindowManager().getDefaultDisplay();
                ArrayList<String> values = new ArrayList<>();
                results = dbhelper.getContactDetail(userId);
                if (results.blockedbyme.equals("block")) {
                    values.add(getString(R.string.unblock));
                } else {
                    values.add(getString(R.string.block));
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        R.layout.option_item, android.R.id.text1, values);
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = layoutInflater.inflate(R.layout.option_layout, null);
                final PopupWindow popup = new PopupWindow(ProfileActivity.this);
                popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popup.setContentView(layout);
                popup.setWidth(display.getWidth() * 50 / 100);
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
                        if (position == 0) {
                            if (isNetworkConnected().equals(NOT_CONNECT)) {
                                networkSnack();
                            } else {
                                String type = "";
                                if (results.blockedbyme.equals("block")) {
                                    type = "unblock";
                                } else {
                                    type = "block";
                                }
                                blockChatConfirmDialog(type, "popup");
                            }
                        }
                    }
                });
                break;
            case R.id.btnMute:
                if (btnMute.isChecked()) {
                    dbhelper.updateMuteUser(userId, "true");
                } else {
                    dbhelper.updateMuteUser(userId, "");
                }
                break;
            case R.id.btnMessage:
                finish();
                Intent ch = new Intent(this, ChatActivity.class);
                ch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ch.putExtra("user_id", userId);
                startActivity(ch);
                break;
            case R.id.btnCall:
                if (ContextCompat.checkSelfPermission(ProfileActivity.this, CAMERA) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(ProfileActivity.this, RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{CAMERA, RECORD_AUDIO}, 100);
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
                } else {
                    if (NetworkReceiver.isConnected()) {
                        ApplicationClass.preventMultiClick(btnCall);
                        AppRTCUtils appRTCUtils = new AppRTCUtils(getApplicationContext());
                        Intent video = appRTCUtils.connectToRoom(userId, Constants.TAG_SEND, Constants.TAG_AUDIO);
                        startActivity(video);
                    } else {
                        makeToast(getString(R.string.no_internet_connection));
                    }
                }
                break;
            case R.id.btnVideo:
                if (ContextCompat.checkSelfPermission(ProfileActivity.this, CAMERA) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(ProfileActivity.this, RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{CAMERA, RECORD_AUDIO}, 101);
                } else if (results.blockedbyme.equals("block")) {
                    blockChatConfirmDialog("unblock", "sent");
                } else {
                    if (NetworkReceiver.isConnected()) {
                        ApplicationClass.preventMultiClick(btnVideo);
                        btnVideo.setOnClickListener(null);
                        AppRTCUtils appRTCUtils = new AppRTCUtils(getApplicationContext());
                        Intent video = appRTCUtils.connectToRoom(userId, Constants.TAG_SEND, Constants.TAG_VIDEO);
                        startActivity(video);
                    } else {
                        makeToast(getString(R.string.no_internet_connection));
                    }
                }
                break;
            case R.id.userImage:
                String url = "";
                if (!userId.equalsIgnoreCase(GetSet.getUserId())) {
                    if (Utils.isProfileEnabled(dbhelper.getContactDetail(userId))) {
                        url = Constants.USER_IMG_PATH + results.user_image;
                    }
                } else {
                    url = Constants.USER_IMG_PATH + GetSet.getImageUrl();
                }
                ApplicationClass.openImage(ProfileActivity.this, url,
                        Constants.TAG_SINGLE, userImage);
                break;
            case R.id.mediaLay:
                Intent mediaIntent = new Intent(ProfileActivity.this, MediaDetailActivity.class);
                mediaIntent.putExtra(TAG_USER_ID, userId);
                mediaIntent.putExtra(Constants.TAG_FROM, Constants.TAG_SINGLE);
                startActivity(mediaIntent);
                break;
            case R.id.closeBtn:
                onBackPressed();
                break;
        }
    }

    private void blockChatConfirmDialog(final String type, String from) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.default_popup);
        dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 90 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView title = dialog.findViewById(R.id.title);
        TextView yes = dialog.findViewById(R.id.yes);
        TextView no = dialog.findViewById(R.id.no);

        if (from.equals("popup")) {
            yes.setText(getString(R.string.im_sure));
            no.setText(getString(R.string.nope));
            if (type.equals(Constants.TAG_BLOCK)) {
                title.setText(R.string.really_block_chat);
            } else {
                title.setText(R.string.really_unblock_chat);
            }
        } else {
            yes.setText(getString(R.string.unblock));
            no.setText(getString(R.string.cancel));
            title.setText(R.string.unblock_message);
        }

        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                    jsonObject.put(Constants.TAG_RECEIVER_ID, userId);
                    jsonObject.put(Constants.TAG_TYPE, type);
                    Log.v("block", "block=" + jsonObject);
                    socketConnection.block(jsonObject);
                    dbhelper.updateBlockStatus(userId, Constants.TAG_BLOCKED_BYME, type);
                    setOtherUserProfile();
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
    protected void onResume() {
        super.onResume();
        btnCall.setOnClickListener(this);
        btnVideo.setOnClickListener(this);
        if (userId.equalsIgnoreCase(GetSet.getUserId())) {
            Glide.with(ProfileActivity.this).load(Constants.USER_IMG_PATH + GetSet.getImageUrl()).thumbnail(0.5f)
                    .apply(new RequestOptions().placeholder(R.drawable.profile_banner).error(R.drawable.profile_banner))
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(userImage);
            Glide.with(ProfileActivity.this).load(Constants.USER_IMG_PATH + GetSet.getImageUrl()).thumbnail(0.5f)
                    .apply(new RequestOptions().placeholder(R.drawable.profile_banner).error(R.drawable.profile_banner))
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(imageView);
            userName.setText(GetSet.getUserName());
            about.setText(GetSet.getAbout());
        }
    }

    @Override
    public void onPrivacyChanged(final JSONObject jsonObject) {
        try {

            if (jsonObject.getString(TAG_USER_ID).equalsIgnoreCase(userId)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            ContactsData.Result result = dbhelper.getContactDetail(jsonObject.getString(Constants.TAG_USER_ID));
                            if (!result.user_id.equalsIgnoreCase(GetSet.getUserId())) {
                                if (Utils.isAboutEnabled(result)) {
                                    aboutLay.setVisibility(View.VISIBLE);
                                    about.setText(results.about);
                                } else {
                                    aboutLay.setVisibility(View.GONE);
                                    about.setText("");
                                }
                                DialogActivity.setProfileBanner(dbhelper.getContactDetail(jsonObject.getString(Constants.TAG_USER_ID)), userImage, getApplicationContext());
                                Glide.with(ProfileActivity.this).load(Constants.USER_IMG_PATH + GetSet.getImageUrl()).thumbnail(0.5f)
                                        .apply(new RequestOptions().placeholder(R.drawable.profile_banner).error(R.drawable.profile_banner))
                                        .transition(new DrawableTransitionOptions().crossFade())
                                        .into(imageView);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        SocketConnection.getInstance(this).setUserProfileListener(null);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        /*if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            collapse_toolbar.setVisibility(View.VISIBLE);
        } else {
        }*/
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            int permissionCamera = ContextCompat.checkSelfPermission(ProfileActivity.this,
                    CAMERA);
            int permissionAudio = ContextCompat.checkSelfPermission(ProfileActivity.this,
                    RECORD_AUDIO);
            int permissionWakeLock = ContextCompat.checkSelfPermission(ProfileActivity.this,
                    WAKE_LOCK);

            if (permissionCamera == PackageManager.PERMISSION_GRANTED &&
                    permissionAudio == PackageManager.PERMISSION_GRANTED &&
                    permissionWakeLock == PackageManager.PERMISSION_GRANTED) {
              /*  Intent video = new Intent(ChatActivity.this, CallActivity.class);
                video.putExtra("from", "send");
                video.putExtra("type", "audio");
                video.putExtra("data", data);
                startActivity(video);*/
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(CAMERA) &&
                            shouldShowRequestPermissionRationale(WAKE_LOCK) &&
                            shouldShowRequestPermissionRationale(RECORD_AUDIO)) {
                        requestPermission(new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 100);
                    } else {
//                        openPermissionDialog("Camera, Record Audio");
                        makeToast(getString(R.string.call_permission_error));
                    }
                }
            }
        } else if (requestCode == 101) {
            int permissionCamera = ContextCompat.checkSelfPermission(ProfileActivity.this,
                    CAMERA);
            int permissionAudio = ContextCompat.checkSelfPermission(ProfileActivity.this,
                    RECORD_AUDIO);
            int permissionWakeLock = ContextCompat.checkSelfPermission(ProfileActivity.this,
                    WAKE_LOCK);

            if (permissionCamera == PackageManager.PERMISSION_GRANTED &&
                    permissionAudio == PackageManager.PERMISSION_GRANTED &&
                    permissionWakeLock == PackageManager.PERMISSION_GRANTED) {
               /* Intent video = new Intent(ChatActivity.this, CallActivity.class);
                video.putExtra("from", "send");
                video.putExtra("type", "video");
                video.putExtra("data", data);
                startActivity(video);*/
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(CAMERA) &&
                            shouldShowRequestPermissionRationale(WAKE_LOCK) &&
                            shouldShowRequestPermissionRationale(RECORD_AUDIO)) {
                        requestPermission(new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 101);
                    } else {
//                        openPermissionDialog("Camera, Record Audio,Phone State");
                        makeToast(getString(R.string.call_permission_error));
                    }
                }
            }
        }
    }

    private void requestPermission(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(ProfileActivity.this, permissions, requestCode);
    }
}
