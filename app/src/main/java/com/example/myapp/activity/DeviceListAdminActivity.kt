package com.example.myapp.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.adapter.DeviceListAdminAdapter
import com.example.myapp.databinding.ActivityDeviceListAdminBinding
import com.example.myapp.model.Device
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class DeviceListAdminActivity : AppCompatActivity() {
    lateinit var binding: ActivityDeviceListAdminBinding
    private var categoryId :String = ""
    private var categoryName :String = ""

    private var imageUri: Uri? = null // Khai báo biến imageUri
    private var isImageSelected = false // Biến kiểm tra xem đã chọn ảnh hay chưa
    private val PICK_IMAGE_REQUEST = 123
    private var data: Intent? = null

    private lateinit var dialogView: View
    private lateinit var dialog: AlertDialog

    // Khởi tạo mảng lưu các idDevice
    private val deviceIdsToDelete = mutableListOf<String>()
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

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            this.data = data
            imageUri = data.data
            imageUri?.let {
                isImageSelected = true
                // Cập nhật ImageView trong dialog đã mở
                val ivDeviceImage = dialogView.findViewById<ImageView>(R.id.iv_device_image)
                ivDeviceImage.setImageURI(imageUri)
            }
        }
    }

    private fun showAddDeviceDialog() {
        dialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        val etDeviceName = dialogView.findViewById<EditText>(R.id.et_device_name)
        val tvAddImg = dialogView.findViewById<TextView>(R.id.tv_add_img)

        // Khi nhấn vào TextView "Tải ảnh lên", mở Intent để chọn ảnh
        tvAddImg.setOnClickListener {
            openImageChooser()
        }

        dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Thêm thiết bị mới")
            .setPositiveButton("Thêm") { _, _ ->
                val name = etDeviceName.text.toString()
                if (name.isNotEmpty() && isImageSelected) {
                    showProgressbar()
                    addDeviceToFirestore(name,imageUri)
                } else {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin và chọn ảnh", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy") { _, _ ->
                isImageSelected = false
            }
            .setOnCancelListener {
                // Xử lý khi dialog bị hủy (ví dụ: click ra ngoài dialog)
                isImageSelected = false
            }
            .create()

        dialog.show()

    }

    private fun addDeviceToFirestore(name: String, imageUri: Uri?) {
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
                uploadImage(newDeviceDocRef.id, imageUri)
                Toast.makeText(this, "Thiết bị đã được thêm thành công", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                hideProgressbar()
                Toast.makeText(this, "Lỗi khi thêm thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImage(id: String, imageUri: Uri?) {
        val storage = FirebaseStorage.getInstance()
        val ref = storage.reference.child("device/$id/$id.jpg")

        imageUri?.let {
            ref.putFile(it)
                .addOnSuccessListener {
                    Toast.makeText(this, "Ảnh đã được tải lên", Toast.LENGTH_SHORT).show()
                    hideProgressbar()
                    loadDevices()

                    isImageSelected = false
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Lỗi tải ảnh", Toast.LENGTH_SHORT).show()
                }
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
                    showEmptyDeviceOptions()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi lấy thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEmptyDeviceOptions() {
        // Hiển thị một dialog hoặc thông báo cho người dùng thêm thiết bị hoặc xóa danh mục
        AlertDialog.Builder(this)
            .setTitle("Danh mục trống")
            .setMessage("Danh mục này chưa có thiết bị nào. Bạn hãy thêm ít nhất 1 thiết bị mới hoặc xóa danh mục?")
            .setPositiveButton("Thêm thiết bị") { _, _ ->
                showAddDeviceDialog() // Mở dialog thêm thiết bị
            }
            .setNegativeButton("Xóa danh mục") { _, _ ->
                deleteCategory(categoryId)
            }
            .show()
    }

    private fun deleteCategory(categoryId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val categoryRef = firestore.collection("service_categories").document(categoryId)

        // Hàm để xóa một collection và các document bên trong nó
        fun deleteSubCollection(collectionRef: CollectionReference, onComplete: () -> Unit) {
            collectionRef.get().addOnSuccessListener { querySnapshot ->
                val batch = firestore.batch() // Dùng batch để xóa nhiều document cùng lúc
                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }

                // Thực hiện batch delete
                batch.commit().addOnSuccessListener {
                    onComplete() // Sau khi xóa xong, gọi lại để tiếp tục xóa các collection tiếp theo
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi khi xóa sub-collection: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi truy cập sub-collection: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Hàm xóa toàn bộ các thiết bị và các gói dịch vụ của chúng trước khi xóa category
        fun deleteDevicesAndPackages(devicesRef: CollectionReference, onComplete: () -> Unit) {
            devicesRef.get().addOnSuccessListener { querySnapshot ->
                var deleteCount = 0
                if (querySnapshot.isEmpty) {
                    // Nếu không có thiết bị, xóa category luôn
                    onComplete()
                    return@addOnSuccessListener
                }
                for (deviceDoc in querySnapshot.documents) {
                    val deviceId = deviceDoc.id
                    deviceIdsToDelete.add(deviceId) // Lưu idDevice vào mảng để sau này xóa ảnh

                    val servicePackagesRef = deviceDoc.reference.collection("service_packages")
                    deleteSubCollection(servicePackagesRef) {
                        deviceDoc.reference.delete().addOnSuccessListener {
                            deleteCount++
                            // Nếu đã xóa hết các thiết bị, gọi onComplete
                            if (deleteCount == querySnapshot.size()) {
                                onComplete()
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Lỗi khi xóa thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi truy cập thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Xóa các thiết bị (devices) và gói dịch vụ (service_packages)
        val devicesRef = categoryRef.collection("devices")
        deleteDevicesAndPackages(devicesRef) {
            // Sau khi đã xóa toàn bộ devices và service_packages, tiếp tục xóa category
            categoryRef.delete().addOnSuccessListener {
                Toast.makeText(this, "Danh mục và các sub-collection đã được xóa", Toast.LENGTH_SHORT).show()

                // Gọi hàm xóa tất cả ảnh sau khi đã xóa document
                deleteAllDeviceImages()
                finish()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi xóa danh mục: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun deleteAllDeviceImages() {
        val storage = FirebaseStorage.getInstance()

        for (deviceId in deviceIdsToDelete) {
            val imageRef = storage.reference.child("device/$deviceId/$deviceId.jpg")
            imageRef.delete().addOnSuccessListener {
                Log.d("deleteDeviceImage", "Đã xóa ảnh cho thiết bị: $deviceId")
            }.addOnFailureListener { e ->
                Log.e("deleteDeviceImage", "Lỗi khi xóa ảnh cho thiết bị $deviceId: ${e.message}")
            }
        }

        // Sau khi hoàn tất xóa ảnh, làm trống mảng
        deviceIdsToDelete.clear()

        // Thông báo sau khi đã xóa hết ảnh
        Toast.makeText(this, "Đã xóa tất cả ảnh của thiết bị và làm trống danh sách", Toast.LENGTH_SHORT).show()
    }



    private fun setupRecyclerView(deviceList: List<Device>) {
        val adapter = DeviceListAdminAdapter(
            deviceList,
            onEditClick = { device ->
                // Xử lý sự kiện sửa thiết bị
                showEditDeviceDialog(device)
            },
            onDeleteClick = { device ->
                // Xử lý sự kiện xóa thiết bị
                showConfirmDeleteDialog(device)
            }
        )

        binding.recyclerViewCategory.adapter = adapter
        binding.recyclerViewCategory.layoutManager = LinearLayoutManager(this)
        adapter.notifyDataSetChanged()
    }

    private fun showConfirmDeleteDialog(device: Device) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa thiết bị này? Nếu xóa thì toàn bộ các gói dịch vụ bên trong (Nếu có) cũng sẽ bị xóa.")
            .setPositiveButton("Xóa") { _, _ ->
                deleImage(device.id_device)
                deleteDevice(device)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleImage(idDevice: String) {
        val storage = FirebaseStorage.getInstance()
        val ref = storage.reference.child("device/$idDevice/$idDevice.jpg")
        ref.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Ảnh đã được xóa", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi xóa ảnh", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDeviceDialog(device: Device) {
        dialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        val etDeviceName = dialogView.findViewById<EditText>(R.id.et_device_name)
        val ivDeviceImage = dialogView.findViewById<ImageView>(R.id.iv_device_image)
        val tvAddImg = dialogView.findViewById<TextView>(R.id.tv_add_img)

        // Điền thông tin hiện tại của thiết bị
        etDeviceName.setText(device.name)
        // Load ảnh từ storage
        val storage = FirebaseStorage.getInstance()
        val ref = storage.reference.child("device/${device.id_device}/${device.id_device}.jpg")
        ref.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this)
                .load(uri)
                .into(ivDeviceImage)
        }

        // Khi nhấn "Tải ảnh lên", mở Intent để chọn ảnh mới
        tvAddImg.setOnClickListener {
            openImageChooser()
        }

        dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Sửa thông tin thiết bị")
            .setPositiveButton("Cập nhật") { _, _ ->
                val newName = etDeviceName.text.toString()
                if (newName.isNotEmpty()) {
                    if (newName != device.name) {
                        // Nếu tên mới khác tên cũ, cập nhật tên và ảnh
                        if (isImageSelected) {
                            updateDeviceName(device, newName)
                            uploadImage(device.id_device, imageUri)
                        } else {
                            updateDeviceName(device, newName)
                        }
                    } else {
                        // Nếu tên không thay đổi, chỉ cập nhật ảnh
                        if (isImageSelected) {
                            uploadImage(device.id_device, imageUri)
                        }else{
                            Toast.makeText(this, "Không có gì thay đổi", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    }
                } else {
                    Toast.makeText(this, "Vui lòng nhập tên thiết bị", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy"){ _, _ ->
                isImageSelected = false
            }
            .setOnCancelListener {
                // Handle dialog canceled (e.g., clicking outside of dialog)
                isImageSelected = false
            }
            .create()

        dialog.show()
    }

    private fun updateDeviceName(device: Device, newName: String) {
        val updatedDevice = device.copy(name = newName)
        FirebaseFirestore.getInstance()
            .collection("service_categories")
            .document(device.categoryId)
            .collection("devices")
            .document(device.id_device)
            .update("name", newName)
            .addOnSuccessListener {
                Toast.makeText(this, "Tên thiết bị đã được cập nhật", Toast.LENGTH_SHORT).show()
                loadDevices() // Reload lại danh sách thiết bị
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi khi cập nhật tên thiết bị", Toast.LENGTH_SHORT).show()
            }

    }

    private fun deleteDevice(device: Device) {
        val firestore = FirebaseFirestore.getInstance()
        val deviceRef = firestore.collection("service_categories")
            .document(device.categoryId)
            .collection("devices")
            .document(device.id_device)

        // Xóa các subcollection
        deleteSubcollections(deviceRef)
    }

    private fun deleteSubcollections(deviceRef: DocumentReference) {
        deviceRef.collection("service_packages")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = FirebaseFirestore.getInstance().batch()
                // Xóa từng tài liệu trong subcollection
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }
                // Commit batch để xóa các tài liệu
                batch.commit().addOnSuccessListener {
                    // Sau khi xóa các subcollection, xóa tài liệu chính
                    deleteDeviceDocument(deviceRef)
                }.addOnFailureListener { e ->
                    Log.e("deleteDevice", "Error deleting service packages: ${e.message}")
                    Toast.makeText(this, "Lỗi khi xóa gói dịch vụ", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("deleteDevice", "Error getting service packages: ${e.message}")
                Toast.makeText(this, "Lỗi khi lấy gói dịch vụ", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteDeviceDocument(deviceRef: DocumentReference) {
        deviceRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Thiết bị đã được xóa", Toast.LENGTH_SHORT).show()
                loadDevices() // Reload lại danh sách thiết bị
                setupRecyclerView(emptyList()) // Hiển thị danh sách trống
            }
            .addOnFailureListener { e ->
                Log.e("deleteDevice", "Error deleting device: ${e.message}")
                Toast.makeText(this, "Lỗi khi xóa thiết bị", Toast.LENGTH_SHORT).show()
            }
    }


    private fun getDataIntent() {
        categoryId = intent.getStringExtra("categoryId").toString()
        categoryName = intent.getStringExtra("categoryName").toString()
    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener {
            onBackPressed()
        }
        // set title toolbar with category name
        tvToolbarTitle.text = "Danh sách thiết bị của $categoryName"
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val firestore = FirebaseFirestore.getInstance()

        // Kiểm tra xem danh mục có thiết bị nào hay không trước khi thoát
        firestore.collection("service_categories")
            .document(categoryId)
            .collection("devices")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // Hiển thị dialog yêu cầu thêm thiết bị hoặc xóa danh mục nếu trống
                    AlertDialog.Builder(this)
                        .setTitle("Danh mục trống")
                        .setMessage("Danh mục này chưa có thiết bị nào. Bạn hãy thêm ít nhất 1 thiết bị mới hoặc xóa danh mục?")
                        .setPositiveButton("Thêm thiết bị") { _, _ ->
                            loadDevices()
                            showAddDeviceDialog() // Mở dialog thêm thiết bị
                        }
                        .setNegativeButton("Xóa danh mục") { _, _ ->
                            deleteCategory(categoryId) // Gọi hàm xóa danh mục
                        }
                        .show()
                } else {
                    // Nếu danh mục có thiết bị, quay lại màn hình trước đó như bình thường
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi kiểm tra thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showProgressbar() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewCategory.visibility = View.GONE
    }

    private fun hideProgressbar() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewCategory.visibility = View.VISIBLE
    }

}