package com.loopytime.country;


import com.loopytime.im.ApplicationClass;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Strings {

    //Pull all links from the body for easy retrieval
    public static ArrayList<String> pullLinks(String text) {
        ArrayList<String> links = new ArrayList<String>();

        //String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
        String regex = "\\(?\\b(https?://|www[.]|ftp://|.)[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);

        while (m.find()) {
            String urlStr = m.group();

            if (urlStr.startsWith("(") && urlStr.endsWith(")")) {
                urlStr = urlStr.substring(1, urlStr.length() - 1);
            }

            links.add(urlStr);
        }

        return links;
    }



    private static final Map<Character, String> charMap = new HashMap<>();

    public static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    private static ThreadLocal<DateFormat> TIME_FORMATTER = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return android.text.format.DateFormat.getTimeFormat(ApplicationClass.getInstance());
        }
    };

    public static int diffDays(long a, long b) {
        Calendar calendar = CALENDAR.get();
        calendar.setTimeInMillis(a);
        int y1 = calendar.get(Calendar.YEAR);
        int m1 = calendar.get(Calendar.MONTH);
        int d1 = calendar.get(Calendar.DATE);
        calendar.setTimeInMillis(b);
        int y2 = calendar.get(Calendar.YEAR);
        int m2 = calendar.get(Calendar.MONTH);
        int d2 = calendar.get(Calendar.DATE);

        return y1 == y2 && m1 == m2 ? d2 - d1 : 100;
    }

    private static ThreadLocal<SimpleDateFormat> MONTH_FORMATTER = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("MMMM");
        }
    };

    private static ThreadLocal<SimpleDateFormat> DATE_FORMATTER = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("dd '%s'");
        }
    };

    private static ThreadLocal<SimpleDateFormat> DATE_YEAR_FORMATTER = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("dd '%s' ''yy");
        }
    };

    public static String formatDate(long date) {
        String month = MONTH_FORMATTER.get().format(date).toUpperCase();
        Calendar calendar = CALENDAR.get();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int currentYear = calendar.get(Calendar.YEAR);
        calendar.setTimeInMillis(date);

        if (calendar.get(Calendar.YEAR) == currentYear) {
            return String.format(DATE_FORMATTER.get().format(date), month);
        } else {
            return String.format(DATE_YEAR_FORMATTER.get().format(date), month);
        }
    }

    private static ThreadLocal<Calendar> CALENDAR = new ThreadLocal<Calendar>() {
        @Override
        protected Calendar initialValue() {
            return Calendar.getInstance();
        }
    };

    public static String formatTime2(long time) {

        return TIME_FORMATTER.get().format(time);
    }

    public static boolean areSameDays(long a, long b) {
        Calendar calendar = CALENDAR.get();
        calendar.setTimeInMillis(a);
        int y1 = calendar.get(Calendar.YEAR);
        int m1 = calendar.get(Calendar.MONTH);
        int d1 = calendar.get(Calendar.DATE);
        calendar.setTimeInMillis(b);
        int y2 = calendar.get(Calendar.YEAR);
        int m2 = calendar.get(Calendar.MONTH);
        int d2 = calendar.get(Calendar.DATE);

        return y1 == y2 && m1 == m2 && d1 == d2;
    }

    public static String transliterate(String string) {
        if (charMap.size() == 0) {
            synchronized (charMap) {
                if (charMap.size() == 0) {
                    charMap.put('а', "a");
                    charMap.put('б', "b");
                    charMap.put('в', "v");
                    charMap.put('г', "g");
                    charMap.put('д', "d");
                    charMap.put('е', "e");
                    charMap.put('ё', "e");
                    charMap.put('ж', "zh");
                    charMap.put('з', "z");
                    charMap.put('и', "i");
                    charMap.put('й', "i");
                    charMap.put('к', "k");
                    charMap.put('л', "l");
                    charMap.put('м', "m");
                    charMap.put('н', "n");
                    charMap.put('о', "o");
                    charMap.put('п', "p");
                    charMap.put('р', "r");
                    charMap.put('с', "s");
                    charMap.put('т', "t");
                    charMap.put('у', "u");
                    charMap.put('ф', "f");
                    charMap.put('х', "h");
                    charMap.put('ц', "c");
                    charMap.put('ч', "ch");
                    charMap.put('ш', "sh");
                    charMap.put('щ', "sh");
                    charMap.put('ъ', "'");
                    charMap.put('ы', "y");
                    charMap.put('ь', "'");
                    charMap.put('э', "e");
                    charMap.put('ю', "u");
                    charMap.put('я', "ya");

                    charMap.put('a', "а");
                    charMap.put('b', "б");
                    charMap.put('c', "ц");
                    charMap.put('d', "д");
                    charMap.put('e', "е");
                    charMap.put('f', "ф");
                    charMap.put('g', "г");
                    charMap.put('h', "х");
                    charMap.put('i', "и");
                    charMap.put('j', "дж");
                    charMap.put('k', "к");
                    charMap.put('l', "л");
                    charMap.put('m', "м");
                    charMap.put('n', "н");
                    charMap.put('o', "о");
                    charMap.put('p', "п");
                    charMap.put('q', "к");
                    charMap.put('r', "р");
                    charMap.put('s', "с");
                    charMap.put('t', "т");
                    charMap.put('u', "ю");
                    charMap.put('v', "в");
                    charMap.put('w', "в");
                    charMap.put('x', "кс");
                    charMap.put('y', "й");
                    charMap.put('z', "з");
                }
            }
        }
        StringBuilder transliteratedString = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            Character ch = string.charAt(i);
            String charFromMap = charMap.get(ch);
            if (charFromMap == null) {
                transliteratedString.append(ch);
            } else {
                transliteratedString.append(charFromMap);
            }
        }
        return transliteratedString.toString();
    }

}
