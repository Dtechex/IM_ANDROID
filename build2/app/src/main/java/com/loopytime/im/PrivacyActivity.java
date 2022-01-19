package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.OkCallback;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.loopytime.utils.Constants.TAG_EVERYONE;
import static com.loopytime.utils.Constants.TAG_MY_CONTACTS;
import static com.loopytime.utils.Constants.TAG_NOBODY;

public class PrivacyActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    static ApiInterface apiInterface;
    ProgressDialog progressDialog;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    DatabaseHandler dbhelper;
    Toolbar toolbar;
    ImageView btnBack;
    TextView txtTitle;
    TextView txtLastSeen, txtProfilePhoto, txtAbout, txtStatus, txtBlocked, txtReceipt;
    LinearLayout lastSeenLay, profilePhotoLay, aboutLay, statusLay, blockedLay, receiptLay;
    RelativeLayout messageLay;
    PrivacyDialogFragment privacyDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = PrivacyActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        toolbar = findViewById(R.id.actionbar);
        btnBack = toolbar.findViewById(R.id.backbtn);
        txtTitle = toolbar.findViewById(R.id.title);
        txtLastSeen = findViewById(R.id.txtLastSeen);
        txtProfilePhoto = findViewById(R.id.txtProfilePhoto);
        txtAbout = findViewById(R.id.txtAbout);
        txtStatus = findViewById(R.id.txtStatus);
        txtBlocked = findViewById(R.id.txtBlocked);
        txtReceipt = findViewById(R.id.txtReceipt);
        lastSeenLay = findViewById(R.id.lastSeenLay);
        profilePhotoLay = findViewById(R.id.photoLay);
        aboutLay = findViewById(R.id.aboutLay);
        messageLay = findViewById(R.id.messageLay);
        statusLay = findViewById(R.id.statusLay);
        blockedLay = findViewById(R.id.blockedLay);
        receiptLay = findViewById(R.id.receiptLay);

        if(ApplicationClass.isRTL()){
            btnBack.setRotation(180);
        } else {
            btnBack.setRotation(0);
        }

        initToolBar();
        initData();

        lastSeenLay.setOnClickListener(this);
        profilePhotoLay.setOnClickListener(this);
        aboutLay.setOnClickListener(this);
        messageLay.setOnClickListener(this);
        statusLay.setOnClickListener(this);
        blockedLay.setOnClickListener(this);
        receiptLay.setOnClickListener(this);

    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void initToolBar() {
        txtTitle.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        txtTitle.setText(getString(R.string.privacy));
        setSupportActionBar(toolbar);
        btnBack.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void initData() {
        String lastSeen = GetSet.getPrivacylastseen();
        String profileImage = GetSet.getPrivacyprofileimage();
        String about = GetSet.getPrivacyabout();

        if (lastSeen != null) {
            if (lastSeen.equalsIgnoreCase(TAG_EVERYONE)) {
                txtLastSeen.setText(getString(R.string.everyone));
            } else if (lastSeen.equalsIgnoreCase(TAG_NOBODY)) {
                txtLastSeen.setText(getString(R.string.nobody));
            } else {
                txtLastSeen.setText(getString(R.string.my_contacts));
            }
        }

        if (profileImage != null) {
            if (profileImage.equalsIgnoreCase(TAG_EVERYONE)) {
                txtProfilePhoto.setText(getString(R.string.everyone));
            } else if (profileImage.equalsIgnoreCase(TAG_NOBODY)) {
                txtProfilePhoto.setText(getString(R.string.nobody));
            } else {
                txtProfilePhoto.setText(getString(R.string.my_contacts));
            }
        }

        if (about != null) {
            if (about.equalsIgnoreCase(TAG_EVERYONE)) {
                txtAbout.setText(getString(R.string.everyone));
            } else if (about.equalsIgnoreCase(TAG_NOBODY)) {
                txtAbout.setText(getString(R.string.nobody));
            } else {
                txtAbout.setText(getString(R.string.my_contacts));
            }
        }
    }

    @Override
    protected void onResume() {
        if (dbhelper != null) {
            int blockedSize = dbhelper.getBlockedContacts(this).size();
            if (blockedSize > 0) {
                txtBlocked.setText(getString(R.string.blocked_contacts) + " " + blockedSize);
            } else {
                txtBlocked.setText(getString(R.string.blocked_contacts));
            }
        }
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.lastSeenLay:
                openPrivacyDialog(R.id.lastSeenLay, txtLastSeen.getText(), getString(R.string.last_seen));
                break;
            case R.id.photoLay:
                openPrivacyDialog(R.id.photoLay, txtProfilePhoto.getText(), getString(R.string.profile_photo));
                break;
            case R.id.aboutLay:
                openPrivacyDialog(R.id.aboutLay, txtAbout.getText(), getString(R.string.about));
                break;
            case R.id.statusLay:
//                openPrivacyDialog(R.id.statusLay, txtStatus.getText(), getString(R.string.status));
                break;
            case R.id.messageLay:

                break;
            case R.id.blockedLay:
                Intent block = new Intent(getApplicationContext(), BlockedContactsActivity.class);
                startActivity(block);
                break;
            case R.id.receiptLay:

                break;
        }
    }

    private void openPrivacyDialog(final int id, CharSequence text, String title) {
        privacyDialogFragment = new PrivacyDialogFragment();
//        privacyDialogFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        privacyDialogFragment.setCancelable(true);
        privacyDialogFragment.setSelected(text);
        privacyDialogFragment.setTitle(title);
        privacyDialogFragment.setCallBack(new OkCallback() {
            @Override
            public void onOkClicked(Object object) {
                privacyDialogFragment.dismiss();
                switch (id) {
                    case R.id.lastSeenLay:
                        txtLastSeen.setText((String) object);
                        break;
                    case R.id.photoLay:
                        txtProfilePhoto.setText((String) object);
                        break;
                    case R.id.aboutLay:
                        txtAbout.setText((String) object);
                        break;
                    case R.id.statusLay:
                        txtStatus.setText((String) object);
                        break;
                    case R.id.messageLay:

                        break;
                    case R.id.blockedLay:

                        break;
                    case R.id.receiptLay:

                        break;
                }

                updatePrivacy();
            }
        });
        privacyDialogFragment.show(getSupportFragmentManager(), TAG);
    }

    private void updatePrivacy() {

        final String lastSeen = checkString(txtLastSeen.getText().toString().trim().toLowerCase());
        final String profileImage = checkString(txtProfilePhoto.getText().toString().trim().toLowerCase());
        final String about = checkString(txtAbout.getText().toString().trim().toLowerCase());

        editor.putString("privacyprofileimage", profileImage);
        editor.putString("privacylastseen", lastSeen);
        editor.putString("privacyabout", about);
        editor.commit();

        GetSet.setPrivacyprofileimage(pref.getString("privacyprofileimage", Constants.TAG_EVERYONE));
        GetSet.setPrivacylastseen(pref.getString("privacylastseen", Constants.TAG_EVERYONE));
        GetSet.setPrivacyabout(pref.getString("privacyabout", Constants.TAG_EVERYONE));

        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_USER_ID, GetSet.getUserId());
        map.put(Constants.TAG_PRIVACY_LAST_SEEN, lastSeen);
        map.put(Constants.TAG_PRIVACY_PROFILE, profileImage);
        map.put(Constants.TAG_PRIVACY_ABOUT, about);

        Call<HashMap<String, String>> call3 = apiInterface.updateMyPrivacy(GetSet.getToken(), map);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                HashMap<String, String> userdata = response.body();
//                if (userdata.get("status").equals("true")) {
//
//                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                call.cancel();
//                Log.e(TAG, "updateMyPrivacy: " + t.getMessage());
            }
        });
    }

    private String checkString(String privacy) {
        if (privacy.equalsIgnoreCase(getString(R.string.everyone))) {
            privacy = TAG_EVERYONE;
        } else if (privacy.equalsIgnoreCase(getString(R.string.nobody))) {
            privacy = TAG_NOBODY;
        } else {
            privacy = TAG_MY_CONTACTS;
        }
        return privacy;
    }
}
