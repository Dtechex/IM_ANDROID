package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by hitasoft on 29/7/18.
 */

public class CallData {

    public List<Result> result;
    public String status;

    public class Result {
        @SerializedName("call_id")
        public String callId;
        @SerializedName("user_id")
        public String userId;
        @SerializedName("type")
        public String type;
        @SerializedName("call_status")
        public String callStatus;
        @SerializedName("created_at")
        public String createdAt;
        @SerializedName("caller_id")
        public String callerId;
    }

}
