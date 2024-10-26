package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.model.PaymentMethod

class PaymentMethodManagementAdapter(private var paymentMethods: List<PaymentMethod>, private val oneMoreClickListener:(PaymentMethod)-> Unit): RecyclerView.Adapter<PaymentMethodManagementAdapter.ViewHolder>(){
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPaymentMethod: ImageView = view.findViewById(R.id.img_payment_method)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvDescription: TextView = view.findViewById(R.id.tv_description)
        val imgStatus: ImageView = view.findViewById(R.id.img_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_method_management, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paymentMethod = paymentMethods[position]
        holder.tvName.text = paymentMethod.name
        holder.tvDescription.text = paymentMethod.description
        when (paymentMethod.id) {
            "1" -> holder.imgPaymentMethod.setImageResource(R.drawable.ic_price)
            "2" -> holder.imgPaymentMethod.setImageResource(R.drawable.ic_credit)
            "3" -> holder.imgPaymentMethod.setImageResource(R.drawable.ic_zalopay)
        }
        holder.imgStatus.setImageResource(if (paymentMethod.isAvailable) R.drawable.baseline_lock_open_24 else R.drawable.baseline_lock_24)

        holder.imgStatus.setOnClickListener {
            oneMoreClickListener(paymentMethod)
        }
    }

    override fun getItemCount() = paymentMethods.size

    fun updateData(paymentMethods: List<PaymentMethod>){
        this.paymentMethods = paymentMethods
        notifyDataSetChanged()
    }
}
