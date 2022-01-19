package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by hitasoft on 5/7/18.
 */

public class BlocksData {

    @SerializedName("status")
    public String status;

    @SerializedName("blockedbyme")
    public ArrayList<Blockedbyme> blockedbyme = new ArrayList<>();

    @SerializedName("blockedme")
    public ArrayList<Blockedme> blockedme = new ArrayList<>();

    public class Blockedbyme {
        @SerializedName("groupId")
        public String _id;

        @SerializedName("buser_id")
        public String buser_id;

        @SerializedName("__v")
        public String __v;

        @SerializedName("user_id")
        public String user_id;
    }

    public class Blockedme {
        @SerializedName("groupId")
        public String _id;

        @SerializedName("buser_id")
        public String buser_id;

        @SerializedName("__v")
        public String __v;

        @SerializedName("user_id")
        public String user_id;
    }
}
