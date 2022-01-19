package com.loopytime.im;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.Utils;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.loopytime.helper.Utils.getURLForResource;
import static com.loopytime.utils.Constants.TAG_GROUP_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER;
import static com.loopytime.utils.Constants.TAG_MEMBER_ID;
import static com.loopytime.utils.Constants.TAG_MEMBER_NO;


public class GroupFragment extends Fragment implements SocketConnection.GroupRecentReceivedListener {

    private final String TAG = this.getClass().getSimpleName();
    RecyclerViewAdapter recyclerViewAdapter;
    RecyclerView recyclerView;
    RelativeLayout progressLay;
    LinearLayout nullLay;
    TextView nullText;
    ArrayList<HashMap<String, String>> groupList = new ArrayList<>();
    LinearLayoutManager linearLayoutManager;
    DatabaseHandler dbhelper;

    public GroupFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);
        progressLay = view.findViewById(R.id.progress);
        nullLay = view.findViewById(R.id.nullLay);
        recyclerView = view.findViewById(R.id.recyclerView);
        nullText = view.findViewById(R.id.nullText);
        dbhelper = DatabaseHandler.getInstance(getActivity());
        SocketConnection.getInstance(getActivity()).setGroupRecentCallbackListener(this);

        nullText.setText(R.string.no_group_yet_buddy);
        linearLayoutManager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        groupList.clear();
        groupList.addAll(dbhelper.getGroupRecentMessages(getActivity()));
        recyclerViewAdapter = new RecyclerViewAdapter(getActivity(), groupList);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerViewAdapter.notifyDataSetChanged();

        if (groupList.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
        } else {
            nullLay.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onGroupCreated() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (recyclerViewAdapter != null) {
                        groupList.clear();
                        groupList.addAll(dbhelper.getGroupRecentMessages(getActivity()));
                        recyclerViewAdapter.notifyDataSetChanged();
                        if (groupList.size() > 0) {
                            nullLay.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onGroupRecentReceived() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (recyclerViewAdapter != null) {
                        groupList.clear();
                        groupList.addAll(dbhelper.getGroupRecentMessages(getActivity()));
                        recyclerViewAdapter.notifyDataSetChanged();

                        if (groupList.size() > 0) {
                            nullLay.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SocketConnection.getInstance(getActivity()).setGroupRecentCallbackListener(this);
        if (recyclerViewAdapter != null) {
            groupList.clear();
            groupList.addAll(dbhelper.getGroupRecentMessages(getActivity()));
            recyclerViewAdapter.notifyDataSetChanged();
        }
        if (groupList.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
        } else {
            nullLay.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SocketConnection.getInstance(getActivity()).setGroupRecentCallbackListener(null);
    }

    @Override
    public void onUserImageChange(String user_id, String user_image) {

    }

    @Override
    public void onMemberExited(JSONObject data) {

    }

    @Override
    public void onUpdateChatStatus(String user_id) {

    }

    @Override
    public void onListenGroupTyping(final JSONObject data) {
        Log.v("GroupChat", "onListenGroupTyping");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recyclerViewAdapter != null && groupList.size() > 0) {
                    try {
                        String memberId = data.getString(Constants.TAG_MEMBER_ID);
                        if (dbhelper.isMemberExist(GetSet.getUserId(), String.valueOf(data.get(Constants.TAG_GROUP_ID)))) {
                            if (!memberId.equalsIgnoreCase(GetSet.getUserId())) {
                                for (int i = 0; i < groupList.size(); i++) {
                                    if (data.get(Constants.TAG_GROUP_ID).equals(groupList.get(i).get(Constants.TAG_GROUP_ID))
                                            && linearLayoutManager.findViewByPosition(i) != null) {
                                        View itemView = linearLayoutManager.findViewByPosition(i);
                                        LinearLayout messageLay = itemView.findViewById(R.id.messageLay);
                                        TextView typing = itemView.findViewById(R.id.typing);
                                        if (data.get("type").equals("typing")) {
                                            typing.setText(ApplicationClass.getContactName(getActivity(), dbhelper.getContactPhone(memberId), dbhelper.getContactCountryCode(memberId),dbhelper.getContactPhone(memberId)) + " " + getString(R.string.typing));
                                            typing.setVisibility(View.VISIBLE);
                                            messageLay.setVisibility(View.INVISIBLE);
                                        }else if (data.get("type").equals("reording")) {
                                            typing.setText(ApplicationClass.getContactName(getActivity(), dbhelper.getContactPhone(memberId), dbhelper.getContactCountryCode(memberId),dbhelper.getContactPhone(memberId)) + " " + getString(R.string.recording));
                                            typing.setVisibility(View.VISIBLE);
                                            messageLay.setVisibility(View.INVISIBLE);
                                        } else {
                                            typing.setVisibility(View.GONE);
                                            messageLay.setVisibility(View.VISIBLE);
                                        }
                                        break;
                                    }
                                }
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
    public void onGroupDeleted(JSONObject jsonObject) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (recyclerViewAdapter != null) {
                        groupList.clear();
                        groupList.addAll(dbhelper.getGroupRecentMessages(getActivity()));
                        recyclerViewAdapter.notifyDataSetChanged();
                        if (groupList.size() == 0) {
                            nullLay.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onGroupModified(JSONObject data) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (recyclerViewAdapter != null) {
                        groupList.clear();
                        groupList.addAll(dbhelper.getGroupRecentMessages(getActivity()));
                        recyclerViewAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

        ArrayList<HashMap<String, String>> groupList;
        Context context;

        public RecyclerViewAdapter(Context context, ArrayList<HashMap<String, String>> Items) {
            this.groupList = Items;
            this.context = context;
        }

        @Override
        public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item, parent, false);

            return new RecyclerViewAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(RecyclerViewAdapter.MyViewHolder holder, final int position) {

            final HashMap<String, String> groupData = groupList.get(position);
            holder.name.setText(groupData.get(Constants.TAG_GROUP_NAME));
            holder.typing.setVisibility(View.GONE);
            holder.messageLay.setVisibility(View.VISIBLE);
            holder.message.setText(groupData.get(Constants.TAG_MESSAGE) != null ? groupData.get(Constants.TAG_MESSAGE) : "");

            if (groupData.get(Constants.TAG_CHAT_TIME) != null) {
                holder.time.setText(Utils.getFormattedDate(context, Long.parseLong(groupData.get(Constants.TAG_CHAT_TIME).replace(".0", ""))));
            }

            Glide.with(context).load(Constants.GROUP_IMG_PATH + groupData.get(Constants.TAG_GROUP_IMAGE)).thumbnail(0.5f)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.create_group).error(R.drawable.create_group).override(ApplicationClass.dpToPx(context, 70)))
                    .into(holder.profileimage);

            if (groupData.get(Constants.TAG_MESSAGE_TYPE) != null) {
                switch (groupData.get(Constants.TAG_MESSAGE_TYPE)) {
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
                        break;
                    case "change_number":
                        if (!groupData.get(Constants.TAG_MEMBER_ID).equals(GetSet.getUserId())) {
                            holder.message.setText(groupData.get(Constants.TAG_MESSAGE) != null ? groupData.get(Constants.TAG_MESSAGE) : "");
                        } else {
                            holder.message.setText("");
                        }
                        break;
                    default:
                        holder.typeicon.setVisibility(View.GONE);
                        break;
                }
            } else {
                holder.typeicon.setVisibility(View.GONE);
            }

            if (groupData.get(Constants.TAG_MUTE_NOTIFICATION).equals("true")) {
                holder.mute.setVisibility(View.VISIBLE);
            } else {
                holder.mute.setVisibility(View.GONE);
            }

            if (groupData.get(Constants.TAG_UNREAD_COUNT).equals("") || groupData.get(Constants.TAG_UNREAD_COUNT).equals("0")) {
                holder.unseenLay.setVisibility(View.GONE);
            } else {
                holder.unseenLay.setVisibility(View.VISIBLE);
                holder.unseenCount.setText(groupData.get(Constants.TAG_UNREAD_COUNT));
            }
        }

        @Override
        public int getItemCount() {
            return groupList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay, messageLay;
            RelativeLayout unseenLay;
            TextView name, message, time, unseenCount, typing;
            ImageView tickimage, typeicon, mute, deleteMsg;
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

                parentlay.setOnClickListener(this);
                profileimage.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
                        ApplicationClass.preventMultiClick(parentlay);
                        Intent i = new Intent(context, GroupChatActivity.class);
                        i.putExtra(TAG_GROUP_ID, groupList.get(getAdapterPosition()).get(Constants.TAG_GROUP_ID));
                        startActivity(i);
                        break;
                    case R.id.profileimage:
                        openUserDialog(profileview, groupList.get(getAdapterPosition()), context);
                        break;

                }
            }
        }
    }

    private void openUserDialog(View view, HashMap<String, String> data, Context context) {
        Intent i = new Intent(context, DialogActivity.class);
        i.putExtra(Constants.TAG_GROUP_ID, data.get(Constants.TAG_GROUP_ID));
        i.putExtra(Constants.TAG_GROUP_NAME, data.get(Constants.TAG_GROUP_NAME));
        i.putExtra(Constants.TAG_GROUP_IMAGE, data.get(Constants.TAG_GROUP_IMAGE));
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(((MainActivity) context), view, getURLForResource(R.drawable.temp));
        startActivity(i, options.toBundle());
    }

    public static HashMap<String, String> getMessages(DatabaseHandler dbhelper, Context mContext, HashMap<String, String> groupData){
        if (groupData.get(Constants.TAG_MESSAGE_TYPE) != null){
            switch (groupData.get(Constants.TAG_MESSAGE_TYPE)) {
                case "text":
                case "image":
                case "video":
                case "file":
                case "location":
                case "contact":
                case "audio":
                    groupData.put(Constants.TAG_MESSAGE, groupData.get(Constants.TAG_MESSAGE) != null ? groupData.get(Constants.TAG_MESSAGE) : "");
                    break;
                case "create_group":
                    if (groupData.get(Constants.TAG_GROUP_ADMIN_ID).equals(GetSet.getUserId())) {
                        groupData.put(Constants.TAG_MESSAGE, mContext.getString(R.string.you_created_the_group));
                    } else {
                        if (dbhelper.isUserExist(groupData.get(Constants.TAG_GROUP_ADMIN_ID))) {
                            groupData.put(Constants.TAG_MESSAGE, ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupData.get(Constants.TAG_GROUP_ADMIN_ID)), dbhelper.getContactCountryCode(groupData.get(Constants.TAG_GROUP_ADMIN_ID)),dbhelper.getContactPhone(groupData.get(Constants.TAG_GROUP_ADMIN_ID))) + " " + mContext.getString(R.string.created_the_group));
                        } else {
                            groupData.put(Constants.TAG_MESSAGE, mContext.getString(R.string.group_created));
                        }
                    }
                    break;
                case "add_member":
                    if (groupData.get(Constants.TAG_ATTACHMENT).equals("")) {
                        if (dbhelper.isUserExist(groupData.get(Constants.TAG_GROUP_ADMIN_ID))) {
                            groupData.put(Constants.TAG_MESSAGE, ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupData.get(Constants.TAG_GROUP_ADMIN_ID)), dbhelper.getContactCountryCode(groupData.get(Constants.TAG_GROUP_ADMIN_ID)), dbhelper.getContactPhone(groupData.get(Constants.TAG_GROUP_ADMIN_ID))) + " " + mContext.getString(R.string.added_you));
                        } else {
                            groupData.put(Constants.TAG_MESSAGE, mContext.getString(R.string.you_were_added));
                        }
                    } else {
                        try {
                            JSONArray jsonArray = new JSONArray(groupData.get(Constants.TAG_ATTACHMENT));
                            ArrayList<String> members = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                if (jsonObject.getString(TAG_MEMBER_ID).equals(GetSet.getUserId())) {
                                    members.add(mContext.getString(R.string.you));
                                } else if (dbhelper.isUserExist(jsonObject.getString(Constants.TAG_MEMBER_ID))) {
                                    members.add(ApplicationClass.getContactName(mContext, jsonObject.getString(TAG_MEMBER_NO),dbhelper.getContactCountryCode(jsonObject.getString(Constants.TAG_MEMBER_ID)),jsonObject.getString(TAG_MEMBER_NO)));
                                }
                            }
                            String memberstr = members.toString().replaceAll("[\\[\\]]|(?<=,)\\s+", "");
                            if (groupData.get(Constants.TAG_MEMBER_ID).equals(GetSet.getUserId())) {
                                groupData.put(Constants.TAG_MESSAGE, mContext.getString(R.string.you_added) + " " + memberstr);
                            } else {
                                groupData.put(Constants.TAG_MESSAGE, ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupData.get(Constants.TAG_MEMBER_ID)), dbhelper.getContactCountryCode(groupData.get(Constants.TAG_MEMBER_ID)),dbhelper.getContactPhone(groupData.get(Constants.TAG_MEMBER_ID))) +" "+ mContext.getString(R.string.added)+" " + memberstr);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case "group_image":
                case "subject":
                    if (groupData.get(Constants.TAG_MEMBER_ID).equalsIgnoreCase(GetSet.getUserId())) {
                        groupData.put(Constants.TAG_MESSAGE, mContext.getString(R.string.you) + " " + groupData.get(Constants.TAG_MESSAGE));
                    } else {
                        groupData.put(Constants.TAG_MESSAGE, ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupData.get(Constants.TAG_MEMBER_ID)), dbhelper.getContactCountryCode(groupData.get(Constants.TAG_MEMBER_ID)),dbhelper.getContactPhone(groupData.get(Constants.TAG_MEMBER_ID))) + " " + groupData.get(Constants.TAG_MESSAGE));
                    }
                    break;
                case "left":
                    if(groupData.get(TAG_MEMBER_ID)!=null){
                        if (groupData.get(Constants.TAG_MEMBER_ID).equalsIgnoreCase(GetSet.getUserId())) {
                            groupData.put(Constants.TAG_MESSAGE, mContext.getString(R.string.you_left));
                        } else {
                            groupData.put(Constants.TAG_MESSAGE, ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupData.get(Constants.TAG_MEMBER_ID)), dbhelper.getContactCountryCode(groupData.get(Constants.TAG_MEMBER_ID)),dbhelper.getContactPhone(groupData.get(Constants.TAG_MEMBER_ID))) + " " + mContext.getString(R.string.left));
                        }
                    }
                    break;
                case "remove_member":
                    if (groupData.get(Constants.TAG_GROUP_ADMIN_ID).equals(GetSet.getUserId())) {
                        groupData.put(Constants.TAG_MESSAGE, mContext.getString(R.string.you_removed) + " " + ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupData.get(Constants.TAG_MEMBER_ID)), dbhelper.getContactCountryCode(groupData.get(Constants.TAG_MEMBER_ID)),dbhelper.getContactPhone(groupData.get(Constants.TAG_MEMBER_ID))));
                    } else {
                        if (groupData.get(Constants.TAG_MEMBER_ID).equals(GetSet.getUserId())) {
                            groupData.put(Constants.TAG_MESSAGE, ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupData.get(Constants.TAG_GROUP_ADMIN_ID)), dbhelper.getContactCountryCode(groupData.get(Constants.TAG_GROUP_ADMIN_ID)),dbhelper.getContactPhone(groupData.get(Constants.TAG_GROUP_ADMIN_ID))) + " " + mContext.getString(R.string.removed_you));
                        } else {
                            groupData.put(Constants.TAG_MESSAGE, ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupData.get(Constants.TAG_GROUP_ADMIN_ID)), dbhelper.getContactCountryCode(groupData.get(Constants.TAG_GROUP_ADMIN_ID)),dbhelper.getContactPhone(groupData.get(Constants.TAG_GROUP_ADMIN_ID))) + " " + mContext.getString(R.string.removed) + " " +
                                    ApplicationClass.getContactName(mContext, dbhelper.getContactPhone(groupData.get(Constants.TAG_MEMBER_ID)), dbhelper.getContactCountryCode(groupData.get(Constants.TAG_MEMBER_ID)),dbhelper.getContactPhone(groupData.get(Constants.TAG_MEMBER_ID))));
                        }
                    }
                    break;
                case "admin":
                    if (groupData.get(Constants.TAG_ATTACHMENT).equals(TAG_MEMBER)) {
                        groupData.put(Constants.TAG_MESSAGE, mContext.getString(R.string.you_are_no_longer_as_admin));
                    } else {
                        groupData.put(Constants.TAG_MESSAGE, mContext.getString(R.string.you_are_now_an_admin));
                    }
                    break;
                case "date":
                    groupData.put(Constants.TAG_MESSAGE, Utils.getFormattedDate(mContext, Long.parseLong(groupData.get(Constants.TAG_CHAT_TIME))));
                    break;
            }
        } else {
            groupData.put(Constants.TAG_MESSAGE, "");
        }

        return groupData;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
