package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

public class SearchData {

    public int viewType;

    /*Recent Data*/
    @SerializedName("chat_id")
    public String chat_id;

    @SerializedName("message_id")
    public String message_id;

    @SerializedName("user_id")
    public String user_id;

    @SerializedName("user_name")
    public String user_name;

    @SerializedName("user_image")
    public String user_image;

    @SerializedName("message_type")
    public String message_type;

    @SerializedName("message")
    public String message;

    @SerializedName("attachment")
    public String attachment;

    @SerializedName("lat")
    public String lat;

    @SerializedName("lon")
    public String lon;

    @SerializedName("contact_name")
    public String contact_name;

    @SerializedName("contact_phone_no")
    public String contact_phone_no;

    @SerializedName("contact_country_code")
    public String contact_country_code;


    @SerializedName("receiver_id")
    public String receiver_id;

    @SerializedName("sender_id")
    public String sender_id;

    @SerializedName("delivery_status")
    public String delivery_status;

    @SerializedName("thumbnail")
    public String thumbnail;

    @SerializedName("progress")
    public String progress;

    /*Group Data*/
    @SerializedName("groupId")
    public String groupId;

    @SerializedName("group_name")
    public String groupName;

    @SerializedName("group_image")
    public String groupImage;

    @SerializedName("group_admin_id")
    public String groupAdminId;

    @SerializedName("member_id")
    public String memberId;

    @SerializedName("member_name")
    public String memberName;

    @SerializedName("member_no")
    public String memberNo;

    @SerializedName("phone_no")
    public String phone_no;

    @SerializedName("country_code")
    public String country_code;

    @SerializedName("privacy_last_seen")
    public String privacy_last_seen;

    @SerializedName("privacy_profile_image")
    public String privacy_profile_image;

    @SerializedName("privacy_about")
    public String privacy_about;

    @SerializedName("blockedme")
    public String blockedme;

    @SerializedName("blockedbyme")
    public String blockedbyme;

    /*ChannelData*/
    @SerializedName("channel_id")
    public String channelId;

    @SerializedName("channel_name")
    public String channelName;

    @SerializedName("channel_image")
    public String channelImage;

    @SerializedName("admin_id")
    public String channelAdminId;

    /*Comman Data*/
    @SerializedName("chat_time")
    public String chatTime;

    @SerializedName("unread_count")
    public String unreadCount;

    @SerializedName("id")
    public String id;

    @SerializedName("statusData")
    public String statusData;
}
