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
import com.example.myapp.databinding.ActivityTechManagementBinding
import com.example.myapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TechManagementActivity : AppCompatActivity() {
    lateinit var binding: ActivityTechManagementBinding

    private lateinit var userManagementAdapter: UserManagementAdapter
    private var technicianList = mutableListOf<User>()
    private var registration: ListenerRegistration? = null
    private val REQUEST_CODE_VOICE_RECOGNITION = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTechManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initRecyclerView()
        loadTechnicians()
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
                val filteredList = technicianList.filter {
                    it.name?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.description?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
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