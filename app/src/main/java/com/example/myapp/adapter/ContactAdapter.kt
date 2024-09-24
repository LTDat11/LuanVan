package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.databinding.ItemContactBinding
import com.example.myapp.model.ContactItem

class ContactAdapter(
    private val contactList: List<ContactItem>,
    private val onItemClick: (ContactItem) -> Unit // Callback để xử lý sự kiện nhấn
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contactItem = contactList[position]
        holder.bind(contactItem, onItemClick)
    }

    override fun getItemCount(): Int = contactList.size

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgIcon: ImageView = itemView.findViewById(R.id.img_contact_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_contact_name)

        fun bind(contactItem: ContactItem, onItemClick: (ContactItem) -> Unit) {
            imgIcon.setImageResource(contactItem.iconResId)
            tvName.text = contactItem.name

            itemView.setOnClickListener {
                onItemClick(contactItem)
            }
        }
    }
}
