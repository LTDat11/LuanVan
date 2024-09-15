package com.example.myapp.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.activity.DeviceListAdminActivity
import com.example.myapp.adapter.CategoryAdminAdapter
import com.example.myapp.model.ServiceCategory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryAdminFragment : Fragment() {
    private var mView: View? = null
    private var searchView: SearchView? = null
    private var fab : FloatingActionButton? = null

    private lateinit var recyclerViewCategory : RecyclerView
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var categoryAdminAdapter: CategoryAdminAdapter

    private val categoryMap = mutableMapOf<String, String>()
    // Khởi tạo mảng lưu các idDevice
    private val deviceIdsToDelete = mutableListOf<String>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_category_admin, container, false)
        initUi()
        initToolbar()
        setupRecyclerView()
        loadCategories()
        initListeners()
        // Inflate the layout for this fragment
        return mView
    }

    private fun initListeners() {
        fab?.setOnClickListener {
            showAddCategoryDialog()
        }

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCategories(newText ?: "")
                return false
            }
        })

    }

    private fun filterCategories(query: String) {
        val filteredCategories = if (query.isEmpty()) {
            categoryMap
        } else {
            categoryMap.filter {
                it.value.contains(query, ignoreCase = true)
            }
        }
        categoryAdminAdapter.updateCategories(filteredCategories)
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_category, null)

        val etCategoryName = dialogView.findViewById<TextView>(R.id.et_category_name)
        val etCategoryDescription = dialogView.findViewById<TextView>(R.id.et_category_description)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Thêm danh mục dịch vụ")
            .setPositiveButton("Thêm") { _, _ ->
                val name = etCategoryName.text.toString()
                val description = etCategoryDescription.text.toString()

                if (name.isNotEmpty() && description.isNotEmpty()) {
                    addCategory(name, description)
                } else {
                    // Hiển thị thông báo lỗi
                    Toast.makeText(context, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy") { _, _ -> }
            .create()
        dialog.show()
    }

    private fun addCategory(name: String, description: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                // Tạo một document mới với ID được tự động sinh ra
                val newCategoryRef = firestore.collection("service_categories").document()
                val categoryId = newCategoryRef.id // Lấy ID của document

                // Tạo đối tượng ServiceCategory với ID vừa tạo
                val newCategory = ServiceCategory(
                    id = categoryId,
                    name = name,
                    description = description
                )

                // Thêm document vào Firestore với ID đã xác định
                newCategoryRef.set(newCategory)
                    .addOnSuccessListener {
                        // Xử lý khi thêm thành công
                        Toast.makeText(context, "Danh mục dịch vụ đã được thêm thành công", Toast.LENGTH_SHORT).show()
                        loadCategories()

                        // Kiểm tra xem subcollection 'devices' có document nào không
                        newCategoryRef.collection("devices").get()
                            .addOnSuccessListener { querySnapshot ->
                                if (querySnapshot.isEmpty) {
                                    // Nếu subcollection 'devices' rỗng, chuyển đến DeviceListAdminActivity
                                    val intent = Intent(context, DeviceListAdminActivity::class.java)
                                    intent.putExtra("categoryId", categoryId)
                                    intent.putExtra("categoryName", name)
                                    startActivity(intent)
                                } else {
                                    // Xử lý nếu subcollection 'devices' không rỗng
                                    Toast.makeText(context, "Danh mục đã có thiết bị", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                // Xử lý khi có lỗi trong quá trình truy vấn subcollection
                                Toast.makeText(context, "Lỗi khi kiểm tra thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        // Xử lý khi có lỗi
                        Toast.makeText(context, "Lỗi khi thêm danh mục dịch vụ: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

            }
        }

    }

    private fun setupRecyclerView() {
        categoryAdminAdapter = CategoryAdminAdapter(categoryMap,
            onEditClick = { categoryId, categoryName ->
                getInfoCategory(categoryId, categoryName)
            },
            onDeleteClick = { categoryId ->
                showDialogDeleteCategory(categoryId)
                searchView?.setQuery("", false)
            },
            )
        recyclerViewCategory.layoutManager = LinearLayoutManager(this.context)
        recyclerViewCategory.adapter = categoryAdminAdapter
    }

    private fun getInfoCategory(categoryId: String, categoryName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                // Lấy thông tin danh mục dịch vụ từ Firestore
                val categoryRef = firestore.collection("service_categories").document(categoryId)
                categoryRef.get().addOnSuccessListener { document ->
                    val category = document.toObject(ServiceCategory::class.java)
                    if (category != null) {
                        showEditCategoryDialog(category, categoryName)
                    } else {
                        Toast.makeText(context, "Không tìm thấy danh mục dịch vụ", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Lỗi khi truy cập danh mục dịch vụ: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            }
        }

    }

    private fun showEditCategoryDialog(category: ServiceCategory, categoryName: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_category, null)

        val etCategoryName = dialogView.findViewById<TextView>(R.id.et_category_name)
        val etCategoryDescription = dialogView.findViewById<TextView>(R.id.et_category_description)

        etCategoryName.text = category.name
        etCategoryDescription.text = category.description

        val dialog = context?.let {
            AlertDialog.Builder(it)
                .setView(dialogView)
                .setTitle("Sửa danh mục dịch vụ")
                .setPositiveButton("Sửa") { _, _ ->
                    val newName = etCategoryName.text.toString()
                    val newDescription = etCategoryDescription.text.toString()

                    if (newName != category.name || newDescription != category.description) {
                        if (newName.isNotEmpty() && newDescription.isNotEmpty()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                withContext(Dispatchers.Main){
                                    val categoryRef = firestore.collection("service_categories").document(category.id)
                                    categoryRef.update("name", newName, "description", newDescription)
                                        .addOnSuccessListener {
                                            // Xử lý khi sửa thành công
                                            loadCategories()
                                        }
                                        .addOnFailureListener { e ->
                                            // Xử lý khi có lỗi
                                            Toast.makeText(context, "Lỗi khi sửa danh mục dịch vụ: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        } else {
                            // Hiển thị thông báo lỗi
                            Toast.makeText(context, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                        }
                    }else {
                        // Hiển thị thông báo lỗi
                        Toast.makeText(context, "Không có thay đổi nào được thực hiện", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Hủy") { _, _ -> }
                .create()
        }
        if (dialog != null) {
            dialog.show()
        }

    }

    private fun showDialogDeleteCategory(categoryId: String) {
        val alertDialog = this.context?.let {
            AlertDialog.Builder(it)
                .setTitle("Xóa Danh Mục Dịch Vụ")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục dịch vụ này không? Tất cả các thiết bị và gói dịch vụ liên quan sẽ bị xóa vĩnh viễn.")
                .setPositiveButton("Xóa") { _, _ ->
                    deleteCategory(categoryId)
                }
                .setNegativeButton("Hủy", null)
                .create()
        }
        if (alertDialog != null) {
            alertDialog.show()
        }
    }

    private fun deleteCategory(categoryId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val firestore = FirebaseFirestore.getInstance()
                val categoryRef = firestore.collection("service_categories").document(categoryId)

                // Hàm để xóa một collection và các document bên trong nó
                fun deleteSubCollection(collectionRef: CollectionReference, onComplete: () -> Unit) {
                    collectionRef.get().addOnSuccessListener { querySnapshot ->
                        val batch = firestore.batch() // Dùng batch để xóa nhiều document cùng lúc
                        for (document in querySnapshot.documents) {
                            batch.delete(document.reference)
                        }

                        // Thực hiện batch delete
                        batch.commit().addOnSuccessListener {
                            onComplete() // Sau khi xóa xong, gọi lại để tiếp tục xóa các collection tiếp theo
                        }.addOnFailureListener { e ->
                            Toast.makeText(context, "Lỗi khi xóa sub-collection: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(context, "Lỗi khi truy cập sub-collection: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // Hàm xóa toàn bộ các thiết bị và các gói dịch vụ của chúng trước khi xóa category
                fun deleteDevicesAndPackages(devicesRef: CollectionReference, onComplete: () -> Unit) {
                    devicesRef.get().addOnSuccessListener { querySnapshot ->
                        var deleteCount = 0
                        if (querySnapshot.isEmpty) {
                            // Nếu không có thiết bị, xóa category luôn
                            onComplete()
                            return@addOnSuccessListener
                        }
                        for (deviceDoc in querySnapshot.documents) {
                            val deviceId = deviceDoc.id
                            deviceIdsToDelete.add(deviceId) // Lưu idDevice vào mảng để sau này xóa ảnh

                            val servicePackagesRef = deviceDoc.reference.collection("service_packages")
                            deleteSubCollection(servicePackagesRef) {
                                deviceDoc.reference.delete().addOnSuccessListener {
                                    deleteCount++
                                    // Nếu đã xóa hết các thiết bị, gọi onComplete
                                    if (deleteCount == querySnapshot.size()) {
                                        onComplete()
                                    }
                                }.addOnFailureListener { e ->
                                    Toast.makeText(context, "Lỗi khi xóa thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(context, "Lỗi khi truy cập thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // Xóa các thiết bị (devices) và gói dịch vụ (service_packages)
                val devicesRef = categoryRef.collection("devices")
                deleteDevicesAndPackages(devicesRef) {
                    // Sau khi đã xóa toàn bộ devices và service_packages, tiếp tục xóa category
                    categoryRef.delete().addOnSuccessListener {
                        Toast.makeText(context, "Danh mục và các sub-collection đã được xóa", Toast.LENGTH_SHORT).show()

                        // Gọi hàm xóa tất cả ảnh sau khi đã xóa document
                        deleteAllDeviceImages()
                    }.addOnFailureListener { e ->
                        Toast.makeText(context, "Lỗi khi xóa danh mục: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

    }


    private fun deleteAllDeviceImages() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val storage = FirebaseStorage.getInstance()

                for (deviceId in deviceIdsToDelete) {
                    val imageRef = storage.reference.child("device/$deviceId/$deviceId.jpg")
                    imageRef.delete().addOnSuccessListener {
                        Log.d("deleteDeviceImage", "Đã xóa ảnh cho thiết bị: $deviceId")
                    }.addOnFailureListener { e ->
                        Log.e("deleteDeviceImage", "Lỗi khi xóa ảnh cho thiết bị $deviceId: ${e.message}")
                    }
                }

                // Sau khi hoàn tất xóa ảnh, làm trống mảng
                deviceIdsToDelete.clear()

                // Thông báo sau khi đã xóa hết ảnh
                Toast.makeText(context, "Đã xóa tất cả ảnh của thiết bị và làm trống danh sách", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                firestore.collection("service_categories")
                    .addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            Log.e("HomeAdminFragment", "Error getting documents.", exception)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            categoryMap.clear()
                            for (document in snapshot.documents) {
                                val category = document.toObject(ServiceCategory::class.java)
                                category?.let {
                                    categoryMap[document.id] = "${it.name},${it.description}"
                                }
                            }
                            // Cập nhật adapter khi có sự thay đổi
                            categoryAdminAdapter.updateCategories(categoryMap)
                        }
                    }
            }
        }
    }

    private fun initToolbar() {
        val tvToolbarTitle = mView?.findViewById<TextView>(R.id.tv_toolbar_title)
        tvToolbarTitle?.text = getString(R.string.nav_category_admin)
    }

    private fun initUi() {
        searchView = mView?.findViewById(R.id.search_view)
        fab = mView?.findViewById(R.id.fab_add)
        recyclerViewCategory = mView?.findViewById(R.id.recycler_view_category)!!
    }
}