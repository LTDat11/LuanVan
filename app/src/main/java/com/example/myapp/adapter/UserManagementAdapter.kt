package com.example.myapp.adapter

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.model.ApiResponse
import com.example.myapp.model.RetrofitInstance
import com.example.myapp.model.User
import com.example.myapp.model.UserRequest
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserManagementAdapter (private var technicianList: List<User>, private val onMoreClickListener: (User) -> Unit): RecyclerView.Adapter<UserManagementAdapter.UserManagementViewHolder>() {

    inner class UserManagementViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val imgTechnician: CircleImageView = itemView.findViewById(R.id.imgTechnician)
        val tvTechnicianName: TextView = itemView.findViewById(R.id.tvTechnicianName)
        val tvTechnicianDescription: TextView = itemView.findViewById(R.id.tvTechnicianDiscription)
        val layoutTechnicianDiscription: View = itemView.findViewById(R.id.layoutTechnicianDiscription)
        val layoutTechnicianJobCount : View = itemView.findViewById(R.id.layoutTechnicianJobCount)
        val tvTechnicianAddress: TextView = itemView.findViewById(R.id.tvTechnicianAddress)
        val tvTechnicianPhone: TextView = itemView.findViewById(R.id.tvTechnicianPhone)
        val tvTechnicianEmail: TextView = itemView.findViewById(R.id.tvTechnicianEmail)
        val tvJobCount: TextView = itemView.findViewById(R.id.technicain_job_count)
        val tvStatusAccount : TextView = itemView.findViewById(R.id.tvStatusAccount)
        val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserManagementViewHolder {
        val view = View.inflate(parent.context, R.layout.item_user_management, null)
        return UserManagementViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserManagementViewHolder, position: Int) {
        val technician = technicianList[position]
        holder.tvTechnicianName.text = technician.name
        holder.tvTechnicianDescription.text = technician.description
        holder.tvTechnicianAddress.text = technician.address
        holder.tvTechnicianPhone.text = technician.phone
        holder.tvTechnicianEmail.text = technician.email
        Glide.with(holder.itemView.context)
            .load(technician.imageURL)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(holder.imgTechnician)
        callAPI(technician, holder)

        // check role of  user and show/hide layout based on role of user
        // if user is admin and customer then hide layoutTechnicianDiscription
        // if user is technician then show layoutTechnicianDiscription
        holder.layoutTechnicianDiscription.visibility = if (technician.role == "Technician") View.VISIBLE else View.GONE
        holder.layoutTechnicianJobCount.visibility = if (technician.role == "Technician") View.VISIBLE else View.GONE

        // get job count of technician
        getTechJobCount(technician.id, holder)

//        holder.layoutTechnicianDiscription.visibility = if (technician.description.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.btnMore.setOnClickListener { onMoreClickListener(technician) }
    }

    private fun callAPI(technician: User, holder: UserManagementAdapter.UserManagementViewHolder) {
        CoroutineScope(Dispatchers.IO).launch {
           val request = UserRequest(technician.id)
            RetrofitInstance.api.checkUserStatus(request).enqueue(object : Callback<ApiResponse>{
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val isDisabled = response.body()?.isDisabled ?: false
                        if (isDisabled) {
                            holder.tvStatusAccount.text = "Đã khóa"
                            holder.tvStatusAccount.setTextColor(holder.itemView.context.resources.getColor(R.color.red))
                        } else {
                            holder.tvStatusAccount.text = "Đang hoạt động"
                            holder.tvStatusAccount.setTextColor(holder.itemView.context.resources.getColor(R.color.green))
                        }
                    }else{
                        holder.tvStatusAccount.text = "Không tìm thấy"
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.d("checkUserStatus", "Lỗi: ${t.message}")
                }
            })
        }
    }

    private fun getTechJobCount(id: String, holder: UserManagementAdapter.UserManagementViewHolder) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()

                db.collection("orders")
                    .whereEqualTo("id_technician", id)
                    .whereEqualTo("status", "processing")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            // Xử lý lỗi
                            holder.tvJobCount.text = "Không tìm thấy"
                            return@addSnapshotListener
                        }

                        if (snapshot != null && !snapshot.isEmpty) {
                            // Cập nhật số lượng đơn hàng của technician
                            holder.tvJobCount.text = snapshot.size().toString()
                        } else {
                            holder.tvJobCount.text = "0"
                        }
                    }

            }
        }
    }

    override fun getItemCount(): Int {
        return technicianList.size
    }

    fun updateList(newList: List<User>){
        technicianList = newList
        notifyDataSetChanged()
    }


}