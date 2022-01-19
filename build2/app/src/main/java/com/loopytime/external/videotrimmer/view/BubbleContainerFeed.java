package com.loopytime.external.videotrimmer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.external.videotrimmer.utils.DateFormatting;
import com.loopytime.external.videotrimmer.utils.Screen;
import com.loopytime.im.R;

import de.hdodenhof.circleimageview.CircleImageView;


public class BubbleContainerFeed extends ViewGroup {

    private static final int MODE_LEFT = 0;
    private static final int MODE_RIGHT = 1;
    private static final int MODE_FULL = 2;
    private final Paint SELECTOR_PAINT = new Paint();
    private boolean showDateDiv;
    private boolean showUnreadDiv;
    private boolean isChildComment;
    private boolean showAvatar;
    private TextView dateDiv;
    private TextView unreadDiv;
    private CircleImageView avatarView;
    private int mode = MODE_FULL;
    private boolean isSelected;
    private OnAvatarClickListener onClickListener;
    private OnAvatarLongClickListener onLongClickListener;

    public BubbleContainerFeed(Context context) {
        super(context);
        init();
    }

    public BubbleContainerFeed(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BubbleContainerFeed(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        setWillNotDraw(false);

        SELECTOR_PAINT.setColor(getResources().getColor(R.color.selector_selected));
        SELECTOR_PAINT.setStyle(Paint.Style.FILL);

        // DATE

        showDateDiv = false;

        dateDiv = new TextView(getContext());
        dateDiv.setTextSize(12);
        //dateDiv.setTypeface(Fonts.regular());
        dateDiv.setIncludeFontPadding(false);
        dateDiv.setBackgroundResource(R.drawable.conv_bubble_date_bg);
        dateDiv.setGravity(Gravity.CENTER);
        dateDiv.setTextColor(Color.WHITE);

        if (!showDateDiv) {
            dateDiv.setVisibility(GONE);
        } else {
            dateDiv.setVisibility(VISIBLE);
        }

        addView(dateDiv, new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        // UNREAD

        showUnreadDiv = false;
        isChildComment = false;

        unreadDiv = new TextView(getContext());
        unreadDiv.setTextSize(13);
        //unreadDiv.setTypeface(Fonts.regular());
        unreadDiv.setIncludeFontPadding(false);
        unreadDiv.setBackgroundColor(Color.parseColor("#90000000"));
        unreadDiv.setGravity(Gravity.CENTER);
        unreadDiv.setTextColor(Color.WHITE);
        unreadDiv.setPadding(0, Screen.dp(6), 0, Screen.dp(6));
        unreadDiv.setText(R.string.chat_new_messages);

        if (!showUnreadDiv) {
            unreadDiv.setVisibility(GONE);
        } else {
            unreadDiv.setVisibility(VISIBLE);
        }

        addView(unreadDiv, new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // AVATAR
        avatarView = new CircleImageView(getContext());
        //avatarView.init(Screen.dp(36), 18);

        addView(avatarView, new MarginLayoutParams(Screen.dp(36), Screen.dp(36)));
    }

    public OnAvatarClickListener getOnClickListener() {
        return onClickListener;
    }

    public void setOnClickListener(OnAvatarClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnLongClickListener(OnAvatarLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public void makeFullSizeBubble() {
        mode = MODE_FULL;
        showAvatar = false;
        avatarView.setVisibility(GONE);
        findMessageView().setLayoutParams(new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        requestLayout();
    }

    public void makeOutboundBubble() {
        mode = MODE_RIGHT;
        showAvatar = false;
        avatarView.setVisibility(GONE);
        findMessageView().setLayoutParams(new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        requestLayout();
    }


    public void makeInboundBubble(boolean showAvatar, final String uid, String upic) {
        mode = MODE_LEFT;
        this.showAvatar = showAvatar;
        if (showAvatar) {
            try {
                //UserVM u = users().get(uid);
                avatarView.setVisibility(VISIBLE);

                Glide.with(this.getContext()).applyDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.temp)).load(upic).
                        into(avatarView);
            } catch (Exception e) {
                //avatarView.bind(null, uname, uid);
            }

        }
        findMessageView().setLayoutParams(new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public void showDate(long time) {
        showDateDiv = true;
        dateDiv.setText(DateFormatting.formatDate(time));
        dateDiv.setVisibility(VISIBLE);
        requestLayout();
    }

    public void hideDate() {
        if (showDateDiv) {
            dateDiv.setVisibility(GONE);
            showDateDiv = false;
            requestLayout();
        }


    }

    public void showChildComment(boolean show) {
        isChildComment = show;

        requestLayout();
    }


    public void showUnread() {
        if (!showUnreadDiv) {
            showUnreadDiv = true;
            unreadDiv.setVisibility(VISIBLE);
            requestLayout();
        }
    }

    public void hideUnread() {
        if (showUnreadDiv) {
            showUnreadDiv = false;
            unreadDiv.setVisibility(GONE);
            requestLayout();
        }
    }

    private View findMessageView() {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v != dateDiv && v != unreadDiv && v != avatarView) {
                return v;
            }
        }
        throw new RuntimeException("Unable to find bubble view!");
    }

    public void setBubbleSelected(boolean isSelected) {
        this.isSelected = isSelected;
        setSelected(isSelected);
        invalidate();
    }

    @Override
    public void setSelected(boolean selected) {
        if (selected != isSelected) {
            return;
        }
        super.setSelected(selected);
    }

    // Small hack for avoiding listview selection

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int topOffset = 0;

        View messageView = findMessageView();
        int padding = Screen.dp(8);

        topOffset += Screen.dp(8);
        if (showAvatar) {
            padding += Screen.dp(36);
        }
        measureChildWithMargins(messageView, widthMeasureSpec, padding, heightMeasureSpec, 0);

        if (showDateDiv) {
            measureChild(dateDiv, widthMeasureSpec, heightMeasureSpec);
            topOffset += Screen.dp(16) + dateDiv.getMeasuredHeight();
        }

        if (showUnreadDiv) {
            measureChild(unreadDiv, widthMeasureSpec, heightMeasureSpec);
            topOffset += Screen.dp(16) + unreadDiv.getMeasuredHeight();
        }

        if (showAvatar) {
            measureChild(avatarView, widthMeasureSpec, heightMeasureSpec);
        }

        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), messageView.getMeasuredHeight() + topOffset);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        int topOffset = 0;

        if (showUnreadDiv) {
            int w = unreadDiv.getMeasuredWidth();
            int h = unreadDiv.getMeasuredHeight();
            int dateLeft = (right - left - w) / 2;
            unreadDiv.layout(dateLeft, topOffset + Screen.dp(8), dateLeft + w, topOffset + Screen.dp(8) + h);
            topOffset += Screen.dp(16) + h;
        }

        if (showDateDiv) {
            int w = dateDiv.getMeasuredWidth();
            int h = dateDiv.getMeasuredHeight();
            int dateLeft = (right - left - w) / 2;
            dateDiv.layout(dateLeft, topOffset + Screen.dp(8), dateLeft + w, topOffset + Screen.dp(8) + h);
            topOffset += Screen.dp(16) + h;
        }
        View bubble = findMessageView();
        int bubh = bubble.getMeasuredHeight();
        int offChild = (isChildComment ? Screen.dp(38) : 0);
        if (showAvatar) {
            int w = avatarView.getMeasuredWidth();
            int h = avatarView.getMeasuredHeight();

            avatarView.layout(Screen.dp(12) +offChild,
                    bottom - top - bubh - Screen.dp(4),
                    Screen.dp(6) + w+offChild,
                    bottom - top + h - bubh - Screen.dp(4));
        }


        int w = bubble.getMeasuredWidth();


        if (mode == MODE_LEFT) {
            int leftOffset = 0;
            if (showAvatar) {
                leftOffset = Screen.dp(36) + offChild;
            }
            bubble.layout(leftOffset, topOffset, leftOffset + w, topOffset + bubh);
        } else if (mode == MODE_RIGHT) {
            bubble.layout(getMeasuredWidth() - w, topOffset, getMeasuredWidth(), topOffset + bubh);
        } else if (mode == MODE_FULL) {
            int bubbleLeft = (right - left - w) / 2;
            bubble.layout(bubbleLeft, topOffset, bubbleLeft + w, topOffset + bubh);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isSelected) {
            View bubble = findMessageView();
            canvas.drawRect(0, getHeight() - bubble.getHeight(),
                    getWidth(), getHeight(), SELECTOR_PAINT);
        }
    }

    public interface OnAvatarClickListener {
        void onAvatarClick(int uid);
    }

    public interface OnAvatarLongClickListener {
        void onAvatarLongClick(int uid);
    }

    public void setOnline(boolean online, boolean isBot) {
        // avatarView.setOnline(online, isBot);
    }
}
