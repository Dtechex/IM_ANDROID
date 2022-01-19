package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class GroupUpdateResult {
    @SerializedName("status")
    public String status;
    @SerializedName("result")
    public GroupUpdate result;

    public class GroupUpdate {
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
        public List<GroupData.GroupMembers> groupMembers;

    }
}
