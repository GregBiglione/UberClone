package com.greg.uberclone.utils

import android.Manifest
import com.greg.uberclone.R

class Constant {
    companion object{
        const val DRIVER_INFORMATION = "DriverInformation"
        const val DRIVER_LOCATION = "DriverLocation"
        const val DEFAULT_ZOOM = 17.0f
        const val ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        const val INFO_CONNECTED = ".info/connected"
        //-------------------------------- Camera & gallery ----------------------------------------
        const val ACCESS_CAMERA = Manifest.permission.CAMERA
        const val READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
        //-------------------------------- Notification --------------------------------------------
        const val TOKEN = "Token"
        const val NOTIFICATION_TITLE = "Title test FCM"
        const val NOTIFICATION_BODY = "Message test FCM"
        const val NOTIFICATION_CHANNEL_ID = "Uber_clone_channel"
        //-------------------------------- Log -----------------------------------------------------
        const val GEO_CODER_TAG = "GeoCodingLocation"
        //-------------------------------- Driver request ------------------------------------------
        const val REQUEST_DRIVER_TITLE = "RequestDriver"
        const val PICKUP_LOCATION = "PickupLocation"
        const val RIDER_KEY = "RiderKey"
        //-------------------------------- Url -----------------------------------------------------
        const val BASE_URL = "https://maps.googleapis.com/"
        const val BASE_URL_FCM = "https://fcm.googleapis.com/"
        //-------------------------------- Firebase key --------------------------------------------
        const val FIREBASE_KEY = R.string.FirebaseKey
    }
}