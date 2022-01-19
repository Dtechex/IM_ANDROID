package com.loopytime.model;

import com.google.gson.annotations.SerializedName;

public class LanguageData {

    @SerializedName("languageId")
    public String languageId;

    @SerializedName("languageCode")
    public String languageCode;

    @SerializedName("language")
    public String language;

    @SerializedName("isSelected")
    public boolean isSelected;
}
