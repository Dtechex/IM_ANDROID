package com.loopytime.im;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.external.TouchImageView;
import com.loopytime.utils.Constants;

public class ImageOpenActivity extends BaseActivity {

    TouchImageView imageView;
    ImageView closeBtn;
    String url = "", from;
    int placeHolderId = R.drawable.profile_banner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_open);
        imageView = findViewById(R.id.imageView);
        closeBtn = findViewById(R.id.closeBtn);

        // overridePendingTransition(R.anim.slide_up,R.anim.slide_up);

        if (getIntent().getExtras().getString(Constants.TAG_USER_IMAGE) != null && getIntent().getExtras().getString(Constants.TAG_FROM) != null) {
            url = getIntent().getExtras().getString(Constants.TAG_USER_IMAGE);
            from = getIntent().getExtras().getString(Constants.TAG_FROM);
        }

        if (from.equals(Constants.TAG_SINGLE)) {
            placeHolderId = R.drawable.profile_banner;
        } else if (from.equals(Constants.TAG_GROUP)) {
            placeHolderId = R.drawable.ic_group_banner;
        } else if (from.equals(Constants.TAG_CHANNEL)) {
            placeHolderId = R.drawable.ic_channel_banner;
        }

        Glide.with(getApplicationContext()).load(url)
                .apply(new RequestOptions().error(placeHolderId).placeholder(placeHolderId))
                .into(imageView);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // overridePendingTransition(R.anim.slide_up,R.anim.slide_down);
                finish();
            }
        });
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    public void finish() {
        super.finish();
        // overridePendingTransition(R.anim.slide_up,R.anim.slide_down);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
