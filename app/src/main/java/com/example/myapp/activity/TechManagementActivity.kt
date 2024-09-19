package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.R
import com.example.myapp.adapter.UserManagementAdapter
import com.example.myapp.databinding.ActivityTechManagementBinding
import com.example.myapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TechManagementActivity : AppCompatActivity() {
    lateinit var binding: ActivityTechManagementBinding

    private lateinit var userManagementAdapter: UserManagementAdapter
    private var technicianList = mutableListOf<User>()
    private var registration: ListenerRegistration? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTechManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initRecyclerView()
        loadTechnicians()
        setupSearchView()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = technicianList.filter {
                    it.name?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.description?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.address?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.phone?.contains(newText ?: return@filter false, ignoreCase = true) ?: false ||
                            it.email.contains(newText ?: return@filter false, ignoreCase = true)
                }
                userManagementAdapter.updateList(filteredList)
                return true
            }
        })
    }

    private fun initRecyclerView() {
        userManagementAdapter = UserManagementAdapter(technicianList) {
            // Handle on more click

        }
        binding.rcvViewManagement.adapter = userManagementAdapter
        binding.rcvViewManagement.layoutManager = LinearLayoutManager(this)
    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = getString(R.string.tech_management)
    }

    override fun onStop() {
        super.onStop()
        registration?.remove() // Remove listener to prevent memory leaks
    }

    private fun loadTechnicians() {
        CoroutineScope(Dispatchers.IO).launch {
           withContext(Dispatchers.Main){

               val db = FirebaseFirestore.getInstance()
               registration = db.collection("Users")
                   .whereEqualTo("role", "Technician")
                   .addSnapshotListener { snapshot, e ->
                       if (e != null) {
                           Log.w("FireStore", "Listen failed.", e)
                           return@addSnapshotListener
                       }

                       if (snapshot != null && !snapshot.isEmpty) {
                           technicianList.clear()
                           for (document in snapshot.documents) {
                               val technician = document.toObject(User::class.java)
                               if (technician != null) {
                                   technicianList.add(technician)
                               }
                           }
                           userManagementAdapter.notifyDataSetChanged()
                       } else {
                           Log.d("FireStore", "No such documents")
                       }
                   }

           }
        }

    }

}