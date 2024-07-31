package com.example.myapp.activity

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.MyViewPagerAdminAdapter
import com.example.myapp.databinding.ActivityAdminBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminActivity : AppCompatActivity() {
    lateinit var binding: ActivityAdminBinding
    private var mBottomNavigationView: BottomNavigationView? = null
    var viewPager2: ViewPager2? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            mBottomNavigationView = bottomNavigation
            viewPager2 = viewpager2
            viewPager2?.isUserInputEnabled = false
            val myViewPagerAdminAdapter = MyViewPagerAdminAdapter(this@AdminActivity)
            viewpager2?.adapter = myViewPagerAdminAdapter
            viewPager2?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    when (position) {
                        0 -> mBottomNavigationView?.menu?.findItem(R.id.nav_home)?.isChecked = true
                        1 -> mBottomNavigationView?.menu?.findItem(R.id.nav_list)?.isChecked = true
                        2 -> mBottomNavigationView?.menu?.findItem(R.id.nav_more)?.isChecked = true
                        3 -> mBottomNavigationView?.menu?.findItem(R.id.nav_gif)?.isChecked = true
                        4 -> mBottomNavigationView?.menu?.findItem(R.id.nav_account)?.isChecked = true
                    }
                }
            })
            mBottomNavigationView?.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        viewPager2?.currentItem = 0
                    }
                    R.id.nav_list -> {
                        viewPager2?.currentItem = 1
                        // Delete bade when click on Orders
                        val badge = mBottomNavigationView?.getBadge(R.id.nav_list)
                        badge?.isVisible = false
                        true
                    }
                    R.id.nav_more -> {
                        viewPager2?.currentItem = 2
                    }
                    R.id.nav_gif -> {
                        viewPager2?.currentItem = 3
                    }
                    R.id.nav_account -> {
                        viewPager2?.currentItem = 4
                    }
                }
                true
            }
        }

        // Add badge to Orders
        addBadgeToOrders(5) // example

    }

    private fun addBadgeToOrders(orderCount: Int) {
        mBottomNavigationView?.let {
            val badge = it.getOrCreateBadge(R.id.nav_list)
            badge.isVisible = true
            badge.number = orderCount
            badge.badgeTextColor = getColor(R.color.white)
            badge.backgroundColor = getColor(R.color.red)
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
}