package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class StatusDatas implements Serializable {

    @SerializedName("attachment")
    public String mAttachment;
    @SerializedName("status_time")
    public String mStatusTime;
    @SerializedName("expiry_time")
    public String mExpiryTime;
    @SerializedName("message")
    public String mMessage;
    @SerializedName("type")
    public String mType;
    @SerializedName("status_id")
    public String mStatusId;
    @SerializedName("sender_id")
    public String mSenderId;
    @SerializedName("user_name")
    public String mUserName;
    @SerializedName("user_image")
    public String mUserImage;
    @SerializedName("thumbnail")
    public String mThumbnail;
    @SerializedName("isSeen")
    public String mIsSeen;
    @SerializedName("members")
    public String mMember;

    public class Status {
        @SerializedName("attachment")
        public String attachment;
        @SerializedName("message")
        public String message;
        @SerializedName("receiver_id")
        public String receiverId;
        @SerializedName("sender_id")
        public String senderId;
        @SerializedName("story_id")
        public String storyId;
        @SerializedName("story_type")
        public String storyType;
        @SerializedName("thumbnail")
        public String mThumbnail;
    }


}
