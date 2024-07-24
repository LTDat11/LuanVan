package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.model.PhotoBanner

class PhotoBannerAdapter(private val photoBanners: List<PhotoBanner>):
    RecyclerView.Adapter<PhotoBannerAdapter.PhotoBannerViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoBannerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.banner_item, parent, false)
        return PhotoBannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoBannerViewHolder, position: Int) {
        val photoBanner = photoBanners[position]
        Glide.with(holder.itemView.context)
            .load(photoBanner.imageUrl)
            .into(holder.imageView)
    }

    class PhotoBannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.banner_image)
    }

    override fun getItemCount(): Int {
        return photoBanners.size
    }

}