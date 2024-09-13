package com.example.myapp.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.model.ServicePackage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.NumberFormat
import java.util.Locale

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
                            loadImageFromFirebase(deviceId, imageView)
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
                //edt package
                showDialogEditPackage(servicePackage, itemView.context)
            }

            deleteButton.setOnClickListener {
                showDialogConfirmDelete(servicePackage, itemView.context)
            }


        }


    }

    private fun showDialogConfirmDelete(servicePackage: ServicePackage, context: Context) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa gói dịch vụ ${servicePackage.name} không?")
            .setPositiveButton("Xóa") { _, _ ->
                val firestore = FirebaseFirestore.getInstance()
                val packageRef = firestore.collection("service_categories")
                    .document(servicePackage.categoryId)
                    .collection("devices")
                    .document(servicePackage.deviceId)
                    .collection("service_packages")
                    .document(servicePackage.id)
                packageRef.delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Xóa gói dịch vụ thành công", Toast.LENGTH_SHORT).show()

                        // Cập nhật danh sách sau khi xóa
                        val updatedPackages = packages.toMutableList()
                        updatedPackages.remove(servicePackage)  // Loại bỏ gói đã xóa khỏi danh sách
                        packages = updatedPackages  // Cập nhật danh sách gói dịch vụ
                        notifyDataSetChanged()  // Thông báo cho RecyclerView cập nhật lại
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Lỗi khi xóa gói dịch vụ: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .create()
        dialog.show()
    }

    private fun showDialogEditPackage(servicePackage: ServicePackage, context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_service_package, null)

        val etPackageName = dialogView.findViewById<EditText>(R.id.et_package_name)
        val etPackageDescription = dialogView.findViewById<EditText>(R.id.et_package_description)
        val etPackagePrice = dialogView.findViewById<EditText>(R.id.et_package_price)

        etPackageName.setText(servicePackage.name)
        etPackageDescription.setText(servicePackage.description)
        etPackagePrice.setText(servicePackage.price.replace(",", "").replace("VND", ""))

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle("Sửa Gói Dịch Vụ")
            .setPositiveButton("Sửa") { _, _ ->
                val updatedName = etPackageName.text.toString()
                val updatedDescription = etPackageDescription.text.toString()
                val updatedPrice = etPackagePrice.text.toString()

                if (updatedName!=servicePackage.name || updatedDescription!=servicePackage.description || updatedPrice!=servicePackage.price.replace(",", "").replace("VND", "")) {
                    if (updatedName.isNotEmpty() && updatedDescription.isNotEmpty() && updatedPrice.isNotEmpty()) {
                        val firestore = FirebaseFirestore.getInstance()
                        val packageRef = firestore.collection("service_categories")
                            .document(servicePackage.categoryId)
                            .collection("devices")
                            .document(servicePackage.deviceId)
                            .collection("service_packages")
                            .document(servicePackage.id)
                        packageRef.update("name", updatedName, "description", updatedDescription, "price", formatPrice(updatedPrice))
                            .addOnSuccessListener {
                                Toast.makeText(context, "Sửa gói dịch vụ thành công", Toast.LENGTH_SHORT).show()
                                // Cập nhật lại danh sách và thông báo thay đổi

                                val position = packages.indexOf(servicePackage)
                                if (position != -1) {
                                    notifyItemChanged(position)
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Lỗi khi sửa gói dịch vụ: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }else {
                        Toast.makeText(context, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Không có thay đổi nào được thực hiện", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .create()
        dialog.show()
    }

    // Hàm định dạng giá tiền
    private fun formatPrice(price: String): String {
        val price = price.toLongOrNull() ?: return price
        // Sử dụng NumberFormat để định dạng theo locale Việt Nam
        val numberFormat = NumberFormat.getNumberInstance(Locale.US)
        val formattedPrice = numberFormat.format(price)

        // Trả về chuỗi đã định dạng kèm với đơn vị tiền tệ VND
        return "$formattedPrice VND"
    }

    private fun loadImageFromFirebase(deviceId: String, imageView: ImageView) {
        val storageRef = FirebaseStorage.getInstance().reference.child("device/$deviceId/")
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