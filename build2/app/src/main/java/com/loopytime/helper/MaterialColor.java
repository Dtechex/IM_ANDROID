package com.loopytime.helper;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.loopytime.im.R;

public enum MaterialColor {
  DEF       (R.color.cyan_500,        R.color.cyan_600,      R.color.cyan_400,         R.color.cyan_700,        "cyan"),

  YELLOW     (R.color.yellow_500,      R.color.yellow_600,    R.color.yellow_500,      R.color.yellow_700,      "yellow"),
  LIME       (R.color.lime_500,        R.color.lime_600,      R.color.lime_400,       R.color.lime_700,        "lime"),
  BLUE       (R.color.blue_500,        R.color.blue_600,      R.color.blue_400,        R.color.blue_700,        "blue"),
  RED        (R.color.red_500,         R.color.red_600,       R.color.red_400,        R.color.red_700,         "red"),
  PINK       (R.color.pink_400,        R.color.pink_500,      R.color.pink_300,       R.color.pink_700,        "pink"),
  PURPLE     (R.color.purple_400,      R.color.purple_500,    R.color.purple_300,      R.color.purple_700,      "purple"),
  DEEP_PURPLE(R.color.deep_purple_400, R.color.deep_purple_500, R.color.deep_purple_300, R.color.deep_purple_700, "deep_purple"),
  INDIGO     (R.color.indigo_400,      R.color.indigo_500,    R.color.indigo_300,      R.color.indigo_700,      "indigo"),
  LIGHT_BLUE (R.color.light_blue_500,  R.color.light_blue_600, R.color.light_blue_400,  R.color.light_blue_700,  "light_blue"),
  CYAN       (R.color.primary,         R.color.action,         R.color.action_pressed,        R.color.primary_hovered,         "def"),
  TEAL       (R.color.teal_500,        R.color.teal_600,      R.color.teal_400,        R.color.teal_700,        "teal"),
  LIGHT_GREEN(R.color.light_green_600, R.color.light_green_600, R.color.light_green_400, R.color.light_green_700, "light_green"),
  GREEN      (R.color.green_500,       R.color.green_600,     R.color.green_400,       R.color.green_700,       "green"),
  AMBER      (R.color.amber_600,       R.color.amber_600,     R.color.amber_400,      R.color.amber_700,       "amber"),
  ORANGE     (R.color.orange_500,      R.color.orange_600,    R.color.orange_400,      R.color.orange_700,      "orange"),
  DEEP_ORANGE(R.color.deep_orange_500, R.color.deep_orange_600, R.color.deep_orange_400, R.color.deep_orange_700, "deep_orange"),
  BROWN      (R.color.brown_500,       R.color.brown_600,        R.color.brown_400,       R.color.brown_700,       "brown"),
  GREY       (R.color.grey_600,        R.color.grey_600,      R.color.grey_500,        R.color.grey_700,        "grey"),
  BLUE_GREY  (R.color.blue_grey_600,   R.color.blue_grey_600,  R.color.blue_grey_500,   R.color.blue_grey_700,   "blue_grey"),
    BLACK  (R.color.black_500,   R.color.black_600,  R.color.black_400,   R.color.black_700,   "black"),
    PINKC  (R.color.pinkC_500,   R.color.pinkC_600,  R.color.pinkC_400,   R.color.pinkC_700,   "pink_c"),
    CUS  (R.color.cus_500,   R.color.cus_600,  R.color.cus_400,   R.color.cus_700,   "custom_1"),
    CUS2  (R.color.cus2_500,   R.color.cus2_600,  R.color.cus2_400,   R.color.cus2_700,   "custom_2"),
    CUS3  (R.color.cus3_500,   R.color.cus3_600,  R.color.cus3_400,   R.color.cus3_700,   "custom_3");

  /*GROUP      (GREY.conversationColorLight, R.color.textsecure_primary, R.color.textsecure_primary_dark,
              GREY.conversationColorDark, R.color.gray95, R.color.black,
              "group_color");*/

  private final int conversationColorLight;
  private final int conversationColorPLight;
  private final int actionBarColorLight;
  private final int statusBarColorLight;
  private final int conversationColorDark;
  private final int conversationColorPDark;
  private final int actionBarColorDark;
  private final int statusBarColorDark;

  private final String serialized;

  MaterialColor(int conversationColorLight,int conversationColorPLight, int actionBarColorLight,
                int statusBarColorLight, int conversationColorDark,
                int actionBarColorDark, int statusBarColorDark,
                String serialized)
  {
    this.conversationColorLight = conversationColorLight;
    this.conversationColorPLight = conversationColorPLight;
    this.actionBarColorLight    = actionBarColorLight;
    this.statusBarColorLight    = statusBarColorLight;
    this.conversationColorDark  = conversationColorDark;
    this.conversationColorPDark  = conversationColorPLight;
    this.actionBarColorDark     = actionBarColorDark;
    this.statusBarColorDark     = statusBarColorDark;
    this.serialized             = serialized;
  }

  MaterialColor(int lightColor, int darkColor,int darkColor2, int statusBarColor, String serialized) {
    this(darkColor,darkColor2, lightColor, statusBarColor, darkColor, darkColor, statusBarColor, serialized);
  }

    public int toConversationColor(@NonNull Context context) {
        // if (getAttribute(context, R.attr.theme_type, "light").equals("dark")) {
        return context.getResources().getColor(conversationColorLight);
  /*  } else {
      return context.getResources().getColor(conversationColorLight);
    }*/
    }
    public int toConversationPColor(@NonNull Context context) {
        // if (getAttribute(context, R.attr.theme_type, "light").equals("dark")) {
        return ContextCompat.getColor(context, conversationColorPLight);
  /*  } else {
      return context.getResources().getColor(conversationColorLight);
    }*/
    }

  public int toActionBarColor(@NonNull Context context) {
   // if (getAttribute(context, R.attr.theme_type, "light").equals("dark")) {
      return ContextCompat.getColor(context, actionBarColorLight);
    /*} else {
      return context.getResources().getColor(actionBarColorLight);
    }*/
  }

  public int toStatusBarColor(@NonNull Context context) {
    //if (getAttribute(context, R.attr.theme_type, "light").equals("dark")) {
      return ContextCompat.getColor(context, statusBarColorLight);
    /*} else {
      return context.getResources().getColor(statusBarColorLight);
    }*/
  }

  public boolean represents(Context context, int colorValue) {
    return ContextCompat.getColor(context, conversationColorDark)  == colorValue ||
            ContextCompat.getColor(context, conversationColorLight) == colorValue ||
            ContextCompat.getColor(context, actionBarColorDark) == colorValue ||
            ContextCompat.getColor(context, actionBarColorLight) == colorValue ||
            ContextCompat.getColor(context, statusBarColorLight) == colorValue ||
            ContextCompat.getColor(context, statusBarColorDark) == colorValue;
  }

  public String serialize() {
    return serialized;
  }

  private String getAttribute(Context context, int attribute, String defaultValue) {
    TypedValue outValue = new TypedValue();

    if (context.getTheme().resolveAttribute(attribute, outValue, true)) {
      return outValue.coerceToString().toString();
    } else {
      return defaultValue;
    }
  }


  public static MaterialColor fromSerialized(String serialized) throws UnknownColorException {
    for (MaterialColor color : MaterialColor.values()) {
      if (color.serialized.equals(serialized)) return color;
    }

    throw new UnknownColorException("Unknown color: " + serialized);
  }

  public static class UnknownColorException extends Exception {
    public UnknownColorException(String message) {
      super(message);
    }
  }

}
