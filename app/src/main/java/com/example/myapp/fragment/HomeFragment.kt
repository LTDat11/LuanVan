package com.example.myapp.fragment

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.PhotoBannerAdapter
import com.example.myapp.model.PhotoBanner
import com.example.myapp.model.ServiceCategory
import com.example.myapp.model.ServicePackage
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import me.relex.circleindicator.CircleIndicator3
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.adapter.DeviceAdapter
import com.example.myapp.adapter.ServicePackageAdapter
import com.example.myapp.model.Device


class HomeFragment : Fragment() {
    private var mView: View? = null
    private var tabCategory: TabLayout? = null
    private var viewPager: ViewPager2? = null
    private var indicator: CircleIndicator3? = null
    private var searchView: SearchView? = null
    private val listPhotoBanners = mutableListOf<PhotoBanner>()
    private lateinit var mHandlerBanner: Handler
    private lateinit var mRunnableBanner: Runnable

    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var recycler_view_packages: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()
    //searchview
    private val devicePackagesMap = mutableMapOf<String, List<ServicePackage>>() // Lưu danh sách gói dịch vụ cho từng thiết bị
    private var currentDeviceId: String? = null // Lưu trữ ID của thiết bị hiện tại
    private var currentSearchText: String = "" // Biến lưu trữ nội dung tìm kiếm hiện tại




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        addServiceCategoriesDevicesAndPackages()
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_home, container, false)
        initUi()  // Initialize UI components
        setupTabLayout()
        getListPhotoBanners() // Get list of photo banners
        setupSearchView() // Setup search view
        return mView
    }

    private fun setupSearchView() {
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Xử lý khi người dùng nhấn enter hoặc nút tìm kiếm trên bàn phím
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // Xử lý khi người dùng thay đổi nội dung của SearchView
                currentSearchText = newText // Lưu giá trị tìm kiếm hiện tại
                filter(currentSearchText)
                return true
            }
        })
    }

    private fun filter(text: String) {
        // Lấy danh sách gốc của thiết bị hiện tại từ devicePackagesMap
        val originalPackages = devicePackagesMap[currentDeviceId] ?: listOf()

        val filteredList: List<ServicePackage> = if (text.isEmpty()) {
            // Nếu query rỗng, hiển thị lại toàn bộ danh sách gốc
            originalPackages
        } else {
            // Lọc các gói dịch vụ dựa trên tên hoặc các thuộc tính khác
            originalPackages.filter {
                it.name.contains(text, ignoreCase = true)
            }
        }

        // Cập nhật lại dữ liệu cho RecyclerView
        recycler_view_packages.adapter?.let { adapter ->
            if (adapter is ServicePackageAdapter) {
                adapter.packages = filteredList
                adapter.notifyDataSetChanged()
            }
        }
    }


    private fun setupTabLayout() {
        firestore.collection("service_categories").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e)
                return@addSnapshotListener
            }

            snapshot?.let {
                tabCategory?.removeAllTabs()
                for (document in it.documents) {
                    val category = document.toObject(ServiceCategory::class.java)
                    val tab = tabCategory?.newTab()?.setText(category?.name)
                    if (tab != null) {
                        tabCategory?.addTab(tab)
                    }
                }
                // Đặt tab đầu tiên làm mặc định
                tabCategory?.getTabAt(0)?.select()

                // Gọi loadDevicesForCategory cho tab đầu tiên để hiển thị dữ liệu đúng
                val firstTabCategory = tabCategory?.getTabAt(0)?.text.toString()
                loadDevicesForCategory(firstTabCategory)

                // Set listener for tab selection
                tabCategory?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        tab?.let {
                            val selectedCategory = tab.text.toString()
                            loadDevicesForCategory(selectedCategory)
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
            }
        }
        //Đặt tab đầu tiên làm mặc định
        val firstTabCategory = tabCategory?.getTabAt(0)?.text.toString()
        loadDevicesForCategory(firstTabCategory)
    }

    private fun loadDevicesForCategory(selectedCategory: String) {
        firestore.collection("service_categories")
            .whereEqualTo("name", selectedCategory)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                snapshot?.let {
                    if (it.isEmpty) return@addSnapshotListener
                    val categoryId = it.documents[0].id
                    firestore.collection("service_categories")
                        .document(categoryId)
                        .collection("devices")
                        .addSnapshotListener { deviceDocs, e ->
                            if (e != null) {
                                Log.w("Firestore", "Listen failed.", e)
                                return@addSnapshotListener
                            }

                            deviceDocs?.let {
                                val devices = it.map { doc -> doc.toObject(Device::class.java) }
                                setupDeviceRecyclerView(devices, categoryId)
                            }
                        }
                }
            }
    }

    private fun setupDeviceRecyclerView(devices: List<Device>, categoryId: String) {
        val adapter = DeviceAdapter(devices) { selectedDevice ->
            loadServicePackagesForDevice(categoryId, selectedDevice.id_device)
        }
        recyclerViewDevices.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerViewDevices.adapter = adapter

        // Chọn thiết bị đầu tiên mặc định
        recyclerViewDevices.post {
            if (devices.isNotEmpty()) {
                val firstDevice = devices[0]
                adapter.selectedPosition = 0
                adapter.notifyDataSetChanged()
                loadServicePackagesForDevice(categoryId, firstDevice.id_device)
            }
        }
    }

    private fun loadServicePackagesForDevice(categoryId: String, idDevice: String) {
        firestore.collection("service_categories")
            .document(categoryId)
            .collection("devices")
            .document(idDevice)
            .collection("service_packages")
            .addSnapshotListener { servicePackageDocs, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                servicePackageDocs?.let {
                    val servicePackages = it.map { doc -> doc.toObject(ServicePackage::class.java) }
                    // Lưu danh sách gốc vào devicePackagesMap
                    devicePackagesMap[idDevice] = servicePackages
                    currentDeviceId = idDevice // Cập nhật ID thiết bị hiện tại
                    // Cập nhật RecyclerView với danh sách mới
                    setupServicePackageRecyclerView(servicePackages)
                    // Áp dụng lại bộ lọc nếu có nội dung tìm kiếm hiện tại
                    if (currentSearchText.isNotEmpty()) {
                        filter(currentSearchText)
                    }
                }
            }
    }

    private fun setupServicePackageRecyclerView(servicePackages: List<ServicePackage>) {
        val adapter = ServicePackageAdapter(servicePackages)
        recycler_view_packages.layoutManager = LinearLayoutManager(requireContext())
        recycler_view_packages.adapter = adapter
    }


    private fun initUi() {
        tabCategory = mView?.findViewById(R.id.tab_category)
        viewPager = mView?.findViewById(R.id.view_pager_banner)
        searchView = mView?.findViewById(R.id.search_view)
        recyclerViewDevices = mView?.findViewById(R.id.recycler_view_devices)!!
        recycler_view_packages = mView?.findViewById(R.id.recycler_view_packages)!!
        indicator = mView?.findViewById(R.id.indicator)

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




//    fun addServiceCategoriesAndPackages have device (now)

    fun addServiceCategoriesDevicesAndPackages() {
        val categories = listOf(
            ServiceCategory(
                name = "Bảo trì",
                description = "Dịch vụ bảo trì thiết bị.",
                price_range = "500,000 - 2,000,000 VND"
            ),
            ServiceCategory(
                name = "Sửa chữa nhỏ",
                description = "Dịch vụ sửa chữa nhỏ các thiết bị.",
                price_range = "300,000 - 1,500,000 VND"
            ),
            ServiceCategory(
                name = "Thay thế linh kiện",
                description = "Dịch vụ thay thế linh kiện cho thiết bị.",
                price_range = "200,000 - 3,000,000 VND"
            )
        )

        val devices = listOf(
            Device(
                name = "Tivi",
                price = "500,000 VND",
                categoryId = "" // This will be set when the category is created
            ),
            Device(
                name = "Tủ lạnh",
                price = "1,000,000 VND",
                categoryId = "" // This will be set when the category is created
            ),
            Device(
                name = "Máy giặt",
                price = "800,000 VND",
                categoryId = "" // This will be set when the category is created
            ),
            Device(
                name = "Máy lạnh",
                price = "600,000 VND",
                categoryId = "" // This will be set when the category is created
            ),
            Device(
                name = "Điện gia dụng",
                price = "300,000 VND",
                categoryId = "" // This will be set when the category is created
            )
        )

        val firestore = FirebaseFirestore.getInstance()

        for (category in categories) {
            val categoryId = firestore.collection("service_categories").document().id
            val categoryWithId = category.copy(id = categoryId)

            firestore.collection("service_categories")
                .document(categoryId)
                .set(categoryWithId)
                .addOnSuccessListener {
                    Log.d("Firestore", "DocumentSnapshot written with ID: $categoryId")

                    // Thêm các device cho mỗi category
                    for (device in devices) {
                        val deviceId = firestore.collection("service_categories")
                            .document(categoryId)
                            .collection("devices")
                            .document().id
                        val deviceWithCategory = device.copy(id_device = deviceId, categoryId = categoryId)

                        firestore.collection("service_categories")
                            .document(categoryId)
                            .collection("devices")
                            .document(deviceId)
                            .set(deviceWithCategory)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Device added with ID: $deviceId")
                                // lấy tên category và device
                                val categoryName = category.name
                                val deviceName = device.name
                                // Thêm các service packages cho mỗi device
                                val servicePackages = generateServicePackages(categoryId, deviceId, categoryName, deviceName)
                                for (servicePackage in servicePackages) {
                                    val packageId = firestore.collection("service_categories")
                                        .document(categoryId)
                                        .collection("devices")
                                        .document(deviceId)
                                        .collection("service_packages")
                                        .document().id
                                    val servicePackageWithId = servicePackage.copy(id = packageId)

                                    firestore.collection("service_categories")
                                        .document(categoryId)
                                        .collection("devices")
                                        .document(deviceId)
                                        .collection("service_packages")
                                        .document(packageId)
                                        .set(servicePackageWithId)
                                        .addOnSuccessListener {
                                            Log.d("Firestore", "ServicePackage added with ID: $packageId")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("Firestore", "Error adding service package", e)
                                        }

                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error adding device", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error adding category", e)
                }
        }
    }

    fun generateServicePackages(
        categoryId: String,
        deviceId: String,
        categoryName: String,
        deviceName: String, ): List<ServicePackage> {
        return listOf(
            ServicePackage(
                categoryId = categoryId,
                deviceId = deviceId,
                name = "Gói dịch vụ $categoryName cho $deviceName 1",
                imageUrl = "url_to_image",
                price = "500,000 VND",
                description = "Mô tả cho gói dịch vụ 1 của $deviceName."
            ),
            ServicePackage(
                categoryId = categoryId,
                deviceId = deviceId,
                name = "Gói dịch vụ $categoryName cho $deviceName 2",
                imageUrl = "url_to_image",
                price = "700,000 VND",
                description = "Mô tả cho gói dịch vụ 2 của $deviceName."
            ),
            ServicePackage(
                categoryId = categoryId,
                deviceId = deviceId,
                name = "Gói dịch vụ $categoryName cho $deviceName 3",
                imageUrl = "url_to_image",
                price = "600,000 VND",
                description = "Mô tả cho gói dịch vụ 3 của $deviceName."
            ),
            ServicePackage(
                categoryId = categoryId,
                deviceId = deviceId,
                name = "Gói dịch vụ $categoryName cho $deviceName 4",
                imageUrl = "url_to_image",
                price = "800,000 VND",
                description = "Mô tả cho gói dịch vụ 3 của $deviceName."
            )
        )
    }

}