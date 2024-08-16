package com.example.myapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.adapter.FilterAdapter
import com.example.myapp.adapter.ServicePackageAdapter
import com.example.myapp.model.FilterItem
import com.example.myapp.model.ServicePackage

class ServicePackageFragment : Fragment() {

    private lateinit var servicePackageAdapter: ServicePackageAdapter
    private lateinit var allPackages: List<ServicePackage>
    companion object {
        private const val ARG_PACKAGES = "packages"

        fun newInstance(servicePackages: List<ServicePackage>): ServicePackageFragment {
            val fragment = ServicePackageFragment()
            val args = Bundle().apply {
                putSerializable(ARG_PACKAGES, ArrayList(servicePackages))
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val mView = inflater.inflate(R.layout.fragment_service_package, container, false)
        // Get data from arguments
        allPackages = arguments?.getSerializable(ARG_PACKAGES) as? List<ServicePackage> ?: emptyList()

        // Setup RecyclerView for service packages
        val recyclerView: RecyclerView = mView.findViewById(R.id.recyclerView)
        servicePackageAdapter = ServicePackageAdapter(allPackages)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = servicePackageAdapter

        // Setup RecyclerView for filters
        val recyclerViewFilter: RecyclerView = mView.findViewById(R.id.rcv_filter)
        val filterOptions = createFilterOptions()
        recyclerViewFilter.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewFilter.adapter = FilterAdapter(filterOptions) { filterItem ->
            applyFilter(filterItem)
        }

        return mView
    }
    private fun createFilterOptions(): List<FilterItem> {
        // Create a list of filter items, customize based on your filtering criteria
        return listOf(
            FilterItem("Tất cả", R.drawable.baseline_filter_alt_24),  // Default filter
            FilterItem("Giá <= 700,000", R.drawable.baseline_attach_money_24),
            FilterItem("Giảm giá", R.drawable.baseline_discount_24)
            // Add more filters as needed
        )
    }

    private fun applyFilter(filterItem: FilterItem) {
        val filteredPackages = when (filterItem.title) {
            "Tất cả" -> allPackages
            "Giá <= 700,000" -> allPackages.filter { it.price.replace(",", "").replace("VND", "").trim().toInt() <= 700000 }  // Example condition
//            "Popular" -> allPackages.filter { it.isPopular }   // Example condition
            else -> allPackages
        }

        servicePackageAdapter.updateData(filteredPackages)
    }
}