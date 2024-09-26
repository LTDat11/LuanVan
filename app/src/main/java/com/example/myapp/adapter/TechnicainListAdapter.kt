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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        getTechJobCount(technicain.id, holder)

        holder.itemView.setOnClickListener {
            // kiểm tra nếu itemUnSelect có src là ic_unselect thì set src là ic_select
            when {
                holder.itemUnSelect.drawable.constantState == holder.itemView.context.resources.getDrawable(R.drawable.ic_item_unselect).constantState && !isSelected -> {
                    holder.itemUnSelect.setImageResource(R.drawable.ic_item_selected)
                    isSelected = true
                    //Toast.makeText(holder.itemView.context, "Đã chọn ktv ${technicain.id} và thêm vào order ${orderId}", Toast.LENGTH_SHORT).show()
                    addTechnicainToOrder(technicain.id, orderId)
                }
                holder.itemUnSelect.drawable.constantState == holder.itemView.context.resources.getDrawable(R.drawable.ic_item_selected).constantState && isSelected -> {
                    holder.itemUnSelect.setImageResource(R.drawable.ic_item_unselect)
                    isSelected = false
                    removeTechnicainFromOrder(technicain.id, orderId)
                }
                holder.itemUnSelect.drawable.constantState == holder.itemView.context.resources.getDrawable(R.drawable.ic_item_unselect).constantState && isSelected -> {
                    Toast.makeText(holder.itemView.context, "Chỉ chọn 1 kỹ thuật viên", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun getTechJobCount(id: String, holder: TechnicainListAdapter.TechnicainViewHolder) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()

                db.collection("orders")
                    .whereEqualTo("id_technician", id)
                    .whereEqualTo("status", "processing")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            // Xử lý lỗi
                            holder.tvJobCount.text = "Không tìm thấy"
                            return@addSnapshotListener
                        }

                        if (snapshot != null && !snapshot.isEmpty) {
                            // Cập nhật số lượng đơn hàng của technician
                            holder.tvJobCount.text = snapshot.size().toString()
                        } else {
                            holder.tvJobCount.text = "0"
                        }
                    }

            }
        }
    }

    private fun removeTechnicainFromOrder(id: String, orderId: String) {
        // update technicain id to order
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            db.collection("orders").document(orderId)
                .update("id_technician", null)
                .addOnSuccessListener {

                }
                .addOnFailureListener {

                }
        }
    }

    private fun addTechnicainToOrder(id: String, orderId: String) {
        // update technicain id to order
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            db.collection("orders").document(orderId)
                .update("id_technician", id)
                .addOnSuccessListener {

                }
                .addOnFailureListener {

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
        val tvJobCount: TextView = itemView.findViewById(R.id.technicain_job_count)
    }
}