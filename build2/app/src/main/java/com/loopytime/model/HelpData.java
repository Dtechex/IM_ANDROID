
package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class HelpData implements Serializable {

    @SerializedName("faq")
    public List<Faq> faq;
    @SerializedName("status")
    public String status;
    @SerializedName("terms")
    public List<Term> terms;

    public class Faq implements Serializable {

        @SerializedName("_id")
        public String _id;
        @SerializedName("description")
        public String description;
        @SerializedName("title")
        public String title;
        @SerializedName("type")
        public String type;
    }

    public class Term implements Serializable {
        @SerializedName("_id")
        public String _id;
        @SerializedName("description")
        public String description;
        @SerializedName("title")
        public String title;
        @SerializedName("type")
        public String type;
        public int viewType;

    }
}
