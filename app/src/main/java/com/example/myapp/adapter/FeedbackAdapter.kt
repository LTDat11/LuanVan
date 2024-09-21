package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.model.Feedback
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedbackAdapter(private var feedbacks: MutableList<Feedback>): RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackAdapter.FeedbackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feedback, parent, false)
        return FeedbackViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        val feedback = feedbacks[position]
        holder.tvName.text = feedback.name
        holder.tvEmail.text = feedback.email
        holder.tvComment.text = feedback.comment
        holder.date.text = feedback.createdAt.toString()
        getAvatar(holder.img, feedback.email)

        feedback.rating?.let { ratingValue ->
            holder.ratingBar.rating = ratingValue.toFloat() // Convert rating về Float
        } ?: run {
            holder.ratingBar.rating = 0f // Nếu không có giá trị, để rating mặc định là 0
        }

    }

    private fun getAvatar(img: CircleImageView?, email: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()
                db.collection("Users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            val imageURL = document.getString("imageURL")
                            imageURL?.let {
                                img?.let { imageView ->
                                    imageView.visibility = View.VISIBLE
                                    Glide.with(imageView.context)
                                        .load(it)
                                        .into(imageView)
                                }
                            }
                        }
                    }
            }
        }

    }

    override fun getItemCount(): Int {
        return feedbacks.size
    }

    fun updateList(newList: List<Feedback>) {
        feedbacks.clear() // Xóa danh sách cũ
        feedbacks.addAll(newList) // Thêm danh sách mới
        notifyDataSetChanged() // Cập nhật RecyclerView
    }



    class FeedbackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName = itemView.findViewById<TextView>(R.id.tv_name)
        val tvEmail = itemView.findViewById<TextView>(R.id.tv_email)
        val tvComment = itemView.findViewById<TextView>(R.id.tv_comment_content)
        val ratingBar = itemView.findViewById<RatingBar>(R.id.ratingBar)
        val img = itemView.findViewById<CircleImageView>(R.id.iv_avatar)
        val date = itemView.findViewById<TextView>(R.id.tv_date)
    }

}