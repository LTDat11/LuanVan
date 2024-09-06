package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.model.Repair

class RepairAdapter(private val repairs: List<Repair>) :
    RecyclerView.Adapter<RepairAdapter.RepairViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepairViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_repaired_device, parent, false)
        return RepairViewHolder(view)
    }

    override fun onBindViewHolder(holder: RepairViewHolder, position: Int) {
        val repair = repairs[position]
        holder.bind(repair)
    }

    override fun getItemCount(): Int = repairs.size

    inner class RepairViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tv_repair_name)
        private val priceTextView: TextView = itemView.findViewById(R.id.tv_repair_price)

        fun bind(repair: Repair) {
            nameTextView.text = repair.name
            priceTextView.text = repair.price.toString()
        }
    }
}
