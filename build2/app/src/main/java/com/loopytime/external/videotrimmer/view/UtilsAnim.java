package com.loopytime.external.videotrimmer.view;

/**
 * Created by Miroslaw Stanek on 21.12.2015.
 */
public class UtilsAnim {
    public static double mapValueFromRangeToRange(double value, double fromLow, double fromHigh, double toLow, double toHigh) {
        return toLow + ((value - fromLow) / (fromHigh - fromLow) * (toHigh - toLow));
    }

    public static double clamp(double value, double low, double high) {
        return Math.min(Math.max(value, low), high);
    }
}
