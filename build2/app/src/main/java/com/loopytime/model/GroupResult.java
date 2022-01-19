package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class GroupResult {
    @SerializedName("status")
    public String status;
    @SerializedName("result")
    public List<GroupData> result;

}
