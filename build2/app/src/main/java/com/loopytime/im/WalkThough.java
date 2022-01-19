package com.loopytime.im;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;


public class WalkThough extends AppCompatActivity {
    View[] layouts = new View[6];
    boolean hasWhatsappOpen = false;
    int lastPos = 0;
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (layouts[0] == null)
                mHandler.postDelayed(mRunnable, 200);
            else
                showAnimationMain(layouts[0]);
        }
    };
    Handler mHandler = new Handler();
    BottomSheetBehavior sheetBehavior;

    public enum ModelObject {

        BLUE("Story", "Chat", "Profile", "You can post you story on Superchat Messenger in form of text, image and video. You can make your story as public or private. Public story will be visible to all im users and private will be visible  to your contacts only." +
                "You can view public story posted by other user in public tab of story page. If you don’t want to see public story then ignore it.", " You can chat with your friends and family and stay close to them. This Chat feature is similar to chat of other apps. You can send media attachment to your contacts. No any strange user can message you. Only your contact can send you a message.", "You can set your username which will be unique and 5 char long, set username different so no one can guess. Set you awesome photo as Profile pic with status which describes best you", Color.parseColor("#ff5862"), R.drawable.gu2, R.drawable.gu1, R.drawable.gu3),
        YELLOW("Global Search", "Chat Request", "Calls", "You can search any user in Superchat Messenger by his name and user name. You can make new friends via global search user. Global Search is available in Contact tab.",
                "To make new friends you need to send chat request to any user. If other user accept your chat request then you will become friends and you can communicate. Chat request feature is visible in Contact tab.",
                "You can make high quality audio and video calls with your contacts. Call history is visible in calls tab.", Color.parseColor("#ff9800"), R.drawable.gu4, R.drawable.gu5, R.drawable.gu6),
        GREEN("Contact", "Message", "Group and Channel", "Your address book will be automatically sync and your contact will be visible in contact tab.",
                "You can Like, Edit and Delete any message shared on Superchat Messenger.Yes, You can edit the last message you sent.",
                "You can create Groups and Channel with unlimited number of users. Yes, Unlimited . Share any type of file with all extension of bigger size. ", Color.parseColor("#23837f"), R.drawable.gu7, R.drawable.gu8, R.drawable.gu9),
        WHITE("Profile", "Privacy", "Notification", "You can set your username which will be unique and 5 char long, set username different so no one can guess. Set you awesome photo as Profile pic with status which describes best you",
                "Set your privacy for your account like last seen, status and profile pic. No account on Superchat Messenger shows mobile number as it is hidden.",
                "You will receive push notification for each and every message you receive. Different notification setting options for device like Xiomi, Oppo, Vivo and other Chines brand. Make sure you enable Superchat Messenger to run in background", Color.parseColor("#e74337"), R.drawable.gu10, R.drawable.gu10, R.drawable.gu11);

        private String title1, title2, title3, des1, des2, des3;
        int id1, id2, id3;
        private int color;

        ModelObject(String title1, String title2, String title3, String des1, String des2, String des3, int layoutResId, int id1, int id2, int id3) {
            this.title1 = title1;
            this.title2 = title2;
            this.title3 = title3;
            this.des1 = des1;
            this.des2 = des2;
            this.des3 = des3;
            color = layoutResId;
            this.id1 = id1;
            this.id2 = id2;
            this.id3 = id3;
        }
    }

    ViewPager pager;

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.sendEmptyMessage(0);
        mHandler.removeMessages(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_though);
        sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
//        getSupportActionBar().hide();
        pager = findViewById(R.id.pager);
        setStatus(Color.parseColor("#3a6fca"));
        pager.setOffscreenPageLimit(6);
        setUiPageViewController();
        pager.addOnPageChangeListener(new
                                              ViewPager.OnPageChangeListener() {
                                                  @Override
                                                  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                                                  }

                                                  @Override
                                                  public void onPageSelected(int position) {
                                                      new Handler().post(new Runnable() {
                                                          @Override
                                                          public void run() {
                                                              if (position == 0) {
                                                                  showAnimationMain(layouts[0]);

                                                              } else if (position == 5) {
                                                                  showAnimationInvite(layouts[5]);

                                                              } else {
                                                                  showAnimation(layouts[position]);

                                                              }
                                                              if (lastPos == 0) {
                                                                  HideViewsMain(layouts[lastPos]);
                                                              } else if (lastPos == 5) {
                                                                  HideViewsInvite(layouts[5]);

                                                              } else {
                                                                  hideViews(layouts[lastPos]);
                                                              }
                                                              lastPos = position;
                                                          }
                                                      });
                                                      setIndicatorAnimation(position);
                                                      setStatus(position == 0 || position == 5 ? Color.parseColor("#3a6fca") : ModelObject.values()[position - 1].color);

                                                  }

                                                  @Override
                                                  public void onPageScrollStateChanged(int state) {

                                                  }
                                              });
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return ModelObject.values().length + 2;
            }

            @Override
            public Object instantiateItem(ViewGroup collection, int position) {
                LayoutInflater inflater = LayoutInflater.from(WalkThough.this);
                ViewGroup layout = (ViewGroup) inflater.inflate(position == 0 ? R.layout.walkthrough_page1 : position == 5 ? R.layout.walkthrough_invite : R.layout.walkthrough_page2, collection, false);
                collection.addView(layout);
                layouts[position] = layout;
                if (position > 0 && position < 5) {
                    hideViews(layout);
                    setData(layout, position - 1);

                } else if (position == 0) {
                    HideViewsMain(layout);
                } else {
                    layout.findViewById(R.id.whatsApp).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hasWhatsappOpen = true;
                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.setPackage("com.whatsapp");

                            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_message));
                            shareIntent.setType("text/plain");

                            startActivity(Intent.createChooser(shareIntent, "Share to:"));
                        }
                    });

                    layout.findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);

                            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_message));

                            shareIntent.setType("text/plain");

                            startActivity(Intent.createChooser(shareIntent, "Share to:"));
                        }
                    });
                    HideViewsInvite(layout);
                }

                return layout;
            }

            @Override
            public void destroyItem(ViewGroup collection, int position, Object view) {
                collection.removeView((View) view);
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }
        });
        mHandler.post(mRunnable);
        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pager.getCurrentItem() < ModelObject.values().length + 1
                )
                    pager.setCurrentItem(pager.getCurrentItem() + 1, true);
                else {
                    if (hasWhatsappOpen) {

                        callActivity();
                        return;
                    }

                    new AlertDialog.Builder(WalkThough.this)
                            .setTitle("Share Invitation")
                            .setMessage("Please share invitation on Whatsapp first")
                            .setPositiveButton("share ", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    hasWhatsappOpen = true;
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.setPackage("com.whatsapp");

                                    shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_message));
                                    shareIntent.setType("text/plain");

                                    startActivity(Intent.createChooser(shareIntent, "Share to:"));
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                }
            }
        });
        findViewById(R.id.skip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                callActivity();
            }
        });
    }

    void setIndicatorAnimation(int position) {

        // for (int i = 0; i < 4; i++) {
        dots[lastPos].clearAnimation();
        dots[lastPos].setAnimation(null);
        //dots[i].setImageDrawable(ContextCompat.getDrawable(WalkThough.this, R.drawable.circle_selector));
        // }

        // dots[position].setImageDrawable(ContextCompat.getDrawable(WalkThough.this, R.drawable.selecteditem_dot));
        ScaleAnimation grow = new ScaleAnimation(1, 1.6f, 1, 1.6f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        grow.setDuration(400);
        grow.setFillAfter(true);

        dots[position].startAnimation(grow);
    }

    void showBottomSheet(String text, String title, int color, int resId) {
        View view = getLayoutInflater().inflate(R.layout.fragment_bottom_sheet_dialog, null);
        ((ImageView) view.findViewById(R.id.imv3)).setImageResource(resId);
        ((TextView) view.findViewById(R.id.tv3_title)).setText(title);
        ((TextView) view.findViewById(R.id.tv3)).setText(text);
        view.setBackgroundColor(color);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);
        dialog.show();
    }

    void hideViews(View root) {
        View lay1 = root.findViewById(R.id.lay1);
        View lay2 = root.findViewById(R.id.lay2);
        View lay3 = root.findViewById(R.id.lay3);
        View im1 = root.findViewById(R.id.imv1);
        View im2 = root.findViewById(R.id.imv2);
        View im3 = root.findViewById(R.id.imv3);

        lay1.setTranslationX(root.getWidth());
        lay1.setAlpha(0);
        im1.setTranslationX(root.getWidth());
        im1.setAlpha(0);
        lay3.setTranslationX(root.getWidth());
        lay3.setAlpha(0);
        im3.setTranslationX(root.getWidth());
        im3.setAlpha(0);

        lay2.setTranslationX(-root.getWidth());
        lay2.setAlpha(0);
        im2.setTranslationX(-root.getWidth());
        im2.setAlpha(0);
    }

    void setData(View layout, int position) {

        TextView title1 = layout.findViewById(R.id.tv1_title);
        TextView title2 = layout.findViewById(R.id.tv2_title);
        TextView title3 = layout.findViewById(R.id.tv3_title);
        TextView des1 = layout.findViewById(R.id.tv1);
        TextView des2 = layout.findViewById(R.id.tv2);
        TextView des3 = layout.findViewById(R.id.tv3);
        ImageView imv1 = layout.findViewById(R.id.imv1);
        ImageView imv2 = layout.findViewById(R.id.imv2);
        ImageView imv3 = layout.findViewById(R.id.imv3);
        imv1.setImageResource(ModelObject.values()[position].id1);
        imv2.setImageResource(ModelObject.values()[position].id2);
        imv3.setImageResource(ModelObject.values()[position].id3);
        title1.setText(ModelObject.values()[position].title1);
        title2.setText(ModelObject.values()[position].title2);
        title3.setText(ModelObject.values()[position].title3);
        des1.setText(ModelObject.values()[position].des1);
        des2.setText(ModelObject.values()[position].des2);
        des3.setText(ModelObject.values()[position].des3);
        layout.setBackgroundColor(ModelObject.values()[position].color);
        imv2.setBackgroundColor(ModelObject.values()[position].color);
        imv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBottomSheet(ModelObject.values()[position].des1, ModelObject.values()[position].title1, ModelObject.values()[position].color, ModelObject.values()[position].id1);
            }
        });
        imv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBottomSheet(ModelObject.values()[position].des2, ModelObject.values()[position].title2, ModelObject.values()[position].color, ModelObject.values()[position].id2);
            }
        });
        imv3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBottomSheet(ModelObject.values()[position].des3, ModelObject.values()[position].title3, ModelObject.values()[position].color, ModelObject.values()[position].id3);
            }
        });
        View lay1 = layout.findViewById(R.id.lay1);
        View lay2 = layout.findViewById(R.id.lay2);
        View lay3 = layout.findViewById(R.id.lay3);
        lay1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imv1.performClick();
            }
        });
        lay2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imv2.performClick();
            }
        });
        lay3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imv3.performClick();
            }
        });

    }

    long offset = 100, animTime = 1000;


    void showAnimation(View root) {
        View lay1 = root.findViewById(R.id.lay1);
        View lay2 = root.findViewById(R.id.lay2);
        View lay3 = root.findViewById(R.id.lay3);
        View im1 = root.findViewById(R.id.imv1);
        View im2 = root.findViewById(R.id.imv2);
        View im3 = root.findViewById(R.id.imv3);

        lay1.setTranslationX(root.getWidth());
        lay1.setAlpha(0);
        im1.setTranslationX(root.getWidth());
        im1.setAlpha(0);
        lay3.setTranslationX(root.getWidth());
        lay3.setAlpha(0);
        im3.setTranslationX(root.getWidth());
        im3.setAlpha(0);

        lay2.setTranslationX(-root.getWidth());
        lay2.setAlpha(0);
        im2.setTranslationX(-root.getWidth());
        im2.setAlpha(0);


        //ObjectAnimator.ofFloat(lay1, "translationX",0);
        final ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(lay1,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//

        anim.setInterpolator(new DecelerateInterpolator());

        final ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(im1,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//

        anim2.setInterpolator(new DecelerateInterpolator());

        final ObjectAnimator anim3 = ObjectAnimator.ofPropertyValuesHolder(im2,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//

        anim3.setInterpolator(new DecelerateInterpolator());

        final ObjectAnimator anim4 = ObjectAnimator.ofPropertyValuesHolder(lay2,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//


        anim4.setInterpolator(new DecelerateInterpolator());


        final ObjectAnimator anim5 = ObjectAnimator.ofPropertyValuesHolder(lay3,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//

        anim5.setInterpolator(new DecelerateInterpolator());

        final ObjectAnimator anim6 = ObjectAnimator.ofPropertyValuesHolder(im3,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//
        anim6.setInterpolator(new DecelerateInterpolator());


        //  anim.setDuration(animTime);
        anim.setStartDelay(offset);
        //anim2.setDuration(animTime);
        //anim3.setDuration(animTime);
        anim4.setStartDelay(offset);
        //anim4.setDuration(animTime);
        //anim5.setDuration(animTime);
        anim5.setStartDelay(offset);
        //anim6.setDuration(animTime);

      /*  anim.start();
        anim2.start();
        anim3.start();
        anim4.start();
        anim5.start();
        anim6.start()*/
        ;

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(animTime);
        animatorSet.playTogether(anim, anim2, anim3, anim4, anim5, anim6);
        animatorSet.start();


    }

    void HideViewsMain(View root) {
        View lay1 = root.findViewById(R.id.tv1);
        View lay2 = root.findViewById(R.id.tv_des);
        View im1 = root.findViewById(R.id.imv1);
        View lets = root.findViewById(R.id.lets);

        lay1.setTranslationX(root.getWidth());
        lay1.setAlpha(0);
        im1.setTranslationX(-root.getWidth());
        im1.setAlpha(0);
        lay2.setTranslationY(lay2.getWidth());
        lay2.setAlpha(0);
        lets.setAlpha(0);
        lets.setScaleY(0);
        lets.setScaleX(0);

    }

    void showAnimationMain(View root) {
        if (root == null) return;
        View lay1 = root.findViewById(R.id.tv1);
        View lay2 = root.findViewById(R.id.tv_des);
        View im1 = root.findViewById(R.id.imv1);
        View lets = root.findViewById(R.id.lets);

        lay1.setTranslationX(root.getWidth());
        lay1.setAlpha(0);
        im1.setTranslationX(-root.getWidth());
        im1.setAlpha(0);
        lay2.setTranslationY(lay2.getWidth());
        lay2.setAlpha(0);
        lets.setAlpha(0);
        lets.setScaleY(0);
        lets.setScaleX(0);

        //ObjectAnimator.ofFloat(lay1, "translationX",0);
        final ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(lay1,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//

        anim.setInterpolator(new DecelerateInterpolator());


        final ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(im1,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//

        anim2.setInterpolator(new DecelerateInterpolator());
        final ObjectAnimator anim3 = ObjectAnimator.ofPropertyValuesHolder(lay2,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//

        anim3.setInterpolator(new DecelerateInterpolator());

        final ObjectAnimator anim4 = ObjectAnimator.ofPropertyValuesHolder(lets,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//
        anim4.setStartDelay(animTime - 600);
        anim4.setDuration(animTime - 600);
        anim4.setInterpolator(new DecelerateInterpolator());

//anim.setStartDelay(offset);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(animTime - 200);
        animatorSet.playTogether(anim, anim2, anim3);
        animatorSet.start();
        anim4.start();


    }

    void HideViewsInvite(View root) {
        View lay1 = root.findViewById(R.id.tv1);
        View lay2 = root.findViewById(R.id.invite_title);
        View im1 = root.findViewById(R.id.tv_des);
        View lets = root.findViewById(R.id.lets);

        lay1.setTranslationX(root.getWidth());
        lay1.setAlpha(0);
        im1.setTranslationX(-root.getWidth());
        im1.setAlpha(0);
        lay2.setTranslationY(lay2.getWidth());
        lay2.setAlpha(0);
        lets.setAlpha(0);
        lets.setScaleY(0);
        lets.setScaleX(0);

    }

    void showAnimationInvite(View root) {
        if (root == null) return;
        View lay1 = root.findViewById(R.id.tv1);
        View lay2 = root.findViewById(R.id.invite_title);
        View im1 = root.findViewById(R.id.tv_des);
        View lets = root.findViewById(R.id.lets);

        lay1.setTranslationX(root.getWidth());
        lay1.setAlpha(0);
        im1.setTranslationX(-root.getWidth());
        im1.setAlpha(0);
        lay2.setTranslationY(lay2.getWidth());
        lay2.setAlpha(0);
        lets.setAlpha(0);
        lets.setScaleY(0);
        lets.setScaleX(0);

        //ObjectAnimator.ofFloat(lay1, "translationX",0);
        final ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(lay1,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//

        anim.setInterpolator(new DecelerateInterpolator());


        final ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(im1,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//

        anim2.setInterpolator(new DecelerateInterpolator());
        final ObjectAnimator anim3 = ObjectAnimator.ofPropertyValuesHolder(lay2,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//

        anim3.setInterpolator(new DecelerateInterpolator());

        final ObjectAnimator anim4 = ObjectAnimator.ofPropertyValuesHolder(lets,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1), // scaleX remains same
                PropertyValuesHolder.ofFloat(View.ALPHA, 1));
        ;//ObjectAnimator.ofFloat(lay1, "translationX",0);//
        anim4.setStartDelay(animTime - 600);
        anim4.setDuration(animTime - 600);
        anim4.setInterpolator(new DecelerateInterpolator());

//anim.setStartDelay(offset);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(animTime - 200);
        animatorSet.playTogether(anim, anim2, anim3);
        animatorSet.start();
        anim4.start();


    }

    void setStatus(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color);
        }
    }

    void setHtml(TextView tv, String txt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tv.setText(Html.fromHtml(txt, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tv.setText(Html.fromHtml(txt));
        }
    }


    void callActivity() {
        Intent i = new Intent(WalkThough.this, ProfileInfo.class);
        i.putExtra("from", "welcome");
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();

    }

    ImageView dots[];

    private void setUiPageViewController() {

        dots = new ImageView[6];

        for (int i = 0; i < 6; i++) {
            dots[i] = new ImageView(WalkThough.this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(WalkThough.this, R.drawable.circle_selector));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            int mar = (int) ApplicationClass.pxToDp(this, 4);
            params.setMargins(mar, mar, mar, mar);
            //   params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
            ((ViewGroup) findViewById(R.id.pager_indicator)).addView(dots[i], params);
        }
        setIndicatorAnimation(0);
        //dots[0].setImageDrawable(ContextCompat.getDrawable(WalkThough.this, R.drawable.selecteditem_dot));
    }
}
