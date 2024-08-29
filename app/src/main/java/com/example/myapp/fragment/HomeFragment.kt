package com.example.myapp.fragment

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class HomeFragment : Fragment() {
    private var mView: View? = null
    private var tabCategory: TabLayout? = null
    private var viewPager: ViewPager2? = null
    private var indicator: CircleIndicator3? = null
    private val listPhotoBanners = mutableListOf<PhotoBanner>()
    private lateinit var mHandlerBanner: Handler
    private lateinit var mRunnableBanner: Runnable

    //carts
    private lateinit var layoutCart: View
    private lateinit var tvCountItem: TextView
    private lateinit var tvPackageName: TextView
    private lateinit var tvAmount: TextView
    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var recycler_view_packages: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()

    private var cartListener: ValueEventListener? = null


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
        setupCartListener() // Check and display cart layout
        return mView
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
                        val selectedCategory = tab.text.toString()
                        loadDevicesForCategory(selectedCategory)
                    }
                }

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
                    setupServicePackageRecyclerView(servicePackages)
                }
            }
    }

    private fun setupServicePackageRecyclerView(servicePackages: List<ServicePackage>) {
        val adapter = ServicePackageAdapter(servicePackages)
        recycler_view_packages.layoutManager = LinearLayoutManager(requireContext())
        recycler_view_packages.adapter = adapter
    }

    private fun setupCartListener() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid
        tvAmount.text = "Giỏ hàng của bạn"
        uid?.let {
            val database = FirebaseDatabase.getInstance().reference
            val userCartRef = database.child("carts").child(it)

            cartListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        layoutCart.visibility = View.VISIBLE
                        val itemCount = snapshot.childrenCount
                        tvCountItem.text = "Số lượng: $itemCount"

                        // Tạo một danh sách để chứa tên của các package
                        val packageNames = mutableListOf<String>()

                        // Lặp qua các phần tử trong snapshot và lấy tên của các package
                        for (packageSnapshot in snapshot.children) {
                            val packageName = packageSnapshot.child("name").getValue(String::class.java)
                            packageName?.let { packageNames.add(it) }
                        }

                        // Kết hợp các tên thành một chuỗi và hiển thị trên tvPackageName
                        tvPackageName.text = packageNames.joinToString(", ")


                    } else {
                        layoutCart.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle potential errors
                }
            }

            userCartRef.addValueEventListener(cartListener!!)
        } ?: run {
            layoutCart.visibility = View.GONE
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the cart listener to avoid memory leaks
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        uid?.let {
            val database = FirebaseDatabase.getInstance().reference
            val userCartRef = database.child("carts").child(it)
            cartListener?.let { listener -> userCartRef.removeEventListener(listener) }
        }
    }


    private fun initUi() {
        tabCategory = mView?.findViewById(R.id.tab_category)
        viewPager = mView?.findViewById(R.id.view_pager_banner)
        recyclerViewDevices = mView?.findViewById(R.id.recycler_view_devices)!!
        recycler_view_packages = mView?.findViewById(R.id.recycler_view_packages)!!
        indicator = mView?.findViewById(R.id.indicator)

        layoutCart = mView?.findViewById(R.id.layout_cart) ?: return
        tvCountItem = mView?.findViewById(R.id.tv_count_item) ?: return
        tvPackageName = mView?.findViewById(R.id.tv_package_name) ?: return
        tvAmount = mView?.findViewById(R.id.tv_amount) ?: return

        layoutCart.setOnClickListener {
            // Navigate to the cart screen

        }
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

                                // Thêm các service packages cho mỗi device
                                val servicePackages = generateServicePackages(categoryId, device.name)
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

    fun generateServicePackages(categoryId: String, deviceId: String): List<ServicePackage> {
        return listOf(
            ServicePackage(
                categoryId = categoryId,
                deviceId = deviceId,
                name = "Gói dịch vụ cho $deviceId 1",
                imageUrl = "url_to_image",
                price = "500,000 VND",
                description = "Mô tả cho gói dịch vụ 1 của $deviceId."
            ),
            ServicePackage(
                categoryId = categoryId,
                deviceId = deviceId,
                name = "Gói dịch vụ cho $deviceId 2",
                imageUrl = "url_to_image",
                price = "700,000 VND",
                description = "Mô tả cho gói dịch vụ 2 của $deviceId."
            ),
            ServicePackage(
                categoryId = categoryId,
                deviceId = deviceId,
                name = "Gói dịch vụ cho $deviceId 3",
                imageUrl = "url_to_image",
                price = "600,000 VND",
                description = "Mô tả cho gói dịch vụ 3 của $deviceId."
            ),
            ServicePackage(
                categoryId = categoryId,
                deviceId = deviceId,
                name = "Gói dịch vụ cho $deviceId 4",
                imageUrl = "url_to_image",
                price = "800,000 VND",
                description = "Mô tả cho gói dịch vụ 3 của $deviceId."
            )
        )
    }

}