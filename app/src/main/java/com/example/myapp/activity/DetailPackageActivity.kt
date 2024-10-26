package com.example.myapp.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.databinding.ActivityDetailPackageBinding
import com.example.myapp.model.NotificationRequest
import com.example.myapp.model.Order
import com.example.myapp.model.RetrofitInstance
import com.example.myapp.model.ServicePackage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class DetailPackageActivity : AppCompatActivity() {
    private lateinit var servicePackage: ServicePackage
    private lateinit var binding: ActivityDetailPackageBinding
    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailPackageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDataIntent()
        initToolbar()
        initData()
        initListener()
    }

    private fun initListener() {
        binding.tvAddOrder.setOnClickListener {
            val (notes, notes2, selectedBrand) = getInputData()

            val location = binding.tvSelectLocation.text.toString()

            if (!isValidInput(notes, selectedBrand, location)) {
                Toast.makeText(this, "Vui lòng nhập mô tả, chọn thương hiệu và địa chỉ hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentTime = java.util.Date()

            // Tạo đối tượng Order
            val order = Order(
                id = null,  // ID sẽ được gán sau khi thêm vào Firestore
                id_customer = FirebaseAuth.getInstance().currentUser?.uid,
                id_technician = null,  // Cung cấp giá trị nếu cần
                id_servicepackage = servicePackage.id,  // Cung cấp id dịch vụ nếu cần
                status = "pending",  // Hoặc trạng thái mặc định khác
                createdAt = currentTime , // Thời gian tạo order
                updatedAt = currentTime , // Thời gian cập nhật order
                description = notes,
                notes2 = notes2,
                selectedBrand = selectedBrand,
                imgURLServicePackage = imageUrl,
                namePackage = servicePackage.name,
                price = servicePackage.price,
                address = location
            )

            // Lưu Order vào Firestore
            addOrderToFirestore(order)
        }

        binding.tvSelectLocation.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SELECT_LOCATION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_LOCATION && resultCode == RESULT_OK) {
            val selectedLocation = data?.getStringExtra("selected_location")
            if (selectedLocation != null) {
                // Điền địa chỉ đã chọn vào TextView
                binding.tvSelectLocation.text = selectedLocation
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SELECT_LOCATION = 1
    }

    private fun getInputData(): Triple<String, String, String> {
        val notes = binding.edtNotes.text.toString()
        val notes2 = binding.edtNotes2.text.toString()
        val selectedBrand = binding.spinnerDeviceBrands.selectedItem.toString()
        return Triple(notes, notes2, selectedBrand)
    }

    private fun isValidInput(notes: String, selectedBrand: String, location: String): Boolean {
        return notes.isNotBlank() && selectedBrand != "Chọn thương hiệu" && location != getString(R.string.label_select_location)
    }


    private fun addOrderToFirestore(order: Order) {
        val db = FirebaseFirestore.getInstance()

        // Thêm order vào Firestore orders collection
        val orderRef = db.collection("orders").document() // Tạo document mới và lấy ID
        val orderWithId = order.copy(id = orderRef.id) // Gán ID vào order

        orderRef.set(orderWithId)
            .addOnSuccessListener {
                Toast.makeText(this, "Đặt hàng thành công", Toast.LENGTH_SHORT).show()
                sendNotificationsToAdmins()
                resetinput()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi đặt hàng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendNotificationsToAdmins() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()

            // Lấy tất cả các document trong collection "Admins"
            db.collection("Admins").get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val fcmToken = document.getString("fcmToken") // Lấy fcmToken từ từng document
                        val uid = document.getString("userId") // Lấy userId từ từng document
                        if (!fcmToken.isNullOrEmpty() && !uid.isNullOrEmpty()) {
                            // Gọi hàm sendNotification với từng token
                            sendNotification(fcmToken, "Thông báo", "Bạn có đơn hàng mới được đặt. Vui lòng kiểm tra!!", uid)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("Firestore", "Error getting documents: ", exception)
                }
        }
    }

    private fun sendNotification(token: String, title: String, body: String, userId: String) {
        val notificationRequest = NotificationRequest(token, title, body, userId) // Thêm userId vào yêu cầu

        RetrofitInstance.api.sendNotification(notificationRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Thông báo đã được gửi thành công
                    Log.d("Notification", "Notification sent successfully.")
                } else {
                    // Xử lý khi có lỗi xảy ra
                    Log.e("Notification", "Failed to send notification: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Xử lý khi xảy ra lỗi kết nối
                Log.e("Notification", "Error: ${t.message}")
            }
        })
    }



    private fun resetinput() {
        binding.apply {
            edtNotes.setText("")
            edtNotes2.setText("")
            spinnerDeviceBrands.setSelection(0)
            tvSelectLocation.text = getString(R.string.label_select_location)
        }
    }


    private fun initData() {
        binding.apply {
            tvName.text = servicePackage.name
            tvPrice.text = servicePackage.price
            tvDescription.text = servicePackage.description
            tvTotal.text = servicePackage.price
            Glide.with(this@DetailPackageActivity)
                .load(imageUrl)
                .into(imgPackage)
            // Tải dữ liệu vào spinner
            ArrayAdapter.createFromResource(
                this@DetailPackageActivity,
                R.array.brands,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDeviceBrands.adapter = adapter
                // Đặt giá trị mặc định
                spinnerDeviceBrands.setSelection(0)
            }
        }
    }

    private fun getDataIntent() {
        servicePackage = intent.getSerializableExtra("package") as ServicePackage
        imageUrl = intent.getStringExtra("imageUrl")
    }

    private fun initToolbar() {
        binding.apply {
            val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
            val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
            imgToolbarBack.setOnClickListener { finish() }
            tvToolbarTitle.text = servicePackage.name
        }
    }

}