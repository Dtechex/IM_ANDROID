package com.loopytime.country;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.loopytime.im.R;
import com.loopytime.im.ApplicationClass;

public class BaseUrlSpan extends URLSpan {

    protected boolean hideUrlStyle;

    public BaseUrlSpan(String url, boolean hideUrlStyle) {
        super(url);
        this.hideUrlStyle = hideUrlStyle;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        if (hideUrlStyle) {
            ds.setColor(Color.BLACK);
        }
        ds.setColor(Color.BLUE);
        ds.setUnderlineText(false);
    }

    @Override
    public void onClick(View v) {
        Context ctx = v.getContext();
        Intent intent = buildChromeIntent().intent;
        intent.setData(Uri.parse(getURL()));
        if (intent.resolveActivity(ctx.getPackageManager()) != null) {
            ctx.startActivity(
                    intent);
        } else {
            intent.setData(Uri.parse("http://" + getURL()));
            if (intent.resolveActivity(ctx.getPackageManager()) != null) {
                ctx.startActivity(
                        intent);
            } else {
                Toast.makeText(ctx, "Unknown URL type", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static CustomTabsIntent buildChromeIntent() {
        CustomTabsIntent.Builder customTabsIntent = new CustomTabsIntent.Builder();

//        Intent sendIntent = new Intent(Intent.ACTION_SEND);
//        sendIntent.setType("*/*");
//        PendingIntent pi = PendingIntent.getActivity(AndroidContext.getContext()    , 0, sendIntent, 0);

        Intent actionIntent = new Intent(
                ApplicationClass.getInstance(), ChromeCustomTabReceiver.class);
        PendingIntent pi =
                PendingIntent.getBroadcast(ApplicationClass.getInstance(), 0, actionIntent, 0);

        customTabsIntent.setToolbarColor(ContextCompat.getColor(ApplicationClass.getInstance(), R.color.primary))
                .setActionButton(BitmapFactory.decodeResource(ApplicationClass.getInstance().getResources(), R.drawable.ic_share), "Share", pi)
                .setCloseButtonIcon(BitmapFactory.decodeResource(ApplicationClass.getInstance().getResources(), R.drawable.ic_back));

        return customTabsIntent.build();
    }
}