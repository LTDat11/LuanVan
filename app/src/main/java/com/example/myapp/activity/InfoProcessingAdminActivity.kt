package com.example.myapp.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.adapter.RepairAdapter
import com.example.myapp.databinding.ActivityInfoProcessingAdminBinding
import com.example.myapp.model.Order
import com.example.myapp.model.Repair
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class InfoProcessingAdminActivity : AppCompatActivity() {
    lateinit var binding: ActivityInfoProcessingAdminBinding
    private var order: Order? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoProcessingAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDataIntent()
        initToolbar()
        intitUI()
        initListener()
    }

    private fun initListener() {
        binding.apply {
            layoutAddressGoogleMap.setOnClickListener{
                // Lấy địa chỉ từ TextView
                val address = binding.tvAddress.text.toString()

                // Tạo Uri để mở Google Maps và chỉ định đường đi đến địa chỉ cụ thể
                val uri = Uri.parse("google.navigation:q=$address")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")

                // Kiểm tra xem có ứng dụng Google Maps hay không trước khi khởi chạy
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    // Xử lý trường hợp khi không tìm thấy ứng dụng Google Maps
                    Toast.makeText(this@InfoProcessingAdminActivity, "Google Maps chưa được cài đặt trên thiết bị.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun intitUI() {
        binding.apply {
            //set image with glide
            Glide.with(this@InfoProcessingAdminActivity).load(order?.imgURLServicePackage).into(imgPackage)
            //set text for text view
            tvName.text = order?.namePackage
            tvPrice.text = order?.price.toString()
            tvDescription.text = order?.description
            if (order?.notes2.isNullOrEmpty()){
                tvNote.text = "Không có"
            }
            tvNameBrand.text = order?.selectedBrand
            tvCreatedAt.text = order?.createdAt.toString()
            tvAddress.text = order?.address
            getInfoCustomer()

            getInfoTechnicain(order?.id_technician)
            loadInfoBill()
        }
    }

    private fun loadInfoBill() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {

                val db = FirebaseFirestore.getInstance()
                // Lấy thông tin các document trong subcollection "repairs" của "orders" theo order_id
                val docRefBill = db.collection("orders").document(order?.id.toString()).collection("repairs")
                docRefBill.addSnapshotListener { documents, e ->
                    if (e != null || documents == null) return@addSnapshotListener
                    val repairs = documents.toObjects(Repair::class.java)
                    val adapter = RepairAdapter(repairs)
                    binding.recyclerViewRepairedItems.layoutManager = LinearLayoutManager(this@InfoProcessingAdminActivity)
                    binding.recyclerViewRepairedItems.adapter = adapter

                    // Lắng nghe thông tin đơn hàng để tính tổng
                    val orderRef = db.collection("orders").document(order?.id.toString())
                    orderRef.addSnapshotListener { document, e ->
                        if (e != null || document == null) return@addSnapshotListener
                        val order = document.toObject(Order::class.java)
                        if (order != null) {
                            updateTotalPrice(order, repairs)
                        }
                    }
                }
            }
        }
    }

    private fun formatPrice(price: String?): String {
        // Kiểm tra nếu giá không phải là null
        price?.let {
            // Loại bỏ ký tự không cần thiết
            val cleanedPrice = it.replace(",", "")?.replace(" VND", "")
            return cleanedPrice.toString()
        }
        return "0" // Nếu giá là null, trả về "0"
    }

    private fun updateTotalPrice(order: Order, repairs: List<Repair>) {
        // Tính tổng tiền của các món sửa chữa
        var totalAmount = 0

        for (repair in repairs) {
            val cleanedPrice = formatPrice(repair.price)
            totalAmount += cleanedPrice.toIntOrNull() ?: 0
        }

        // Cộng với giá gói
        val packagePrice = formatPrice(order.price)
        totalAmount += packagePrice.toIntOrNull() ?: 0

        // Định dạng số tiền theo đơn vị tiền tệ Việt Nam
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        val formattedTotal = numberFormat.format(totalAmount).replace("₫", "VND").replace(".", ",")

        binding.tvTotalPrice.text = formattedTotal
    }

    private fun getInfoCustomer() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()
                // use snapshot to get name and phone of user by id user
                db.collection("Users").document(order?.id_customer!!)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val name = document.getString("name")
                            val phone = document.getString("phone")
                            val address = document.getString("address")
                            binding.apply {
                                tvNameCustomer.text = name
                                tvPhoneCustomer.text = phone
                                tvAddressCustomer.text = address
                            }
                        }
                    }
            }
        }
    }

    private fun getInfoTechnicain(idTechnician: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()
                // use snapshot to get name and description of technician by id technician
                db.collection("Users").document(idTechnician!!)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val name = document.getString("name")
                            val description = document.getString("description")
                            val imageURL = document.getString("imageURL")

                            binding.apply {
                                technicainName.text = name
                                technicainDescription.text = description
                                Glide.with(this@InfoProcessingAdminActivity).load(imageURL).into(technicainImage)
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
        tvToolbarTitle.text = "Thông tin đơn hàng đang xử lý"
    }

    private fun getDataIntent() {
        order = intent.getSerializableExtra("order") as Order
    }
}