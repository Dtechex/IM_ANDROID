package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class GroupChatResult {
    @SerializedName("status")
    public String status;
    @SerializedName("result")
    public List<GroupMessage> result;

}
