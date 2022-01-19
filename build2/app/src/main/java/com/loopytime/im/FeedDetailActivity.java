package com.loopytime.im;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.loopytime.external.videotrimmer.utils.Screen;
import com.loopytime.external.videotrimmer.view.CircleViewAnim;
import com.loopytime.external.videotrimmer.view.DotsView;
import com.loopytime.external.videotrimmer.view.VerticalViewPagerFeed;
import com.loopytime.helper.ExecuteServices;
import com.loopytime.helper.MaterialColor;
import com.loopytime.helper.MaterialColors;
import com.loopytime.helper.Utils;
import com.loopytime.im.status.FeedCommentObject;
import com.loopytime.im.status.FeedObject;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.loopytime.helper.Utils.feedMediaExist;
import static com.loopytime.im.WhatStatusFragmentFeedProfile.formatDate;


public class FeedDetailActivity extends AppCompatActivity implements CommentsAdapter.OnCommentListener {
    Calendar c = Calendar.getInstance();
    int insertedId = 0;
    int lastSelectdPos = -1;
    TextView uName, rpName, rpText;
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    TextView feedTxt, feedTxtCount, textView4, anchorText;
    View overlay, rplyLay;
    ImageView feedImage;
    int itemInserted = 0;
    CircleImageView avatarView;
    private EditText aboutEdit;
    RecyclerView listLikes, commentList;
    public static ArrayList<String> noLoadSubComment = new ArrayList<String>();

    FeedCommentObject objModified = null;
    //MaterialColor color;
    DotsView vDotsView;
    ArrayList<FeedCommentObject> feedCommentObjectList = new ArrayList<>();
    CircleViewAnim vCircle;
    ImageView ivStar;
    View bottomBar;
    ImageView emoji;
    ArrayList<String> listData = new ArrayList<>();
    HashMap<String, ArrayList<FeedCommentObject>> map = new HashMap<>();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int reqCode = intent.getIntExtra("reqCode", -1);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
      //  MyFirebaseMessagingService.remove(intent.getStringExtra("id"));
        notificationManager.cancel(reqCode);
        Intent intent1 = new Intent(this, FeedDetailActivity.class);
        intent1.putExtra("feed", (FeedObject) intent.getSerializableExtra("feed"));
        startActivity(intent1);

    finish();
}

MaterialColor color;
    void setColor(){
    color = MaterialColors.CONVERSATION_PALETTE.get(this.getSharedPreferences("wall", Context.MODE_PRIVATE).getInt(MaterialColors.THEME, 0));

        ((CollapsingToolbarLayout) findViewById(R.id.toolbar_layout)).setContentScrimColor((color.toActionBarColor(this)));
       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color.toStatusBarColor(this));
        }*/
    }

    ViewGroup.LayoutParams layoutParams;
    NestedScrollView nsv;
    View expandedImageView;
    View thumbView = null;
    VerticalViewPagerFeed viewPager;
    //
    // FeedDisplayFragment fragmentDisplay;
    TextView show_next, show_prev;
    ProgressDialog pd;

    public void onHideFrag() {
        if (expandedImageView.getVisibility() == View.VISIBLE) {
            hideExpendedView();
        }
    }

    Handler mHandler = new Handler();

    void setUpViewPager() {
        setLikesDatList();
        setCommentList();
        feedDetailPagerAdapter = new FeedDetailPagerAdapter(getSupportFragmentManager());
        viewPager = new VerticalViewPagerFeed(this);
        viewPager.setId(R.id.pager);
        ((FrameLayout) findViewById(R.id.pagerContainer)).addView(viewPager);
        for (int i = 0; i < PostFragment.listData.size(); i++) {
            feedDetailPagerAdapter.addFrag(new FeedDisplayFragment());
        }
        viewPager.setAdapter(feedDetailPagerAdapter);

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                mHandler.sendEmptyMessage(0);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (lastSelectdPos > 0 )
                            ((FeedDisplayFragment) feedDetailPagerAdapter.getItem(lastSelectdPos)).releasePlayer();
                        lastSelectdPos = i;
                        changeData(i);
                    }
                }, 500);
                if (!stopLoadMore && loadMore
                        && i >= PostFragment.listData.size() - 3) {
                    loadMore = false;
                    setDataMore();
                }

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                viewPager.setCurrentItem(getIntent().getIntExtra("position", 0));
                if (getIntent().getIntExtra("position", 0) == 0) {
                    changeData(0);
                }
            }
        }, 300);
    }

    public void disableWakeLock() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    protected PowerManager powerManager;
    protected PowerManager.WakeLock wakeLock;
    protected int field = 0x00000020;

    public void enableWakeLock() {
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(field, getLocalClassName());

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    FeedDetailPagerAdapter feedDetailPagerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        clr = getResources().getIntArray(R.array.clr);

        setContentView(R.layout.activity_feed_detail);
        int reqCode = getIntent().getIntExtra("reqCode", -1);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
  //      MyFirebaseMessagingService.remove(getIntent().getStringExtra("id"));
        notificationManager.cancel(reqCode);
        ((FrameLayout) findViewById(R.id.pagerContainer)).removeView(viewPager);
        noLoadSubComment.clear();
        pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);
        pd.setCancelable(false);
        pd.setMessage("Loading comments...");
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        expandedImageView = (View) findViewById(
                R.id.pagerContainer);
        setColor();
        rplyLay = findViewById(R.id.rplyLay);

        bottomBar = findViewById(R.id.editLay);
        layoutParams = bottomBar.getLayoutParams();
        //rplyLay = findViewById(R.id.rplyLay);
        commentList = findViewById(R.id.comments);
        vDotsView = (DotsView) findViewById(R.id.vDotsView);
        show_next = findViewById(R.id.show_next);
        show_prev = findViewById(R.id.show_previous);
        setUpViewPager();
        findViewById(R.id.prev_shimmer).findViewById(R.id.div).setVisibility(View.GONE);
        findViewById(R.id.next_shimmer).findViewById(R.id.div).setVisibility(View.GONE);
        show_prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.prev_shimmer).setVisibility(View.VISIBLE);
                show_prev.setVisibility(View.GONE);
                setDataMore(false);
            }
        });
        show_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.next_shimmer).setVisibility(View.VISIBLE);
                show_next.setVisibility(View.GONE);
                setDataMore(true);
            }
        });
        vCircle = (CircleViewAnim) findViewById(R.id.vCircle);
        nsv = findViewById(R.id.nsv);
        ivStar = (ImageView) findViewById(R.id.ivStar);
        findViewById(R.id.backPress).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        aboutEdit = (EditText) findViewById(R.id.editText);
        //aboutEdit.setHintTextColor(style.getTextHintColor());
        //aboutEdit.setTextColor(style.getTextPrimaryColor());
        anchorText = findViewById(R.id.anchorText);
        rpName = findViewById(R.id.rpName);
        rpText = findViewById(R.id.rpText);
        aboutEdit.setHint("your comment...");
        findViewById(R.id.sizeImageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                objComment = null;
                toggleProductDescriptionHeight(false, rplyLay.getMeasuredHeight());
            }
        });
        uName = findViewById(R.id.uName);
        overlay = findViewById(R.id.overlay_nm);
        textView4 = findViewById(R.id.textView4);
        Drawable dr = AppCompatResources.getDrawable(this, R.drawable.ic_insert_emoticon_black_24dp);
        DrawableCompat.setTint(dr, Color.parseColor("#8a000000"));
        findViewById(R.id.react).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FeedDetailActivity.this, FeedLikes.class);
                intent.putExtra("post_id", feed._id);
                startActivity(intent);
            }
        });

        findViewById(R.id.download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DownloadFileFromURL().execute(feed.media);
            }
        });
        textView4.setCompoundDrawablesWithIntrinsicBounds(dr, null, null, null);
        Drawable dr2 = ContextCompat.getDrawable(this, R.drawable.ic_textsms_white_18dp);//DrawableCompat.getDrawable(getActivity(),R.drawable.ic_chat_black_24dp);
        dr2 = DrawableCompat.wrap(dr2);
        //dr.mutate().setColorFilter(Color.parseColor("#8a000000"),PorterDuff.Mode.SRC_IN);
        DrawableCompat.setTint(dr2, Color.parseColor("#8a000000"));
        ((TextView) findViewById(R.id.textView4)).setCompoundDrawablesWithIntrinsicBounds(dr2, null, null, null);
        ;

        feedImage = findViewById(R.id.img);
        avatarView = (CircleImageView) findViewById(R.id.avatarView);
        feedTxt = findViewById(R.id.text1);
        feedTxtCount = findViewById(R.id.textView3);
        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(aboutEdit.getText())) {

                    return;
                }
                if (objComment != null) {
                    int id = Integer.parseInt(objComment.parent_id);
                    id = id != -1 ? id : Integer.parseInt(objComment._id);
                    sendComment(id);
                } else
                    sendComment(-1);
            }
        });

        feedImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thumbView = feedImage;
                zoomImageFromThumb();
            }
        });

        feedTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thumbView = feedTxt;
                zoomImageFromThumb();
            }
        });

    }

    void changeData(int position) {
        System.out.println("hjhjhjs " + position);
        if(PostFragment.listData.size()==0)return;
        feed = PostFragment.listData.get(position);
        ((FeedDisplayFragment) feedDetailPagerAdapter.getItem(position)).changeMedia(feed);


        feedCommentObjectList.clear();
        if (commentList.getAdapter() != null)
            commentList.getAdapter().notifyDataSetChanged();
        commentList.setAdapter(new CommentsAdapter(this, feedCommentObjectList, false, false));
        setUpFeed();
        setData();
    }

    void setCommentList() {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;// cbPredictive.isChecked();
            }
        };
        //ViewCompat.setNestedScrollingEnabled(commentList,false);
        commentList.setLayoutManager(mLayoutManager);
        commentList.setNestedScrollingEnabled(false);

    }

    FeedObject feed;
    //View fragment;

    void setUpFeed() {
        //   fragment = findViewById(R.id.fragment);
        // fragment.setVisibility(View.INVISIBLE);
        //   fragmentDisplay = new FeedDisplayFragment();
        if ((FeedObject) getIntent().getSerializableExtra("feed") != null) {
            PostFragment.listData.add((FeedObject) getIntent().getSerializableExtra("feed"));
        }
        // feed = (FeedObject) getIntent().getSerializableExtra("feed"); //listData.get(position);
        findViewById(R.id.download).setVisibility(View.GONE);
        if (!feed.uid.equalsIgnoreCase(GetSet.getUserId())) {
            findViewById(R.id.download).setVisibility(View.GONE);
            findViewById(R.id.delete).setVisibility(View.GONE);

        }

        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(FeedDetailActivity.this).setTitle("Delete Feed?")
                        .setMessage("Really want to delete this feed?")
                        .setPositiveButton(R.string.alert_delete_chat_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteFeed();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            }
        });

        expandedImageView.post(new Runnable() {
            @Override
            public void run() {
                if (getIntent().getBooleanExtra("showComment", false)) {
                    expandedImageView.setVisibility(View.GONE);
                    return;
                }
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                if (!TextUtils.isEmpty(feed.text1) && !feed.text1.equalsIgnoreCase("null")) {
                    thumbView = feedTxt;
                } else
                    thumbView = feedImage;
                //zoomImageFromThumb();
            }
        });


        try {

            c.setTimeInMillis(format.parse(feed.created_at).getTime() + Calendar.getInstance().get(Calendar.ZONE_OFFSET));
            feedTxtCount.setText(formatDate(c, false));
        } catch (ParseException e) {
            feedTxtCount.setText(feed.created_at);
        }

            //avatarView.bind(null, feed.uname, Integer.parseInt(feed.uid));
showAvatar(feed.upic,avatarView);
        overlay.setVisibility(View.GONE);
        feedTxt.setVisibility(View.GONE);
        feedImage.setVisibility(View.GONE);
        uName.setText(feed.uname);


        textView4.setText(feed.reaction_count + " reaction & " + feed.comment_count + " comments");
        if (!TextUtils.isEmpty(feed.text1) && !feed.text1.equalsIgnoreCase("null")) {
            JSONObject jsonObject;
            feedTxt.setVisibility(View.VISIBLE);
            try {
                jsonObject = new JSONObject(feed.text1);


                feedTxt.setText(jsonObject.getString("txt"));
            } catch (JSONException e) {
                return;
            }

        } else if (feed.media.startsWith("vid_")) {
            feedImage.setVisibility(View.VISIBLE);
            overlay.setVisibility(View.VISIBLE);
            if (feedMediaExist(feed.media)) {
                RequestOptions options = new RequestOptions().frame(2000);
                Glide.with(FeedDetailActivity.this).asBitmap().
                        load(Utils.getFeedUrl(feed.media)).apply(options).
                        into(feedImage);


            } else
                Glide.with(this).
                        load(Uri.parse(Constants.NODE_URL+"pic_st/" + feed.media + ".png"
                        )).into(feedImage);

        } else {
            feedImage.setVisibility(View.VISIBLE);
            if (feedMediaExist(feed.media)) {
                Glide.with(this).
                        load(Utils.getFeedUrl(feed.media)).into(feedImage);
            } else
                Glide.with(this).
                        load(Uri.parse(Constants.NODE_URL+"pic_st/" + feed.media
                        )).into(feedImage);

        }
        anchorText.setVisibility(TextUtils.isEmpty(feed.text2) || feed.text2.equalsIgnoreCase("null") ? View.GONE : View.VISIBLE);
        if (!TextUtils.isEmpty(feed.text2) && !feed.text2.equalsIgnoreCase("null"))
            anchorText.setText(feed.text2);

    }


    void showAvatar(String url, ImageView avatarView) {
        Glide.with(this).applyDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.temp)).load(url).
                into(avatarView);
    }
    int[] arrLikes = {R.drawable.ic_like, R.drawable.ic_haha, R.drawable.ic_love, R.drawable.ic_sad, R.drawable.ic_wow, R.drawable.ic_angry, R.drawable.ic_ya};

    void setLikesDatList() {
        listLikes = findViewById(R.id.listLikes);
        listLikes.hasFixedSize();
        listLikes.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        listLikes.setAdapter(new RecyclerView.Adapter<ViewHolderLikes>() {
            @NonNull
            @Override
            public ViewHolderLikes onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                return new ViewHolderLikes(LayoutInflater.from(FeedDetailActivity.this).inflate(R.layout.item_likesmedium, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolderLikes holder, int position) {
                holder.imv.setImageResource(arrLikes[position]);
            }

            @Override
            public int getItemCount() {
                return arrLikes.length;
            }
        });
    }

    class ViewHolderLikes extends RecyclerView.ViewHolder {
        ImageView imv;

        public ViewHolderLikes(View itemView) {
            super(itemView);
            imv = (ImageView) itemView.findViewById(R.id.ic_img);
            //  imv.getLayoutParams().height = Screen.dp(30);
            //imv.getLayoutParams().width = Screen.dp(30);
            imv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //likeAnimation( getAdapterPosition());
                    likeAnimation(getAdapterPosition());
                }
            });
        }
    }

    void likeAnimation(int pos) {
        updateStatusReaction("" + pos, null);

        ivStar.setImageResource(arrLikes[pos]);
        vCircle.setInnerCircleRadiusProgress(0);
        vCircle.setOuterCircleRadiusProgress(0);
        vDotsView.setCurrentProgress(0);

        AnimatorSet animatorSet = new AnimatorSet();
 /*       ObjectAnimator starScaleYAnimator = ObjectAnimator.ofFloat(ivStar, ImageView.SCALE_Y, 0.2f, 1f);
        starScaleYAnimator.setDuration(350);
        starScaleYAnimator.setStartDelay(250);
      //  starScaleYAnimator.setInterpolator(new OvershootInterpolator(4));

        ObjectAnimator starScaleXAnimator = ObjectAnimator.ofFloat(ivStar, ImageView.SCALE_X, 0.2f, 1f);
        starScaleXAnimator.setDuration(350);
        starScaleXAnimator.setStartDelay(250);*/
        //starScaleXAnimator.setInterpolator(new OvershootInterpolator(4));


        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("scaleX", .0f, 1.5f, 1, 1.5f, 0);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("scaleY", .0f, 1.5f, 1, 1.5f, 0);
        ObjectAnimator starScaleYAnimator = ObjectAnimator.ofPropertyValuesHolder(ivStar, pvhX, pvhY, PropertyValuesHolder.ofFloat(View.ALPHA.getName(), 0.2f, 1f, 1, 0.2f));

        starScaleYAnimator.setDuration(1500);
        //starScaleYAnimator.setRepeatMode(ValueAnimator.REVERSE);
        //starScaleYAnimator.setRepeatCount(1);

        ObjectAnimator outerCircleAnimator = ObjectAnimator.ofFloat(vCircle, CircleViewAnim.OUTER_CIRCLE_RADIUS_PROGRESS, 0.1f, 1f);
        outerCircleAnimator.setDuration(250);
        outerCircleAnimator.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator innerCircleAnimator = ObjectAnimator.ofFloat(vCircle, CircleViewAnim.INNER_CIRCLE_RADIUS_PROGRESS, 0.1f, 1f);
        innerCircleAnimator.setDuration(200);
        innerCircleAnimator.setStartDelay(200);
        innerCircleAnimator.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator dotsAnimator = ObjectAnimator.ofFloat(vDotsView, DotsView.DOTS_PROGRESS, 0, 1f);
        dotsAnimator.setDuration(900);
        dotsAnimator.setStartDelay(50);
        dotsAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.playTogether(
                outerCircleAnimator,
                innerCircleAnimator,
                starScaleYAnimator,
                dotsAnimator
        );
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                //    super.onAnimationStart(animation);
                ivStar.setVisibility(View.VISIBLE);


            }

            @Override
                public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                ivStar.setVisibility(View.GONE);

                new Handler().post(new Runnable() {
                    @Override
                    public void run() {


                        //     addToFav();
                    }
                });
            }

            @Override
            public void onAnimationCancel(Animator animation) {
               /* vCircle.setInnerCircleRadiusProgress(0);
                vCircle.setOuterCircleRadiusProgress(0);
                vDotsView.setCurrentProgress(0);
                ivStar.setScaleX(1);
                ivStar.setScaleY(1);
                ivStar.setAlpha(1f);*/
                ivStar.setVisibility(View.GONE);
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {

                        //addToFav();
                    }
                });
            }
        });
        animatorSet.start();
    }

    private int findTopRelativeToParent(ViewGroup parent, View child) {

        int top = child.getTop();

        View childDirectParent = ((View) child.getParent());
        boolean isDirectChild = (childDirectParent.getId() == parent.getId());

        while (!isDirectChild) {
            top += childDirectParent.getTop();
            childDirectParent = ((View) childDirectParent.getParent());
            isDirectChild = (childDirectParent.getId() == parent.getId());
        }

        return top;

    }

    void sendComment(int pid) {
        ProgressDialog pd = ProgressDialog.show(this, "", "Wait", true, false);
        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(aboutEdit.getWindowToken(), 0);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("pid", String.valueOf(pid))
                .addFormDataPart("uid", GetSet.getUserId())
                .addFormDataPart("post_id", feed._id)
                .addFormDataPart("comment", aboutEdit.getText().toString())
                .addFormDataPart("uname", GetSet.getUserName())
                .addFormDataPart("uPhone",GetSet.getphonenumber())
                .addFormDataPart("upic", GetSet.getImageUrl()==null?"":GetSet.getImageUrl())
                .build();
        new ExecuteServices().executePost(Constants.NODE_URL+"set_comment_hiddy", new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("response ex "+response);
                        findViewById(R.id.noComments).setVisibility(View.GONE);
                        pd.dismiss();
                        try {
                            JSONObject pr = new JSONObject(response);
                            if (pr.getBoolean("success")) {

                                JSONObject job = pr.getJSONObject("data");
                                if (objComment != null) {
                                    feedCommentObjectList.add(insertedId, new FeedCommentObject(job.getString("comment"), job.getString("post_id"), job.getString("pid"), job.getString("created_at"), job.getString("uname"), job.getString("upic"), job.getString("uphone"), job.getString("id"), job.getString("uid")));

                                    objModified = feedCommentObjectList.get(insertedId);
                                    feed.feedComment = objModified;
                                    feed.comment_count = String.valueOf(Integer.parseInt(feed.comment_count) + 1);
                                    textView4.setText(feed.reaction_count + " reaction & " + feed.comment_count + " comments");
                                    //                         setResult(false, false);
                                    PostFragment.listData.set(viewPager.getCurrentItem(), feed);
                                    commentList.getAdapter().notifyItemInserted(insertedId);
                                    commentList.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            nsv.scrollTo(0, findTopRelativeToParent(nsv, commentList.getChildAt(insertedId)) - (insertedId == feedCommentObjectList.size() - 1 ? 0 : Screen.dp(40)));
                                        }
                                    });
                                } else {
                                    feedCommentObjectList.add(new FeedCommentObject(job.getString("comment"), job.getString("post_id"), job.getString("pid"), job.getString("created_at"), job.getString("uname"), job.getString("upic"), job.getString("uphone"), job.getString("id"), job.getString("uid")));
                                    objModified = feedCommentObjectList.get(feedCommentObjectList.size() - 1);
                                    feed.feedComment = objModified;
                                    feed.comment_count = String.valueOf(Integer.parseInt(feed.comment_count) + 1);
                                    textView4.setText(feed.reaction_count + " reaction & " + feed.comment_count + " comments");
                                    PostFragment.listData.set(viewPager.getCurrentItem(), feed);
                                    commentList.getAdapter().notifyItemInserted(feedCommentObjectList.size());
                                    ++itemInserted;
                                    commentList.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            nsv.scrollTo(0, findTopRelativeToParent(nsv, commentList.getChildAt(feedCommentObjectList.size() - 1)));
                                            //     setResult(false, false);
                                        }
                                    });

                                }

                                aboutEdit.setText("");
                                objComment = null;
                                toggleProductDescriptionHeight(false, rplyLay.getMeasuredHeight());


                                rpName.setText("");
                                rpText.setText("");

                            } else {
                                if (pr.has("blocked") && pr.getInt("blocked") > 0) {
                                    showBlockAlert();
                                }

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                });
            }

            @Override
            public void onServiceExecutedFailed(String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                        //   Toast.makeText(StatusUploadActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, requestBody);
    }

    public void setData() {
        show_next.setVisibility(View.GONE);
        show_prev.setVisibility(View.GONE);
        noLoadSubComment.clear();

        listData.clear();

        if (feed.feedComment == null) {
            findViewById(R.id.noComments).setVisibility(View.VISIBLE);
            commentList.setAdapter(new CommentsAdapter(FeedDetailActivity.this, feedCommentObjectList, true, feed.uid.equalsIgnoreCase(GetSet.getUserId())));
            return;
        }
        System.out.println("dfghjk " + viewPager.getCurrentItem() + " " + (feed.feedComment == null));
        if (TextUtils.isEmpty(feed.feedComment._id) || feed.feedComment._id.equalsIgnoreCase("null")) {
            commentList.setAdapter(new CommentsAdapter(FeedDetailActivity.this, feedCommentObjectList, true, feed.uid.equalsIgnoreCase(GetSet.getUserId())));
            findViewById(R.id.noComments).setVisibility(View.VISIBLE);
            return;
        }
        System.out.println("dfghjkn " + viewPager.getCurrentItem() + " " + (feed.feedComment == null));
        String cmtId = feed.feedComment.parent_id;
        if (!cmtId.equalsIgnoreCase("null") && Integer.parseInt(cmtId) == -1) {
            cmtId = feed.feedComment._id;
        }
        new ExecuteServices().execute(Constants.NODE_URL_GET+"get_feed_comment_now_hiddy/" + feed._id + "/" + cmtId, new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                                if (commentList.getAdapter() != null)
                                    commentList.getAdapter().notifyDataSetChanged();

                                try {
                                    listData.clear();
                                    map.clear();
                                    feedCommentObjectList.clear();
                                    JSONObject fjob = new JSONObject(response);
                                    JSONArray jAr = fjob.getJSONArray("data");
                                    for (int i = 0; i < jAr.length(); i++) {
                                        JSONObject job = jAr.getJSONObject(i);
                                        if (!job.getString("post_id").equalsIgnoreCase(feed._id)) {
                                            feedCommentObjectList.clear();
                                            return ;
                                        }
                                        if (job.getString("pid").equalsIgnoreCase("-1") && !listData.contains(job.getString("id"))) {
                                            listData.add(job.getString("id"));

                                            map.put(job.getString("id"), new ArrayList<FeedCommentObject>());

                                            map.get(job.getString("id")).add(new FeedCommentObject(job.getString("comment"), job.getString("post_id"), job.getString("pid"), job.getString("created_at"), job.getString("uname"), job.getString("upic"), job.getString("uphone"), job.getString("id"), job.getString("uid")));

                                        } else {
                                            if (!map.containsKey(job.getString("pid")) || map.get(job.getString("pid")).size() < 1) {

                                                map.put(job.getString("pid"), new ArrayList<FeedCommentObject>());
                                            }
                                            map.get(job.getString("pid")).add(new FeedCommentObject(job.getString("comment"), job.getString("post_id"), job.getString("pid"), job.getString("created_at"), job.getString("uname"), job.getString("upic"), job.getString("uphone"), job.getString("id"), job.getString("uid")));
                                        }
                                    }
                                    for (String id : listData) {
                                        for (FeedCommentObject feedComObj : map.get(id)) {
                                            feedCommentObjectList.add(feedComObj);

                                        }
                                    }

                                    listData.clear();
                                    map.clear();


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                        }
                                    });
                                }


                                commentList.setAdapter(new CommentsAdapter(FeedDetailActivity.this, feedCommentObjectList, true, feed.uid.equalsIgnoreCase(GetSet.getUserId())));

                                System.out.println("iam   out" + feedCommentObjectList.size());
                                findViewById(R.id.noComments).setVisibility(feedCommentObjectList.size() == 0 ? View.VISIBLE : View.GONE);
                                if (feedCommentObjectList.size() > 0 && feedCommentObjectList.size() < 10)
                                    show_next.setVisibility(View.VISIBLE);
                                show_next.performClick();

                                if (feedCommentObjectList.size() < 10) {
                                    show_prev.setVisibility(View.GONE);
                                } else {
                                    show_prev.setVisibility(View.VISIBLE);
                                    show_next.setVisibility(View.VISIBLE);
                                }
                                if (feedCommentObjectList.size() > 0 && Integer.parseInt(feedCommentObjectList.get(feedCommentObjectList.size() - 1).parent_id) == -1) {
                                    show_next.setVisibility(View.GONE);
                                }

                                if (getIntent().getBooleanExtra("showComment", false)) {
                                    commentList.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            nsv.scrollTo(0, findTopRelativeToParent(nsv, commentList.getChildAt(feedCommentObjectList.size() - 1)));
                                        }
                                    });
                                }



                    }
                });

            }


            @Override
            public void onServiceExecutedFailed(String message) {
                //
            }

        });

    }

    void setResult(boolean commentDeleted, boolean feedDeleted) {

        /*Intent intent = new Intent();
        intent.putExtra("obj", feed);
        intent.putExtra("deleted", feedDeleted);
        intent.putExtra("commentDeleted", commentDeleted);
        setResult(Activity.RESULT_OK, intent);*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableWakeLock();
    }

    FeedCommentObject objComment = null;

    @Override
    public void onCommentClicked(FeedCommentObject obj, int id_inserted) {
        objComment = obj;
        insertedId = id_inserted;
        rpText.setText(obj.comment);
        rpName.setText(obj.uname);
        rplyLay.setVisibility(View.INVISIBLE);


        rplyLay.post(new Runnable() {
            @Override
            public void run() {
                int mn = rplyLay.getMeasuredHeight();
                ViewGroup.LayoutParams layoutParams = bottomBar.getLayoutParams();
                layoutParams.height = Screen.dp(56);
                //bottomBar.setLayoutParams(layoutParams);
                bottomBar.post(new Runnable() {
                    @Override
                    public void run() {
                        toggleProductDescriptionHeight(true, mn);
                    }
                });
            }
        });
    }

    private void toggleProductDescriptionHeight(boolean expand, int mn) {

        int maxHeight = bottomBar.getHeight();//Screen.dp(56);
        int minHeight = expand ? maxHeight + mn : maxHeight - mn;
        rplyLay.setVisibility(View.VISIBLE);
        ValueAnimator anim = ValueAnimator.ofInt(maxHeight,
                minHeight);
        anim.setDuration(500);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                   @Override
                                   public void onAnimationUpdate(ValueAnimator valueAnimator) {

                                       int val = (Integer) valueAnimator.getAnimatedValue();

                                       layoutParams.height = val;
                                       //bottomBar.setLayoutParams(layoutParams);
                                       if (!expand && val == minHeight) {

                                           layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                              //             bottomBar.setLayoutParams(layoutParams);
                                           rplyLay.setVisibility(View.GONE);
                                       }
                                   }

                               }
        );
        anim.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    void hideExpendedView() {
        if (mCurrentAnimator != null && startBounds != null) {
            mCurrentAnimator.cancel();
        }

        // Animate the four positioning/sizing properties in parallel,
        // back to their original values.
        if (startBounds == null) {
            setFullscreen(false);
            thumbView.setAlpha(1f);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            expandedImageView.setVisibility(View.GONE);
            return;
        }
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator
                .ofFloat(expandedImageView, View.X, startBounds.left))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.Y, startBounds.top))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.SCALE_X, startScaleFinal))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.SCALE_Y, startScaleFinal));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setFullscreen(false);
                thumbView.setAlpha(1f);
                expandedImageView.setVisibility(View.GONE);
                mCurrentAnimator = null;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                thumbView.setAlpha(1f);
                expandedImageView.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

    }

    Rect startBounds = null, finalBounds = null;
    Point globalOffset = null;
    float startScaleFinal = -1;
    private Animator mCurrentAnimator;

    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int mShortAnimationDuration;

    private void zoomImageFromThumb() {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        //fragmentDisplay.changeMedia(feed);
        expandedImageView.setVisibility(View.INVISIBLE);

        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.

        //expandedImageView.setImageResource(imageResId);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        startBounds = new Rect();
        finalBounds = new Rect();
        globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.container)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        setFullscreen(true);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView,
                        View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        startScaleFinal = startScale;
        findViewById(R.id.toolbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
    }


    private void setFullscreen(boolean fullscreen) {
        findViewById(R.id.app_bar).setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        } else {
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
    }

    ProgressDialog pDialog;

    void updateStatusReaction(String reaction, TextView reactionTextView) {
        RequestBody requestBody = new MultipartBody.Builder()

                .setType(MultipartBody.FORM)
                .addFormDataPart("reaction", reaction)
                .addFormDataPart("post_id", feed._id)
                .addFormDataPart("ct_id",feed.uid)
                .addFormDataPart("uid", GetSet.getUserId())
                .addFormDataPart("uPhone", GetSet.getphonenumber())
                .addFormDataPart("uname", GetSet.getUserName())
                .addFormDataPart("upic", GetSet.getImageUrl() == null ? "" : GetSet.getImageUrl())
                .build();
        String url = Constants.NODE_URL + "set_feed_reaction_Hiddy";
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        ApplicationClass.getInstance().httpClient.newCall(request)
                .enqueue(new Callback() {
                             @Override
                             public void onFailure(Call call, IOException e) {

                             }

                             @Override
                             public void onResponse(Call call, Response response) {
                                 String res = null;
                                 try {
                                     res = response.body().string();
                                     System.out.println("ding dong tkkkkknp res" + res);
                                     try {
                                         JSONObject obj = new JSONObject(res);

                                         if (obj.getBoolean("success")) {

                                             if (obj.getInt("data") == 1) {
                                                 runOnUiThread(new Runnable() {
                                                     @Override
                                                     public void run() {
                                                         int pos =  viewPager.getCurrentItem();
                                                         FeedObject feedTmp = PostFragment.listData.get(pos);
                                                         feedTmp.reaction_count = String.valueOf(Integer.parseInt(feedTmp.reaction_count) + 1);
                                                         PostFragment.listData.set(pos, feedTmp);
                                                         if (pos == viewPager.getCurrentItem()) {
                                                             textView4.setText(feedTmp.reaction_count + " reaction & " + feedTmp.comment_count + " comments");
                                                             //setResult(false, false);

                                                             if (reactionTextView != null) {
                                                                 reactionTextView.setText(feedTmp.reaction_count);
                                                             }
                                                         }

                                                     }
                                                 });

                                             }

                                         }
                                         //Toast.makeText(this, "d" + obj.getString("data"), Toast.LENGTH_SHORT).show();
                                     } catch (JSONException e) {
                                         e.printStackTrace();

                                     }

                                 } catch (IOException e) {

                                     e.printStackTrace();
                                 }

                             }
                         }
                );
    }


    int scrollPosition = 0;

    public void setDataMore(boolean isNext) {
        if (feedCommentObjectList.size() == 0) return;
        String cmtId = feedCommentObjectList.get(0)._id;
        if (isNext) {
            cmtId = feedCommentObjectList.get(feedCommentObjectList.size() - 1 - itemInserted).parent_id;
            if (Integer.parseInt(cmtId) == -1) {
                cmtId = feedCommentObjectList.get(feedCommentObjectList.size() - 1 - itemInserted)._id;
            }

        }
        scrollPosition = 0;
        new ExecuteServices().execute(Constants.NODE_URL_GET + (isNext ? "get_feed_comments_next_hiddy/" : "get_feed_comments_prev_hiddy/") + feed._id + "/" + cmtId, new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                                try {
                                    listData.clear();
                                    map.clear();
                                    JSONObject fjob = new JSONObject(response);
                                    JSONArray jAr = fjob.getJSONArray("data");
                                    scrollPosition = jAr.length();
                                    for (int i = 0; i < scrollPosition; i++) {
                                        JSONObject job = jAr.getJSONObject(i);
                                        if (!job.getString("post_id").equalsIgnoreCase(feed._id)) {
                                            listData.clear();
                                            map.clear();
                                            feedCommentObjectList.clear();
                                            return ;
                                        }
                                        if (job.getString("pid").equalsIgnoreCase("-1") && !listData.contains(job.getString("id"))) {
                                            listData.add(job.getString("id"));

                                            map.put(job.getString("id"), new ArrayList<FeedCommentObject>());

                                            map.get(job.getString("id")).add(new FeedCommentObject(job.getString("comment"), job.getString("post_id"), job.getString("pid"), job.getString("created_at"), job.getString("uname"), job.getString("upic"), job.getString("uphone"), job.getString("id"), job.getString("uid")));

                                        } else {
                                            if (!map.containsKey(job.getString("pid")) || map.get(job.getString("pid")).size() < 1) {

                                                map.put(job.getString("pid"), new ArrayList<FeedCommentObject>());
                                            }
                                            map.get(job.getString("pid")).add(new FeedCommentObject(job.getString("comment"), job.getString("post_id"), job.getString("pid"), job.getString("created_at"), job.getString("uname"), job.getString("upic"), job.getString("uphone"), job.getString("id"), job.getString("uid")));
                                        }
                                    }
                                    ArrayList<FeedCommentObject> feedCommentObjectList2 = new ArrayList<>();
                                    final int dataSize = listData.size();

                                            findViewById(R.id.prev_shimmer).setVisibility(View.GONE);
                                            findViewById(R.id.next_shimmer).setVisibility(View.GONE);
                                            if (isNext)
                                                show_next.setVisibility(dataSize < 10 ? View.GONE : View.VISIBLE);
                                            else
                                                show_prev.setVisibility(dataSize < 10 ? View.GONE : View.VISIBLE);


                                    for (String id : listData) {
                                        if (isNext && itemInserted > 0) {
                                            if (feedCommentObjectList.get(feedCommentObjectList.size() - itemInserted)._id.equalsIgnoreCase(id)) {
                                                feedCommentObjectList.remove(feedCommentObjectList.size() - itemInserted);
                                                itemInserted--;
                                            }
                                        }
                                        for (FeedCommentObject feedComObj : map.get(id)) {
                                            feedCommentObjectList2.add(feedComObj);

                                        }
                                    }


                                    feedCommentObjectList.addAll(isNext ? feedCommentObjectList.size() - itemInserted : 0, feedCommentObjectList2);
                                    feedCommentObjectList2 = null;

                                    listData.clear();
                                    map.clear();


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                        }
                                    });
                                }

                                if (scrollPosition > 0) {
                                    if (isNext)
                                        commentList.getAdapter().notifyDataSetChanged();
                                    else {
                                        commentList.getAdapter().notifyItemRangeInserted(0, scrollPosition);

                                    }
                                }


                    }
                });

            }


            @Override
            public void onServiceExecutedFailed(String message) {
                //
            }

        });

    }

    int scrollPositionSub = 0;

    @Override
    public void setDataSubCommentMore(int position, FeedCommentObject objectComment) {
        pd.show();
        scrollPositionSub = 0;
        new ExecuteServices().execute(Constants.NODE_URL_GET+"get_feed_sub_comments_prev_hiddy/" + objectComment._id + "/" + objectComment.parent_id, new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                                try {
                                    JSONObject fjob = new JSONObject(response);
                                    JSONArray jAr = fjob.getJSONArray("data");
                                    scrollPositionSub = jAr.length();

                                    ArrayList<FeedCommentObject> feedCommentObjectList2 = new ArrayList<>();

                                    for (int i = 0; i < scrollPositionSub; i++) {
                                        JSONObject job = jAr.getJSONObject(i);
                                        if (!job.getString("post_id").equalsIgnoreCase(feed._id)) {
                                            feedCommentObjectList.clear();
                                            listData.clear();
                                            map.clear();
                                            return ;
                                        }

                                        feedCommentObjectList2.add(new FeedCommentObject(job.getString("comment"), job.getString("post_id"), job.getString("pid"), job.getString("created_at"), job.getString("uname"), job.getString("upic"), job.getString("uphone"), job.getString("id"), job.getString("uid")));
                                    }

                                    if (scrollPositionSub < 4) {
                                        noLoadSubComment.add(objectComment.parent_id);
                                    }
                                    feedCommentObjectList.addAll(position, feedCommentObjectList2);
                                    feedCommentObjectList2 = null;

                                    listData.clear();
                                    map.clear();


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                        }
                                    });
                                }


                                pd.dismiss();
                                if (scrollPositionSub == 0)
                                    Toast.makeText(FeedDetailActivity.this, "No more comments", Toast.LENGTH_SHORT).show();
                                //  if (commentList.getAdapter() == null)
                                //   commentList.setAdapter(new CommentsAdapter(FeedDetailActivity.this, feedCommentObjectList,true));
                                if (scrollPositionSub > 0) {
                                    commentList.getAdapter().notifyItemRangeInserted(position, scrollPositionSub);
                                }
                                commentList.getAdapter().notifyItemChanged(position + scrollPositionSub);
                    }
                });

            }


            @Override
            public void onServiceExecutedFailed(String message) {
                //
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                    }
                });
            }

        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            deleteFeed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void deleteFeed() {
        ProgressDialog pd = ProgressDialog.show(this, "", "Deleting...");
        pd.setCancelable(false);


        String up_id = feed._id;
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)

                .addFormDataPart("status_id", up_id)
                .build();

        new ExecuteServices().executePost(Constants.NODE_URL+"delete_feed_hiddy", new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pd.dismiss();
                            JSONObject jObj = new JSONObject(response);
                            if (jObj.getBoolean("success")) {
                                Toast.makeText(FeedDetailActivity.this, "Deleted ", Toast.LENGTH_SHORT).show();

                                PostFragment.listData.remove(viewPager.getCurrentItem());
                                feedDetailPagerAdapter.removeFrag(viewPager.getCurrentItem());
                                //setResult(false, true);
                                if (PostFragment.listData.size() == 0)
                                    finish();

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });


            }

            @Override
            public void onServiceExecutedFailed(String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                        Toast.makeText(FeedDetailActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }, requestBody);
    }


    @Override
    public void deleteComment(int id, String commentID) {
        ProgressDialog pd = ProgressDialog.show(this, "", "Deleting...");
        pd.setCancelable(false);


        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)

                .addFormDataPart("status_id", commentID)
                .build();

        new ExecuteServices().executePost(Constants.NODE_URL+"delete_comment", new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pd.dismiss();
                            JSONObject jObj = new JSONObject(response);
                            if (jObj.getBoolean("success")) {
                                Toast.makeText(FeedDetailActivity.this, "Deleted ", Toast.LENGTH_SHORT).show();
                                boolean commentDeleted = false;
                                if (feedCommentObjectList.size() == 1) {
                                    commentDeleted = true;
                                    objModified = feed.feedComment = null;
                                    findViewById(R.id.noComments).setVisibility(View.VISIBLE);
                                } else if (id == feedCommentObjectList.size() - 1)
                                    objModified = feedCommentObjectList.get(feedCommentObjectList.size() - 2);
                                else
                                    objModified = feed.feedComment;
                                feed.feedComment = objModified;
                                feed.comment_count = String.valueOf(Integer.parseInt(feed.comment_count) - 1);
                                textView4.setText(feed.reaction_count + " reaction & " + feed.comment_count + " comments");
                                //setResult(commentDeleted, false);

                                commentList.getAdapter().notifyItemRemoved(id);
                                feedCommentObjectList.remove(id);
                                PostFragment.listData.set(viewPager.getCurrentItem(), feed);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });


            }

            @Override
            public void onServiceExecutedFailed(String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                        Toast.makeText(FeedDetailActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }, requestBody);
    }



    @Override
    protected void onResume() {
        super.onResume();
        enableWakeLock();
    }

    void showBlockAlert() {
        new AlertDialog.Builder(this).setTitle("You are Blocked").setMessage("You are blocked on Indian Messenger to post any content.\nPlease contact to Admin to resolve the issue").
                setPositiveButton("Contact Admin", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create().show();
    }


    ProgressDialog pDialog2;
    String ur = Constants.NODE_URL+"pic_st/";

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting bg_default_pattern thread
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            System.out.println("Starting download");

            pDialog2 = new ProgressDialog(FeedDetailActivity.this);
            pDialog2.setMessage("Downloading... Please wait...");
            pDialog2.setIndeterminate(false);
            pDialog2.setCancelable(false);
            pDialog2.show();
        }

        /**
         * Downloading file in bg_default_pattern thread
         */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            File root = null;
            try {
                String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();//externalFile.getAbsolutePath();

                /*File externalFile = AndroidContext.getContext().getExternalFilesDir(null);
                if (externalFile == null) {
                    return null;
                }*/
                // String externalPath = externalFile.getAbsolutePath();
                root = new File(externalPath + "/Indian-Messenger/downloads/");
                // root = new File(externalPath + "/Indian-Messenger/.IM sent/");
                root.mkdirs();

                //String root = Environment.getExternalStorageDirectory().toString();

                System.out.println("Downloading");
                URL url = new URL(ur + f_url[0]);

                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file

                OutputStream output = new FileOutputStream(root + "/" + f_url[0]);
                byte data[] = new byte[1024];

                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;

                    // writing data to file
                    output.write(data, 0, count);

                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
            }

            return root == null ? null : root.getPath() + "/" + f_url[0];
        }


        /**
         * After completing bg_default_pattern task
         **/
        @Override
        protected void onPostExecute(String file_url) {
            //Uri contentUri = FileProvider.getUriForFile(getContext(), getContext().getApplicationContext().getPackageName() + ".my.package.name.provider", new File(file_url));
            pDialog2.dismiss();

            if (file_url != null) {
                File file = new File(file_url);
                Uri uri = Uri.fromFile(file);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        if (file.getName().startsWith("vid_")) {

                            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            intent.setData(Uri.fromFile(file));
                            sendBroadcast(intent);
                        } else {
                           /*     MediaStore.Images.Media.insertImage(getActivity().getContentResolver(),
                                        file_url, file.getName(), null);*/
                            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            intent.setData(Uri.fromFile(file));
                            sendBroadcast(new Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                        }


                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(Uri.parse(file_url));
                        sendBroadcast(intent);

                    }
                }, 200);

            }
        }

    }

    boolean stopLoadMore = false;
    private boolean loadMore = true;

    public void setDataMore() {
        if (stopLoadMore) {
            loadMore = true;
        }
        // Toast.makeText(getActivity(), "setDta", Toast.LENGTH_SHORT).show();
        new ExecuteServices().execute(Constants.NODE_URL_GET+"get_feeds_more_private_hiddy/" + PostFragment.listData.size(), new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<FeedObject> listDataTmp = new ArrayList<>();
                        //     Toast.makeText(getActivity(), "feed", Toast.LENGTH_SHORT).show();
                                try {
                                    JSONObject fjob = new JSONObject(response);
                                    JSONArray jAr = fjob.getJSONArray("data");
                                    if (jAr.length() < 5) {
                                        stopLoadMore = true;
                                    }

                                    for (int i = 0; i < jAr.length(); i++) {
                                        JSONObject job = jAr.getJSONObject(i);
                                        String media_type = "text_";
                                        if (!TextUtils.isEmpty(job.getString("amedia")) && !job.getString("amedia").equalsIgnoreCase("null")) {
                                            media_type = job.getString("amedia").startsWith("vid_") ? "vid_" : "img_";
                                        }
                                        listDataTmp.add(new FeedObject(job.getString("atext1"), job.getString("atext2"), job.getString("amedia"), job.getString("acreated_at"), job.getString("auname"), job.getString("aupic"), job.getString("auphone"), job.getString("aid"), job.getString("auid"), new FeedCommentObject(job.getString("comment"), job.getString("post_id"), job.getString("pid"), job.getString("created_at"), job.getString("uname"), job.getString("upic"), job.getString("uphone"), job.getString("id"), job.getString("uid")), job.isNull("count") ? "0" : job.getString("count"), job.isNull("ckount") ? "0" : job.getString("ckount"), job.getInt("afile_size"), media_type, 0));

                                    }


                                } catch (JSONException e) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            //     Toast.makeText(getActivity(), "" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
              loadMore = true;
                                PostFragment.listData.addAll(listDataTmp);
                                if (listDataTmp.size() > 0) {
                                    for (int i = 0; i < listDataTmp.size(); i++) {

                                        feedDetailPagerAdapter.addFrag(new FeedDisplayFragment());
                                    }
                                }


                    }
                });

            }


            @Override
            public void onServiceExecutedFailed(String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }

        });

    }


}