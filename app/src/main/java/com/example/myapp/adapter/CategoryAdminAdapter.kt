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
        val categoryData = categoryMap[categoryId]?.split(",") ?: listOf("", "")
        val categoryName = categoryData[0]
        val categoryDescription = categoryData[1]

        holder.tvCategoryName.text = categoryName
        holder.tvCategoryDescription.text = categoryDescription

        val btnEditCategory = holder.itemView.findViewById<ImageButton>(R.id.btn_edit_category)
        val btnDeleteCategory = holder.itemView.findViewById<ImageButton>(R.id.btn_delete_category)

        holder.itemView.setOnClickListener {
            // intent to DeviceListAdminActivity
            val intent = Intent(holder.itemView.context, DeviceListAdminActivity::class.java)
            intent.putExtra("categoryId", categoryId)
            intent.putExtra("categoryName", categoryName)
            holder.itemView.context.startActivity(intent)
        }

        btnEditCategory.setOnClickListener {
            onEditClick(categoryId, categoryName)
        }

        btnDeleteCategory.setOnClickListener {
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

}
