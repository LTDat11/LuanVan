package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.model.FilterItem

class FilterAdapter(private val filterOptions: List<FilterItem>, private val onFilterSelected: (FilterItem) -> Unit) :
    RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    private var selectedPosition: Int = 0 // Default to "All" filter
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_filter, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val option = filterOptions[position]
        holder.bind(option)
    }

    override fun getItemCount(): Int = filterOptions.size

    inner class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.img_filter)
        private val textView: TextView = itemView.findViewById(R.id.tv_title)

        fun bind(filterItem: FilterItem) {
            imageView.setImageResource(filterItem.iconResId)
            textView.text = filterItem.title

            // Set the background based on selection state
            if (position == selectedPosition) {
                imageView.setColorFilter(itemView.context.resources.getColor(R.color.white))
                textView.setTextColor(itemView.context.resources.getColor(R.color.white))
                itemView.setBackgroundResource(R.drawable.bg_button_enable_corner_16)
            } else {
                imageView.setColorFilter(itemView.context.resources.getColor(R.color.black))
                textView.setTextColor(itemView.context.resources.getColor(R.color.black))
                itemView.setBackgroundResource(R.drawable.bg_button_disable_corner_16)
            }

            itemView.setOnClickListener {
                selectedPosition = position
                notifyDataSetChanged()
                onFilterSelected(filterItem)
            }
        }
    }
}
