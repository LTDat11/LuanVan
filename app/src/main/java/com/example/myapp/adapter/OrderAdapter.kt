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
import com.example.myapp.activity.ReceiptOrderActivity
import com.example.myapp.activity.TrackingOrderActivity
import com.example.myapp.model.Order
import com.google.firebase.firestore.FirebaseFirestore

class OrderAdapter(private val orders: List<Order>) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.tvOrderId.text = order.id
        holder.tvPackageName.text = order.namePackage
        holder.creatAt.text = order.createdAt.toString()
        holder.updateAt.text = order.updatedAt.toString()
        holder.tvTotal.text = order.price.toString()
//        holder.tvAction.text = "Theo dõi đơn hàng"
        Glide.with(holder.itemView.context).load(order.imgURLServicePackage).into(holder.img_package) // Hiển thị ảnh nếu cần

        when (order.status){
            "pending" -> {
                holder.tvStatus.text = "Chờ nhận đơn"
                holder.tvAction.text = "Theo dõi đơn hàng"
            }
            "processing" -> {
                holder.tvStatus.text = "Đã nhận đơn"
                holder.tvAction.text = "Theo dõi đơn hàng"
            }
            "completed" -> {
                holder.tvStatus.text = "Đang giao hàng"
                holder.tvAction.text = "Theo dõi đơn hàng"
            }
            "finish" -> {
                holder.tvStatus.text = "Đã thanh toán"
                // Đổi tên tvAction
                holder.tvAction.text = "Xem hóa đơn"
                // Lấy dữ liệu từ collection bills
                val db = FirebaseFirestore.getInstance()
                db.collection("orders")
                    .document(order.id!!)
                    .collection("bills")
                    .get()
                    .addOnSuccessListener {
                        val total = it.documents[0].get("total")
                        holder.tvTotal.text = total.toString()
                    }
            }
            "cancel" -> {
                holder.tvStatus.text = "Đã hủy"
                holder.tvAction.text = "Xem lý do"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_white_corner_6_border_red)
                holder.tvStatus.setTextColor(holder.itemView.context.resources.getColor(R.color.red))
            }
            else -> holder.tvStatus.text = "Không xác định"
        }

        holder.layoutAction.setOnClickListener {
            // kiểm tra trạng thái của layoutAction
            if (order.status == "finish") {
                // Xử lý khi click vào button action
                val context = holder.itemView.context
                val intent = Intent(context, ReceiptOrderActivity::class.java)
                intent.putExtra("order_id", order.id)
                context.startActivity(intent)
            }else {
                // Xử lý khi click vào button action
                val context = holder.itemView.context
                val intent = Intent(context, TrackingOrderActivity::class.java)
                intent.putExtra("order_id", order.id)
                context.startActivity(intent)
            }
        }
    }



    override fun getItemCount() = orders.size

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        val tvPackageName: TextView = itemView.findViewById(R.id.tv_package_name)
        val creatAt : TextView = itemView.findViewById(R.id.tv_created_at_value)
        val updateAt : TextView = itemView.findViewById(R.id.tv_updated_at_value)
        val tvTotal: TextView = itemView.findViewById(R.id.tv_total)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvAction: TextView = itemView.findViewById(R.id.tv_action)
        val img_package: ImageView = itemView.findViewById(R.id.img_package)
        val layoutAction: LinearLayout = itemView.findViewById(R.id.layout_action)
    }
}