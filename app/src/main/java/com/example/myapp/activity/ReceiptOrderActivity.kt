package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.R
import com.example.myapp.adapter.RepairAdapter
import com.example.myapp.databinding.ActivityReceiptOrderBinding
import com.example.myapp.databinding.ActivityTrackingOrderBinding
import com.example.myapp.model.Bill
import com.example.myapp.model.Order
import com.example.myapp.model.PaymentMethod
import com.example.myapp.model.Repair
import com.example.myapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReceiptOrderActivity : AppCompatActivity() {
    lateinit var binding: ActivityReceiptOrderBinding
    private var orderId :String = ""
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDataIntent()
        initToolbar()
        initUi()
    }

    private fun initUi() {
        binding.apply {
            tvIdTransaction.text = orderId
            val uid = auth.currentUser?.uid
            // lấy thông tin order từ firestore va techician
            getOrder()
            // lấy thông tin cá nhân khách hàng từ firestore
            getCustomerInfo()
            // lấy danh sách thiết bị sửa chữa từ firestore
            getRepairs()
            // lấy thông tin bill từ firestore
            getReciptOrder()
        }
    }


    private fun getReciptOrder() {
        binding.apply {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    // lấy thông tin bill từ firestore
                    db.collection("orders").document(orderId).collection("bills")
                        .get()
                        .addOnSuccessListener {
                            val bill = it.toObjects(Bill::class.java)
                            tvDateTime.text = bill.get(0).createdAt.toString()
                            tvTotal.text = bill.get(0).total.toString().replace(".", ",").replace("₫", "VND")
                            val id_paymentMethod = bill.get(0).id_paymentMethod.toString()
                            // lấy thông tin phương thức thanh toán từ firestore
                            db.collection("paymentMethods").document(id_paymentMethod)
                                .get()
                                .addOnSuccessListener {
                                    val paymentMethod = it.toObject(PaymentMethod::class.java)
                                    tvPaymentMethod.text = paymentMethod?.name
                                }
                        }
                }
            }
        }
    }


    private fun getRepairs() {
        binding.apply {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    // Hiển thị danh sách thiết bị sửa chữa từ firestore theo orderId của order đó
                    val docRefBill = db.collection("orders").document(orderId).collection("repairs")
                    docRefBill.get().addOnSuccessListener {
                        val repairs = it.toObjects(Repair::class.java)
                        val adapter = RepairAdapter(repairs)
                        recyclerViewRepairedItems.layoutManager = LinearLayoutManager(this@ReceiptOrderActivity) // Đặt LayoutManager
                        recyclerViewRepairedItems.adapter = adapter
                        Log.d("RepairList", "Repairs size: ${repairs.size}")
                    }
                }
            }
        }
    }

    private fun getCustomerInfo() {
        binding.apply {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main){
                    // Lấy id khách hàng từ đơn hàng
                    db.collection("orders").document(orderId)
                        .get()
                        .addOnSuccessListener { orderSnapshot ->
                            val order = orderSnapshot.toObject(Order::class.java)
                            val customerId = order?.id_customer

                            // Lấy thông tin khách hàng từ Firestore theo id_customer
                            if (customerId != null) {
                                db.collection("Users").document(customerId)
                                    .get()
                                    .addOnSuccessListener { userSnapshot ->
                                        val userName = userSnapshot.getString("name")
                                        val userPhone = userSnapshot.getString("phone")
                                        val userAddress = userSnapshot.getString("address")
                                        tvName.text = userName
                                        tvPhone.text = userPhone
                                        tvAddress.text = userAddress
                                    }
                            } else {
                                Log.e("getCustomerInfo", "Customer ID is null")
                            }
                        }
                }
            }
        }
    }


    private fun getOrder() {
        binding.apply {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    // lấy thông tin order từ firestore
                    db.collection("orders").document(orderId)
                        .get()
                        .addOnSuccessListener {
                            val order = it .toObject(Order::class.java)
                            tvNamePackage.text = order?.namePackage
                            val id_technicain = order?.id_technician
                            tvPrice.text = order?.price.toString()
                            tvAddr.text = order?.address

                            // lấy thông tin technician từ firestore
                            db.collection("Users").document(id_technicain!!)
                                .get()
                                .addOnSuccessListener {
                                    val nameTechnicain = it.getString("name")
                                    tvNameTechnicain.text = nameTechnicain
                                }


                        }
                }
            }
        }
    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = getString(R.string.label_receipt_order)
    }

    private fun getDataIntent() {
        orderId = intent.getStringExtra("order_id") ?: ""
    }
}