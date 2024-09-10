package com.example.myapp.adapter

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
import com.example.myapp.model.ServicePackage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ServicePackageAdminAdapter(var packages: List<ServicePackage>) :  RecyclerView.Adapter<ServicePackageAdminAdapter.PackageAdminViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageAdminViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_package_admin, parent, false)
        return PackageAdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: PackageAdminViewHolder, position: Int) {
        val servicePackage = packages[position]
        holder.bind(servicePackage)
    }

    override fun getItemCount(): Int {
        return packages.size
    }

    inner class PackageAdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.package_image)
        private val nameTextView: TextView = itemView.findViewById(R.id.package_name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.package_description)
        private val priceTextView: TextView = itemView.findViewById(R.id.package_price)
        private val editButton: ImageButton = itemView.findViewById(R.id.btn_edit_package)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete_package)

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
                            loadImageFromFirebase(deviceName, imageView)
                        }
                    }else {
                        Log.e("ServicePackageAdapter", "Device document does not exist")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ServicePackageAdapter", "Error getting device name", exception)
                }

            // event click
            editButton.setOnClickListener {
                //Toast id of package
                Toast.makeText(itemView.context, "Edit button clicked for package with id: $id", Toast.LENGTH_SHORT).show()
            }

            deleteButton.setOnClickListener {
                //Toast id of package
                Toast.makeText(itemView.context, "Delete button clicked for package with id: $id", Toast.LENGTH_SHORT).show()
            }


        }


    }

    private fun loadImageFromFirebase(deviceName: String, imageView: ImageView) {
        val storageRef = FirebaseStorage.getInstance().reference.child("device/$deviceName/")
        storageRef.listAll()
            .addOnSuccessListener{listResult ->
                if (listResult.items.isNotEmpty()) {
                    val imageRef = listResult.items[0]
                    imageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            Glide.with(imageView.context)
                                .load(uri)
                                .into(imageView)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ServicePackageAdapter", "Error getting image URL", exception)
                        }
                }else{
                    Log.e("ServicePackageAdapter", "No image found")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ServicePackageAdapter", "Error getting image URL", exception)
            }
    }

}