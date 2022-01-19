package com.loopytime.im;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.loopytime.external.videotrimmer.utils.Screen;
import com.loopytime.external.videotrimmer.view.BubbleContainerFeed;
import com.loopytime.im.status.FeedCommentObject;
import com.loopytime.utils.GetSet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import io.supercharge.shimmerlayout.ShimmerLayout;

import static com.loopytime.im.WhatStatusFragmentFeedProfile.formatDate;


public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
    Context mContext;
    Calendar c = Calendar.getInstance();
    Drawable dr, dr2;
    ArrayList<FeedCommentObject> dataList;
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

   public interface OnCommentListener {
        void onCommentClicked(FeedCommentObject obj, int insertId);

        void setDataSubCommentMore(int position, FeedCommentObject obj);

        void deleteComment(int position, String commentID);
    }

    OnCommentListener mOnCommentListener;
    boolean loaded = false, isMyPost = false;

    public CommentsAdapter(Context mContext, ArrayList<FeedCommentObject> dataList, boolean loaded, boolean isMyPost) {
        this.mContext = mContext;
        this.isMyPost = isMyPost;
        this.loaded = loaded;
        this.dataList = dataList;
        mOnCommentListener = (OnCommentListener) mContext;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(!loaded ? R.layout.comment_item_shimmer : R.layout.adapter_feed_text, parent, false));
    }

    void loadPreviousSubComment(TextView tv, int position, String pid) {
        if (position > 0 && !FeedDetailActivity.noLoadSubComment.contains(pid) && position + 3 < dataList.size() && !pid.equalsIgnoreCase("-1") && dataList.get(position - 1).parent_id.equalsIgnoreCase("-1")
                && dataList.get(position + 1).parent_id.equalsIgnoreCase(pid) && dataList.get(position + 2).parent_id.equalsIgnoreCase(pid)
                && dataList.get(position + 3).parent_id.equalsIgnoreCase(pid)) {
            tv.setVisibility(View.VISIBLE);
        } else
            tv.setVisibility(View.GONE);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!loaded) {
            ((ShimmerLayout) holder.shimmer).startShimmerAnimation();
            return;
        }
        FeedCommentObject feed = dataList.get(position);
        try {

            c.setTimeInMillis(format.parse(feed.created_at).getTime() + Calendar.getInstance().get(Calendar.ZONE_OFFSET));
            holder.time.setText(formatDate(c, true));
        } catch (ParseException e) {
        }
        if (position > 0 && !feed.parent_id.equalsIgnoreCase("-1") && dataList.get(position - 1).parent_id.equalsIgnoreCase("-1")) {
            holder.load_prev_subcomment.setVisibility(View.VISIBLE);
        } else
            holder.load_prev_subcomment.setVisibility(View.GONE);

        holder.container.setPadding(0,0,(!feed.parent_id.equalsIgnoreCase("-1")? Screen.dp(4):0),0);
        holder.text.setPadding(0,0,(!feed.parent_id.equalsIgnoreCase("-1")?Screen.dp(8):0),0);
        holder.container.showChildComment(!feed.parent_id.equalsIgnoreCase("-1"));
        holder.container.makeInboundBubble(true, feed.uname, feed.upic);
        holder.text.setText(feed.comment);
        holder.tv_name.setText(feed.uname);
        loadPreviousSubComment(holder.load_prev_subcomment, position, feed.parent_id);

        int colorm = Color.parseColor("#b9e9e8");
        if (!feed.parent_id.equalsIgnoreCase("-1")) {

            dr = ContextCompat.getDrawable(mContext, R.drawable.bubble_in);
            dr.mutate().setColorFilter(colorm, PorterDuff.Mode.SRC_IN);
            holder.messageBubble.setBackgroundDrawable(dr);
        } else {
            colorm = Color.parseColor("#C9ECFC");
            dr2 = ContextCompat.getDrawable(mContext, R.drawable.bubble_in);
            dr2.mutate().setColorFilter(colorm, PorterDuff.Mode.SRC_IN);
            holder.messageBubble.setBackgroundDrawable(dr2);
        }

        //int colorm = Color.parseColor("#C9ECFC");// c6f4f3#C9ECFC: Color.WHITE;//ColorUtils.compositeColors(ColorUtils.setAlphaComponent(Color.parseColor("#ff9800"),110),Color.WHITE):Color.WHITE;
        //dr.mutate().setColorFilter(colorm, PorterDuff.Mode.SRC_IN);
        //holder.messageBubble.setBackgroundDrawable(dr);
    }

    @Override
    public int getItemCount() {
        return !loaded ? 10 : dataList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_name, text, time, load_prev_subcomment;
        FrameLayout messageBubble;
        BubbleContainerFeed container;
        View shimmer;

        public ViewHolder(View itemView) {
            super(itemView);
            if (!loaded) {
                shimmer = itemView.findViewById(R.id.shimmer);
                return;
            }
            container = (BubbleContainerFeed) itemView.findViewById(R.id.mainContainer);
            text = (TextView) itemView.findViewById(R.id.tv_text);
            load_prev_subcomment = (TextView) itemView.findViewById(R.id.load_prev_subcomment);
            //text.setTextColor(ActorSDK.sharedActor().style.getConvTextColor());
            //text.setTypeface(Fonts.regular());
            tv_name = itemView.findViewById(R.id.tv_name);
            time = (TextView) itemView.findViewById(R.id.tv_time);
            //ActorSDK.sharedActor().style.getConvTimeColor();
            itemView.findViewById(R.id.stateIcon).setVisibility(View.GONE);
            //time.setTypeface(Fonts.regular());
            //time.setTextColor(ActorSDK.sharedActor().style.getConvTimeColor());
            messageBubble = (FrameLayout) itemView.findViewById(R.id.fl_bubble);
            container.hideDate();
            container.hideUnread();
            load_prev_subcomment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnCommentListener.setDataSubCommentMore(getAdapterPosition(), dataList.get(getAdapterPosition()));
                }
            });

            container.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (isMyPost ||dataList.get(getAdapterPosition()).uid.equalsIgnoreCase(GetSet.getUserId())) {
                        new AlertDialog.Builder(mContext).setMessage(R.string.alert_delete_comment)
                                .setPositiveButton(R.string.alert_delete_chat_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mOnCommentListener.deleteComment(getAdapterPosition(), dataList.get(getAdapterPosition())._id);
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
                    return true;
                }
            });


            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = getAdapterPosition();
                    for (int i = getAdapterPosition(); i < dataList.size(); i++) {
                        if (i + 1 == dataList.size()) {
                            id = i + 1;
                            break;
                        }
                        System.out.println("kappif " + dataList.get(i + 1).parent_id);
                        if (dataList.get(i + 1).parent_id.equalsIgnoreCase("-1")) {
                            System.out.println("kappi " + dataList.get(i + 1).parent_id + "**" + i);
                            id = i + 1;
                            break;
                        }
                    }
                    mOnCommentListener.onCommentClicked(dataList.get(getAdapterPosition()), id);
                }
            });


        }
    }
}
