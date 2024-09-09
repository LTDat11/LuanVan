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
import java.text.NumberFormat
import java.util.Locale

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
        val id_order = repair.id_order
        // Kiểm tra status của order để ẩn hiện button
        checkStatusOrder(holder, id_order!!)
        // Xử lý sự kiện click cho btnEdit, btnDelete nếu cần
        holder.btnEdit.setOnClickListener {
            //show dialog edit
            showDialogEdit(holder.itemView, repair, position)
        }
        holder.btnDelete.setOnClickListener {
            deleteRepair(holder.itemView, repair, position)
        }
    }

    private fun checkStatusOrder(holder: RepairTechAdapter.RepairViewHolder, idOrder: String) {
        val db = FirebaseFirestore.getInstance()
        val orderRef = db.collection("orders").document(idOrder)
        orderRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val status = document.getString("status")
                    if (status == "processing") {
                        holder.btnEdit.visibility = View.VISIBLE
                        holder.btnDelete.visibility = View.VISIBLE
                    } else {
                        holder.btnEdit.visibility = View.GONE
                        holder.btnDelete.visibility = View.GONE
                    }
                }
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
        etDevicePrice.setText(repair.price!!.replace(",", "").replace("VND", ""))

        val dialog = AlertDialog.Builder(context)
            .setTitle("Chỉnh sửa thiết bị")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val updatedName = etDeviceName.text.toString()
                val updatedPrice = etDevicePrice.text.toString()

                // Kiểm tra nếu dữ liệu mới khác với dữ liệu cũ
                if (updatedName != repair.name || updatedPrice != repair.price!!.replace(",", "").replace("VND", "")) {
                    if (updatedName.isNotEmpty() && updatedPrice.isNotEmpty()) {
                        // Cập nhật dữ liệu trong Firestore
                        val db = FirebaseFirestore.getInstance()
                        val repairRef = db.collection("orders")
                            .document(repair.id_order!!)
                            .collection("repairs")
                            .document(repair.id!!)

                        // Cập nhật thông tin thiết bị
                        repairRef.update("name", updatedName, "price", formatPrice(updatedPrice))
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
                } else {
                    Toast.makeText(context, "Không có thay đổi nào được thực hiện", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .create()

        dialog.show()
    }

    // Hàm định dạng giá tiền
    private fun formatPrice(price: String): String {
        val price = price.toLongOrNull() ?: return price
        // Sử dụng NumberFormat để định dạng theo locale Việt Nam
        val numberFormat = NumberFormat.getNumberInstance(Locale.US)
        val formattedPrice = numberFormat.format(price)

        // Trả về chuỗi đã định dạng kèm với đơn vị tiền tệ VND
        return "$formattedPrice VND"
    }


    override fun getItemCount(): Int {
        return repairList.size
    }
}
