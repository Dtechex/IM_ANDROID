package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by hitasoft on 20/6/18.
 */

public class MessagesData implements Serializable {

    @SerializedName("chat_id")
    public String chat_id;

    @SerializedName("message_id")
    public String message_id;

    @SerializedName("user_id")
    public String user_id;

    @SerializedName("user_name")
    public String user_name;

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

    @SerializedName("chat_time")
    public String chat_time;

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

    @SerializedName("isDelete")
    public String isDelete;

    @SerializedName("statusData")
    public String statusData;
}
