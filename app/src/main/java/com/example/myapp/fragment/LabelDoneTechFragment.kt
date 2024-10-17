package com.example.myapp.fragment

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.adapter.JobAdapter
import com.example.myapp.model.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LabelDoneTechFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var jobAdapter: JobAdapter
    private lateinit var searchView: SearchView
    private lateinit var imageButtonMicrophone: ImageButton
    private val jobs = mutableListOf<Order>()
    private val REQUEST_CODE_VOICE_RECOGNITION = 100
    private var filteredJobs = mutableListOf<Order>()  // Danh sách tạm thời lưu trữ kết quả lọc
    private lateinit var tvFilterDate: TextView
    private lateinit var tvNoData: TextView
    private var selectedDate: Date? = null
    private var currentQuery: String = ""  // Lưu trữ từ khóa tìm kiếm hiện tại
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.fragment_label_done_tech, container, false)
        tvNoData = mView.findViewById(R.id.tv_no_data)
        imageButtonMicrophone = mView.findViewById(R.id.image_button_microphone)
        recyclerView = mView.findViewById(R.id.rcv_label_done_tech)
        recyclerView.layoutManager = LinearLayoutManager(context)
        jobAdapter = JobAdapter(jobs)
        recyclerView.adapter = jobAdapter

        loadJobs()

        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpSearchView()
        tvFilterDate = view.findViewById(R.id.tv_filter_date)
        // Khởi tạo với ngày hiện tại
        val calendar = Calendar.getInstance()
        selectedDate = calendar.time // Ngày hiện tại
        // Hiển thị ngày hiện tại trong TextView
        tvFilterDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)
        tvFilterDate.setOnClickListener {
            showDatePickerDialog()
        }

        imageButtonMicrophone.setOnClickListener {
            openVoiceRecognizer()
        }
    }

    private fun openVoiceRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói điều gì đó...")
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE_RECOGNITION)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Không hỗ trợ nhận diện giọng nói.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_VOICE_RECOGNITION && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.let {
                if (it.isNotEmpty()) {
                    // Điền kết quả vào SearchView
                    searchView?.setQuery(it[0], false)
                }
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                selectedDate = calendar.time
                tvFilterDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)

                // Lọc đơn hàng theo ngày đã chọn
                filterJobs()
            }, year, month, day)

        datePickerDialog.show()
    }

    private fun setUpSearchView() {
        searchView = requireView().findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                // Lọc đơn hàng khi từ khóa tìm kiếm thay đổi
                filterJobs()
                return true
            }
        })
    }

    private fun filterJobs() {
        // Lọc theo từ khóa tìm kiếm
        val tempFilteredOrders = jobs.filter { order ->
            order.id?.contains(currentQuery, ignoreCase = true) ?: false ||
                    order.namePackage?.contains(currentQuery, ignoreCase = true) ?: false
        }

        // Lọc theo ngày nếu ngày đã được chọn
        selectedDate?.let { date ->
            filteredJobs = tempFilteredOrders.filter { order ->
                order.updatedAt?.let { updatedAt ->
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(updatedAt) == sdf.format(date)
                } ?: false
            }.toMutableList()
        } ?: run {
            // Nếu không chọn ngày, chỉ lọc theo từ khóa tìm kiếm
            filteredJobs = tempFilteredOrders.toMutableList()
        }

        // Cập nhật UI với kết quả lọc
        updateUI(filteredJobs)
    }

    private fun updateUI(jobList: List<Order>) {
        if (jobList.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvNoData.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvNoData.visibility = View.GONE
            jobAdapter = JobAdapter(jobList)
            recyclerView.adapter = jobAdapter
        }
    }

    private fun loadJobs() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()


        db.collection("orders")
            .whereEqualTo("id_technician", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Xử lý lỗi nếu cần
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    jobs.clear()
                    for (document in snapshot.documents) {
                        val order = document.toObject(Order::class.java)
                        if (order != null && (order.status == "completed" || order.status == "finish" || order.status == "cancel")) { // Kiểm tra trạng thái
                            jobs.add(order)
                        }
                    }
                    // Cập nhật danh sách lọc khi dữ liệu thay đổi
                    filterJobs()
                }
            }
    }

}