package com.loopytime.utils;

/****************
 *
 * @author 'Hitasoft Technologies'
 *
 * Description:
 * This class is used for get and set logged user data
 *
 * Revision History:
 * Version 1.0 - Initial Version
 *
 *****************/
public class GetSet {
    private static boolean isLogged = false;
    private static String userId = null;
    private static String token = null;
    private static String userName = null;
    private static String phonenumber = null;
    private static String countrycode = null;
    private static String countryname = null;
    private static String imageUrl = null;
    private static String about = null;
    private static String privacylastseen = null;
    private static String privacyprofileimage = null;
    private static String privacyabout = null;
    private static String applanguage = null;
    private static int playPos = 0;
    private static int groupPlayPos = 0;
    private static int channelPlayPos = 0;

    public static String getBday() {
        return bday;
    }

    public static void setBday(String bday) {
        GetSet.bday = bday;
    }

    public static String getGender() {
        return gender;
    }

    public static void setGender(String gender) {
        GetSet.gender = gender;
    }

    public static boolean isIsRandomOn() {
        return isRandomOn;
    }

    public static void setIsRandomOn(boolean isRandomOn) {
        GetSet.isRandomOn = isRandomOn;
    }

    private static String bday = null;
    private static String gender = null;
    private static String country = null;
    private static String city = null;

    public static String getCountry() {
        return country;
    }

    public static void setCountry(String country) {
        GetSet.country = country;
    }

    public static String getCity() {
        return city;
    }

    public static void setCity(String city) {
        GetSet.city = city;
    }

    private static boolean isRandomOn = false;
    private static String lastAttachment = "";

    public static String getCountryname() {
        return countryname;
    }

    public static void setCountryname(String countryname) {
        GetSet.countryname = countryname;
    }

    public static int getPlayPos() {
        return playPos;
    }

    public static void setPlayPos(int playPos) {
        GetSet.playPos = playPos;
    }

    public static int getGroupPlayPos() {
        return groupPlayPos;
    }

    public static void setGroupPlayPos(int groupPlayPos) {
        GetSet.groupPlayPos = groupPlayPos;
    }

    public static int getChannelPlayPos() {
        return channelPlayPos;
    }

    public static void setChannelPlayPos(int channelPlayPos) {
        GetSet.channelPlayPos = channelPlayPos;
    }

    public static String getLastAttachment() {
        return lastAttachment;
    }

    public static void setLastAttachment(String lastAttachment) {
        GetSet.lastAttachment = lastAttachment;
    }

    public static void Logingin(String userId, String phonenumber, String countrycode, String imageUrl, String token, String wallet) {
        GetSet.isLogged = true;
        GetSet.userId = userId;
        GetSet.phonenumber = phonenumber;
        GetSet.imageUrl = imageUrl;
        GetSet.countrycode = countrycode;
        GetSet.token = token;
    }


    public static boolean isLogged() {
        return isLogged;
    }

    public static void setLogged(boolean isLogged) {
        GetSet.isLogged = isLogged;
    }

    public static String getUserId() {
        return userId;
    }

    public static void setUserId(String userId) {
        GetSet.userId = userId;
    }

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        GetSet.token = token;
    }

    public static String getUserName() {
        return userName;
    }

    public static void setUserName(String userName) {
        GetSet.userName = userName;
    }

    public static void setphonenumber(String phonenumber) {
        GetSet.phonenumber = phonenumber;
    }

    public static String getphonenumber() {
        return phonenumber;
    }

    public static void setcountrycode(String countrycode) {
        GetSet.countrycode = countrycode;
    }

    public static String getcountrycode() {
        return countrycode;
    }

    public static String getImageUrl() {
        return imageUrl;
    }

    public static void setImageUrl(String imageUrl) {
        GetSet.imageUrl = imageUrl;
    }

    public static String getAbout() {
        return about;
    }

    public static void setAbout(String about) {
        GetSet.about = about;
    }

    public static String getPrivacylastseen() {
        return privacylastseen;
    }

    public static void setPrivacylastseen(String privacylastseen) {
        GetSet.privacylastseen = privacylastseen;
    }

    public static String getPrivacyprofileimage() {
        return privacyprofileimage;
    }

    public static void setPrivacyprofileimage(String privacyprofileimage) {
        GetSet.privacyprofileimage = privacyprofileimage;
    }

    public static String getPrivacyabout() {
        return privacyabout;
    }

    public static void setPrivacyabout(String privacyabout) {
        GetSet.privacyabout = privacyabout;
    }

    public static String getAppLanguage() {
        return applanguage;
    }

    public static void setAppLanguage(String appLanguage) {
        GetSet.applanguage = applanguage;
    }

    public static void logout() {
        GetSet.isLogged = false;
        GetSet.setUserId(null);
        GetSet.setUserName(null);
        GetSet.setImageUrl(null);
        GetSet.setcountrycode(null);
        GetSet.setphonenumber(null);
        GetSet.setToken(null);
        GetSet.setAbout(null);
        GetSet.setPrivacylastseen(null);
        GetSet.setPrivacyprofileimage(null);
        GetSet.setPrivacyabout(null);
        GetSet.setAppLanguage(null);
    }

}
