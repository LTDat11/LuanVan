package com.example.myapp.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.adapter.BannerManagementAdapter
import com.example.myapp.databinding.ActivityBannerManagementBinding
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BannerManagementActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var bannerManagementAdapter: BannerManagementAdapter
    private val bannerList = mutableListOf<String>() // Danh sách chứa URL ảnh
    lateinit var binding: ActivityBannerManagementBinding
    private val PICK_IMAGE_REQUEST = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBannerManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = binding.recyclerViewBanner
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Thiết lập adapter cho RecyclerView

        initToolbar()
        // Tải danh sách banner từ Firebase Storage
        loadBanners()
        initListeners()
    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        // set title toolbar with category name
        tvToolbarTitle.text = "Quản lý banner"
    }

    private fun initListeners() {
        binding.fabAdd.setOnClickListener {
            addBanner()
        }

        bannerManagementAdapter = BannerManagementAdapter(bannerList) { imageUrl ->
            // Xử lý sự kiện xóa banner
            deleteBanner(imageUrl)
        }
        recyclerView.adapter = bannerManagementAdapter
    }

    private fun loadBanners() {
        // Truy cập vào Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference.child("banner/")

        storageRef.listAll().addOnSuccessListener { listResult ->
            bannerList.clear()
            for (fileRef in listResult.items) {
                // Lấy URL của mỗi file trong banners/
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    bannerList.add(uri.toString())
                    bannerManagementAdapter.notifyDataSetChanged()
                }
            }
        }
    }


    private fun deleteBanner(imageUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                // Xóa banner khỏi Firebase Storage với URL imageUrl
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                storageRef.delete().addOnSuccessListener {
                    bannerList.remove(imageUrl)
                    bannerManagementAdapter.notifyDataSetChanged()
                    Toast.makeText(this@BannerManagementActivity, "Xóa banner thành công", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this@BannerManagementActivity, "Xóa banner thất bại", Toast.LENGTH_SHORT).show()
                }

            }
        }

    }

    private fun addBanner() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            uploadBanner(imageUri)
        }
    }

    private fun uploadBanner(imageUri: Uri?) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                if (imageUri != null) {
                    val fileName = System.currentTimeMillis().toString() + ".jpg"
                    val storageRef = FirebaseStorage.getInstance().reference.child("banner/$fileName")

                    // Tải lên ảnh
                    storageRef.putFile(imageUri).addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            bannerList.add(uri.toString())
                            bannerManagementAdapter.notifyDataSetChanged()
                            Toast.makeText(this@BannerManagementActivity, "Tải lên banner thành công", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this@BannerManagementActivity, "Tải lên banner thất bại", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

    }

}