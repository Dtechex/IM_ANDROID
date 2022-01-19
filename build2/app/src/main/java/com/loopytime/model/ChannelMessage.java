
package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ChannelMessage implements Serializable {

    @SerializedName("attachment")
    public String attachment;
    @SerializedName("channel_id")
    public String channelId;
    @SerializedName("admin_id")
    public String channelAdminId;
    @SerializedName("channel_name")
    public String channelName;
    @SerializedName("chat_time")
    public String chatTime;
    @SerializedName("chat_type")
    public String chatType;
    @SerializedName("contact_country_code")
    public String contactCountryCode;
    @SerializedName("contact_name")
    public String contactName;
    @SerializedName("contact_phone_no")
    public String contactPhoneNo;
    @SerializedName("lat")
    public String lat;
    @SerializedName("lon")
    public String lon;
    @SerializedName("message")
    public String message;
    @SerializedName("message_id")
    public String messageId;
    @SerializedName("message_type")
    public String messageType;
    @SerializedName("progress")
    public String progress;
    @SerializedName("thumbnail")
    public String thumbnail;
    @SerializedName("delivery_status")
    public String deliveryStatus;
    @SerializedName("isDelete")
    public String isDelete;

}
