package com.example.myapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.adapter.ServicePackageAdapter
import com.example.myapp.model.ServicePackage

class ServicePackageFragment : Fragment() {
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
        val recyclerView: RecyclerView = mView.findViewById(R.id.recyclerView)
        val packages = arguments?.getSerializable(ARG_PACKAGES) as? List<ServicePackage> ?: emptyList()

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ServicePackageAdapter(packages)

        return mView
    }

}