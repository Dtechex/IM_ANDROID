package com.loopytime.im;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.loopytime.external.RandomString;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.model.ChangeNumberResult;
import com.loopytime.model.GroupData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangeNumberActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    private static final int APP_REQUEST_CODE = 9002;
    static ApiInterface apiInterface;
    ProgressDialog progressDialog;
    DatabaseHandler dbhelper;
    EditText edtCountryCode, edtPhoneNumber;
    LinearLayout btnNext;
    TextView txtTitle;
    ImageView btnBack;
    String newCountryCode, newPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_number);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);

        dbhelper = DatabaseHandler.getInstance(this);
        btnBack = findViewById(R.id.backbtn);
        txtTitle = findViewById(R.id.title);
        btnNext = findViewById(R.id.btnNext);
        edtCountryCode = findViewById(R.id.edtCountryCode);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);

        if(ApplicationClass.isRTL()){
            btnBack.setRotation(180);
        } else {
            btnBack.setRotation(0);
        }

        btnBack.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        txtTitle.setText(getString(R.string.change_number));

        btnBack.setOnClickListener(this);
        btnNext.setOnClickListener(this);
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
            case R.id.btnNext:
                if (TextUtils.isEmpty("" + edtCountryCode.getText())) {
                    makeToast(getString(R.string.enter_country_code));
                } else if (TextUtils.isEmpty("" + edtPhoneNumber.getText())) {
                    makeToast(getString(R.string.enter_phone_number));
                } else {
                    String countryCode = "" + edtCountryCode.getText();
                    if (countryCode.contains("+")) {
                        countryCode = countryCode.replaceAll("\\+", "");
                    }

                    checkAvailability(countryCode, "" + edtPhoneNumber.getText());
                }
                break;
        }
    }

    public void checkAvailability(String countryCode, String phoneNumber) {
        Call<Map<String, String>> call = apiInterface.verifyNewNumber(GetSet.getToken(), GetSet.getUserId(), phoneNumber);
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
//                Log.i(TAG, "checkAvailability: " + response.body());
                Map<String, String> map = response.body();
                if (map.get(Constants.TAG_STATUS).equalsIgnoreCase(Constants.TRUE)) {
                    newCountryCode = countryCode;
                    newPhoneNumber = phoneNumber;
                    final Dialog dialog = new Dialog(ChangeNumberActivity.this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.setContentView(R.layout.default_popup);
                    dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 90 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(true);

                    TextView title = dialog.findViewById(R.id.title);
                    TextView yes = dialog.findViewById(R.id.yes);
                    TextView no = dialog.findViewById(R.id.no);
                    yes.setText(getString(R.string.im_sure));
                    no.setText(getString(R.string.nope));
                    title.setText(R.string.verify_old_number);
                    no.setVisibility(View.VISIBLE);

                    yes.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            verifyNumber(countryCode, phoneNumber);
                        }
                    });

                    no.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                } else {
                    makeToast(getString(R.string.account_already_exists_with_this_number));
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
//                Log.e(TAG, "checkAvailability: " + t.getMessage());
                call.cancel();
            }
        });
    }

    public void verifyNumber(String countryCode, String phoneNumber) {
        /*final Intent intent = new Intent(ChangeNumberActivity.this, MobileNumberActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(LoginType.PHONE,
                        MobileNumberActivity.ResponseType.TOKEN);
        configurationBuilder.setReadPhoneStateEnabled(true);
        configurationBuilder.setEnableSms(true);
        configurationBuilder.setDefaultCountryCode(GetSet.getcountrycode());
        configurationBuilder.setInitialPhoneNumber(new PhoneNumber(countryCode, phoneNumber, ""));
        intent.putExtra(
                MobileNumberActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,
                configurationBuilder.build());
        startActivityForResult(intent, APP_REQUEST_CODE);*/

        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build());
        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setTheme(R.style.OTPTheme)
                        .build(),
                APP_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getPhoneNumber() != null) {
                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    Log.d(TAG, "onActivityResult: "+user.getPhoneNumber());
                    try {
                        Phonenumber.PhoneNumber numberProto = phoneUtil.parse(user.getPhoneNumber(), null);
//                                String regionCode = phoneUtil.getRegionCodeForCountryCode(Integer.parseInt(phNumber.getCountryCode()));
                        String regionCode = phoneUtil.getRegionCodeForNumber(numberProto);

                        changeMyNumber(GetSet.getUserId(), ""+numberProto.getNationalNumber(),""+numberProto.getCountryCode());
                    } catch (NumberParseException e) {
                        Log.d(TAG, "NumberParseException: "+e.getMessage());
                        makeToast(getString(R.string.something_wrong));
                        finish();
                    }
                }
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {

                            }
                        });
                // ...
            }
        }
    }

    private void changeMyNumber(String userId, String phoneNumber, String countryCode) {
        Call<ChangeNumberResult> call = apiInterface.changeMyNumber(GetSet.getToken(), GetSet.getUserId(), phoneNumber, countryCode);
        call.enqueue(new Callback<ChangeNumberResult>() {
            @Override
            public void onResponse(Call<ChangeNumberResult> call, Response<ChangeNumberResult> response) {
                Log.i(TAG, "changeMyNumber: " + new Gson().toJson(response.body()));
                ChangeNumberResult result = response.body();
                if (result.status.equalsIgnoreCase(Constants.TRUE)) {
                    sendMessageToGroup(result);
                } else {
                    makeToast(getString(R.string.there_is_some_error));
                }
            }

            @Override
            public void onFailure(Call<ChangeNumberResult> call, Throwable t) {
//                Log.e(TAG, "changeMyNumber: " + t.getMessage());
                call.cancel();
            }
        });
    }

    private void sendMessageToGroup(ChangeNumberResult result) {
        if (progressDialog != null && !progressDialog.isShowing())
            progressDialog.show();
        List<GroupData> groupData = dbhelper.getGroups();

        for (GroupData groupDatum : groupData) {
            try {
                String unixStamp = String.valueOf(System.currentTimeMillis() / 1000L);
                RandomString randomString = new RandomString(10);
                String messageId = groupDatum.groupId + randomString.nextString();

                JSONObject message = new JSONObject();
                message.put(Constants.TAG_GROUP_ID, groupDatum.groupId);
                message.put(Constants.TAG_GROUP_NAME, groupDatum.groupName);
                message.put(Constants.TAG_CHAT_TYPE, Constants.TAG_GROUP);
                message.put(Constants.TAG_CHAT_TIME, unixStamp);
                message.put(Constants.TAG_MESSAGE_ID, messageId);
                message.put(Constants.TAG_MEMBER_ID, GetSet.getUserId());
                message.put(Constants.TAG_MEMBER_NAME, GetSet.getUserName());
                message.put(Constants.TAG_MEMBER_NO, GetSet.getphonenumber());
                message.put(Constants.TAG_MESSAGE_TYPE, "change_number");
                String tempMsg = getString(R.string.changed_therir_number);
                message.put(Constants.TAG_MESSAGE, tempMsg);
                message.put(Constants.TAG_ATTACHMENT, GetSet.getphonenumber());
                message.put(Constants.TAG_CONTACT_COUNTRY_CODE, result.changeNumber.countryCode);
                message.put(Constants.TAG_CONTACT_PHONE_NO, result.changeNumber.phoneNo);
                message.put(Constants.TAG_CONTACT_NAME, result.changeNumber.userName);
                message.put(Constants.TAG_GROUP_ADMIN_ID, groupDatum.groupAdminId);
                socketConnection.startGroupChat(message);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        updateMyNumber(result);
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

    private void updateMyNumber(ChangeNumberResult result) {
        GetSet.setphonenumber(result.changeNumber.phoneNo);
        GetSet.setcountrycode(result.changeNumber.countryCode);
        finish();
    }

}
