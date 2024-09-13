package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.model.User

class TechnicainListAdapter(private val technicains: List<User>, private val orderId: String): RecyclerView.Adapter<TechnicainListAdapter.TechnicainViewHolder>() {

    private var isSelected: Boolean = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TechnicainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_technicain, parent, false)
        return TechnicainViewHolder(view)
    }

    override fun onBindViewHolder(holder: TechnicainViewHolder, position: Int) {
        val technicain = technicains[position]
        holder.tvTechnicainName.text = technicain.name
        holder.tvTechnicainDesciption.text = technicain.description
        // set image with glide
        Glide.with(holder.itemView.context).load(technicain.imageURL).into(holder.technicainImg)

        holder.itemView.setOnClickListener {
            // kiểm tra nếu itemUnSelect có src là ic_unselect thì set src là ic_select
            when {
                holder.itemUnSelect.drawable.constantState == holder.itemView.context.resources.getDrawable(R.drawable.ic_item_unselect).constantState && !isSelected -> {
                    holder.itemUnSelect.setImageResource(R.drawable.ic_item_selected)
                    isSelected = true
                    Toast.makeText(holder.itemView.context, "Đã chọn ktv ${technicain.id} và thêm vào order ${orderId}", Toast.LENGTH_SHORT).show()
                }
                holder.itemUnSelect.drawable.constantState == holder.itemView.context.resources.getDrawable(R.drawable.ic_item_selected).constantState && isSelected -> {
                    holder.itemUnSelect.setImageResource(R.drawable.ic_item_unselect)
                    isSelected = false
                }
                holder.itemUnSelect.drawable.constantState == holder.itemView.context.resources.getDrawable(R.drawable.ic_item_unselect).constantState && isSelected -> {
                    Toast.makeText(holder.itemView.context, "Chỉ chọn 1 kỹ thuật viên", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return technicains.size
    }

    class TechnicainViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val technicainImg: ImageView = itemView.findViewById(R.id.technicain_image)
        val tvTechnicainName: TextView = itemView.findViewById(R.id.technicain_name)
        val tvTechnicainDesciption: TextView = itemView.findViewById(R.id.technicain_description)
        val itemUnSelect: ImageView = itemView.findViewById(R.id.item_unselect)
    }
}