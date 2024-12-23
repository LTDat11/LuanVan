package com.example.myapp.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.adapter.TechnicainListAdapter
import com.example.myapp.databinding.ActivityAssignTechAdminBinding
import com.example.myapp.model.NotificationRequest
import com.example.myapp.model.Order
import com.example.myapp.model.RetrofitInstance
import com.example.myapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssignTechAdminActivity : AppCompatActivity() {
    lateinit var binding: ActivityAssignTechAdminBinding
    private var order: Order? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var TechnicainListAdapter: TechnicainListAdapter
    private val technicain = mutableListOf<User>()
    private var orderId = ""
    private var techuid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignTechAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDataIntent()
        initToolbar()
        intitUI()
        loadTechnicainsList()
        innitListener()
        //function check use snapshot id_techician in order not null then show layout_bottom
        checkTechInOrder()

    }

    private fun checkTechInOrder() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()
                db.collection("orders").document(orderId)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            // Xử lý lỗi nếu cần
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val idTechnician = snapshot.getString("id_technician") // Truy cập trực tiếp field id_technician
                            if (idTechnician != null) {
                                binding.layoutBottom.visibility = View.VISIBLE
                                techuid = idTechnician
                            } else {
                                binding.layoutBottom.visibility = View.GONE // Ẩn nếu không có kỹ thuật viên
                                techuid = ""
                            }
                        }
                    }
            }
        }
    }

    private fun loadTechnicainsList() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()
                db.collection("Users")
                    .whereEqualTo("role", "Technician")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            // Xử lý lỗi nếu cần
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            technicain.clear()
                            for (document in snapshot.documents) {
                                val tech = document.toObject(User::class.java)
                                if (tech != null) {
                                    technicain.add(tech)
                                }
                            }
                            TechnicainListAdapter.notifyDataSetChanged()
                        }
                    }
            }
        }
    }



    private fun innitListener() {
        binding.apply {
            toggleTechList.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    searchView.visibility = View.VISIBLE
                    rvTechnicians.visibility = View.VISIBLE
                } else {
                    searchView.visibility = View.GONE
                    rvTechnicians.visibility = View.GONE
                }
            }

            tvAddOrder.setOnClickListener {
                // update status order to processing
                CoroutineScope(Dispatchers.IO).launch {
                    val currentTime = java.util.Date()
                    val db = FirebaseFirestore.getInstance()
                    db.collection("orders").document(orderId)
                        .update("status", "processing","updatedAt", currentTime)
                        .addOnSuccessListener {
                            getTokenFCM(techuid)
                            finish()
                        }
                        .addOnFailureListener {

                        }
                }
            }

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
                    Toast.makeText(this@AssignTechAdminActivity, "Google Maps chưa được cài đặt trên thiết bị.", Toast.LENGTH_SHORT).show()
                }
            }

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText != null) {
                        val filteredOrders = technicain.filter { technicain ->
                            technicain.name?.contains(newText, ignoreCase = true) ?: false ||
                                    technicain.description?.contains(newText, ignoreCase = true) ?: false
                        }
                        TechnicainListAdapter = TechnicainListAdapter(filteredOrders, orderId)
                        recyclerView.adapter = TechnicainListAdapter
                    }
                    return true
                }
            })

        }
    }

    private fun getTokenFCM(userId: String){
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            db.collection("Technicians").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val token = document.getString("fcmToken")
                        if (token != null) {
                            sendNotification(token, "Thông báo", "Bạn có đơn hàng mới được phân công. Vui lòng kiểm tra!!", userId)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("AssignTechAdminActivity", "Error getting token: ", exception)
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

    private fun intitUI() {
        binding.apply {
            //set image with glide
            Glide.with(this@AssignTechAdminActivity).load(order?.imgURLServicePackage).into(imgPackage)
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
            // fucntion get info User
            getInfoCustomer()


            recyclerView = rvTechnicians
            recyclerView.layoutManager = LinearLayoutManager(this@AssignTechAdminActivity)
            TechnicainListAdapter = TechnicainListAdapter(technicain, orderId)
            recyclerView.adapter = TechnicainListAdapter

            searchView.visibility = View.GONE
            rvTechnicians.visibility = View.GONE
        }
    }

    private fun getInfoCustomer() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()
                db.collection("Users").document(order?.id_customer.toString())
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val user = document.toObject(User::class.java)
                            binding.apply {
                                tvNameCustomer.text = user?.name
                                tvPhoneCustomer.text = user?.phone
                                tvAddressCustomer.text = user?.address
                            }
                        }
                    }
                    .addOnFailureListener { exception ->

                    }
            }
        }
    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = "Phân công kỹ thuật viên"
    }


    private fun getDataIntent() {
        order = intent.getSerializableExtra("order") as Order
        orderId = order?.id.toString()
    }
}