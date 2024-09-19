package com.example.myapp.fragment

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.DeviceAdapter
import com.example.myapp.adapter.DeviceAdminAdapter
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var recyclerViewPackages: RecyclerView
    private lateinit var tvEmptyPackage: TextView

    // Hai biến lưu dữ liệu của tab và thiết bị hiện tại
    private var selectedTabId: String? = null // ID của tab hiện tại
    private var selectedDeviceId: String? = null // ID của thiết bị hiện tại

    private var currentSearchText: String = "" // Biến lưu trữ nội dung tìm kiếm hiện tại

    private val categoryMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_home_admin, container, false)
        initUi()
        loadCategories()
        getListPhotoBanners()

        innitListener()
        setupSearchView()
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
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                if (selectedDeviceId.isNullOrEmpty()) {
                    // Nếu chưa chọn thiết bị thì không thực hiện tìm kiếm
                    return@withContext
                }
                // Nếu đã chọn thiết bị thì tiến hành tìm kiếm
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("service_categories")
                    .document(selectedTabId ?: "")
                    .collection("devices")
                    .document(selectedDeviceId ?: "")
                    .collection("service_packages")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val packages = mutableListOf<ServicePackage>()
                        for (document in snapshot.documents) {
                            val servicePackage = document.toObject(ServicePackage::class.java)
                            servicePackage?.let {
                                packages.add(it)
                            }
                        }
                        val filteredPackages = packages.filter {
                            it.name.contains(text, ignoreCase = true) ||
                                    it.description.contains(text, ignoreCase = true) ||
                                    it.price.contains(text, ignoreCase = true)
                        }
                        updateServicePackageRecyclerView(filteredPackages)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("HomeAdminFragment", "Error getting service packages.", exception)
                    }
            }
        }
    }


    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("service_categories")
                    .addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            Log.e("HomeAdminFragment", "Error getting documents.", exception)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            categoryMap.clear()
                            tabCategory?.removeAllTabs()
                            for (document in snapshot.documents) {
                                val category = document.toObject(ServiceCategory::class.java)
                                category?.let {
                                    categoryMap[document.id] = it.name
                                    tabCategory?.newTab()?.setText(it.name)
                                        ?.let { it1 -> tabCategory?.addTab(it1) }
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun innitListener() {
        fab?.setOnClickListener {
            if (selectedDeviceId.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn thiết bị để thêm gói dịch vụ", Toast.LENGTH_SHORT).show()
            } else {
                showAddServicePackageDialog()
            }
        }

        tabCategory?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val tabId = categoryMap.entries.find { it.value == tab.text }?.key
                    tabId?.let { id ->
//                        Toast.makeText(requireContext(), "Tab ID: $id", Toast.LENGTH_SHORT).show()
                        selectedTabId = id
                        selectedDeviceId = null // Reset selectedDeviceId

                        recyclerViewPackages.visibility = View.GONE
                        tvEmptyPackage.visibility = View.VISIBLE
                        tvEmptyPackage.text = "Vui lòng chọn thiết bị để xem gói dịch vụ"
                        loadDevicesForCategory(id)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Không cần xử lý cho sự kiện này
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Không cần xử lý cho sự kiện này
            }
        })
    }

    private fun loadDevicesForCategory(categoryId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("service_categories")
                    .document(categoryId)
                    .collection("devices")
                    .addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            Log.e("HomeAdminFragment", "Error getting devices.", exception)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val devices = mutableListOf<Device>()
                            for (document in snapshot.documents) {
                                val device = document.toObject(Device::class.java)
                                device?.let {
                                    devices.add(it)
                                }
                            }
                            updateDeviceRecyclerView(devices)
                        }
                    }
            }
        }
    }

    private fun updateDeviceRecyclerView(devices: List<Device>) {
        val deviceAdapter = DeviceAdminAdapter(devices, object : DeviceAdminAdapter.OnDeviceClickListener {
            override fun onDeviceClick(deviceId: String) {
                selectedDeviceId = deviceId
                //Toast.makeText(requireContext(), "Selected Device ID: $selectedDeviceId and tab id $selectedTabId", Toast.LENGTH_SHORT).show()
                // Fetch packages for the selected device and update UI
                loadServicePackagesForDevice(deviceId)
                filter(currentSearchText)
            }
        })
        recyclerViewDevices.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewDevices.adapter = deviceAdapter
    }

    private fun loadServicePackagesForDevice(deviceId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("service_categories")
            .document(selectedTabId ?: "")
            .collection("devices")
            .document(deviceId)
            .collection("service_packages")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("HomeAdminFragment", "Error getting service packages.", exception)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val packages = mutableListOf<ServicePackage>()
                    for (document in snapshot.documents) {
                        val servicePackage = document.toObject(ServicePackage::class.java)
                        servicePackage?.let {
                            packages.add(it)
                        }
                    }
                    updateServicePackageRecyclerView(packages)
                } else {
                    Log.d("HomeAdminFragment", "No service packages found.")
                }
            }
    }


    private fun updateServicePackageRecyclerView(packages: List<ServicePackage>) {
        if (packages.isNotEmpty()) {
            recyclerViewPackages.visibility = View.VISIBLE
            tvEmptyPackage.visibility = View.GONE

            val packageAdapter = ServicePackageAdminAdapter(packages)
            recyclerViewPackages.layoutManager = LinearLayoutManager(requireContext())
            recyclerViewPackages.adapter = packageAdapter
        } else {
            recyclerViewPackages.visibility = View.GONE
            tvEmptyPackage.text = "Không có gói dịch vụ nào"
            tvEmptyPackage.visibility = View.VISIBLE
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
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.Main){
                            addServicePackage(name, description, price, selectedTabId!!, selectedDeviceId!!)
                        }
                    }
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
                        loadServicePackagesForDevice(selectedDeviceId)
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
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
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

    private fun initUi() {
        tabCategory = mView?.findViewById(R.id.tab_category)
        viewPager = mView?.findViewById(R.id.view_pager_banner)
        searchView = mView?.findViewById(R.id.search_view)
        recyclerViewDevices = mView?.findViewById(R.id.recycler_view_devices)!!
        recyclerViewPackages = mView?.findViewById(R.id.recycler_view_packages)!!
        tvEmptyPackage = mView?.findViewById(R.id.tv_empty_package)!!
        indicator = mView?.findViewById(R.id.indicator)
        fab = mView?.findViewById(R.id.fab_add)
    }
}