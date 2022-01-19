package com.loopytime.im;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportActivity extends BaseActivity {

    private String TAG = this.getClass().getSimpleName();
    Toolbar toolbar;
    ImageView btnBack;
    EditText edtReport;
    TextView txtTitle, btnReport;
    private ProgressDialog progressDialog;
    ApiInterface apiInterface;
    String channelId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        channelId = getIntent().getStringExtra(Constants.TAG_CHANNEL_ID);
        toolbar = findViewById(R.id.actionbar);
        btnBack = toolbar.findViewById(R.id.backbtn);
        txtTitle = toolbar.findViewById(R.id.title);
        btnReport = findViewById(R.id.btnReport);
        edtReport = findViewById(R.id.edtReport);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);

        if(ApplicationClass.isRTL()){
            btnBack.setRotation(180);
        } else {
            btnBack.setRotation(0);
        }

        initToolBar();

        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty("" + edtReport.getText())) {
                    makeToast(getString(R.string.enter_any_description));
                } else {
                    reportChannel(channelId, edtReport.getText().toString());
                }
            }
        });

    }

    private void reportChannel(String channelId, String description) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(Constants.TAG_USER_ID, GetSet.getUserId());
        hashMap.put(Constants.TAG_CHANNEL_ID, channelId);
        hashMap.put(Constants.TAG_REPORT, description);

        Call<HashMap<String, String>> call = apiInterface.reportChannel(GetSet.getToken(), hashMap);
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                Log.i(TAG, "ReportChannel Response: " + response.body());
                if (response.body().get(Constants.TAG_STATUS).equalsIgnoreCase(Constants.TRUE)) {
                    makeToast(getString(R.string.reported_successfully));
                    finish();
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                call.cancel();
                Log.e(TAG, "Report Channel onFailure: " + t.getMessage());
            }
        });
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void initToolBar() {
        txtTitle.setText(getString(R.string.report));
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
}
