package com.example.myapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapp.fragment.ServicePackageFragment
import com.example.myapp.model.ServicePackage


// tablayout and viewpager2 adapter
class ServicePackagePagerAdapter(
    fragmentActivity: FragmentActivity,
    private var servicePackagesList: List<List<ServicePackage>>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = servicePackagesList.size

    override fun createFragment(position: Int): Fragment {
        return ServicePackageFragment.newInstance(servicePackagesList[position])
    }
}