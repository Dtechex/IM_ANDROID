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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.StorageManager;
import com.loopytime.model.MediaModelData;
import com.loopytime.utils.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageFragment extends Fragment {

    private static final String ARG_PARAM1 = "user_id";
    private static final String ARG_PARAM2 = "type";
    String userId = "", type = "", TAG = "AudioFragment";
    RecyclerView recyclerView;
    RelativeLayout imageViewLay;
    Context context;
    DatabaseHandler dbhelper;
    StorageManager storageManager;
    BottomSheetBehavior bottomSheetBehavior;
    private TextView empty_view;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ImageFragment() {
    }

    static ImageFragment newInstance(String param1, String param2) {
        ImageFragment fragment = new ImageFragment();
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
        recyclerView.setLayoutManager(new GridLayoutManager(context, 3));

        storageManager = StorageManager.getInstance(context);
        dbhelper = DatabaseHandler.getInstance(context);

        setMediaAdapter();

        return view;
    }

    private void setMediaAdapter() {
        List<MediaModelData> data = new ArrayList<>();
        try {
            for (MediaModelData messagesData : dbhelper.getMedia(userId, type,context)) {
                if (messagesData.message_type.equals("image") || messagesData.message_type.equals("video")) {
                    data.add(messagesData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!data.isEmpty()) {
            GridAdapter adapter = new GridAdapter(context, data);
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

    private String getFileName(String url) {
        String imgSplit = url;
        int endIndex = imgSplit.lastIndexOf("/");
        if (endIndex != -1) {
            imgSplit = imgSplit.substring(endIndex + 1);
        }
        return imgSplit;
    }

    public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {
        Context context;
        List<MediaModelData> mediaList;

        GridAdapter(Context context, List<MediaModelData> mediaItem) {
            this.context = context;
            this.mediaList = mediaItem;
        }

        @NonNull
        @Override
        public GridAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.media_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull GridAdapter.ViewHolder holder, int position) {
            MediaModelData message = mediaList.get(position);
            if (message.message_type.equalsIgnoreCase("video")) {
                if (storageManager.checkifFileExists(message.attachment, message.message_type, "sent")) {
                    File file = storageManager.getFile(message.attachment, message.message_type, "sent");
                    setImage(holder.mediaItem,file);
                } else if (storageManager.checkifFileExists(message.attachment, message.message_type, "receive")) {
                    File file = storageManager.getFile(message.attachment, message.message_type, "receive");
                    setImage(holder.mediaItem,file);
                }
            }else {
                if (storageManager.checkifImageExists("sent", message.attachment)) {
                    File file = storageManager.getImage("sent", message.attachment);
                    setImage(holder.mediaItem,file);
                } else if (storageManager.checkifImageExists("thumb", message.attachment)) {
                    File file = storageManager.getImage("thumb", message.attachment);
                    setImage(holder.mediaItem,file);
                }
            }
            /*if (message.sender_id.equals(GetSet.getUserId())) {
                if (message.message_type.equalsIgnoreCase("video")) {
                    if (storageManager.checkifFileExists(message.attachment, message.message_type, "sent")) {
                        File file = storageManager.getFile(message.attachment, message.message_type, "sent");
                        setImage(holder.mediaItem,file);
                    }
                }else {
                    if (storageManager.checkifImageExists("sent", message.attachment)) {
                        File file = storageManager.getImage("sent", message.attachment);
                        setImage(holder.mediaItem,file);
                    }
                }
            } else {
                if (message.message_type.equalsIgnoreCase("video")) {
                    if (storageManager.checkifFileExists(message.attachment, message.message_type, "receive")) {
                        File file = storageManager.getFile(message.attachment, message.message_type, "receive");
                        setImage(holder.mediaItem,file);
                    }
                }else {
                    if (storageManager.checkifImageExists("thumb", message.attachment)) {
                        File file = storageManager.getImage("thumb", message.attachment);
                        setImage(holder.mediaItem,file);
                    }
                }
            }*/
        }

        private void setImage(ImageView view, File file) {
            RequestOptions options = new RequestOptions().frame(1000).centerCrop();
            Glide.with(context).asBitmap()
                    .load(Uri.fromFile(file))
                    .apply(options)
                    .into(view);
        }

        @Override
        public int getItemCount() {
            return mediaList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            ImageView mediaItem;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                mediaItem = itemView.findViewById(R.id.mediaItem);

                mediaItem.getLayoutParams().width = (int) ((ApplicationClass.getWidth(itemView.getContext())) * (0.33));
                mediaItem.getLayoutParams().height = (int) ((ApplicationClass.getWidth(itemView.getContext())) * (0.33));

                mediaItem.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.mediaItem) {
                    MediaModelData message = mediaList.get(getAdapterPosition());
                    openMedia(message,mediaItem);
                }
            }
        }

        private void openMedia(MediaModelData message,ImageView view) {
            String type = message.message_type;
            String name = message.attachment;
            if (type.equalsIgnoreCase("video")) {
                if (storageManager.checkifFileExists(name, type, "sent")) {
                    try {
                        Intent intent = new Intent();
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        File file = storageManager.getFile(message.attachment, message.message_type, "sent");
                        Uri photoURI = FileProvider.getUriForFile(context,
                                BuildConfig.APPLICATION_ID + ".provider", file);

                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                        String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                        String mimeType = mime.getMimeTypeFromExtension(ext);

                        intent.setDataAndType(photoURI, mimeType);

                        context.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        showToast(context,context.getString(R.string.no_application));
                        e.printStackTrace();
                    }
                } else if (storageManager.checkifFileExists(name, type, "receive")) {
                    try {
                        Intent intent = new Intent();
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        File file = storageManager.getFile(name, type, "receive");
                        Uri photoURI = FileProvider.getUriForFile(context,
                                BuildConfig.APPLICATION_ID + ".provider", file);

                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                        String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                        String mimeType = mime.getMimeTypeFromExtension(ext);

                        intent.setDataAndType(photoURI, mimeType);

                        context.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        showToast(context,context.getString(R.string.no_application));
                        e.printStackTrace();
                    }
                }else {
                    showToast(context,context.getString(R.string.no_media));
                }
            } else {
                if (storageManager.checkifImageExists("sent", name)) {
                    File file = storageManager.getImage("sent", name);
                    if (file != null) {
                        ApplicationClass.openImage(context,file.getAbsolutePath(), Constants.TAG_MESSAGE,view);
                    }
                } else if (storageManager.checkifImageExists("thumb", name)) {
                    File file = storageManager.getImage("thumb", name);
                    if (file != null) {
                        ApplicationClass.openImage(context,file.getAbsolutePath(), Constants.TAG_MESSAGE,view);
                    }
                } else {
                    showToast(context,context.getString(R.string.no_media));
                }
            }
        }

        void showToast(Context context , String text){
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }
}
