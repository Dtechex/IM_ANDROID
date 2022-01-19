package com.loopytime.im;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.loopytime.country.BaseUrlSpan;
import com.loopytime.country.Countries;
import com.loopytime.country.Country;
import com.loopytime.country.CustomClicableSpan;
import com.loopytime.country.Devices;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;
import com.truecaller.android.sdk.ITrueCallback;
import com.truecaller.android.sdk.TrueError;
import com.truecaller.android.sdk.TrueException;
import com.truecaller.android.sdk.TrueProfile;
import com.truecaller.android.sdk.TruecallerSDK;
import com.truecaller.android.sdk.TruecallerSdkScope;
import com.truecaller.android.sdk.clients.VerificationCallback;
import com.truecaller.android.sdk.clients.VerificationDataBundle;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MobileNumberActivity extends FragmentActivity {
    private static final String TAG = MobileNumberActivity.class.getSimpleName();
    private static final int APP_REQUEST_CODE = 9002;
    ApiInterface apiInterface;

    private Countries countryDb;

    private Button countrySelectButton;
    private EditText countryCodeEditText;
    private BackspaceKeyEditText phoneNumberEditText;

    private boolean ignoreNextCodeChange;
    int lastPos = 0;
    private static final int REQUEST_COUNTRY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_sign_phone2);
        TruecallerSdkScope trueScope = new TruecallerSdkScope.Builder(this, sdkCallback)
                .consentMode(TruecallerSdkScope.CONSENT_MODE_FULLSCREEN)
                .consentTitleOption(TruecallerSdkScope.SDK_CONSENT_TITLE_VERIFY)
                .footerType(TruecallerSdkScope.FOOTER_TYPE_CONTINUE)
                .sdkOptions(TruecallerSdkScope.SDK_OPTION_WITH_OTP)
                .build();

        TruecallerSDK.init(trueScope);
        countrySelectButton = (Button) findViewById(R.id.button_country_select);
        countryCodeEditText = (EditText) findViewById(R.id.tv_country_code);
        countryCodeEditText.setEnabled(false);
        countryCodeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                final Activity a = MobileNumberActivity.this;
                if (a != null) {

                    final String str = s.toString();
                    if (str.length() == 4 && countryDb != null) {
                        if (countryDb.getCountryByPhoneCode(str) != null) {
                            focusPhone();
                        } else if (countryDb.getCountryByPhoneCode(str.substring(0, 3)) != null) {
                            countryCodeEditText.setText(str.substring(0, 3));
                            phoneNumberEditText.setText(str.substring(3, 4));
                            focusPhone();
                            return;
                        } else if (countryDb.getCountryByPhoneCode(str.substring(0, 2)) != null) {
                            countryCodeEditText.setText(str.substring(0, 2));
                            phoneNumberEditText.setText(str.substring(2, 4));
                            focusPhone();
                            return;
                        } else if (countryDb.getCountryByPhoneCode(str.substring(0, 1)) != null) {
                            countryCodeEditText.setText(str.substring(0, 1));
                            phoneNumberEditText.setText(str.substring(1, 4));
                            focusPhone();
                            return;
                        }
                    }

                    if (!ignoreNextCodeChange) {
                        if (TextUtils.isEmpty(s)) {
                            countrySelectButton.setText(R.string.auth_phone_country_title);
                        } else {
                            if (countryDb != null) {
                                final Country country = countryDb.getCountryByPhoneCode(s.toString());
                                if (country == null) {
                                    countrySelectButton.setText(R.string.auth_phone_error_invalid_country);
                                } else {
                                    setCountryName(country);
                                }
                            }
                        }
                    } else {
                        ignoreNextCodeChange = false;
                    }
                }
            }
        });


        phoneNumberEditText = (BackspaceKeyEditText) findViewById(R.id.tv_phone_number);
        phoneNumberEditText.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        phoneNumberEditText.setBackspaceListener(new BackspaceKeyEditText.BackspacePressListener() {
            @Override
            public boolean onBackspacePressed() {
                if (phoneNumberEditText.getText().length() == 0) {
                    focusCode();
                    return false;
                } else {
                    return true;
                }
            }
        });

        phoneNumberEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_GO) {
                    //requestCode();
                    return true;
                }
                return false;
            }
        });
        phoneNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        countryCodeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    focusPhone();
                    return true;
                }
                return false;
            }
        });
        countryDb = Countries.getInstance();
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        String deviceCountry = Devices.getDeviceCountry();
        countryCodeEditText.setText("+91");
        countrySelectButton.setText("INDIA");
        if (TruecallerSDK.getInstance().isUsable()) {
            TruecallerSDK.getInstance().getUserProfile(MobileNumberActivity.this);
        }
        /*if (!TextUtils.isEmpty(deviceCountry)) {
            Country country = countryDb.getCountryByShortName(deviceCountry);
            setCountryName(country);
            if (country != null) {
                countryCodeEditText.setText("+91");//country.phoneCode);
                focusPhone();
            } else {
                focusCode();
            }
        } else {
            setCountryName(null);
            countryCodeEditText.setText("");
            focusCode();
        }*/

        //    v.findViewById(R.id.divider).setBackgroundColor(style.getDividerColor());
        setTosAndPrivacy((TextView) findViewById(R.id.disclaimer));

        findViewById(R.id.button_country_select).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivityForResult(new Intent(MobileNumberActivity.this, PickCountryActivity.class), REQUEST_COUNTRY);
            }
        });

        findViewById(R.id.button_why).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new android.app.AlertDialog.Builder(MobileNumberActivity.this)
                        .setMessage(R.string.auth_phone_why_description)
                        .setPositiveButton(R.string.auth_phone_why_done, null)
                        .show()
                        .setCanceledOnTouchOutside(true);
            }
        });


        findViewById(R.id.button_continue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //requestCode();
               // Signin(phoneNumberEditText.getText().toString(), countryCodeEditText.getText().toString(), "IN");
              /*if(TruecallerSDK.getInstance().isUsable())
                TruecallerSDK.getInstance().getUserProfile(MobileNumberActivity.this);
            else{*/
                if (TextUtils.isEmpty(phoneNumberEditText.getText().toString().trim())) {
                    Toast.makeText(MobileNumberActivity.this, R.string.fui_invalid_phone_number, Toast.LENGTH_SHORT).show();
                    return;
                }
                TruecallerSDK.getInstance().requestVerification("IN", phoneNumberEditText.getText().toString(), apiCallback, MobileNumberActivity.this);
                //}
            }
        });
    }

    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_COUNTRY && resultCode == Activity.RESULT_OK) {
            setCountry(new Country(data.getStringExtra("country_code"),
                    data.getStringExtra("country_shortname"),
                    data.getIntExtra("country_id", 0)));
        }
        TruecallerSDK.getInstance().onActivityResultObtained(this, resultCode, data);
    }

    private void setCountry(final Country country) {
        final Activity a = this;
        if (a != null) {
            if (country != null) {
                ignoreNextCodeChange = true;
                setCountryName(country);
                countryCodeEditText.setText(country.phoneCode);
            }
            focusPhone();
        }
    }

    private void focusCode() {
        focus(countryCodeEditText);
    }

    private void focusPhone() {
        focus(phoneNumberEditText);
    }

    void focus(final EditText editText) {
        editText.post(new Runnable() {
            @Override
            public void run() {
                editText.requestFocus();
                editText.setSelection(editText.getText().length());
            }
        });
    }

    private void setCountryName(final Country country) {
        if (country == null) {
            countrySelectButton.setText(getString(R.string.auth_phone_country_title));
        } else {
            countrySelectButton.setText(getString(country.fullNameRes));
        }
    }

    DatabaseHandler dbhelper;

    void Signin(String number, String code, String countryName) {

        dbhelper = DatabaseHandler.getInstance(this);
        number = number.replaceAll("[^0-9]", "");
        if (number.startsWith("0")) {
            number = number.replaceFirst("^0+(?!$)", "");
        }
        System.out.println("ghdd " + number + " " + code);
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

                        Intent i = new Intent(MobileNumberActivity.this, WalkThough.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();

                    } else if (userdata.get("status").equals("false")) {
                        Toast.makeText(getApplicationContext(), userdata.get("message"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("signInResponse " + e.toString());
                    //makeToast(getString(R.string.something_wrong)+" "+e.getMessage());
                    finish();
                }

            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                t.printStackTrace();
                call.cancel();
                //       System.out.println("signInResponse failure "+t.getMessage());
                //makeToast(getString(R.string.something_wrong));
                finish();
            }
        });
    }

    protected void setTosAndPrivacy(TextView tv) {

        String tosUrl = "http://loopytime.im";
        boolean tosUrlAvailable = tosUrl != null && !tosUrl.isEmpty();
        boolean tosTextAvailable = false;
        boolean tosAvailable = tosUrlAvailable || tosTextAvailable;
        boolean privacyUrlAvailable = false;
        boolean privacyTextAvailable = false;//privacyText != null && !privacyText.isEmpty();
        boolean ppAvailable = privacyUrlAvailable || privacyTextAvailable;

        boolean tosOrPrivacyAvailable = tosAvailable || ppAvailable;

        if (!tosOrPrivacyAvailable) {
            tv.setVisibility(View.GONE);
            return;
        }

        String text;
        SpannableStringBuilder builder;
        text = getString(R.string.auth_privacy);
        builder = new SpannableStringBuilder(text);

        tv.setText(getString(R.string.auth_privacy));
        findAndHilightPrivacy(builder, text, privacyUrlAvailable);


        // builder.append(" ".concat(getString(R.string.auth_find_by_diclamer)));
        tv.setText(builder);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void findAndHilightTos(SpannableStringBuilder builder, String text, boolean urlAvailable) {
        String tosIndex = getString(R.string.auth_tos_index);
        int index = text.indexOf(tosIndex);
        ClickableSpan span;
        if (urlAvailable) {
            span = new BaseUrlSpan("http://loopytime.im", false);
        } else {
            span = new CustomClicableSpan(new CustomClicableSpan.SpanClickListener() {
                @Override
                public void onClick() {
                    new AlertDialog.Builder(MobileNumberActivity.this)
                            .setTitle(R.string.auth_tos_index)
                            .setMessage("")
                            .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
            });
        }
        builder.setSpan(span, index, index + tosIndex.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    private void findAndHilightPrivacy(SpannableStringBuilder builder, String text, boolean urlAvailable) {
        String ppIndex = getString(R.string.auth_privacy_index);
        int index = text.indexOf(ppIndex);
        ClickableSpan span;

        span = new CustomClicableSpan(new CustomClicableSpan.SpanClickListener() {
            @Override
            public void onClick() {
                new AlertDialog.Builder(MobileNumberActivity.this)
                        .setTitle(R.string.auth_privacy_index)
                        .setMessage("")
                        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        builder.setSpan(span, index, index + ppIndex.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    private final ITrueCallback sdkCallback = new ITrueCallback() {

        @Override
        public void onSuccessProfileShared(@NonNull final TrueProfile trueProfile) {
            phoneNumberEditText.setText(trueProfile.phoneNumber.replaceAll("\\+91", ""));
            Signin(phoneNumberEditText.getText().toString().replaceAll("[^0-9]", ""), countryCodeEditText.getText().toString(), "IN");
        }

        @Override
        public void onFailureProfileShared(@NonNull final TrueError trueError) {

        }

        @Override
        public void onVerificationRequired() {
            TruecallerSDK.getInstance().requestVerification("IN", countryCodeEditText + phoneNumberEditText.getText().toString(), apiCallback, MobileNumberActivity.this);

        }

    };

    VerificationCallback apiCallback = new VerificationCallback() {

        @Override
        public void onRequestSuccess(int requestCode, @Nullable VerificationDataBundle extras) {

            if (requestCode == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
                createAlertDialog("Miss Call");
            }
            if (requestCode == VerificationCallback.TYPE_MISSED_CALL_RECEIVED) {
                alertDialog.dismiss();
                Signin(phoneNumberEditText.getText().toString(), countryCodeEditText.getText().toString(), "IN");
            }
            if (requestCode == VerificationCallback.TYPE_OTP_INITIATED) {
                createAlertDialog("OTP SMS");
            }
            if (requestCode == VerificationCallback.TYPE_OTP_RECEIVED) {
                alertDialog.dismiss();
                Signin(phoneNumberEditText.getText().toString(), countryCodeEditText.getText().toString(), "IN");
            }

            if (requestCode == VerificationCallback.TYPE_VERIFICATION_COMPLETE) {
                alertDialog.dismiss();
                Signin(phoneNumberEditText.getText().toString(), countryCodeEditText.getText().toString(), "IN");
            }
            if (requestCode == VerificationCallback.TYPE_PROFILE_VERIFIED_BEFORE) {
                alertDialog.dismiss();
                Signin(phoneNumberEditText.getText().toString(), countryCodeEditText.getText().toString(), "IN");
            }

        }

        @Override
        public void onRequestFailure(final int requestCode, @NonNull final TrueException e) {
            System.out.println("failure " + e.getExceptionMessage());
        }


    };
    AlertDialog alertDialog;

    public void createAlertDialog(String type) {
        alertDialog = new AlertDialog.Builder(this).setTitle("Verifying by " + type).setMessage("Initiating " + type).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).create();
        alertDialog.show();
    }
}

