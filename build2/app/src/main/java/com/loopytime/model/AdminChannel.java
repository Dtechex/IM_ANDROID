package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AdminChannel {
    @SerializedName("status")
    public String status;
    @SerializedName("result")
    public List<Result> result;

    public class Result {
        @SerializedName("_id")
        public String channelId;
        @SerializedName("title")
        public String channelName;
        @SerializedName("description")
        public String channelDes;
        @SerializedName("created_time")
        public String createdTime;
        @SerializedName("created_at")
        public String createdAt;
        @SerializedName("channel_image")
        public String channelImage;
    }
}