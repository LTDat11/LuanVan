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
import com.example.myapp.model.Order

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
            }
            "completed" -> {
                holder.tvStatus.text = "Chờ thanh toán"
            }
            "finish" -> {
                holder.tvStatus.text = "Đã thanh toán"
            }
        }

        holder.itemView.setOnClickListener {
            // Xử lý khi click vào item
            val context = holder.itemView.context
            val intent = Intent(context, AssignTechAdminActivity::class.java)
            // truyền dữ liệu order qua màn hình tiếp theo
            intent.putExtra("order", order)
            context.startActivity(intent)
        }

        holder.btnAcecept.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, AssignTechAdminActivity::class.java)
            // truyền dữ liệu order qua màn hình tiếp theo
            intent.putExtra("order", order)
            context.startActivity(intent)
        }

    }

    override fun getItemCount() = orders.size

    class OrderAdminViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(R.id.tv_order_id)
        val tvPackageName: TextView = view.findViewById(R.id.tv_package_name)
        val tvStatusDescription: TextView = view.findViewById(R.id.tv_status_description)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
        val tvIntruction: TextView = view.findViewById(R.id.tv_instruction)
        val btnAcecept: Button = view.findViewById(R.id.btn_accept)
    }
}