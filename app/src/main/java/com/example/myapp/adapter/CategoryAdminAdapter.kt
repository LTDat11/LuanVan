package com.example.myapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.activity.DeviceListAdminActivity
import com.example.myapp.model.ServiceCategory

class CategoryAdminAdapter(
    private var categories: List<ServiceCategory>,
    private val onEditClick: (ServiceCategory) -> Unit,
    private val onDeleteClick: (ServiceCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdminAdapter.CategoryViewHolder>() {

    private var fullCategoryList: List<ServiceCategory> = categories

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category)
    }

    override fun getItemCount(): Int = categories.size


    fun updateCategories(newCategories: List<ServiceCategory>) {
        this.categories = newCategories
        this.fullCategoryList = newCategories
        notifyDataSetChanged()
    }

    fun filterCategories(query: String) {
        val filteredCategories = if (query.isEmpty()) {
            fullCategoryList
        } else {
            fullCategoryList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description?.contains(query, ignoreCase = true) == true
            }
        }
        categories = filteredCategories
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val tvCategoryDescription: TextView = itemView.findViewById(R.id.tv_category_description)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit_category)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_category)

        fun bind(category: ServiceCategory) {
            tvCategoryName.text = category.name
            tvCategoryDescription.text = category.description

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, DeviceListAdminActivity::class.java)
                intent.putExtra("categoryId", category.id)
                intent.putExtra("categoryName", category.name)
                itemView.context.startActivity(intent)
            }

            btnEdit.setOnClickListener {
                onEditClick(category)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(category)
            }
        }

    }
}