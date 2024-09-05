package com.example.myapp.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.databinding.ActivityUserInfoBinding
import com.example.myapp.fragment.HomeFragment
import com.example.myapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserInfoActivity : AppCompatActivity() {
    lateinit var binding: ActivityUserInfoBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val PICK_IMAGE_REQUEST_CODE = 123
    private var isImageSelected = false
    private var data: Intent? = null
    private var selectedImageUri: Uri? = null
    private var originalName: String? = null
    private var originalPhone: String? = null
    private var originalAddress: String? = null
    private var isEnableButtonSave = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initUi()
        initListener()
    }

    private fun initListener() {
        val btnEdit = findViewById<Button>(R.id.btn_edit_avatar)
        val btnDelete = findViewById<Button>(R.id.btn_delete_avatar)
        val btnSave = findViewById<Button>(R.id.btn_save)

        // Chọn ảnh đại diện
        btnEdit.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    openGallery()
                }
            }
        }

        // Xóa ảnh đại diện
        btnDelete.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    showDeleteConfirmationDialog()
                }
            }
        }

        binding.apply {
            edtName?.addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    updateUIState()
                }
            })

            edtPhone?.addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    updateUIState()
                }
            })

            edtAddress?.addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    updateUIState()
                }
            })

        }

        // Lưu thông tin người dùng sau khi sửa
        btnSave.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    if (!isEnableButtonSave){
                        return@withContext
                    }else{
                        saveInfo()
                    }
                }
            }

        }


    }

    private fun saveInfo() {
        binding.apply {
            val name = edtName.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val address = edtAddress.text.toString().trim()

            val currentUser = firebaseAuth.currentUser
            currentUser?.let { user ->
                val userDocument = firestore.collection("Users").document(user.uid)
                userDocument.update("name", name)
                userDocument.update("phone", phone)
                userDocument.update("address", address)
                    .addOnSuccessListener {
                        // Cập nhật thông tin thành công
                        Toast.makeText(this@UserInfoActivity, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show()
                        // Cập nhật trạng thái nút Button
                        initUi()
                        updateUIState()
                    }
                    .addOnFailureListener {
                        // Cập nhật thông tin thất bại
                        initUi()
                        Toast.makeText(this@UserInfoActivity, "Cập nhật thông tin thất bại!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun openGallery() {
        //Chọn ảnh từ album
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
                    this@UserInfoActivity.data = data // Lưu trữ data vào biến data
                    selectedImageUri = data.data
                    selectedImageUri?.let {
                        binding.civAvatar.setImageURI(it)
                        isImageSelected = true //đã chọn ảnh
                        updateImage()
                    }
                }
            }
        }
    }

    private fun updateImage() {
        val currentUser = firebaseAuth.currentUser
        currentUser?.let { user ->
            // Tạo đường dẫn tới ảnh trên Firebase Storage
            val storageRef = storage.reference
            val imageRef = storageRef.child("avatar/${user.uid}.jpg")

            // Tạo AlertDialog để hiển thị xác nhận
            val confirmationDialog = AlertDialog.Builder(this)
                .setTitle("Xác nhận thay đổi ảnh đại diện")
                .setMessage("Bạn có chắc chắn muốn thay đổi ảnh đại diện?")
                .setPositiveButton("Chấp nhận") { _, _ ->
                    // Nếu chấp nhận, tiếp tục tải ảnh lên Firebase Storage
                    uploadImage(imageRef)
                }
                .setNegativeButton("Hủy bỏ"){_,_ ->
                    // load lại nội dung trang
                    return@setNegativeButton
                }
                .create()

            // Hiển thị AlertDialog
            confirmationDialog.show()
        }
    }

    private fun uploadImage(imageRef: StorageReference) {
        // Lấy Uri của ảnh đã chọn từ Intent
        val selectedImageUri: Uri? = data?.data

        // Kiểm tra xem Uri có tồn tại và đã chọn ảnh chưa
        if (selectedImageUri != null && isImageSelected) {
            // Tải ảnh lên Firebase Storage
            imageRef.putFile(selectedImageUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Lấy đường dẫn tới ảnh trên Firebase Storage
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Cập nhật đường dẫn ảnh mới vào Firestore
                        val userDocument = firestore.collection("Users").document(firebaseAuth.currentUser?.uid!!)
                        userDocument.update("imageURL", uri.toString())
                            .addOnSuccessListener {
                                // Thông báo cập nhật thành công
                                Toast.makeText(this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                // Thông báo cập nhật thất bại
                                Toast.makeText(this, "Cập nhật ảnh đại diện thất bại!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    // Thông báo tải ảnh lên thất bại
                    Toast.makeText(this, "Tải ảnh lên thất bại!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa ảnh")
            .setMessage("Bạn có chắc chắn muốn xóa ảnh?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteCurrentImage()
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    private fun deleteCurrentImage() {
        val currentUser = firebaseAuth.currentUser
        currentUser?.let { user ->
            val userDocument = firestore.collection("Users").document(user.uid)
            userDocument.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Lấy đường dẫn ảnh hiện tại từ Firestore
                    val currentImageUrl = documentSnapshot.getString("imageURL")
                    // Kiểm tra xem ảnh hiện tại có phải là avtdf.jpg không
                    if (!currentImageUrl.isNullOrEmpty() && currentImageUrl.contains("avatardf.jpg")) {
                        // Thông báo rằng đây là ảnh mặc định và không thể xóa
                        Toast.makeText(this, "Đây là ảnh mặc định và không thể xóa!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Tạo StorageReference từ đường dẫn ảnh hiện tại
                        val currentImageRef = storage.getReferenceFromUrl(currentImageUrl.toString())
                        // Xóa ảnh hiện tại trong Firebase Storage
                        currentImageRef.delete()
                            .addOnSuccessListener {
                                // Thành công, cập nhật đường dẫn ảnh avtdf vào Firestore
                                val imageStorageRef = FirebaseStorage.getInstance().reference.child("avatar/avatardf.jpg")
                                // Lấy URL của ảnh từ Firebase Storage
                                imageStorageRef.downloadUrl.addOnSuccessListener {imageUrl ->
                                    val newImageUrl = imageUrl.toString()

                                    userDocument.update("imageURL", newImageUrl)
                                        .addOnSuccessListener {
                                            // Thông báo cập nhật thành công
                                            Toast.makeText(this, "Xóa và cập nhật ảnh thành công!", Toast.LENGTH_SHORT).show()
                                            // Hiển thị ảnh mới lên giao diện
                                            Glide.with(this)
                                                .load(newImageUrl)
                                                .circleCrop()
                                                .into(binding.civAvatar)
                                        }
                                        .addOnFailureListener {
                                            // Thông báo cập nhật thất bại
                                            Toast.makeText(this, "Cập nhật ảnh thất bại!", Toast.LENGTH_SHORT).show()
                                        }
                                }

                            }
                            .addOnFailureListener {
                                // Thông báo xóa ảnh hiện tại thất bại
                                Toast.makeText(this, "Xóa ảnh hiện tại thất bại!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
        }
    }


    private fun initToolbar() {
        val imgBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvTitle = findViewById<TextView>(R.id.tv_toolbar_title)

        // Thiết lập tiêu đề cho Toolbar
        tvTitle.text = getString(R.string.user_info_title)

        // Xử lý sự kiện khi click vào nút back
        imgBack.setOnClickListener {
            finish()
        }
    }

    private fun initUi() {
        binding.apply {
            val currentUser = firebaseAuth.currentUser
            currentUser?.let { user ->
                val userDocument = firestore.collection("Users").document(user.uid)

                userDocument.get().addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        originalName = documentSnapshot.getString("name")
                        originalPhone = documentSnapshot.getString("phone")
                        originalAddress = documentSnapshot.getString("address")
                        val imageURL = documentSnapshot.getString("imageURL")

                        // Hiển thị thông tin người dùng lên giao diện
                        edtName.setText(originalName)
                        edtPhone.setText(originalPhone)
                        edtAddress.setText(originalAddress)
                        Glide.with(this@UserInfoActivity)
                            .load(imageURL)
                            .circleCrop()
                            .into(civAvatar)

                        // Sau khi khởi tạo giao diện, cập nhật trạng thái ban đầu của nút Button
                        updateUIState()
                    }
                }
            }
        }

    }

    private fun updateUIState() {
        binding.apply {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main){
                    val name = edtName.text.toString().trim()
                    val phone = edtPhone.text.toString().trim()
                    val address = edtAddress.text.toString().trim()

                    val isNameChanged = name != originalName
                    val isPhoneChanged = phone != originalPhone
                    val isAddressChanged = address != originalAddress

                    isEnableButtonSave = isNameChanged || isPhoneChanged || isAddressChanged

                    btnSave.apply {
                        background = ContextCompat.getDrawable(
                            context,
                            if (isEnableButtonSave) R.drawable.bg_button_enable_corner_16 else R.drawable.bg_button_disable_corner_16
                        )
                        isEnabled = isEnableButtonSave
                    }
                }
            }
        }
    }

}