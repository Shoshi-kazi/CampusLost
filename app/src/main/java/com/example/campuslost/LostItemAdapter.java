
package com.example.campuslost;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class LostItemAdapter extends RecyclerView.Adapter<LostItemAdapter.ItemViewHolder> {

    private ArrayList<LostItem> itemList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(LostItem item);
    }

    public LostItemAdapter(ArrayList<LostItem> itemList) {
        this.itemList = itemList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_row, parent, false);

        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {

        LostItem item = itemList.get(position);

        holder.tvItemTitle.setText(item.getTitle());
        holder.tvItemDescription.setText(item.getDescription());
        holder.tvItemLocation.setText("📍 " + item.getLocation());

        String type = item.getType();

        if (type != null && type.equalsIgnoreCase("Lost")) {
            holder.tvItemType.setText("LOST ITEM");
            holder.tvItemType.setBackgroundResource(R.drawable.badge_lost);
        } else {
            holder.tvItemType.setText("FOUND ITEM");
            holder.tvItemType.setBackgroundResource(R.drawable.badge_found);
        }

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            holder.ivItemImage.setVisibility(View.VISIBLE);

            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivItemImage);
        } else {
            holder.ivItemImage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {

        ImageView ivItemImage;
        TextView tvItemTitle, tvItemDescription;
        TextView tvItemLocation, tvItemType;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
            tvItemDescription = itemView.findViewById(R.id.tvItemDescription);
            tvItemLocation = itemView.findViewById(R.id.tvItemLocation);
            tvItemType = itemView.findViewById(R.id.tvItemType);
        }
    }
}
