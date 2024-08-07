package com.example.myapp.interfaces

import com.example.myapp.model.ServicePackage

interface OnServicePackageClickListener {
    fun onServicePackageClick(servicePackage: ServicePackage)
    fun onOptionsButtonClick(servicePackage: ServicePackage)
}