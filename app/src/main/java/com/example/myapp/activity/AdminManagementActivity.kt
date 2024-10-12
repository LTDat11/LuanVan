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
import com.example.myapp.databinding.ActivityAdminManagementBinding
import com.example.myapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AdminManagementActivity : AppCompatActivity() {
    lateinit var binding: ActivityAdminManagementBinding
    private lateinit var userManagementAdapter: UserManagementAdapter
    private var adminList = mutableListOf<User>()
    private var registration: ListenerRegistration? = null
    private val REQUEST_CODE_VOICE_RECOGNITION = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initRecyclerView()
        loadAdmins()
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
                val filteredList = adminList.filter {
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

    private fun loadAdmins() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val db = FirebaseFirestore.getInstance()

                // Lắng nghe thay đổi theo thời gian thực
                registration = db.collection("Users")
                    .whereEqualTo("role", "Admin")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w("FireStore", "Listen failed.", e)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            for (documentChange in snapshot.documentChanges) {
                                val admin = documentChange.document.toObject(User::class.java)
                                if (admin.id == currentUserId) {
                                    continue // Loại trừ tài khoản admin hiện tại
                                }

                                when (documentChange.type) {
                                    // Khi tài liệu được thêm mới
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                        adminList.add(admin)
                                    }
                                    // Khi tài liệu bị sửa đổi
                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                        val index = adminList.indexOfFirst { it.id == admin.id }
                                        if (index != -1) {
                                            adminList[index] = admin // Cập nhật dữ liệu mới
                                        }
                                    }
                                    // Khi tài liệu bị xóa
                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                        adminList.removeIf { it.id == admin.id }
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
        userManagementAdapter = UserManagementAdapter(adminList) { admin ->
            // Handle on more button click
            showDialogOption(admin)
        }
        binding.rcvViewManagement.adapter = userManagementAdapter
        binding.rcvViewManagement.layoutManager = LinearLayoutManager(this)
    }

    private fun showDialogOption(admin: User) {
        // Tạo AlertDialog Builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chọn hành động")

        // Tạo danh sách các lựa chọn trong dialog
        val options = arrayOf("Phân quyền kỹ thuật viên", "Phân quyền khách hàng")

        // Set hành động khi nhấn vào các lựa chọn
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    // Phân quyền kỹ thuật viên
                    showDialogGrantTechnicianConfirmation(admin)
                }
                1 -> {
                    // Phân quyền khách hàng
                    showDialogGrantCustomerConfirmation(admin)
                }
            }
        }

        // Hiển thị dialog
        builder.create().show()
    }

    private fun showDialogGrantCustomerConfirmation(admin: User) {
        // Tạo AlertDialog Builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Xác nhận phân quyền khách hàng")
        builder.setMessage("Bạn có chắc chắn muốn phân quyền khách hàng cho ${admin.name}?")

        // Set hành động khi nhấn vào các lựa chọn
        builder.setPositiveButton("Đồng ý") { dialog, which ->
            // Phân quyền khách hàng
            grantCustomerRole(admin)
        }
        builder.setNegativeButton("Hủy") { dialog, which ->
            dialog.dismiss()
        }

        // Hiển thị dialog
        builder.create().show()
    }

    private fun grantCustomerRole(admin: User) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()
                // Cập nhật role của người dùng thành "Customer"
                val userRef = db.collection("Users").document(admin.id)
                userRef.update("role", "Customer")
                    .addOnSuccessListener {
                        Toast.makeText(this@AdminManagementActivity, "Đã phân quyền khách hàng thành công", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@AdminManagementActivity, "Lỗi khi phân quyền khách hàng: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

            }
        }
    }

    private fun showDialogGrantTechnicianConfirmation(admin: User) {
        // Tạo AlertDialog Builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Xác nhận phân quyền kỹ thuật viên")
        builder.setMessage("Bạn có chắc chắn muốn phân quyền kỹ thuật viên cho ${admin.name}?")

        // Set hành động khi nhấn vào các lựa chọn
        builder.setPositiveButton("Đồng ý") { dialog, which ->
            // Phân quyền kỹ thuật viên
            grantTechnicianRole(admin)
        }
        builder.setNegativeButton("Hủy") { dialog, which ->
            dialog.dismiss()
        }

        // Hiển thị dialog
        builder.create().show()
    }

    private fun grantTechnicianRole(admin: User) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()
                // Cập nhật role của người dùng thành "Admin"
                val userRef = db.collection("Users").document(admin.id)
                userRef.update("role", "Technician")
                    .addOnSuccessListener {
                        Toast.makeText(this@AdminManagementActivity, "Đã phân quyền admin thành công", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@AdminManagementActivity, "Lỗi khi phân quyền admin: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

            }
        }

    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = getString(R.string.admin_management)
    }

}