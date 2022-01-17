package com.greg.uberclone.ui.home

import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.droidman.ktoasty.KToasty
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.greg.uberclone.R
import com.greg.uberclone.databinding.FragmentHomeBinding
import com.greg.uberclone.utils.Constant
import com.greg.uberclone.utils.Constant.Companion.ACCESS_FINE_LOCATION
import com.greg.uberclone.utils.Constant.Companion.DEFAULT_ZOOM
import com.greg.uberclone.utils.Constant.Companion.INFO_CONNECTED
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var map: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    //------------------- Location -----------------------------------------------------------------
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var newPosition: LatLng
    private lateinit var userLocation: LatLng
    private var marker: Marker? = null
    //------------------- Online system ------------------------------------------------------------
    private lateinit var geoFire: GeoFire
    private lateinit var onlineDatabaseReference: DatabaseReference
    private var currentDriverReference: DatabaseReference? = null
    private lateinit var driverLocationReference: DatabaseReference
    //------------------- Geo coder ----------------------------------------------------------------
    private lateinit var geoCoder: Geocoder
    private var cityName: String = ""
    private var lat: Double = 0.0
    private var lng: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]
        binding = FragmentHomeBinding.inflate(layoutInflater)
        getLocationRequest()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Show map when ready & add a maker ----------------------------------------
    //----------------------------------------------------------------------------------------------

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        mapStyle()
        requestDexterPermission()
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Move camera --------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun moveCamera(currentLatLng: LatLng) {
        map.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Zoom level ---------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun zoomOnLocation(){
      map.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM))
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Custom style -------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun mapStyle(){
        try {
            val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.mapstyle))
            if (!success){
               Log.e("Style error", "Style parsing error")
            }
        }
        catch (e: Resources.NotFoundException){
            Log.e("Style error", e.message!!)
        }
        Snackbar.make(mapFragment.requireView(), "You're online!", Snackbar.LENGTH_SHORT).show()
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Get location -------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun getLocationRequest(){
        getDriverLocationFromDatabase()
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.fastestInterval = 15000
        locationRequest.interval = 10000
        locationRequest.smallestDisplacement = 50f

        locationCallback
        createLocationService()
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Location callback --------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private var locationCallback = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            val newLat = locationResult.lastLocation.latitude
            val newLng = locationResult.lastLocation.longitude

            newPosition = LatLng(newLat, newLng)
            addMarker(newPosition)
            //------------------- Geo coder  -------------------------------------------------------
            getCityNameFromLocation(newLat, newLng)
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get last known location -------------------------------------
    //----------------------------------------------------------------------------------------------

    //-------------------------------- Location service --------------------------------------------

    @SuppressLint("MissingPermission")
    private fun createLocationService() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper()!!)
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        removeLocation()
        removeOnlineListener()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(){
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location/*: Location?*/ ->
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                    addMarker(userLocation)
                }
            }.addOnFailureListener { e ->
                KToasty.error(requireContext(), "$e.message",
                    Toast.LENGTH_SHORT).show()
                Log.d("Failure", "${e.message}")
            }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Add marker --------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addMarker(markerLatLng: LatLng){
        if (marker == null) {
            val options = MarkerOptions().position(markerLatLng)
                    .title("Marker Title")
            marker = map.addMarker(options)
        } else {
            marker!!.position = markerLatLng
        }
        moveCamera(markerLatLng)
        zoomOnLocation()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Request Dexter permission -----------------------------------
    //----------------------------------------------------------------------------------------------

    private fun requestDexterPermission(){
        Dexter.withContext(context)
            .withPermission(ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener{
                override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse?) {
                    clickOnMyLocation()
                }

                override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse?) {
                    KToasty.error(requireContext(), "Permission ${permissionDeniedResponse!!.permissionName} was denied",
                        Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissionRequest: PermissionRequest?,
                    permissionToken: PermissionToken?
                ) {}
            }).check()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Center map on my location -----------------------------------
    //----------------------------------------------------------------------------------------------

    private fun clickOnMyLocation(){
        binding.gps.setOnClickListener {
            lastKnownLocation()
        }
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Online system ----------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Online value event listener ---------------------------------
    //----------------------------------------------------------------------------------------------

    private val onlineValueEventListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists() && currentDriverReference != null){
                currentDriverReference!!.onDisconnect().removeValue()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Remove location ---------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun removeLocation(){
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Remove online value event listener --------------------------
    //----------------------------------------------------------------------------------------------

    private fun removeOnlineListener(){
        onlineDatabaseReference.removeEventListener(onlineValueEventListener)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Register online system --------------------------------------
    //----------------------------------------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineDatabaseReference.addValueEventListener(onlineValueEventListener)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get driver's location from database -------------------------
    //----------------------------------------------------------------------------------------------

    private fun getDriverLocationFromDatabase(){
        onlineDatabaseReference = FirebaseDatabase.getInstance().reference.child(INFO_CONNECTED)
        driverLocationReference = FirebaseDatabase.getInstance().getReference(Constant.DRIVER_LOCATION)
        currentDriverReference = FirebaseDatabase.getInstance().reference.child(
            FirebaseAuth.getInstance().currentUser!!.uid
        )
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get driver's realtime location ------------------------------
    //----------------------------------------------------------------------------------------------

    private fun getRealTimeLocation(){
        geoFire = GeoFire(driverLocationReference)
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Geo coder --------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Geo coder ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeGeoCoder(){
        geoCoder = Geocoder(requireContext(), Locale.getDefault())
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get city name from location ---------------------------------
    //----------------------------------------------------------------------------------------------

    fun getCityNameFromLocation(latitude: Double, longitude: Double): String {
        initializeGeoCoder()
        lat = latitude
        lng = longitude
        try {
            val addressList = geoCoder.getFromLocation(latitude, longitude, 1)
            Log.d("AddressList", addressList.toString())
            if (addressList != null && addressList.size > 0){
                val address = (addressList as MutableList<Address>)[0]

                if (address.adminArea == null){
                    cityName = address.locality
                    saveCityNameInRealTimeDatabase()
                }
                if (address.locality == null){
                    cityName = address.adminArea
                    saveCityNameInRealTimeDatabase()
                }
            }

        } catch (e: IOException) {
            Log.e(Constant.GEO_CODER_TAG, "Unable to connect to GeoCoder", e)
        }
        return cityName
    }

    private fun saveCityNameInRealTimeDatabase(){
        driverLocationReference = FirebaseDatabase.getInstance().getReference(Constant.DRIVER_LOCATION)
                .child(cityName)
        currentDriverReference = driverLocationReference.child(
                FirebaseAuth.getInstance().currentUser!!.uid
        )
        getRealTimeLocation()
        //------------------- Update real time location  -------------------------------------------
        geoFire.setLocation(
                FirebaseAuth.getInstance().currentUser!!.uid, GeoLocation(lat, lng)
        ){ _: String?, databaseError: DatabaseError? ->
            if (databaseError != null){
                Snackbar.make(mapFragment.requireView(), databaseError.message, Snackbar.LENGTH_LONG).show()
            }

            registerOnlineSystem()
        }
    }
}