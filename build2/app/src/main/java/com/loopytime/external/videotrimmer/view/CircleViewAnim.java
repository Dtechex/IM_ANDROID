package com.loopytime.external.videotrimmer.view;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Created by jack on 29/12/2016.
 */

public class CircleViewAnim  extends View {
        private static final int START_COLOR = 0xFFFF5722;
        private static final int END_COLOR = 0xFFFFC107;

        @NonNull
        private ArgbEvaluator argbEvaluator = new ArgbEvaluator();

        @NonNull
        private Paint circlePaint = new Paint();
        @NonNull
        private Paint maskPaint = new Paint();

        private Bitmap tempBitmap;
        private Canvas tempCanvas;

        private float outerCircleRadiusProgress = 0f;
        private float innerCircleRadiusProgress = 0f;

        private int maxCircleSize;

        public CircleViewAnim(Context context) {
            super(context);
            init();
        }

        public CircleViewAnim(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public CircleViewAnim(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public CircleViewAnim(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            init();
        }

        private void init() {
            circlePaint.setStyle(Paint.Style.FILL);
            maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            maxCircleSize = w / 2;
            tempBitmap = Bitmap.createBitmap(getWidth(), getWidth(), Bitmap.Config.ARGB_8888);
            tempCanvas = new Canvas(tempBitmap);
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            tempCanvas.drawColor(0xffffff, PorterDuff.Mode.CLEAR);
            tempCanvas.drawCircle(getWidth() / 2, getHeight() / 2, outerCircleRadiusProgress * maxCircleSize, circlePaint);
            tempCanvas.drawCircle(getWidth() / 2, getHeight() / 2, innerCircleRadiusProgress * maxCircleSize, maskPaint);
            canvas.drawBitmap(tempBitmap, 0, 0, null);
        }

        public void setInnerCircleRadiusProgress(float innerCircleRadiusProgress) {
            this.innerCircleRadiusProgress = innerCircleRadiusProgress;
            postInvalidate();
        }

        public float getInnerCircleRadiusProgress() {
            return innerCircleRadiusProgress;
        }

        public void setOuterCircleRadiusProgress(float outerCircleRadiusProgress) {
            this.outerCircleRadiusProgress = outerCircleRadiusProgress;
            updateCircleColor();
            postInvalidate();
        }

        private void updateCircleColor() {
            float colorProgress = (float) UtilsAnim.clamp(outerCircleRadiusProgress, 0.5, 1);
            colorProgress = (float) UtilsAnim.mapValueFromRangeToRange(colorProgress, 0.5f, 1f, 0f, 1f);
            this.circlePaint.setColor((Integer) argbEvaluator.evaluate(colorProgress, START_COLOR, END_COLOR));
        }

        public float getOuterCircleRadiusProgress() {
            return outerCircleRadiusProgress;
        }

        public static final Property<CircleViewAnim, Float> INNER_CIRCLE_RADIUS_PROGRESS =
                new Property<CircleViewAnim, Float>(Float.class, "innerCircleRadiusProgress") {
                    @Override
                    public Float get(@NonNull CircleViewAnim object) {
                        return object.getInnerCircleRadiusProgress();
                    }

                    @Override
                    public void set(@NonNull CircleViewAnim object, Float value) {
                        object.setInnerCircleRadiusProgress(value);
                    }
                };

        public static final Property<CircleViewAnim, Float> OUTER_CIRCLE_RADIUS_PROGRESS =
                new Property<CircleViewAnim, Float>(Float.class, "outerCircleRadiusProgress") {
                    @Override
                    public Float get(@NonNull CircleViewAnim object) {
                        return object.getOuterCircleRadiusProgress();
                    }

                    @Override
                    public void set(@NonNull CircleViewAnim object, Float value) {
                        object.setOuterCircleRadiusProgress(value);
                    }
                };
    }
