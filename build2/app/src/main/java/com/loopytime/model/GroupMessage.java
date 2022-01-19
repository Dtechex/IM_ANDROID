
package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class GroupMessage implements Serializable {

    @SerializedName("group_id")
    public String groupId;
    @SerializedName("group_name")
    public String groupName;
    @SerializedName("groupImage")
    public String groupImage;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("message_id")
    public String messageId;
    @SerializedName("message_type")
    public String messageType;
    @SerializedName("message")
    public String message;
    @SerializedName("group_admin_id")
    public String groupAdminId;
    @SerializedName("member_id")
    public String memberId;
    @SerializedName("member_name")
    public String memberName;
    @SerializedName("member_no")
    public String memberNo;
    @SerializedName("attachment")
    public String attachment;
    @SerializedName("chat_time")
    public String chatTime;
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
    @SerializedName("delivery_status")
    public String deliveryStatus;
    @SerializedName("progress")
    public String progress;
    @SerializedName("thumbnail")
    public String thumbnail;
    @SerializedName("isDelete")
    public String isDelete;
}
