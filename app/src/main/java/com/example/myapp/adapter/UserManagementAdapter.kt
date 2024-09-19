package com.example.myapp.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.model.User
import de.hdodenhof.circleimageview.CircleImageView

class UserManagementAdapter (private var technicianList: List<User>, private val onMoreClickListener: (User) -> Unit): RecyclerView.Adapter<UserManagementAdapter.UserManagementViewHolder>() {

    inner class UserManagementViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val imgTechnician: CircleImageView = itemView.findViewById(R.id.imgTechnician)
        val tvTechnicianName: TextView = itemView.findViewById(R.id.tvTechnicianName)
        val tvTechnicianDescription: TextView = itemView.findViewById(R.id.tvTechnicianDiscription)
        val layoutTechnicianDiscription: View = itemView.findViewById(R.id.layoutTechnicianDiscription)
        val tvTechnicianAddress: TextView = itemView.findViewById(R.id.tvTechnicianAddress)
        val tvTechnicianPhone: TextView = itemView.findViewById(R.id.tvTechnicianPhone)
        val tvTechnicianEmail: TextView = itemView.findViewById(R.id.tvTechnicianEmail)
        val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserManagementViewHolder {
        val view = View.inflate(parent.context, R.layout.item_user_management, null)
        return UserManagementViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserManagementViewHolder, position: Int) {
        val technician = technicianList[position]
        holder.tvTechnicianName.text = technician.name
        holder.tvTechnicianDescription.text = technician.description
        holder.tvTechnicianAddress.text = technician.address
        holder.tvTechnicianPhone.text = technician.phone
        holder.tvTechnicianEmail.text = technician.email
        Glide.with(holder.itemView.context)
            .load(technician.imageURL)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(holder.imgTechnician)
        holder.layoutTechnicianDiscription.visibility = if (technician.description.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.btnMore.setOnClickListener { onMoreClickListener(technician) }
    }

    override fun getItemCount(): Int {
        return technicianList.size
    }

    fun updateList(newList: List<User>){
        technicianList = newList
        notifyDataSetChanged()
    }


}