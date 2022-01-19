package com.loopytime.im;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.Utils;
import com.loopytime.im.status.CameraKitActivity;
import com.loopytime.im.status.StoryActivity;
import com.loopytime.model.ContactsData;
import com.loopytime.model.MessagesData;
import com.loopytime.model.StatusDatas;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.Context.MODE_PRIVATE;
import static androidx.recyclerview.widget.RecyclerView.VERTICAL;
import static androidx.recyclerview.widget.RecyclerView.ViewHolder;
import static com.loopytime.utils.Constants.TAG_CONTACT_STATUS;
import static com.loopytime.utils.Constants.TAG_MY_CONTACTS;
import static com.loopytime.utils.Constants.TAG_NOBODY;
import static com.loopytime.utils.Constants.TAG_PRIVACY_PROFILE;
import static com.loopytime.utils.Constants.TRUE;

public class ChatFragment extends Fragment
        implements SocketConnection.RecentChatReceivedListener {

    private static final int CAPTURE_MEDIA = 369;
    private final String TAG = ChatFragment.this.getClass().getSimpleName();
    RecyclerViewAdapter recyclerViewAdapter;
    LinearLayout nullLay;
    TextView nullText, recenttitle;
    ArrayList<HashMap<String, String>> chatAry = new ArrayList<>();
    List<ContactsData.Result> statusList = new ArrayList<>();
    LinearLayoutManager linearLayoutManager;
    DatabaseHandler dbhelper;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    List<StatusDatas> datas = new ArrayList<>();
    HorizontalScrollView horizontalScrollLay;
    private StatusRecyclerAdapter statusRecyclerAdapter;
    private RecyclerView recyclerView, statusRecyclerView;
    private RelativeLayout progressLay;

    public ChatFragment() {

    }

    public static String getURLForResource(int resourceId) {
        return Uri.parse("android.resource://com.hitasoft.loopytime.hiddy/" + resourceId).toString();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        Log.v(TAG, "onCreateView");
        pref = getActivity().getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        progressLay = view.findViewById(R.id.progress);
        nullLay = view.findViewById(R.id.nullLay);
        nullText = view.findViewById(R.id.nullText);
        recyclerView = view.findViewById(R.id.recyclerView);
        statusRecyclerView = view.findViewById(R.id.statusRecyclerView);
        recenttitle = view.findViewById(R.id.recenttitle);
        horizontalScrollLay = view.findViewById(R.id.horizontalScrollLay);

        dbhelper = DatabaseHandler.getInstance(getActivity());
        SocketConnection.getInstance(getActivity()).setRecentChatReceivedListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
        statusRecyclerView.setLayoutManager(layoutManager);

        updateStatusAry();
        statusRecyclerAdapter = new StatusRecyclerAdapter(getContext(), statusList);
        statusRecyclerView.setAdapter(statusRecyclerAdapter);
        statusRecyclerView.setNestedScrollingEnabled(false);

        linearLayoutManager = new LinearLayoutManager(getActivity(), VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
//            recyclerView.setNestedScrollingEnabled(false);


        updateChatAry();
        recyclerViewAdapter = new RecyclerViewAdapter(getActivity(), chatAry);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerViewAdapter.notifyDataSetChanged();

        nullText.setText(R.string.no_chat_yet_buddy);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    private void moveToStart() {
        if (statusList.size() > 0) {
            horizontalScrollLay.post(new Runnable() {
                @Override
                public void run() {
                    horizontalScrollLay.fullScroll(View.FOCUS_BACKWARD);
                }
            });
        }
    }

    public void refreshAdapter() {
        if (recyclerViewAdapter != null) {
            updateChatAry();
            recyclerViewAdapter.notifyDataSetChanged();
            if (chatAry.size() > 0) {
                nullLay.setVisibility(View.GONE);
            }
        }
    }

    void openCamera() {
         Intent  i =new Intent(getContext(), CameraKitActivity.class);
        i.putExtra(Constants.IS_POST,false);
        startActivity(i);
    }

    private void updateChatAry() {
        chatAry.clear();
        chatAry.addAll(dbhelper.getAllRecentsMessages(getActivity()));
        if (chatAry.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
            recenttitle.setVisibility(View.GONE);
        } else {
            nullLay.setVisibility(View.GONE);
            recenttitle.setVisibility(View.VISIBLE);
        }
    }

    private void deleteChatConfirmDialog(Context context, String userId, int position, boolean clearChat) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.default_popup);
        dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 90 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView title = dialog.findViewById(R.id.title);
        TextView yes = dialog.findViewById(R.id.yes);
        TextView no = dialog.findViewById(R.id.no);
        yes.setText(getString(R.string.im_sure));
        no.setText(getString(R.string.nope));
        if (clearChat) {
            title.setText(R.string.really_delete_chat_history);
        } else {
            title.setText(R.string.really_delete_chat);
        }
        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (clearChat) {
                    dbhelper.deleteAllChats(GetSet.getUserId() + userId);
                    dbhelper.updateRecentChat(GetSet.getUserId() + userId, Constants.TAG_UNREAD_COUNT, "0");
                } else {
                    dbhelper.deleteAllChats(GetSet.getUserId() + userId);
                    dbhelper.deleteRecentChat(GetSet.getUserId() + userId);
                }
                /*chatAry.get(position).put(Constants.TAG_MESSAGE, "");
                chatAry.get(position).put(Constants.TAG_DELIVERY_STATUS, "");
                chatAry.get(position).put(Constants.TAG_DELIVERY_STATUS, "");*/
                updateChatAry();
                recyclerViewAdapter.notifyDataSetChanged();
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void openUserDialog(View view, HashMap<String, String> hashMap, Context context) {
        Intent i = new Intent(context, DialogActivity.class);
        i.putExtra(Constants.TAG_USER_ID, hashMap.get(Constants.TAG_USER_ID));
        i.putExtra(Constants.TAG_USER_NAME, hashMap.get(Constants.TAG_USER_NAME));
        if (hashMap.get(TAG_PRIVACY_PROFILE).equalsIgnoreCase(TAG_MY_CONTACTS)) {
            if (hashMap.get(TAG_CONTACT_STATUS) != null && hashMap.get(TAG_CONTACT_STATUS) != null && hashMap.get(TAG_CONTACT_STATUS).equalsIgnoreCase(TRUE)) {
                i.putExtra(Constants.TAG_USER_IMAGE, hashMap.get(Constants.TAG_USER_IMAGE));
            } else {
                i.putExtra(Constants.TAG_USER_IMAGE, "");
            }
        } else if (hashMap.get(TAG_PRIVACY_PROFILE).equalsIgnoreCase(TAG_NOBODY)) {
            i.putExtra(Constants.TAG_USER_IMAGE, "");
        } else {
            i.putExtra(Constants.TAG_USER_IMAGE, hashMap.get(Constants.TAG_USER_IMAGE));
        }
        i.putExtra(Constants.TAG_BLOCKED_ME, hashMap.get(Constants.TAG_BLOCKED_ME));

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(((MainActivity) context), view, getURLForResource(R.drawable.temp));
        startActivity(i, options.toBundle());
    }

    public String getFormattedDate(Context context, long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis * 1000L);

        Calendar now = Calendar.getInstance();

        final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "EEE, MMM d";
        final long HOURS = 60 * 60 * 60;
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            return String.valueOf(DateFormat.format(timeFormatString, smsTime));
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            return getString(R.string.yesterday);
        } else if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        } else {
            return DateFormat.format("MMM dd yyyy", smsTime).toString();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:
                boolean isContactEnabled = false;

                for (String permission : permissions) {
                    if (permission.equals(READ_CONTACTS)) {
                        if (ActivityCompat.checkSelfPermission(getActivity(), READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            isContactEnabled = true;
                        }
                    }
                }

                if (!isContactEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
                            requestPermission(new String[]{READ_CONTACTS}, 100);
                        } else {
                            Toast.makeText(getContext(), R.string.contact_permission_error, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + getActivity().getApplication().getPackageName()));
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    }
                }

                break;
            case 1010:
                int permissionStorage = ContextCompat.checkSelfPermission(getContext(),
                        WRITE_EXTERNAL_STORAGE);

                int permissionCamera = ContextCompat.checkSelfPermission(getContext(),
                        CAMERA);
                int permissionAudio = ContextCompat.checkSelfPermission(getContext(),
                        RECORD_AUDIO);

                if (permissionAudio == PackageManager.PERMISSION_GRANTED &&
                        permissionStorage == PackageManager.PERMISSION_GRANTED &&
                        permissionCamera == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    if (permissionAudio != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(RECORD_AUDIO)) {
                                requestPermission(new String[]{RECORD_AUDIO}, 100);
                            } else {
                                Toast.makeText(getContext(), R.string.contact_permission_error, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + getActivity().getApplication().getPackageName()));
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        }
                    }
                    if (permissionStorage != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                                requestPermission(new String[]{WRITE_EXTERNAL_STORAGE}, 100);
                            } else {
                                Toast.makeText(getContext(), R.string.contact_permission_error, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + getActivity().getApplication().getPackageName()));
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        }
                    }
                    if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(CAMERA)) {
                                requestPermission(new String[]{CAMERA}, 100);
                            } else {
                                Toast.makeText(getContext(), R.string.contact_permission_error, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + getActivity().getApplication().getPackageName()));
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        }
                    }
                }
                break;
        }
    }

    private void requestPermission(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(getActivity(), permissions, requestCode);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        SocketConnection.getInstance(getActivity()).setRecentChatReceivedListener(this);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (recyclerViewAdapter != null) {
                    updateChatAry();
                    recyclerViewAdapter.notifyDataSetChanged();
                }
                if (statusRecyclerAdapter != null) {
                    updateStatusAry();
                    statusRecyclerAdapter.notifyDataSetChanged();
                    statusRecyclerView.smoothScrollToPosition(0);
                }
            }
        });
        /*if(checkPermissions()){
        } else {
            if (ContextCompat.checkSelfPermission(getContext(), READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getContext(), WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{READ_CONTACTS, WRITE_EXTERNAL_STORAGE}, 100);
            }
        }*/
    }

    private boolean checkPermissions() {
        int permissionContacts = ContextCompat.checkSelfPermission(getActivity(),
                READ_CONTACTS);

        return permissionContacts == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
        SocketConnection.getInstance(getActivity()).setRecentChatReceivedListener(null);
    }

    @Override
    public void onRecentChatReceived() {
        Log.v(TAG, "onRecentChatReceived");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recyclerViewAdapter != null) {
                    updateChatAry();
                    recyclerViewAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onUserImageChange(final String user_id, final String user_image) {
        Log.v(TAG, "onUserImageChange");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recyclerViewAdapter != null && chatAry.size() > 0) {
                    for (int i = 0; i < chatAry.size(); i++) {
                        if (chatAry.get(i) != null && user_id.equals(chatAry.get(i).get(Constants.TAG_USER_ID))) {
                            chatAry.get(i).put(Constants.TAG_USER_IMAGE, user_image);
                            recyclerViewAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onBlockStatus(final JSONObject data) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recyclerViewAdapter != null && chatAry.size() > 0) {
                    try {
                        String sender_id = data.getString(Constants.TAG_SENDER_ID);
                        String type = data.getString(Constants.TAG_TYPE);
                        for (int i = 0; i < chatAry.size(); i++) {
                            if (chatAry.get(i) != null && sender_id.equals(chatAry.get(i).get(Constants.TAG_USER_ID))) {
                                chatAry.get(i).put(Constants.TAG_BLOCKED_ME, type);
                                recyclerViewAdapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onUpdateChatStatus(final String user_id) {
        Log.v("Chat", "onUpdateChatStatus");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recyclerViewAdapter != null && chatAry.size() > 0) {
                    for (int i = 0; i < chatAry.size(); i++) {
                        if (chatAry.get(i) != null && user_id.equals(chatAry.get(i).get(Constants.TAG_USER_ID)) && linearLayoutManager.findViewByPosition(i) != null) {
                            String msgId = chatAry.get(i).get(Constants.TAG_MESSAGE_ID);
                            Log.v("msgId", "msgId=" + msgId);
                            if (msgId != null) {
                                MessagesData mdata = dbhelper.getSingleMessage(msgId);
                                if (mdata != null) {
                                    chatAry.get(i).put(Constants.TAG_DELIVERY_STATUS, mdata.delivery_status);
                                    View itemView = linearLayoutManager.findViewByPosition(i);
                                    ImageView tickimage = itemView.findViewById(R.id.tickimage);
                                    if (mdata.sender_id != null && mdata.sender_id.equals(GetSet.getUserId())) {
                                        tickimage.setVisibility(View.VISIBLE);
                                        if (mdata.delivery_status.equals("read")) {
                                            tickimage.setImageResource(R.drawable.double_tick);
                                        } else if (mdata.delivery_status.equals("sent")) {
                                            tickimage.setImageResource(R.drawable.double_tick_unseen);
                                        } else if (mdata.progress.equals("completed") && (mdata.message_type.equals("image") ||
                                                mdata.message_type.equals("video") || mdata.message_type.equals("file") || mdata.message_type.equals("audio"))) {
                                            tickimage.setImageResource(R.drawable.single_tick);
                                        } else if (mdata.message_type.equals("text") || mdata.message_type.equals("contact") || mdata.message_type.equals("location")) {
                                            tickimage.setImageResource(R.drawable.single_tick);
                                        } else {
                                            tickimage.setVisibility(View.GONE);
                                        }
                                    } else {
                                        tickimage.setVisibility(View.GONE);
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onListenTyping(final JSONObject data) {
        Log.v("Chat", "onListenGroupTyping");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recyclerViewAdapter != null && linearLayoutManager != null && chatAry.size() > 0) {
                    try {
                        for (int i = 0; i < chatAry.size(); i++) {
                            if (chatAry.get(i) != null && data.get(Constants.TAG_SENDER_ID).equals(chatAry.get(i).get(Constants.TAG_USER_ID))
                                    && linearLayoutManager.findViewByPosition(i) != null) {
                                View itemView = linearLayoutManager.findViewByPosition(i);
                                LinearLayout messageLay;
                                messageLay = itemView.findViewById(R.id.messageLay);
                                TextView typing = itemView.findViewById(R.id.typing);
                                if (data.get(Constants.TAG_SENDER_ID).equals(chatAry.get(i).get(Constants.TAG_USER_ID)) && data.get("type").equals("typing")) {
                                    typing.setText(getString(R.string.typing));
                                    typing.setVisibility(View.VISIBLE);
                                    messageLay.setVisibility(View.INVISIBLE);
                                } else if (data.get(Constants.TAG_SENDER_ID).equals(chatAry.get(i).get(Constants.TAG_USER_ID)) && data.get("type").equals("recording")) {
                                    typing.setText(getString(R.string.recording));
                                    typing.setVisibility(View.VISIBLE);
                                    messageLay.setVisibility(View.INVISIBLE);
                                } else {
                                    typing.setVisibility(View.GONE);
                                    messageLay.setVisibility(View.VISIBLE);
                                }
                                break;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onPrivacyChanged(final JSONObject jsonObject) {
//        Log.i(TAG, "onPrivacyChanged: " + jsonObject);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recyclerViewAdapter != null) {
                    updateChatAry();
                    recyclerViewAdapter.notifyDataSetChanged();
                }
                if (statusRecyclerAdapter != null) {
                    updateStatusAry();
                    statusRecyclerAdapter.notifyDataSetChanged();
                    statusRecyclerView.smoothScrollToPosition(0);
                }
            }
        });
    }

    @Override
    public void onStatusReceived(JSONObject object) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (statusRecyclerAdapter != null) {
                    updateStatusAry();
                    statusRecyclerAdapter.notifyDataSetChanged();
                    statusRecyclerView.smoothScrollToPosition(0);
                }
            }
        });
    }

    private void updateStatusAry() {
        statusList.clear();
        statusList.addAll(dbhelper.getStatusContactData(getContext()));
    }

    @Override
    public void onDeleteStatus(String statusId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (statusRecyclerAdapter != null) {
                    updateStatusAry();
                    statusRecyclerAdapter.notifyDataSetChanged();
                    statusRecyclerView.smoothScrollToPosition(0);
                }
            }
        });
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;
        ArrayList<HashMap<String, String>> Items;
        Context context;

        public RecyclerViewAdapter(Context context, ArrayList<HashMap<String, String>> Items) {
            this.Items = Items;
            this.context = context;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item, parent, false);
            return new MyViewHolder(itemView);
            /*if (viewType == TYPE_ITEM) {
            } else if (viewType == TYPE_HEADER) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.favorites_header, parent, false);
                return new HeaderViewHolder(itemView);
            }
            return null;*/
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, int position) {

            if (viewHolder instanceof MyViewHolder) {
                MyViewHolder holder = (MyViewHolder) viewHolder;
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
                    DialogActivity.setProfileImage(dbhelper.getContactDetail(map.get(Constants.TAG_USER_ID)), holder.profileimage, context);
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
            } else if (viewHolder instanceof HeaderView) {
                HeaderView holder = (HeaderView) viewHolder;
                Log.v("header", "header");
                if (Items.size() == 1) {
                    holder.recenttitle.setVisibility(View.GONE);
                }
                holder.horizontalScrollLay.post(new Runnable() {
                    @Override
                    public void run() {
                        holder.horizontalScrollLay.fullScroll(View.FOCUS_BACKWARD);
                    }
                });
                Glide.with(context).load(Constants.USER_IMG_PATH + GetSet.getImageUrl()).thumbnail(0.5f)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                        .into(holder.userImage);
                List<ContactsData.Result> statusList = dbhelper.getStatusContactData(getContext());
                LinearLayoutManager layoutManager = new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false);
                holder.favrecyclerView.setLayoutManager(layoutManager);
                holder.favrecyclerView.setAdapter(new StatusRecyclerAdapter(context, statusList));
                holder.favrecyclerView.setNestedScrollingEnabled(false);
                holder.favrecyclerView.setHasFixedSize(true);
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

        public class MyViewHolder extends ViewHolder implements View.OnClickListener, View.OnLongClickListener {

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
                        openUserDialog(profileview, Items.get(getAdapterPosition()), context);
                        break;

                }
            }

            @Override
            public boolean onLongClick(View view) {
                switch (view.getId()) {
                    case R.id.parentlay:
                        View bottomView = getLayoutInflater().inflate(R.layout.chat_longpress_dialog, null);
                        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialog.setContentView(bottomView);

                        String userId = Items.get(getAdapterPosition()).get(Constants.TAG_USER_ID);
                        ContactsData.Result results = dbhelper.getContactDetail(userId);
                        TextView txtFavourite = bottomView.findViewById(R.id.txtFavourite);
                        TextView txtView = bottomView.findViewById(R.id.txtView);
                        TextView txtClear = bottomView.findViewById(R.id.txtClear);
                        TextView txtDelete = bottomView.findViewById(R.id.txtDelete);

                        if (results.favourited.equals("true")) {
                            txtFavourite.setText(getString(R.string.remove_favourite));
                        } else {
                            txtFavourite.setText(getString(R.string.mark_favourite));
                        }

                        txtFavourite.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                                if (results.favourited.equals("true")) {
                                    dbhelper.updateFavUser(userId, "false");
                                    Toast.makeText(context, getString(R.string.removed_favourites), Toast.LENGTH_SHORT).show();
                                } else {
                                    dbhelper.updateFavUser(userId, "true");
                                    Toast.makeText(context, getString(R.string.marked_favourite), Toast.LENGTH_SHORT).show();
                                }
                                /*List<ContactsData.Result> favList = dbhelper.getStatusContactData(getActivity());
                                if (chatAry.size() > 0 && chatAry.get(0) != null) {
                                } else if (favList.size() == 0 && chatAry.size() > 0 && chatAry.get(0) == null) {
                                    chatAry.remove(0);
                                }*/
                                updateChatAry();
                                recyclerViewAdapter.notifyDataSetChanged();
                            }
                        });

                        txtView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                                Intent profile = new Intent(context, ProfileActivity.class);
                                profile.putExtra(Constants.TAG_USER_ID, userId);
                                startActivity(profile);
                            }
                        });

                        txtClear.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                                deleteChatConfirmDialog(context, userId, getAdapterPosition(), true);
                            }
                        });

                        txtDelete.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                                deleteChatConfirmDialog(context, userId, getAdapterPosition(), false);
                            }
                        });

                        dialog.show();
                        break;
                }
                return false;
            }
        }

        public class HeaderView extends ViewHolder implements View.OnClickListener {

            RecyclerView favrecyclerView;
            RelativeLayout parentLay;
            LinearLayout addStoryLay;
            CircleImageView userImage;
            TextView recenttitle;
            HorizontalScrollView horizontalScrollLay;

            public HeaderView(View view) {
                super(view);
                favrecyclerView = view.findViewById(R.id.favrecyclerView);
                parentLay = view.findViewById(R.id.parentLay);
                addStoryLay = view.findViewById(R.id.addStoryLay);
                userImage = view.findViewById(R.id.userImage);
                recenttitle = view.findViewById(R.id.recenttitle);
                horizontalScrollLay = view.findViewById(R.id.horizontalScrollLay);

                addStoryLay.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.addStoryLay) {
                    if (ContextCompat.checkSelfPermission(context, CAMERA) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{CAMERA, RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, 1010);
                    } else {
                        openCamera();
                    }
                }
            }
        }
    }

    public class StatusRecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;
        List<ContactsData.Result> favList;
        Context context;

        public StatusRecyclerAdapter(Context context, List<ContactsData.Result> favList) {
            this.context = context;
            this.favList = favList;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            if (viewType == TYPE_ITEM) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fav_item, parent, false);
                return new MyViewHolder(itemView);
            } else if (viewType == TYPE_HEADER) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.add_story_lay, parent, false);
                return new HeaderViewHolder(itemView);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
            if (viewHolder instanceof MyViewHolder) {
                MyViewHolder holder = (MyViewHolder) viewHolder;
                ContactsData.Result result = favList.get(position - 1);

                if (ApplicationClass.isStringNotNull(result.blockedme) && result.blockedme.equals("block")) {
                    Glide.with(context).load(R.drawable.temp)
                            .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                            .into(holder.profileimage);
                } else {
                    if (ApplicationClass.isStringNotNull(result.privacy_profile_image) && result.privacy_profile_image.equalsIgnoreCase(TAG_MY_CONTACTS)) {
                        if (result.contactstatus != null && result.contactstatus.equalsIgnoreCase(TRUE)) {
                            Glide.with(context).load(Constants.USER_IMG_PATH + result.user_image).thumbnail(0.5f)
                                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                                    .into(holder.profileimage);
                        } else {
                            Glide.with(context).load(R.drawable.temp).thumbnail(0.5f)
                                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                                    .into(holder.profileimage);
                        }

                    } else if (ApplicationClass.isStringNotNull(result.privacy_profile_image) && result.privacy_profile_image.equalsIgnoreCase(TAG_NOBODY)) {
                        Glide.with(context).load(R.drawable.temp).thumbnail(0.5f)
                                .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                                .into(holder.profileimage);
                    } else {
                        Glide.with(context).load(Constants.USER_IMG_PATH + result.user_image).thumbnail(0.5f)
                                .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                                .into(holder.profileimage);
                    }
                }

                holder.profileimage.setBorderWidth(10);
                if (result.is_View.equals("0")) {
                    holder.profileimage.setBorderColor(ContextCompat.getColor(context, R.color.colorAccent));
                } else {
                    holder.profileimage.setBorderColor(ContextCompat.getColor(context, R.color.chat_selected));
                }

                holder.txtName.setText(result.user_name);
            } else if (viewHolder instanceof HeaderViewHolder) {
                HeaderViewHolder holder = (HeaderViewHolder) viewHolder;
                Glide.with(getContext()).load(Constants.USER_IMG_PATH + GetSet.getImageUrl()).thumbnail(0.5f)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(getContext(), 70)))
                        .into(holder.userImage);
            }
        }

        @Override
        public int getItemCount() {
            return favList.size() + 1;
        }

        public class MyViewHolder extends ViewHolder implements View.OnClickListener {

            LinearLayout parentlay;
            CircleImageView profileimage;
            TextView txtName;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                profileimage = view.findViewById(R.id.userImage);
                txtName = view.findViewById(R.id.txtName);

                parentlay.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
                        ApplicationClass.preventMultiClick(parentlay);
                        if (favList.size() > 0 && getAdapterPosition() != -1) {
                            Intent i = new Intent(context, StoryActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            i.putExtra(Constants.TAG_DATA, (Serializable) favList);
                            i.putExtra(Constants.TAG_POSITION, getAdapterPosition() - 1);
                            startActivity(i);
                        }
                        break;
                }
            }
        }

        public class HeaderViewHolder extends ViewHolder implements View.OnClickListener {
            LinearLayout addStoryLay;
            CircleImageView userImage;
            TextView txtName;

            public HeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                addStoryLay = itemView.findViewById(R.id.addStoryLay);
                userImage = itemView.findViewById(R.id.userImage);

                addStoryLay.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.addStoryLay) {
                    ApplicationClass.preventMultiClick(addStoryLay);
                    if (ContextCompat.checkSelfPermission(getContext(), CAMERA) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(getContext(), RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(getContext(), WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{CAMERA, RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, 1010);
                    } else {
                        openCamera();
                    }
                }
            }
        }

    }
}
