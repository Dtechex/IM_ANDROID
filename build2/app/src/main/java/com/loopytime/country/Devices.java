package com.loopytime.country;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.loopytime.im.ApplicationClass;

import static com.loopytime.country.Strings.capitalize;

public class Devices {

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    public static String getDeviceCountry() {
        TelephonyManager tm = (TelephonyManager) ApplicationClass.getInstance().getSystemService(Context.TELEPHONY_SERVICE);

        String country = tm.getSimCountryIso();

        if (android.text.TextUtils.isEmpty(country)) {
            country = tm.getNetworkCountryIso();
        }

        if (android.text.TextUtils.isEmpty(country)) {
            country = ApplicationClass.getInstance().getResources().getConfiguration().locale.getCountry();
        }

        return country;
    }
}
