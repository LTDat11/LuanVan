package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.R
import com.example.myapp.adapter.UserManagementAdapter
import com.example.myapp.databinding.ActivityTechManagementBinding
import com.example.myapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TechManagementActivity : AppCompatActivity() {
    lateinit var binding: ActivityTechManagementBinding

    private lateinit var userManagementAdapter: UserManagementAdapter
    private var technicianList = mutableListOf<User>()
    private var registration: ListenerRegistration? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTechManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initRecyclerView()
        loadTechnicians()
        setupSearchView()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = technicianList.filter {
                    it.name?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.description?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.address?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.phone?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.email.contains(newText ?: return@filter false, ignoreCase = true)
                }
                userManagementAdapter.updateList(filteredList)
                return true
            }
        })
    }

    private fun initRecyclerView() {
        userManagementAdapter = UserManagementAdapter(technicianList) { technician ->
            // Handle on more button click
            showDialogOption(technician)
        }
        binding.rcvViewManagement.adapter = userManagementAdapter
        binding.rcvViewManagement.layoutManager = LinearLayoutManager(this)
    }

    private fun showDialogOption(technician: User) {
        // Tạo AlertDialog Builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chọn hành động")

        // Tạo danh sách các lựa chọn trong dialog
        val options = arrayOf("Phân quyền admin")

        // Set hành động khi nhấn vào các lựa chọn
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    showGrantAdminConfirmationDialog(technician)
                }
                else -> {
                    dialog.dismiss()
                }
            }
        }

        // Hiển thị dialog
        builder.create().show()
    }

    private fun showGrantAdminConfirmationDialog(technician: User) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Xác nhận phân quyền quản trị viên")
        builder.setMessage("Bạn có chắc chắn muốn phân quyền admin cho kỹ thuật viên ${technician.name}?")

        // Set hành động khi nhấn vào nút "Xác nhận"
        builder.setPositiveButton("Xác nhận") { dialog, _ ->
            grantAdminRole(technician)  // Gọi hàm phân quyền
            dialog.dismiss()
        }

        // Set hành động khi nhấn vào nút "Hủy"
        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.dismiss()
        }

        // Hiển thị dialog
        builder.create().show()
    }

    private fun grantAdminRole(technician: User) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()

                // Cập nhật role của người dùng thành "Admin"
                val userRef = db.collection("Users").document(technician.id)
                userRef.update("role", "Admin")
                    .addOnSuccessListener {
                        Toast.makeText(this@TechManagementActivity, "Đã phân quyền admin thành công", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@TechManagementActivity, "Lỗi khi phân quyền admin: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

            }
        }

    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = getString(R.string.tech_management)
    }

    private fun loadTechnicians() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val db = FirebaseFirestore.getInstance()
                registration = db.collection("Users")
                    .whereEqualTo("role", "Technician")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w("FireStore", "Listen failed.", e)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            for (documentChange in snapshot.documentChanges) {
                                val technician = documentChange.document.toObject(User::class.java)

                                when (documentChange.type) {
                                    // Khi tài liệu được thêm mới
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                        technicianList.add(technician)
                                    }
                                    // Khi tài liệu bị sửa đổi
                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                        val index = technicianList.indexOfFirst { it.id == technician.id }
                                        if (index != -1) {
                                            technicianList[index] = technician // Cập nhật dữ liệu mới
                                        }
                                    }
                                    // Khi tài liệu bị xóa
                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                        technicianList.removeIf { it.id == technician.id }
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


}