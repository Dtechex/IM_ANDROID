package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GroupData {
    @SerializedName("_id")
    public String groupId;
    @SerializedName("group_admin_id")
    public String groupAdminId;
    @SerializedName("group_name")
    public String groupName;
    @SerializedName("group_image")
    public String groupImage;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("mute_notification")
    public String muteNotification;
    @SerializedName("group_members")
    public List<GroupMembers> groupMembers;

public class GroupMembers {
        @SerializedName("member_id")
        public String memberId;
        @SerializedName("member_role")
        public String memberRole;
        @SerializedName("member_name")
        public String memberName;
        @SerializedName("member_picture")
        public String memberPicture;
        @SerializedName("member_no")
        public String memberNo;
        @SerializedName("member_about")
        public String memberAbout;
    }


}
