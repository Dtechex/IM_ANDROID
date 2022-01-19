package com.loopytime.utils;

import com.loopytime.model.AdminChannel;
import com.loopytime.model.AdminChannelMsg;
import com.loopytime.model.BlocksData;
import com.loopytime.model.CallData;
import com.loopytime.model.ChangeNumberResult;
import com.loopytime.model.ChannelChatResult;
import com.loopytime.model.ChannelResult;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupChatResult;
import com.loopytime.model.GroupInvite;
import com.loopytime.model.GroupResult;
import com.loopytime.model.GroupUpdateResult;
import com.loopytime.model.HelpData;
import com.loopytime.model.RecentsData;
import com.loopytime.model.SaveMyContacts;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Created by hitasoft on 24/1/18.
 */

public interface ApiInterface {

    @GET("service/admindatas")
    Call<HashMap<String, String>> adminData();

    @FormUrlEncoded
    @POST("service/signin")
    Call<HashMap<String, String>> signin(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("service/updatemycontacts")
    Call<ContactsData> updatemycontacts(@Header("Authorization") String user_token, @FieldMap Map<String, String> params);


    @FormUrlEncoded
    @POST("service/updatemyprofile")
    Call<HashMap<String, String>> updatemyprofile(@Header("Authorization") String user_token, @FieldMap Map<String, String> params);

    @Multipart
    @POST("service/upmyprofile")
    Call<HashMap<String, String>> upmyprofile(@Header("Authorization") String user_token, @Part MultipartBody.Part image, @Part("user_id") RequestBody user_id);

    @Multipart
    @POST("service/upmychat")
    Call<HashMap<String, String>> upmychat(@Header("Authorization") String user_token, @Part MultipartBody.Part attachment, @Part("user_id") RequestBody user_id);

    @FormUrlEncoded
    @POST("service/pushsignin")
    Call<Map<String, String>> pushsignin(@Header("Authorization") String user_token, @FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("service/pushsignout")
    Call<Map<String, String>> pushsignout(@Header("Authorization") String user_token, @FieldMap Map<String, String> params);

    @GET("service/getuserprofile/{phone_no}/{contact_id}")
    Call<Map<String, String>> getuserprofile(@Header("Authorization") String user_token, @Path("phone_no") String phone_no, @Path("contact_id") String contact_id);

    @GET("service/recentchats/{user_id}")
    Call<RecentsData> recentchats(@Header("Authorization") String user_token, @Path("user_id") String user_id);

    @GET("service/getblockstatus/{user_id}")
    Call<BlocksData> getblockstatus(@Header("Authorization") String user_token, @Path("user_id") String user_id);

    @Multipart
    @POST("service/modifyGroupimage")
    Call<HashMap<String, String>> uploadGroupImage(@Header("Authorization") String user_token, @Part MultipartBody.Part image, @Part("group_id") RequestBody groupId);

    @GET("/service/groupinvites/{user_id}")
    Call<GroupInvite> getGroupInvites(@Header("Authorization") String token, @Path("user_id") String userId);

    @FormUrlEncoded
    @POST("service/groupinfo")
    Call<GroupResult> getGroupInfo(@Header("Authorization") String token, @Field("group_list") String group_list);

    @FormUrlEncoded
    @POST("service/deviceinfo")
    Call<Map<String, String>> deviceinfo(@Header("Authorization") String user_token, @FieldMap Map<String, String> params);

    @Multipart
    @POST("service/upmychat")
    Call<Map<String, String>> upchat(@Header("Authorization") String user_token, @Part MultipartBody.Part attachment, @Part("user_id") RequestBody user_id);

    @Multipart
    @POST("service/upmygroupchat")
    Call<HashMap<String, String>> uploadGroupChat(@Header("Authorization") String user_token, @Part MultipartBody.Part attachment, @Part("user_id") RequestBody user_id);

    @Multipart
    @POST("service/upmygroupchat")
    Call<Map<String, String>> upGroupchat(@Header("Authorization") String user_token, @Part MultipartBody.Part attachment, @Part("user_id") RequestBody user_id);

    @FormUrlEncoded
    @POST("service/chatreceived")
    Call<Map<String, String>> chatreceived(@Header("Authorization") String user_token, @FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("service/modifyGroupinfo")
    Call<GroupUpdateResult> updateGroup(@Header("Authorization") String user_token, @Field("group_id") String groupId,
                                        @Field("group_name") String groupName);

    @FormUrlEncoded
    @POST("service/modifyGroupinfo")
    Call<GroupUpdateResult> updateGroup(@Header("Authorization") String user_token, @Field("group_id") String groupId,
                                        @Field("group_members") JSONArray members);

    @GET("service/recentgroupchats/{user_id}")
    Call<GroupChatResult> getRecentGroupChats(@Header("Authorization") String token, @Path("user_id") String userId);

    @FormUrlEncoded
    @POST("service/savemycontacts")
    Call<SaveMyContacts> saveMyContacts(@Header("Authorization") String user_token, @FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("service/updatemyprivacy")
    Call<HashMap<String, String>> updateMyPrivacy(@Header("Authorization") String user_token, @FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("service/modifyGroupmembers")
    Call<HashMap<String, String>> modifyGroupmembers(@Header("Authorization") String user_token, @Field("user_id") String userId, @Field("group_id") String groupId,
                                                     @Field("group_members") JSONArray members);

    @GET("service/MySubscribedChannels/{user_id}")
    Call<ChannelResult> getMySubscribedChannels(@Header("Authorization") String user_token, @Path("user_id") String userId);

    @GET("service/MyChannels/{user_id}")
    Call<ChannelResult> getMyChannels(@Header("Authorization") String user_token, @Path("user_id") String userId);

    @GET("service/helps")
    Call<HelpData> getHelpList();

    @Multipart
    @POST("service/modifyChannelImage")
    Call<HashMap<String, String>> uploadChannelImage(@Header("Authorization") String user_token, @Part MultipartBody.Part body, @Part("channel_id") RequestBody channelID);

    @GET("service/recentcalls/{user_id}")
    Call<CallData> recentcalls(@Header("Authorization") String user_token, @Path("user_id") String user_id);

    @FormUrlEncoded
    @POST("service/channelinfo")
    Call<ChannelResult> getChannelInfo(@Header("Authorization") String user_token, @Field("channel_list") JSONArray channelList);

    @FormUrlEncoded
    @POST("service/updatemychannel")
    Call<HashMap<String, String>> updateChannel(@Header("Authorization") String user_token, @FieldMap HashMap<String, String> map);

    @Multipart
    @POST("service/upmychannelchat")
    Call<HashMap<String, String>> uploadChannelChat(@Header("Authorization") String user_token, @Part MultipartBody.Part attachment, @Part("channel_id") RequestBody channel_Id, @Part("user_id") RequestBody userId);

    @Multipart
    @POST("service/upmychannelchat")
    Call<Map<String, String>> upChannelChat(@Header("Authorization") String user_token, @Part MultipartBody.Part attachment, @Part("channel_id") RequestBody channel_Id, @Part("user_id") RequestBody userId);

    @GET("service/recentChannelChats/{user_id}")
    Call<ChannelChatResult> recentChannelChats(@Header("Authorization") String user_token, @Path("user_id") String user_id);

    @GET("service/adminchannels/{user_id}")
    Call<AdminChannel> getAdminChannels(@Header("Authorization") String user_token, @Path("user_id") String user_id);

    @GET("service/msgfromadminchannels/{timestamp}")
    Call<AdminChannelMsg> getMsgFromAdminChannels(@Header("Authorization") String user_token, @Path("timestamp") String timestamp);

    @GET("service/recentChannelInvites/{user_id}")
    Call<ChannelResult> getRecentChannelInvites(@Header("Authorization") String user_token, @Path("user_id") String user_id);

    @GET("service/AllPublicChannels/{user_id}/{search_string}/{offset}/{limit}")
    Call<ChannelResult> getAllPublicChannels(@Header("Authorization") String user_token, @Path("user_id") String user_id, @Path("search_string") String search, @Path("offset") String offSet, @Path("limit") String limit);

    @GET("service/channelSubscribers/{channel_id}/{phone_no}/{offset}/{limit}")
    Call<ContactsData> getChannelSubscribers(@Header("Authorization") String user_token, @Path("channel_id") String channel_id, @Path("phone_no") String phoneNo, @Path("offset") String offSet, @Path("limit") String limit);

    @GET("service/MyGroups/{user_id}")
    Call<GroupResult> getMyGroups(@Header("Authorization") String user_token, @Path("user_id") String userId);

    @GET("service/deleteMyAccount/{user_id}")
    Call<HashMap<String, String>> deleteMyAccount(@Header("Authorization") String user_token, @Path("user_id") String userId);

    @GET("service/verifyMyNumber/{user_id}/{phone_no}")
    Call<Map<String, String>> verifyNewNumber(@Header("Authorization") String user_token, @Path("user_id") String userId, @Path("phone_no") String phoneNumber);


    @GET("service/changeMyNumber/{user_id}/{phone_no}/{country_code}")
    Call<ChangeNumberResult> changeMyNumber(@Header("Authorization") String user_token, @Path("user_id") String userId, @Path("phone_no") String phoneNumber, @Path("country_code") String countryCode);

    @GET("service/checkforupdates")
    Call<HashMap<String, String>> checkForUpdates();

    @FormUrlEncoded
    @POST("service/reportchannel")
    Call<HashMap<String,String>> reportChannel(@Header("Authorization") String user_token, @FieldMap HashMap<String, String> hashMap);
}

