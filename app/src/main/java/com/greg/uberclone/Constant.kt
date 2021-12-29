package com.greg.uberclone

import android.Manifest

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
        const val WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
        const val GALLERY_REQUEST_CODE = 1201 // cam
        const val CAMERA_REQUEST_CODE = 807
        const val IMAGE_PICK_CODE = 2108
        const val TAKE_PHOTO_CODE = 3003
    }
}