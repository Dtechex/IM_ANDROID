package com.loopytime.im;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.apprtc.util.AppRTCUtils;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.model.ContactsData;
import com.loopytime.utils.Constants;
import com.makeramen.roundedimageview.RoundedImageView;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.RECORD_AUDIO;
import static com.loopytime.utils.Constants.TAG_MY_CONTACTS;
import static com.loopytime.utils.Constants.TAG_NOBODY;
import static com.loopytime.utils.Constants.TRUE;

public class DialogActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    private TextView txtUserName;
    private RoundedImageView userImageView;
    private ImageView btnMessage, btnCall, btnVideo, btnInfo;
    private RelativeLayout imageLay, mainLay, messageLay, callLay, videoLay, infoLay;
    private String userName;
    private String userId;
    private String userImage, blockedme;
    ContactsData.Result result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_user);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        txtUserName = findViewById(R.id.txtUserName);
        userImageView = findViewById(R.id.userImage);
        btnMessage = findViewById(R.id.btnMessage);
        btnCall = findViewById(R.id.btnCall);
        btnVideo = findViewById(R.id.btnVideo);
        btnInfo = findViewById(R.id.btnInfo);
        imageLay = findViewById(R.id.imageLay);
        mainLay = findViewById(R.id.mainLay);
        messageLay = findViewById(R.id.messageLayout);
        callLay = findViewById(R.id.callLayout);
        videoLay = findViewById(R.id.videoLayout);
        infoLay = findViewById(R.id.infoLayout);


        ViewCompat.setTransitionName(mainLay, getURLForResource(R.drawable.temp));
        if (getIntent() != null) {
            if (getIntent().getStringExtra(Constants.TAG_USER_ID) != null) {
                userId = getIntent().getStringExtra(Constants.TAG_USER_ID);
                result = dbhelper.getContactDetail(userId);
                if (ContextCompat.checkSelfPermission(DialogActivity.this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    //userName = ApplicationClass.getContactName(this, result.phone_no);
                    userName = result.user_name;
                } else {
                    userName = result.phone_no;
                }

                userImage = result.user_image;
                blockedme = result.blockedme;
                txtUserName.setText(userName);
                if (!blockedme.equals("block")) {
                    if (result.privacy_profile_image.equalsIgnoreCase(TAG_MY_CONTACTS)) {
                        if (result.contactstatus != null && result.contactstatus.equalsIgnoreCase(TRUE)) {
                            Glide.with(DialogActivity.this).load(Constants.USER_IMG_PATH + result.user_image)
                                    .apply(new RequestOptions().placeholder(R.drawable.profile_square).error(R.drawable.profile_square))
                                    .into(userImageView);
                        } else {
                            Glide.with(DialogActivity.this).load(R.drawable.profile_square)
                                    .apply(new RequestOptions().placeholder(R.drawable.profile_square).error(R.drawable.profile_square))
                                    .into(userImageView);
                        }

                    } else if (result.privacy_profile_image.equalsIgnoreCase(TAG_NOBODY)) {
                        Glide.with(DialogActivity.this).load(R.drawable.profile_square)
                                .apply(new RequestOptions().placeholder(R.drawable.profile_square).error(R.drawable.profile_square))
                                .into(userImageView);
                    } else {

                        Glide.with(DialogActivity.this).load(Constants.USER_IMG_PATH + result.user_image)
                                .apply(new RequestOptions().placeholder(R.drawable.profile_square).error(R.drawable.profile_square))
                                .into(userImageView);
                    }

                } else {
                    Glide.with(DialogActivity.this).load(R.drawable.profile_square)
                            .apply(new RequestOptions().placeholder(R.drawable.profile_square).error(R.drawable.profile_square))
                            .into(userImageView);
                }

                btnMessage.setOnClickListener(this);
                btnCall.setOnClickListener(this);
                btnVideo.setOnClickListener(this);
                btnInfo.setOnClickListener(this);
                imageLay.setOnClickListener(this);

            } else if (getIntent().getStringExtra(Constants.TAG_GROUP_ID) != null) {
                userId = getIntent().getStringExtra(Constants.TAG_GROUP_ID);
                userName = getIntent().getStringExtra(Constants.TAG_GROUP_NAME);
                userImage = getIntent().getStringExtra(Constants.TAG_GROUP_IMAGE);

                txtUserName.setText(userName);
                if (userImage == null || userImage.equalsIgnoreCase("")) {
                    Glide.with(DialogActivity.this).load(R.drawable.ic_group_square)
                            .apply(new RequestOptions().placeholder(R.drawable.ic_group_square).error(R.drawable.ic_group_square))
                            .into(userImageView);
                } else {
                    Glide.with(DialogActivity.this).load(Constants.GROUP_IMG_PATH + userImage)
                            .apply(new RequestOptions().placeholder(R.drawable.ic_group_square).error(R.drawable.ic_group_square))
                            .into(userImageView);
                }

                callLay.setVisibility(View.GONE);
                videoLay.setVisibility(View.GONE);

                btnMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                        Intent i = new Intent(DialogActivity.this, GroupChatActivity.class);
                        i.putExtra(Constants.TAG_GROUP_ID, userId);
                        startActivity(i);
                    }
                });

                btnInfo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                        Intent profile = new Intent(DialogActivity.this, GroupInfoActivity.class);
                        profile.putExtra(Constants.TAG_GROUP_ID, userId);
                        startActivity(profile);
                    }
                });

                imageLay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                        Intent profile = new Intent(DialogActivity.this, GroupInfoActivity.class);
                        profile.putExtra(Constants.TAG_GROUP_ID, userId);
                        startActivity(profile);
                    }
                });
            }
        }
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    public static String getURLForResource(int resourceId) {
        return Uri.parse("android.resource://com.hitasoft.loopytime.hiddy/" + resourceId).toString();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imageLay:
                ApplicationClass.preventMultiClick(imageLay);
            case R.id.btnInfo:
                ApplicationClass.preventMultiClick(btnInfo);
                finish();
                Intent profile = new Intent(DialogActivity.this, ProfileActivity.class);
                profile.putExtra(Constants.TAG_USER_ID, userId);
                startActivity(profile);
                break;

            case R.id.btnMessage:
                finish();
                Intent i = new Intent(DialogActivity.this, ChatActivity.class);
                i.putExtra(Constants.TAG_USER_ID, userId);
                startActivity(i);
                break;

            case R.id.btnCall:
                ApplicationClass.preventMultiClick(btnCall);
                if (ContextCompat.checkSelfPermission(DialogActivity.this, CAMERA) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(DialogActivity.this, RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DialogActivity.this, new String[]{CAMERA, RECORD_AUDIO}, 100);
                } else if (result.blockedbyme.equals("block")) {
                    makeToast(getString(R.string.unblock_message));
                } else {
                    if (NetworkReceiver.isConnected()) {
                        AppRTCUtils appRTCUtils = new AppRTCUtils(getApplicationContext());
                        Intent video = appRTCUtils.connectToRoom(userId,Constants.TAG_SEND,Constants.TAG_AUDIO);
                        startActivity(video);
                    } else {
                        makeToast(getString(R.string.no_internet_connection));
                    }
                }
                break;
            case R.id.btnVideo:
                ApplicationClass.preventMultiClick(btnVideo);
                if (ContextCompat.checkSelfPermission(DialogActivity.this, CAMERA) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(DialogActivity.this, RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DialogActivity.this, new String[]{CAMERA, RECORD_AUDIO}, 101);
                } else if (result.blockedbyme.equals("block")) {
                    makeToast(getString(R.string.unblock_message));
                } else {
                    if (NetworkReceiver.isConnected()) {
                        AppRTCUtils appRTCUtils = new AppRTCUtils(getApplicationContext());
                        Intent video = appRTCUtils.connectToRoom(userId,Constants.TAG_SEND,Constants.TAG_VIDEO);
                        startActivity(video);
                    } else {
                        makeToast(getString(R.string.no_internet_connection));
                    }
                }
                break;
        }
    }

    public static void setProfileImage(ContactsData.Result result, ImageView profileImage, Context context) {
        if (result.privacy_profile_image.equalsIgnoreCase(TAG_MY_CONTACTS)) {
            if (result.contactstatus != null && result.contactstatus.equalsIgnoreCase(TRUE)) {
                Glide.with(context).load(Constants.USER_IMG_PATH + result.user_image).thumbnail(0.5f)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                        .into(profileImage);
            } else {
                Glide.with(context).load(R.drawable.temp).thumbnail(0.5f)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                        .into(profileImage);
            }

        } else if (result.privacy_profile_image.equalsIgnoreCase(TAG_NOBODY)) {
            Glide.with(context).load(R.drawable.temp).thumbnail(0.5f)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                    .into(profileImage);
        } else {
            Glide.with(context).load(Constants.USER_IMG_PATH + result.user_image).thumbnail(0.5f)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                    .into(profileImage);
        }
    }

    public static void setProfileBanner(ContactsData.Result result, ImageView profileImage, Context context) {
        if (result.privacy_profile_image.equalsIgnoreCase(TAG_MY_CONTACTS)) {
            if (result.contactstatus != null && result.contactstatus.equalsIgnoreCase(TRUE)) {
                Glide.with(context).load(Constants.USER_IMG_PATH + result.user_image)
                        .apply(new RequestOptions().placeholder(R.drawable.profile_banner).error(R.drawable.profile_banner))
                        .transition(new DrawableTransitionOptions().crossFade())
                        .into(profileImage);
            } else {
                Glide.with(context).load(R.drawable.profile_banner)
                        .apply(new RequestOptions().placeholder(R.drawable.profile_banner).error(R.drawable.profile_banner))
                        .transition(new DrawableTransitionOptions().crossFade())
                        .into(profileImage);
            }

        } else if (result.privacy_profile_image.equalsIgnoreCase(TAG_NOBODY)) {
            Glide.with(context).load(R.drawable.profile_banner)
                    .apply(new RequestOptions().placeholder(R.drawable.profile_banner).error(R.drawable.profile_banner))
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(profileImage);
        } else {

            Glide.with(context).load(Constants.USER_IMG_PATH + result.user_image)
                    .apply(new RequestOptions().placeholder(R.drawable.profile_banner).error(R.drawable.profile_banner))
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(profileImage);
        }
    }

    public static void setAboutUs(ContactsData.Result result, TextView txtAbout) {
        if (result.privacy_about.equalsIgnoreCase(TAG_MY_CONTACTS)) {
            if (result.contactstatus != null && result.contactstatus.equalsIgnoreCase(TRUE)) {
                txtAbout.setText("" + result.about);
            } else {
                txtAbout.setText("");
            }
        } else if (result.privacy_about.equalsIgnoreCase(TAG_NOBODY)) {
            txtAbout.setText("");
        } else {
            txtAbout.setText(result.about);
        }
    }

}