package com.loopytime.im;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.loopytime.country.Strings;
import com.loopytime.external.videotrimmer.utils.Screen;
import com.loopytime.external.videotrimmer.view.BubbleContainerFeed;
import com.loopytime.external.videotrimmer.view.CircleViewAnim;
import com.loopytime.external.videotrimmer.view.DotsView;
import com.loopytime.helper.ExecuteServices;
import com.loopytime.im.status.FeedCommentObject;
import com.loopytime.im.status.FeedObject;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import io.supercharge.shimmerlayout.ShimmerLayout;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING;
import static com.loopytime.helper.Utils.isInteger;
import static java.util.Calendar.SHORT;

/**
 * Created by ex3ndr on 30.09.14.
 */
public class WhatStatusFragmentFeedProfile extends Fragment {

    ArrayList<Integer> whatsList = new ArrayList<>();
    View res;
    //public MaterialColor color;
    public static ArrayList<FeedObject> listDataProfile = new ArrayList<>();
    RecyclerView list;
    boolean loaded = false;
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static SimpleDateFormat format2 = new SimpleDateFormat("MMM''dd yyyy, HH:mma", Locale.ENGLISH);
    static SimpleDateFormat format3 = new SimpleDateFormat("MMM''dd, HH:mma", Locale.ENGLISH);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        res = inflater.inflate(R.layout.fragment_what_status_tab, container, false);
        res.setBackgroundColor(Color.parseColor("#dedede"));
        return res;
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    Drawable dr, dr2;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //color = MaterialColors.CONVERSATION_PALETTE.get(getActivity().getSharedPreferences("wall", Context.MODE_PRIVATE).getInt(MaterialColors.THEME, 0));
        dr = AppCompatResources.getDrawable(getActivity(), R.drawable.ic_insert_emoticon_black_24dp);
        dr2 = ContextCompat.getDrawable(getContext(), R.drawable.ic_textsms_white_18dp);//DrawableCompat.getDrawable(getActivity(),R.drawable.ic_chat_black_24dp);
        dr2 = DrawableCompat.wrap(dr2);
        //dr.mutate().setColorFilter(Color.parseColor("#8a000000"),PorterDuff.Mode.SRC_IN);
        DrawableCompat.setTint(dr, Color.parseColor("#8a000000"));
        DrawableCompat.setTint(dr2, Color.parseColor("#8a000000"));

        if (savedInstanceState != null) {
            loadMore = true;
            loaded = false;
            stopLoadMore = false;
            if (list != null)
                list.scrollTo(0, 0);
        }
        list = (RecyclerView) res.findViewById(R.id.list);
        list.scrollTo(0, 0);
        //  list.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayout.VERTICAL));
        list.setLayoutManager(linearLayoutManager = new LinearLayoutManager(getActivity()));
        //clr = getResources().getIntArray(R.array.clr);
        setListListener();
        setData();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (resultCode == Activity.RESULT_OK) {
                if (getActivity() != null)
                    if (clickedItem != -1) {
                        if (data.getBooleanExtra("deleted", false)) {
                            list.getAdapter().notifyItemRemoved(clickedItem);
                            listDataProfile.remove(clickedItem);
                            return;
                        }
                        if (data.getBooleanExtra("commentDeleted", false)) {
                            listDataProfile.set(clickedItem, (FeedObject) data.getSerializableExtra("obj"));
                            list.getAdapter().notifyItemChanged(clickedItem);
                            return;
                        }
                        if (null != data.getSerializableExtra("obj")) {
                            listDataProfile.set(clickedItem, (FeedObject) data.getSerializableExtra("obj"));
                            list.getAdapter().notifyItemChanged(clickedItem);
                        }
                    }
            }
        }
    }

    LinearLayoutManager linearLayoutManager;
    private int visibleThreshold = 2, totalItemCount, lastVisibleItem, firstVisibleItem;
    private boolean loadMore = true;

    void setListListener() {
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager
                        .findLastVisibleItemPosition();
                if (!stopLoadMore && loaded && loadMore
                        && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    loadMore = false;
                    list.getAdapter().notifyItemChanged(listDataProfile.size() - 1);
                    //list.scrollTo(0,list.getScrollY()+Screen.dp(20));
                    setDataMore();

                    //    Toast.makeText(getActivity(), "load more"+totalItemCount+" "+lastVisibleItem, Toast.LENGTH_SHORT).show();
                }

               /* if(newState == SCROLL_STATE_IDLE || newState == SCROLL_STATE_SETTLING){
                    if(linearLayoutManager.findLastCompletelyVisibleItemPosition() == listData.size()-1)
                        list.smoothScrollBy(0,list.getScrollY()+Screen.dp(150));//lToPosition(linearLayoutManager.findLastVisibleItemPosition()+1);
                } */
              /*  if(!loadMore ){
                    res.findViewById(R.id.load_more).setVisibility(lastVisibleItem == totalItemCount-1?View.VISIBLE:View.GONE);
                }
                else
                    res.findViewById(R.id.load_more).setVisibility(View.GONE);*/
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(list.getAdapter()!=null){
            list.getAdapter().notifyDataSetChanged();
        }

    }




    public void setData() {
        Glide.with(getActivity()).asGif().load(R.drawable.loading_gif);//.into((ImageView) res.findViewById(R.id.no_conn_img));
        if (res == null)
            return;
        res.findViewById(R.id.no_connection).setVisibility(View.GONE);
        loaded = false;
        list.setAdapter(new FeedsAdapter());

        // Toast.makeText(getActivity(), "setDta", Toast.LENGTH_SHORT).show();
        new ExecuteServices().execute(Constants.NODE_URL_GET+"get_profile_feeds_hiddy/"+((WhatStatusFragmentFeedProfileActivity)getActivity()).uid, new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //     Toast.makeText(getActivity(), "feed", Toast.LENGTH_SHORT).show();
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                if (list.getAdapter() != null)
                                    list.getAdapter().notifyDataSetChanged();
                            }

                            @Override
                            protected Void doInBackground(Void... voids) {
                                try {
                                    listDataProfile.clear();
                                    JSONObject fjob = new JSONObject(response);
                                    JSONArray jAr = fjob.getJSONArray("data");
                                    for (int i = 0; i < jAr.length(); i++) {
                                        JSONObject job = jAr.getJSONObject(i);
                                        String media_type = "text_";
                                        if (!TextUtils.isEmpty(job.getString("amedia")) && !job.getString("amedia").equalsIgnoreCase("null")) {
                                            media_type = job.getString("amedia").startsWith("vid_") ? "vid_" : "img_";
                                        }
                                        listDataProfile.add(new FeedObject(job.getString("atext1"), job.getString("atext2"), job.getString("amedia"), job.getString("acreated_at"), job.getString("auname"), job.getString("aupic"), job.getString("auphone"), job.getString("aid"), job.getString("auid"), new FeedCommentObject(job.getString("comment"), job.getString("post_id"), job.getString("pid"), job.getString("created_at"), job.getString("uname"), job.getString("upic"), job.getString("uphone"), job.getString("id"), job.getString("uid")), job.isNull("count") ? "0" : job.getString("count"), job.isNull("ckount") ? "0" : job.getString("ckount"), job.getInt("afile_size"), media_type, 0));
                                    }


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            Toast.makeText(getActivity(), "woops Something wrong happen", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                super.onPostExecute(aVoid);
                                //  if (list.getAdapter() == null)
                                loaded = true;
                                list.setAdapter(new FeedsAdapter());
                                list.scrollToPosition(0);
                                //else
                                //  list.getAdapter().notifyDataSetChanged();

                            }
                        }.execute();


                    }
                });

            }


            @Override
            public void onServiceExecutedFailed(String message) {
                //

                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        res.findViewById(R.id.no_connection).setVisibility(View.VISIBLE);
                    }
                });
            }

        });

    }

    boolean stopLoadMore = false;
    int insertCount = 0;

    public void setDataMore() {
        insertCount = 0;
        if (stopLoadMore) {
            loadMore = true;
        }

        // Toast.makeText(getActivity(), "setDta", Toast.LENGTH_SHORT).show();
        new ExecuteServices().execute(Constants.NODE_URL_GET+"get_profile_feeds_more_hiddy/" + listDataProfile.size()+"/"+((WhatStatusFragmentFeedProfileActivity)getActivity()).uid, new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //     Toast.makeText(getActivity(), "feed", Toast.LENGTH_SHORT).show();
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                            }

                            @Override
                            protected Void doInBackground(Void... voids) {

                                try {
                                    JSONObject fjob = new JSONObject(response);
                                    JSONArray jAr = fjob.getJSONArray("data");
                                    insertCount = jAr.length();
                                    if (jAr.length() < 5) {
                                        stopLoadMore = true;
                                    }

                                    for (int i = 0; i < jAr.length(); i++) {
                                        JSONObject job = jAr.getJSONObject(i);
                                        String media_type = "text_";
                                        if (!TextUtils.isEmpty(job.getString("amedia")) && !job.getString("amedia").equalsIgnoreCase("null")) {
                                            media_type = job.getString("amedia").startsWith("vid_") ? "vid_" : "img_";
                                        }
                                        listDataProfile.add(new FeedObject(job.getString("atext1"), job.getString("atext2"), job.getString("amedia"), job.getString("acreated_at"), job.getString("auname"), job.getString("aupic"), job.getString("auphone"), job.getString("aid"), job.getString("auid"), new FeedCommentObject(job.getString("comment"), job.getString("post_id"), job.getString("pid"), job.getString("created_at"), job.getString("uname"), job.getString("upic"), job.getString("uphone"), job.getString("id"), job.getString("uid")), job.isNull("count") ? "0" : job.getString("count"), job.isNull("ckount") ? "0" : job.getString("ckount"), job.getInt("afile_size"), media_type, 0));
                                    }


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            //     Toast.makeText(getActivity(), "" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                super.onPostExecute(aVoid);
                                //  if (list.getAdapter() == null)
                                //loaded = true;
                                //list.setAdapter(new FeedsAdapter());
                                //else
                                //  list.getAdapter().notifyDataSetChanged();
                                loadMore = true;
                                list.getAdapter().notifyItemChanged(listDataProfile.size() - insertCount - 1);

                                if (insertCount > 0) {
                                    list.getAdapter().notifyItemRangeInserted(listDataProfile.size() - insertCount, listDataProfile.size() - 1);
                                }
                                if (list.getScrollState() == SCROLL_STATE_IDLE || list.getScrollState() == SCROLL_STATE_SETTLING) {
                                    //if (linearLayoutManager.findLastCompletelyVisibleItemPosition() + 1 < listData.size())
                                    //   list.smoothScrollBy(0, list.getScrollY() + Screen.dp(50));//lToPosition(linearLayoutManager.findLastVisibleItemPosition()+1);
                                }
                            }
                        }.execute();


                    }
                });

            }


            @Override
            public void onServiceExecutedFailed(String message) {
                //
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadMore = false;
                     //   res.findViewById(R.id.no_connection).setVisibility(View.VISIBLE);
                    }
                });
            }

        });

    }


    public static String formatDate(Calendar c, boolean isComment) {
        if (Strings.diffDays(c.getTimeInMillis(), new Date().getTime()) > 1) {
            return isComment ? format3.format(c.getTime()) : format2.format(c.getTime());
        }
        Calendar cc = Calendar.getInstance();

        if (Strings.diffDays(c.getTimeInMillis(), new Date().getTime()) == 1) {
            return "Yesterday, " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE) + " " + c.getDisplayName(Calendar.AM_PM, SHORT, Locale.ENGLISH);
        }
        if (cc.get(Calendar.HOUR) == c.get(Calendar.HOUR) && cc.get(Calendar.AM_PM) == c.get(Calendar.AM_PM) && (cc.get(Calendar.MINUTE) - c.get(Calendar.MINUTE) <= 5)) {
            return "Now";
        }

        return "Today, " + c.get(Calendar.HOUR) + ":" + (c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE)) + " " + c.getDisplayName(Calendar.AM_PM, SHORT, Locale.ENGLISH);
    }

    Calendar c = Calendar.getInstance();

    //int clr[];

    class FeedsAdapter extends RecyclerView.Adapter<FeedsAdapter.ViewHolder> {
        @Override
        public FeedsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FeedsAdapter.ViewHolder(LayoutInflater.from(getActivity()).inflate(!loaded ? R.layout.public_feed_layout_shimmer : R.layout.public_feed_layout, (ViewGroup) list.getRootView(), false));
        }


        void setText(String text, TextView tv) throws JSONException {
            JSONObject obj = new JSONObject(text);
            //tv.setSolidColor(clr[Integer.parseInt(obj.getString("bc"))]);
            tv.setText(obj.getString("txt"));

        }


        void performUpload(ViewHolder holder, int position) {


        }

        void getStatusUpload(ViewHolder holder, int position) {

        }

        void setUpFeed(FeedsAdapter.ViewHolder holder, int position) {
            holder.shimmer.setVisibility(View.GONE);
            holder.delete.setVisibility(View.GONE);
            holder.download.setVisibility(View.VISIBLE);
            holder.whatsapp.setVisibility(View.VISIBLE);
            if (!loadMore && position == listDataProfile.size() - 1) {
                holder.shimmer.setVisibility(View.VISIBLE);
                //   holder.card.setVisibility(View.GONE);
                //return;
            } else if (holder.shimmer != null) {
                //        holder.card.setVisibility(View.VISIBLE);
                holder.shimmer.setVisibility(View.GONE);
            }
            holder.load_more.setVisibility(View.GONE);
            if (stopLoadMore && position == listDataProfile.size() - 1) {
                holder.load_more.setVisibility(View.VISIBLE);
            }
            if (position!=0  && position % 8 == 0) {
                holder.adLayout.setVisibility(View.VISIBLE);
                //holder.adView.setAdSize(AdSize.BANNER);
                //holder.adView.setAdUnitId(getString(R.string.banner_ad_unit_id));
               /* holder.adView.loadAd( new AdRequest.Builder().addTestDevice("90DE0FF5D1BEB82ACBE8518D057B6FA5")
                        .build());*/
                //remove it foradd holder.adView.loadAd( new AdRequest.Builder().build());
            }
            boolean isLocal = false;
            if (getActivity() == null) return;
            FeedObject feed = listDataProfile.get(position);
            try {
                isLocal = isInteger(feed.created_at);

                    c.setTimeInMillis(format.parse(feed.created_at).getTime() + Calendar.getInstance().get(Calendar.ZONE_OFFSET));
                    holder.feedTxtCount.setText(formatDate(c, false));
                    if (feed.feedComment != null && !TextUtils.isEmpty(feed.feedComment.created_at) && !feed.feedComment.created_at.equalsIgnoreCase("null")) {
                        c.setTimeInMillis(format.parse(feed.feedComment.created_at).getTime() + Calendar.getInstance().get(Calendar.ZONE_OFFSET));
                        holder.time.setText(formatDate(c, true));
                    }

            } catch (ParseException e) {
                holder.feedTxtCount.setText(feed.created_at);

            }
            holder.container.hideDate();
            holder.container.hideUnread();

            if (feed.feedComment == null || TextUtils.isEmpty(feed.feedComment.created_at) || feed.feedComment.created_at.equalsIgnoreCase("null")) {

                holder.container.setVisibility(View.GONE);
                holder.no_comments.setVisibility(View.VISIBLE);
            } else {
                holder.container.makeInboundBubble(true, feed.feedComment.uname, feed.feedComment.upic);
                holder.container.setVisibility(View.VISIBLE);
                holder.no_comments.setVisibility(View.GONE);
                holder.text.setText(feed.feedComment.comment);
                holder.cmmntName.setText(feed.feedComment.uname);
            }
            Drawable dr = ContextCompat.getDrawable(getActivity(), R.drawable.bubble_in);
            int colorm = Color.parseColor("#C9ECFC");// c6f4f3#C9ECFC: Color.WHITE;//ColorUtils.compositeColors(ColorUtils.setAlphaComponent(Color.parseColor("#ff9800"),110),Color.WHITE):Color.WHITE;
            dr.mutate().setColorFilter(colorm, PorterDuff.Mode.SRC_IN);
            holder.messageBubble.setBackgroundDrawable(dr);


            holder.overlay.setVisibility(View.GONE);
            holder.feedTxt.setVisibility(View.GONE);
            holder.feedImage.setVisibility(View.GONE);
            holder.uName.setText(feed.uname);
            Glide.with(getActivity()).applyDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.temp)).load(feed.upic).
                    into(holder.avatarView);
            holder.textView4.setText(feed.reaction_count + " reaction & " + feed.comment_count + " comments");
            holder.listLikes.setVisibility(View.VISIBLE);
            if (isLocal)
                getStatusUpload(holder, position);

            if (!TextUtils.isEmpty(feed.text1) && !feed.text1.equalsIgnoreCase("null")) {
                JSONObject jsonObject;
                holder.feedTxt.setVisibility(View.VISIBLE);
                if (!isLocal) {
                    holder.download.setVisibility(View.VISIBLE);

                    holder.download.setImageDrawable(AppCompatResources.getDrawable(ApplicationClass.getInstance(), R.drawable.ic_share));
                }
                holder.progressView.setVisibility(View.GONE);
                holder.progressValue.setVisibility(View.GONE);

                try {
                    jsonObject = new JSONObject(feed.text1);


                    holder.feedTxt.setText(jsonObject.getString("txt"));


                    //holder.feedTxt.setBackgroundColor(clr[Integer.parseInt(jsonObject.getString("bc"))]);
                } catch (JSONException e) {
                    return;
                }

            } else if (feed.media_type.equalsIgnoreCase("vid_")) {
                holder.feedImage.setVisibility(View.VISIBLE);
                holder.overlay.setVisibility(View.VISIBLE);
                if (!isLocal) {
                    holder.download.setVisibility(View.VISIBLE);
                }
               /* Glide.with(getActivity()).
                        load(Uri.parse(ActorSDK.sharedActor().getNodeUrl()+":3040/pic_st/" + feed.media + ".png"
                        )).into(holder.feedImage);*/
                holder.feedImage.setImageResource(0);


               showGif(holder.feedImage, Constants.NODE_URL+"pic_st/" + feed.media + ".png", position, holder.imgPlaceholder);



                /* Glide.with(getActivity()).asBitmap().
                        load(Uri.parse(ActorSDK.sharedActor().getNodeUrl()+":3040/pic_st/" + feed.media + ".png"
                        )).into(new SimpleTarget<Bitmap>() {
                    @Override-
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        setAspectImage(resource, holder.feedImage, position);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                    }
                });*/
                  /*  Glide.with(getContext())
                            .load(Uri.parse("http://97.74.4.59:3040/pic_st/" + map.get(uid.get(position)).media))

                            .thumbnail(Glide.with(getContext()).load(Uri.parse("97.74.4.59:3040/pic_st/" + map.get(uid.get(position)).media))).apply(RequestOptions.circleCropTransform())
                            .apply(RequestOptions.circleCropTransform())
                            .into(holder.avatar_my);*/
            } else {

                    showGif(holder.feedImage, Constants.NODE_URL+"pic_st/" + feed.media, position, holder.imgPlaceholder);

            }
            holder.anchorText.setVisibility(TextUtils.isEmpty(feed.text2) || feed.text2.equalsIgnoreCase("null") ? View.GONE : View.VISIBLE);
            if (!TextUtils.isEmpty(feed.text2) && !feed.text2.equalsIgnoreCase("null"))
                holder.anchorText.setText(feed.text2);

        }

        int imgWidth = 0;

        void setAspectImage(Bitmap bit, ImageView imgView, int position) {
            if (imgWidth == 0)
                imgWidth = ((View) imgView.getParent()).getWidth();
           /* if(bit.getWidth()<imgWidth){
                imgView.setImageBitmap(bit);
                return;
            }*/
            double ratio = 1;
            if(bit.getWidth()>imgWidth)

            {
                ratio=(bit.getWidth()- Screen.dp(4) * 1.0) / (1.0 * imgWidth);
            }else if( bit.getWidth()<imgWidth- Screen.dp(40)){

                ratio=(bit.getWidth() * 1.0) / (1.0 * imgWidth- Screen.dp(36));
            }
            if (imgWidth == 0 || ratio == 0 || bit.getHeight() == 0 || bit.getWidth() == 0)
                return;
            imgView.setImageBitmap(bit.createScaledBitmap(bit, (int) (bit.getWidth() / ratio), (int) (bit.getHeight() / ratio), false));

        }

        void showGif(ImageView imageView, String url, int position, ImageView placeHolder) {
            Glide.with(getActivity()).asGif().load(R.drawable.loading_gif).
                    into(placeHolder
                    );
            Glide.with(getActivity()).asBitmap()
                    .load((url
                    )).into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                    setAspectImage(resource, imageView, position);
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    super.onLoadFailed(errorDrawable);
                }
            });
        }

        void sst(ImageView imageView, String url, int position, ImageView placeHolder) {  // imageView.setImageDrawable(null);
            Glide.with(getActivity()).asGif().load(R.drawable.loading_gif).listener(new RequestListener<GifDrawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                    imageView.setImageDrawable(null);
                    return false;
                }

                @Override
                public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showGif(imageView, url, position, placeHolder);
                        }
                    }, 200);
                    return false;
                }
            }).into(imageView);
            //   DrawableImageViewTarget imageViewTarget = new DrawableImageViewTarget(imageView);
            // Glide.with(getActivity()).load(R.raw.loadingG).into(imageViewTarget);
        }


        @Override
        public void onBindViewHolder(FeedsAdapter.ViewHolder holder, int position) {
            if (!loaded) {
                ((ShimmerLayout) holder.shimmer).startShimmerAnimation();
                return;
            }
            setLikesDatList(holder, holder.listLikes, position);
            //      }
            // });
            System.out.println("ding dong tkkkkknp setLikesDatList pos *****" + position);
            setUpFeed(holder, position);

        }

        @Override
        public int getItemCount() {
            return loaded ? listDataProfile.size() : 4;
        }


        void setLikesDatList(ViewHolder holder, RecyclerView listLikes, int pos) {


            listLikes.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
            listLikes.setAdapter(new RecyclerView.Adapter<ViewHolderLikes>() {
                @NonNull
                @Override
                public ViewHolderLikes onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                    return new ViewHolderLikes(LayoutInflater.from(getActivity()).inflate(R.layout.item_likessmall, parent, false), holder, pos);
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

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            holder.unbind();
            super.onViewRecycled(holder);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public void unbind() {
            }

            ProgressBar progressView;
            TextView uName, progressValue;
            View shimmer, progressBg;
            TextView feedTxt, no_comments, feedTxtCount, textView4, text, time, anchorText, cmmntName;
            View overlay, load_more, card;
            ImageView feedImage, delete, download, whatsapp, imgPlaceholder, upload;
            CircleImageView avatarView;
            BubbleContainerFeed container;
            FrameLayout messageBubble;
            RecyclerView listLikes;
            View adLayout;

            public ViewHolder(View itemView) {
                super(itemView);
                shimmer = itemView.findViewById(R.id.shimmer);
                if (!loaded) {

                    return;
                }
                listLikes = itemView.findViewById(R.id.listLikes);
                delete = itemView.findViewById(R.id.delete);
                whatsapp = itemView.findViewById(R.id.whatsapp);
                imgPlaceholder = itemView.findViewById(R.id.imgPlaceholder);
                itemView.findViewById(R.id.history).setVisibility(View.GONE);
                progressBg = itemView.findViewById(R.id.progressBg);
                progressValue = itemView.findViewById(R.id.progressValue);
                progressView = itemView.findViewById(R.id.progressView);
                //progressValue.setTextColor(color.toActionBarColor(AndroidContext.getContext()));
                //progressView.setColor(color.toActionBarColor(AndroidContext.getContext()));
                //delete.setColorFilter(color.toActionBarColor(AndroidContext.getContext()));
                container = (BubbleContainerFeed) itemView.findViewById(R.id.mainContainer);
                load_more = itemView.findViewById(R.id.load_more);
                card = itemView.findViewById(R.id.card);
              /*  container.post(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("ding dong tkkkkknp setLikesDatList pos *****"+getAdapterPosition());
                    */
                itemView.findViewById(R.id.stateIcon).setVisibility(View.GONE);
                text = (TextView) itemView.findViewById(R.id.tv_text);
                no_comments = itemView.findViewById(R.id.noComments);
                download = itemView.findViewById(R.id.download);
                upload = itemView.findViewById(R.id.upload);
                //upload.setColorFilter(color.toActionBarColor(AndroidContext.getContext()));
                //download.setColorFilter(color.toActionBarColor(AndroidContext.getContext()));
                cmmntName = itemView.findViewById(R.id.tv_name);
                anchorText = (TextView) itemView.findViewById(R.id.anchorText);
                //(ActorSDK.sharedActor().style.getConvTextColor());
                //text.setTypeface(Fonts.regular());
                time = (TextView) itemView.findViewById(R.id.tv_time);
                //ActorSDK.sharedActor().style.getConvTimeColor();
                //time.setTypeface(Fonts.regular());
                //time.setTextColor(ActorSDK.sharedActor().style.getConvTimeColor());
                messageBubble = (FrameLayout) itemView.findViewById(R.id.fl_bubble);
                uName = itemView.findViewById(R.id.uName);
                overlay = itemView.findViewById(R.id.overlay_nm);
                textView4 = itemView.findViewById(R.id.textView4);
                textView4.setCompoundDrawablesWithIntrinsicBounds(dr, null, null, null);
                no_comments.setCompoundDrawablesWithIntrinsicBounds(dr2, null, null, null);
                feedImage = itemView.findViewById(R.id.img);
                avatarView = (CircleImageView) itemView.findViewById(R.id.avatarView);
                feedTxt = itemView.findViewById(R.id.text1);
                feedTxtCount = itemView.findViewById(R.id.textView3);
                itemView.findViewById(R.id.progressBg).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FeedObject feed = listDataProfile.get(getAdapterPosition());
                        //downloadFeedMedia(feed, ViewHolder.this);
                        //   new DownloadFileFromURL().execute(feed.media);
                    }
                });
                itemView.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
                itemView.findViewById(R.id.upload).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
                itemView.findViewById(R.id.download).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });


                whatsapp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getAdapterPosition() == -1) return;
                        FeedObject feed = listDataProfile.get(getAdapterPosition());
                        if (!TextUtils.isEmpty(feed.text1) && !feed.text1.equalsIgnoreCase("null")) {

                            try {
                                JSONObject jsonObject = new JSONObject(feed.text1);

                                Intent shareIntent = new Intent();
                                shareIntent.setAction(Intent.ACTION_SEND);
                                shareIntent.setPackage("com.whatsapp");

                                shareIntent.putExtra(Intent.EXTRA_TEXT, jsonObject.getString("txt") + "\n\n*"+getString(R.string.share_app)+"*");
                                shareIntent.setType("text/plain");

                                startActivity(Intent.createChooser(shareIntent, "Share to:"));
                            } catch (JSONException e) {
                                return;
                            }


                        } else {
                        }
                    }
                });

                container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getAdapterPosition() == -1) return;

                    }
                });
                itemView.findViewById(R.id.notifications).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getAdapterPosition() == -1) return;
                        clickedItem = getAdapterPosition();


                    }
                });



            }
        }


    }

    int clickedItem = -1;

    class ViewHolderLikes extends RecyclerView.ViewHolder {
        ImageView imv;

        public ViewHolderLikes(View itemView, FeedsAdapter.ViewHolder holder, int pos) {
            super(itemView);
            imv = (ImageView) itemView.findViewById(R.id.ic_img);
            //  imv.getLayoutParams().height = Screen.dp(30);
            //imv.getLayoutParams().width = Screen.dp(30);
            final int fpos = pos;
            imv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("ding dong tkkkkknp ViewHolderLikes pos *****" + pos + " fpoa" + fpos);
                    //likeAnimation( getAdapterPosition());
                    likeAnimation(getAdapterPosition(), fpos, holder);
                }
            });
        }
    }

    int[] arrLikes = {R.drawable.ic_like, R.drawable.ic_haha, R.drawable.ic_love, R.drawable.ic_sad, R.drawable.ic_wow, R.drawable.ic_angry, R.drawable.ic_ya};

    void likeAnimation(int reaction, int pos, FeedsAdapter.ViewHolder holder) {
        View itemView = holder.itemView;
updateStatusReaction(""+reaction,pos);

        DotsView vDotsView;
        CircleViewAnim vCircle;
        ImageView ivStar;
        vDotsView = (DotsView) itemView.findViewById(R.id.vDotsView);
        vCircle = (CircleViewAnim) itemView.findViewById(R.id.vCircle);
        ivStar = (ImageView) itemView.findViewById(R.id.ivStar);

        ivStar.setImageResource(arrLikes[reaction]);
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


                        //   updateStatusReaction();
                        //addToFav();
                    }
                });
            }
        });
        animatorSet.start();
    }

    ProgressDialog pDialog;

    void updateStatusReaction(String reaction, int pos) {

        if (getActivity() == null) return;
        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage("Sending Reaction...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        pDialog.show();



        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reaction", reaction)
                .addFormDataPart("post_id", listDataProfile.get(pos)._id)
                .addFormDataPart("ct_id", listDataProfile.get(pos).uid)
                .addFormDataPart("uid", GetSet.getUserId())
                .addFormDataPart("uPhone", GetSet.getphonenumber())
                .addFormDataPart("uname", GetSet.getUserName())
                .addFormDataPart("upic", GetSet.getImageUrl()==null?"":GetSet.getImageUrl())
                .build();

        new ExecuteServices().executePost(Constants.NODE_URL+"/set_feed_reaction_Hiddy", new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                //              if (this == null) return;
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pDialog.dismiss();
                        try {
                            JSONObject obj = new JSONObject(response);

                            if (obj.getBoolean("success")) {

                                if (obj.getInt("data") == 1) {
                                    listDataProfile.get(pos).reaction_count = String.valueOf(Integer.parseInt(listDataProfile.get(pos).reaction_count) + 1);
                                    list.getAdapter().notifyItemChanged(pos);
                                }
                            }
                            //Toast.makeText(this, "d" + obj.getString("data"), Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });


            }

            @Override
            public void onServiceExecutedFailed(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pDialog.dismiss();
                    }
                });

            }
        }, requestBody);
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
            if (getActivity() == null) {
                return;
            }
            pDialog2 = new ProgressDialog(getActivity());
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
                // root = new File(externalPath + "/"+ ActorSDK.sharedActor().getDirectory()+" /.IM sent/");
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
            //Uri contentUri = FileProvider.getUriForFile(ApplicationClass.getInstance(), ApplicationClass.getInstance().getApplicationContext().getPackageName() + ".my.package.name.provider", new File(file_url));
            pDialog2.dismiss();

            if (file_url != null) {
                File file = new File(file_url);
                Uri uri = Uri.fromFile(file);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() == null) {
                            return;
                        }
                        if (file.getName().startsWith("vid_")) {

                            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            intent.setData(Uri.fromFile(file));
                            getActivity().sendBroadcast(intent);
                        } else {
                           /*     MediaStore.Images.Media.insertImage(getActivity().getContentResolver(),
                                        file_url, file.getName(), null);*/
                            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            intent.setData(Uri.fromFile(file));
                            getActivity().sendBroadcast(new Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                        }


                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(Uri.parse(file_url));
                        getActivity().sendBroadcast(intent);

                    }
                }, 200);

            }
        }

    }

}