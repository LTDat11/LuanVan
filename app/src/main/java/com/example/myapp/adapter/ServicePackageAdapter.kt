package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.interfaces.OnServicePackageClickListener
import com.example.myapp.model.ServicePackage

// recyclerview adapter for service packages
class ServicePackageAdapter(private val packages: List<ServicePackage>) : RecyclerView.Adapter<ServicePackageAdapter.PackageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_package, parent, false)
        return PackageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        val servicePackage = packages[position]
        holder.bind(servicePackage)
    }

    override fun getItemCount(): Int = packages.size

    inner class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.package_image)
        private val nameTextView: TextView = itemView.findViewById(R.id.package_name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.package_description)
        private val priceTextView: TextView = itemView.findViewById(R.id.package_price)
        private val optionsButton: ImageButton = itemView.findViewById(R.id.package_options)


        fun bind(servicePackage: ServicePackage) {
            nameTextView.text = servicePackage.name
            descriptionTextView.text = servicePackage.description
            priceTextView.text = servicePackage.price
            imageView.setImageResource(R.drawable.ic_launcher_foreground)

            val id = servicePackage.id
            val categoryId = servicePackage.categoryId
            

            itemView.setOnClickListener {
                Toast.makeText(itemView.context, "Package ID: ${id}", Toast.LENGTH_SHORT).show()
            }

            optionsButton.setOnClickListener {
                Toast.makeText(itemView.context, "Options for package ID: ${id}", Toast.LENGTH_SHORT).show()
            }

        }
    }
}
