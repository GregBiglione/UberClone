package com.greg.uberclone.services

import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.greg.uberclone.event.DriverReceivedRequestEvent
import com.greg.uberclone.ui.activity.DriverHomeActivity
import com.greg.uberclone.utils.Common
import com.greg.uberclone.utils.Constant.Companion.DESTINATION_LOCATION
import com.greg.uberclone.utils.Constant.Companion.DESTINATION_LOCATION_STRING
import com.greg.uberclone.utils.Constant.Companion.NOTIFICATION_BODY
import com.greg.uberclone.utils.Constant.Companion.NOTIFICATION_TITLE
import com.greg.uberclone.utils.Constant.Companion.PICKUP_LOCATION
import com.greg.uberclone.utils.Constant.Companion.PICKUP_LOCATION_STRING
import com.greg.uberclone.utils.Constant.Companion.REQUEST_DRIVER_TITLE
import com.greg.uberclone.utils.Constant.Companion.RIDER_KEY
import com.greg.uberclone.utils.UserUtils
import org.greenrobot.eventbus.EventBus
import java.util.*

class Notification: FirebaseMessagingService() {

    private var currentUser = FirebaseAuth.getInstance().currentUser

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (currentUser != null){
            UserUtils.updateToken(this, token)
            Log.d("Token in new token", token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        if (data != null){
            if (data[NOTIFICATION_TITLE]!! == REQUEST_DRIVER_TITLE){

                val driverReceivedRequestEvent = DriverReceivedRequestEvent()
                driverReceivedRequestEvent.key = data[RIDER_KEY]
                driverReceivedRequestEvent.pickupLocation = data[PICKUP_LOCATION]
                driverReceivedRequestEvent.pickupLocationString = data[PICKUP_LOCATION_STRING]
                driverReceivedRequestEvent.destinationLocation = data[DESTINATION_LOCATION]
                driverReceivedRequestEvent.destinationLocationString = data[DESTINATION_LOCATION_STRING]

                EventBus.getDefault().postSticky(driverReceivedRequestEvent)
            }
            else{
                val intent = Intent(this, DriverHomeActivity::class.java)
                Common.showNotification(this, Random().nextInt(), NOTIFICATION_TITLE, NOTIFICATION_BODY, intent)
            }
        }
    }
}