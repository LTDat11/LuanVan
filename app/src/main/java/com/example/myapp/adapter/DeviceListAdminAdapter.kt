package com.example.myapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.model.Device
import com.google.firebase.storage.FirebaseStorage

class DeviceListAdminAdapter(
    private var deviceList: List<Device>,
    private val onEditClick: (Device) -> Unit,
    private val onDeleteClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceListAdminAdapter.DeviceViewHolder>() {

    private var fullDeviceList: List<Device> = deviceList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_admin, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    fun updateDevices(newDevices: List<Device>) {
        this.deviceList = newDevices
        this.fullDeviceList = newDevices
        notifyDataSetChanged()
    }

    fun filterDevices(query: String) {
        val filteredDevices = if (query.isEmpty()) {
            fullDeviceList
        } else {
            fullDeviceList.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        deviceList = filteredDevices
        notifyDataSetChanged()
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val btnEditDevice: ImageButton = itemView.findViewById(R.id.btn_edit_category)
        private val btnDeleteDevice: ImageButton = itemView.findViewById(R.id.btn_delete_category)
        private val circleImageView: ImageView = itemView.findViewById(R.id.circleImageView)

        fun bind(device: Device) {
            tvDeviceName.text = device.name

//            loadImage(device.name, circleImageView)

            btnEditDevice.setOnClickListener {
                onEditClick(device)
            }

            btnDeleteDevice.setOnClickListener {
                onDeleteClick(device)
            }
        }
    }

    private fun loadImage(name: String, circleImageView: ImageView) {
        // Load image from Firebase Storage with name of device
        val storageRef = FirebaseStorage.getInstance().reference.child("device/$name/")
        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isNotEmpty()) {
                    val imageRef = listResult.items[0]
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(circleImageView.context)
                            .load(uri)
                            .into(circleImageView)
                    }.addOnFailureListener { exception ->
                        Log.e("ServicePackageAdapter", "Error loading image URL", exception)
                    }
                } else {
                    Log.e("ServicePackageAdapter", "No images found in storage for $name")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ServicePackageAdapter", "Error listing files in storage", exception)
            }
    }


}
