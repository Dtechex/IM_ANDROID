package com.loopytime.im;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

public class AccountActivity extends BaseActivity implements View.OnClickListener {

    private static final int APP_REQUEST_CODE = 9002;
    private final String TAG = this.getClass().getSimpleName();
    TextView txtPrivacy, txtChangeNumber, txtDeleteAccount, txtAppLanguage, txtLanguage, txtLogout;
    Toolbar toolbar;
    ImageView btnBack;
    TextView txtTitle;
    LinearLayout languageLayout;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    public static AccountActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        activity = this;
        pref = AccountActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();

        toolbar = findViewById(R.id.actionbar);
        btnBack = toolbar.findViewById(R.id.backbtn);
        txtTitle = toolbar.findViewById(R.id.title);
        txtPrivacy = findViewById(R.id.txtPrivacy);
        txtChangeNumber = findViewById(R.id.txtChangeNumber);
        txtDeleteAccount = findViewById(R.id.txtDeleteAccount);
        txtAppLanguage = findViewById(R.id.txtAppLanguage);
        txtLanguage = findViewById(R.id.txtLanguage);
        txtLogout = findViewById(R.id.txtLogout);
        languageLayout = findViewById(R.id.languageLayout);

        if(ApplicationClass.isRTL()){
            btnBack.setRotation(180);
        } else {
            btnBack.setRotation(0);
        }

        initToolBar();

        txtPrivacy.setOnClickListener(this);
        txtChangeNumber.setOnClickListener(this);
        txtDeleteAccount.setOnClickListener(this);
        txtLogout.setOnClickListener(this);
        languageLayout.setOnClickListener(this);
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void initToolBar() {
        txtTitle.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        txtTitle.setText(R.string.account);
        btnBack.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
//        Log.e(TAG, "onCreate: " + pref.getString(Constants.TAG_LANGUAGE_CODE, ""));
        if (pref.getString(Constants.TAG_LANGUAGE_CODE, Constants.TAG_DEFAULT_LANGUAGE_CODE).contentEquals(Constants.LANGUAGE_ENGLISH)) {
            txtLanguage.setText(getString(R.string.english));
        } else if (pref.getString(Constants.TAG_LANGUAGE_CODE, Constants.TAG_DEFAULT_LANGUAGE_CODE).contentEquals(Constants.LANGUAGE_FRENCH)) {
            txtLanguage.setText(getString(R.string.french));
        } else {
            txtLanguage.setText(getString(R.string.Arabic));
        }
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.txtPrivacy:
                Intent privacy = new Intent(AccountActivity.this, PrivacyActivity.class);
                startActivity(privacy);
                break;
            case R.id.txtChangeNumber:
                //    verifyMobileNo();
                startActivity(new Intent(getApplicationContext(), ChangeNumberActivity.class));
                break;
            case R.id.txtDeleteAccount:
//                Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), DeleteAccountActivity.class));
                break;
            case R.id.txtLogout:
                openLogoutDialog();
                break;
            case R.id.languageLayout:
                Intent language = new Intent(AccountActivity.this, LanguageActivity.class);
                startActivity(language);
                break;
        }
    }

    private void openLogoutDialog() {
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
        yes.setText(getString(R.string.im_sure));
        no.setText(getString(R.string.nope));
        title.setText(R.string.do_you_want_to_logout);
        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                GetSet.logout();
                SharedPreferences settings = AccountActivity.this.getSharedPreferences("SavedPref", Context.MODE_PRIVATE);
                settings.edit().clear().commit();
                Intent logout = new Intent(AccountActivity.this, WelcomeActivity.class);
                logout.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(logout);
                finish();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
