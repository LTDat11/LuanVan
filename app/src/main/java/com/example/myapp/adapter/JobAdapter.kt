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
import com.example.myapp.activity.CancelReasonActivity
import com.example.myapp.activity.ReceiptOrderActivity
import com.example.myapp.activity.TrackingOrderActivity
import com.example.myapp.activity.TrackingOrderTechActivity
import com.example.myapp.model.Order

class JobAdapter(private val jobs: List<Order>) : RecyclerView.Adapter<JobAdapter.JobViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]
        holder.tvOrderId.text = job.id
        holder.tvPackageName.text = job.namePackage
        holder.creatAt.text = job.createdAt.toString()
        holder.updateAt.text = job.updatedAt.toString()
        holder.tvTotal.visibility = View.GONE
        holder.tvAction.text = "Thông tin đơn hàng"
        Glide.with(holder.itemView.context).load(job.imgURLServicePackage).into(holder.img_package)

        when (job.status){
            "processing" -> {
                holder.tvStatus.text = "Chờ sửa chữa"
            }
            "completed" -> {
                holder.tvStatus.text = "Đang giao hàng"
            }
            "finish" -> {
                holder.tvStatus.text = "Đã thanh toán"
            }
            "cancel" -> {
                holder.tvStatus.text = "Đã hủy đơn"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_white_corner_6_border_red)
                holder.tvStatus.setTextColor(holder.itemView.resources.getColor(R.color.red))
                holder.tvAction.text = "Xem lý do"
            }
            else -> {
                holder.tvStatus.text = "Không xác định"
            }
        }

        holder.layoutAction.setOnClickListener {
            when (job.status){
                "processing" -> {
                    // Xử lý khi click vào button action
                    val context = holder.itemView.context
                    val intent = Intent(context, TrackingOrderTechActivity::class.java)
                    intent.putExtra("order_id", job.id)
                    intent.putExtra("imgURL", job.imgURLServicePackage)
                    context.startActivity(intent)
                }
                "completed" -> {
                    // Xử lý khi click vào button action
                    val context = holder.itemView.context
                    val intent = Intent(context, TrackingOrderTechActivity::class.java)
                    intent.putExtra("order_id", job.id)
                    intent.putExtra("imgURL", job.imgURLServicePackage)
                    context.startActivity(intent)
                }
                "finish" -> {
                    // Xử lý khi click vào button action
                    val context = holder.itemView.context
                    val intent = Intent(context, TrackingOrderTechActivity::class.java)
                    intent.putExtra("order_id", job.id)
                    intent.putExtra("imgURL", job.imgURLServicePackage)
                    context.startActivity(intent)
                }
                "cancel" -> {
                    val context = holder.itemView.context
                    val intent = Intent(context, CancelReasonActivity::class.java)
                    intent.putExtra("order", job)
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount() = jobs.size

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        val tvPackageName: TextView = itemView.findViewById(R.id.tv_package_name)
        val tvTotal: TextView = itemView.findViewById(R.id.tv_total)
        val creatAt : TextView = itemView.findViewById(R.id.tv_created_at_value)
        val updateAt : TextView = itemView.findViewById(R.id.tv_updated_at_value)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvAction: TextView = itemView.findViewById(R.id.tv_action)
        val img_package: ImageView = itemView.findViewById(R.id.img_package)
        val layoutAction: LinearLayout = itemView.findViewById(R.id.layout_action)
    }
}