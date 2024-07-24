package com.example.myapp.fragment

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.PhotoBannerAdapter
import com.example.myapp.model.PhotoBanner
import com.example.myapp.model.ServiceCategory
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import me.relex.circleindicator.CircleIndicator3

class HomeFragment : Fragment() {
    private var mView: View? = null
    private var tabCategory: TabLayout? = null
    private var viewPager: ViewPager2? = null
    private var indicator: CircleIndicator3? = null
    private val serviceCategories = mutableListOf<ServiceCategory>()
    private val listPhotoBanners = mutableListOf<PhotoBanner>()
    private lateinit var mHandlerBanner: Handler
    private lateinit var mRunnableBanner: Runnable

    private val bannerImages = mutableListOf<String>() // List to store image URLs
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        addServiceCategories()
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_home, container, false)
        initUi()
        getListCategory()
        getListPhotoBanners()
        return mView
    }

    private fun initUi() {
        tabCategory = mView?.findViewById(R.id.tab_category)
        viewPager = mView?.findViewById(R.id.view_pager_banner)
        indicator = mView?.findViewById(R.id.indicator)
    }

    private fun getListCategory() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("service_categories")
            .get()
            .addOnSuccessListener { documents ->
                serviceCategories.clear()
                for (document in documents) {
                    val category = document.toObject(ServiceCategory::class.java)
                    serviceCategories.add(category)
                }
                displayTabsCategory()
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }

    private fun displayTabsCategory() {
        tabCategory?.removeAllTabs() // Clear any existing tabs

        for (category in serviceCategories) {
            tabCategory?.newTab()?.setText(category.name)?.let { tabCategory?.addTab(it) }
        }

        // Optional: Set a listener for tab selection
        tabCategory?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab selection
                val position = tab?.position ?: 0
                Log.d("Tab Selected", "Selected tab position: $position")
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Handle tab unselection
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Handle tab reselection
            }
        })
    }


    private fun getListPhotoBanners() {
        val storageReference = FirebaseStorage.getInstance().reference.child("banner/")
        storageReference.listAll()
            .addOnSuccessListener { listResult ->
                listPhotoBanners.clear()
                for (fileRef in listResult.items) {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        listPhotoBanners.add(PhotoBanner(uri.toString()))
                        if (listPhotoBanners.size == listResult.items.size) {
                            displayListBanner()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Storage", "Error getting banner images: ", exception)
            }
    }

    private fun displayListBanner() {
        val adapter = PhotoBannerAdapter(listPhotoBanners)
        viewPager?.adapter = adapter
        indicator?.setViewPager(viewPager)
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                mHandlerBanner.removeCallbacks(mRunnableBanner)
                mHandlerBanner.postDelayed(mRunnableBanner, 3000)
            }
        })
        mHandlerBanner = Handler()
        mRunnableBanner = Runnable {
            var currentItem = viewPager?.currentItem ?: 0
            currentItem = (currentItem + 1) % listPhotoBanners.size
            viewPager?.currentItem = currentItem
        }
        mHandlerBanner.postDelayed(mRunnableBanner, 3000)
    }


//    // Add service categories to Firestore

//    fun addServiceCategories() {
//        val categories = listOf(
//            ServiceCategory(
//                name = "Điện gia dụng",
//                description = "Dịch vụ sửa chữa và bảo trì các thiết bị điện gia dụng như tủ lạnh, máy giặt, lò vi sóng, vv.",
//                price_range = "500,000 - 2,000,000 VND"
//            ),
//            ServiceCategory(
//                name = "Sửa ống nước",
//                description = "Dịch vụ sửa chữa và bảo trì hệ thống ống nước trong gia đình.",
//                price_range = "300,000 - 1,500,000 VND"
//            ),
//            ServiceCategory(
//                name = "Bảo trì điều hòa",
//                description = "Dịch vụ bảo trì và sửa chữa hệ thống điều hòa không khí.",
//                price_range = "400,000 - 2,500,000 VND"
//            ),
//            ServiceCategory(
//                name = "Sửa chữa điện tử",
//                description = "Dịch vụ sửa chữa các thiết bị điện tử như TV, máy tính, điện thoại di động.",
//                price_range = "200,000 - 3,000,000 VND"
//            ),
//            ServiceCategory(
//                name = "Làm sạch và bảo trì",
//                description = "Dịch vụ làm sạch và bảo trì các thiết bị và không gian trong gia đình.",
//                price_range = "100,000 - 1,000,000 VND"
//            )
//        )
//
//        val firestore = FirebaseFirestore.getInstance()
//        for (category in categories) {
//            firestore.collection("service_categories")
//                .add(category)
//                .addOnSuccessListener { documentReference ->
//                    Log.d("Firestore", "DocumentSnapshot written with ID: ${documentReference.id}")
//                }
//                .addOnFailureListener { e ->
//                    Log.w("Firestore", "Error adding document", e)
//                }
//        }
//    }
}