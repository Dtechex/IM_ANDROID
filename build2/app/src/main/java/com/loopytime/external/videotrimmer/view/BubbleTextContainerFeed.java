package com.loopytime.external.videotrimmer.view;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.text.BidiFormatter;

import com.loopytime.external.videotrimmer.utils.Screen;
import com.loopytime.im.R;


/**
 * Created by ex3ndr on 11.09.14.
 */
public class BubbleTextContainerFeed extends FrameLayout {
    public BubbleTextContainerFeed(Context context) {
        super(context);
        setClipToPadding(false);
    }

    public BubbleTextContainerFeed(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipToPadding(false);
    }

    public BubbleTextContainerFeed(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setClipToPadding(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Rect bounds = new Rect();
        Drawable background = getBackground();
        if (background != null) {
            background.getPadding(bounds);
        }

        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int maxW = MeasureSpec.getSize(widthMeasureSpec) - bounds.left - bounds.right;

        TextView messageView = (TextView) findViewById(R.id.tv_text);//getChildAt(0);
        TextView nameView = (TextView) findViewById(R.id.tv_name);//getChildAt(0);
        messageView.measure(MeasureSpec.makeMeasureSpec(maxW, wMode), heightMeasureSpec);
        nameView.measure(MeasureSpec.makeMeasureSpec(maxW, wMode), heightMeasureSpec);
        View timeView = findViewById(R.id.lay);//getChildAt(1);
        timeView.measure(MeasureSpec.makeMeasureSpec(maxW, wMode), heightMeasureSpec);

        Layout textLayout = messageView.getLayout();
        Layout textLayout2 = nameView.getLayout();

        int contentW = messageView.getMeasuredWidth();
        int contentW2 = nameView.getMeasuredWidth();
        int timeW = timeView.getMeasuredWidth();
        boolean isRtl = BidiFormatter.getInstance().isRtl(messageView.getText().toString());

        if (messageView.getLayout().getLineCount() < 5 && !isRtl) {
            contentW = 0;
            for (int i = 0; i < textLayout.getLineCount(); i++) {
                contentW = Math.max(contentW, (int) textLayout.getLineWidth(i));
            }
            contentW = Math.max(contentW, contentW2);
            for (int i = 0; i < textLayout2.getLineCount(); i++) {
                contentW = Math.max(contentW, (int) textLayout2.getLineWidth(i));
            }
        }

        int lastLineW = (int) textLayout.getLineWidth(textLayout.getLineCount() - 1);
        int lastLineW2 = (int) textLayout.getLineWidth(textLayout.getLineCount() - 1);
        lastLineW = Math.max(lastLineW,  lastLineW2);
        if (isRtl) {
            lastLineW = contentW;
        }

        int fullContentW, fullContentH;
        ((LayoutParams)messageView.getLayoutParams()).bottomMargin = 0;
        if (isRtl) {
            fullContentW = contentW;
            fullContentH = messageView.getMeasuredHeight() + timeView.getMeasuredHeight()+nameView.getMeasuredHeight();
        } else {
            if (lastLineW + timeW < contentW) {
                // Nothing to do
                fullContentW = contentW;
                fullContentH = messageView.getMeasuredHeight()+nameView.getMeasuredHeight();
            } else if (lastLineW + timeW < maxW) {
                fullContentW = lastLineW + timeW;
                fullContentH = messageView.getMeasuredHeight()+nameView.getMeasuredHeight();
            } else {
                fullContentW = contentW;

                fullContentH = messageView.getMeasuredHeight() + timeView.getMeasuredHeight()+nameView.getMeasuredHeight();
                ((LayoutParams)messageView.getLayoutParams()).bottomMargin = timeView.getMeasuredHeight();
            }
        }

        setMeasuredDimension(fullContentW + bounds.left + bounds.right+ Screen.dp(8), fullContentH + bounds.top + bounds.bottom+Screen.dp(4));
    }
}
