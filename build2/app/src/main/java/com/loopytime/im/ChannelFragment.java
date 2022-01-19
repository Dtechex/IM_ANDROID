package com.loopytime.im;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.Utils;
import com.loopytime.utils.Constants;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChannelFragment extends Fragment implements SocketConnection.ChannelRecentReceivedListener, View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    RecyclerViewAdapter channelAdapter;
    RecyclerView recyclerView;
    RelativeLayout progressLay;
    LinearLayout nullLay, headerLayout;
    TextView nullText, btnAllChannel;
    LinearLayoutManager linearLayoutManager;
    DatabaseHandler dbhelper;
    private ArrayList<HashMap<String, String>> channelList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);
        SocketConnection.getInstance(getActivity()).setChannelRecentReceivedListener(this);
        progressLay = view.findViewById(R.id.progress);
        nullLay = view.findViewById(R.id.nullLay);
        nullText = view.findViewById(R.id.nullText);
        headerLayout = view.findViewById(R.id.headerLayout);
        btnAllChannel = view.findViewById(R.id.btnAllChannel);
        recyclerView = view.findViewById(R.id.recyclerView);

        headerLayout.setVisibility(View.VISIBLE);

        if(ApplicationClass.isRTL()){

        } else {

        }

        dbhelper = DatabaseHandler.getInstance(getActivity());
        linearLayoutManager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        btnAllChannel.setOnClickListener(this);
        channelList = dbhelper.getChannelRecentMessages(getContext());
        channelAdapter = new RecyclerViewAdapter(getActivity(), channelList);
        recyclerView.setAdapter(channelAdapter);
        channelAdapter.notifyDataSetChanged();

        nullText.setText(R.string.no_channels_yet_buddy);
        if (channelList.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
        } else {
            nullLay.setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public void onResume() {
        SocketConnection.getInstance(getActivity()).setChannelRecentReceivedListener(this);
        if (channelAdapter != null) {
            channelList.clear();
            channelList.addAll(dbhelper.getChannelRecentMessages(getContext()));
            channelAdapter.notifyDataSetChanged();
        }
        if (channelList.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
        } else {
            nullLay.setVisibility(View.GONE);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        SocketConnection.getInstance(getActivity()).setChannelRecentReceivedListener(null);
    }

    @Override
    public void onAdminChatReceive() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (channelAdapter != null) {
                        channelList.clear();
                        channelList.addAll(dbhelper.getChannelRecentMessages(getContext()));
                        channelAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    public void onChannelInviteReceived(JSONObject jsonObject) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (channelAdapter != null) {
                    channelList.clear();
                    channelList.addAll(dbhelper.getChannelRecentMessages(getContext()));
                    channelAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onChannelBlocked(String channelId) {
        updateAdapter();
    }

    @Override
    public void onChannelDeleted() {
        updateAdapter();
    }

    @Override
    public void onChannelCreated(JSONObject jsonObject) {
        updateAdapter();
    }

    private void updateAdapter() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (channelAdapter != null) {
                        channelList.clear();
                        channelList.addAll(dbhelper.getChannelRecentMessages(getContext()));
                        channelAdapter.notifyDataSetChanged();

                        if (channelList.size() == 0) {
                            nullLay.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onChannelRecentReceived() {
        updateAdapter();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnAllChannel:
                ApplicationClass.preventMultiClick(btnAllChannel);
                Intent allChannel = new Intent(getActivity(), AllChannelsActivity.class);
                startActivity(allChannel);
                break;
        }
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

        ArrayList<HashMap<String, String>> channelList = new ArrayList<>();
        Context context;

        public RecyclerViewAdapter(Context context, ArrayList<HashMap<String, String>> channelList) {
            this.channelList = channelList;
            this.context = context;
        }

        @Override
        public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item, parent, false);

            return new RecyclerViewAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {

            final HashMap<String, String> hashMap = channelList.get(position);

            holder.name.setText(hashMap.get(Constants.TAG_CHANNEL_NAME));

            if (hashMap.get(Constants.TAG_CHANNEL_TYPE).equalsIgnoreCase(Constants.TAG_PRIVATE)) {
                holder.privateImage.setVisibility(View.VISIBLE);
            } else {
                holder.privateImage.setVisibility(View.GONE);
            }

            int unreadCount = dbhelper.getUnseenChannelMessagesCount(hashMap.get(Constants.TAG_CHANNEL_ID));
            if (unreadCount > 0) {
                holder.unseenLay.setVisibility(View.VISIBLE);
                holder.unseenCount.setText("" + unreadCount);
            } else {
                holder.unseenLay.setVisibility(View.GONE);
            }

            /*if(hashMap.get(Constants.TAG_MESSAGE_TYPE)!=null && hashMap.get(Constants.TAG_MESSAGE_TYPE).equals(Constants.TAG_ISDELETE)){
                holder.deleteMsg.setVisibility(View.VISIBLE);
            } else {
                holder.deleteMsg.setVisibility(View.GONE);
            }*/

            if (hashMap.get(Constants.TAG_CHANNEL_CATEGORY).equalsIgnoreCase(Constants.TAG_ADMIN_CHANNEL)) {
                Glide.with(context).load(Constants.CHANNEL_IMG_PATH + hashMap.get(Constants.TAG_CHANNEL_IMAGE)).thumbnail(0.5f)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.profile_square).error(R.drawable.profile_square).override(ApplicationClass.dpToPx(context, 70)))
                        .into(holder.profileimage);

                holder.message.setText((hashMap.get(Constants.TAG_MESSAGE) != null && !hashMap.get(Constants.TAG_MESSAGE).equals("")) ? Utils.fromHtml(hashMap.get(Constants.TAG_MESSAGE)) : Utils.fromHtml(hashMap.get(Constants.TAG_CHANNEL_DES)));
            } else {
                Glide.with(context).load(Constants.CHANNEL_IMG_PATH + hashMap.get(Constants.TAG_CHANNEL_IMAGE)).thumbnail(0.5f)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.ic_channel_square).error(R.drawable.ic_channel_square).override(ApplicationClass.dpToPx(context, 70)))
                        .into(holder.profileimage);

                if (hashMap.get(Constants.TAG_MESSAGE_TYPE) == null) {
                    holder.message.setText(hashMap.get(Constants.TAG_CHANNEL_DES));
                } else if (hashMap.get(Constants.TAG_MESSAGE_TYPE).equals("create_channel") || hashMap.get(Constants.TAG_MESSAGE_TYPE).equals("new_invite")) {
                    holder.message.setText(hashMap.get(Constants.TAG_MESSAGE) != null ? Utils.fromHtml(hashMap.get(Constants.TAG_CHANNEL_DES)) : "");
                } else {
                    holder.message.setText(hashMap.get(Constants.TAG_MESSAGE) != null ? Utils.fromHtml(hashMap.get(Constants.TAG_MESSAGE)) : Utils.fromHtml(hashMap.get(Constants.TAG_CHANNEL_DES)));
                }
            }

            if (hashMap.get(Constants.TAG_MESSAGE_TYPE) != null) {
                switch (hashMap.get(Constants.TAG_MESSAGE_TYPE)) {
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

            if (hashMap.get(Constants.TAG_MUTE_NOTIFICATION).equals("true")) {
                holder.mute.setVisibility(View.VISIBLE);
            } else {
                holder.mute.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return channelList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay, messageLay;
            RelativeLayout unseenLay;
            TextView name, message, time, unseenCount, typing;
            ImageView tickimage, typeicon, mute, privateImage, deleteMsg;
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
                privateImage = view.findViewById(R.id.privateImage);

                parentlay.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.parentlay:
                        ApplicationClass.preventMultiClick(parentlay);
                        HashMap<String, String> hashMap = channelList.get(getAdapterPosition());
                        Intent i = new Intent();
                        if (hashMap.get(Constants.TAG_CHANNEL_CATEGORY).equalsIgnoreCase(Constants.TAG_USER_CHANNEL)) {
                            if (hashMap.get(Constants.TAG_SUBSCRIBE_STATUS).equalsIgnoreCase("")) {
                                i = new Intent(context, ChannelRequestActivity.class);
                            } else if (hashMap.get(Constants.TAG_SUBSCRIBE_STATUS).equalsIgnoreCase(Constants.TRUE)) {
                                i = new Intent(context, ChannelChatActivity.class);
                            }
                        } else {
                            i = new Intent(context, ChannelChatActivity.class);
                        }
                        i.putExtra(Constants.TAG_CHANNEL_ID, hashMap.get(Constants.TAG_CHANNEL_ID));
                        startActivity(i);
                        break;
                }
            }
        }
    }
}
