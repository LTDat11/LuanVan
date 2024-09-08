package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.model.Repair
import com.google.firebase.firestore.FirebaseFirestore

class RepairTechAdapter(private val repairList: List<Repair>) : RecyclerView.Adapter<RepairTechAdapter.RepairViewHolder>() {

    inner class RepairViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRepairName: TextView = itemView.findViewById(R.id.tv_repair_name)
        val tvRepairPrice: TextView = itemView.findViewById(R.id.tv_repair_price)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepairViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_repaired_device_tech, parent, false)
        return RepairViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RepairViewHolder, position: Int) {
        val repair = repairList[position]
        holder.tvRepairName.text = repair.name
        holder.tvRepairPrice.text = repair.price
        // Xử lý sự kiện click cho btnEdit, btnDelete nếu cần
        holder.btnEdit.setOnClickListener {
            //show dialog edit
            showDialogEdit(holder.itemView, repair, position)
        }
        holder.btnDelete.setOnClickListener {
            deleteRepair(holder.itemView, repair, position)
        }
    }

    private fun deleteRepair(itemView: View, repair: Repair, position: Int) {
        val context = itemView.context
        val db = FirebaseFirestore.getInstance()
        val repairRef = db.collection("orders")
            .document(repair.id_order!!)
            .collection("repairs")
            .document(repair.id!!)

        repairRef.delete()
            .addOnSuccessListener {
                // Xóa thiết bị khỏi danh sách và cập nhật RecyclerView
                notifyItemRemoved(position)
                Toast.makeText(context, "Xóa thiết bị thành công", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Lỗi khi xóa thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showDialogEdit(itemView: View, repair: Repair, position: Int) {
        val context = itemView.context
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_device_repairted, null)
        val etDeviceName = dialogView.findViewById<EditText>(R.id.et_device_name)
        val etDevicePrice = dialogView.findViewById<EditText>(R.id.et_device_price)

        // Điền dữ liệu hiện tại vào EditText
        etDeviceName.setText(repair.name)
        etDevicePrice.setText(repair.price)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Chỉnh sửa thiết bị")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val updatedName = etDeviceName.text.toString()
                val updatedPrice = etDevicePrice.text.toString()

                if (updatedName.isNotEmpty() && updatedPrice.isNotEmpty()) {
                    // Cập nhật dữ liệu trong Firestore
                    val db = FirebaseFirestore.getInstance()
                    val repairRef = db.collection("orders")
                        .document(repair.id_order!!)
                        .collection("repairs")
                        .document(repair.id!!)

                    // Cập nhật thông tin thiết bị
                    repairRef.update("name", updatedName, "price", updatedPrice)
                        .addOnSuccessListener {
                            // Cập nhật danh sách sau khi chỉnh sửa thành công
                            notifyItemChanged(position)
                            Toast.makeText(context, "Chỉnh sửa thiết bị thành công", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Lỗi khi chỉnh sửa thiết bị: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .create()

        dialog.show()
    }

    override fun getItemCount(): Int {
        return repairList.size
    }
}
