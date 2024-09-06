package com.example.myapp.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.MyViewPagerAdapter
import com.example.myapp.databinding.ActivityMainBinding
import com.example.myapp.fragment.AccountFragment
import com.example.myapp.fragment.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var mBottomNavigationView: BottomNavigationView? = null
    var viewPager2: ViewPager2? = null
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kiểm tra thông tin người dùng và chuyển hướng nếu cần
        checkUserProfileAndNavigate()

        binding.apply {
            mBottomNavigationView = bottomNavigation
            viewPager2 = viewpager2
            viewPager2?.isUserInputEnabled = false
            val myViewPagerAdapter = MyViewPagerAdapter(this@MainActivity)
            viewPager2?.adapter = myViewPagerAdapter
            viewPager2?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    when (position) {
                        0 -> mBottomNavigationView?.menu?.findItem(R.id.nav_home)?.isChecked = true
                        1 -> mBottomNavigationView?.menu?.findItem(R.id.nav_history)?.isChecked = true
                        2 -> mBottomNavigationView?.menu?.findItem(R.id.nav_account)?.isChecked = true
                    }
                }
            })
            mBottomNavigationView?.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        viewPager2?.currentItem = 0
                    }
                    R.id.nav_history -> {
                        viewPager2?.currentItem = 1
                    }
                    R.id.nav_account -> {
                        viewPager2?.currentItem = 2
                    }
                }
                true
            }

        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        showConfirmExitApp()
    }

    private fun showConfirmExitApp() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.confirm_exit_app))
        builder.setPositiveButton(getString(R.string.yes)) { _, _ ->
            finish()
        }
        builder.setNegativeButton(getString(R.string.no)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun checkUserProfileAndNavigate() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        // Check user profile status from Firestore
        val docRef = db.collection("Users").document(userId)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val phone = document.getString("phone")
                    val address = document.getString("address")

                    if (name.isNullOrEmpty() || phone.isNullOrEmpty() || address.isNullOrEmpty()) {
                        navigateToCompleteProfile()
                    }
                } else {

                }
            }
            .addOnFailureListener {
                // Handle failure (e.g., show a message to the user)
            }
    }

    private fun navigateToCompleteProfile() {
        val intent = Intent(this, CompleteProfileActivity::class.java)
        startActivity(intent)
    }
}
