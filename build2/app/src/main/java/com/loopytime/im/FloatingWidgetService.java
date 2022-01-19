package com.loopytime.im;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopytime.external.videotrimmer.utils.Screen;
import com.loopytime.helper.MaterialColor;
import com.loopytime.helper.MaterialColors;


/**
 * Created by anupamchugh on 01/08/17.
 */

public class FloatingWidgetService extends Service {

    private final IBinder myBinder = new MyLocalBinder();
    private WindowManager mWindowManager;
    public View mOverlayView;
    int mWidth;
    FloatingActionButton counterFab;
    boolean activity_background;

    interface FabClickListener {
        void OnFabClicked();
    }

    FabClickListener fabClickListener;

    public void setOnFabClickListener(FabClickListener mFabClickListener) {
        fabClickListener = mFabClickListener;
    }

    public class MyLocalBinder extends Binder {
        FloatingWidgetService getService() {
            return FloatingWidgetService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }



    @Override
    public void onCreate() {
        super.onCreate();

        setTheme(R.style.ChatActivityTheme);


        if (mOverlayView == null) {

            mOverlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);

            int layoutType;
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            }
            MaterialColor color = MaterialColors.CONVERSATION_PALETTE.get(getSharedPreferences("wall", Context.MODE_PRIVATE).getInt(MaterialColors.THEME, 0));
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            //Specify the view position
            params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
            params.x = Screen.getWidth()-Screen.dp(64);
            params.y = Screen.getHeight()/2;//-Screen.dp(146);


            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mWindowManager.addView(mOverlayView, params);

            Display display = mWindowManager.getDefaultDisplay();
            final Point size = new Point();
            display.getSize(size);

            counterFab = (FloatingActionButton) mOverlayView.findViewById(R.id.fabHead);
            //counterFab.setCount(1);

/*counterFab.setRippleColor(color.toConversationColor(this));
            counterFab.setBackgroundTintList(new ColorStateList(new int[][]{
                    new int[]{android.R.attr.state_pressed},
                    new int[]{color.toConversationPColor(this)},


            }, new int[]{
                    color.toConversationPColor(this),
                    color.toConversationColor(this),
            }));*/
            final RelativeLayout layout = (RelativeLayout) mOverlayView.findViewById(R.id.layout);
            ViewTreeObserver vto = layout.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    int width = layout.getMeasuredWidth();

                    //To get the accurate middle of the screen we subtract the width of the floating widget.
                    mWidth = size.x - width;

                }
            });

            counterFab.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            System.out.println("fggfgg clickd");
                            //remember the initial position.
                            initialX = params.x;
                            initialY = params.y;


                            //get the touch location
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();


                            return true;
                        case MotionEvent.ACTION_UP:

                            //Only start the activity if the application is in bg_default_pattern. Pass the current badge_count to the activity
                           // if (activity_background) {

                                float xDiff = event.getRawX() - initialTouchX;
                                float yDiff = event.getRawY() - initialTouchY;

                                if ((Math.abs(xDiff) < 5) && (Math.abs(yDiff) < 5)) {
                                    // Intent intent = new Intent(FloatingWidgetService.this, MainActivity.class);
                                    //     intent.putExtra("badge_count", counterFab.getCount());
                                    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    // startActivity(intent);

                                    //close the service and remove the fab view
                                    // stopSelf();
                                    System.out.println("fggfgg clickd");
                                    fabClickListener.OnFabClicked();
                                }

                            //}
                            //Logic to auto-position the widget based on where it is positioned currently w.r.t middle of the screen.
                            int middle = mWidth / 2;
                            float nearestXWall = params.x >= middle ? mWidth : 0;
                            params.x = (int) nearestXWall;


                            mWindowManager.updateViewLayout(mOverlayView, params);


                            return true;
                        case MotionEvent.ACTION_MOVE:


                            int xDiff2 = Math.round(event.getRawX() - initialTouchX);
                            int yDiff2 = Math.round(event.getRawY() - initialTouchY);


                            //Calculate the X and Y coordinates of the view.
                            params.x = initialX + xDiff2;
                            params.y = initialY + yDiff2;

                            //Update the layout with new X & Y coordinates
                            mWindowManager.updateViewLayout(mOverlayView, params);


                            return true;
                    }
                    return false;
                }
            });
        } else {

            //counterFab.increase();

        }



    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOverlayView != null)
            mWindowManager.removeView(mOverlayView);
    }

}
