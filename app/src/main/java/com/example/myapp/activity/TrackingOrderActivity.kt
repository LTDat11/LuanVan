package com.example.myapp.activity

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.myapp.Api.CreateOrder
import com.example.myapp.R
import com.example.myapp.adapter.PaymentMethodAdapter
import com.example.myapp.adapter.RepairAdapter
import com.example.myapp.databinding.ActivityTrackingOrderBinding
import com.example.myapp.model.Bill
import com.example.myapp.model.NotificationRequest
import com.example.myapp.model.Order
import com.example.myapp.model.PaymentMethod
import com.example.myapp.model.Repair
import com.example.myapp.model.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import vn.zalopay.sdk.Environment
import vn.zalopay.sdk.ZaloPayError
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener
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

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // ZaloPay SDK Init

        // ZaloPay SDK Init
        ZaloPaySDK.init(553, Environment.SANDBOX)

        getDataIntent()
        initToolbar()
        initUi()
        initListener()
    }

    private fun initListener() {
        binding.tvTakeOrder.setOnClickListener {
            //createBillAndUpdateOrder()
            if (selectedPaymentMethodId.isNullOrEmpty()) {
                Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (selectedPaymentMethodId == "3") {
                payWithZaloPay()
            } else {
                createBillAndUpdateOrder()
                sendNotificationsToAdmins()
            }
        }

        binding.btnCancelOrder.setOnClickListener {
            showDialogCancelOrder()
        }
    }

    private fun showDialogCancelOrder() {
        AlertDialog.Builder(this)
            .setTitle("Hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này?")
            .setPositiveButton("Có") { dialog, _ ->
                cancelOrder()
                dialog.dismiss()
            }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun cancelOrder() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                // delete order with orderId
                val db = FirebaseFirestore.getInstance()
                db.collection("orders").document(orderId).delete().addOnSuccessListener {
                    Log.d("TrackingOrderActivity", "Order deleted successfully")
                    finish()
                }.addOnFailureListener { e ->
                    Log.e("TrackingOrderActivity", "Error deleting order", e)
                }

            }
        }
    }

    private fun payWithZaloPay() {
        val totalPriceText = binding.tvTotalPrice.text.toString().replace(",", "").replace("VND", "").trim()
        val total = totalPriceText.toDoubleOrNull() ?: 0.0
        val totalString = String.format("%.0f", total)
        // Lấy tên gói dịch vụ và mô tả
        val packageName = binding.tvPackageName.text.toString()
        val description = binding.tvDescriptionValue.text.toString()
        val orderApi = CreateOrder()

        try {
            val data = orderApi.createOrder(totalString, packageName, description)
            val code = data.getString("returncode")

            if (code == "1") {
                val token: String = data.getString("zptranstoken")
                ZaloPaySDK.getInstance().payOrder(this, token, "demozpdk://app", object : PayOrderListener {
                    override fun onPaymentSucceeded(transactionId: String, transToken: String, appTransID: String) {
//                        runOnUiThread {
//                            AlertDialog.Builder(this@TrackingOrderActivity)
//                                .setTitle("Payment Success")
//                                .setMessage("TransactionId: $transactionId - TransToken: $transToken")
//                                .setPositiveButton("OK") { dialog, _ ->
//                                    dialog.dismiss()
//                                }
//                                .setNegativeButton("Cancel", null)
//                                .show()
//                        }
                        createBillAndUpdateOrder()
                        sendNotificationsToAdmins()
                    }

                    override fun onPaymentCanceled(zpTransToken: String, appTransID: String) {
                        AlertDialog.Builder(this@TrackingOrderActivity)
                            .setTitle("Hủy thanh toán")
                            .setMessage("Thanh toán đã bị hủy bởi người dùng")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }

                    override fun onPaymentError(zaloPayError: ZaloPayError, zpTransToken: String, appTransID: String) {
                        AlertDialog.Builder(this@TrackingOrderActivity)
                            .setTitle("Lỗi thanh toán")
                            .setMessage("ZaloPayErrorCode: ${zaloPayError.toString()} \nTransToken: $zpTransToken")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                })

            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                            sendNotification(fcmToken, "Thông báo thanh toán", "Bạn có đơn hàng vừa mới thanh toán. Vui lòng kiểm tra!!", uid)
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        ZaloPaySDK.getInstance().onResult(intent)
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
                // Cập nhật trạng thái đơn hàng thành 'finish' và cập nhật updatedAt
                val orderRef = db.collection("orders").document(orderId)
                orderRef.update("status", "finish","updatedAt",Date()).addOnSuccessListener {
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
                    btnCancelOrder.visibility = TextView.VISIBLE
                    layoutBill.visibility = TextView.GONE
                    tvTakeOrder.visibility = TextView.GONE
                    tvWarning.visibility = TextView.GONE
                }
                "processing" -> {
                    imgStep1.setImageResource(R.drawable.ic_step_enable)
                    imgStep2.setImageResource(R.drawable.ic_step_enable)
                    imgStep3.setImageResource(R.drawable.ic_step_disable)
                    dividerStep1.setBackgroundColor(ContextCompat.getColor(this@TrackingOrderActivity, R.color.green))
                    dividerStep2.setBackgroundColor(ContextCompat.getColor(this@TrackingOrderActivity, R.color.green))
                    tvTakeOrder.setBackgroundResource(R.drawable.bg_button_disable_corner_16)
                    loadInfoBill()
                    btnCancelOrder.visibility = TextView.GONE
                    layoutBill.visibility = TextView.VISIBLE
                    rcvPaymentMethod.visibility = TextView.GONE
                    tvTakeOrder.visibility = TextView.GONE
                    tvWarning.visibility = TextView.GONE
                }
                "completed" -> {
                    imgStep1.setImageResource(R.drawable.ic_step_enable)
                    imgStep2.setImageResource(R.drawable.ic_step_enable)
                    imgStep3.setImageResource(R.drawable.ic_step_enable)
                    dividerStep1.setBackgroundColor(ContextCompat.getColor(this@TrackingOrderActivity, R.color.green))
                    dividerStep2.setBackgroundColor(ContextCompat.getColor(this@TrackingOrderActivity, R.color.green))
                    tvTakeOrder.setBackgroundResource(R.drawable.bg_button_enable_corner_16)
                    loadInfoBill()
                    loadPaymentMethods()
                    btnCancelOrder.visibility = TextView.GONE
                    layoutBill.visibility = TextView.VISIBLE
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
            withContext(Dispatchers.Main) {
                val db = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()
                // Lấy UID của user hiện tại
                val uid = auth.currentUser?.uid ?: ""
                val docRef = db.collection("Users").document(uid)

                // Lắng nghe thay đổi thông tin user bằng snapshotListener
                docRef.addSnapshotListener { document, e ->
                    if (e != null || document == null) return@addSnapshotListener
                    val name = document.getString("name")
                    binding.apply {
                        tvCustomerName.text = name
                    }
                }

                // Lấy thông tin các document trong subcollection "repairs" của "orders" theo order_id
                val docRefBill = db.collection("orders").document(orderId).collection("repairs")
                docRefBill.addSnapshotListener { documents, e ->
                    if (e != null || documents == null) return@addSnapshotListener
                    val repairs = documents.toObjects(Repair::class.java)
                    val adapter = RepairAdapter(repairs)
                    binding.recyclerViewRepairedItems.layoutManager = LinearLayoutManager(this@TrackingOrderActivity)
                    binding.recyclerViewRepairedItems.adapter = adapter

                    // Lắng nghe thông tin đơn hàng để tính tổng
                    val orderRef = db.collection("orders").document(orderId)
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