
package com.example.campuslost;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class LostItemAdapter extends RecyclerView.Adapter<LostItemAdapter.ItemViewHolder> {

    private ArrayList<LostItem> itemList;

    public LostItemAdapter(ArrayList<LostItem> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_row, parent, false);

        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ItemViewHolder holder, int position) {

        LostItem item = itemList.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvDescription.setText(item.getDescription());
        holder.tvLocation.setText("Location: " + item.getLocation());
        holder.tvType.setText("Type: " + item.getType());

        // Display item image
        String imageUrl = item.getImageUrl();

        if (imageUrl != null && !imageUrl.isEmpty()) {

            holder.ivItemImage.setVisibility(View.VISIBLE);

            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivItemImage);

        } else {
            holder.ivItemImage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvDescription, tvLocation, tvType;
        ImageView ivItemImage;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvDescription = itemView.findViewById(R.id.tvItemDescription);
            tvLocation = itemView.findViewById(R.id.tvItemLocation);
            tvType = itemView.findViewById(R.id.tvItemType);

            ivItemImage = itemView.findViewById(R.id.ivItemImage);
        }
    }
}
