package com.loopytime.utils;

import android.content.Context;
import android.provider.ContactsContract;

/**
 * Created by hitasoft on 12/3/18.
 */

public class Constants {

    public final static String BASE_URL = "http://indianmessenger.co.in:3002/";
//    public final static String BASE_URL = "http://192.168.1.127:3002/";

    public final static String SOCKETURL = "http://indianmessenger.co.in:8085";
//  public final static String SOCKETURL = "http://192.168.1.127:8085";
    public static final String APPRTC_URL = "http://indianmessenger.co.in:8080";
    public static final String NODE_URL = "http://indianmessenger.co.in:5040/";
    public static final String NODE_URL_GET = "http://indianmessenger.co.in:5000/";
    public static final  String FOLDER = "Indian-Messenger";
    public static final String USER_IMG_PATH = BASE_URL + "media/users/";
    public static final String CHAT_IMG_PATH = BASE_URL + "media/chats/";
    public static final String GROUP_IMG_PATH = BASE_URL + "media/chats/";
    public static final String CHANNEL_IMG_PATH = BASE_URL + "media/chats/";
    public static final String TAG_PLATFORM = "platform";
    public static final String TAG_RESULT = "result";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String TAG_THUMBNAIL = "thumbnail";
    public static final String TAG_PROGRESS = "progress";
    public static final String TAG_ISDELETE = "isDelete";
    public static final String TAG_MUTE_NOTIFICATION = "mute_notification";
    public static final String TAG_ADMIN = "1";
    public static final String TAG_MEMBER = "0";
    public static final String TAG_MY_CONTACTS = "mycontacts";
    public static final String TAG_EVERYONE = "everyone";
    public static final String TAG_NOBODY = "nobody";
    public static final String TAG_CHANNEL_NAME = "channel_name";
    public static final String TAG_CHANNEL_ID = "channel_id";
    public static final String TAG_CHANNEL_DES = "channel_des";
    public static final String TAG_CHANNEL_IMAGE = "channel_image";
    public static final String TAG_CHANNEL_TYPE = "channel_type";
    public static final String TAG_INVITE_SUBSCRIBERS = "invite_subscribers";
    public static final String TAG_PUBLIC = "public";
    public static final String TAG_PRIVATE = "private";
    public static final String TAG_ADMIN_ID = "admin_id";
    public static final String TAG_CHANNEL_ADMIN_ID = "channel_admin_id";
    public static final String TAG_CHANNEL_ADMIN_NAME = "channel_admin_name";
    public static final String TAG_TOTAL_SUBSCRIBERS = "total_subscribers";
    public static final String SentFileHolder = "created_at";
    public static final String TAG_CALL_ID = "call_id";
    public static final String TAG_CALLER_ID = "caller_id";
    public static final String TAG_CALL_STATUS = "call_status";
    public static final String TAG_CHANNEL_LIST = "channel_list";
    public static final String TAG_TIME_STAMP = "timestamp";
    public static final String TAG_SUBSCRIBE_STATUS = "subscribe_status";
    public static final String TAG_BLOCK_STATUS = "block_status";
    public static final String TAG_CHANNEL_CATEGORY = "channel_category";
    public static final String NEW = "new";
    public static final String TAG_FAVOURITED = "favourited";
    public static final String TAG_IS_SELECTED = "isSelected";
    public static final String TAG_STATUS_ID = "status_id";
    public static final String TAG_STATUS_TIME = "status_time";
    public static final String TAG_STATUS_TYPE = "status_type";
    public static final String TAG_STATUS_VIEW = "status_view";
    /*Used for Intent and other purpose*/
    public static final String TAG_LANGUAGE_CODE = "language_code";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_FRENCH = "fr";
    public static final String TAG_DEFAULT_LANGUAGE_CODE = LANGUAGE_ENGLISH;
    public static final String TAG_BLOCK = "block";
    public static final String TAG_UNBLOCK = "unblock";
    public static final String TAG_GROUP_LIST = "group_list";
    public static final String IS_FROM = "IS_FROM";
    public static final String IS_EDIT = "IS_EDIT";
    public static final String TAG_GROUP_INVITATION = "groupinvitation";
    public static final String TAG_CHANNEL_INVITATION = "channelinvitation";
    public static final String TAG_TITLE = "title";
    public static final String ID = "id";
    public static final String TAG_NOTIFICATION = "notification";
    public static final String TAG_REPORT = "report";
    public static final String TAG_FROM = "from";
    public static final String TAG_VIDEO = "video";
    public static final String TAG_AUDIO = "audio";
    public static final String TAG_SEND = "send";
    public static final String TAG_RECEIVE = "receive";
    public static final String TAG_ROOM_ID = "room_id";
    /*For Get Contacts*/
    public static final String[] PROJECTION = new String[]{
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.STARRED,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
            ContactsContract.CommonDataKinds.Contactables.DATA,
            ContactsContract.CommonDataKinds.Contactables.TYPE
    };
    public static final String SELECTION = ContactsContract.Data.MIMETYPE + " in (?, ?)" + " AND " +
            ContactsContract.Data.HAS_PHONE_NUMBER + " = '" + 1 + "'";
    public static final String[] SELECTION_ARGS = {
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
    };
    public static final String SORT_ORDER = ContactsContract.Contacts.SORT_KEY_ALTERNATIVE;
    // Table column name:
    public static String TAG_STATUS = "status";
    public static String TAG_PHONE_NUMBER = "phone_no";
    public static String TAG_COUNTRY_CODE = "country_code";
    public static String TAG_COUNTRY = "country";
    public static String TAG_USER_NAME = "user_name";
    public static String TAG_SAVED_NAME = "saved_name";
    public static String TAG_USER_IMAGE = "user_image";
    public static String TAG_ABOUT = "about";
    public static String TAG_PRIVACY_PROFILE = "privacy_profile_image";
    public static String TAG_PRIVACY_ABOUT = "privacy_about";
    public static String TAG_USER_ID = "user_id";
    public static String TAG_CONTACT_ID = "contact_id";
    public static String TAG_PRIVACY_LAST_SEEN = "privacy_last_seen";
    public static String TAG_CONTACTS = "contacts";
    public static String TAG_CONTACT_STATUS = "contactstatus";
    public static String TAG_SENDER_ID = "sender_id";
    public static String TAG_RECEIVER_ID = "receiver_id";
    public static String TAG_MESSAGE_ID = "message_id";
    public static String TAG_MESSAGE_TYPE = "message_type";
    public static String TAG_CHAT_TYPE = "chat_type";
    public static String TAG_MESSAGE = "message";
    public static String TAG_MESSAGE_DATA = "message_data";
    public static String TAG_ATTACHMENT = "attachment";
    public static String TAG_LAT = "lat";
    public static String TAG_LON = "lon";
    public static String TAG_CONTACT_NAME = "contact_name";
    public static String TAG_CONTACT_PHONE_NO = "contact_phone_no";
    public static String TAG_CONTACT_COUNTRY_CODE = "contact_country_code";
    public static String TAG_CHAT_TIME = "chat_time";
    public static String TAG_DELIVERY_STATUS = "delivery_status";
    public static String TAG_CHAT_ID = "chat_id";
    public static String TAG_UNREAD_COUNT = "unread_count";
    public static String TAG_BLOCKED_BYME = "blockedbyme";
    public static String TAG_BLOCKED_ME = "blockedme";
    public static String TAG_TYPE = "type";
    public static String TAG_ID = "_id";
    public static String TAG_GROUP_ID = "group_id";
    public static String TAG_GROUP_ADMIN_ID = "group_admin_id";
    public static String TAG_GROUP_NAME = "group_name";
    public static String TAG_GROUP_MEMBERS = "group_members";
    public static String TAG_GROUP_DESC = "group_description";
    public static String TAG_GROUP_IMAGE = "group_image";
    public static String TAG_GROUP_CREATED_BY = "group_created_by";
    public static String TAG_CREATED_AT = "created_at";
    public static String TAG_CREATED_TIME = "created_time";
    public static String TAG_SINGLE = "single";
    public static String TAG_GROUP = "group";
    public static String TAG_CHANNEL = "channel";
    public static String TAG_ADMIN_CHANNEL = "admin_channel";
    public static String TAG_USER_CHANNEL = "user_channel";
    public static String TAG_CALL = "call";
    public static String TAG_CALLS = "CALLS";
    public static String TAG_MEMBER_ID = "member_id";
    public static String TAG_MEMBER_NAME = "member_name";
    public static String TAG_MEMBER_PICTURE = "member_picture";
    public static String TAG_MEMBER_NO = "member_no";
    public static String TAG_MEMBER_ABOUT = "member_about";
    public static String TAG_MEMBER_ROLE = "member_role";
    public static String TAG_MEMBER_KEY = "member_key";
    public static String TAG_IS_DELETE = "is_delete";
    public static String TAG_STATUS_DATA = "status_data";
    public static int DIALOG_TIME = 5000;

    public static String TAG_STORY_ID = "story_id";
    public static String TAG_STORY_MEMBERS = "story_members";
    public static String TAG_STORY_TYPE = "story_type";
    public static String TAG_STORY_DATE = "story_date";
    public static String TAG_STORY_TIME = "story_time";
    public static String TAG_EXPIRY_TIME = "expiry_time";
    public static String TAG_DATA = "data";
    public static String IS_POST = "isPost";
    public static String TAG_MUTE_STATUS = "mute_status";
    public static String TAG_STORY_VIEWED = "story_viewed";
    public static String TAG_POSITION = "position";
    public static String TAG_IS_ALERT = "is_alert";
    public static String TAG_SUCCESS = "success";
    public static String TAG_IMAGE = "image";
    public static String TAG_NAV_HEIGHT = "nav_height";
    public static String TAG_STATUS_HEIGHT = "status_height";
    public static String TAG_CAMERA = "camera";
    public static String TAG_GALLERY = "gallery";
    public static String TAG_SENT = "sent";
    public static String TAG_SOURCE_TYPE = "source_type";

    /* minute * 1000 * seconds * milliseconds */
    public static final long STATUS_EXPIRY_TIME = 24 * 1000 * 60 * 60;
    public static final int STATUS_TEXT_CODE = 300;
    public static final int STATUS_VIDEO_CODE = 301;
    public static final int STATUS_IMAGE_CODE = 302;

    public static boolean isChatOpened = false, isGroupChatOpened = false, isChannelChatOpened = false, isExternalPlay = false;
    public static Context chatContext, groupContext, channelContext;

    public static long storyDuration = 6000;

    public static String SHARED_PREFERENCE = "SavedPref";
    public static String MESSAGE_CRYPT_KEY = "Hiddy123!@#";

    public final static String STATUS_WHATSAPP = "whatspp_shared";
    public final static String STATUS_WHATSAPP_PATH= "whatspp_vid_path";
    public final static String STATUS_WHATSAPP_COUNT = "whatspp_shared_status_count";
    public final static String RATING_SHOWN = "rating_shown";
}
