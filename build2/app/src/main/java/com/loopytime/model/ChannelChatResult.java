package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ChannelChatResult {
    @SerializedName("status")
    public String status;
    @SerializedName("result")
    public List<ChannelMessage> result;
}
