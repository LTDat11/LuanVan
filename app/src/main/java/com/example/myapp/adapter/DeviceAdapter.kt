package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.model.Device

class DeviceAdapter(
    private val devices: List<Device>,
    private val onDeviceClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    var selectedPosition: Int = -1 // Khởi tạo là -1 để không chọn mặc định

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.tv_device_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceName.text = device.name
        // Set image here if you have device images

        // Set selected background and text color
        if (selectedPosition == position) {
            holder.itemView.setBackgroundResource(R.drawable.bg_button_enable_corner_16)
            holder.deviceName.setTextColor(holder.itemView.context.resources.getColor(R.color.white))
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_button_disable_corner_16)
            holder.deviceName.setTextColor(holder.itemView.context.resources.getColor(R.color.black))
        }

        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            onDeviceClick(device)
        }
    }

    override fun getItemCount() = devices.size
}