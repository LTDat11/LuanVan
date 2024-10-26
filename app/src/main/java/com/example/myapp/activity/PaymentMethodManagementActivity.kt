package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.R
import com.example.myapp.adapter.PaymentMethodManagementAdapter
import com.example.myapp.databinding.ActivityPaymentMethodManagementBinding
import com.example.myapp.model.PaymentMethod
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PaymentMethodManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentMethodManagementBinding
    private lateinit var paymentMethodManagementAdapter: PaymentMethodManagementAdapter
    private var paymentMethods = mutableListOf<PaymentMethod>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentMethodManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initRecyclerView()
        loadPaymentMethods()
    }

    private fun initRecyclerView() {
        paymentMethodManagementAdapter = PaymentMethodManagementAdapter(paymentMethods){ paymentMethod ->
            updateIsAvailable(paymentMethod)
        }
        binding.rcvPaymentMethod.adapter = paymentMethodManagementAdapter
        binding.rcvPaymentMethod.layoutManager = LinearLayoutManager(this)
    }

    private fun updateIsAvailable(paymentMethod: PaymentMethod) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()
                db.collection("paymentMethods")
                    .document(paymentMethod.id)
                    .update("isAvailable", !paymentMethod.isAvailable)
                    .addOnSuccessListener {
                        Log.d("FireStore", "DocumentSnapshot successfully updated!")
                    }
                    .addOnFailureListener { e ->
                        Log.w("FireStore", "Error updating document", e)
                    }
            }
        }
    }

    private fun loadPaymentMethods() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()
                db.collection("paymentMethods")
                    .addSnapshotListener{ snapshot, e ->
                        if (e != null) {
                            Log.w("FireStore", "Listen failed.", e)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            paymentMethods.clear()
                            for (doc in snapshot) {
                                val paymentMethod = doc.toObject(PaymentMethod::class.java)
                                paymentMethod.isAvailable = doc.getBoolean("isAvailable") ?: true
                                paymentMethods.add(paymentMethod)
                            }
                            paymentMethodManagementAdapter.notifyDataSetChanged()
                        }
                        else {
                            Log.d("FireStore", "Current data: null")
                        }
                    }
            }
        }
    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = getString(R.string.payment_management)
    }


}
