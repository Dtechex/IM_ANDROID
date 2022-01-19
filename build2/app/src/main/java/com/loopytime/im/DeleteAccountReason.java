package com.loopytime.im;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.SocketConnection;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;

public class DeleteAccountReason extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Toolbar toolbar;
    ImageView btnBack;
    TextView txtTitle;
    LinearLayout btnNext;
    Spinner reasonSpinner;
    EditText edtReason;
    DatabaseHandler dbhelper;
    static ApiInterface apiInterface;
    SocketConnection socketConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account_reason);

        socketConnection = SocketConnection.getInstance(this);
        dbhelper = DatabaseHandler.getInstance(this);
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = DeleteAccountReason.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();

        toolbar = findViewById(R.id.actionbar);
        btnBack = findViewById(R.id.backbtn);
        txtTitle = findViewById(R.id.title);
        btnNext = findViewById(R.id.btnNext);
        reasonSpinner = findViewById(R.id.reasonSpinner);
        edtReason = findViewById(R.id.edtReason);
        btnBack.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        txtTitle.setText(getString(R.string.delete_account));

        if(ApplicationClass.isRTL()){
            btnBack.setRotation(180);
        } else {
            btnBack.setRotation(0);
        }

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
                break;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
