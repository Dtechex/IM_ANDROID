package com.loopytime.im;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.loopytime.model.HelpData;

public class HelpViewActivity extends BaseActivity {

    private final String TAG = this.getClass().getSimpleName();
    Toolbar toolbar;
    ImageView btnBack;
    TextView txtTitle;
    HelpData.Term term;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_help_view);

        toolbar = findViewById(R.id.actionbar);
        btnBack = toolbar.findViewById(R.id.backbtn);
        txtTitle = toolbar.findViewById(R.id.title);
        webView = findViewById(R.id.webView);

        if(ApplicationClass.isRTL()){
            btnBack.setRotation(180);
        } else {
            btnBack.setRotation(0);
        }

        if (getIntent().getSerializableExtra("HELP") != null) {
            term = (HelpData.Term) getIntent().getSerializableExtra("HELP");
        }
        initToolBar();
        initWebView(term);
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void initToolBar() {
        txtTitle.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        txtTitle.setText(term.title != null ? term.title : "");
        btnBack.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void initWebView(HelpData.Term term) {
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                // Activities and WebViews measure progress with different scales.
                // The progress meter will automatically disappear when we reach 100%
                HelpViewActivity.this.setProgress(progress * 1000);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

            }
        });

        Log.e(TAG, "initWebView: "+term.description );
        webView.loadData(term.description, "text/html", "UTF-8");

    }
}
