package com.example.myapp.fragment

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.DeviceAdapter
import com.example.myapp.adapter.PhotoBannerAdapter
import com.example.myapp.adapter.ServicePackageAdapter
import com.example.myapp.adapter.ServicePackageAdminAdapter
import com.example.myapp.model.Device
import com.example.myapp.model.PhotoBanner
import com.example.myapp.model.ServiceCategory
import com.example.myapp.model.ServicePackage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import me.relex.circleindicator.CircleIndicator3
import java.text.NumberFormat
import java.util.Locale

class HomeAdminFragment : Fragment() {
    private var mView: View? = null
    private var tabCategory: TabLayout? = null
    private var viewPager: ViewPager2? = null
    private var indicator: CircleIndicator3? = null
    private var searchView: SearchView? = null
    private var fab : FloatingActionButton? = null
    private val listPhotoBanners = mutableListOf<PhotoBanner>()
    private lateinit var mHandlerBanner: Handler
    private lateinit var mRunnableBanner: Runnable

    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var recycler_view_packages: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()
    private val devicePackagesMap = mutableMapOf<String, List<ServicePackage>>() // Lưu danh sách gói dịch vụ cho từng thiết bị
    private var currentDeviceId: String? = null // Lưu trữ ID của thiết bị hiện tại
    private var currentSearchText: String = "" // Biến lưu trữ nội dung tìm kiếm hiện tại

    // Hai biến lưu dữ liệu của tab và thiết bị hiện tại
    private var selectedTabId: String? = null // ID của tab hiện tại
    private var selectedDeviceId: String? = null // ID của thiết bị hiện tại

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_home_admin, container, false)
        initUi()
        setupTabLayout()
        getListPhotoBanners()
        setupSearchView()
        innitListener()
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

    private fun innitListener() {
        fab?.setOnClickListener {
            showAddServicePackageDialog()
        }
    }

    private fun showAddServicePackageDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_service_package, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Thêm Gói Dịch Vụ")
            .setPositiveButton("Thêm") { _, _ ->
                // Xử lý khi nhấn nút Thêm
                val name = dialogView.findViewById<EditText>(R.id.et_package_name).text.toString()
                val description = dialogView.findViewById<EditText>(R.id.et_package_description).text.toString()
                val price = dialogView.findViewById<EditText>(R.id.et_package_price).text.toString()

                if (name.isNotEmpty() && description.isNotEmpty() && price.isNotEmpty()) {
                    // Thêm gói dịch vụ vào Firestore
                    addServicePackage(name, description, price, selectedTabId!!, selectedDeviceId!!)
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .create()
        dialog.show()
    }

    private fun addServicePackage(name: String, description: String, price: String, selectedTabId: String, selectedDeviceId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("service_categories")
            .document(selectedTabId)
            .collection("devices")
            .document(selectedDeviceId)
            .collection("service_packages").add(
                ServicePackage(
                    name = name,
                    description = description,
                    price = formatPrice(price),
                    categoryId = selectedTabId,
                    deviceId = selectedDeviceId,
                )
            ).addOnSuccessListener { documentReference ->
                val repairWithId = ServicePackage(
                    name = name,
                    description = description,
                    price = formatPrice(price),
                    categoryId = selectedTabId,
                    deviceId = selectedDeviceId,
                    id = documentReference.id
                )
                documentReference.set(repairWithId)
                    .addOnSuccessListener {
                        Log.d("HomeAdminFragment", "DocumentSnapshot added with ID: ${documentReference.id}")
                        Toast.makeText(requireContext(), "Thêm gói dịch vụ thành công", Toast.LENGTH_SHORT).show()
                        loadServicePackagesForDevice(selectedTabId, selectedDeviceId)
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeAdminFragment", "Error updating document", e)
                        Toast.makeText(requireContext(), "Có lỗi xảy ra, vui lòng thử lại sau", Toast.LENGTH_SHORT).show()
                    }

            }
            .addOnFailureListener { e ->
                Log.e("HomeAdminFragment", "Error adding document", e)
                Toast.makeText(requireContext(), "Có lỗi xảy ra, vui lòng thử lại sau", Toast.LENGTH_SHORT).show()
            }
    }


    private fun formatPrice(price: String): String {
        // Chuyển đổi chuỗi sang kiểu số nguyên
        val priceLong = price.toLongOrNull() ?: return "0 VND"

        // Sử dụng NumberFormat để định dạng theo locale Việt Nam
        val numberFormat = NumberFormat.getNumberInstance(Locale.US)
        val formattedPrice = numberFormat.format(priceLong)

        // Trả về chuỗi đã định dạng kèm với đơn vị tiền tệ VND
        return "$formattedPrice VND"
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

                // Cập nhật selectedTabId với ID của tab đầu tiên
                selectedTabId = snapshot.documents[0].id

                // Gọi loadDevicesForCategory cho tab đầu tiên để hiển thị dữ liệu đúng
                val firstTabCategory = tabCategory?.getTabAt(0)?.text.toString()
                loadDevicesForCategory(firstTabCategory)

                // Set listener for tab selection
                tabCategory?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        tab?.let {
                            val selectedCategory = tab.text.toString()
                            // Cập nhật selectedTabId với ID của tab được chọn
                            selectedTabId = snapshot.documents[it.position].id
                            loadDevicesForCategory(selectedCategory)
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
            }
        }
        // Đặt tab đầu tiên làm mặc định
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
            // Cập nhật selectedDeviceId khi thiết bị được chọn
            selectedDeviceId = selectedDevice.id_device
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
                selectedDeviceId = firstDevice.id_device // Cập nhật selectedDeviceId với thiết bị đầu tiên
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

    private fun filter(text: String) {
        // Lấy danh sách gốc của thiết bị hiện tại từ devicePackagesMap
        val originalPackages = devicePackagesMap[selectedDeviceId] ?: listOf()

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
            if (adapter is ServicePackageAdminAdapter) {
                adapter.packages = filteredList
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupServicePackageRecyclerView(servicePackages: List<ServicePackage>) {
        val adapter = ServicePackageAdminAdapter(servicePackages)
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
        fab = mView?.findViewById(R.id.fab_add)
    }
}