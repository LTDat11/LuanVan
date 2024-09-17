package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.databinding.ActivityInfoProcessingAdminBinding
import com.example.myapp.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InfoProcessingAdminActivity : AppCompatActivity() {
    lateinit var binding: ActivityInfoProcessingAdminBinding
    private var order: Order? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoProcessingAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDataIntent()
        initToolbar()
        intitUI()
    }

    private fun intitUI() {
        binding.apply {
            //set image with glide
            Glide.with(this@InfoProcessingAdminActivity).load(order?.imgURLServicePackage).into(imgPackage)
            //set text for text view
            tvName.text = order?.namePackage
            tvPrice.text = order?.price.toString()
            tvDescription.text = order?.description
            if (order?.notes2.isNullOrEmpty()){
                tvNote.text = "Không có"
            }
            tvNameBrand.text = order?.selectedBrand
            tvCreatedAt.text = order?.createdAt.toString()

            getInfoTechnicain(order?.id_technician)
        }
    }

    private fun getInfoTechnicain(idTechnician: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()
                // use snapshot to get name and description of technician by id technician
                db.collection("Users").document(idTechnician!!)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val name = document.getString("name")
                            val description = document.getString("description")
                            val imageURL = document.getString("imageURL")

                            binding.apply {
                                technicainName.text = name
                                technicainDescription.text = description
                                Glide.with(this@InfoProcessingAdminActivity).load(imageURL).into(technicainImage)
                            }

                        }
                    }

            }
        }

    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = "Thông tin đơn hàng đang xử lý"
    }

    private fun getDataIntent() {
        order = intent.getSerializableExtra("order") as Order
    }
}