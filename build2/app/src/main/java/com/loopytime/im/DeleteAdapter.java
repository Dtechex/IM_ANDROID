package com.loopytime.im;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeleteAdapter extends RecyclerView.Adapter<DeleteAdapter.MyViewHolder> {

    Dialog dialog;
    List<String> reportList;
    deleteListener listener;

    public DeleteAdapter(List<String> reportList, Dialog dialog, deleteListener listener) {
        this.reportList = reportList;
        this.dialog = dialog;
        this.listener = listener;
    }

    public interface deleteListener{
        public void deletetype(String type);
    }

    @NonNull
    @Override
    public DeleteAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.report_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DeleteAdapter.MyViewHolder holder, int position) {
        holder.texItem.setText(reportList.get(position));
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView texItem;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            texItem = itemView.findViewById(R.id.itemText);

            texItem.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.itemText) {

                if(getAdapterPosition()==0){
                    listener.deletetype("me");
                } else if(getAdapterPosition() == 2){
                    listener.deletetype("everyone");
                }  

                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }
    }
}
