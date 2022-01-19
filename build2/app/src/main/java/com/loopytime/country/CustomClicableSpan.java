package com.loopytime.country;

import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

public class CustomClicableSpan extends ClickableSpan {

    protected boolean hideUrlStyle;
    private SpanClickListener clickListener;

    public CustomClicableSpan(SpanClickListener clickListener) {
        this.clickListener = clickListener;
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
        clickListener.onClick();
    }

    public interface SpanClickListener {
        void onClick();
    }
}