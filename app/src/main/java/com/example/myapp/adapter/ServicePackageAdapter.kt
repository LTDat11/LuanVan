package com.example.myapp.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.activity.DetailPackageActivity
import com.example.myapp.model.ServicePackage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson

// recyclerview adapter for service packages
class ServicePackageAdapter(var packages: List<ServicePackage>) : RecyclerView.Adapter<ServicePackageAdapter.PackageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_package, parent, false)
        return PackageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        val servicePackage = packages[position]
        holder.bind(servicePackage)
    }

    override fun getItemCount(): Int = packages.size

    inner class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.package_image)
        private val nameTextView: TextView = itemView.findViewById(R.id.package_name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.package_description)
        private val priceTextView: TextView = itemView.findViewById(R.id.package_price)
        private val optionsButton: ImageButton = itemView.findViewById(R.id.package_options)

        fun bind(servicePackage: ServicePackage) {
            nameTextView.text = servicePackage.name
            descriptionTextView.text = servicePackage.description
            priceTextView.text = servicePackage.price
            val id = servicePackage.id
            val deviceId = servicePackage.deviceId
            val categoryId = servicePackage.categoryId
            val firestore = FirebaseFirestore.getInstance()

            firestore.collection("service_categories")
                .document(categoryId)
                .collection("devices")
                .document(deviceId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val deviceName = document.getString("name")
                        if (deviceName != null) {
                            loadImageFromFirebase(deviceName, imageView) { imageUrl ->
                                // Khi đã lấy được imageUrl, ta truyền nó qua intent
                                itemView.setOnClickListener {
                                    val intent = Intent(itemView.context, DetailPackageActivity::class.java)
                                    intent.putExtra("package", servicePackage)
                                    intent.putExtra("imageUrl", imageUrl)  // Kèm theo URL ảnh
                                    itemView.context.startActivity(intent)
                                }
                            }
                        }
                    } else {
                        Log.e("ServicePackageAdapter", "Device document does not exist")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ServicePackageAdapter", "Error getting device name", exception)
                }

            optionsButton.setOnClickListener {
                Toast.makeText(itemView.context, "Options for package ID: $id", Toast.LENGTH_SHORT).show()
            }
        }

        private fun loadImageFromFirebase(deviceName: String, imageView: ImageView, onImageUrlReady: (String) -> Unit) {
            val storageRef = FirebaseStorage.getInstance().reference.child("device/$deviceName/")

            storageRef.listAll()
                .addOnSuccessListener { listResult ->
                    if (listResult.items.isNotEmpty()) {
                        val imageRef = listResult.items[0]
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            Glide.with(imageView.context)
                                .load(uri)
                                .into(imageView)
                            onImageUrlReady(uri.toString())  // Trả về URL ảnh
                        }.addOnFailureListener { exception ->
                            Log.e("ServicePackageAdapter", "Error loading image URL", exception)
                        }
                    } else {
                        Log.e("ServicePackageAdapter", "No images found in storage for $deviceName")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ServicePackageAdapter", "Error listing files in storage", exception)
                }
        }
    }

}
