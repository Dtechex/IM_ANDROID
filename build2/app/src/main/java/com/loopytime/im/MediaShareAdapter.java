package com.loopytime.im;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.helper.StorageManager;
import com.loopytime.model.MediaModelData;
import com.loopytime.utils.Constants;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.List;

public class MediaShareAdapter extends RecyclerView.Adapter<MediaShareAdapter.ImageViewHolder> {
    Context context;
    StorageManager storageManager;
    String TAG = "MediaShareAdapter";
    int totalMedia;
    private List<MediaModelData> mediaList;

    MediaShareAdapter(List<MediaModelData> mediaList, Context context, int totalMedia) {
        this.mediaList = mediaList;
        this.context = context;
        this.totalMedia = totalMedia;
        storageManager = StorageManager.getInstance(context);
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.media_item, parent, false);
        return new ImageViewHolder(view);
    }

    private String getFileName(String url) {
        String imgSplit = url;
        int endIndex = imgSplit.lastIndexOf("/");
        if (endIndex != -1) {
            imgSplit = imgSplit.substring(endIndex + 1);
        }
        return imgSplit;
    }

    public String firstThree(String str) {
        return str.length() < 3 ? str : str.substring(0, 3);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        MediaModelData message = mediaList.get(position);
        if (position < 12) {
            if (message.message_type.equals("document")) {
                holder.mediaItem.setImageResource(R.drawable.icon_file_unknown);
                holder.file_type_tv.setVisibility(View.VISIBLE);
                holder.file_type_tv.setText(firstThree(FilenameUtils.getExtension(message.attachment)));
            } else {
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
        } else if (position == 12) {
            holder.mediaItem.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            holder.mediaItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.next_arrow));
        }
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    private void setImage(ImageView view, File file) {

        RequestOptions options = new RequestOptions().frame(1000).centerCrop();
        Glide.with(context).asBitmap()
                .load(Uri.fromFile(file))
                .apply(options)
                .into(view);
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

    void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    class ImageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView mediaItem;
        TextView file_type_tv;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            mediaItem = itemView.findViewById(R.id.mediaItem);
            file_type_tv = itemView.findViewById(R.id.file_type_tv);

            mediaItem.getLayoutParams().width = (int) ((ApplicationClass.getWidth(itemView.getContext())) * (0.23));
            mediaItem.getLayoutParams().height = (int) ((ApplicationClass.getWidth(itemView.getContext())) * (0.23));

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
}
