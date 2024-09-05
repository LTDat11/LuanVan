package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.databinding.ActivityDetailPackageBinding
import com.example.myapp.model.Order
import com.example.myapp.model.ServicePackage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

            if (!isValidInput(notes, selectedBrand)) {
                Toast.makeText(this, "Vui lòng nhập ghi chú và chọn thương hiệu", Toast.LENGTH_SHORT).show()
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
                price = servicePackage.price
            )

            // Lưu Order vào Firestore
            addOrderToFirestore(order)
        }
    }

    private fun getInputData(): Triple<String, String, String> {
        val notes = binding.edtNotes.text.toString()
        val notes2 = binding.edtNotes2.text.toString()
        val selectedBrand = binding.spinnerDeviceBrands.selectedItem.toString()
        return Triple(notes, notes2, selectedBrand)
    }

    private fun isValidInput(notes: String, selectedBrand: String): Boolean {
        return notes.isNotBlank() && selectedBrand != "Chọn thương hiệu"
    }

    private fun addOrderToFirestore(order: Order) {
        val db = FirebaseFirestore.getInstance()

        // Thêm order vào Firestore orders collection
        val orderRef = db.collection("orders").document() // Tạo document mới và lấy ID
        val orderWithId = order.copy(id = orderRef.id) // Gán ID vào order

        orderRef.set(orderWithId)
            .addOnSuccessListener {
                Toast.makeText(this, "Đặt hàng thành công", Toast.LENGTH_SHORT).show()
                resetinput()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi đặt hàng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resetinput() {
        binding.apply {
            edtNotes.setText("")
            edtNotes2.setText("")
            spinnerDeviceBrands.setSelection(0)
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