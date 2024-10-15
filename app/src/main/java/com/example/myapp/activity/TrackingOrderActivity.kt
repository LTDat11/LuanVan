package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.adapter.PaymentMethodAdapter
import com.example.myapp.adapter.RepairAdapter
import com.example.myapp.databinding.ActivityTrackingOrderBinding
import com.example.myapp.model.Bill
import com.example.myapp.model.Order
import com.example.myapp.model.PaymentMethod
import com.example.myapp.model.Repair
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class TrackingOrderActivity : AppCompatActivity() {
    lateinit var binding: ActivityTrackingOrderBinding
    private var orderId :String = ""
    private var orderListener: ListenerRegistration? = null // Biến lưu trữ ListenerRegistration
    private var selectedPaymentMethodId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackingOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDataIntent()
        initToolbar()
        initUi()
        initListener()
    }

    private fun initListener() {
        binding.tvTakeOrder.setOnClickListener {
            createBillAndUpdateOrder()
        }
    }

    private fun createBillAndUpdateOrder() {
        if (selectedPaymentMethodId.isNullOrEmpty()) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid ?: ""

            // Tạo đối tượng Bill
            val bill = Bill(
                id = db.collection("orders").document(orderId).collection("bills").document().id,
                id_customer = uid,
                id_order = orderId,
                id_paymentMethod = selectedPaymentMethodId, // Sử dụng id của phương thức thanh toán đã chọn
                total = binding.tvTotalPrice.text.toString(),
                createdAt = Date()
            )

            // Lưu Bill vào subcollection bills
            val billRef = db.collection("orders").document(orderId).collection("bills").document(bill.id!!)
            billRef.set(bill).addOnSuccessListener {
                // Cập nhật trạng thái đơn hàng thành 'finish'
                val orderRef = db.collection("orders").document(orderId)
                orderRef.update("status", "finish").addOnSuccessListener {
                    Log.d("TrackingOrderActivity", "Order status updated to 'finish'")
                    binding.layoutBottom.visibility = LinearLayout.GONE
                    finish()
                }.addOnFailureListener { e ->
                    Log.e("TrackingOrderActivity", "Error updating order status", e)
                }
            }.addOnFailureListener { e ->
                Log.e("TrackingOrderActivity", "Error creating bill", e)
            }
        }
    }


    private fun initUi() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("orders").document(orderId)

            listenToOrderUpdates(docRef)
        }
    }

    private suspend fun listenToOrderUpdates(docRef: DocumentReference) {
        withContext(Dispatchers.Main) {
            orderListener = docRef.addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.e("TrackingOrderActivity", "Error listening to document updates", e)
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val order = documentSnapshot.toObject(Order::class.java)
                    if (order != null) {
                        updateUI(order)
                    }
                }
            }
        }
    }

    private fun updateUI(order: Order) {
        binding.apply {
            tvPackageName.text = order.namePackage
            tvPrice.text = order.price.toString()
            tvCreatedAtValue.text = order.createdAt.toString()
            tvUpdatedAtValue.text = order.updatedAt.toString()
            tvDescriptionValue.text = order.description
            tvNoteValue.text = order.notes2?.takeIf { it.isNotEmpty() } ?: "Không có"
            tvAddressValue.text = order.address
            tvPackagePrice.text = order.price.toString()

            //function get name of technician by id_technician and set text to tv_technician_name
            getNameTech(order.id_technician, tvTechnicianName)

            Glide.with(this@TrackingOrderActivity).load(order.imgURLServicePackage).into(imgPackage)

            order.status?.let { updateOrderStatusUI(it) }
        }
    }

    private fun getNameTech(idTechnician: String?, tvTechnicianName: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()
                db.collection("Users")
                    .document(idTechnician ?: return@withContext) // Kiểm tra null cho idTechnician
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            // Xử lý lỗi nếu có
                            tvTechnicianName.text = "Lỗi khi lắng nghe dữ liệu"
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            val name = snapshot.getString("name") // Lấy giá trị name
                            tvTechnicianName.text = name ?: "Không có tên"
                        } else {
                            tvTechnicianName.text = "Chưa có nhân viên xử lý"
                        }
                    }

            }
        }
    }

    private fun updateOrderStatusUI(status: String) {
        binding.apply {
            when (status) {
                "pending" -> {
                    imgStep1.setImageResource(R.drawable.ic_step_enable)
                    imgStep2.setImageResource(R.drawable.ic_step_disable)
                    imgStep3.setImageResource(R.drawable.ic_step_disable)
                    dividerStep1.setBackgroundColor(ContextCompat.getColor(this@TrackingOrderActivity, R.color.green))
                    dividerStep2.setBackgroundColor(ContextCompat.getColor(this@TrackingOrderActivity, R.color.colorAccent))
                    tvTakeOrder.setBackgroundResource(R.drawable.bg_button_disable_corner_16)
                    layoutBill.visibility = TextView.GONE
                }
                "processing" -> {
                    imgStep1.setImageResource(R.drawable.ic_step_enable)
                    imgStep2.setImageResource(R.drawable.ic_step_enable)
                    imgStep3.setImageResource(R.drawable.ic_step_disable)
                    dividerStep1.setBackgroundColor(ContextCompat.getColor(this@TrackingOrderActivity, R.color.green))
                    dividerStep2.setBackgroundColor(ContextCompat.getColor(this@TrackingOrderActivity, R.color.green))
                    tvTakeOrder.setBackgroundResource(R.drawable.bg_button_disable_corner_16)
                    layoutBill.visibility = TextView.GONE
                }
                "completed" -> {
                    imgStep1.setImageResource(R.drawable.ic_step_enable)
                    imgStep2.setImageResource(R.drawable.ic_step_enable)
                    imgStep3.setImageResource(R.drawable.ic_step_enable)
                    dividerStep1.setBackgroundColor(ContextCompat.getColor(this@TrackingOrderActivity, R.color.green))
                    dividerStep2.setBackgroundColor(ContextCompat.getColor(this@TrackingOrderActivity, R.color.green))
                    tvTakeOrder.setBackgroundResource(R.drawable.bg_button_enable_corner_16)
                    layoutBill.visibility = TextView.VISIBLE
                    loadInfoBill()
                    loadPaymentMethods()
                    tvTakeOrder.visibility = TextView.VISIBLE
                    tvWarning.visibility = TextView.VISIBLE
                }
            }
        }
    }

    private fun loadPaymentMethods() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val db = FirebaseFirestore.getInstance()
                val docRef = db.collection("paymentMethods")
                docRef.get().addOnSuccessListener { documents ->
                    val paymentMethods = documents.toObjects(PaymentMethod::class.java)
                    val adapter = PaymentMethodAdapter(paymentMethods) { selectedPaymentMethod ->
                        selectedPaymentMethodId = selectedPaymentMethod.id // Cập nhật id đã chọn
                        Log.d("TrackingOrderActivity", "Selected payment method: ${selectedPaymentMethod.id}")
                    }
                    binding.rcvPaymentMethod.layoutManager = LinearLayoutManager(this@TrackingOrderActivity)
                    binding.rcvPaymentMethod.adapter = adapter
                }
            }
        }
    }

    private fun loadInfoBill() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()
                // lấy name user thông qua uid của user
                val uid = auth.currentUser?.uid ?: ""
                val docRef = db.collection("Users").document(uid)
                docRef.get().addOnSuccessListener { document ->
                    if (document != null) {
                        val name = document.getString("name")
                        binding.apply {
                            tvCustomerName.text = name
                        }
                    }
                }

                // lấy thông tin các document trong subcolection repairs từ colecton orders theo order_id hiển thị lên recyclerview
                val docRefBill = db.collection("orders").document(orderId).collection("repairs")
                docRefBill.get().addOnSuccessListener { documents ->
                    val repairs = documents.toObjects(Repair::class.java)
                    val adapter = RepairAdapter(repairs)
                    binding.recyclerViewRepairedItems.layoutManager = LinearLayoutManager(this@TrackingOrderActivity) // Đặt LayoutManager
                    binding.recyclerViewRepairedItems.adapter = adapter

                    // Lấy thông tin đơn hàng để tính tổng
                    val orderRef = db.collection("orders").document(orderId)
                    orderRef.get().addOnSuccessListener { document ->
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

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = getString(R.string.label_tracking_order)
    }

    private fun getDataIntent() {
        orderId = intent.getStringExtra("order_id") ?: ""
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hủy listener khi activity bị hủy
        orderListener?.remove()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
