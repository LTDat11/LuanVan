package com.example.myapp.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.myapp.R
import com.example.myapp.activity.AdminActivity
import com.example.myapp.activity.LoginActivity
import com.example.myapp.activity.MainActivity
import com.example.myapp.activity.TechnicianActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OnboardingFragment3 : Fragment() {

    private lateinit var btn_start: TextView
    private val db = FirebaseFirestore.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.fragment_onboarding3, container, false)
        btn_start = mView.findViewById(R.id.btn_start)

        btn_start.setOnClickListener {
            checkUserLoginStatus()
        }

        return mView
    }

    private fun checkUserLoginStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val userDocRef = db.collection("Users").document(uid)
            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        // Kiểm tra role của user để chuyển hướng
                        val role = document.getString("role")
                        when (role) {
                            "Customer" -> goToMainActivity()
                            "Technician" -> goToTechnicianActivity()
                            else -> goToAdminActivity()
                        }
                    } else {
                        Toast.makeText(context, "Document có vấn đề", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            goToLoginActivity()
        }
    }

    private fun goToTechnicianActivity() {
        val intent = Intent(activity, TechnicianActivity::class.java)
        startActivity(intent)
        activity?.finish() // Kết thúc activity hiện tại
    }

    private fun goToLoginActivity() {
        val intent = Intent(activity, LoginActivity::class.java)
        startActivity(intent)
        activity?.finish() // Kết thúc activity hiện tại
    }

    private fun goToMainActivity() {
        val intent = Intent(activity, MainActivity::class.java)
        startActivity(intent)
        activity?.finish() // Kết thúc activity hiện tại
    }

    private fun goToAdminActivity() {
        val intent = Intent(activity, AdminActivity::class.java)
        startActivity(intent)
        activity?.finish() // Kết thúc activity hiện tại
    }

}