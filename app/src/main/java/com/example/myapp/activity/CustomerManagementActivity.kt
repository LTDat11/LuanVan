package com.example.myapp.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.R
import com.example.myapp.adapter.UserManagementAdapter
import com.example.myapp.databinding.ActivityCustomerManagementBinding
import com.example.myapp.model.ApiResponse
import com.example.myapp.model.RetrofitInstance
import com.example.myapp.model.User
import com.example.myapp.model.UserRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class CustomerManagementActivity : AppCompatActivity() {
    lateinit var binding: ActivityCustomerManagementBinding
    private lateinit var userManagementAdapter: UserManagementAdapter
    private var customerList = mutableListOf<User>()
    private var registration: ListenerRegistration? = null
    private val REQUEST_CODE_VOICE_RECOGNITION = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initRecyclerView()
        loadCustomers()
        setupSearchView()
        initListeners()
    }

    private fun initListeners() {
        binding.apply {
            imageButtonMicrophone.setOnClickListener {
                openVoiceRecognizer()
            }
        }
    }

    private fun openVoiceRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói điều gì đó...")
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE_RECOGNITION)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Không hỗ trợ nhận diện giọng nói.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_VOICE_RECOGNITION && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.let {
                if (it.isNotEmpty()) {
                    // Điền kết quả vào SearchView
                    binding.searchView?.setQuery(it[0], false)
                }
            }
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = customerList.filter {
                    it.name?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.address?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.phone?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.email.contains(newText ?: return@filter false, ignoreCase = true)
                }
                userManagementAdapter.updateList(filteredList)
                // Hiển thị hoặc ẩn TextView tv_no_data dựa vào danh sách đã lọc
                if (filteredList.isEmpty()) {
                    binding.tvNoData.visibility = View.VISIBLE
                    binding.rcvViewManagement.visibility = View.GONE
                } else {
                    binding.tvNoData.visibility = View.GONE
                    binding.rcvViewManagement.visibility = View.VISIBLE
                }
                return true
            }
        })
    }

    private fun loadCustomers() {

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()

                // Lắng nghe thay đổi theo thời gian thực
                registration = db.collection("Users")
                    .whereEqualTo("role", "Customer")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w("FireStore", "Listen failed.", e)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            for (documentChange in snapshot.documentChanges) {
                                val customer = documentChange.document.toObject(User::class.java)

                                when (documentChange.type) {
                                    // Khi tài liệu được thêm mới
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                        customerList.add(customer)
                                    }
                                    // Khi tài liệu bị sửa đổi
                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                        val index = customerList.indexOfFirst { it.id == customer.id }
                                        if (index != -1) {
                                            customerList[index] = customer // Cập nhật dữ liệu mới
                                        }
                                    }
                                    // Khi tài liệu bị xóa
                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                        customerList.removeIf { it.id == customer.id }
                                    }
                                }
                            }

                            // Cập nhật lại giao diện
                            userManagementAdapter.notifyDataSetChanged()
                        } else {
                            Log.d("FireStore", "No such documents")
                        }
                    }

            }
        }

    }


    private fun initRecyclerView() {
        userManagementAdapter = UserManagementAdapter(customerList) { customer ->
            // Handle on more button click
            showDialogOption(customer)
        }
        binding.rcvViewManagement.adapter = userManagementAdapter
        binding.rcvViewManagement.layoutManager = LinearLayoutManager(this)
    }

    private fun showDialogOption(customer: User) {
        // Tạo AlertDialog Builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chọn hành động")

        // Tạo danh sách các lựa chọn trong dialog
        val options = arrayOf("Phân quyền admin","Khóa tài khoản", "Xóa tài khoản","Mở khóa tài khoản")

        // Set hành động khi nhấn vào các lựa chọn
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    // Phân quyền admin
                    showGrantAdminConfirmationDialog(customer)
                }
                1 -> {
                    // Khóa tài khoản
                    showDisableUserConfirmationDialog(customer)
                }
                2 -> {
                    // Xóa tài khoản
                    showDeleteUserConfirmationDialog(customer)
                }
                3 -> {
                    // Mở khóa tài khoản
                    showEnableUserConfirmationDialog(customer)
                }
                else -> {
                    dialog.dismiss()
                }
            }
        }

        // Hiển thị dialog
        builder.create().show()
    }

    private fun showEnableUserConfirmationDialog(customer: User) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Xác nhận mở khóa tài khoản")
        builder.setMessage("Bạn có chắc chắn muốn mở khóa tài khoản của khách hàng ${customer.name}?")

        // Set hành động khi nhấn vào nút "Xác nhận"
        builder.setPositiveButton("Xác nhận") { dialog, _ ->
            enableUser(customer.id)  // Gọi hàm mở khóa tài khoản
            dialog.dismiss()
        }

        // Set hành động khi nhấn vào nút "Hủy"
        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.dismiss()
        }

        // Hiển thị dialog
        builder.create().show()
    }

    fun enableUser(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {

            val request = UserRequest(uid)
            RetrofitInstance.api.enableUser(request).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        Log.d("enableUser","enableUser: ${response.body()?.message}")
                        Toast.makeText(this@CustomerManagementActivity, "Đã mở khóa tài khoản", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("enableUser","enableUser: ${response.errorBody()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.d("enableUser","enableUser: ${t.message}")
                }
            })

        }
    }

    private fun showDeleteUserConfirmationDialog(customer: User) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Xác nhận xóa tài khoản")
        builder.setMessage("Bạn có chắc chắn muốn xóa tài khoản của khách hàng ${customer.name}?")

        // Set hành động khi nhấn vào nút "Xác nhận"
        builder.setPositiveButton("Xác nhận") { dialog, _ ->
            disableUser(customer.id)  // Gọi hàm khóa tài khoản
            deleteUser(customer.id)  // Gọi hàm xóa tài khoản
            dialog.dismiss()
        }

        // Set hành động khi nhấn vào nút "Hủy"
        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.dismiss()
        }

        // Hiển thị dialog
        builder.create().show()
    }

    fun deleteUser(uid: String) {
        RetrofitInstance.api.deleteUser(uid).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    deleteCustomerFromFirestore(uid)
                    Log.d("deleteUser", "deleteUser: ${response.body()?.message}")
                    Toast.makeText(this@CustomerManagementActivity, "Đã xóa tài khoản", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("deleteUser", "deleteUser: ${response.errorBody()?.string()}") // In ra nội dung lỗi
                    Log.d("deleteUser", "Response Code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.d("deleteUser", "deleteUser: ${t.message}")
            }
        })
    }

    private fun deleteCustomerFromFirestore(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()

                // Xóa tài khoản khách hàng khỏi Firestore
                db.collection("Users").document(uid)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("deleteUser", "DocumentSnapshot successfully deleted!")
                    }
                    .addOnFailureListener { e ->
                        Log.w("deleteUser", "Error deleting document", e)
                    }

            }
        }
    }

    private fun showDisableUserConfirmationDialog(customer: User) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Xác nhận khóa tài khoản")
        builder.setMessage("Bạn có chắc chắn muốn khóa tài khoản của khách hàng ${customer.name}?")

        // Set hành động khi nhấn vào nút "Xác nhận"
        builder.setPositiveButton("Xác nhận") { dialog, _ ->
            disableUser(customer.id)  // Gọi hàm khóa tài khoản
            dialog.dismiss()
        }

        // Set hành động khi nhấn vào nút "Hủy"
        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.dismiss()
        }

        // Hiển thị dialog
        builder.create().show()
    }

    fun disableUser(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {

            val request = UserRequest(uid)
            RetrofitInstance.api.disableUser(request).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        Log.d("disableUser","disableUser: ${response.body()?.message}")
                        Toast.makeText(this@CustomerManagementActivity, "Đã khóa tài khoản", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("disableUser","disableUser: ${response.errorBody()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.d("disableUser","disableUser: ${t.message}")
                }
            })

        }
    }

    private fun showGrantAdminConfirmationDialog(customer: User) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Xác nhận phân quyền quản trị viên")
        builder.setMessage("Bạn có chắc chắn muốn phân quyền admin cho khách hàng ${customer.name}?")

        // Set hành động khi nhấn vào nút "Xác nhận"
        builder.setPositiveButton("Xác nhận") { dialog, _ ->
            grantAdminRole(customer)  // Gọi hàm phân quyền
            dialog.dismiss()
        }

        // Set hành động khi nhấn vào nút "Hủy"
        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.dismiss()
        }

        // Hiển thị dialog
        builder.create().show()
    }

    private fun grantAdminRole(customer: User) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()

                // Cập nhật role của người dùng thành "Admin"
                val userRef = db.collection("Users").document(customer.id)
                userRef.update("role", "Admin")
                    .addOnSuccessListener {
                        Toast.makeText(this@CustomerManagementActivity, "Đã phân quyền admin thành công", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@CustomerManagementActivity, "Lỗi khi phân quyền admin: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

            }
        }
    }


    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = getString(R.string.cust_management)
    }

}