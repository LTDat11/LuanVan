package com.example.myapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.model.PaymentMethod
import com.google.api.Context
import com.google.firebase.firestore.FirebaseFirestore

class PaymentMethodAdapter(
    private val paymentMethods: List<PaymentMethod>,
    private var selectedPaymentMethodId: String? = null, // Biến theo dõi phương thức đã chọn
    private val onPaymentMethodSelected: (PaymentMethod) -> Unit // Callback khi chọn phương thức thanh toán
) : RecyclerView.Adapter<PaymentMethodAdapter.PaymentMethodViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentMethodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payment_method, parent, false)
        return PaymentMethodViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentMethodViewHolder, position: Int) {
        val paymentMethod = paymentMethods[position]
        holder.tvPaymentMethodName.text = paymentMethod.name
        holder.tvPaymentMethodDescription.text = paymentMethod.description

        // Set icon cho phương thức thanh toán dựa vào id
        when (paymentMethod.id) {
            "1" -> holder.imgPaymentMethod.setImageResource(R.drawable.ic_price)
            "2" -> holder.imgPaymentMethod.setImageResource(R.drawable.ic_credit)
            "3" -> holder.imgPaymentMethod.setImageResource(R.drawable.ic_gopay)
        }

        // Kiểm tra xem phương thức hiện tại có được chọn không và cập nhật icon trạng thái
        if (paymentMethod.id == selectedPaymentMethodId) {
            holder.imgPaymentMethodIsSelected.setImageResource(R.drawable.ic_item_selected)
        } else {
            holder.imgPaymentMethodIsSelected.setImageResource(R.drawable.ic_item_unselect)
        }

        // Xử lý sự kiện khi nhấn vào phương thức thanh toán
        holder.layoutItem.setOnClickListener {
            // Log the current selectedPaymentMethodId
            Log.d("PaymentMethodAdapter", "Current selectedPaymentMethodId: $selectedPaymentMethodId")

            if (selectedPaymentMethodId == paymentMethod.id) {
                selectedPaymentMethodId = null
                onPaymentMethodSelected(PaymentMethod()) // Pass an empty PaymentMethod object

                // Log after unselecting a payment method
                Log.d("PaymentMethodAdapter", "Payment method unselected. New selectedPaymentMethodId: $selectedPaymentMethodId")
            } else {
                selectedPaymentMethodId = paymentMethod.id
                onPaymentMethodSelected(paymentMethod)

                // Log after selecting a new payment method
                Log.d("PaymentMethodAdapter", "New payment method selected. New selectedPaymentMethodId: $selectedPaymentMethodId")
            }

            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return paymentMethods.size
    }

    class PaymentMethodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPaymentMethodName: TextView = view.findViewById(R.id.tv_name)
        val tvPaymentMethodDescription: TextView = view.findViewById(R.id.tv_description)
        val imgPaymentMethodIsSelected: ImageView = view.findViewById(R.id.img_status)
        val imgPaymentMethod: ImageView = view.findViewById(R.id.img_payment_method)
        val layoutItem: View = view.findViewById(R.id.layout_item)
    }
}
