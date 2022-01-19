
package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ChannelResult {

    public List<Result> result;
    public String status;

    public class Result {

        @SerializedName("_id")
        public String channelId;
        @SerializedName("channel_adminId")
        public String channelAdminId;
        @SerializedName("channel_admin_id")
        public String adminId;
        @SerializedName("channel_admin")
        public String channelAdminName;
        @SerializedName("channel_des")
        public String channelDes;
        @SerializedName("channel_image")
        public String channelImage;
        @SerializedName("channel_name")
        public String channelName;
        @SerializedName("channel_type")
        public String channelType;
        @SerializedName("created_time")
        public String createdTime;
        @SerializedName("created_at")
        public String createdAt;
        @SerializedName("total_subscribers")
        public String totalSubscribers;
        @SerializedName("mute_notification")
        public String muteNotification;
        @SerializedName("channel_category")
        public String channelCategory;
        @SerializedName("subscribe_status")
        public String subscribeStatus;
        @SerializedName("block_status")
        public String blockStatus;
        @SerializedName("report")
        public String report;


    }

}
