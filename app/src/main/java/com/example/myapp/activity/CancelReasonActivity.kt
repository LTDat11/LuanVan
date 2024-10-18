package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.databinding.ActivityCancelReasonBinding
import com.example.myapp.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CancelReasonActivity : AppCompatActivity() {
    lateinit var binding: ActivityCancelReasonBinding
    private var order: Order? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCancelReasonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDataIntent()
        initToolbar()
        intitUI()
    }

    private fun intitUI() {
        binding.apply {
            //set image with glide
            Glide.with(this@CancelReasonActivity).load(order?.imgURLServicePackage).into(imgPackage)
            //set text for text view
            tvName.text = order?.namePackage
            tvPrice.text = order?.price.toString()
            tvDescription.text = order?.description
            if (order?.notes2.isNullOrEmpty()){
                tvNote.text = "Không có"
            }
            tvNameBrand.text = order?.selectedBrand
            tvCreatedAt.text = order?.createdAt.toString()
            tvAddress.text = order?.address
            tvReason.text = order?.cancelReason
            getInfoCustomer()

            getInfoTechnicain(order?.id_technician)
        }
    }

    private fun getInfoCustomer() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()
                // use snapshot to get name and phone of user by id user
                db.collection("Users").document(order?.id_customer!!)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val name = document.getString("name")
                            val phone = document.getString("phone")
                            val address = document.getString("address")
                            binding.apply {
                                tvNameCustomer.text = name
                                tvPhoneCustomer.text = phone
                                tvAddressCustomer.text = address
                            }
                        }
                    }
            }
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
                                Glide.with(this@CancelReasonActivity).load(imageURL).into(technicainImage)
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
        tvToolbarTitle.text = "Lý do hủy đơn hàng"
    }

    private fun getDataIntent() {
        order = intent.getSerializableExtra("order") as Order
    }
}