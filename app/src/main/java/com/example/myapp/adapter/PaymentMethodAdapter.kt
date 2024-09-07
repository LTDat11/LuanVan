package com.example.myapp.adapter

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

class PaymentMethodAdapter(private val paymentMethods: List<PaymentMethod>): RecyclerView.Adapter<PaymentMethodAdapter.PaymentMethodViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentMethodViewHolder {
       val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payment_method, parent, false)
        return PaymentMethodViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentMethodViewHolder, position: Int) {
        val paymentMethod = paymentMethods[position]
        holder.tvPaymentMethodName.text = paymentMethod.name
        holder.tvPaymentMethodDescription.text = paymentMethod.description

        when (paymentMethod.id){
            "1" -> {
                holder.imgPaymentMethod.setImageResource(R.drawable.ic_price)
                holder.imgPaymentMethodIsSelected.setImageResource(R.drawable.ic_item_selected)
            }
            "2" -> {
                holder.imgPaymentMethod.setImageResource(R.drawable.ic_credit)
            }
            "3" -> {
                holder.imgPaymentMethod.setImageResource(R.drawable.ic_gopay)
            }
        }
        holder.layoutItem.setOnClickListener {
            // lấy id của phương thức thanh toán
            val id = paymentMethod.id
            // Truy cập firestore paymentMethods với id tương ứng để tìm trạng thái isAvailable
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("paymentMethods").document(id)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val isAvailable = document.getBoolean("isAvailable")
                        if (isAvailable != null && isAvailable){
                            return@addOnSuccessListener
                        } else {
                            Toast.makeText(holder.itemView.context, "Phương thức thanh toán $id không khả dụng", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(holder.itemView.context, "Không tìm thấy phương thức thanh toán $id", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(holder.itemView.context, "Lỗi khi truy cập phương thức thanh toán $id", Toast.LENGTH_SHORT).show()
                }
        }

    }

    override fun getItemCount(): Int {
        return paymentMethods.size
    }

    class PaymentMethodViewHolder(view: View): RecyclerView.ViewHolder(view){
        val tvPaymentMethodName = view.findViewById<TextView>(R.id.tv_name)
        val tvPaymentMethodDescription = view.findViewById<TextView>(R.id.tv_description)
        val imgPaymentMethodIsSelected = view.findViewById<ImageView>(R.id.img_status)
        val imgPaymentMethod = view.findViewById<ImageView>(R.id.img_payment_method)
        val layoutItem = view.findViewById<View>(R.id.layout_item)
    }
}