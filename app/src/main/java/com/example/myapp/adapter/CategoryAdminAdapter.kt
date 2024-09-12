package com.example.myapp.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.activity.DeviceListAdminActivity
import com.example.myapp.model.ServiceCategory
import com.google.firebase.firestore.FirebaseFirestore

class CategoryAdminAdapter(
    private var categoryMap: Map<String, String>,
    private val onEditClick: (String, String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdminAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        val tvCategoryDescription = itemView.findViewById<TextView>(R.id.tv_category_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val categoryId = categoryMap.keys.elementAt(position)
        val categoryName = categoryMap[categoryId]
        val btnEditCategory = holder.itemView.findViewById<ImageButton>(R.id.btn_edit_category)
        val btnDeleteCategory = holder.itemView.findViewById<ImageButton>(R.id.btn_delete_category)

        holder.tvCategoryName.text = categoryName
        getInfoCategory(categoryId, holder.tvCategoryDescription )

        holder.itemView.setOnClickListener {
            // intent to DeviceListAdminActivity
            val intent = Intent(holder.itemView.context, DeviceListAdminActivity::class.java)
            intent.putExtra("categoryId", categoryId)
            intent.putExtra("categoryName", categoryName)
            holder.itemView.context.startActivity(intent)

        }

        btnEditCategory.setOnClickListener {
            // Handle edit button click
            onEditClick(categoryId, categoryName!!)
        }

        btnDeleteCategory.setOnClickListener {
            // Handle delete button click
            onDeleteClick(categoryId)
        }
    }

    override fun getItemCount(): Int {
        return categoryMap.size
    }

    fun updateCategories(newCategoryMap: Map<String, String>) {
        categoryMap = newCategoryMap
        notifyDataSetChanged() // Notify RecyclerView to refresh the data
    }

    private fun getInfoCategory(
        categoryId: String,
        tvCategoryDescription: TextView
    ) {
        // Lấy thông tin danh mục dịch vụ từ Firestore
        val firestore = FirebaseFirestore.getInstance()
        val categoryRef = firestore.collection("service_categories").document(categoryId)
        categoryRef.get().addOnSuccessListener { document ->
            val category = document.toObject(ServiceCategory::class.java)
            if (category != null) {
                tvCategoryDescription.text = category.description
            } else {

            }
        }.addOnFailureListener { e ->

        }

    }
}
