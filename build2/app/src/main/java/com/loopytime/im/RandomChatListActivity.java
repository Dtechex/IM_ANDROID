package com.loopytime.im;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.MaterialColor;
import com.loopytime.helper.MaterialColors;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.Utils;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;

public class RandomChatListActivity extends AppCompatActivity {
    private static final String TAG = "lockChats";
    TextView name, gen, bday, phone,city;
    CircleImageView avatar;
    RandomProfile randomProfile;
    MaterialColor color;
    DatabaseHandler dbhelper;
    void setColor() {
        color = MaterialColors.CONVERSATION_PALETTE.get(this.getSharedPreferences("wall", Context.MODE_PRIVATE).getInt(MaterialColors.THEME, 0));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color.toActionBarColor(this)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color.toStatusBarColor(this));
        }
        tintShape(this, color.toActionBarColor(this), ContextCompat.getDrawable(this, R.drawable.fablay_bg));
    }


    void tintShape(Context mContext, int color, Drawable background) {

        if (background instanceof ShapeDrawable) {
            ((ShapeDrawable) background).getPaint().setColor(color);
        } else if (background instanceof GradientDrawable) {
            ((GradientDrawable) background).setColor(color);
        } else if (background instanceof ColorDrawable) {
            ((ColorDrawable) background).setColor(color);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random_chat_list);
        dbhelper = DatabaseHandler.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "2");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Random Chat List "+GetSet.isIsRandomOn());
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Activity");
        ApplicationClass.getInstance().mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.ran_chats);
        gen = findViewById(R.id.gen);
        city = findViewById(R.id.city);
        name = findViewById(R.id.name);
        bday = findViewById(R.id.dob);
        phone = findViewById(R.id.phone);
        avatar = findViewById(R.id.avatarView);
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(RandomChatListActivity.this, RandomChatSettingActivity.class));
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_ITEM = 1;
        ArrayList<HashMap<String, String>> Items;
        Context context;

        public RecyclerViewAdapter(Context context, ArrayList<HashMap<String, String>> Items) {
            this.Items = Items;
            this.context = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item, parent, false);
            return new RecyclerViewAdapter.MyViewHolder(itemView);
            /*if (viewType == TYPE_ITEM) {
            } else if (viewType == TYPE_HEADER) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.favorites_header, parent, false);
                return new HeaderViewHolder(itemView);
            }
            return null;*/
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position) {

            if (viewHolder instanceof RecyclerViewAdapter.MyViewHolder) {
                RecyclerViewAdapter.MyViewHolder holder = (RecyclerViewAdapter.MyViewHolder) viewHolder;
                holder.typing.setVisibility(View.GONE);
                holder.messageLay.setVisibility(View.VISIBLE);
                HashMap<String, String> map = Items.get(position);
                holder.name.setText(map.get(Constants.TAG_USER_NAME));
                holder.message.setText(map.get(Constants.TAG_MESSAGE));

                if (map.get(Constants.TAG_CHAT_TIME) != null) {
                    holder.time.setText(Utils.getFormattedDate(context, Long.parseLong(map.get(Constants.TAG_CHAT_TIME).replace(".0", ""))));
                }

                if (map.get(Constants.TAG_BLOCKED_ME).equals("block")) {
                    Glide.with(context).load(R.drawable.temp).thumbnail(0.5f)
                            .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                            .into(holder.profileimage);
                } else {
                    //               DialogActivity.setProfileImage(dbhelper.getContactDetail(map.get(Constants.TAG_USER_ID)), holder.profileimage, context);
                }

                if (map.get(Constants.TAG_SENDER_ID) != null && map.get(Constants.TAG_SENDER_ID).equals(GetSet.getUserId())) {
                    holder.tickimage.setVisibility(View.VISIBLE);
                    if (map.get(Constants.TAG_MESSAGE_TYPE) != null) {

                        if (map.get(Constants.TAG_DELIVERY_STATUS).equals("read")) {
                            holder.tickimage.setImageResource(R.drawable.double_tick);
                        } else if (map.get(Constants.TAG_DELIVERY_STATUS).equals("sent")) {

                            holder.tickimage.setImageResource(R.drawable.double_tick_unseen);
                        } else if (map.get(Constants.TAG_PROGRESS) != null && map.get(Constants.TAG_PROGRESS).equals("completed") && (map.get(Constants.TAG_MESSAGE_TYPE).equals("image") ||
                                map.get(Constants.TAG_MESSAGE_TYPE).equals("video") || map.get(Constants.TAG_MESSAGE_TYPE).equals("file") || map.get(Constants.TAG_MESSAGE_TYPE).equals("audio"))) {
                            holder.tickimage.setImageResource(R.drawable.single_tick);
                        } else if (map.get(Constants.TAG_MESSAGE_TYPE).equals("text") || map.get(Constants.TAG_MESSAGE_TYPE).equals("contact") || map.get(Constants.TAG_MESSAGE_TYPE).equals("location")) {
                            holder.tickimage.setImageResource(R.drawable.single_tick);
                        } else {
                            holder.tickimage.setVisibility(View.GONE);
                        }
                    }
                } else {
                    holder.tickimage.setVisibility(View.GONE);
                }

                if (map.get(Constants.TAG_MESSAGE_TYPE) != null) {
                    switch (map.get(Constants.TAG_MESSAGE_TYPE)) {
                        case "image":
                        case "video":
                            holder.typeicon.setVisibility(View.VISIBLE);
                            holder.typeicon.setImageResource(R.drawable.upload_gallery);
                            break;
                        case "location":
                            holder.typeicon.setVisibility(View.VISIBLE);
                            holder.typeicon.setImageResource(R.drawable.upload_location);
                            break;
                        case "audio":
                            holder.typeicon.setVisibility(View.VISIBLE);
                            holder.typeicon.setImageResource(R.drawable.upload_audio);
                            break;
                        case "contact":
                            holder.typeicon.setVisibility(View.VISIBLE);
                            holder.typeicon.setImageResource(R.drawable.upload_contact);
                            break;
                        case "file":
                            holder.typeicon.setVisibility(View.VISIBLE);
                            holder.typeicon.setImageResource(R.drawable.upload_file);
                            break;
                        case Constants.TAG_ISDELETE:
                            holder.typeicon.setVisibility(View.VISIBLE);
                            holder.typeicon.setImageResource(R.drawable.block_primary);
                            holder.tickimage.setVisibility(View.GONE);
                            break;
                        default:
                            holder.typeicon.setVisibility(View.GONE);
                            break;
                    }
                } else {
                    holder.typeicon.setVisibility(View.GONE);
                }

                if (map.get(Constants.TAG_FAVOURITED).equals("true")) {
                    holder.favorite.setVisibility(View.VISIBLE);
                } else {
                    holder.favorite.setVisibility(View.GONE);
                }

                if (map.get(Constants.TAG_MUTE_NOTIFICATION).equals("true")) {
                    holder.mute.setVisibility(View.VISIBLE);
                } else {
                    holder.mute.setVisibility(View.GONE);
                }

                if (map.get(Constants.TAG_UNREAD_COUNT).equals("") || map.get(Constants.TAG_UNREAD_COUNT).equals("0")) {
                    holder.unseenLay.setVisibility(View.GONE);
                } else {
                    holder.unseenLay.setVisibility(View.VISIBLE);
                    holder.unseenCount.setText(map.get(Constants.TAG_UNREAD_COUNT));
                }
            }
        }

        @Override
        public int getItemCount() {
            return Items.size();
        }

        /*@Override
        public int getItemViewType(int position) {
            return Items.get(position) == null ? TYPE_HEADER : TYPE_ITEM;
        }*/

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

            LinearLayout parentlay, messageLay;
            RelativeLayout unseenLay;
            TextView name, message, time, unseenCount, typing;
            ImageView tickimage, typeicon, mute, favorite, deleteMsg;
            CircleImageView profileimage;
            View profileview;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                message = view.findViewById(R.id.message);
                time = view.findViewById(R.id.time);
                name = view.findViewById(R.id.name);
                profileimage = view.findViewById(R.id.profileimage);
                tickimage = view.findViewById(R.id.tickimage);
                typeicon = view.findViewById(R.id.typeicon);
                unseenLay = view.findViewById(R.id.unseenLay);
                unseenCount = view.findViewById(R.id.unseenCount);
                profileview = view.findViewById(R.id.profileview);
                typing = view.findViewById(R.id.typing);
                messageLay = view.findViewById(R.id.messageLay);
                mute = view.findViewById(R.id.mute);
                favorite = view.findViewById(R.id.favorite);

                parentlay.setOnClickListener(this);
                profileimage.setOnClickListener(this);
                parentlay.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
                        ApplicationClass.preventMultiClick(parentlay);
                        if (Items.size() > 0 && getAdapterPosition() != -1) {

                            Intent i = new Intent(context, ChatActivity.class);
                            i.putExtra("user_id", Items.get(getAdapterPosition()).get(Constants.TAG_USER_ID));
                            startActivity(i);
                        }
                        break;
                    case R.id.profileimage:
                        break;

                }
            }

            @Override
            public boolean onLongClick(View view) {
                switch (view.getId()) {
                    case R.id.parentlay:
                        break;
                }
                return false;
            }
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        setColor();
        findViewById(R.id.imv).setVisibility(View.VISIBLE);
        findViewById(R.id.pb).setVisibility(View.GONE);
        findViewById(R.id.no_item).setVisibility(View.GONE);
        if (!GetSet.isIsRandomOn()) {
            ((TextView)findViewById(R.id.msg)).setText(R.string.enable_random_chat);
            findViewById(R.id.no_item).setVisibility(View.VISIBLE);
        }
        else
            getChat();
        Log.v(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    private void networkSnack() {
        ((TextView)findViewById(R.id.msg)).setText(R.string.network_failure);
        findViewById(R.id.no_item).setVisibility(View.VISIBLE);
        findViewById(R.id.imv).setVisibility(View.VISIBLE);
        findViewById(R.id.pb).setVisibility(View.GONE);
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.parentLay), getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    void getChat() {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
            return;
        }
        ((TextView)findViewById(R.id.msg)).setText(R.string.loading_ran_chat);
        findViewById(R.id.no_item).setVisibility(View.VISIBLE);
        findViewById(R.id.imv).setVisibility(View.GONE);
        findViewById(R.id.pb).setVisibility(View.VISIBLE);
        MediaType MEDIA_TYPE_JPG = MediaType.parse("*/*");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userId", GetSet.getUserId())
                .build();


        System.out.println("ding dong tkkkkknp amzonxxnmju");
        String url = Constants.NODE_URL + "getRandomChat";
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        System.out.println("ding dong tkkkkknp amzonxx");
        ApplicationClass.getInstance().httpClient.newCall(request)
                .enqueue(new Callback() {
                             @Override
                             public void onFailure(Call call, IOException e) {
                                 runOnUiThread(new Runnable() {
                                     @Override
                                     public void run() {
                                         networkSnack();
                                     }
                                 });
                             }

                             @Override
                             public void onResponse(Call call, Response response) {
                                 String res = null;
                                 try {
                                     res = response.body().string();
                                     System.out.println("ding dong tkkkkknp res" + res);


                                     String finalRes = res;
                                     runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                             try {
                                                 JSONObject obj = new JSONObject(finalRes);
                                                 if (obj.getBoolean("success")) {
                                                     if (obj.isNull("data") ) {
                                                         findViewById(R.id.imv).setVisibility(View.VISIBLE);
                                                         findViewById(R.id.pb).setVisibility(View.GONE);
                                                         ((TextView)findViewById(R.id.msg)).setText(R.string.no_ran_chat);
                                                         findViewById(R.id.no_item).setVisibility(View.VISIBLE);
                                                     } else {
                                                         findViewById(R.id.no_item).setVisibility(View.GONE);
                                                         JSONObject chat = obj.getJSONObject("data");

                                                         //RandomProfile(String name, String userId, String profile_url, String phone, String gender, String bday) {
                                                         randomProfile = new RandomProfile(chat.getString("name"), chat.getString("userid"), chat.getString("profile_url"), chat.getString("phone"), chat.getString("gender"), chat.getString("bday"), chat.getString("country"), chat.getString("city"));
                                                         setValues(randomProfile.name, randomProfile.phone, randomProfile.gender, randomProfile.bday, randomProfile.profile_url,randomProfile.country,randomProfile.city);
                                                     }

                                                 } else {
                                                     //                                   reportProgress(-1);
                                                     findViewById(R.id.imv).setVisibility(View.VISIBLE);
                                                     findViewById(R.id.pb).setVisibility(View.GONE);
                                                     ((TextView)findViewById(R.id.msg)).setText(R.string.no_ran_chat);
                                                     findViewById(R.id.no_item).setVisibility(View.VISIBLE);
                                                 }


                                             } catch (JSONException e) {
                                                 e.printStackTrace();
                                                 runOnUiThread(new Runnable() {
                                                     @Override
                                                     public void run() {
                                                         System.out.println("errrr " + e.getLocalizedMessage());
                                                         networkSnack();
                                                     }
                                                 });
                                                 //                             reportError();
                                             }
                                         }
                                     });


                                 } catch (IOException e) {
                                     //reportError();
                                     runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                             networkSnack();
                                         }
                                     });
                                     e.printStackTrace();
                                 }

                             }
                         }
                );

    }

    void setValues(String name, String phone, String gender, String bday, String profile_url,String country, String city) {
        this.name.setText(name);
        this.phone.setText(phone);
        gen.setText(gender);
        this.city.setText(city+", "+country);
        this.bday.setText(bday);
        Glide.with(this).applyDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.temp)).load(profile_url).
                into(avatar);
    }

    public void onCancelClicked(View view) {
        getChat();
    }

    public void onChatClicked(View view) {
        dbhelper.addContactDetails(randomProfile.name, randomProfile.userId, randomProfile.name, randomProfile.phone, "", randomProfile.profile_url, "",
                "", randomProfile.profile_url, "", "");
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra("user_id", randomProfile.userId);
        startActivity(intent);
    }


class RandomProfile {
    String name;
    String userId;
    String country;
    String city;

    public RandomProfile(String name, String userId, String profile_url, String phone, String gender, String bday,String country, String city) {
        this.name = name;
        this.userId = userId;
        this.profile_url = profile_url;
        this.phone = phone;
        this.gender = gender;
        this.bday = bday;
        this.country = country;
        this.city = city;
    }

    String profile_url;
    String phone;
    String gender;
    String bday;

}}