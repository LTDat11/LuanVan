package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.model.Device

class DeviceAdminAdapter(
    private val devices: List<Device>,
    private val onDeviceClickListener: OnDeviceClickListener
) : RecyclerView.Adapter<DeviceAdminAdapter.DeviceViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.tv_device_name)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(selectedPosition)
                    selectedPosition = position
                    notifyItemChanged(selectedPosition)
                    onDeviceClickListener.onDeviceClick(devices[position].id_device)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceName.text = device.name

        // Set selected background and text color
        if (selectedPosition == position) {
            holder.itemView.setBackgroundResource(R.drawable.bg_button_enable_corner_16)
            holder.deviceName.setTextColor(holder.itemView.context.resources.getColor(R.color.white))
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_button_disable_corner_16)
            holder.deviceName.setTextColor(holder.itemView.context.resources.getColor(R.color.black))
        }
    }

    override fun getItemCount(): Int = devices.size



    interface OnDeviceClickListener {
        fun onDeviceClick(deviceId: String)
    }
}
