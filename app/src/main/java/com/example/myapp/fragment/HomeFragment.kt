package com.example.myapp.fragment

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.PhotoBannerAdapter
import com.example.myapp.adapter.ServicePackagePagerAdapter
import com.example.myapp.model.PhotoBanner
import com.example.myapp.model.ServiceCategory
import com.example.myapp.model.ServicePackage
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import me.relex.circleindicator.CircleIndicator3
import androidx.appcompat.widget.SearchView


class HomeFragment : Fragment() {
    private var mView: View? = null
    private var tabCategory: TabLayout? = null
    private var viewPager: ViewPager2? = null
    private var viewPagerCategory: ViewPager2? = null
    private var indicator: CircleIndicator3? = null
    private val serviceCategories = mutableListOf<ServiceCategory>()
    private val listPhotoBanners = mutableListOf<PhotoBanner>()
    private val servicePackagesList = mutableListOf<List<ServicePackage>>()
    private lateinit var mHandlerBanner: Handler
    private lateinit var mRunnableBanner: Runnable

    private val bannerImages = mutableListOf<String>() // List to store image URLs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        addServiceCategoriesAndPackages()
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_home, container, false)
        initUi()
        getListCategory()
        getListPhotoBanners()
        setupSearchView()
        return mView
    }

    private fun initUi() {
        tabCategory = mView?.findViewById(R.id.tab_category)
        viewPager = mView?.findViewById(R.id.view_pager_banner)
        viewPagerCategory = mView?.findViewById(R.id.view_pager_category)
        indicator = mView?.findViewById(R.id.indicator)
    }

    private fun getListCategory() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("service_categories")
            .get()
            .addOnSuccessListener { documents ->
                serviceCategories.clear()
                servicePackagesList.clear()
//                val servicePackagesList = mutableListOf<List<ServicePackage>>()
                for (document in documents) {
                    val category = document.toObject(ServiceCategory::class.java)
                    serviceCategories.add(category)
                    getServicePackagesForCategory(document.id) { packages ->
                        servicePackagesList.add(packages)
                        if (servicePackagesList.size == serviceCategories.size) {
                            displayTabsCategory(servicePackagesList)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }

    private fun getServicePackagesForCategory(categoryId: String, callback: (List<ServicePackage>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("service_categories")
            .document(categoryId)
            .collection("service_packages")
            .get()
            .addOnSuccessListener { documents ->
                val servicePackages = documents.map { it.toObject(ServicePackage::class.java) }
                callback(servicePackages)
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting service packages: ", exception)
            }
    }

    private fun displayTabsCategory(servicePackagesList: List<List<ServicePackage>>) {
        tabCategory?.removeAllTabs() // Clear any existing tabs

        for (category in serviceCategories) {
            tabCategory?.newTab()?.setText(category.name)?.let { tabCategory?.addTab(it) }
        }

        val adapter = ServicePackagePagerAdapter(requireActivity(), servicePackagesList)
        viewPagerCategory?.adapter = adapter

        TabLayoutMediator(tabCategory!!, viewPagerCategory!!) { tab, position ->
            tab.text = serviceCategories[position].name
        }.attach()
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

    private fun setupSearchView() {
        val searchView: SearchView = mView?.findViewById(R.id.search_view) ?: return
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Xử lý khi người dùng nhấn enter hoặc nút tìm kiếm trên bàn phím
//                query?.let { filterPackages(it) }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // Xử lý khi người dùng thay đổi nội dung của SearchView
                filterPackages(newText)
                return true
            }
        })
    }

    private fun filterPackages(query: String) {

//        val filteredPackagesList = servicePackagesList.map { packages ->
//            packages.filter { servicePackage ->
//                servicePackage.name.contains(query, ignoreCase = true) ||
//                        servicePackage.price.contains(query, ignoreCase = true)
//            }
//        }

        val filteredList = mutableListOf<List<ServicePackage>>()
        // Lọc gói dịch vụ trong mỗi tab
        for (packages in servicePackagesList) {
            val filteredPackages = packages.filter {
                it.name.toLowerCase().contains(query.toLowerCase()) ||
                        it.price.toLowerCase().contains(query.toLowerCase())
            }
            filteredList.add(filteredPackages)
        }

        // Cập nhật adapter cho ViewPager2
        val adapter = ServicePackagePagerAdapter(requireActivity(), filteredList)
        viewPagerCategory?.adapter = adapter

        TabLayoutMediator(tabCategory!!, viewPagerCategory!!) { tab, position ->
            tab.text = serviceCategories[position].name
        }.attach()
    }

      // Add name of service categories and their packages to Firestore

//    fun addServiceCategoriesAndPackages() {
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
//                .document(category.name)
//                .set(category)
//                .addOnSuccessListener { documentReference ->
//                    Log.d("Firestore", "DocumentSnapshot written with ID: ${category.name}")
//                    val servicePackages = generateServicePackages(category.name)
//                    for (servicePackage in servicePackages) {
//                        firestore.collection("service_categories")
//                            .document(category.name)
//                            .collection("service_packages")
//                            .add(servicePackage)
//                            .addOnSuccessListener {
//                                Log.d("Firestore", "ServicePackage added with ID: ${it.id}")
//                            }
//                            .addOnFailureListener { e ->
//                                Log.w("Firestore", "Error adding service package", e)
//                            }
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Log.w("Firestore", "Error adding document", e)
//                }
//        }
//    }
//
//    fun generateServicePackages(categoryId: String): List<ServicePackage> {
//        return listOf(
//            ServicePackage(
//                name = "Gói sửa chữa ${categoryId} 1",
//                imageUrl = "url_to_image",
//                price = "500,000 VND",
//                description = "Mô tả cho gói dịch vụ 1 của $categoryId."
//            ),
//            ServicePackage(
//                name = "Gói sửa chữa ${categoryId} 2",
//                imageUrl = "url_to_image",
//                price = "700,000 VND",
//                description = "Mô tả cho gói dịch vụ 2 của $categoryId."
//            ),
//            ServicePackage(
//                name = "Gói sửa chữa ${categoryId} 3",
//                imageUrl = "url_to_image",
//                price = "600,000 VND",
//                description = "Mô tả cho gói dịch vụ 3 của $categoryId."
//            ),
//            ServicePackage(
//                name = "Gói sửa chữa ${categoryId} 4",
//                imageUrl = "url_to_image",
//                price = "800,000 VND",
//                description = "Mô tả cho gói dịch vụ 4 của $categoryId."
//            ),
//            ServicePackage(
//                name = "Gói sửa chữa ${categoryId} 5",
//                imageUrl = "url_to_image",
//                price = "900,000 VND",
//                description = "Mô tả cho gói dịch vụ 5 của $categoryId."
//            )
//        )
//    }

        //    fun addServiceCategoriesAndPackages updated

//    fun addServiceCategoriesAndPackages() {
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
//            val categoryId = firestore.collection("service_categories").document().id
//            val categoryWithId = category.copy(id = categoryId)
//
//            firestore.collection("service_categories")
//                .document(categoryId)
//                .set(categoryWithId)
//                .addOnSuccessListener {
//                    Log.d("Firestore", "DocumentSnapshot written with ID: $categoryId")
//                    val servicePackages = generateServicePackages(categoryId)
//                    for (servicePackage in servicePackages) {
//                        val packageId = firestore.collection("service_categories")
//                            .document(categoryId)
//                            .collection("service_packages")
//                            .document().id
//                        val servicePackageWithId = servicePackage.copy(id = packageId)
//
//                        firestore.collection("service_categories")
//                            .document(categoryId)
//                            .collection("service_packages")
//                            .document(packageId)
//                            .set(servicePackageWithId)
//                            .addOnSuccessListener {
//                                Log.d("Firestore", "ServicePackage added with ID: $packageId")
//                            }
//                            .addOnFailureListener { e ->
//                                Log.w("Firestore", "Error adding service package", e)
//                            }
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Log.w("Firestore", "Error adding document", e)
//                }
//        }
//    }
//
//    fun generateServicePackages(categoryId: String): List<ServicePackage> {
//        return listOf(
//            ServicePackage(
//                categoryId = categoryId,
//                name = "Gói sửa chữa $categoryId 1",
//                imageUrl = "url_to_image",
//                price = "500,000 VND",
//                description = "Mô tả cho gói dịch vụ 1 của $categoryId."
//            ),
//            ServicePackage(
//                categoryId = categoryId,
//                name = "Gói sửa chữa $categoryId 2",
//                imageUrl = "url_to_image",
//                price = "700,000 VND",
//                description = "Mô tả cho gói dịch vụ 2 của $categoryId."
//            ),
//            ServicePackage(
//                categoryId = categoryId,
//                name = "Gói sửa chữa $categoryId 3",
//                imageUrl = "url_to_image",
//                price = "600,000 VND",
//                description = "Mô tả cho gói dịch vụ 3 của $categoryId."
//            ),
//            ServicePackage(
//                categoryId = categoryId,
//                name = "Gói sửa chữa $categoryId 4",
//                imageUrl = "url_to_image",
//                price = "800,000 VND",
//                description = "Mô tả cho gói dịch vụ 4 của $categoryId."
//            ),
//            ServicePackage(
//                categoryId = categoryId,
//                name = "Gói sửa chữa $categoryId 5",
//                imageUrl = "url_to_image",
//                price = "900,000 VND",
//                description = "Mô tả cho gói dịch vụ 5 của $categoryId."
//            )
//        )
//    }

}