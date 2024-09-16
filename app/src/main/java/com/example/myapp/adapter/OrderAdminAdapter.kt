package com.example.myapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.activity.AssignTechAdminActivity
import com.example.myapp.activity.InfoProcessingAdminActivity
import com.example.myapp.activity.ReceiptOrderActivity
import com.example.myapp.activity.TrackingOrderTechActivity
import com.example.myapp.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderAdminAdapter(private val orders: List<Order>): RecyclerView.Adapter<OrderAdminAdapter.OrderAdminViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderAdminAdapter.OrderAdminViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_admin, parent, false)
        return OrderAdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderAdminViewHolder, position: Int) {
        val order = orders[position]
        holder.tvOrderId.text = order.id
        holder.tvPackageName.text = order.namePackage
        holder.tvStatusDescription.text = order.description
        holder.tvIntruction.text = order.notes2

        when (order.status) {
            "pending" -> {
                holder.tvStatus.text = "Chờ xác nhận"
            }
            "processing" -> {
                holder.tvStatus.text = "Đang xử lý"
                // hide button accept and hide
                holder.layoutInstruction.visibility = View.GONE
                holder.btnAcecept.visibility = View.GONE
                // set text status description title and status description
                holder.tvStatusDescriptionTitle.text = "Nhân viên xử lý:"
                getNameTech(order.id_technician, holder.tvStatusDescription)
            }
            "completed" -> {
                holder.tvStatus.text = "Chờ thanh toán"
                // hide button accept and hide
                holder.layoutInstruction.visibility = View.GONE
                holder.btnAcecept.visibility = View.GONE
                // set text status description title and status description
                holder.tvStatusDescriptionTitle.text = "Nhân viên xử lý:"
                getNameTech(order.id_technician, holder.tvStatusDescription)
            }
            "finish" -> {
                holder.tvStatus.text = "Đã thanh toán"
                // hide button accept and hide
                holder.layoutInstruction.visibility = View.GONE
                holder.btnAcecept.visibility = View.GONE
                // set text status description title and status description
                holder.tvStatusDescriptionTitle.text = "Nhân viên xử lý:"
                getNameTech(order.id_technician, holder.tvStatusDescription)
            }
        }

        holder.itemView.setOnClickListener {
            when (order.status) {
                "pending" -> {
                    // Xử lý khi click vào item
                    val context = holder.itemView.context
                    val intent = Intent(context, AssignTechAdminActivity::class.java)
                    // truyền dữ liệu order qua màn hình tiếp theo
                    intent.putExtra("order", order)
                    context.startActivity(intent)
                }
                "processing" -> {
                    val context = holder.itemView.context
                    val intent = Intent(context, InfoProcessingAdminActivity::class.java)
                    // truyền dữ liệu order qua màn hình tiếp theo
                    intent.putExtra("order", order)
                    context.startActivity(intent)
                }
                "completed" -> {
                    val context = holder.itemView.context
                    val intent = Intent(context, TrackingOrderTechActivity::class.java)
                    // truyền dữ liệu order qua màn hình tiếp theo
                    intent.putExtra("order_id", order.id)
                    intent.putExtra("imgURL", order.imgURLServicePackage)
                    context.startActivity(intent)
                }
                "finish" -> {
                    val context = holder.itemView.context
                    val intent = Intent(context, ReceiptOrderActivity::class.java)
                    // truyền dữ liệu order qua màn hình tiếp theo
                    intent.putExtra("order_id", order.id)
                    context.startActivity(intent)
                }
            }
        }

        holder.btnAcecept.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, AssignTechAdminActivity::class.java)
            // truyền dữ liệu order qua màn hình tiếp theo
            intent.putExtra("order", order)
            context.startActivity(intent)
        }

    }

    private fun getNameTech(idTechnician: String?, tvStatusDescription: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {

                // use snapshot to get name of technician
                val db = FirebaseFirestore.getInstance()
                db.collection("Users")
                    .document(idTechnician ?: return@withContext) // Kiểm tra null cho idTechnician
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            // Xử lý lỗi nếu có
                            tvStatusDescription.text = "Lỗi khi lắng nghe dữ liệu"
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            val name = snapshot.getString("name") // Lấy giá trị name
                            tvStatusDescription.text = name ?: "Không có tên"
                        } else {
                            tvStatusDescription.text = "Chưa có nhân viên xử lý"
                        }
                    }
            }
        }

    }


    override fun getItemCount() = orders.size

    class OrderAdminViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(R.id.tv_order_id)
        val tvPackageName: TextView = view.findViewById(R.id.tv_package_name)
        val tvStatusDescription: TextView = view.findViewById(R.id.tv_status_description)
        val tvStatusDescriptionTitle: TextView = view.findViewById(R.id.tv_status_description_title)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
        val tvIntruction: TextView = view.findViewById(R.id.tv_instruction)
        val layoutInstruction: View = view.findViewById(R.id.layout_instruction)
        val btnAcecept: Button = view.findViewById(R.id.btn_accept)
    }
}