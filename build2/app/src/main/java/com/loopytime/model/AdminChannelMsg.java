package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AdminChannelMsg {
    @SerializedName("status")
    public String status;
    @SerializedName("result")
    public List<Result> result;

    public class Result {
        @SerializedName("_id")
        public String messageId;
        @SerializedName("channel_id")
        public String channelId;
        @SerializedName("message")
        public String message;
        @SerializedName("message_type")
        public String messageType;
        @SerializedName("message_at")
        public String chatTime;
        @SerializedName("message_date")
        public String messageDate;
        @SerializedName("attachment")
        public String attachment;
        @SerializedName("thumbnail")
        public String thumbnail;
        @SerializedName("chat_type")
        public String chatType;
        @SerializedName("progress")
        public String progress;
    }
}