package com.loopytime.im;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.apprtc.util.AppRTCUtils;
import com.loopytime.external.RecyclerItemClickListener;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.SocketConnection;
import com.loopytime.helper.Utils;
import com.loopytime.model.ContactsData;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WAKE_LOCK;

/**
 * Created by hitasoft on 29/7/18.
 */

public class CallFragment extends Fragment {
    public static CallFragment callFragment;
    private final String TAG = this.getClass().getSimpleName();
    ClearLogFunction clearLogFunction;
    RecyclerViewAdapter callAdapter;
    RecyclerView recyclerView;
    RelativeLayout progressLay;
    LinearLayout nullLay;
    TextView nullText;
    LinearLayoutManager linearLayoutManager;
    DatabaseHandler dbhelper;
    SocketConnection socketConnection;
    boolean callLongPressed = false;
    private ArrayList<HashMap<String, String>> callList = new ArrayList<>();
    private ArrayList<HashMap<String, String>> selectedCallList = new ArrayList<>();
    public CallFragment() {
    }

    static CallFragment newInstance(ClearLogFunction image) {
        CallFragment fragment = new CallFragment();
        Bundle args = new Bundle();
        fragment.setClearLogFunction(image);
        return fragment;
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (callList != null && !callList.isEmpty() && dbhelper !=null) {
                for (HashMap<String, String> map : callList) {
                    if (map.get(Constants.TAG_CALL_STATUS).equals("missed")
                            && map.get(Constants.TAG_IS_ALERT).equals("0")) {
                        dbhelper.updateCallAlert(map.get(Constants.TAG_CALL_ID));
                    }
                }
            }
            if (SocketConnection.onUpdateTabIndication != null) {
                SocketConnection.onUpdateTabIndication.updateIndication();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void setClearLogFunction(ClearLogFunction clearLogFunction) {
        this.clearLogFunction = clearLogFunction;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);

        progressLay = view.findViewById(R.id.progress);
        nullLay = view.findViewById(R.id.nullLay);
        nullText = view.findViewById(R.id.nullText);
        recyclerView = view.findViewById(R.id.recyclerView);

        socketConnection = SocketConnection.getInstance(getActivity());
        dbhelper = DatabaseHandler.getInstance(getActivity());
        linearLayoutManager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        callList.clear();
        callList = dbhelper.getRecentCall();
        callAdapter = new RecyclerViewAdapter(getActivity(), callList);
        recyclerView.setAdapter(callAdapter);
        callAdapter.notifyDataSetChanged();
        nullText.setText(R.string.no_calls_yet_buddy);
        if (callList.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
        } else {
            nullLay.setVisibility(View.GONE);
        }

        /*if(!callList.isEmpty()){
            for(HashMap<String,String> map : callList){
                if(map.get(Constants.TAG_CALL_STATUS).equals("missed")
                        && map.get(Constants.TAG_IS_ALERT).equals("0")){
                    dbhelper.updateCallAlert(map.get(Constants.TAG_CALL_ID));
                }
            }
        }
        if (SocketConnection.onUpdateTabIndication != null) {
            SocketConnection.onUpdateTabIndication.updateIndication();
        }*/

        callFragment = this;

        recyclerView.addOnItemTouchListener(chatItemClick(getContext(), recyclerView));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAdapter();

    }

    void refreshAdapter() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (callAdapter != null) {
                    callList.clear();
                    callList.addAll(dbhelper.getRecentCall());
                    callAdapter.notifyDataSetChanged();
                    if (SocketConnection.onUpdateTabIndication != null) {
                        SocketConnection.onUpdateTabIndication.updateIndication();
                    }
                }
                if (callList.size() == 0) {
                    nullLay.setVisibility(View.VISIBLE);
                } else {
                    nullLay.setVisibility(View.GONE);
                }
            }
        });
    }

    public RecyclerItemClickListener chatItemClick(Context mContext, final RecyclerView recyclerView) {
        return new RecyclerItemClickListener(mContext, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (callLongPressed) {

                    if (callList.get(position).get(Constants.TAG_IS_SELECTED).equals("0")) {
                        callList.get(position).put(Constants.TAG_IS_SELECTED, "1");
                        selectedCallList.add(callList.get(position));
                        callAdapter.notifyItemChanged(position);
                    } else {
                        callList.get(position).put(Constants.TAG_IS_SELECTED, "0");
                        selectedCallList.remove(callList.get(position));
                        callAdapter.notifyItemChanged(position);
                    }
                    if (clearLogFunction != null)
                        clearLogFunction.isDeleteVisible(true, selectedCallList.size());

                    if (selectedCallList.isEmpty()) {
                        callLongPressed = false;
                        if (clearLogFunction != null)
                            clearLogFunction.isDeleteVisible(false, selectedCallList.size());
                    }
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                Log.d(TAG, "onItemLongClick: " + position);
                if (!callLongPressed) {
                    callLongPressed = true;
                    callList.get(position).put(Constants.TAG_IS_SELECTED, "1");
                    selectedCallList.add(callList.get(position));
                    callAdapter.notifyItemChanged(position);
                    if (clearLogFunction != null)
                        clearLogFunction.isDeleteVisible(true, selectedCallList.size());
                }
            }
        });
    }

    public void deleteCallLog(String type) {
        if (type.equals("delete")) {
            deleteCallConfirmDialog();
        } else {
            updateView();
        }
    }

    void updateView() {
        callLongPressed = false;
        for (int i = 0; i < callList.size(); i++) {
            HashMap<String, String> map = callList.get(i);
            if (map.get(Constants.TAG_IS_SELECTED).equals("1")) {
                map.put(Constants.TAG_IS_SELECTED, "0");
                callAdapter.notifyItemChanged(i);
            }
        }
        if (!selectedCallList.isEmpty()) {
            selectedCallList.clear();
        }
    }

    private void deleteCallConfirmDialog() {
        final Dialog dialog = new Dialog(getContext());
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
        title.setText(R.string.really_delete_call_history
        );
        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                for (int i = 0; i < selectedCallList.size(); i++) {
                    HashMap<String, String> map = selectedCallList.get(i);
                    dbhelper.deleteCallFromId(map.get(Constants.TAG_CALL_ID));
                }
                selectedCallList.clear();
                refreshAdapter();
                callLongPressed = false;
                if (clearLogFunction != null)
                    clearLogFunction.isDeleteVisible(false, selectedCallList.size());
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (clearLogFunction != null)
                    clearLogFunction.isDeleteVisible(false, selectedCallList.size());
                updateView();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                updateView();
            }
        });

        dialog.show();
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

    private void blockChatConfirmDialog(String userId) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.default_popup);
        dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 90 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView title = dialog.findViewById(R.id.title);
        TextView yes = dialog.findViewById(R.id.yes);
        TextView no = dialog.findViewById(R.id.no);

        yes.setText(getString(R.string.unblock));
        no.setText(getString(R.string.cancel));
        title.setText(R.string.unblock_message);

        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                    jsonObject.put(Constants.TAG_RECEIVER_ID, userId);
                    jsonObject.put(Constants.TAG_TYPE, "unblock");
                    Log.v(TAG, "block=" + jsonObject);
                    socketConnection.block(jsonObject);
                    dbhelper.updateBlockStatus(userId, Constants.TAG_BLOCKED_BYME, "unblock");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {

            boolean isPermissionEnabled = false;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    isPermissionEnabled = false;
                    break;
                } else {
                    isPermissionEnabled = true;
                }
            }

            if (!isPermissionEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(CAMERA) &&
                            shouldShowRequestPermissionRationale(RECORD_AUDIO) &&
                            shouldShowRequestPermissionRationale(WAKE_LOCK)) {
                        requestPermission(new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 100);
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.call_permission_error), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private boolean checkPermissions() {
        int permissionCamera = ContextCompat.checkSelfPermission(getActivity(),
                CAMERA);
        int permissionAudio = ContextCompat.checkSelfPermission(getActivity(),
                RECORD_AUDIO);
        int permissionWakeLock = ContextCompat.checkSelfPermission(getActivity(),
                WAKE_LOCK);
        return permissionCamera == PackageManager.PERMISSION_GRANTED &&
                permissionAudio == PackageManager.PERMISSION_GRANTED &&
                permissionWakeLock == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String[] permissions, int requestCode) {
        requestPermissions(permissions, requestCode);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        callFragment = null;
    }

    public interface ClearLogFunction {
        void isDeleteVisible(boolean isDelete, int count);
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

        ArrayList<HashMap<String, String>> callList = new ArrayList<>();
        Context context;
        int lastSelectedPosition = -1;
        boolean canSelect = false;

        public RecyclerViewAdapter(Context context, ArrayList<HashMap<String, String>> callList) {
            this.callList = callList;
            this.context = context;
        }

        @Override
        public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_call, parent, false);

            return new RecyclerViewAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final RecyclerViewAdapter.MyViewHolder holder, int position) {

            final HashMap<String, String> map = callList.get(position);

            Glide.with(context).load(Constants.USER_IMG_PATH + map.get(Constants.TAG_USER_IMAGE)).thumbnail(0.5f)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                    .into(holder.profileImage);
            holder.txtName.setText(map.get(Constants.TAG_USER_NAME));
            if (map.get(Constants.TAG_CREATED_AT) != null) {
                holder.txtTime.setText(Utils.getFormattedDate(context, Long.parseLong(map.get(Constants.TAG_CREATED_AT).replace(".", ""))));
            }

            if (map.get(Constants.TAG_TYPE).equals("audio")) {
                holder.callType.setImageResource(R.drawable.call);
            } else {
                holder.callType.setImageResource(R.drawable.videocall);
            }

            if (map.get(Constants.TAG_CALL_STATUS) != null) {
                switch (map.get(Constants.TAG_CALL_STATUS)) {
                    case "incoming":
                        holder.statusIcon.setImageResource(R.drawable.incoming);
                        break;
                    case "missed":
                        holder.statusIcon.setImageResource(R.drawable.missed);
                        break;
                    case "outgoing":
                        holder.statusIcon.setImageResource(R.drawable.outgoing);
                        break;
                }
            }

            if (map.get(Constants.TAG_IS_SELECTED).equals("1")) {
                holder.callType.setVisibility(View.GONE);
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.chat_selected));
            } else {
                holder.callType.setVisibility(View.VISIBLE);
                holder.itemView.setBackgroundColor(0);
            }

        }

        @Override
        public int getItemCount() {
            return callList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentLay;
            TextView txtName, txtTime;
            CircleImageView profileImage;
            ImageView statusIcon, callType;
            View profileView;

            public MyViewHolder(View view) {
                super(view);

                parentLay = view.findViewById(R.id.parentLay);
                txtTime = view.findViewById(R.id.txtTime);
                txtName = view.findViewById(R.id.txtName);
                profileImage = view.findViewById(R.id.profileImage);
                profileView = view.findViewById(R.id.profileView);
                statusIcon = view.findViewById(R.id.statusIcon);
                callType = view.findViewById(R.id.callType);
                callType.setOnClickListener(this);
                profileImage.setOnClickListener(this);

            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.callType:
                        ContactsData.Result result = dbhelper.getContactDetail(callList.get(getAdapterPosition()).get(Constants.TAG_USER_ID));
                        if (callList.get(getAdapterPosition()).get(Constants.TAG_TYPE).equals("audio")) {
                            if (!checkPermissions()) {
                                requestPermission(new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 100);
                            } else if (result.blockedbyme.equals("block")) {
                                blockChatConfirmDialog(result.user_id);
                            } else {
                                if (NetworkReceiver.isConnected()) {
                                    ApplicationClass.preventMultiClick(callType);
                                    AppRTCUtils appRTCUtils = new AppRTCUtils(getContext());
                                    Intent video = appRTCUtils.connectToRoom(callList.get(getAdapterPosition()).get(Constants.TAG_USER_ID), Constants.TAG_SEND, Constants.TAG_AUDIO);
                                    video.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                    startActivity(video);
                                } else {
                                    ApplicationClass.showToast(context, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT);
                                }
                            }
                        } else {
                            if (!checkPermissions()) {
                                requestPermission(new String[]{CAMERA, RECORD_AUDIO, WAKE_LOCK}, 100);
                            } else if (result.blockedbyme.equals("block")) {
                                blockChatConfirmDialog(result.user_id);
                            } else {
                                if (NetworkReceiver.isConnected()) {
                                    ApplicationClass.preventMultiClick(callType);
                                    AppRTCUtils appRTCUtils = new AppRTCUtils(getContext());
                                    Intent video = appRTCUtils.connectToRoom(result.user_id, Constants.TAG_SEND, Constants.TAG_VIDEO);
                                    startActivity(video);
                                } else {
                                    ApplicationClass.showToast(context, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT);
                                }
                            }
                        }
                        break;
                    case R.id.profileImage:
                        break;

                }
            }
        }
    }
}
