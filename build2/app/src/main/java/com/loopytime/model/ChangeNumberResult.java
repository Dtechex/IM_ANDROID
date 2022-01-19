package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

public class ChangeNumberResult {
    @SerializedName("status")
    public String status;
    @SerializedName("result")
    public ChangeNumber changeNumber;

    public class ChangeNumber {
        @SerializedName("user_id")
        public String userId;
        @SerializedName("phone_no")
        public String phoneNo;
        @SerializedName("country_code")
        public String countryCode;
        @SerializedName("user_name")
        public String userName;
    }
}
