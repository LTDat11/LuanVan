package com.example.myapp.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.example.myapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.example.myapp.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.tasks.Task
import java.io.IOException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentMarker: Marker? = null
    private var selectedAddress: String? = null // Biến lưu địa chỉ được chọn
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initSearchView()
        initListeners()
    }

    private fun initListeners() {
        binding.btnSaveLocation.setOnClickListener {
            selectedAddress?.let {
                // Return selected location to previous activity
                val resultIntent = Intent()
                resultIntent.putExtra("selected_location", it)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Enable My Location layer on Google Map
        mMap.isMyLocationEnabled = true

        // Get the current location of the device and set the position of the map
        getDeviceLocation()

        // Set a listener for map clicks to add a marker
        mMap.setOnMapClickListener { latLng ->
            placeMarker(latLng)
        }
    }

    private fun getDeviceLocation() {
        try {
            val locationResult: Task<Location> = fusedLocationClient.lastLocation
            locationResult.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val lastKnownLocation = task.result
                    if (lastKnownLocation != null) {
                        val currentLatLng = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)

                        // Add a marker at the current location
                        placeMarker(currentLatLng)

                        // Move the camera to the current location
                        val cameraPosition = CameraPosition.Builder()
                            .target(currentLatLng) // Set the center of the map to current location
                            .zoom(15f) // Set the zoom level
                            .build()
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun placeMarker(latLng: LatLng) {
        // Clear previous marker
        currentMarker?.remove()

        // Get address from latLng using Geocoder
        val address = getAddress(latLng)

        // Add a new marker at the clicked position
        currentMarker = mMap.addMarker(
            MarkerOptions().position(latLng).title(address ?: "Marker at (${latLng.latitude}, ${latLng.longitude})")
        )

        // Move camera to the new marker
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        // Lưu địa chỉ được chọn
        selectedAddress = address
    }

    private fun getAddress(latLng: LatLng): String? {
        val geocoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses: List<Address>? = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun initSearchView() {
        // Initialize the search view
        val searchView = findViewById<SearchView>(R.id.sv_location)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchLocation(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun searchLocation(location: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList = geocoder.getFromLocationName(location, 1)
            if (addressList != null) {
                if (addressList.isNotEmpty()) {
                    val address = addressList[0]
                    val latLng = LatLng(address.latitude, address.longitude)

                    // Place a marker at the searched location
                    placeMarker(latLng)
                } else {
                    // No results found, handle it here
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
