package com.loopytime.im;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.StorageManager;
import com.loopytime.model.MediaModelData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AudioFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AudioFragment extends Fragment {
    private static final String ARG_PARAM1 = "user_id";
    private static final String ARG_PARAM2 = "type";
    String userId = "", type = "", TAG = "AudioFragment";
    RecyclerView recyclerView;
    Context context;
    DatabaseHandler dbhelper;
    StorageManager storageManager;
    BottomSheetBehavior bottomSheetBehavior;
    private TextView empty_view;

    public AudioFragment() {
        // Required empty public constructor
    }

    static AudioFragment newInstance(String param1, String param2) {
        AudioFragment fragment = new AudioFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_PARAM1);
            type = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_imageitem_list, container, false);
        context = view.getContext();
        recyclerView = view.findViewById(R.id.recyclerView);
        empty_view = view.findViewById(R.id.empty_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));

        storageManager = StorageManager.getInstance(context);
        dbhelper = DatabaseHandler.getInstance(context);

        setMediaAdapter();

        return view;
    }

    private void setMediaAdapter() {
        List<MediaModelData> data = new ArrayList<>();
        try {
            for (MediaModelData messagesData : dbhelper.getMedia(userId, type,context)) {
                if (messagesData.message_type.equals("audio")) {
                    data.add(messagesData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!data.isEmpty()) {
            AudioAdapter adapter = new AudioAdapter(context, data);
            recyclerView.setAdapter(adapter);
        } else {
            empty_view.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.ViewHolder> {

        Context context;
        List<MediaModelData> data;

        public AudioAdapter(Context context, List<MediaModelData> data) {
            this.context = context;
            this.data = data;
        }

        @NonNull
        @Override
        public AudioAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.document_item,
                    parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull AudioAdapter.ViewHolder holder, int position) {
            holder.icon.setImageResource(R.drawable.mp3);
            holder.filename.setText(data.get(position).attachment);
            //holder.file_type_tv.setText(FilenameUtils.getExtension(data.get(position).attachment));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView file_type_tv, filename;

            ViewHolder(View itemView) {
                super(itemView);

                icon = itemView.findViewById(R.id.icon);
                file_type_tv = itemView.findViewById(R.id.file_type_tv);
                filename = itemView.findViewById(R.id.filename);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MediaModelData message = data.get(getAdapterPosition());
                        if (storageManager.checkifFileExists(message.attachment, message.message_type, "receive")) {
                            try {
                                Intent intent = new Intent();
                                intent.setAction(android.content.Intent.ACTION_VIEW);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                File file = storageManager.getFile(message.attachment, message.message_type, "receive");
                                Uri photoURI = FileProvider.getUriForFile(context,
                                        BuildConfig.APPLICATION_ID + ".provider", file);

                                MimeTypeMap mime = MimeTypeMap.getSingleton();
                                String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                String type = mime.getMimeTypeFromExtension(ext);

                                intent.setDataAndType(photoURI, type);

                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(context, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        } else if (storageManager.checkifFileExists(message.attachment, message.message_type, "sent")) {
                            try {
                                Intent intent = new Intent();
                                intent.setAction(android.content.Intent.ACTION_VIEW);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                File file = storageManager.getFile(message.attachment, message.message_type, "sent");
                                Uri photoURI = FileProvider.getUriForFile(context,
                                        BuildConfig.APPLICATION_ID + ".provider", file);

                                MimeTypeMap mime = MimeTypeMap.getSingleton();
                                String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                String type = mime.getMimeTypeFromExtension(ext);

                                intent.setDataAndType(photoURI, type);

                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(context, getString(R.string.no_application), Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        } else {

                            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
                            /*if (isNetworkConnected().equals(NOT_CONNECT)) {
                                networkSnack();
                            } else {
                                DownloadFiles downloadFiles = new DownloadFiles(ChatActivity.this) {
                                    @Override
                                    protected void onPostExecute(String downPath) {
                                        progressbar.setVisibility(View.GONE);
                                        progressbar.stopSpinning();
                                        downloadicon.setVisibility(View.GONE);
                                        if (downPath == null) {
                                            Log.v("Download Failed", "Download Failed");
                                            Toast.makeText(mContext, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                                        } else {
                                            //Toast.makeText(mContext, getString(R.string.downloaded), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                };
                                downloadFiles.execute(Constants.CHAT_IMG_PATH + message.attachment, message.message_type);
                                progressbar.setVisibility(View.VISIBLE);
                                progressbar.spin();
                                downloadicon.setVisibility(View.VISIBLE);
                            }*/
                        }
                    }
                });
            }

        }
    }

}
