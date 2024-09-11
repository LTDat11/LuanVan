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
import com.google.firebase.firestore.FirebaseFirestore

class CategoryAdminFragment : Fragment() {
    private var mView: View? = null
    private var searchView: SearchView? = null
    private var fab : FloatingActionButton? = null

    private lateinit var recyclerViewCategory : RecyclerView
    private lateinit var categoryAdapter: CategoryAdminAdapter
    private val firestore = FirebaseFirestore.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_category_admin, container, false)

        initToolbar()
        initRecyclerView()
        loadCategoriesFromFirestore()
        initListeners()


        // Inflate the layout for this fragment
        return mView
    }

    private fun initListeners() {
        fab = mView?.findViewById(R.id.fab_add)
        fab?.setOnClickListener {
            showDialogAddCategory()
        }

        // Lắng nghe sự kiện khi người dùng nhập từ khóa tìm kiếm
        searchView = mView?.findViewById(R.id.search_view)
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Không cần thực hiện gì khi nhấn Enter
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                categoryAdapter.filterCategories(newText.orEmpty())
                return true
            }
        })
    }

    private fun showDialogAddCategory() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)

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
                loadCategoriesFromFirestore()

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


    private fun initRecyclerView() {
        recyclerViewCategory  = mView?.findViewById(R.id.recycler_view_category)!!
        recyclerViewCategory.layoutManager = LinearLayoutManager(context)

        categoryAdapter = CategoryAdminAdapter(listOf(),
            onEditClick = { category -> showDialogEditCategory(category) },
            onDeleteClick = { category -> showDialogDeleteCategory(category)}
        )
        recyclerViewCategory.adapter = categoryAdapter
    }

    private fun showDialogDeleteCategory(category: ServiceCategory) {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Xóa Danh Mục Dịch Vụ")
            .setMessage("Bạn có chắc chắn muốn xóa danh mục dịch vụ này không? Tất cả các thiết bị và gói dịch vụ liên quan sẽ bị xóa vĩnh viễn.")
            .setPositiveButton("Xóa") { _, _ ->
                deleteServiceCategoryWithDevicesAndPackages(category.id)
            }
            .setNegativeButton("Hủy", null)
            .create()
        alertDialog.show()
    }

    private fun deleteServiceCategoryWithDevicesAndPackages(id: String) {
        val categoryRef = firestore.collection("service_categories").document(id)
        // Xóa tất cả các thiết bị và gói dịch vụ trước
        categoryRef.collection("devices").get().addOnSuccessListener { devicesSnapshot ->
            for (deviceDoc in devicesSnapshot) {
                val deviceId = deviceDoc.id
                val deviceRef = categoryRef.collection("devices").document(deviceId)

                // Xóa tất cả các gói dịch vụ của từng thiết bị
                deviceRef.collection("service_packages").get().addOnSuccessListener { packagesSnapshot ->
                    for (packageDoc in packagesSnapshot) {
                        deviceRef.collection("service_packages").document(packageDoc.id).delete()
                            .addOnSuccessListener {
                                Log.d("Firestore", "Đã xóa gói dịch vụ: ${packageDoc.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Lỗi khi xóa gói dịch vụ: ${e.message}")
                            }
                    }

                    // Sau khi xóa hết các gói dịch vụ, xóa thiết bị
                    deviceRef.delete().addOnSuccessListener {
                        Log.d("Firestore", "Đã xóa thiết bị: $deviceId")
                    }.addOnFailureListener { e ->
                        Log.e("Firestore", "Lỗi khi xóa thiết bị: ${e.message}")
                    }
                }
            }

            // Sau khi xóa hết các thiết bị, xóa service category
            categoryRef.delete().addOnSuccessListener {
                Toast.makeText(context, "Đã xóa danh mục dịch vụ thành công", Toast.LENGTH_SHORT).show()
                //set text of search view to empty
                searchView?.setQuery("", false)
                loadCategoriesFromFirestore()
            }.addOnFailureListener { e ->
                Log.e("Firestore", "Lỗi khi xóa danh mục dịch vụ: ${e.message}")
                Toast.makeText(context, "Lỗi khi xóa danh mục dịch vụ", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Lỗi khi truy cập danh sách thiết bị: ${e.message}")
        }
    }

    private fun showDialogEditCategory(category: ServiceCategory) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)

        val etCategoryName = dialogView.findViewById<TextView>(R.id.et_category_name)
        val etCategoryDescription = dialogView.findViewById<TextView>(R.id.et_category_description)

        etCategoryName.text = category.name
        etCategoryDescription.text = category.description

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Sửa danh mục dịch vụ")
            .setPositiveButton("Sửa") { _, _ ->
                val newName = etCategoryName.text.toString()
                val newDescription = etCategoryDescription.text.toString()

                if (newName != category.name || newDescription != category.description) {
                    if (newName.isNotEmpty() && newDescription.isNotEmpty()) {
                        val categoryRef = firestore.collection("service_categories").document(category.id)
                        categoryRef.update("name", newName, "description", newDescription)
                            .addOnSuccessListener {
                                // Xử lý khi sửa thành công
                                loadCategoriesFromFirestore()
                            }
                            .addOnFailureListener { e ->
                                // Xử lý khi có lỗi
                                Toast.makeText(context, "Lỗi khi sửa danh mục dịch vụ: ${e.message}", Toast.LENGTH_SHORT).show()
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
        dialog.show()

    }

    private fun loadCategoriesFromFirestore() {
        firestore.collection("service_categories")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val categories = querySnapshot.documents.map { document ->
                    ServiceCategory(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: ""
                    )
                }
                categoryAdapter.updateCategories(categories)
            }
            .addOnFailureListener { e ->
                // Xử lý khi có lỗi
            }
    }

    private fun initToolbar() {
        val tvToolbarTitle = mView?.findViewById<TextView>(R.id.tv_toolbar_title)
        tvToolbarTitle?.text = getString(R.string.nav_category_admin)
    }

}