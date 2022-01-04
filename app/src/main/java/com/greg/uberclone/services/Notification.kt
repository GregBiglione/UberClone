package com.greg.uberclone.services

import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.greg.uberclone.ui.activity.DriverHomeActivity
import com.greg.uberclone.utils.Common
import com.greg.uberclone.utils.Constant.Companion.NOTIFICATION_BODY
import com.greg.uberclone.utils.Constant.Companion.NOTIFICATION_TITLE
import com.greg.uberclone.utils.UserUtils
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
            val intent = Intent(this, DriverHomeActivity::class.java)
            Common.showNotification(this, Random().nextInt(), NOTIFICATION_TITLE, NOTIFICATION_BODY, intent)
        }
    }
}