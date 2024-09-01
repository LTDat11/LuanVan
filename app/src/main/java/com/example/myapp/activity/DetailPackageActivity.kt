package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.example.myapp.R
import com.example.myapp.databinding.ActivityDetailPackageBinding
import com.example.myapp.model.ServicePackage
import java.text.NumberFormat
import java.util.Locale

class DetailPackageActivity : AppCompatActivity() {
    private lateinit var servicePackage: ServicePackage
    private lateinit var binding: ActivityDetailPackageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailPackageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDataIntent()
        initToolbar()
        initData()
        initListener()
    }

    private fun initListener() {
        binding.apply {

        }
    }


    private fun initData() {
        binding.apply {
            tvName.text = servicePackage.name
            tvPrice.text = servicePackage.price
            tvDescription.text = servicePackage.description
            tvTotal.text = servicePackage.price
        }
    }

    private fun getDataIntent() {
        servicePackage = intent.getSerializableExtra("package") as ServicePackage
    }

    private fun initToolbar() {
        binding.apply {
            val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
            val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
            imgToolbarBack.setOnClickListener { finish() }
            tvToolbarTitle.text = servicePackage.name
        }
    }

}