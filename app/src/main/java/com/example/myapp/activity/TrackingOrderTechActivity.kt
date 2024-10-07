package com.example.myapp.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.adapter.RepairTechAdapter
import com.example.myapp.databinding.ActivityTrackingOrderTechBinding
import com.example.myapp.model.Order
import com.example.myapp.model.Repair
import com.example.myapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class TrackingOrderTechActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingOrderTechBinding
    private var orderId: String = ""
    private var imgURL : String = ""
    private var idCustomer: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackingOrderTechBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getStringExtra("order_id").toString()
        imgURL = intent.getStringExtra("imgURL").toString()

        initToolbar()
        fetchOrderData()
        fetchRepairs()
        initListeners()
    }

    private fun fetchInfoCustomer() {
        val db = FirebaseFirestore.getInstance()
        val customerRef = db.collection("Users").document(idCustomer)

        // Lấy thông tin của khách hàng từ Firestore
        customerRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // Parse dữ liệu của Order
                val customer = documentSnapshot.toObject(User::class.java)
                customer?.let {
                    // Hiển thị dữ liệu lên giao diện
                    binding.tvNameCustomerValue.text = customer.name
                    binding.tvPhoneCustomerValue.text = customer.phone
                    binding.tvAddressCustomerValue.text = customer.address
                }
            } else {
                Log.d("TrackingOrderTech", "Customer document không tồn tại.")
            }
        }.addOnFailureListener { exception ->
            Log.e("TrackingOrderTech", "Lỗi khi lấy dữ liệu khách hàng", exception)
        }
    }

    private fun checkStatusOrder() {
        val db = FirebaseFirestore.getInstance()
        val orderRef = db.collection("orders").document(orderId)
        orderRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val status = document.getString("status")
                    if (status == "completed" || status == "finish") {
                        binding.layoutBottom.visibility = View.GONE
                        binding.fabAddDeviceRepairs.visibility = View.GONE
                    }
                }
            }
    }

    private fun fetchRepairs() {
        val db = FirebaseFirestore.getInstance()
        val repairsRef = db.collection("orders").document(orderId).collection("repairs").whereEqualTo("id_order", orderId)

        // Lắng nghe sự thay đổi dữ liệu theo thời gian thực
        repairsRef.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                Log.e("TrackingOrderTech", "Lỗi khi lắng nghe danh sách thiết bị sửa chữa", exception)
                return@addSnapshotListener
            }

            if (querySnapshot != null && !querySnapshot.isEmpty) {
                val repairs = querySnapshot.toObjects(Repair::class.java)

                // Khởi tạo adapter và set cho RecyclerView
                val adapter = RepairTechAdapter(repairs)
                binding.recyclerViewRepairedItems.layoutManager = LinearLayoutManager(this)
                binding.recyclerViewRepairedItems.adapter = adapter
            } else {
                Log.d("TrackingOrderTech", "Không có thiết bị sửa chữa nào.")
            }
        }
    }



    private fun fetchOrderData() {
        val db = FirebaseFirestore.getInstance()
        val orderRef = db.collection("orders").document(orderId)

        // Lấy thông tin của đơn hàng từ Firestore
        orderRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // Parse dữ liệu của Order
                val order = documentSnapshot.toObject(Order::class.java)
                order?.let {
                    // Hiển thị dữ liệu lên giao diện
                    displayOrderData(it)
                    idCustomer = order.id_customer.toString()
                    fetchInfoCustomer()
                }
            } else {
                Log.d("TrackingOrderTech", "Order document không tồn tại.")
            }
        }.addOnFailureListener { exception ->
            Log.e("TrackingOrderTech", "Lỗi khi lấy dữ liệu đơn hàng", exception)
        }
    }

    private fun displayOrderData(order: Order) {
        binding.apply {
            // Hiển thị dữ liệu lên các TextView
            tvPackageName.text = order.namePackage
            tvCreatedAtValue.text = order.createdAt.toString()
            tvUpdatedAtValue.text = order.updatedAt.toString()
            tvDescriptionValue.text = order.description
            tvNoteValue.text = order.notes2
            tvAddressValue.text = order.address
            Glide.with(this@TrackingOrderTechActivity).load(imgURL).into(imgPackage)
        }
    }





    private fun initListeners() {
        binding.apply {
            checkboxComplete.setOnCheckedChangeListener { _, isChecked ->
                tvDone.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            tvDone.setOnClickListener {
                updateStatus()
            }

            fabAddDeviceRepairs.setOnClickListener {
                showDialogAdd()
            }

            layoutAddressGoogleMap.setOnClickListener {
                // Lấy địa chỉ từ TextView
                val address = binding.tvAddressValue.text.toString()

                // Tạo Uri để mở Google Maps và chỉ định đường đi đến địa chỉ cụ thể
                val uri = Uri.parse("google.navigation:q=$address")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")

                // Kiểm tra xem có ứng dụng Google Maps hay không trước khi khởi chạy
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    // Xử lý trường hợp khi không tìm thấy ứng dụng Google Maps
                    Toast.makeText(this@TrackingOrderTechActivity, "Google Maps chưa được cài đặt trên thiết bị.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateStatus() {
        val currentTime = java.util.Date()
        val db = FirebaseFirestore.getInstance()
        val orderRef = db.collection("orders").document(orderId)


        // Cập nhật trạng thái đơn hàng
        orderRef.update("status", "completed", "updatedAt", currentTime)
            .addOnSuccessListener {
                // Gửi thông báo cho khách hàng
                Toast.makeText(this@TrackingOrderTechActivity, "Đã hoàn thành đơn hàng", Toast.LENGTH_SHORT).show()
                binding.layoutBottom.visibility = View.GONE
                binding.fabAddDeviceRepairs.visibility = View.GONE
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("TrackingOrderTech", "Lỗi khi cập nhật trạng thái đơn hàng", e)
                Toast.makeText(this@TrackingOrderTechActivity, "Có lỗi xảy ra, vui lòng thử lại sau", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDialogAdd() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_device_repairted, null)
        val etDeviceName = dialogView.findViewById<EditText>(R.id.et_device_name)
        val etDevicePrice = dialogView.findViewById<EditText>(R.id.et_device_price)

        val dialog = AlertDialog.Builder(this@TrackingOrderTechActivity)
            .setTitle("Thêm giá và tên thiết bị sửa chữa/thay thế")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val deviceName = etDeviceName.text.toString()
                val devicePrice = etDevicePrice.text.toString()

                if (deviceName.isNotEmpty() && devicePrice.isNotEmpty()) {
                    // Tạo một đối tượng Repair mà chưa có id
                    val db = FirebaseFirestore.getInstance()
                    val repairsCollection = db.collection("orders").document(orderId).collection("repairs")

                    // Thêm vào Firestore và lấy ID của document tự động tạo
                    repairsCollection.add(
                        Repair(
                            id = null,  // Firestore sẽ tự động tạo ID
                            id_order = orderId,
                            name = deviceName,
                            price = formatPrice(devicePrice)
                        )
                    ).addOnSuccessListener { documentReference ->
                        // Cập nhật ID sau khi thêm thành công
                        val repairWithId = Repair(
                            id = documentReference.id,
                            id_order = orderId,
                            name = deviceName,
                            price = formatPrice(devicePrice)
                        )

                        // Cập nhật ID cho document vừa tạo
                        documentReference.set(repairWithId)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Thêm thiết bị thành công", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("TrackingOrderTech", "Lỗi khi cập nhật ID cho thiết bị", e)
                            }

                    }.addOnFailureListener { exception ->
                        Log.e("TrackingOrderTech", "Lỗi khi thêm thiết bị", exception)
                        Toast.makeText(this, "Có lỗi xảy ra, vui lòng thử lại sau", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Vui lòng nhập tên và giá thiết bị", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .create()

        dialog.show()
    }


    private fun formatPrice(price: String): String {
        // Chuyển đổi chuỗi sang kiểu số nguyên
        val priceLong = price.toLongOrNull() ?: return "0 VND"

        // Sử dụng NumberFormat để định dạng theo locale Việt Nam
        val numberFormat = NumberFormat.getNumberInstance(Locale.US)
        val formattedPrice = numberFormat.format(priceLong)

        // Trả về chuỗi đã định dạng kèm với đơn vị tiền tệ VND
        return "$formattedPrice VND"
    }


    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = getString(R.string.label_tracking_order)
        checkStatusOrder()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
