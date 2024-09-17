package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R

class BannerManagementAdapter(private val bannerList: List<String>, private val onDeleteClick: (String) -> Unit) :
    RecyclerView.Adapter<BannerManagementAdapter.BannerViewHolder>() {

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view_banner)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete_banner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val imageUrl = bannerList[position]

        // Sử dụng Glide để load ảnh từ URL
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.imageView)

        holder.deleteButton.setOnClickListener {
            onDeleteClick(imageUrl) // Gọi hàm xóa banner
        }
    }

    override fun getItemCount(): Int {
        return bannerList.size
    }
}
