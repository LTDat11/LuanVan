package com.example.myapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.model.Order

class OrderAdapter(private val orders: List<Order>) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.tvOrderId.text = order.id
        holder.tvPackageName.text = order.namePackage
        holder.tvTotal.text = order.price.toString()
        holder.tvAction.text = "Theo dõi đơn hàng"
        Glide.with(holder.itemView.context).load(order.imgURLServicePackage).into(holder.img_package) // Hiển thị ảnh nếu cần

        // Hiển thị thêm thông tin nếu cần
        if (order.status == "pending") {
            holder.tvStatus.visibility = View.GONE
        } else {
            holder.tvStatus.visibility = View.VISIBLE
        }

        holder.layoutAction.setOnClickListener {
            // Xử lý khi click vào button action
//            val context = holder.itemView.context
//            val intent = Intent(context, OrderTrackingActivity::class.java)
//            intent.putExtra("order_id", order.id)
//            context.startActivity(intent)
        }
    }

    override fun getItemCount() = orders.size

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        val tvPackageName: TextView = itemView.findViewById(R.id.tv_package_name)
        val tvTotal: TextView = itemView.findViewById(R.id.tv_total)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvAction: TextView = itemView.findViewById(R.id.tv_action)
        val img_package: ImageView = itemView.findViewById(R.id.img_package)
        val layoutAction: LinearLayout = itemView.findViewById(R.id.layout_action)
    }
}