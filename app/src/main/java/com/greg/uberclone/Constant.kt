package com.greg.uberclone

import android.Manifest

class Constant {
    companion object{
        const val DRIVER_INFORMATION = "DriverInformation"
        const val DRIVER_LOCATION = "DriverLocation"
        const val DEFAULT_ZOOM = 17.0f
        const val ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        const val INFO_CONNECTED = ".info/connected"
    }
}