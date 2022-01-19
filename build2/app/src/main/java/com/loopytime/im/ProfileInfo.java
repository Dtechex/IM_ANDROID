package com.loopytime.im;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.loopytime.external.ImagePicker;
import com.loopytime.external.RandomString;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.ForegroundService;
import com.loopytime.helper.ImageCompression;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.SharedPrefManager;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.StorageManager;
import com.loopytime.model.ChannelResult;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupData;
import com.loopytime.model.GroupResult;
import com.loopytime.model.SaveMyContacts;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.loopytime.utils.Constants.TAG_CONTACT_STATUS;
import static com.loopytime.utils.Constants.TAG_ID;

public class ProfileInfo extends BaseActivity implements View.OnClickListener {
    private static final String TAG = ProfileInfo.class.getSimpleName();
    static ApiInterface apiInterface;
    ProgressDialog progressDialog;
    ProgressBar progressbar;
    EditText name, about;
    TextView detail;
    CoordinatorLayout mainLay;
    CircleImageView userImage, noimage;
    ImageView backbtn, fab;
    RelativeLayout btnNext;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String from = "";
    DatabaseHandler dbhelper;
    StorageManager storageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_info);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userImage = findViewById(R.id.userImage);
        noimage = findViewById(R.id.noimage);
        name = findViewById(R.id.name);
        about = findViewById(R.id.about);
        detail = findViewById(R.id.detail);
        btnNext = findViewById(R.id.btnNext);
        backbtn = findViewById(R.id.backbtn);
        fab = findViewById(R.id.fab);
        mainLay = findViewById(R.id.mainLay);
        progressbar = findViewById(R.id.progressbar);
        progressbar.setIndeterminate(true);
        progressbar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

        if (ApplicationClass.isRTL()) {
            backbtn.setRotation(180);
            btnNext.setRotation(180);
        } else {
            backbtn.setRotation(0);
            btnNext.setRotation(0);
        }

        from = getIntent().getExtras().getString("from");
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = ProfileInfo.this.getSharedPreferences(Constants.SHARED_PREFERENCE, MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);
        storageManager = StorageManager.getInstance(this);

        progressDialog = new ProgressDialog(ProfileInfo.this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        if (GetSet.getUserName() != null) {
            name.setText(GetSet.getUserName());
        }

        if (GetSet.getImageUrl() != null) {
            Glide.with(ProfileInfo.this).load(Constants.USER_IMG_PATH + GetSet.getImageUrl())
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            noimage.setVisibility(View.VISIBLE);
                            userImage.setVisibility(View.INVISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            noimage.setVisibility(View.GONE);
                            userImage.setVisibility(View.VISIBLE);
                            return false;
                        }
                    }).into(userImage);
        }

        if (from.equals("edit")) {
            hideLoading();
            detail.setText(R.string.editprofileinfo);
            about.setVisibility(View.VISIBLE);
            if (GetSet.getAbout() != null) {
                about.setText(GetSet.getAbout());
            }
        } else {
            if (NetworkReceiver.isConnected()) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getMyGroups();
                    }
                }, 1000);
            }
            detail.setText(R.string.profileinfodetail);
            about.setVisibility(View.GONE);
        }
        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));

        userImage.setOnClickListener(this);
        noimage.setOnClickListener(this);
        backbtn.setOnClickListener(this);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(ProfileInfo.this, READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(ProfileInfo.this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ProfileInfo.this, new String[]{READ_CONTACTS, WRITE_EXTERNAL_STORAGE}, 100);
                } else {
                    if (NetworkReceiver.isConnected()) {
                        updateProfile();
                    } else {
                        Snackbar.make(mainLay, getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void getMyChannels() {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<ChannelResult> call3 = apiInterface.getMyChannels(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<ChannelResult>() {
            @Override
            public void onResponse(Call<ChannelResult> call, Response<ChannelResult> response) {
                try {
                    Log.i(TAG, "getMyChannels: " + new Gson().toJson(response.body()));
                    if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                        new InsertMyChannelTask(response.body().result).execute();
                    } else {
                        getSubscribedChannels();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    getSubscribedChannels();
                }
            }

            @Override
            public void onFailure(Call<ChannelResult> call, Throwable t) {
                Log.e(TAG, "getMyChannels" + t.getMessage());
                call.cancel();
                getSubscribedChannels();
            }
        });
    }

    public void getSubscribedChannels() {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<ChannelResult> call3 = apiInterface.getMySubscribedChannels(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<ChannelResult>() {
            @Override
            public void onResponse(Call<ChannelResult> call, Response<ChannelResult> response) {
                try {
                    Log.i(TAG, "getMySubscribedChannels: " + new Gson().toJson(response.body()));
                    if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {

                        new InsertSubscribedChannelTask(response.body().result).execute();
                    } else {
                        hideLoading();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    hideLoading();
                }
            }

            @Override
            public void onFailure(Call<ChannelResult> call, Throwable t) {
                Log.e(TAG, "getMySubscribedChannels" + t.getMessage());
                call.cancel();
                hideLoading();
            }
        });
    }

    private void getMyGroups() {
        showLoading();
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<GroupResult> call3 = apiInterface.getMyGroups(GetSet.getToken(), GetSet.getUserId());
        call3.enqueue(new Callback<GroupResult>() {
            @Override
            public void onResponse(Call<GroupResult> call, Response<GroupResult> response) {
                try {
                    Log.i(TAG, "getMyGroups: " + new Gson().toJson(response.body()));
                    if (response.body().status.equalsIgnoreCase(Constants.TRUE)) {
                        new InsertMyGroupTask(response.body().result).execute();
                    } else {
                        getMyChannels();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    getMyChannels();
                }
            }

            @Override
            public void onFailure(Call<GroupResult> call, Throwable t) {
                Log.e(TAG, "getMyGroups" + t.getMessage());
                call.cancel();
                getMyChannels();
            }
        });
    }

    private void getUserInfo(GroupData groupData, GroupData.GroupMembers groupMember) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<Map<String, String>> call3 = apiInterface.getuserprofile(GetSet.getToken(), GetSet.getphonenumber(), groupMember.memberId);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                try {
                    Log.i(TAG, "getUserInfo: " + new Gson().toJson(response.body()));
                    Map<String, String> userdata = response.body();
                    if (userdata.get(Constants.TAG_STATUS).equals("true")) {
                        new InsertGroupMemberTask(groupData, groupMember, userdata).execute();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.v("getUserInfo Failed", "TEST" + t.getMessage());
                call.cancel();
            }
        });
    }

    private void updateProfile() {
        if (name.getText().toString().trim().length() == 0) {
            Snackbar.make(mainLay, getString(R.string.profileinfodetail), Snackbar.LENGTH_LONG).show();
        } else {
            if (from.equals("welcome")) {
                Signin(name.getText().toString());
            } else {
                if (about.getText().toString().trim().length() == 0) {
                    Snackbar.make(mainLay, getString(R.string.about_blank), Snackbar.LENGTH_LONG).show();
                } else {
                    updateMyProfile();
                }
            }
        }
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    void Signin(String name) {
        showLoading();
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_PHONE_NUMBER, GetSet.getphonenumber());
        map.put(Constants.TAG_COUNTRY_CODE, GetSet.getcountrycode());
        map.put(Constants.TAG_COUNTRY, GetSet.getCountryname());
        map.put(Constants.TAG_USER_NAME, name);

        Log.v(TAG, "SignInParams: " + map);
        Call<HashMap<String, String>> call3 = apiInterface.signin(map);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                try {
                    HashMap<String, String> userdata = response.body();
                    Log.v(TAG, "SignInResponse: " + new JSONObject(userdata));

                    if (userdata.get("status").equals("true")) {
                        editor.putBoolean("isLogged", true);
                        editor.putString("userId", userdata.get("_id"));
                        editor.putString("userName", userdata.get("user_name"));
                        editor.putString("userImage", userdata.get("user_image"));
                        editor.putString("phoneNumber", userdata.get("phone_no"));
                        editor.putString("countryCode", userdata.get("country_code"));
                        editor.putString("countryName", userdata.get("country_name"));
                        editor.putString("token", userdata.get("token"));
                        editor.putString("about", userdata.get("about"));
                        editor.putString("privacyprofileimage", userdata.get("privacy_profile_image"));
                        editor.putString("privacylastseen", userdata.get("privacy_last_seen"));
                        editor.putString("privacyabout", userdata.get("privacy_about"));
                        editor.commit();
                        editor.apply();

                        GetSet.setLogged(true);
                        GetSet.setUserId(pref.getString("userId", null));
                        GetSet.setUserName(pref.getString("userName", null));
                        GetSet.setphonenumber(pref.getString("phoneNumber", null));
                        GetSet.setcountrycode(pref.getString("countryCode", null));
                        GetSet.setCountryname(pref.getString("countryName", null));
                        GetSet.setImageUrl(pref.getString("userImage", null));
                        GetSet.setToken(pref.getString("token", null));
                        GetSet.setAbout(pref.getString("about", null));
                        GetSet.setPrivacyprofileimage(pref.getString("privacyprofileimage", Constants.TAG_EVERYONE));
                        GetSet.setPrivacylastseen(pref.getString("privacylastseen", Constants.TAG_EVERYONE));
                        GetSet.setPrivacyabout(pref.getString("privacyabout", Constants.TAG_EVERYONE));

                        dbhelper.addContactDetails("", GetSet.getUserId(), GetSet.getUserName(),
                                GetSet.getphonenumber(), GetSet.getcountrycode(), GetSet.getImageUrl(),
                                GetSet.getPrivacyabout(), GetSet.getPrivacylastseen(), GetSet.getPrivacyprofileimage(),
                                GetSet.getAbout(), "true");

                        addDeviceId(ProfileInfo.this);
                        new GetContactTask().execute();

                    } else if (userdata.get("status").equals("false")) {
                        hideLoading();
                        Toast.makeText(getApplicationContext(), userdata.get("message"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    hideLoading();
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                t.printStackTrace();
                call.cancel();
                hideLoading();
            }
        });

    }

    private void addDeviceId(final Context context) {
        final String[] token = {SharedPrefManager.getInstance(context).getDeviceToken()};
        final String deviceId = android.provider.Settings.Secure.getString(context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        if (token[0] == null) {
            FirebaseInstanceId.getInstance().getInstanceId().
                    addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                        @Override
                        public void onSuccess(InstanceIdResult instanceIdResult) {
                            token[0] = instanceIdResult.getToken();
                            SharedPrefManager.getInstance(getApplicationContext()).saveDeviceToken(token[0]);
                        }
                    });
        }

        Map<String, String> map = new HashMap<>();
        map.put("user_id", GetSet.getUserId());
        map.put("device_token", token[0]);
        map.put("device_type", "1");
        map.put("device_id", deviceId);

        Log.v(TAG, "addDeviceIdParams: " + map);
        Call<Map<String, String>> call3 = apiInterface.pushsignin(GetSet.getToken(), map);
        call3.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                Map<String, String> data = response.body();
                Log.v(TAG, "addDeviceId: " + data);

            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                call.cancel();
            }
        });
    }

    void updateMyProfile() {
        showLoading();
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_USER_ID, GetSet.getUserId());
        map.put(Constants.TAG_USER_NAME, name.getText().toString().trim());
        if (about.getText().toString().trim().length() != 0) {
            map.put(Constants.TAG_ABOUT, about.getText().toString().trim());
        }

//        Log.v(TAG, "updateMyProfile: " + map);
//        Log.e(TAG, "GetSet.getToken() " + GetSet.getToken());
        Call<HashMap<String, String>> call3 = apiInterface.updatemyprofile(GetSet.getToken(), map);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                try {
                    HashMap<String, String> userdata = response.body();
                    Log.v(TAG, "updateMyProfileResponse: " + userdata.toString());

                    if (userdata.get("status").equals("true")) {
                        editor.putBoolean("isLogged", true);
                        editor.putString("userId", userdata.get("_id"));
                        editor.putString("userName", userdata.get("user_name"));
                        editor.putString("userImage", userdata.get("user_image"));
                        editor.putString("phoneNumber", userdata.get("phone_no"));
                        editor.putString("countryCode", userdata.get("country_code"));
                        editor.putString("token", "" + GetSet.getToken());
                        editor.putString("about", userdata.get("about"));
                        editor.putString("privacyprofileimage", userdata.get("privacy_profile_image"));
                        editor.putString("privacylastseen", userdata.get("privacy_last_seen"));
                        editor.putString("privacyabout", userdata.get("privacy_about"));
                        editor.commit();
                        editor.apply();

                        GetSet.setLogged(true);
                        GetSet.setUserId(pref.getString("userId", null));
                        GetSet.setUserName(pref.getString("userName", null));
                        GetSet.setphonenumber(pref.getString("phoneNumber", null));
                        GetSet.setcountrycode(pref.getString("countryCode", null));
                        GetSet.setImageUrl(pref.getString("userImage", null));
                        GetSet.setToken(pref.getString("token", null));
                        GetSet.setAbout(pref.getString("about", null));
                        GetSet.setPrivacyprofileimage(pref.getString("privacyprofileimage", Constants.TAG_EVERYONE));
                        GetSet.setPrivacylastseen(pref.getString("privacylastseen", Constants.TAG_EVERYONE));
                        GetSet.setPrivacyabout(pref.getString("privacyabout", Constants.TAG_EVERYONE));

                        Toast.makeText(getApplicationContext(), getString(R.string.updated_successfully), Toast.LENGTH_SHORT).show();
                        hideLoading();
                        finish();

                    } else if (userdata.get("status").equals("false")) {
                        hideLoading();
                        Toast.makeText(getApplicationContext(), userdata.get("message"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    hideLoading();
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                Log.e(TAG, "updateMyProfile: " + t.getMessage());
                call.cancel();
                hideLoading();
            }
        });

    }

    public void saveMyContacts(List<String> contacts, ArrayList<HashMap<String, String>> contactsName, JsonArray contactsJS) {
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_USER_ID, GetSet.getUserId());
        //map.put(Constants.TAG_CONTACTS, "" + contacts);
        map.put(Constants.TAG_CONTACTS, contactsJS.toString().replaceAll(", 0",", "));
//        Log.v(TAG, "saveMyContacts=" + contacts);
        Call<SaveMyContacts> call = apiInterface.saveMyContacts(GetSet.getToken(), map);
        call.enqueue(new Callback<SaveMyContacts>() {
            @Override
            public void onResponse(Call<SaveMyContacts> call, Response<SaveMyContacts> response) {
                Log.v(TAG, "saveMyContacts=" + response.isSuccessful());
                updateMyContacts(contacts, contactsName,contactsJS);
            }

            @Override
            public void onFailure(Call<SaveMyContacts> call, Throwable t) {
                Log.e(TAG, "saveMyContacts: " + t.getMessage());
                call.cancel();
                hideLoading();
            }
        });
    }

    void updateMyContacts(List<String> contactsNum, ArrayList<HashMap<String, String>> contactsName, JsonArray contactsJS) {
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_USER_ID, GetSet.getUserId());
        map.put(Constants.TAG_CONTACTS, contactsJS.toString().replaceAll(", 0",", "));
        map.put(Constants.TAG_PHONE_NUMBER, GetSet.getphonenumber());

//        Log.v(TAG, "updateMyContacts: " + map);
        Call<ContactsData> call3 = apiInterface.updatemycontacts(GetSet.getToken(), map);
        call3.enqueue(new Callback<ContactsData>() {
            @Override
            public void onResponse(Call<ContactsData> call, Response<ContactsData> response) {
                try {
                    if (response.isSuccessful()) {
                        ContactsData data = response.body();
                        Log.i(TAG, "updateMyContactsResponse: " + new Gson().toJson(response.body()));
                        if (data.status.equals("true")) {
                            new UpdateContactTask(data, contactsName).execute();
                        } else if (data.status.equals("false")) {
                            socketConnection.disconnect();
                            socketConnection = SocketConnection.getInstance(ProfileInfo.this);
                            Intent service = new Intent(ProfileInfo.this, ForegroundService.class);
                            service.setAction("start");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(service);
                            } else {
                                startService(service);
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    hideLoading();
                                    Intent in = new Intent(ProfileInfo.this, MainActivity.class);
                                    finish();
                                    startActivity(in);
                                }
                            }, 500);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    hideLoading();
                }
            }

            @Override
            public void onFailure(Call<ContactsData> call, Throwable t) {
                Log.e(TAG, "updateMyContacts: " + t.getMessage());
                call.cancel();
                hideLoading();
            }
        });

    }

    public boolean isValidPhoneNumber(CharSequence target) {
        if (target.length() < 7 || target.length() > 15) {
            return false;
        } else {
            return android.util.Patterns.PHONE.matcher(target).matches();
        }
    }

    private void uploadImage(byte[] imageBytes) {
        progressDialog.show();
        RequestBody requestFile = RequestBody.create(MediaType.parse("openImage/*"), imageBytes);
        MultipartBody.Part body = MultipartBody.Part.createFormData("user_image", "openImage.jpg", requestFile);

        RequestBody userid = RequestBody.create(MediaType.parse("multipart/form-data"), GetSet.getUserId());
        Call<HashMap<String, String>> call3 = apiInterface.upmyprofile(GetSet.getToken(), body, userid);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> data = response.body();
                Log.v(TAG, "uploadImageresponse=" + data);

                if (data.get(Constants.TAG_USER_IMAGE) != null) {
                    GetSet.setImageUrl(data.get(Constants.TAG_USER_IMAGE));
                    editor.putString("userImage", data.get(Constants.TAG_USER_IMAGE));
                    editor.commit();
                    editor.apply();

                    if (data.get(Constants.TAG_USER_IMAGE) != null) {
                        Glide.with(ProfileInfo.this).load(Constants.USER_IMG_PATH + data.get(Constants.TAG_USER_IMAGE))
                                .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        noimage.setVisibility(View.VISIBLE);
                                        userImage.setVisibility(View.GONE);
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        noimage.setVisibility(View.GONE);
                                        userImage.setVisibility(View.VISIBLE);
                                        return false;
                                    }
                                }).into(userImage);
                    }

                    Toast.makeText(getApplicationContext(), getString(R.string.updated_successfully), Toast.LENGTH_SHORT).show();
                }
                if (progressDialog.isShowing())
                    progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                Log.v(TAG, "uploadImage=" + t.getMessage());
                if (progressDialog.isShowing())
                    progressDialog.dismiss();
                call.cancel();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1 && requestCode == 234) {
            try {
                Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
                String imageStatus = storageManager.saveToSdCard(bitmap, "profile", timestamp + ".jpg");
                if (imageStatus.equals("success")) {
                    File file = storageManager.getImage("profile", timestamp + ".jpg");
                    String filepath = file.getAbsolutePath();
                    Log.i(TAG, "selectedImageFile: " + filepath);
                    ImageCompression imageCompression = new ImageCompression(ProfileInfo.this) {
                        @Override
                        protected void onPostExecute(String imagePath) {
                            try {
                                byte[] bytes = FileUtils.readFileToByteArray(new File(imagePath));
                                uploadImage(bytes);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    };
                    imageCompression.execute(filepath);
                } else {
                    Toast.makeText(this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "onActivityResult: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v("requestCode", "requestCode=" + requestCode);
        switch (requestCode) {
            case 100:
                int permissionContacts = ContextCompat.checkSelfPermission(ProfileInfo.this,
                        READ_CONTACTS);
                int permissionStorage = ContextCompat.checkSelfPermission(ProfileInfo.this,
                        WRITE_EXTERNAL_STORAGE);

                if (permissionContacts == PackageManager.PERMISSION_GRANTED && permissionStorage == PackageManager.PERMISSION_GRANTED) {
                    if (NetworkReceiver.isConnected()) {
                        updateProfile();
                    } else {
                        Snackbar.make(mainLay, getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG).show();
                    }
                }
                break;
            case 101:
                int permStorage = ContextCompat.checkSelfPermission(ProfileInfo.this,
                        WRITE_EXTERNAL_STORAGE);
                if (permStorage == PackageManager.PERMISSION_GRANTED) {
                    ImagePicker.pickImage(this, "Select your openImage:");
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.userImage:
            case R.id.noimage:
                if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 101);
                } else {
                    ImagePicker.pickImage(this, getString(R.string.select_your_image));
                }
                break;

            case R.id.backbtn:
                finish();
                break;
        }
    }

    public void showLoading() {
        progressbar.setVisibility(View.VISIBLE);
        fab.setVisibility(View.GONE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        btnNext.setEnabled(false);
//        if (progressDialog != null && !progressDialog.isShowing()){
//            progressDialog.show();
//        }

    }

    public void hideLoading() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        progressbar.setVisibility(View.GONE);
        fab.setVisibility(View.VISIBLE);
        btnNext.setEnabled(true);
//        if(progressDialog != null && progressDialog.isShowing()){
//            progressDialog.dismiss();
//        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetContactTask extends AsyncTask<Void, Integer, Void> {
        List<String> contactsNum = new ArrayList<>();
        ArrayList<HashMap<String, String>> contactsName = new ArrayList<>();
        JsonArray contactsNumJson = new JsonArray();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Uri uri = null;
            uri = ContactsContract.CommonDataKinds.Contactables.CONTENT_URI;
            ContentResolver cr = getContentResolver();
            Cursor cur = cr.query(uri, Constants.PROJECTION, Constants.SELECTION, Constants.SELECTION_ARGS, null);

            if (cur != null) {
                try {
                    final int nameIndex = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    final int numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    String countryCode = tm.getNetworkCountryIso();
                    while (cur.moveToNext()) {
                        HashMap<String, String> map = new HashMap<>();
                        String phoneNo = cur.getString(numberIndex).replace(" ", "");
                        String name = cur.getString(nameIndex);
                        map.put(Constants.TAG_SAVED_NAME, name);
                        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                        try {
                            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNo, countryCode.toUpperCase());
                            if (phoneNo != null && !phoneNo.equals("") && phoneNo.length() > 6 && phoneUtil.isPossibleNumberForType(numberProto, PhoneNumberUtil.PhoneNumberType.MOBILE)) {
                                String tempNo = ("" + numberProto.getNationalNumber()).replaceAll("[^0-9]", "");;
                                if (tempNo.startsWith("0")) {
                                    tempNo = tempNo.replaceFirst("^0+(?!$)", "");
                                }
                                if (!contactsNum.contains(tempNo)) {
                                    contactsNumJson.add(tempNo.replaceAll("[^0-9]", ""));
                                    contactsNum.add(tempNo.replaceAll("[^0-9]", ""));
                                }
                                map.put(Constants.TAG_PHONE_NUMBER, tempNo.replaceAll("[^0-9]", ""));
//                                Log.v("Name", "name=" + name + " num="+tempNo.replaceAll("[^0-9]", ""));
                            }
                        } catch (NumberParseException e) {
                            phoneNo = phoneNo.replaceAll("[^0-9]", "");
                            if (isValidPhoneNumber(phoneNo)) {
                                if (phoneNo.startsWith("0")) {
                                    phoneNo = phoneNo.replaceFirst("^0+(?!$)", "");
                                }
//                                Log.v("Name", "excep name=" + name + " num="+phoneNo.replaceAll("[^0-9]", ""));
                                if (!contactsNum.contains(phoneNo)) {
                                    contactsNumJson.add(phoneNo.replaceAll("[^0-9]", ""));
                                    contactsNum.add(phoneNo.replaceAll("[^0-9]", ""));
                                }
                                map.put(Constants.TAG_PHONE_NUMBER, phoneNo.replaceAll("[^0-9]", ""));
                            }
                        }

                        contactsName.add(map);
                    }
                } finally {
                    cur.close();
                }
            }
            Log.e(TAG, "getContactList: " + contactsNum.size());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            saveMyContacts(contactsNum, contactsName,contactsNumJson);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class InsertMyChannelTask extends AsyncTask<Void, Integer, Void> {
        List<ChannelResult.Result> result = new ArrayList<>();

        public InsertMyChannelTask(List<ChannelResult.Result> result) {
            this.result = result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (ChannelResult.Result result : result) {
                dbhelper.addChannel(result.channelId, result.channelName, result.channelDes, result.channelImage, result.channelType,
                        result.adminId, GetSet.getUserName(), result.totalSubscribers, result.createdTime,
                        Constants.TAG_USER_CHANNEL, "", result.blockStatus, result.report);

                if (!dbhelper.isChannelIdExistInMessages(result.channelId)) {
                    String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                    RandomString randomString = new RandomString(10);
                    String messageId = result.channelId + randomString.nextString();
                    dbhelper.addChannelMessages(result.channelId, Constants.TAG_CHANNEL, messageId, "create_channel",
                            "", "", "", "", "", "", "",
                            unixStamp, "", "");

                    int unseenCount = dbhelper.getUnseenChannelMessagesCount(result.channelId);
                    dbhelper.addChannelRecentMsgs(result.channelId, messageId, unixStamp, "" + unseenCount);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            getSubscribedChannels();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class InsertSubscribedChannelTask extends AsyncTask<Void, Integer, Void> {
        List<ChannelResult.Result> result = new ArrayList<>();

        public InsertSubscribedChannelTask(List<ChannelResult.Result> result) {
            this.result = result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (ChannelResult.Result result : result) {

                if (dbhelper.isChannelExist(result.channelId)) {
                    dbhelper.updateChannelWithoutAdminName(result.channelId, result.channelName, result.channelDes, result.channelImage,
                            result.channelType != null ? result.channelType : Constants.TAG_PUBLIC, result.adminId != null ? result.adminId : "", result.totalSubscribers);
                } else {
                    dbhelper.addChannel(result.channelId, result.channelName, result.channelDes, result.channelImage, result.channelType,
                            result.adminId, "", result.totalSubscribers, result.createdTime,
                            Constants.TAG_USER_CHANNEL, Constants.TRUE, result.blockStatus, result.report);
                }
                if (!dbhelper.isChannelIdExistInMessages(result.channelId)) {
                    String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                    RandomString randomString = new RandomString(10);
                    String messageId = result.channelId + randomString.nextString();
                    dbhelper.addChannelMessages(result.channelId, Constants.TAG_CHANNEL, messageId, "create_channel",
                            "", "", "", "", "", "", "",
                            unixStamp, "", "");

                    int unseenCount = dbhelper.getUnseenChannelMessagesCount(result.channelId);
                    dbhelper.addChannelRecentMsgs(result.channelId, messageId, unixStamp, "" + unseenCount);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            hideLoading();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class InsertMyGroupTask extends AsyncTask<Void, Integer, Void> {
        List<GroupData> result = new ArrayList<>();

        public InsertMyGroupTask(List<GroupData> result) {
            this.result = result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (GroupData groupData : result) {
                dbhelper.createGroup(groupData.groupId, groupData.groupAdminId, groupData.groupName, groupData.createdAt, groupData.groupImage);

                for (GroupData.GroupMembers groupMember : groupData.groupMembers) {
                    getUserInfo(groupData, groupMember);
                }

                String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                RandomString randomString = new RandomString(10);
                String messageId = groupData.groupId + randomString.nextString();

                if (!groupData.groupAdminId.equals(GetSet.getUserId())) {
                    String unixStamp2 = String.valueOf(System.currentTimeMillis() / 1000L);
                    String messageId2 = groupData.groupId + randomString.nextString();
                    dbhelper.addGroupMessages(messageId2, groupData.groupId, GetSet.getUserId(), groupData.groupAdminId, "add_member",
                            "", "", "", "",
                            "", "", "", unixStamp2, "", "");
                }

                dbhelper.addGroupMessages(messageId, groupData.groupId, GetSet.getUserId(), groupData.groupAdminId, "create_group",
                        "", "", "", "",
                        "", "", "", unixStamp, "", "");

                int unseenCount = dbhelper.getUnseenGroupMessagesCount(groupData.groupId);
                dbhelper.addGroupRecentMsgs(groupData.groupId, messageId, GetSet.getUserId(), unixStamp, "" + unseenCount);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            getMyChannels();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class InsertGroupMemberTask extends AsyncTask<Void, Integer, Void> {
        Map<String, String> userdata = new HashMap<>();
        GroupData groupData = new GroupData();
        GroupData.GroupMembers groupMember = new GroupData().new GroupMembers();

        public InsertGroupMemberTask(GroupData groupData, GroupData.GroupMembers groupMember, Map<String, String> userdata) {
            this.groupData = groupData;
            this.userdata = userdata;
            this.groupMember = groupMember;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String name = userdata.get(Constants.TAG_USER_NAME);
            HashMap<String, String> map = ApplicationClass.getContactrNot(getApplicationContext(), userdata.get(Constants.TAG_PHONE_NUMBER));
            if (map.get("isAlready").equals("true")) {
                name = map.get(Constants.TAG_USER_NAME);
            }
            dbhelper.addContactDetails(name, userdata.get(TAG_ID), userdata.get(Constants.TAG_USER_NAME), userdata.get(Constants.TAG_PHONE_NUMBER), userdata.get(Constants.TAG_COUNTRY_CODE), userdata.get(Constants.TAG_USER_IMAGE),
                    userdata.get(Constants.TAG_PRIVACY_ABOUT), userdata.get(Constants.TAG_PRIVACY_LAST_SEEN), userdata.get(Constants.TAG_PRIVACY_PROFILE), userdata.get(Constants.TAG_ABOUT), userdata.get(TAG_CONTACT_STATUS));

            String memberKey = groupData.groupId + groupMember.memberId;
            dbhelper.createGroupMembers(memberKey, groupData.groupId, groupMember.memberId, groupMember.memberRole);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class UpdateContactTask extends AsyncTask<Void, Integer, Void> {
        ContactsData data = new ContactsData();
        ArrayList<HashMap<String, String>> contactName = new ArrayList<>();

        public UpdateContactTask(ContactsData data, ArrayList<HashMap<String, String>> contactData) {
            this.data = data;
            this.contactName = contactData;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (ContactsData.Result result : data.result) {
                String savedName = "";
                /*for (HashMap<String, String> map : contactName) {
                    if((ApplicationClass.isStringNotNull(map.get(Constants.TAG_PHONE_NUMBER)))
                            &&(ApplicationClass.isStringNotNull(map.get(Constants.TAG_SAVED_NAME)))
                            && map.get(Constants.TAG_PHONE_NUMBER).equals(result.phone_no)){
                        savedName = map.get(Constants.TAG_SAVED_NAME);
                        break;
                    }
                }*/
                String name = "";
                HashMap<String, String> map = ApplicationClass.getContactrNot(getApplicationContext(), result.phone_no);
                if (map.get("isAlready").equals("true")) {
                    name = map.get(Constants.TAG_USER_NAME);
                }
                dbhelper.addContactDetails(name, result.user_id, result.user_name, result.phone_no, result.country_code, result.user_image, result.privacy_about,
                        result.privacy_last_seen, result.privacy_profile_image, result.about, result.contactstatus);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            socketConnection.disconnect();
            socketConnection = SocketConnection.getInstance(ProfileInfo.this);
            Intent service = new Intent(ProfileInfo.this, ForegroundService.class);
            service.setAction("start");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent in = new Intent(ProfileInfo.this, MainActivity.class);
                    startActivity(in);
                    hideLoading();
                    finish();
                }
            }, 1000);
        }
    }
}