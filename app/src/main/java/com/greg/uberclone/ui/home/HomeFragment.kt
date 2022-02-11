package com.greg.uberclone.ui.home

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.greg.uberclone.R
import com.greg.uberclone.databinding.FragmentHomeBinding
import com.greg.uberclone.event.DriverReceivedRequestEvent
import com.greg.uberclone.remote.RetrofitService
import com.greg.uberclone.ui.activity.DriverHomeActivity
import com.greg.uberclone.utils.Common
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var map: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    //------------------- Location -----------------------------------------------------------------
    private var locationRequest: LocationRequest? = null
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
    //------------------- Routes -------------------------------------------------------------------
    private val compositeDisposable = CompositeDisposable()
    private lateinit var iRetrofitService: RetrofitService
    private var blackPolyline: Polyline? = null
    private var greyPolyline: Polyline? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolylineOptions: PolylineOptions? = null
    private var polylineList: ArrayList<LatLng?>? = null
    private lateinit var jsonArray: JSONArray
    private lateinit var latLngBound: LatLngBounds
    private lateinit var riderSendingRequestLocation: Location
    private var driverReceivedRequestEvent: DriverReceivedRequestEvent? = null
    private lateinit var time: JSONObject
    private lateinit var duration: String
    private lateinit var estimateDistance: JSONObject
    private lateinit var distance: String
    private lateinit var destination: LatLng

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]
        binding = FragmentHomeBinding.inflate(layoutInflater)
        //initializeChip(binding.root)
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
        initializeRetrofit()
        getDriverLocationFromDatabase()

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(binding.rootLayout, getString(R.string.permission_required), Snackbar.LENGTH_LONG).show()
            return
        }
        buildLocationRequest()
        buildLocationCallback()
        createLocationService()
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Location request ---------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun buildLocationRequest(){
        if (locationRequest == null){
            locationRequest = LocationRequest.create()
            locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest!!.fastestInterval = 15000
            locationRequest!!.interval = 10000
            locationRequest!!.smallestDisplacement = 50f
        }
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Location callback --------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun buildLocationCallback(){
        if (locationCallback == null){
            locationCallback
        }
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Location callback --------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private var locationCallback: LocationCallback? = object: LocationCallback(){
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

    private fun createLocationService() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(binding.rootLayout, getString(R.string.permission_required), Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest!!, locationCallback!!, Looper.myLooper()!!)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback!!)
        removeLocation()
        removeOnlineListener()

        compositeDisposable.clear()
        if(EventBus.getDefault().hasSubscriberForEvent(DriverHomeActivity::class.java)){
            EventBus.getDefault().removeStickyEvent(DriverHomeActivity::class.java)
        }
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    private fun lastKnownLocation(){
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(binding.rootLayout, getString(R.string.permission_required), Snackbar.LENGTH_LONG).show()
            return
        }
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
                    //-------------------------------- Location ------------------------------------
                    buildLocationRequest()
                    buildLocationCallback()
                    createLocationService()
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

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Driver request received ------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Initialize retrofit -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeRetrofit(){
        iRetrofitService = RetrofitService.getInstance()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Driver received request event -------------------------------
    //----------------------------------------------------------------------------------------------

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDriverReceivedRequestEvent(event: DriverReceivedRequestEvent){
        driverReceivedRequestEvent = event
        riderFromRequestLocation()
    }

    private fun riderFromRequestLocation(){
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(mapFragment.requireView(), getString(R.string.permission_required), Snackbar.LENGTH_SHORT).show()
            return
        }
        fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    riderSendingRequestLocation = location
                    drawPath()

                }.addOnFailureListener { e ->
                    Snackbar.make(mapFragment.requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()
                }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Draw path ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun drawPath() {
        //-------------------------------- Request api ---------------------------------------------
        compositeDisposable.add(iRetrofitService.getDirections(
                "driving",
                "less_driving",
                //selectedPlaceEvent.originString,
                Common.buildRiderSendingRequestLocation(riderSendingRequestLocation.latitude, riderSendingRequestLocation.longitude),
                driverReceivedRequestEvent!!.pickupLocation,
                getString(R.string.ApiKey))
        !!.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { returnResult ->
                    Log.d("Api return", returnResult)
                    try {
                        val jsonObject = JSONObject(returnResult)
                        jsonArray = jsonObject.getJSONArray("routes")

                        for (j in 0 until jsonArray.length()){
                            val route = jsonArray.getJSONObject(j)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        //-------------------------------- Polyline options ------------------------
                        pathStyle()
                        //-------------------------------- Animation -------------------------------
                        animate()

                    } catch (e: Exception) {
                        Snackbar.make(mapFragment.requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
                    }
                }
        )
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Path style --------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun pathStyle(){
        polylineOptions = PolylineOptions()
        polylineOptions!!.color(Color.GRAY)
        polylineOptions!!.width(12f)
        polylineOptions!!.startCap(SquareCap())
        polylineOptions!!.jointType(JointType.ROUND)
        polylineOptions!!.addAll(polylineList!!)
        greyPolyline = map.addPolyline(polylineOptions!!)
        blackPolylineStyle()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Black polyline -----------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun blackPolylineStyle(){
        blackPolylineOptions = PolylineOptions()
        blackPolylineOptions!!.color(Color.BLACK)
        blackPolylineOptions!!.width(5f)
        blackPolylineOptions!!.startCap(SquareCap())
        blackPolylineOptions!!.jointType(JointType.ROUND)
        blackPolylineOptions!!.addAll(polylineList!!)
        blackPolyline = map.addPolyline(blackPolylineOptions!!)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Animation ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun animate(){
        val valueAnimator = ValueAnimator.ofInt(0,100)
        valueAnimator.duration = 1100
        valueAnimator.repeatCount = ValueAnimator.INFINITE
        valueAnimator.interpolator = LinearInterpolator()

        valueAnimator.addUpdateListener { value ->
            val points =  greyPolyline!!.points
            val percentValue = value.animatedValue.toString().toInt()
            val size = points.size
            val newPoints = (size * (percentValue / 100))
            val p = points.subList(0, newPoints)
            blackPolyline!!.points = (p)
        }

        valueAnimator.start()

        val origin = LatLng(riderSendingRequestLocation.latitude, riderSendingRequestLocation.longitude)
        destination = LatLng(
                driverReceivedRequestEvent!!.pickupLocation.split(",")[0].toDouble(),
                driverReceivedRequestEvent!!.pickupLocation.split(",")[1].toDouble()
        )

        latLngBound = LatLngBounds.Builder().include(origin)
                .include(destination)
                .build()

        //-------------------------------- Add tie fighter icon ------------------------------------
        addTieFighterIcon()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Add tie fighter icon ----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addTieFighterIcon(){
        val objects = jsonArray.getJSONObject(0)
        val legs = objects.getJSONArray("legs")
        val legsObject = legs.getJSONObject(0)

        time = legsObject.getJSONObject("duration")
        duration = time.getString("text")

        estimateDistance = legsObject.getJSONObject("distance")
        distance = estimateDistance.getString("text")

        initializeChip()
        addMarkerForRequest(destination)
        //-------------------------------- Display request layout ----------------------------------
        showChip()
        showAcceptLayout()
        countdown()
        //-------------------------------- Move camera ---------------------------------------------
        moveCameraToLatLngBounds()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Add marker for request --------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addMarkerForRequest(destination: LatLng) {
        map.addMarker(MarkerOptions().position(destination).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title("Pickup Location"))
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Move camera to lat lng bounds -------------------------------
    //----------------------------------------------------------------------------------------------

    private fun moveCameraToLatLngBounds() {
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
        zoomToBounds()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Zoom to bounds ----------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun zoomToBounds(){
        map.moveCamera(CameraUpdateFactory.zoomTo(map.cameraPosition.zoom-1))
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Initialize chip ---------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeChip() {
        binding.estimateTimeTv.text = duration
        binding.estimateDistanceTv.text = distance
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Shop chip ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showChip(){
       binding.chipDecline.visibility = View.VISIBLE
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Show accept layout ------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showAcceptLayout(){
        binding.acceptCv.visibility = View.VISIBLE
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Countdown ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun countdown(){
        Observable.interval(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { x ->
                    binding.circularProgressBar.progress += 1f
                }
                .takeUntil { aLong -> aLong == "100".toLong() } // 10 seconds
                .doOnComplete {
                    KToasty.success(requireContext(), "Fake accept action", Toast.LENGTH_LONG).show()
                }
                .subscribe()
    }
}