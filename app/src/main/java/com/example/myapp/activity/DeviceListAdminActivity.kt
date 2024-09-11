package com.example.myapp.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.R
import com.example.myapp.adapter.DeviceListAdminAdapter
import com.example.myapp.databinding.ActivityDeviceListAdminBinding
import com.example.myapp.model.Device
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class DeviceListAdminActivity : AppCompatActivity() {
    lateinit var binding: ActivityDeviceListAdminBinding
    private var categoryId :String = ""
    private var categoryName :String = ""

    private var imageUri: Uri? = null // Khai báo biến imageUri
    private val PICK_IMAGE_REQUEST = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceListAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDataIntent()
        initToolbar()

        loadDevices()

        setupSearchView()

        initListeners()
    }

    private fun initListeners() {
        binding.fabAdd.setOnClickListener {
            showAddDeviceDialog()
        }
    }

    private fun showAddDeviceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        val etDeviceName = dialogView.findViewById<EditText>(R.id.et_device_name)
        val tvAddImg = dialogView.findViewById<TextView>(R.id.tv_add_img)
        val ivDeviceImage = dialogView.findViewById<ImageView>(R.id.iv_device_image)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Thêm thiết bị mới")
            .setPositiveButton("Thêm") { _, _ ->
                val name = etDeviceName.text.toString()
                if (name.isNotEmpty()) {
                    uploadImageAndAddDevice(name)
                } else {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin và chọn ảnh", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .create()


        dialog.show()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            // Cập nhật ImageView trong dialog nếu dialog đang mở
            // Truy cập đến ImageView trong dialog
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
            val ivDeviceImage = dialogView.findViewById<ImageView>(R.id.iv_device_image)
            ivDeviceImage.setImageURI(imageUri)
        }
    }

    private fun uploadImageAndAddDevice(name: String) {
//        val storage = FirebaseStorage.getInstance()
//        val ref = storage.reference.child("device/${name}/${name}.jpg")
//        imageUri?.let {
//            ref.putFile(it)
//                .addOnSuccessListener {
//                    ref.downloadUrl.addOnSuccessListener { uri ->
//                        addDeviceToFirestore(name)
//                    }
//                }
//                .addOnFailureListener {
//                    Toast.makeText(this, "Lỗi tải ảnh", Toast.LENGTH_SHORT).show()
//                }
//        }

        addDeviceToFirestore(name)
    }

    private fun addDeviceToFirestore(name: String) {
        val firestore = FirebaseFirestore.getInstance()
        val categoryDocRef = firestore.collection("service_categories").document(categoryId)

        // Tạo một document reference mới để lấy ID
        val newDeviceDocRef = categoryDocRef.collection("devices").document()

        // Tạo một Device object với id_device trùng với ID của document
        val newDevice = Device(
            id_device = newDeviceDocRef.id,
            name = name,
            categoryId = categoryId
        )

        // Thêm document vào Firestore với ID đã tạo
        newDeviceDocRef.set(newDevice)
            .addOnSuccessListener {
                Toast.makeText(this, "Thiết bị đã được thêm thành công", Toast.LENGTH_SHORT).show()
                loadDevices()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi thêm thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredText = newText ?: ""
                val adapter = binding.recyclerViewCategory.adapter as DeviceListAdminAdapter
                adapter.filterDevices(filteredText)
                return true
            }
        })
    }

    private fun loadDevices() {
        val deviceList = mutableListOf<Device>()
        val firestore = FirebaseFirestore.getInstance()

        // Lấy danh sách devices từ Firestore dựa trên categoryId
        firestore.collection("service_categories")
            .document(categoryId)
            .collection("devices")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val device = document.toObject(Device::class.java)
                        if (device != null) {
                            deviceList.add(device)
                        }
                    }
                    setupRecyclerView(deviceList)
                } else {
                    Toast.makeText(this, "Chưa có thiết bị nào", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi lấy thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView(deviceList: List<Device>) {
        val adapter = DeviceListAdminAdapter(
            deviceList,
            onEditClick = { device ->
                // Xử lý sự kiện sửa thiết bị
                //editDevice(device)
            },
            onDeleteClick = { device ->
                // Xử lý sự kiện xóa thiết bị
                //deleteDevice(device)
            }
        )

        binding.recyclerViewCategory.adapter = adapter
        binding.recyclerViewCategory.layoutManager = LinearLayoutManager(this)
    }


    private fun getDataIntent() {
        categoryId = intent.getStringExtra("categoryId").toString()
        categoryName = intent.getStringExtra("categoryName").toString()
    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        // set title toolbar with category name
        tvToolbarTitle.text = "Danh sách thiết bị của $categoryName"
    }
}