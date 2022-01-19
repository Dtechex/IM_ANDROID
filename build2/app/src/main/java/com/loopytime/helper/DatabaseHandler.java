package com.loopytime.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.loopytime.im.ApplicationClass;
import com.loopytime.im.GroupChatActivity;
import com.loopytime.im.GroupFragment;
import com.loopytime.im.R;
import com.loopytime.model.AdminChannel;
import com.loopytime.model.CallData;
import com.loopytime.model.ChannelMessage;
import com.loopytime.model.ChannelResult;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupData;
import com.loopytime.model.GroupMessage;
import com.loopytime.model.MediaModelData;
import com.loopytime.model.MessagesData;
import com.loopytime.model.StatusDatas;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.loopytime.utils.Constants.TAG_PRIVACY_ABOUT;
import static com.loopytime.utils.Constants.TAG_PRIVACY_LAST_SEEN;
import static com.loopytime.utils.Constants.TAG_PRIVACY_PROFILE;
import static com.loopytime.utils.Constants.TAG_USER_ID;

/**
 * Created by hitasoft on 30/5/18.
 */

public class DatabaseHandler extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "hidapp";
    // Table Names
    private static final String TABLE_CONTACTS = "contacts";
    private static final String TABLE_MESSAGES = "messages";
    private static final String TABLE_RECENTS = "recents";
    private static final String TABLE_GROUP = "group_table";
    private static final String TABLE_GROUP_MEMBERS = "group_members";
    private static final String TABLE_GROUP_MESSAGES = "group_messages";
    private static final String TABLE_GROUP_RECENT_MESSAGES = "group_recent_messages";
    private static final String TABLE_CHANNEL = "channel";
    private static final String TABLE_CHANNEL_MESSAGES = "channel_messages";
    private static final String TABLE_CHANNEL_RECENT_MESSAGES = "channel_recent_messages";
    private static final String TABLE_CALL = "call";
    private static final String TABLE_STATUS = "status";
    private static final String TABLE_STATUS_VIEW = "status_view";
    private static final String[] ALL_TABLES = new String[]{TABLE_CONTACTS, TABLE_MESSAGES,
            TABLE_RECENTS, TABLE_GROUP, TABLE_GROUP_MEMBERS, TABLE_GROUP_MESSAGES, TABLE_GROUP_RECENT_MESSAGES,
            TABLE_CHANNEL, TABLE_CHANNEL_MESSAGES, TABLE_CHANNEL_RECENT_MESSAGES, TABLE_CALL, TABLE_STATUS, TABLE_STATUS_VIEW};
    private static final String TAG = "DatabaseHandler";
    private static int DATABASE_VERSION = 2;
    private static DatabaseHandler sInstance;
    private StorageManager storageManager;

    /**
     * Constructor should be private to prevent direct instantiation.
     * Make a call to the static method "getInstance()" instead.
     */
    private DatabaseHandler(Context context) {
        super(context, context.getString(R.string.app_name) + DATABASE_NAME, null, DATABASE_VERSION);
        storageManager = StorageManager.getInstance(context);
    }

    public static synchronized DatabaseHandler getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHandler(context.getApplicationContext());
        }
        return sInstance;
    }

    // Called when the database is created for the FIRST time.
    // If a database already exists on disk with the same DATABASE_NAME, this method will NOT be called.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACT_TABLE = "CREATE TABLE " + TABLE_CONTACTS +
                "(" +
                Constants.TAG_USER_ID + " TEXT PRIMARY KEY," + Constants.TAG_USER_NAME + " TEXT," +
                Constants.TAG_PHONE_NUMBER + " TEXT," + Constants.TAG_COUNTRY_CODE + " TEXT," + Constants.TAG_USER_IMAGE + " TEXT," +
                TAG_PRIVACY_ABOUT + " TEXT," + TAG_PRIVACY_LAST_SEEN + " TEXT," + TAG_PRIVACY_PROFILE + " TEXT," +
                Constants.TAG_BLOCKED_ME + " TEXT," + Constants.TAG_BLOCKED_BYME + " TEXT," + Constants.TAG_ABOUT + " TEXT," +
                Constants.TAG_MUTE_NOTIFICATION + " TEXT," + Constants.TAG_CONTACT_STATUS + " TEXT," + Constants.TAG_SAVED_NAME + " TEXT," +
                Constants.TAG_MUTE_STATUS + " TEXT," + Constants.TAG_FAVOURITED + " TEXT" + ")";

        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES +
                "(" +
                Constants.TAG_CHAT_ID + " TEXT," + Constants.TAG_MESSAGE_ID + " TEXT PRIMARY KEY," + TAG_USER_ID + " TEXT," + Constants.TAG_USER_NAME + " TEXT," +
                Constants.TAG_MESSAGE_TYPE + " TEXT," + Constants.TAG_MESSAGE + " TEXT," + Constants.TAG_ATTACHMENT + " TEXT," +
                Constants.TAG_LAT + " TEXT," + Constants.TAG_LON + " TEXT," + Constants.TAG_CONTACT_NAME + " TEXT," +
                Constants.TAG_CONTACT_PHONE_NO + " TEXT," + Constants.TAG_CONTACT_COUNTRY_CODE + " TEXT," + Constants.TAG_CHAT_TIME + " TEXT," +
                Constants.TAG_RECEIVER_ID + " TEXT," + Constants.TAG_SENDER_ID + " TEXT," + Constants.TAG_DELIVERY_STATUS + " TEXT," + Constants.TAG_THUMBNAIL
                + " TEXT," + Constants.TAG_IS_DELETE + " TEXT," + Constants.TAG_STATUS_DATA + " TEXT," + Constants.TAG_PROGRESS + " TEXT" + ")";

        String CREATE_RECENT_TABLE = "CREATE TABLE " + TABLE_RECENTS +
                "(" +
                Constants.TAG_CHAT_ID + " TEXT PRIMARY KEY," + Constants.TAG_USER_ID + " TEXT," + Constants.TAG_MESSAGE_ID + " TEXT," +
                Constants.TAG_CHAT_TIME + " TEXT," + Constants.TAG_IS_DELETE + " TEXT," + Constants.TAG_UNREAD_COUNT + " TEXT" + ")";

        String CREATE_GROUP_TABLE = "CREATE TABLE " + TABLE_GROUP + "(" +
                Constants.TAG_GROUP_ID + " TEXT PRIMARY KEY," + Constants.TAG_GROUP_NAME + " TEXT, " + Constants.TAG_GROUP_CREATED_BY + " TEXT, " +
                Constants.TAG_CREATED_AT + " TEXT, " + Constants.TAG_GROUP_IMAGE + " TEXT, " + Constants.TAG_MUTE_NOTIFICATION + " TEXT" + ")";

        String CREATE_GROUP_MEMBERS_TABLE = "CREATE TABLE " + TABLE_GROUP_MEMBERS + "(" +
                Constants.TAG_MEMBER_KEY + " TEXT PRIMARY KEY," + Constants.TAG_GROUP_ID + " TEXT," + Constants.TAG_MEMBER_ID + " TEXT, " + Constants.TAG_MEMBER_ROLE + " TEXT " + ")";

        String CREATE_TABLE_GROUP_MESSAGES = "CREATE TABLE " + TABLE_GROUP_MESSAGES +
                "(" + Constants.TAG_MESSAGE_ID + " TEXT PRIMARY KEY," + Constants.TAG_GROUP_ID + " TEXT," + Constants.TAG_MEMBER_ID + " TEXT," + Constants.TAG_GROUP_ADMIN_ID + " TEXT," +
                Constants.TAG_MESSAGE_TYPE + " TEXT," + Constants.TAG_MESSAGE + " TEXT," + Constants.TAG_ATTACHMENT + " TEXT," + Constants.TAG_LAT + " TEXT," +
                Constants.TAG_LON + " TEXT," + Constants.TAG_CONTACT_NAME + " TEXT," + Constants.TAG_CONTACT_PHONE_NO + " TEXT," +
                Constants.TAG_CONTACT_COUNTRY_CODE + " TEXT," + Constants.TAG_CHAT_TIME + " TEXT," + Constants.TAG_DELIVERY_STATUS + " TEXT," + Constants.TAG_THUMBNAIL + " TEXT,"
                + Constants.TAG_IS_DELETE + " TEXT," + Constants.TAG_PROGRESS + " TEXT " + ")";

        String CREATE_TABLE_GROUP_RECENT_MESSAGES = "CREATE TABLE " + TABLE_GROUP_RECENT_MESSAGES +
                "(" +
                Constants.TAG_GROUP_ID + " TEXT PRIMARY KEY," + Constants.TAG_MESSAGE_ID + " TEXT," +
                Constants.TAG_MEMBER_ID + " TEXT," + Constants.TAG_CHAT_TIME + " TEXT," + Constants.TAG_IS_DELETE + " TEXT," + Constants.TAG_UNREAD_COUNT + " TEXT" + ")";

        String CREATE_TABLE_CHANNEL = "CREATE TABLE " + TABLE_CHANNEL +
                "(" +
                Constants.TAG_CHANNEL_ID + " TEXT PRIMARY KEY," + Constants.TAG_CHANNEL_NAME + " TEXT," +
                Constants.TAG_CHANNEL_DES + " TEXT," + Constants.TAG_CHANNEL_IMAGE + " TEXT," + Constants.TAG_CHANNEL_TYPE + " TEXT," +
                Constants.TAG_CHANNEL_ADMIN_ID + " TEXT," + Constants.TAG_CHANNEL_ADMIN_NAME + " TEXT," + Constants.TAG_TOTAL_SUBSCRIBERS + " TEXT," + Constants.TAG_CREATED_AT + " TEXT," +
                Constants.TAG_SUBSCRIBE_STATUS + " TEXT," + Constants.TAG_CHANNEL_CATEGORY + " TEXT," + Constants.TAG_MUTE_NOTIFICATION + " TEXT," + Constants.TAG_REPORT + " TEXT," + Constants.TAG_BLOCK_STATUS + " TEXT " + ")";

        String CREATE_TABLE_CHANNEL_MESSAGES = "CREATE TABLE " + TABLE_CHANNEL_MESSAGES +
                "(" + Constants.TAG_MESSAGE_ID + " TEXT PRIMARY KEY," + Constants.TAG_CHANNEL_ID + " TEXT," +
                Constants.TAG_CHAT_TYPE + " TEXT," + Constants.TAG_MESSAGE_TYPE + " TEXT," + Constants.TAG_MESSAGE + " TEXT," + Constants.TAG_ATTACHMENT + " TEXT," + Constants.TAG_LAT + " TEXT," +
                Constants.TAG_LON + " TEXT," + Constants.TAG_CONTACT_NAME + " TEXT," + Constants.TAG_CONTACT_PHONE_NO + " TEXT," +
                Constants.TAG_CONTACT_COUNTRY_CODE + " TEXT," + Constants.TAG_CHAT_TIME + " TEXT," + Constants.TAG_IS_DELETE + " TEXT," + Constants.TAG_DELIVERY_STATUS + " TEXT," + Constants.TAG_THUMBNAIL + " TEXT," + Constants.TAG_PROGRESS + " TEXT " + ")";

        String CREATE_TABLE_CHANNEL_RECENT_MESSAGES = "CREATE TABLE " + TABLE_CHANNEL_RECENT_MESSAGES +
                "(" +
                Constants.TAG_CHANNEL_ID + " TEXT PRIMARY KEY," + Constants.TAG_MESSAGE_ID + " TEXT," +
                Constants.TAG_CHAT_TIME + " TEXT," + Constants.TAG_IS_DELETE + " TEXT," + Constants.TAG_UNREAD_COUNT + " TEXT" + ")";

        String CREATE_CALL_TABLE = "CREATE TABLE " + TABLE_CALL + "(" +
                Constants.TAG_CALL_ID + " TEXT PRIMARY KEY," + Constants.TAG_USER_ID + " TEXT, " + Constants.TAG_TYPE + " TEXT, " + Constants.TAG_IS_ALERT + " TEXT, " +
                Constants.TAG_CALL_STATUS + " TEXT, " + Constants.TAG_CREATED_AT + " TEXT" + ")";


        String CREATE_STATUS_TABLE = "CREATE TABLE " + TABLE_STATUS + "(" +
                Constants.TAG_STATUS_ID + " TEXT PRIMARY KEY," + Constants.TAG_STATUS_TIME + " TEXT, " + Constants.TAG_EXPIRY_TIME + " TEXT, "
                + Constants.TAG_TYPE + " TEXT, " + Constants.TAG_ATTACHMENT + " TEXT, " + Constants.TAG_THUMBNAIL + " TEXT, "
                + Constants.TAG_MESSAGE + " TEXT, " + Constants.TAG_SENDER_ID + " TEXT, " + Constants.TAG_CHAT_ID + " TEXT, " + Constants.TAG_STORY_MEMBERS + " TEXT, "
                + Constants.TAG_STATUS_VIEW + " TEXT" + ")";

        String CREATE_STATUS_VIEW_TABLE = "CREATE TABLE " + TABLE_STATUS_VIEW + "(" + Constants.TAG_MEMBER_KEY + " TEXT PRIMARY KEY," +
                Constants.TAG_STATUS_ID + " TEXT," + Constants.TAG_STATUS_TIME + " TEXT, " + Constants.TAG_MEMBER_ID + " TEXT " + ")";

        db.execSQL(CREATE_CONTACT_TABLE);
        db.execSQL(CREATE_MESSAGES_TABLE);
        db.execSQL(CREATE_RECENT_TABLE);
        db.execSQL(CREATE_GROUP_TABLE);
        db.execSQL(CREATE_GROUP_MEMBERS_TABLE);
        db.execSQL(CREATE_TABLE_GROUP_MESSAGES);
        db.execSQL(CREATE_TABLE_GROUP_RECENT_MESSAGES);
        db.execSQL(CREATE_TABLE_CHANNEL);
        db.execSQL(CREATE_TABLE_CHANNEL_MESSAGES);
        db.execSQL(CREATE_TABLE_CHANNEL_RECENT_MESSAGES);
        db.execSQL(CREATE_CALL_TABLE);
        db.execSQL(CREATE_STATUS_TABLE);
        db.execSQL(CREATE_STATUS_VIEW_TABLE);
    }

    // Called when the database needs to be upgraded.
    // This method will only be called if a database already exists on disk with the same DATABASE_NAME,
    // but the DATABASE_VERSION is different than the version of the database that exists on disk.
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.e(TAG, "onUpgrade: " + oldVersion + " " + newVersion);
        if (oldVersion < DATABASE_VERSION) {
            dropTables(sqLiteDatabase);
        }
    }

    private void dropTables(SQLiteDatabase sqLiteDatabase) {
        for (String tableName : ALL_TABLES) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS '" + tableName + "'");
        }
        onCreate(sqLiteDatabase);
    }

    @Override
    public synchronized void close() {
        super.close();
    }

    // Insert a contact into the database
    public void addContactDetails(String savedName, String id, String name, String number, String countrycode,
                                  String image, String aboutprivacy, String lastseenprivacy,
                                  String profileprivacy, String about, String contactstatus) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            boolean exists = isUserExist(db, id);
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_USER_ID, id);
            values.put(Constants.TAG_USER_NAME, name);
            values.put(Constants.TAG_PHONE_NUMBER, number);
            values.put(Constants.TAG_COUNTRY_CODE, countrycode);
            values.put(Constants.TAG_USER_IMAGE, image);
            values.put(Constants.TAG_PRIVACY_ABOUT, aboutprivacy);
            values.put(Constants.TAG_PRIVACY_LAST_SEEN, lastseenprivacy);
            values.put(Constants.TAG_PRIVACY_PROFILE, profileprivacy);
            values.put(Constants.TAG_ABOUT, about);
            values.put(Constants.TAG_CONTACT_STATUS, contactstatus);
            values.put(Constants.TAG_SAVED_NAME, savedName);

            /*HashMap<String, String> map = ApplicationClass.getContactrNot(context, number);
            if (map.get("isAlready").equals("true"))
                values.put(Constants.TAG_SAVED_NAME,map.get(Constants.TAG_USER_NAME));*/

            if (exists) {
                db.update(TABLE_CONTACTS, values, Constants.TAG_USER_ID + " =? ",
                        new String[]{id});
            } else {
                values.put(Constants.TAG_BLOCKED_ME, "");
                values.put(Constants.TAG_BLOCKED_BYME, "");
                values.put(Constants.TAG_MUTE_NOTIFICATION, "");
                values.put(Constants.TAG_FAVOURITED, "false");
                db.insert(TABLE_CONTACTS, null, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateContactInfo(String userId, String key, String value) {
        if (isUserExist(userId)) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(key, value);
            db.update(TABLE_CONTACTS, values, Constants.TAG_USER_ID + " =? ",
                    new String[]{userId});
        }
    }

    public boolean isUserExist(SQLiteDatabase db, String id) {
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_CONTACTS + " WHERE " + Constants.TAG_USER_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    public boolean isGroupExist(SQLiteDatabase db, String groupId) {
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_GROUP + " WHERE " + Constants.TAG_GROUP_ID + "=?",
                new String[]{groupId});
        return line > 0;
    }

    public boolean isGroupExist(String groupId) {
        long line = DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + TABLE_GROUP + " WHERE " + Constants.TAG_GROUP_ID + "=?",
                new String[]{groupId});
        return line > 0;
    }

    public boolean isUserExist(String id) {
        long line = DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + TABLE_CONTACTS + " WHERE " + Constants.TAG_USER_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    boolean isContactSaved(String id) {

        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + Constants.TAG_USER_ID + "='" + id + "' AND "
                + Constants.TAG_STATUS_VIEW + "='true'";

        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        if (cursor != null)
            cursor.moveToFirst();

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            } else {
                cursor.close();
                return false;
            }
        } else
            return false;
    }

    public boolean isChannelExist(String channelId) {
        long line = DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + TABLE_CHANNEL + " WHERE " + Constants.TAG_CHANNEL_ID + "=?",
                new String[]{channelId});
        return line > 0;
    }

    public boolean isStoryExists(String storyId) {
        long line = DatabaseUtils.longForQuery(this.getWritableDatabase(), "SELECT COUNT(*) FROM " + TABLE_STATUS + " WHERE " + Constants.TAG_STATUS_ID + "=?",
                new String[]{storyId});
        return line > 0;
    }

    public boolean isStoryUserExists(String senderId) {
        long line = DatabaseUtils.longForQuery(this.getWritableDatabase(), "SELECT COUNT(*) FROM " + TABLE_STATUS + " WHERE " + Constants.TAG_SENDER_ID + "=?",
                new String[]{senderId});
        return line > 0;
    }

    public ContactsData.Result getContactDetail(String user_id) {
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + Constants.TAG_USER_ID + "='" + user_id + "'";

        SQLiteDatabase db = getReadableDatabase();
        ContactsData.Result results = new ContactsData().new Result();
        try {
            Cursor cursor = db.rawQuery(selectQuery, null);
            if (cursor != null) {
                cursor.moveToFirst();
                if (cursor.moveToFirst()) {
                    results.user_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID));
                    results.user_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_IMAGE));
                    results.phone_no = cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER));
                    results.country_code = cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE));
                    results.privacy_last_seen = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_LAST_SEEN));
                    results.privacy_profile_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_PROFILE));
                    results.privacy_about = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_ABOUT));
                    results.blockedme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_ME));
                    results.blockedbyme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_BYME));
                    results.about = cursor.getString(cursor.getColumnIndex(Constants.TAG_ABOUT));
                    results.mute_notification = cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION));
                    results.contactstatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_STATUS));
                    results.favourited = cursor.getString(cursor.getColumnIndex(Constants.TAG_FAVOURITED));
                    results.user_name = cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME));
                           // results.phone_no, results.country_code);
                    results.saved_name = cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME));
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public String getContactPhone(String user_id) {
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + Constants.TAG_USER_ID + "='" + user_id + "'";

        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        String phone_no = "";
        if (cursor != null) {
            cursor.moveToFirst();

            if (cursor.moveToFirst()) {
                phone_no = cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER));
            }
            cursor.close();
        }
        return phone_no;
    }

    public String getContactCountryCode(String user_id) {
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + Constants.TAG_USER_ID + "='" + user_id + "'";

        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        String phone_no = "";
        if (cursor != null) {
            cursor.moveToFirst();

            if (cursor.moveToFirst()) {
                phone_no = cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE));
            }
            cursor.close();
        }
        return phone_no;
    }

    public List<ContactsData.Result> getAllContacts(Context context) {

        String selectQuery = "SELECT  * FROM " + TABLE_CONTACTS + " WHERE " + Constants.TAG_USER_ID + "!='" + GetSet.getUserId() + "'";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        List<ContactsData.Result> contactAry = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ContactsData.Result results = new ContactsData().new Result();
                results.user_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID));
                results.user_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_IMAGE));
                results.phone_no = cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER));
                results.country_code = cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE));
                results.user_name = getUserName(cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME)),
                        results.phone_no, results.country_code);
                results.privacy_last_seen = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_LAST_SEEN));
                results.privacy_profile_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_PROFILE));
                results.privacy_about = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_ABOUT));
                results.blockedme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_ME));
                results.blockedbyme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_BYME));
                results.about = cursor.getString(cursor.getColumnIndex(Constants.TAG_ABOUT));
                results.mute_notification = cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION));
                results.contactstatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_STATUS));
                results.favourited = cursor.getString(cursor.getColumnIndex(Constants.TAG_FAVOURITED));
                contactAry.add(results);
                //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return contactAry;
    }

    public List<ContactsData.Result> getStoredContacts(Context context) {

        String selectQuery = "SELECT  * FROM " + TABLE_CONTACTS + " WHERE " + Constants.TAG_USER_ID + "!='" + GetSet.getUserId() + "'";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        List<ContactsData.Result> contactAry = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                String phone_no = cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER));
                // HashMap<String, String> map = ApplicationClass.getContactrNot(context, phone_no);
                //  if (map.get("isAlready").equals("true")) {
                ContactsData.Result results = new ContactsData().new Result();
                results.user_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID));
                results.user_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_IMAGE));
                results.phone_no = cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER));
                results.country_code = cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE));
                results.privacy_last_seen = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_LAST_SEEN));
                results.privacy_profile_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_PROFILE));
                results.privacy_about = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_ABOUT));
                results.blockedme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_ME));
                results.blockedbyme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_BYME));
                results.about = cursor.getString(cursor.getColumnIndex(Constants.TAG_ABOUT));
                results.mute_notification = cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION));
                results.contactstatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_STATUS));
                results.favourited = cursor.getString(cursor.getColumnIndex(Constants.TAG_FAVOURITED));
                results.user_name = getUserName(cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME))
                        , results.phone_no, results.country_code);
                if (cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME)) != null &&
                        !cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME)).equals("")) {
                    contactAry.add(results);
                }
                //  }
                //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return contactAry;
    }

    public List<String> getAllContactsNumber(Context context) {

        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS;

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        List<String> contactAry = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                contactAry.add(cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER)));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return contactAry;
    }

    public void updateBlockStatus(String user_id, String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isUserExist(db, user_id);
        ContentValues values = new ContentValues();
        values.put(key, value);

        if (exists) {
            db.update(TABLE_CONTACTS, values, Constants.TAG_USER_ID + " =? ",
                    new String[]{user_id});
        }
    }

    public void resetAllBlockStatus(String key) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(key, "");

        db.update(TABLE_CONTACTS, values, key + " =? ",
                new String[]{"block"});
    }

    // Insert Members into the database for Mute Notifications
    public void updateMuteUser(String user_id, String mute) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_MUTE_NOTIFICATION, mute);

            getWritableDatabase().update(TABLE_CONTACTS, values, Constants.TAG_USER_ID + " =? ",
                    new String[]{user_id});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateFavUser(String user_id, String fav) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_FAVOURITED, fav);

            getWritableDatabase().update(TABLE_CONTACTS, values, Constants.TAG_USER_ID + " =? ",
                    new String[]{user_id});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ContactsData.Result> getFavContacts(Context context) {

        String selectQuery = "SELECT  * FROM " + TABLE_CONTACTS + " WHERE " + Constants.TAG_FAVOURITED + "='true'";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        List<ContactsData.Result> contactAry = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ContactsData.Result results = new ContactsData().new Result();
                results.user_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID));
                results.user_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_IMAGE));
                results.phone_no = cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER));
                results.country_code = cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE));
                results.user_name = getUserName(cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME)),
                        results.phone_no, results.country_code);
                results.privacy_last_seen = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_LAST_SEEN));
                results.privacy_profile_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_PROFILE));
                results.privacy_about = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_ABOUT));
                results.blockedme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_ME));
                results.blockedbyme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_BYME));
                results.about = cursor.getString(cursor.getColumnIndex(Constants.TAG_ABOUT));
                results.mute_notification = cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION));
                results.contactstatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_STATUS));
                results.favourited = cursor.getString(cursor.getColumnIndex(Constants.TAG_FAVOURITED));
                contactAry.add(results);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return contactAry;
    }

    // Delete all contacts in the database
    public void deleteAllContacts() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Order of deletions is important when foreign key relationships exist.
            db.delete(TABLE_CONTACTS, null, null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete all posts and users");
        } finally {
            db.endTransaction();
        }
    }

    // Insert a message into the database
    public void addMessageDatas(String chat_id, String message_id, String user_id, String user_name, String message_type, String message,
                                String attachment, String lat, String lon, String contact_name, String contact_phone_no,
                                String contact_country_code, String chat_time, String receiver_id, String sender_id,
                                String delivery_status, String thumbnail, String statusData) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_CHAT_ID, chat_id);
            values.put(Constants.TAG_MESSAGE_ID, message_id);
            values.put(Constants.TAG_USER_ID, user_id);
            values.put(Constants.TAG_USER_NAME, user_name);
            values.put(Constants.TAG_MESSAGE_TYPE, message_type);
            values.put(Constants.TAG_MESSAGE, message);
            values.put(Constants.TAG_ATTACHMENT, attachment);
            values.put(Constants.TAG_LAT, lat);
            values.put(Constants.TAG_LON, lon);
            values.put(Constants.TAG_CONTACT_NAME, contact_name);
            values.put(Constants.TAG_CONTACT_PHONE_NO, contact_phone_no);
            values.put(Constants.TAG_CONTACT_COUNTRY_CODE, contact_country_code);
            values.put(Constants.TAG_CHAT_TIME, chat_time);
            values.put(Constants.TAG_RECEIVER_ID, receiver_id);
            values.put(Constants.TAG_SENDER_ID, sender_id);
            values.put(Constants.TAG_DELIVERY_STATUS, delivery_status);
            values.put(Constants.TAG_THUMBNAIL, thumbnail);
            values.put(Constants.TAG_PROGRESS, "");
            values.put(Constants.TAG_IS_DELETE, "0");
            values.put(Constants.TAG_STATUS_DATA, statusData);

            getWritableDatabase().insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<MessagesData> getMessages(String chat_id, String offset, String limit) throws Exception {

        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + Constants.TAG_CHAT_ID + "='" + chat_id + "'" + " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC" + " LIMIT " + limit + " OFFSET " + offset;

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        List<MessagesData> messageAry = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                MessagesData results = new MessagesData();
                results.chat_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_ID));
                results.message_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
                results.user_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID));
                results.user_name = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_NAME));
                results.message_type = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
                /*Log.e(TAG, "getMessages: "+ cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
                if(results.message_type.equalsIgnoreCase("text")){
                    results.message = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE));
                    CryptLib cryptLib = new CryptLib();
                    results.message = cryptLib.decryptCipherTextWithRandomIV(results.message,"123");
                } else {
                    results.message = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE));
                }*/
                results.message = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
                results.attachment = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT)));
                results.lat = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LAT)));
                results.lon = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LON)));
                results.contact_name = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_NAME)));
                results.contact_phone_no = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_PHONE_NO)));
                results.contact_country_code = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_COUNTRY_CODE)));
                results.chat_time = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
                results.receiver_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_RECEIVER_ID));
                results.sender_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_SENDER_ID));
                results.delivery_status = cursor.getString(cursor.getColumnIndex(Constants.TAG_DELIVERY_STATUS));
                results.thumbnail = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL)));
                results.progress = cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS));
                results.isDelete = cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_DELETE));
                results.statusData = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_DATA)));
                messageAry.add(results);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return messageAry;
    }

    public MessagesData getSingleMessage(String message_id) {

        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + Constants.TAG_MESSAGE_ID + "='" + message_id + "'";

        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        if (cursor != null)
            cursor.moveToFirst();

        if (cursor.getCount() > 0) {
            MessagesData results = new MessagesData();
            results.chat_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_ID));
            results.message_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
            results.user_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID));
            results.message_type = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
            results.attachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
            results.lat = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LAT)));
            results.lon = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LON)));
            results.contact_name = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_NAME)));
            results.contact_phone_no = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_PHONE_NO)));
            results.contact_country_code = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_COUNTRY_CODE)));
            results.chat_time = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
            results.receiver_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_RECEIVER_ID));
            results.sender_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_SENDER_ID));
            results.delivery_status = cursor.getString(cursor.getColumnIndex(Constants.TAG_DELIVERY_STATUS));
            results.thumbnail = cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL));
            results.progress = cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS));
            results.isDelete = cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_DELETE));
            results.statusData = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_DATA)));
            results.user_name = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_NAME));
            results.message = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
            cursor.close();
            return results;
        }
        return null;
    }

    public String getSavedName(String userId) {

        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + TAG_USER_ID + "='" + userId + "'";
        String name = "";

        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        if (cursor != null)
            cursor.moveToFirst();

        if (cursor.getCount() > 0) {
            name = cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME));
            cursor.close();
            return name;
        }
        return name;
    }

    // Delete all chats in the database
    public void deleteAllChats(String chat_id) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // Order of deletions is important when foreign key relationships exist.
            db.delete(TABLE_MESSAGES, Constants.TAG_CHAT_ID + " =? ", new String[]{chat_id});
        } catch (Exception e) {
            Log.e(TAG, "deleteAllChats: " + e.getMessage());
        }
    }

    // Update Recent chats in the database
    public void updateRecentChat(String chat_id, String key, String value) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // Order of deletions is important when foreign key relationships exist.
            boolean exists = isRecentChatExist(chat_id);
            ContentValues values = new ContentValues();
            values.put(key, value);

            if (exists) {
                db.update(TABLE_RECENTS, values, Constants.TAG_CHAT_ID + " =? ", new String[]{chat_id});
            }
        } catch (Exception e) {
            Log.e(TAG, "updateRecentChat: " + e.getMessage());
        }
    }

    public void deleteRecentChat(String chat_id) {
        if (isRecentChatExist(chat_id)) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                // Order of deletions is important when foreign key relationships exist.
                db.delete(TABLE_RECENTS, Constants.TAG_CHAT_ID + " =? ", new String[]{chat_id});
            } catch (Exception e) {
                Log.e(TAG, "deleteRecentChat: " + e.getMessage());
            }
        }
    }

    public boolean isRecentChatExist(String chat_id) {
        long line = DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + TABLE_RECENTS + " WHERE " + Constants.TAG_CHAT_ID + "=?",
                new String[]{chat_id});
        return line > 0;
    }

    public void deleteMessageFromId(String message_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Order of deletions is important when foreign key relationships exist.
            db.delete(TABLE_MESSAGES, Constants.TAG_MESSAGE_ID + " =? ", new String[]{message_id});
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete all posts and users");
        }
    }

    public void deleteCallFromId(String callId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Order of deletions is important when foreign key relationships exist.
            db.delete(TABLE_CALL, Constants.TAG_CALL_ID + " =? ", new String[]{callId});
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete all posts and users");
        }
    }

    public void deleteGroupMessageFromId(String message_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Order of deletions is important when foreign key relationships exist.
            db.delete(TABLE_GROUP_MESSAGES, Constants.TAG_MESSAGE_ID + " =? ", new String[]{message_id});
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete all posts and users");
        }
    }

    public void deleteChannelMessageFromId(String message_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Order of deletions is important when foreign key relationships exist.
            db.delete(TABLE_CHANNEL_MESSAGES, Constants.TAG_MESSAGE_ID + " =? ", new String[]{message_id});
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete all posts and users");
        }
    }

    public int getMessagesCount(String chat_id) {
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + Constants.TAG_CHAT_ID + "='" + chat_id + "'";
        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public void updateMessageDeliverStatus(String message_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isMessageIdExist(db, message_id);
        ContentValues values = new ContentValues();
        values.put(Constants.TAG_DELIVERY_STATUS, "sent");

        if (exists) {
            db.update(TABLE_MESSAGES, values, Constants.TAG_MESSAGE_ID + " =? ",
                    new String[]{message_id});
        }
    }

    public void updateMessageData(String message_id, String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isMessageIdExist(db, message_id);
        ContentValues values = new ContentValues();
        values.put(key, value);

        if (exists) {
            if (key.equals(Constants.TAG_ATTACHMENT) && value.equals(ApplicationClass.decryptMessage(""))) {

            }
            db.update(TABLE_MESSAGES, values, Constants.TAG_MESSAGE_ID + " =? ",
                    new String[]{message_id});
        }
    }

    public void updateRecentMessageData(String chatId, String key, String value) {
        boolean exists = isRecentMessageIdExist(chatId);
        ContentValues values = new ContentValues();
        values.put(key, value);

        if (exists) {
            this.getWritableDatabase().update(TABLE_RECENTS, values, Constants.TAG_CHAT_ID + " =? ",
                    new String[]{chatId});
        }
    }

    public boolean isRecentMessageIdExist(String id) {
        long line = DatabaseUtils.longForQuery(this.getWritableDatabase(), "SELECT COUNT(*) FROM " + TABLE_RECENTS + " WHERE " + Constants.TAG_MESSAGE_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    public boolean isMessageIdExist(SQLiteDatabase db, String id) {
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_MESSAGES + " WHERE " + Constants.TAG_MESSAGE_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    public void updateMessageReadStatus(String chat_id, String user_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isChatIdExist(db, chat_id);
        ContentValues values = new ContentValues();
        values.put(Constants.TAG_DELIVERY_STATUS, "read");

        if (exists) {
            db.update(TABLE_MESSAGES, values, Constants.TAG_CHAT_ID + " =? AND " + Constants.TAG_DELIVERY_STATUS + " =? AND " + Constants.TAG_RECEIVER_ID + " =? ",
                    new String[]{chat_id, "sent", user_id});
        }
    }

    public boolean isChatIdExist(SQLiteDatabase db, String id) {
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_MESSAGES + " WHERE " + Constants.TAG_CHAT_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    // Insert a recent message into the database
    public void addRecentMessages(String chat_id, String user_id, String message_id,
                                  String chat_time, String unread_count) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_CHAT_ID, chat_id);
            values.put(Constants.TAG_USER_ID, user_id);
            values.put(Constants.TAG_MESSAGE_ID, message_id);
            values.put(Constants.TAG_CHAT_TIME, chat_time);
            values.put(Constants.TAG_UNREAD_COUNT, unread_count);

            getWritableDatabase().insertWithOnConflict(TABLE_RECENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<HashMap<String, String>> getAllRecentsMessages(Context context) {

        String selectQueryFav = "SELECT * FROM " + TABLE_RECENTS + " LEFT JOIN " + TABLE_MESSAGES + " ON " + TABLE_RECENTS + "." + Constants.TAG_MESSAGE_ID + " = " + TABLE_MESSAGES + "." + Constants.TAG_MESSAGE_ID +
                " INNER JOIN " + TABLE_CONTACTS + " ON " + TABLE_RECENTS + "." + Constants.TAG_USER_ID +
                " = " + TABLE_CONTACTS + "." + Constants.TAG_USER_ID + " WHERE " + TABLE_CONTACTS + "." + Constants.TAG_FAVOURITED + "='true' and " +TABLE_CONTACTS+"."+  Constants.TAG_USER_ID+"!='"+GetSet.getUserId()+
                "' ORDER BY " + Constants.TAG_CHAT_TIME + " DESC";

        ArrayList<HashMap<String, String>> recentAry = new ArrayList<>(getRecentMessageAry(context, selectQueryFav));

        String selectQuery = "SELECT * FROM " + TABLE_RECENTS + " LEFT JOIN " + TABLE_MESSAGES + " ON " + TABLE_RECENTS + "." + Constants.TAG_MESSAGE_ID + " = " + TABLE_MESSAGES + "." + Constants.TAG_MESSAGE_ID +
                " INNER JOIN " + TABLE_CONTACTS + " ON " + TABLE_RECENTS + "." + Constants.TAG_USER_ID +
                " = " + TABLE_CONTACTS + "." + Constants.TAG_USER_ID + " WHERE " + TABLE_CONTACTS + "." + Constants.TAG_FAVOURITED + "!='true' and " +TABLE_CONTACTS+"."+  Constants.TAG_USER_ID+"!='"+GetSet.getUserId()+
                "' ORDER BY " + Constants.TAG_CHAT_TIME + " DESC";

        recentAry.addAll(getRecentMessageAry(context, selectQuery));

        return recentAry;
    }


    public ArrayList<HashMap<String, String>> getAllRecentsMessagesLock(Context context) {

        String selectQueryFav = "SELECT * FROM " + TABLE_RECENTS + " LEFT JOIN " + TABLE_MESSAGES + " ON " + TABLE_RECENTS + "." + Constants.TAG_MESSAGE_ID + " = " + TABLE_MESSAGES + "." + Constants.TAG_MESSAGE_ID +
                " INNER JOIN " + TABLE_CONTACTS + " ON " + TABLE_RECENTS + "." + Constants.TAG_USER_ID +
                " = " + TABLE_CONTACTS + "." + Constants.TAG_USER_ID + " WHERE " + TABLE_CONTACTS + "." + Constants.TAG_FAVOURITED + "='true'" +
                " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC";

        ArrayList<HashMap<String, String>> recentAry = new ArrayList<>(getRecentMessageAryLock(context, selectQueryFav));

        String selectQuery = "SELECT * FROM " + TABLE_RECENTS + " LEFT JOIN " + TABLE_MESSAGES + " ON " + TABLE_RECENTS + "." + Constants.TAG_MESSAGE_ID + " = " + TABLE_MESSAGES + "." + Constants.TAG_MESSAGE_ID +
                " INNER JOIN " + TABLE_CONTACTS + " ON " + TABLE_RECENTS + "." + Constants.TAG_USER_ID +
                " = " + TABLE_CONTACTS + "." + Constants.TAG_USER_ID + " WHERE " + TABLE_CONTACTS + "." + Constants.TAG_FAVOURITED + "!='true'" +
                " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC";

        recentAry.addAll(getRecentMessageAryLock(context, selectQuery));

        return recentAry;
    }

    private ArrayList<HashMap<String, String>> getRecentMessageAry(Context context, String selectQuery) {
        ArrayList<HashMap<String, String>> tempAry = new ArrayList<>();
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                if(context.getSharedPreferences("wall", Context.MODE_PRIVATE).contains(cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID))))
                {
                    continue;
                }
                HashMap<String, String> map = new HashMap<>();
                map.put(Constants.TAG_CHAT_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_ID)));
                map.put(Constants.TAG_USER_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID)));
                map.put(Constants.TAG_USER_IMAGE, cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_IMAGE)));
                map.put(Constants.TAG_PHONE_NUMBER, cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER)));
                map.put(Constants.TAG_COUNTRY_CODE, cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE)));
                map.put(Constants.TAG_USER_NAME, getUserName(cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME)),
                        map.get(Constants.TAG_PHONE_NUMBER), map.get(Constants.TAG_COUNTRY_CODE)));
                map.put(Constants.TAG_ABOUT, cursor.getString(cursor.getColumnIndex(Constants.TAG_ABOUT)));
                map.put(Constants.TAG_PRIVACY_LAST_SEEN, cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_LAST_SEEN)));
                map.put(Constants.TAG_PRIVACY_PROFILE, cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_PROFILE)));
                map.put(Constants.TAG_PRIVACY_ABOUT, cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_ABOUT)));
                map.put(Constants.TAG_CONTACT_STATUS, cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_STATUS)));
                map.put(Constants.TAG_BLOCKED_ME, cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_ME)));
                map.put(Constants.TAG_BLOCKED_BYME, cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_BYME)));
                map.put(Constants.TAG_MESSAGE_TYPE, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE)));
                map.put(Constants.TAG_MESSAGE_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID)));
                map.put(Constants.TAG_CHAT_TIME, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME)));
                map.put(Constants.TAG_DELIVERY_STATUS, cursor.getString(cursor.getColumnIndex(Constants.TAG_DELIVERY_STATUS)));
                map.put(Constants.TAG_UNREAD_COUNT, cursor.getString(cursor.getColumnIndex(Constants.TAG_UNREAD_COUNT)));
                map.put(Constants.TAG_SENDER_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_SENDER_ID)));
                map.put(Constants.TAG_MESSAGE, getMessage(map.get(Constants.TAG_SENDER_ID), map.get(Constants.TAG_MESSAGE_TYPE),
                        ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE))), context));
                map.put(Constants.TAG_MUTE_NOTIFICATION, cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION)));
                map.put(Constants.TAG_PROGRESS, cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS)));
                map.put(Constants.TAG_FAVOURITED, cursor.getString(cursor.getColumnIndex(Constants.TAG_FAVOURITED)));


                tempAry.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return tempAry;
    }


    private ArrayList<HashMap<String, String>> getRecentMessageAryLock(Context context, String selectQuery) {
        ArrayList<HashMap<String, String>> tempAry = new ArrayList<>();
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                if(context.getSharedPreferences("wall", Context.MODE_PRIVATE).contains(cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID))))
                {


                    HashMap<String, String> map = new HashMap<>();
                    map.put(Constants.TAG_CHAT_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_ID)));
                    map.put(Constants.TAG_USER_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID)));
                    map.put(Constants.TAG_USER_IMAGE, cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_IMAGE)));
                    map.put(Constants.TAG_PHONE_NUMBER, cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER)));
                    map.put(Constants.TAG_COUNTRY_CODE, cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE)));
                    map.put(Constants.TAG_USER_NAME, getUserName(cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME)),
                            map.get(Constants.TAG_PHONE_NUMBER), map.get(Constants.TAG_COUNTRY_CODE)));
                    map.put(Constants.TAG_ABOUT, cursor.getString(cursor.getColumnIndex(Constants.TAG_ABOUT)));
                    map.put(Constants.TAG_PRIVACY_LAST_SEEN, cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_LAST_SEEN)));
                    map.put(Constants.TAG_PRIVACY_PROFILE, cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_PROFILE)));
                    map.put(Constants.TAG_PRIVACY_ABOUT, cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_ABOUT)));
                    map.put(Constants.TAG_CONTACT_STATUS, cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_STATUS)));
                    map.put(Constants.TAG_BLOCKED_ME, cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_ME)));
                    map.put(Constants.TAG_BLOCKED_BYME, cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_BYME)));
                    map.put(Constants.TAG_MESSAGE_TYPE, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE)));
                    map.put(Constants.TAG_MESSAGE_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID)));
                    map.put(Constants.TAG_CHAT_TIME, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME)));
                    map.put(Constants.TAG_DELIVERY_STATUS, cursor.getString(cursor.getColumnIndex(Constants.TAG_DELIVERY_STATUS)));
                    map.put(Constants.TAG_UNREAD_COUNT, cursor.getString(cursor.getColumnIndex(Constants.TAG_UNREAD_COUNT)));
                    map.put(Constants.TAG_SENDER_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_SENDER_ID)));
                    map.put(Constants.TAG_MESSAGE, getMessage(map.get(Constants.TAG_SENDER_ID), map.get(Constants.TAG_MESSAGE_TYPE),
                            ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE))), context));
                    map.put(Constants.TAG_MUTE_NOTIFICATION, cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION)));
                    map.put(Constants.TAG_PROGRESS, cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS)));
                    map.put(Constants.TAG_FAVOURITED, cursor.getString(cursor.getColumnIndex(Constants.TAG_FAVOURITED)));


                    tempAry.add(map);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        return tempAry;
    }



    public int getUnseenMessagesCount(String sender_id) {
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + Constants.TAG_DELIVERY_STATUS + " ='sent' AND " + Constants.TAG_SENDER_ID + "='" + sender_id + "'";
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    List<MessagesData> getUnseenMessage(String senderId, Context context) {
        /*String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + Constants.TAG_DELIVERY_STATUS + " ='sent"
                + "' GROUP BY " + Constants.TAG_SENDER_ID + " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC"; */
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + Constants.TAG_DELIVERY_STATUS + " ='sent' AND "
                + Constants.TAG_SENDER_ID + "='" + senderId + "'" + " ORDER BY " + Constants.TAG_CHAT_TIME + " ASC";
        List<MessagesData> messagesData = new ArrayList<>();
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                MessagesData results = new MessagesData();
                results.chat_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_ID));
                results.message_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
                results.user_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID));
                results.user_name = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_NAME));
                results.message_type = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
                /*Log.e(TAG, "getMessages: "+ cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
                if(results.message_type.equalsIgnoreCase("text")){
                    results.message = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE));
                    CryptLib cryptLib = new CryptLib();
                    results.message = cryptLib.decryptCipherTextWithRandomIV(results.message,"123");
                } else {
                    results.message = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE));
                }*/
                results.attachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
                results.lat = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LAT)));
                results.lon = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LON)));
                results.contact_name = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_NAME)));
                results.contact_phone_no = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_PHONE_NO)));
                results.contact_country_code = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_COUNTRY_CODE)));
                results.chat_time = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
                results.receiver_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_RECEIVER_ID));
                results.sender_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_SENDER_ID));
                results.delivery_status = cursor.getString(cursor.getColumnIndex(Constants.TAG_DELIVERY_STATUS));
                results.thumbnail = cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL));
                results.progress = cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS));
                results.isDelete = cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_DELETE));
                results.statusData = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_DATA)));
               /* if(results.message_type != null && results.message_type.equals(Constants.TAG_IS_DELETE)){
                    results.message = context.getString(R.string.this_message_was_deleted);
                } else {
                }*/
                results.message = getMessage(results.user_id, results.message_type,
                        ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE))), context);
                messagesData.add(results);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return messagesData;
    }

    List<String> getUnseenMessageUser() {
        List<String> user = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + Constants.TAG_DELIVERY_STATUS + " ='sent"
                + "' GROUP BY " + Constants.TAG_SENDER_ID + " ORDER BY " + Constants.TAG_CHAT_TIME + " ASC";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                user.add(cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return user;
    }

    public void resetUnseenMessagesCount(String user_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isRecentUserIdExist(db, user_id);
        ContentValues values = new ContentValues();
        values.put(Constants.TAG_UNREAD_COUNT, "0");

        if (exists) {
            db.update(TABLE_RECENTS, values, Constants.TAG_USER_ID + " =? ",
                    new String[]{user_id});
        }
    }

    public boolean isRecentUserIdExist(SQLiteDatabase db, String id) {
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_RECENTS + " WHERE " + Constants.TAG_USER_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    public List<ContactsData.Result> getBlockedContacts(Context context) {

        String selectQuery = "SELECT  * FROM " + TABLE_CONTACTS + " WHERE " + Constants.TAG_BLOCKED_BYME + " = '" + Constants.TAG_BLOCK + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        List<ContactsData.Result> contactAry = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ContactsData.Result results = new ContactsData().new Result();
                results.user_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID));
                results.user_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_IMAGE));
                results.phone_no = cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER));
                results.country_code = cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE));
                results.user_name = getUserName(cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME)),
                        results.phone_no, results.country_code);
                results.privacy_last_seen = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_LAST_SEEN));
                results.privacy_profile_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_PROFILE));
                results.privacy_about = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_ABOUT));
                results.blockedme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_ME));
                results.blockedbyme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_BYME));
                contactAry.add(results);
                //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return contactAry;
    }

    public void createGroup(String groupId, String groupAdminId, String groupName, String createdAt, String groupImage) {

        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_GROUP_ID, groupId);
            values.put(Constants.TAG_GROUP_CREATED_BY, groupAdminId);
            values.put(Constants.TAG_GROUP_NAME, groupName);
            values.put(Constants.TAG_CREATED_AT, createdAt);
            values.put(Constants.TAG_GROUP_IMAGE, groupImage);
            values.put(Constants.TAG_MUTE_NOTIFICATION, "");
            db.insertWithOnConflict(TABLE_GROUP, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createGroupMembers(String memberKey, String groupId, String memberId, String memberRole) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_MEMBER_KEY, memberKey);
            values.put(Constants.TAG_GROUP_ID, groupId);
            values.put(Constants.TAG_MEMBER_ID, memberId);
            values.put(Constants.TAG_MEMBER_ROLE, memberRole);

            db.insertWithOnConflict(TABLE_GROUP_MEMBERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateGroupMembers(String memberKey, String groupId, String memberId, String memberRole) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_MEMBER_KEY, memberKey);
            values.put(Constants.TAG_GROUP_ID, groupId);
            values.put(Constants.TAG_MEMBER_ID, memberId);
            values.put(Constants.TAG_MEMBER_ROLE, memberRole);

            db.insertWithOnConflict(TABLE_GROUP_MEMBERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<GroupData> getGroups() {
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        List<GroupData> groupList = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                GroupData groupData = new GroupData();
                groupData.groupId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ID));
                groupData.groupName = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_NAME));
                groupData.groupAdminId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_CREATED_BY));
                groupData.createdAt = cursor.getString(cursor.getColumnIndex(Constants.TAG_CREATED_AT));
                groupData.groupImage = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_IMAGE));
                groupList.add(groupData);
                //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return groupList;
    }

    public GroupData getGroupData(Context context, String groupId) {
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP + " WHERE " + Constants.TAG_GROUP_ID + " ='" + groupId + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isGroupExist(db, groupId);
        if (exists) {
            Cursor cursor = db.rawQuery(selectQuery, null);
            // looping through all rows and adding to list
            GroupData groupData = new GroupData();
            if (cursor != null)
                cursor.moveToFirst();
            groupData.groupId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ID));
            groupData.groupName = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_NAME));
            groupData.groupAdminId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_CREATED_BY));
            groupData.createdAt = cursor.getString(cursor.getColumnIndex(Constants.TAG_CREATED_AT));
            groupData.groupImage = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_IMAGE));
            groupData.muteNotification = cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION));
            //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            cursor.close();

            return groupData;
        } else {
            return null;
        }
    }

    public void updateGroupData(String groupId, String key, String value) {

        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isGroupExist(db, groupId);
        ContentValues values = new ContentValues();
        values.put(key, value);

        if (exists) {
            db.update(TABLE_GROUP, values, Constants.TAG_GROUP_ID + " =? ",
                    new String[]{groupId});
        }
    }

    public void updateGroup(String groupId, String adminId, String name, String createdAt, String groupImage) {

        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isGroupExist(db, groupId);
        ContentValues values = new ContentValues();
        values.put(Constants.TAG_GROUP_ID, groupId);
        values.put(Constants.TAG_GROUP_CREATED_BY, adminId);
        values.put(Constants.TAG_GROUP_NAME, name);
        values.put(Constants.TAG_CREATED_AT, createdAt);
        values.put(Constants.TAG_GROUP_IMAGE, groupImage);

        if (exists) {
            db.update(TABLE_GROUP, values, Constants.TAG_GROUP_ID + " =? ",
                    new String[]{groupId});
        }
    }

    public List<GroupData.GroupMembers> getGroupMembers(Context context, String groupId) {
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP_MEMBERS + " WHERE " + Constants.TAG_GROUP_ID + " ='" + groupId + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        List<GroupData.GroupMembers> groupList = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                GroupData.GroupMembers groupData = new GroupData().new GroupMembers();
                groupData.memberId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ID));
                groupData.memberRole = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ROLE));
                groupList.add(groupData);
                //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return groupList;
    }

    public int getGroupMemberSize(String groupId) {
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP_MEMBERS + " WHERE " + Constants.TAG_GROUP_ID + " ='" + groupId + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public GroupData.GroupMembers getSingleMemberFromGroup(String groupId, String memberId) {
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP_MEMBERS + " WHERE " + Constants.TAG_GROUP_ID + " ='" +
                groupId + "'" + " AND " + Constants.TAG_MEMBER_ID + " ='" + memberId + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        GroupData.GroupMembers groupData = new GroupData().new GroupMembers();
        // looping through all rows and adding to list
        if (cursor != null) {
            cursor.moveToFirst();
            Log.e(TAG, "getSingleMemberFromGroup: " + cursor.getCount());
            if (cursor.moveToFirst()) {
                groupData.memberId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ID));
                groupData.memberRole = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ROLE));
                //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            }
            cursor.close();
        }
        return groupData;
    }

    public List<GroupData.GroupMembers> getThreeMembers(Context context, String groupId) {
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP_MEMBERS + " WHERE " + Constants.TAG_GROUP_ID + " ='" + groupId + "'" + " LIMIT 5";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        List<GroupData.GroupMembers> groupList = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                GroupData.GroupMembers groupData = new GroupData().new GroupMembers();
                groupData.memberId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ID));
                groupData.memberRole = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ROLE));
                groupList.add(groupData);
                //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return groupList;
    }

    public void addGroupMessages(String messageId, String groupID, String memberId, String adminId, String messageType,
                                 String message, String attachment, String lat, String lon,
                                 String contactName, String contactPhone, String countryCode, String chatTime, String thumbnail, String deliveryStatus) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_GROUP_ID, groupID);
            values.put(Constants.TAG_MEMBER_ID, memberId);
            values.put(Constants.TAG_MESSAGE_ID, messageId);
            values.put(Constants.TAG_GROUP_ADMIN_ID, adminId);
            values.put(Constants.TAG_MESSAGE_TYPE, messageType);
            values.put(Constants.TAG_MESSAGE, message);
            values.put(Constants.TAG_ATTACHMENT, ApplicationClass.decryptMessage(attachment));
            values.put(Constants.TAG_LAT, lat);
            values.put(Constants.TAG_LON, lon);
            values.put(Constants.TAG_CONTACT_NAME, contactName);
            values.put(Constants.TAG_CONTACT_PHONE_NO, contactPhone);
            values.put(Constants.TAG_CONTACT_COUNTRY_CODE, countryCode);
            values.put(Constants.TAG_CHAT_TIME, chatTime);
            values.put(Constants.TAG_THUMBNAIL, ApplicationClass.decryptMessage(thumbnail));
            values.put(Constants.TAG_PROGRESS, "");
            values.put(Constants.TAG_DELIVERY_STATUS, deliveryStatus);

            getWritableDatabase().insertWithOnConflict(TABLE_GROUP_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<GroupMessage> getGroupMessages(String groupId, String offset, String limit, Context context) {

        String selectQuery = "SELECT * FROM " + TABLE_GROUP_MESSAGES + " WHERE " + Constants.TAG_GROUP_ID + "='" + groupId + "'" + " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC" + " LIMIT " + limit + " OFFSET " + offset;

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        List<HashMap<String, String>> recentAry = new ArrayList<>();
        List<GroupMessage> groupMessage = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                GroupMessage message = new GroupMessage();
                message.groupId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ID));
                message.messageId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
                message.messageType = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
                message.memberId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ID));
                message.groupAdminId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ADMIN_ID));
                message.message = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
                message.attachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
                message.lat = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LAT)));
                message.lon = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LON)));
                message.contactName = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_NAME)));
                message.contactPhoneNo = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_PHONE_NO)));
                message.contactCountryCode = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_COUNTRY_CODE)));
                message.chatTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
                message.thumbnail = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL)));
                message.progress = cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS));
                message.isDelete = cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_DELETE));
//                groupMessage.add(message);
                groupMessage.add(GroupChatActivity.getMessages(this, context, message));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return groupMessage;
    }

    public GroupMessage getSingleGroupMessage(String groupId, String messageId) {

        String selectQuery = "SELECT * FROM " + TABLE_GROUP_MESSAGES + " WHERE " + Constants.TAG_GROUP_ID + "='" + groupId + "'" + " AND " + Constants.TAG_MESSAGE_ID + " ='" + messageId + "'";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        GroupMessage message = new GroupMessage();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            message.groupId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ID));
            message.messageId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
            message.messageType = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
            message.memberId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ID));
            message.groupAdminId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ADMIN_ID));
            message.message = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
            message.attachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
            message.lat = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LAT)));
            message.lon = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LON)));
            message.contactName = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_NAME)));
            message.contactPhoneNo = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_PHONE_NO)));
            message.contactCountryCode = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_COUNTRY_CODE)));
            message.chatTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
            message.thumbnail = cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL));
            message.progress = cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS));
            message.isDelete = cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_DELETE));
        }
        cursor.close();

        return message;
    }

    public void addGroupRecentMsgs(String groupID, String messageId, String senderId, String chatTime, String unreadCount) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_GROUP_ID, groupID);
            values.put(Constants.TAG_MEMBER_ID, senderId);
            values.put(Constants.TAG_MESSAGE_ID, messageId);
            values.put(Constants.TAG_CHAT_TIME, chatTime);
            values.put(Constants.TAG_UNREAD_COUNT, unreadCount);

            getWritableDatabase().insertWithOnConflict(TABLE_GROUP_RECENT_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<HashMap<String, String>> getGroupRecentMessages(Context context) {

        String selectQuery = "SELECT * FROM " + TABLE_GROUP_RECENT_MESSAGES + " LEFT JOIN " + TABLE_GROUP_MESSAGES + " ON " +
                TABLE_GROUP_RECENT_MESSAGES + "." + Constants.TAG_MESSAGE_ID + " = " + TABLE_GROUP_MESSAGES + "." + Constants.TAG_MESSAGE_ID +
                " INNER JOIN " + TABLE_GROUP + " ON " + TABLE_GROUP_RECENT_MESSAGES + "." + Constants.TAG_GROUP_ID +
                " = " + TABLE_GROUP + "." + Constants.TAG_GROUP_ID +
                " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        ArrayList<HashMap<String, String>> recentAry = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<>();
                map.put(Constants.TAG_MESSAGE_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID)));
                map.put(Constants.TAG_GROUP_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ID)));
                map.put(Constants.TAG_MEMBER_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ID)));
                map.put(Constants.TAG_GROUP_IMAGE, cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_IMAGE)));
                map.put(Constants.TAG_GROUP_NAME, cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_NAME)));
                map.put(Constants.TAG_MESSAGE_TYPE, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE)));
                map.put(Constants.TAG_MESSAGE, getMessage(map.get(Constants.TAG_MEMBER_ID), map.get(Constants.TAG_MESSAGE_TYPE),
                        ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE))), context));
                map.put(Constants.TAG_CHAT_TIME, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME)));
                map.put(Constants.TAG_UNREAD_COUNT, cursor.getString(cursor.getColumnIndex(Constants.TAG_UNREAD_COUNT)));
                map.put(Constants.TAG_MUTE_NOTIFICATION, cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION)));
                map.put(Constants.TAG_GROUP_ADMIN_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ADMIN_ID)));
                map.put(Constants.TAG_ATTACHMENT, cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT)));

                recentAry.add(GroupFragment.getMessages(this, context, map));
                //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return recentAry;
    }

    public ArrayList<HashMap<String, String>> getGroupUnReadMessage(Context context, String groupId) {

        String selectQuery = "SELECT * FROM " + TABLE_GROUP_RECENT_MESSAGES + " LEFT JOIN " + TABLE_GROUP_MESSAGES + " ON " +
                TABLE_GROUP_RECENT_MESSAGES + "." + Constants.TAG_MESSAGE_ID + " = " + TABLE_GROUP_MESSAGES + "." + Constants.TAG_MESSAGE_ID +
                " INNER JOIN " + TABLE_GROUP + " ON " + TABLE_GROUP_RECENT_MESSAGES + "." + Constants.TAG_GROUP_ID +
                " ='" + groupId + "' ORDER BY " + Constants.TAG_CHAT_TIME + " DESC" + " LIMIT 3";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        ArrayList<HashMap<String, String>> recentAry = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<>();
                map.put(Constants.TAG_MESSAGE_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID)));
                map.put(Constants.TAG_GROUP_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ID)));
                map.put(Constants.TAG_MEMBER_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ID)));
                map.put(Constants.TAG_GROUP_IMAGE, cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_IMAGE)));
                map.put(Constants.TAG_GROUP_NAME, cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_NAME)));
                map.put(Constants.TAG_MESSAGE_TYPE, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE)));
                if (map.get(Constants.TAG_MESSAGE_TYPE) != null && map.get(Constants.TAG_MESSAGE_TYPE).equals(Constants.TAG_ISDELETE)) {
                    map.put(Constants.TAG_MESSAGE, ApplicationClass.decryptMessage(context.getString(R.string.this_message_was_deleted)));
                } else {
                    map.put(Constants.TAG_MESSAGE, ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE))));
                }
                map.put(Constants.TAG_CHAT_TIME, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME)));
                map.put(Constants.TAG_UNREAD_COUNT, cursor.getString(cursor.getColumnIndex(Constants.TAG_UNREAD_COUNT)));
                map.put(Constants.TAG_MUTE_NOTIFICATION, cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION)));
                map.put(Constants.TAG_GROUP_ADMIN_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ADMIN_ID)));
                map.put(Constants.TAG_ATTACHMENT, cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT)));

                recentAry.add(GroupFragment.getMessages(this, context, map));
                //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return recentAry;
    }

    public int getGroupMessagesCount(String groupId) {
        String selectQuery = "SELECT * FROM " + TABLE_GROUP_MESSAGES + " WHERE " + Constants.TAG_GROUP_ID + "='" + groupId + "'";
        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public void deleteFromGroup(String groupId, String memberId) {
        String deleteQuery = "DELETE FROM " + TABLE_GROUP_MEMBERS + " WHERE " + Constants.TAG_GROUP_ID +
                "='" + groupId + "'" + " AND " + Constants.TAG_MEMBER_ID + "='" + memberId + "'";
        getWritableDatabase().execSQL(deleteQuery);
    }

    public void updateMemberInGroup(GroupData.GroupMembers members, String memberKey) {

        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_MEMBER_ID, members.memberId);
            values.put(Constants.TAG_MEMBER_ROLE, members.memberRole);

            db.update(TABLE_GROUP_MEMBERS, values, Constants.TAG_MEMBER_KEY + " =? ",
                    new String[]{memberKey});
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean isMemberExist(String userId, String groupId) {
        Log.d(TAG, "isMemberExist: " + userId + " " + groupId);
        SQLiteDatabase db = this.getWritableDatabase();
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_GROUP_MEMBERS +
                        " WHERE " + Constants.TAG_MEMBER_ID + "=?" + " AND " + Constants.TAG_GROUP_ID + "=?",
                new String[]{userId, groupId});
        return line > 0;
    }

    public void deleteGroup(String groupId) {
        String deleteQuery = "DELETE FROM " + TABLE_GROUP + " WHERE " + Constants.TAG_GROUP_ID +
                "='" + groupId + "'";
        getWritableDatabase().execSQL(deleteQuery);
    }

    public void deleteMembers(String groupId) {
        String deleteQuery = "DELETE FROM " + TABLE_GROUP_MEMBERS + " WHERE " + Constants.TAG_GROUP_ID +
                "='" + groupId + "'";
        getWritableDatabase().execSQL(deleteQuery);
    }

    public void deleteGroupMessages(String groupId) {
        String deleteQuery = "DELETE FROM " + TABLE_GROUP_MESSAGES + " WHERE " + Constants.TAG_GROUP_ID +
                "='" + groupId + "'";
        getWritableDatabase().execSQL(deleteQuery);
    }

    public void deleteGroupRecentChats(String groupId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isRecentGroupIdExist(groupId);
        if (exists) {
            String deleteQuery = "DELETE FROM " + TABLE_GROUP_RECENT_MESSAGES + " WHERE " + Constants.TAG_GROUP_ID +
                    "='" + groupId + "'";
            getWritableDatabase().execSQL(deleteQuery);

        }
    }

    public void updateGroupMessageReadStatus(String group_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isGroupIdExist(db, group_id);
        ContentValues values = new ContentValues();
        values.put(Constants.TAG_DELIVERY_STATUS, "read");

        if (exists) {
            db.update(TABLE_GROUP_MESSAGES, values, Constants.TAG_GROUP_ID + " =? ",
                    new String[]{group_id});
        }
    }

    public boolean isGroupIdExist(SQLiteDatabase db, String id) {
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_GROUP_MESSAGES + " WHERE " + Constants.TAG_GROUP_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    public int getUnseenGroupMessagesCount(String group_id) {
        String selectQuery = "SELECT * FROM " + TABLE_GROUP_MESSAGES + " WHERE " + Constants.TAG_DELIVERY_STATUS + " ='' AND " + Constants.TAG_GROUP_ID + "='" + group_id + "'";
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    List<GroupMessage> getUnseenGroupMessage(String groupId, String userId) {
        String selectQuery = "SELECT * FROM " + TABLE_GROUP_MESSAGES + " WHERE " + Constants.TAG_DELIVERY_STATUS + " ='sent' AND "
                + Constants.TAG_GROUP_ID + "='" + groupId + "' AND " + Constants.TAG_MEMBER_ID + "='" + userId + "' ORDER BY " + Constants.TAG_CHAT_TIME + " ASC";
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        List<GroupMessage> groupMessage = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                GroupMessage message = new GroupMessage();
                message.groupId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ID));
                message.messageId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
                message.messageType = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
                message.memberId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ID));
                message.groupAdminId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ADMIN_ID));
                message.message = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
                message.attachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
                message.lat = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LAT)));
                message.lon = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LON)));
                message.contactName = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_NAME)));
                message.contactPhoneNo = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_PHONE_NO)));
                message.contactCountryCode = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_COUNTRY_CODE)));
                message.chatTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
                message.thumbnail = cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL));
                message.progress = cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS));
                message.isDelete = cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_DELETE));

                groupMessage.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return groupMessage;
    }

    List<String> getGroupId() {
        List<String> user = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_GROUP_MEMBERS + " GROUP BY " + Constants.TAG_GROUP_ID;

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                user.add(cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ID)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return user;
    }


    public void resetUnseenGroupMessagesCount(String group_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isRecentGroupIdExist(group_id);
        ContentValues values = new ContentValues();
        values.put(Constants.TAG_UNREAD_COUNT, "0");

        if (exists) {
            db.update(TABLE_GROUP_RECENT_MESSAGES, values, Constants.TAG_GROUP_ID + " =? ",
                    new String[]{group_id});
        }
    }

    public boolean isRecentGroupIdExist(String groupId) {
        long line = DatabaseUtils.longForQuery(this.getWritableDatabase(), "SELECT COUNT(*) FROM " + TABLE_GROUP_RECENT_MESSAGES + " WHERE " + Constants.TAG_GROUP_ID + "=?",
                new String[]{groupId});
        return line > 0;
    }

    public void updateGroupMessageData(String message_id, String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isGroupMessageIdExist(db, message_id);
        ContentValues values = new ContentValues();
        values.put(key, value);

        if (exists) {
            db.update(TABLE_GROUP_MESSAGES, values, Constants.TAG_MESSAGE_ID + " =? ",
                    new String[]{message_id});
        }
    }

    public boolean isGroupMessageIdExist(SQLiteDatabase db, String id) {
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_GROUP_MESSAGES + " WHERE " + Constants.TAG_MESSAGE_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    public long isGroupHaveAdmin(String group_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_GROUP_MEMBERS +
                        " WHERE " + Constants.TAG_GROUP_ID + "=?" + " AND " + Constants.TAG_MEMBER_ROLE + "=?",
                new String[]{group_id, "1"});
        return line;
    }

    public GroupData.GroupMembers getAdminFromMembers(String group_id) {
        String selectQuery = "SELECT * FROM " + TABLE_GROUP_MEMBERS +
                " WHERE " + Constants.TAG_GROUP_ID + "='" + group_id + "' AND " + Constants.TAG_MEMBER_ROLE + "='1'";

        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        GroupData.GroupMembers results = new GroupData().new GroupMembers();
        if (cursor != null) {
            cursor.moveToFirst();

            if (cursor.moveToFirst()) {
                results.memberId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ID));
                results.memberRole = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ROLE));
            }
            cursor.close();
        }
        return results;
    }

    // Insert Members into the database for Mute Notifications
    public void updateMuteGroup(String group_id, String mute) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_MUTE_NOTIFICATION, mute);

            getWritableDatabase().update(TABLE_GROUP, values, Constants.TAG_GROUP_ID + " =? ",
                    new String[]{group_id});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUserPrivacy(JSONObject jsonObject) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            boolean exists = isUserExist(db, jsonObject.getString(TAG_USER_ID));
            if (exists) {
                ContentValues values = new ContentValues();
                values.put(TAG_USER_ID, jsonObject.getString(TAG_USER_ID));
                values.put(TAG_PRIVACY_LAST_SEEN, jsonObject.getString(TAG_PRIVACY_LAST_SEEN));
                values.put(TAG_PRIVACY_ABOUT, jsonObject.getString(TAG_PRIVACY_ABOUT));
                values.put(TAG_PRIVACY_PROFILE, jsonObject.getString(TAG_PRIVACY_PROFILE));

                db.update(TABLE_CONTACTS, values, Constants.TAG_USER_ID + " =? ",
                        new String[]{jsonObject.getString(TAG_USER_ID)});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addChannel(String channelId, String channelName, String channelDes, String channelImage, String channelType, String adminId,
                           String channelAdminName, String totalSubscribers, String createdAt, String channelCategory, String subscribeStatus,
                           String blockStatus, String report) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_CHANNEL_ID, channelId);
            values.put(Constants.TAG_CHANNEL_NAME, channelName);
            values.put(Constants.TAG_CHANNEL_DES, channelDes);
            values.put(Constants.TAG_CHANNEL_IMAGE, channelImage);
            values.put(Constants.TAG_CHANNEL_TYPE, channelType);
            values.put(Constants.TAG_CHANNEL_ADMIN_ID, adminId);
            values.put(Constants.TAG_CHANNEL_ADMIN_NAME, channelAdminName);
            values.put(Constants.TAG_TOTAL_SUBSCRIBERS, totalSubscribers);
            values.put(Constants.TAG_MUTE_NOTIFICATION, "");
            values.put(Constants.SentFileHolder, createdAt);
            values.put(Constants.TAG_CHANNEL_CATEGORY, channelCategory);
            values.put(Constants.TAG_SUBSCRIBE_STATUS, subscribeStatus);
            if (blockStatus != null && !blockStatus.equals("")) {
                values.put(Constants.TAG_BLOCK_STATUS, blockStatus);
            } else {
                values.put(Constants.TAG_BLOCK_STATUS, "0");
            }
            if (report == null || report.equals("")) {
                report = "0";
            }
            values.put(Constants.TAG_REPORT, report);
//            values.put(Constants.TAG_BLOCK_STATUS, blockStatus);

            if (isChannelExist(channelId)) {
                getWritableDatabase().update(TABLE_CHANNEL, values, Constants.TAG_CHANNEL_ID + " =? ",
                        new String[]{channelId});
            } else {
                values.put(Constants.TAG_MUTE_NOTIFICATION, "");
                getWritableDatabase().insert(TABLE_CHANNEL, null, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ChannelResult.Result> getAllChannels() {
        String selectQuery = "SELECT * FROM " + TABLE_CHANNEL;
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        ArrayList<ChannelResult.Result> channelList = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ChannelResult.Result result = new ChannelResult().new Result();
                result.channelId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ID));
                result.channelName = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_NAME));
                result.channelDes = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_DES));
                result.channelImage = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_IMAGE));
                result.channelType = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_TYPE));
                result.createdAt = cursor.getString(cursor.getColumnIndex(Constants.TAG_CREATED_AT));
                result.totalSubscribers = cursor.getString(cursor.getColumnIndex(Constants.TAG_TOTAL_SUBSCRIBERS));
                result.channelAdminId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ADMIN_ID));
                result.channelAdminName = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ADMIN_NAME));
                result.muteNotification = cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION));
                result.channelCategory = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_CATEGORY));
                result.subscribeStatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_SUBSCRIBE_STATUS));
                result.report = cursor.getString(cursor.getColumnIndex(Constants.TAG_REPORT));

                channelList.add(result);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return channelList;
    }

    public List<ChannelResult.Result> getSubscribedChannels(String userId) {
        String selectQuery = "SELECT * FROM " + TABLE_CHANNEL + " WHERE " + Constants.TAG_CHANNEL_ADMIN_ID + " !='" + userId + "'";
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        ArrayList<ChannelResult.Result> channelList = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ChannelResult.Result result = new ChannelResult().new Result();
                result.channelId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ID));
                result.channelName = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_NAME));
                result.channelDes = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_DES));
                result.channelImage = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_IMAGE));
                result.channelType = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_TYPE));
                result.createdAt = cursor.getString(cursor.getColumnIndex(Constants.TAG_CREATED_TIME));
                result.totalSubscribers = cursor.getString(cursor.getColumnIndex(Constants.TAG_TOTAL_SUBSCRIBERS));
                result.channelAdminId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ADMIN_ID));
                result.subscribeStatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_SUBSCRIBE_STATUS));
                result.muteNotification = cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION));

                channelList.add(result);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return channelList;
    }

    public List<ChannelResult.Result> getMyChannels(String userId) {
        String selectQuery = "SELECT * FROM " + TABLE_CHANNEL + " WHERE " + Constants.TAG_CHANNEL_ADMIN_ID + " ='" + userId + "'" + " ORDER BY " + Constants.TAG_CREATED_AT + " DESC";
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        ArrayList<ChannelResult.Result> channelList = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ChannelResult.Result result = new ChannelResult().new Result();
                result.channelId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ID));
                result.channelName = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_NAME));
                result.channelDes = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_DES));
                result.channelImage = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_IMAGE));
                result.channelType = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_TYPE));
                result.createdAt = cursor.getString(cursor.getColumnIndex(Constants.SentFileHolder));
                result.totalSubscribers = cursor.getString(cursor.getColumnIndex(Constants.TAG_TOTAL_SUBSCRIBERS));
                result.channelAdminId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ADMIN_ID));
                result.subscribeStatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_SUBSCRIBE_STATUS));
                result.channelCategory = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_CATEGORY));
                result.report = cursor.getString(cursor.getColumnIndex(Constants.TAG_REPORT));

                channelList.add(result);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return channelList;
    }

    public ChannelResult.Result getChannelInfo(String channelId) {
        String selectQuery = "SELECT  * FROM " + TABLE_CHANNEL + " WHERE " + Constants.TAG_CHANNEL_ID + " ='" +
                channelId + "'";

        if (!isChannelExist(channelId)) {
            return null;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        ChannelResult.Result channelData = new ChannelResult().new Result();
        // looping through all rows and adding to list
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.moveToFirst()) {
                channelData.channelAdminId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ADMIN_ID));
                channelData.channelAdminName = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ADMIN_NAME));
                channelData.channelType = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_TYPE));
                channelData.totalSubscribers = cursor.getString(cursor.getColumnIndex(Constants.TAG_TOTAL_SUBSCRIBERS));
                channelData.channelImage = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_IMAGE));
                channelData.channelDes = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_DES));
                channelData.channelName = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_NAME));
                channelData.channelId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ID));
                channelData.createdTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_CREATED_AT));
                channelData.muteNotification = cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION));
                channelData.channelCategory = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_CATEGORY));
                channelData.subscribeStatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_SUBSCRIBE_STATUS));
                channelData.report = cursor.getString(cursor.getColumnIndex(Constants.TAG_REPORT));
//                channelData.blockStatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCK_STATUS));
//                channelData.blockStatus = "";

            }
            cursor.close();
        }
        return channelData;
    }

    public AdminChannel.Result getAdminChannelInfo(String channelId) {
        String selectQuery = "SELECT  * FROM " + TABLE_CHANNEL + " WHERE " + Constants.TAG_CHANNEL_ID + " ='" +
                channelId + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        AdminChannel.Result channelData = new AdminChannel().new Result();
        // looping through all rows and adding to list
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.moveToFirst()) {
                channelData.channelDes = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_DES));
                channelData.channelName = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_NAME));
                channelData.channelId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ID));
                channelData.channelImage = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_IMAGE));
                channelData.createdTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_CREATED_AT));
            }
            cursor.close();
        }
        return channelData;
    }

    public void updateChannelData(String channelId, String key, String value) {

        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isChannelExist(channelId);
        ContentValues values = new ContentValues();
        values.put(key, value);

        if (exists) {
            db.update(TABLE_CHANNEL, values, Constants.TAG_CHANNEL_ID + " =? ",
                    new String[]{channelId});
        }
    }

    public void updateChannelInfo(String channelId, String channelName, String channelDes, String channelImage, String channelType, String adminId, String channelAdminName, String totalSubscribers, String blockStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isChannelExist(channelId);
        ContentValues values = new ContentValues();
        values.put(Constants.TAG_CHANNEL_ID, channelId);
        values.put(Constants.TAG_CHANNEL_NAME, channelName);
        values.put(Constants.TAG_CHANNEL_DES, channelDes);
        values.put(Constants.TAG_CHANNEL_IMAGE, channelImage);
        values.put(Constants.TAG_CHANNEL_TYPE, channelType);
        values.put(Constants.TAG_CHANNEL_ADMIN_ID, adminId);
        values.put(Constants.TAG_CHANNEL_ADMIN_NAME, channelAdminName);
        values.put(Constants.TAG_TOTAL_SUBSCRIBERS, totalSubscribers);
//        values.put(Constants.TAG_BLOCK_STATUS, blockStatus);

        if (exists) {
            db.update(TABLE_CHANNEL, values, Constants.TAG_CHANNEL_ID + " =? ",
                    new String[]{channelId});
        }
    }

    public void updateChannelWithoutAdminName(String channelId, String channelName, String channelDes, String channelImage, String channelType, String adminId, String totalSubscribers) {

        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isChannelExist(channelId);
        ContentValues values = new ContentValues();
        values.put(Constants.TAG_CHANNEL_ID, channelId);
        values.put(Constants.TAG_CHANNEL_NAME, channelName);
        values.put(Constants.TAG_CHANNEL_DES, channelDes);
        values.put(Constants.TAG_CHANNEL_IMAGE, channelImage);
        values.put(Constants.TAG_CHANNEL_TYPE, channelType);
        values.put(Constants.TAG_CHANNEL_ADMIN_ID, adminId);
        values.put(Constants.TAG_TOTAL_SUBSCRIBERS, totalSubscribers);

        if (exists) {
            db.update(TABLE_CHANNEL, values, Constants.TAG_CHANNEL_ID + " =? ",
                    new String[]{channelId});
        }
    }

    public void addChannelMessages(String channelId, String chatType, String messageId, String messageType,
                                   String message, String attachment, String lat, String lon,
                                   String contactName, String contactPhone, String countryCode, String chatTime, String thumbnail, String deliveryStatus) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_CHANNEL_ID, channelId);
            values.put(Constants.TAG_CHAT_TYPE, chatType);
            values.put(Constants.TAG_MESSAGE_ID, messageId);
            values.put(Constants.TAG_MESSAGE_TYPE, messageType);
            values.put(Constants.TAG_MESSAGE, message);
            values.put(Constants.TAG_ATTACHMENT, attachment);
            values.put(Constants.TAG_LAT, lat);
            values.put(Constants.TAG_LON, lon);
            values.put(Constants.TAG_CONTACT_NAME, contactName);
            values.put(Constants.TAG_CONTACT_PHONE_NO, contactPhone);
            values.put(Constants.TAG_CONTACT_COUNTRY_CODE, countryCode);
            values.put(Constants.TAG_CHAT_TIME, chatTime);
            values.put(Constants.TAG_THUMBNAIL, thumbnail);
            values.put(Constants.TAG_PROGRESS, "");
            values.put(Constants.TAG_DELIVERY_STATUS, deliveryStatus);

            getWritableDatabase().insertWithOnConflict(TABLE_CHANNEL_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addChannelRecentMsgs(String channelId, String messageId, String chatTime, String unreadCount) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_CHANNEL_ID, channelId);
            values.put(Constants.TAG_MESSAGE_ID, messageId);
            values.put(Constants.TAG_CHAT_TIME, chatTime);
            values.put(Constants.TAG_UNREAD_COUNT, unreadCount);

            getWritableDatabase().insertWithOnConflict(TABLE_CHANNEL_RECENT_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ChannelMessage> getChannelMessages(String channelId, String offset, String limit) {

        String selectQuery = "SELECT * FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID + "='" + channelId + "'" + " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC" + " LIMIT " + limit + " OFFSET " + offset;

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        List<ChannelMessage> channelMessages = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ChannelMessage message = new ChannelMessage();
                message.messageId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
                message.channelId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ID));
                message.messageType = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
                message.message = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
                message.chatType = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TYPE));
                message.attachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
                message.lat = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LAT)));
                message.lon = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LON)));
                message.contactName = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_NAME)));
                message.contactPhoneNo = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_PHONE_NO)));
                message.contactCountryCode = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_COUNTRY_CODE)));
                message.chatTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
                message.thumbnail = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL)));
                message.progress = cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS));
                message.isDelete = cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_DELETE));

                channelMessages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return channelMessages;
    }

    public ChannelMessage getSingleChannelMessage(String channelId, String messageId) {
        String selectQuery = "SELECT * FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID + "='" + channelId + "'" + " AND " + Constants.TAG_MESSAGE_ID + "='" + messageId + "'";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        ChannelMessage message = new ChannelMessage();

        if (cursor.moveToFirst()) {
            message.messageId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
            message.channelId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ID));
            message.messageType = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
            message.message = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
            message.chatType = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TYPE));
            message.attachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
            message.lat = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LAT)));
            message.lon = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LON)));
            message.contactName = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_NAME)));
            message.contactPhoneNo = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_PHONE_NO)));
            message.contactCountryCode = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_COUNTRY_CODE)));
            message.chatTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
            message.thumbnail = cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL));
            message.progress = cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS));
            message.isDelete = cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_DELETE));
        }
        return message;
    }

    public ArrayList<HashMap<String, String>> getChannelRecentMessages(Context context) {

        String selectQuery = "SELECT * FROM " + TABLE_CHANNEL_RECENT_MESSAGES + " LEFT JOIN " + TABLE_CHANNEL_MESSAGES + " ON " +
                TABLE_CHANNEL_RECENT_MESSAGES + "." + Constants.TAG_MESSAGE_ID + " = " + TABLE_CHANNEL_MESSAGES + "." + Constants.TAG_MESSAGE_ID +
                " INNER JOIN " + TABLE_CHANNEL + " ON " + TABLE_CHANNEL_RECENT_MESSAGES + "." + Constants.TAG_CHANNEL_ID +
                " = " + TABLE_CHANNEL + "." + Constants.TAG_CHANNEL_ID + " WHERE " + TABLE_CHANNEL + "." +
                Constants.TAG_CHANNEL_ADMIN_ID + " !='" + GetSet.getUserId() + "' AND " + TABLE_CHANNEL + "." + Constants.TAG_BLOCK_STATUS + " !='1' " +
                " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC";

//        Log.d(TAG, "getChannelRecentMessages: " + selectQuery);

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        ArrayList<HashMap<String, String>> recentAry = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<>();
                map.put(Constants.TAG_MESSAGE_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID)));
                map.put(Constants.TAG_CHANNEL_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ID)));
                map.put(Constants.TAG_CHANNEL_IMAGE, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_IMAGE)));
                map.put(Constants.TAG_CHANNEL_NAME, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_NAME)));
                map.put(Constants.TAG_CHANNEL_DES, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_DES)));
                map.put(Constants.TAG_MESSAGE_TYPE, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE)));
                ChannelResult.Result info = getChannelInfo(map.get(Constants.TAG_CHANNEL_ID));
                if (map.get(Constants.TAG_MESSAGE_TYPE) != null && map.get(Constants.TAG_MESSAGE_TYPE).equals(Constants.TAG_ISDELETE)) {
                    if (info.adminId != null && info.adminId.equals(GetSet.getUserId())) {
                        map.put(Constants.TAG_MESSAGE, context.getString(R.string.you_delete_this_message));
                    } else {
                        map.put(Constants.TAG_MESSAGE, context.getString(R.string.this_message_was_deleted));
                    }
                } else {
                    if (cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)) != null && !TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)))) {
                        map.put(Constants.TAG_MESSAGE, ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE))));
                    } else {
                        map.put(Constants.TAG_MESSAGE, "");
                    }
                }
                map.put(Constants.TAG_CHAT_TIME, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME)));
                map.put(Constants.TAG_CHANNEL_TYPE, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_TYPE)));
                map.put(Constants.TAG_UNREAD_COUNT, cursor.getString(cursor.getColumnIndex(Constants.TAG_UNREAD_COUNT)));
                map.put(Constants.TAG_MUTE_NOTIFICATION, cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION)));
                map.put(Constants.TAG_CHANNEL_CATEGORY, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_CATEGORY)));
                map.put(Constants.TAG_SUBSCRIBE_STATUS, cursor.getString(cursor.getColumnIndex(Constants.TAG_SUBSCRIBE_STATUS)));

                recentAry.add(map);
                //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
            } while (cursor.moveToNext());
        }
        cursor.close();

        Log.d(TAG, "getChannelRecentMessages: " + recentAry);

        return recentAry;
    }

    public HashMap<String, String> getRecentChannelMsg() {

        String selectQuery = "SELECT * FROM " + TABLE_CHANNEL_RECENT_MESSAGES +
                " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC" + " LIMIT " + "'1'";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        HashMap<String, String> map = new HashMap<>();
        if (cursor.moveToFirst()) {
            map.put(Constants.TAG_MESSAGE_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID)));
            map.put(Constants.TAG_CHANNEL_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ID)));
            map.put(Constants.TAG_CHAT_TIME, cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME)));
            map.put(Constants.TAG_UNREAD_COUNT, cursor.getString(cursor.getColumnIndex(Constants.TAG_UNREAD_COUNT)));
            //Log.v("Items", "Id="+cursor.getString(0)+"ItemId="+cursor.getString(1)+" Liked="+cursor.getString(2)+" Report="+cursor.getString(3)+" Share="+cursor.getString(4));
        }
        cursor.close();

        return map;
    }

    public int getUnseenChannelMessagesCount(String channelId) {
        String selectQuery = "SELECT * FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_DELIVERY_STATUS + " ='' AND " + Constants.TAG_CHANNEL_ID + "='" + channelId + "'";
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public boolean isRecentChannelIdExist(SQLiteDatabase db, String channelId) {
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_CHANNEL_RECENT_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID + "=?",
                new String[]{channelId});
        return line > 0;
    }

    public void updateChannelMessageData(String message_id, String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isChannelMessageIdExist(db, message_id);
        ContentValues values = new ContentValues();
        values.put(key, value);

        if (exists) {
            db.update(TABLE_CHANNEL_MESSAGES, values, Constants.TAG_MESSAGE_ID + " =? ",
                    new String[]{message_id});
        }
    }

    public void updateChannelReadData(String channelId, String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isChannelIdExist(db, channelId);
        ContentValues values = new ContentValues();
        values.put(key, value);

        if (exists) {
            db.update(TABLE_CHANNEL_MESSAGES, values, Constants.TAG_CHANNEL_ID + " =? ",
                    new String[]{channelId});
        }
    }

    public boolean isChannelMessageIdExist(SQLiteDatabase db, String id) {
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_MESSAGE_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    public boolean isChannelMessageIdExist(String id) {
        long line = DatabaseUtils.longForQuery(this.getWritableDatabase(), "SELECT COUNT(*) FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_MESSAGE_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    public boolean isChannelIdExist(SQLiteDatabase db, String id) {
        long line = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    public boolean isChannelIdExistInMessages(String channelId) {
        long line = DatabaseUtils.longForQuery(this.getWritableDatabase(), "SELECT COUNT(*) FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID + "=?",
                new String[]{channelId});
        return line > 0;
    }

    public boolean isChannelRecentIdExist(String channelId) {
        long line = DatabaseUtils.longForQuery(this.getWritableDatabase(), "SELECT COUNT(*) FROM " + TABLE_CHANNEL_RECENT_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID + "=?",
                new String[]{channelId});
        return line > 0;
    }

    public int getChannelMessagesCount(String channelId) {
        String selectQuery = "SELECT * FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID + "='" + channelId + "'";
        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public void updateChannelMessageReadStatus(String channelId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isChannelMessageIdExist(db, channelId);
        ContentValues values = new ContentValues();
        values.put(Constants.TAG_DELIVERY_STATUS, "read");

        if (exists) {
            db.update(TABLE_CHANNEL_MESSAGES, values, Constants.TAG_CHANNEL_ID + " =? ",
                    new String[]{channelId});
        }
    }

    public void resetUnseenChannelMessagesCount(String channelId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isRecentChannelIdExist(db, channelId);
        ContentValues values = new ContentValues();
        values.put(Constants.TAG_UNREAD_COUNT, "0");

        if (exists) {
            db.update(TABLE_CHANNEL_RECENT_MESSAGES, values, Constants.TAG_CHANNEL_ID + " =? ",
                    new String[]{channelId});
        }
    }

    public void deleteChannelMessages(String channelId) {
        String deleteQuery = "DELETE FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID +
                "='" + channelId + "'";
        getWritableDatabase().execSQL(deleteQuery);
    }

    public void deleteChannel(String channelId) {
        String deleteQuery = "DELETE FROM " + TABLE_CHANNEL + " WHERE " + Constants.TAG_CHANNEL_ID +
                "='" + channelId + "'";
        getWritableDatabase().execSQL(deleteQuery);
    }

    public void deleteChannelRecentMessages(String channelId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isRecentChannelIdExist(db, channelId);
        if (exists) {
            String deleteQuery = "DELETE FROM " + TABLE_CHANNEL_RECENT_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID +
                    "='" + channelId + "'";
            getWritableDatabase().execSQL(deleteQuery);

        }
    }

    // Insert a recent call into the database

    public void addRecentCall(String call_id, String user_id, String type,
                              String call_status, String created_at, String isAlert) {
        try {
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_CALL_ID, call_id);
            values.put(Constants.TAG_USER_ID, user_id);
            values.put(Constants.TAG_TYPE, type);
            values.put(Constants.TAG_CALL_STATUS, call_status);
            values.put(Constants.TAG_CREATED_AT, created_at);
            values.put(Constants.TAG_IS_ALERT, isAlert);

            getWritableDatabase().insertWithOnConflict(TABLE_CALL, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<HashMap<String, String>> getRecentCall() {
        String selectQuery = "SELECT * FROM " + TABLE_CALL + " INNER JOIN " + TABLE_CONTACTS + " ON " + TABLE_CALL + "." + Constants.TAG_USER_ID + " = " +
                TABLE_CONTACTS + "." + Constants.TAG_USER_ID + " ORDER BY " + Constants.TAG_CREATED_AT + " DESC";
        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        ArrayList<HashMap<String, String>> recentAry = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<>();
                map.put(Constants.TAG_CALL_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_CALL_ID)));
                map.put(Constants.TAG_USER_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID)));
                map.put(Constants.TAG_TYPE, cursor.getString(cursor.getColumnIndex(Constants.TAG_TYPE)));
                map.put(Constants.TAG_CALL_STATUS, cursor.getString(cursor.getColumnIndex(Constants.TAG_CALL_STATUS)));
                map.put(Constants.TAG_CREATED_AT, cursor.getString(cursor.getColumnIndex(Constants.TAG_CREATED_AT)));
                map.put(Constants.TAG_USER_IMAGE, cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_IMAGE)));
                map.put(Constants.TAG_ABOUT, cursor.getString(cursor.getColumnIndex(Constants.TAG_ABOUT)));
                map.put(Constants.TAG_PHONE_NUMBER, cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER)));
                map.put(Constants.TAG_COUNTRY_CODE, cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE)));
                map.put(Constants.TAG_USER_NAME, getUserName(cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME)),
                        map.get(Constants.TAG_PHONE_NUMBER), map.get(Constants.TAG_COUNTRY_CODE)));
                map.put(Constants.TAG_PRIVACY_ABOUT, cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_ABOUT)));
                map.put(Constants.TAG_PRIVACY_PROFILE, cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_PROFILE)));
                map.put(Constants.TAG_PRIVACY_LAST_SEEN, cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_LAST_SEEN)));
                map.put(Constants.TAG_CONTACT_STATUS, cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_STATUS)));
                map.put(Constants.TAG_IS_ALERT, cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_ALERT)));
                map.put(Constants.TAG_IS_SELECTED, "0");
                recentAry.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return recentAry;
    }

    public boolean isCallIdExist(String id) {
        long line = DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + TABLE_CALL + " WHERE " + Constants.TAG_CALL_ID + "=?",
                new String[]{id});
        return line > 0;
    }

    public CallData.Result getCallData(String Id) {
        String selectQuery = "SELECT * FROM " + TABLE_CALL + " WHERE " + Constants.TAG_CALL_ID + " ='" + Id + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = isCallIdExist(Id);
        if (exists) {
            Cursor cursor = db.rawQuery(selectQuery, null);
            // looping through all rows and adding to list
            CallData.Result data = new CallData().new Result();
            if (cursor != null)
                cursor.moveToFirst();
            data.callId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CALL_ID));
            data.userId = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID));
            data.type = cursor.getString(cursor.getColumnIndex(Constants.TAG_TYPE));
            data.callStatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_CALL_STATUS));
            data.createdAt = cursor.getString(cursor.getColumnIndex(Constants.TAG_CREATED_AT));
            cursor.close();
            return data;
        } else {
            return null;
        }
    }

    public boolean isRecentChatIndicationExist() {
        long line = DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + TABLE_RECENTS + " WHERE " + Constants.TAG_UNREAD_COUNT + "!=?",
                new String[]{"0"});
        return line > 0;
    }

    public boolean isRecentGroupIndicationExist() {
        long line = DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + TABLE_GROUP_RECENT_MESSAGES + " WHERE " + Constants.TAG_UNREAD_COUNT + "!=?",
                new String[]{"0"});
        return line > 0;
    }

    public boolean isRecentChannelIndicationExist() {
        long line = DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + TABLE_CHANNEL_RECENT_MESSAGES + " WHERE " + Constants.TAG_UNREAD_COUNT + "!=?",
                new String[]{"0"});
        return line > 0;
    }

    public boolean isMissedCallIndicationExist() {

        long line = DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + TABLE_CALL + " WHERE " + Constants.TAG_IS_ALERT + "!=?",
                new String[]{"1"});
        return line > 0;
    }

    public void clearDB(Context context) {
        SQLiteDatabase db = getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        for (String tableName : ALL_TABLES) {
            db.delete(tableName, null, null);
        }
    }

    public List<MediaModelData> getMedia(String chat_id, String type, Context context) throws Exception {
        String selectQuery = "";

        if (type.equals(Constants.TAG_CHANNEL)) {
            selectQuery = "SELECT * FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID + "='" +
                    chat_id + "' AND " /*+ Constants.TAG_PROGRESS + "='completed'" + " AND "*/ + Constants.TAG_MESSAGE_TYPE + " IN " + "('image','video','document')" +
                    " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC";
        } else if (type.equals(Constants.TAG_GROUP)) {
            selectQuery = "SELECT * FROM " + TABLE_GROUP_MESSAGES + " WHERE " + Constants.TAG_GROUP_ID + "='" +
                    chat_id + "' AND " /*+ Constants.TAG_PROGRESS + "='completed'" + " AND " */ + Constants.TAG_MESSAGE_TYPE + " IN " + "('image','video','document')" +
                    " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC";
        } else {
            selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + Constants.TAG_CHAT_ID + "='" +
                    GetSet.getUserId() + chat_id + "' AND "/* + Constants.TAG_PROGRESS + "='completed'" + " AND " */ + Constants.TAG_MESSAGE_TYPE + " IN " + "('image','video','document')" +
                    " ORDER BY " + Constants.TAG_CHAT_TIME + " DESC";
        }

        StorageManager storageManager = StorageManager.getInstance(context);

        if (!selectQuery.equals("")) {
            Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
            List<MediaModelData> messageAry = new ArrayList<>();
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    MediaModelData results = new MediaModelData();
                    results.message_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
                    results.message_type = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
                    results.attachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
                    results.chat_time = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
                    results.thumbnail = cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL));

                    if (results.message_type.equalsIgnoreCase("video") || results.message_type.equalsIgnoreCase("document")) {
                        if (storageManager.checkifFileExists(results.attachment, results.message_type, "sent")) {
                            messageAry.add(results);
                        } else if (storageManager.checkifFileExists(results.attachment, results.message_type, "receive")) {
                            messageAry.add(results);
                        }
                    } else {
                        if (storageManager.checkifImageExists("sent", results.attachment)) {
                            messageAry.add(results);
                        } else if (storageManager.checkifImageExists("thumb", results.attachment)) {
                            messageAry.add(results);
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();

            return messageAry;
        }
        return null;
    }

    public void createStatus(String status_id, String time, String type, String senderId,
                             String attachment, String message, String isViewed, String thumbnail,
                             String expiryTime, String members) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_STATUS_ID, status_id);
            values.put(Constants.TAG_STATUS_TIME, time);
            values.put(Constants.TAG_TYPE, type);
            values.put(Constants.TAG_SENDER_ID, senderId);
            values.put(Constants.TAG_ATTACHMENT, attachment);
            values.put(Constants.TAG_MESSAGE, message);
            values.put(Constants.TAG_STATUS_VIEW, isViewed);
            values.put(Constants.TAG_THUMBNAIL, thumbnail);
            values.put(Constants.TAG_STORY_MEMBERS, members);

            Long expiry = Long.parseLong(expiryTime) / 1000;
            values.put(Constants.TAG_EXPIRY_TIME, String.valueOf(expiry));

            db.insertWithOnConflict(TABLE_STATUS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void createStatusView(String statusId, String senderId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_STATUS_ID, statusId);
            values.put(Constants.TAG_MEMBER_ID, senderId);
            values.put(Constants.TAG_STATUS_TIME, String.valueOf(System.currentTimeMillis() / 1000L));
            values.put(Constants.TAG_MEMBER_KEY, statusId + senderId);

            db.insertWithOnConflict(TABLE_STATUS_VIEW, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteStatus(String storyId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Order of deletions is important when foreign key relationships exist.
            db.delete(TABLE_STATUS, Constants.TAG_STATUS_ID + " =? ", new String[]{storyId});
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete all posts and users");
        }
    }

    public List<StatusDatas> getSingleUserStatus(String userId) {
        String selectQuery = "SELECT * FROM " + TABLE_STATUS + " WHERE " + Constants.TAG_SENDER_ID + "='" + userId + "'" + " ORDER BY " + Constants.TAG_STATUS_TIME + " ASC";

        List<StatusDatas> data = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                StatusDatas datas = new StatusDatas();
                datas.mSenderId = cursor.getString(cursor.getColumnIndex(Constants.TAG_SENDER_ID));
                datas.mStatusTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_TIME));
                datas.mStatusId = cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_ID));
                datas.mAttachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
                datas.mThumbnail = cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL));
                datas.mType = cursor.getString(cursor.getColumnIndex(Constants.TAG_TYPE));
                datas.mMessage = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
                datas.mIsSeen = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_VIEW)));
                data.add(datas);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return data;
    }

    public StatusDatas getSingleStatus(String statusID) {
        String selectQuery = "SELECT * FROM " + TABLE_STATUS + " WHERE " + Constants.TAG_STATUS_ID + "='" + statusID + "'";

        StatusDatas datas = new StatusDatas();
        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        if (cursor != null)
            cursor.moveToFirst();

        if (cursor.getCount() > 0) {
            datas.mSenderId = cursor.getString(cursor.getColumnIndex(Constants.TAG_SENDER_ID));
            datas.mStatusTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_TIME));
            datas.mStatusId = cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_ID));
            datas.mAttachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
            datas.mThumbnail = cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL));
            datas.mType = cursor.getString(cursor.getColumnIndex(Constants.TAG_TYPE));
            datas.mMessage = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
        }
        cursor.close();
        return datas;
    }

    private void deleteOldStatus() {
        long currentTime = System.currentTimeMillis() / 1000;
        String[] projection = {Constants.TAG_STATUS_ID, Constants.TAG_EXPIRY_TIME, Constants.TAG_ATTACHMENT, Constants.TAG_SENDER_ID,
                Constants.TAG_TYPE};
        String selection = Constants.TAG_EXPIRY_TIME + "<?"; //this must be array
        String[] selectionArgs = new String[]{"" + currentTime}; //this must be array

        Cursor cursor = getWritableDatabase().query(TABLE_STATUS, projection, selection, selectionArgs, null, null, null);
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String statusId = cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_ID));
                String fileName = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
                String senderId = cursor.getString(cursor.getColumnIndex(Constants.TAG_SENDER_ID));
                String type = cursor.getString(cursor.getColumnIndex(Constants.TAG_TYPE));
                try {
                    File statusFile;
                    if (senderId.equals(GetSet.getUserId())) {
                        if (type.equals(StorageManager.TAG_VIDEO)) {
                            statusFile = new File(storageManager.getFilePath(StorageManager.TAG_VIDEO_SENT, fileName));
                        } else {
                            statusFile = new File(storageManager.getFilePath(StorageManager.TAG_SENT, fileName));
                        }
                    } else {
                        statusFile = new File(storageManager.getFilePath(StorageManager.TAG_STATUS, fileName));
                    }

                    if (!TextUtils.isEmpty(statusFile.getAbsolutePath()) && statusFile.exists()) {
                        storageManager.deleteFile(statusFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                deleteStatus(statusId);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }


    public List<ContactsData.Result> getStatusContactData(Context context) {
        deleteOldStatus();

        List<ContactsData.Result> contactData = new ArrayList<>();

        if (isStoryUserExists(GetSet.getUserId())) {
            ContactsData.Result results = new ContactsData().new Result();
            results.user_id = GetSet.getUserId();
            results.user_name = context.getString(R.string.you);
            results.user_image = GetSet.getImageUrl();
            results.is_View = "1";
            results.blockedme = "";
            results.blockedbyme = "";
            results.phone_no = GetSet.getphonenumber();
            results.country_code = GetSet.getcountrycode();
            results.privacy_last_seen = "";
            results.privacy_profile_image = "";
            results.privacy_about = "";
            results.about = "";
            results.mute_notification = "";
            results.contactstatus = "";
            results.favourited = "";

            contactData.add(results);

        }

        String selectQueryUnseen = "SELECT * FROM " + TABLE_STATUS + " JOIN " +
                TABLE_CONTACTS + " WHERE " + TABLE_STATUS + "." + Constants.TAG_SENDER_ID + "=" +
                TABLE_CONTACTS + "." + TAG_USER_ID + " AND " + TABLE_STATUS + "." + Constants.TAG_SENDER_ID + "!='" + GetSet.getUserId() + "' AND " + TABLE_CONTACTS + "." + Constants.TAG_CONTACT_STATUS + "='true' "
                + " GROUP BY " + Constants.TAG_SENDER_ID + " ORDER BY " + TABLE_STATUS + "." + Constants.TAG_STATUS_VIEW + " ASC,"
                + TABLE_STATUS + "." + Constants.TAG_STATUS_TIME + " DESC";

        contactData.addAll(getContact(context, selectQueryUnseen));

        Log.d(TAG, "getContact: " + new Gson().toJson(contactData));

        return contactData;
    }

    private List<ContactsData.Result> getContact(Context context, String query) {

        List<ContactsData.Result> data = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(query, null);
        Log.d(TAG, "getContact: " + cursor.getCount());
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                ContactsData.Result results = new ContactsData().new Result();
                results.user_id = cursor.getString(cursor.getColumnIndex(Constants.TAG_SENDER_ID));
                results.user_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_IMAGE));
                results.phone_no = cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER));
                results.country_code = cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE));
                if (results.user_id.equals(GetSet.getUserId())) {
                    results.user_name = context.getString(R.string.you);
                } else {
                    results.user_name = getUserName(cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME)),
                            results.phone_no, results.country_code);
                }
                results.privacy_last_seen = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_LAST_SEEN));
                results.privacy_profile_image = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_PROFILE));
                results.privacy_about = cursor.getString(cursor.getColumnIndex(Constants.TAG_PRIVACY_ABOUT));
                results.blockedme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_ME));
                results.blockedbyme = cursor.getString(cursor.getColumnIndex(Constants.TAG_BLOCKED_BYME));
                results.about = cursor.getString(cursor.getColumnIndex(Constants.TAG_ABOUT));
                results.mute_notification = cursor.getString(cursor.getColumnIndex(Constants.TAG_MUTE_NOTIFICATION));
                results.contactstatus = cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_STATUS));
                results.favourited = cursor.getString(cursor.getColumnIndex(Constants.TAG_FAVOURITED));
                results.is_View = cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_VIEW));

                results.saved_name = cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME));

                HashMap<String, String> map = ApplicationClass.getContactrNot(context, results.phone_no);
                if (results.user_id.equals(GetSet.getUserId())) {
                    data.add(results);
                } else if (map.get("isAlready").equals("true")) {
                    Log.d(TAG, "getContact: " + map.get(Constants.TAG_USER_NAME));
                    data.add(results);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        return data;
    }

    public ArrayList<HashMap<String, String>> getViewedContact(Context context, String storyId) {
        ArrayList<HashMap<String, String>> viewedContact = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_STATUS_VIEW + " JOIN " + TABLE_CONTACTS + " WHERE " + Constants.TAG_STATUS_ID + "='" + storyId + "' AND " + TABLE_STATUS_VIEW + "." + Constants.TAG_MEMBER_ID + "=" +
                TABLE_CONTACTS + "." + TAG_USER_ID + " ORDER BY " + TABLE_STATUS_VIEW + "." + Constants.TAG_STATUS_TIME + " DESC";

        Log.d(TAG, "getViewedContact: " + selectQuery);
        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<>();
                map.put(TAG_USER_ID, cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_ID)));
                map.put(Constants.TAG_USER_IMAGE, cursor.getString(cursor.getColumnIndex(Constants.TAG_USER_IMAGE)));
                map.put(Constants.TAG_PHONE_NUMBER, cursor.getString(cursor.getColumnIndex(Constants.TAG_PHONE_NUMBER)));
                map.put(Constants.TAG_COUNTRY_CODE, cursor.getString(cursor.getColumnIndex(Constants.TAG_COUNTRY_CODE)));
                map.put(Constants.TAG_USER_NAME, getUserName(cursor.getString(cursor.getColumnIndex(Constants.TAG_SAVED_NAME)),
                        map.get(Constants.TAG_PHONE_NUMBER), map.get(Constants.TAG_COUNTRY_CODE)));
                map.put(Constants.TAG_STATUS_TIME, cursor.getString(cursor.getColumnIndex(Constants.TAG_STATUS_TIME)));
                viewedContact.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return viewedContact;
    }

    public void updateStatusSeen(String statusId) {
        if (isStoryExists(statusId)) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_STATUS_VIEW, "1");
            db.update(TABLE_STATUS, values, Constants.TAG_STATUS_ID + " =? ",
                    new String[]{statusId});
        }
    }

    public void updateCallAlert(String callId) {
        if (isCallIdExist(callId)) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(Constants.TAG_IS_ALERT, "1");
            db.update(TABLE_CALL, values, Constants.TAG_CALL_ID + " =? ",
                    new String[]{callId});
        }
    }

    public String getStatusMember(String statusId) {
        String selectQuery = "SELECT * FROM " + TABLE_STATUS +
                " WHERE " + Constants.TAG_STATUS_ID + "='" + statusId + "'";

        Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
        String members = "";
        if (cursor != null) {
            cursor.moveToFirst();

            if (cursor.moveToFirst()) {
                members = cursor.getString(cursor.getColumnIndex(Constants.TAG_STORY_MEMBERS)).trim();
            }
            cursor.close();
        }
        return members;
    }

    public List<GroupMessage> getGroupUnreadMessages(String groupId, Context context) {

        String selectQuery = "SELECT * FROM " + TABLE_GROUP_MESSAGES + " WHERE " + Constants.TAG_GROUP_ID + "='" + groupId
                + "' AND " + Constants.TAG_MEMBER_ID + "!='" + GetSet.getUserId() + "' AND "
                + Constants.TAG_DELIVERY_STATUS + "!='read'" + " ORDER BY " + Constants.TAG_CHAT_TIME + " ASC";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        List<HashMap<String, String>> recentAry = new ArrayList<>();
        List<GroupMessage> groupMessage = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                GroupMessage message = new GroupMessage();
                message.groupId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ID));
                message.messageId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
                message.messageType = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
                message.memberId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MEMBER_ID));
                message.groupAdminId = cursor.getString(cursor.getColumnIndex(Constants.TAG_GROUP_ADMIN_ID));
                message.message = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
                message.attachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
                message.lat = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LAT)));
                message.lon = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LON)));
                message.contactName = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_NAME)));
                message.contactPhoneNo = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_PHONE_NO)));
                message.contactCountryCode = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_COUNTRY_CODE)));
                message.chatTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
                message.thumbnail = cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL));
                message.progress = cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS));
                message.isDelete = cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_DELETE));
//                groupMessage.add(message);
                groupMessage.add(GroupChatActivity.getMessages(this, context, message));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return groupMessage;
    }

    public List<ChannelMessage> getChannelUnreadMessages(String channelId) {

        String selectQuery = "SELECT * FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID + "='"
                + channelId + "' AND " + Constants.TAG_DELIVERY_STATUS + "!='read' AND " +
                Constants.TAG_MESSAGE_TYPE + "!='create_channel'" + " ORDER BY " + Constants.TAG_CHAT_TIME + " ASC";

        Cursor cursor = getWritableDatabase().rawQuery(selectQuery, null);
        List<ChannelMessage> channelMessages = new ArrayList<>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ChannelMessage message = new ChannelMessage();
                message.messageId = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_ID));
                message.channelId = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHANNEL_ID));
                message.messageType = cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE_TYPE));
                message.message = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_MESSAGE)));
                message.chatType = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TYPE));
                message.attachment = cursor.getString(cursor.getColumnIndex(Constants.TAG_ATTACHMENT));
                message.lat = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LAT)));
                message.lon = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_LON)));
                message.contactName = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_NAME)));
                message.contactPhoneNo = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_PHONE_NO)));
                message.contactCountryCode = ApplicationClass.decryptMessage(cursor.getString(cursor.getColumnIndex(Constants.TAG_CONTACT_COUNTRY_CODE)));
                message.chatTime = cursor.getString(cursor.getColumnIndex(Constants.TAG_CHAT_TIME));
                message.thumbnail = cursor.getString(cursor.getColumnIndex(Constants.TAG_THUMBNAIL));
                message.progress = cursor.getString(cursor.getColumnIndex(Constants.TAG_PROGRESS));
                message.isDelete = cursor.getString(cursor.getColumnIndex(Constants.TAG_IS_DELETE));

                channelMessages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        Log.i(TAG, "getChannelUnreadMessages: " + channelMessages.size());
        return channelMessages;
    }

    public boolean getChannelUnreadCount(String channelId) {
        Log.i(TAG, "getChannelUnreadCount: " + channelId);
        long line = DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM " + TABLE_CHANNEL_MESSAGES + " WHERE " + Constants.TAG_CHANNEL_ID + "=?" +
                        " AND " + Constants.TAG_DELIVERY_STATUS + "!=? AND " + Constants.TAG_MESSAGE_TYPE + "!=?" + " ORDER BY " + Constants.TAG_CHAT_TIME + " ASC",
                new String[]{"1", "read", "create_channel"});
        Log.i(TAG, "getChannelUnreadCount: " + line);
        return line > 0;
    }

    public String getUserName(String name, String phoneNumber, String countryCode) {
        if (name != null && !name.equals("")) {
            return name;
        } else {
            if (countryCode != null && !countryCode.equals("")) {
                return "+" + countryCode + " " + phoneNumber;
            } else {
                return phoneNumber;
            }
        }
    }

    private String getMessage(String userId, String messageType, String message, Context context) {
        if (messageType != null && messageType.equals(Constants.TAG_ISDELETE)) {
            if (userId != null && userId.equals(GetSet.getUserId())) {
                return context.getString(R.string.you_delete_this_message);
            } else {
                return context.getString(R.string.this_message_was_deleted);
            }
        } else {
            return message;
        }
    }

}
