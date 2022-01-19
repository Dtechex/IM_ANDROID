package com.loopytime.external.videotrimmer.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

/**
 * Created by HP on 11/7/2015.
 */
public class CircleFillView extends ImageView {


        private Paint mWhitePaint;
        private int mCanvasWidth;
        private int mCanvasHeight;
    private int color= Color.WHITE;

        public CircleFillView(Context context) {
            super(context);
            this.init();
        }

        public CircleFillView(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.init();
        }

        public CircleFillView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            this.init();
        }
    boolean useTheme=false;
public void update(int color,boolean useTheme){
    this.color=color;
    this.useTheme=useTheme;
    this.mWhitePaint.setColor(this.color);
    invalidate();

}
        private void init() {
            this.mWhitePaint = new Paint(1);
            this.mWhitePaint.setColor(this.color);
            this.mWhitePaint.setStyle(Paint.Style.FILL);
            Resources r = this.getResources();
            float strokePx = TypedValue.applyDimension(1, 2.0F, r.getDisplayMetrics());
          //  this.mWhitePaint.setStrokeWidth(strokePx);
        }

        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float centerX = (float)(this.mCanvasWidth / 2);
            float centerY = (float)(this.mCanvasHeight / 2);
            float radius = (useTheme?.7f:1)* Math.min(centerX, centerY);
            canvas.drawCircle(centerX, centerY, radius, this.mWhitePaint);
        }

        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            this.mCanvasWidth = w;
            this.mCanvasHeight = h;
        }
    }
