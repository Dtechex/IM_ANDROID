package com.loopytime.im;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MobileNumberActivity2 extends BaseActivity {
    private static final String TAG = MobileNumberActivity2.class.getSimpleName();
    private static final int APP_REQUEST_CODE = 9002;
    ApiInterface apiInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_kit);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);

        verifyMobileNo();

    }

    @Override
    public void onNetworkChange(boolean isConnected) {

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
                    Log.d(TAG, "onActivityResult: " + user.getPhoneNumber());
                    try {
                        Phonenumber.PhoneNumber numberProto = phoneUtil.parse(user.getPhoneNumber(), null);
//                                String regionCode = phoneUtil.getRegionCodeForCountryCode(Integer.parseInt(phNumber.getCountryCode()));
                        String regionCode = phoneUtil.getRegionCodeForNumber(numberProto);

                        Signin("" + numberProto.getNationalNumber(), "" + numberProto.getCountryCode(), regionCode);

                    } catch (NumberParseException e) {
                        Log.d(TAG, "NumberParseException: " + e.getMessage());
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
            }  // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...

        } else {
            finish();
        }
    }

    void Signin(String number, String code, String countryName) {
        number = number.replaceAll("[^0-9]", "");
        if (number.startsWith("0")) {
            number = number.replaceFirst("^0+(?!$)", "");
        }
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_PHONE_NUMBER, number);
        map.put(Constants.TAG_COUNTRY_CODE, code);
        map.put(Constants.TAG_COUNTRY, countryName);

        Call<HashMap<String, String>> call3 = apiInterface.signin(map);
        call3.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                try {

                    dbhelper.clearDB(getApplicationContext());
                    HashMap<String, String> userdata = response.body();
                    Log.v(TAG, "signInResponse: " + userdata);

                    if (userdata.get("status").equals("true")) {
                        GetSet.setToken(userdata.get("token"));
                        GetSet.setUserId(userdata.get("_id"));
                        GetSet.setUserName(userdata.get("user_name"));
                        GetSet.setImageUrl(userdata.get("user_image"));
                        GetSet.setphonenumber(userdata.get("phone_no"));
                        GetSet.setcountrycode(userdata.get("country_code"));
                        GetSet.setCountryname(countryName);

                        Intent i = new Intent(MobileNumberActivity2.this, ProfileInfo.class);
                        i.putExtra("from", "welcome");
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();

                    } else if (userdata.get("status").equals("false")) {
                        Toast.makeText(getApplicationContext(), userdata.get("message"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("signInResponse "+e.toString());
                    makeToast(getString(R.string.something_wrong)+" "+e.getMessage());
                    finish();
                }

            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                t.printStackTrace();
                call.cancel();
         //       System.out.println("signInResponse failure "+t.getMessage());
                makeToast(getString(R.string.something_wrong));
                finish();
            }
        });
    }

    public void verifyMobileNo() {
        if (NetworkReceiver.isConnected()) {
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
        } else {
            makeToast(getString(R.string.no_internet_connection));
        }
    }
}
