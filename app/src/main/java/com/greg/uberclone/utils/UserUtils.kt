package com.greg.uberclone.utils

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import com.droidman.ktoasty.KToasty
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.greg.uberclone.R
import com.greg.uberclone.model.FCMSendData
import com.greg.uberclone.model.Token
import com.greg.uberclone.remote.FCMService
import com.greg.uberclone.utils.Constant.Companion.ACCEPTED_REQUEST_DRIVER_BODY_MSG
import com.greg.uberclone.utils.Constant.Companion.DECLINED_REQUEST_DRIVER_BODY_MSG
import com.greg.uberclone.utils.Constant.Companion.DRIVER_INFORMATION
import com.greg.uberclone.utils.Constant.Companion.DRIVER_KEY
import com.greg.uberclone.utils.Constant.Companion.NOTIFICATION_BODY
import com.greg.uberclone.utils.Constant.Companion.NOTIFICATION_TITLE
import com.greg.uberclone.utils.Constant.Companion.REQUEST_DRIVER_ACCEPT
import com.greg.uberclone.utils.Constant.Companion.REQUEST_DRIVER_DECLINE
import com.greg.uberclone.utils.Constant.Companion.TOKEN
import com.greg.uberclone.utils.Constant.Companion.TRIP_KEY
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

object UserUtils {

    private var currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Update driver -----------------------------------------------
    //----------------------------------------------------------------------------------------------

    fun updateDriver(view: View?, updateAvatar: Map<String, Any>){
        FirebaseDatabase.getInstance()
            .getReference(DRIVER_INFORMATION)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateAvatar)
            .addOnSuccessListener {
                Snackbar.make(view!!, R.string.information_update_success, Snackbar.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(view!!, e.message!!, Snackbar.LENGTH_LONG).show()
            }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Update token ------------------------------------------------
    //----------------------------------------------------------------------------------------------

    fun updateToken(context: Context, token: String) {
        val currentToken = Token()
        currentToken.token = token

        FirebaseDatabase.getInstance().getReference(TOKEN)
                .child(currentUserUid)
                .setValue(token)
                .addOnSuccessListener {}
                .addOnFailureListener { e ->
                    KToasty.error(context, e.message!!, Toast.LENGTH_LONG).show()
                }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Decline request ---------------------------------------------
    //----------------------------------------------------------------------------------------------

    fun sendDeclineRequest(view: View, activity: Activity, key: String) {
        val compositeDisposable = CompositeDisposable()
        val iFcmService = FCMService.getInstance()

        FirebaseDatabase.getInstance().getReference(TOKEN)
                .child(key)
                .addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val token = snapshot.getValue(true)

                            val notificationData: MutableMap<String, String> = HashMap()

                            notificationData[NOTIFICATION_TITLE] = REQUEST_DRIVER_DECLINE
                            notificationData[NOTIFICATION_BODY] = DECLINED_REQUEST_DRIVER_BODY_MSG
                            notificationData[DRIVER_KEY] = FirebaseAuth.getInstance().currentUser!!.uid

                            val fcmSendData = FCMSendData(token.toString(), notificationData)

                            compositeDisposable.add(iFcmService.sendNotification(fcmSendData)
                            !!.subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ fcmResponse ->
                                        if (fcmResponse!!.success == 0){
                                            compositeDisposable.clear()
                                            Snackbar.make(view, activity.getString(R.string.decline_failed),
                                                    Snackbar.LENGTH_LONG).show()
                                        }
                                        else{
                                            Snackbar.make(view, activity.getString(R.string.decline_success),
                                                    Snackbar.LENGTH_LONG).show()
                                        }
                                    },
                                            { t: Throwable ->
                                                compositeDisposable.clear()
                                                Snackbar.make(view, t.message!!, Snackbar.LENGTH_LONG).show()
                                                Log.e(TAG, t.message!!)
                                                KToasty.error(activity, t.message!!, Toast.LENGTH_LONG).show()
                                            })
                            )
                        }
                        else{
                            compositeDisposable.clear()
                            Snackbar.make(view, activity.getString(R.string.token_not_found), Snackbar.LENGTH_LONG).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                        Log.e(TAG, error.message)
                        KToasty.error(activity, error.message, Toast.LENGTH_LONG).show()
                    }
                })
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Accept request ----------------------------------------------
    //----------------------------------------------------------------------------------------------

    fun sendAcceptedRequestToRider(view: View?, requireContext: Context, key: String?, tripNumberId: String?) {
        val compositeDisposable = CompositeDisposable()
        val iFcmService = FCMService.getInstance()

        FirebaseDatabase.getInstance().getReference(TOKEN)
                .child(key!!)
                .addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val token = snapshot.getValue(true)

                            val notificationData: MutableMap<String, String> = HashMap()

                            notificationData[NOTIFICATION_TITLE] = REQUEST_DRIVER_ACCEPT
                            notificationData[NOTIFICATION_BODY] = ACCEPTED_REQUEST_DRIVER_BODY_MSG
                            notificationData[DRIVER_KEY] = FirebaseAuth.getInstance().currentUser!!.uid
                            notificationData[TRIP_KEY] = tripNumberId!!

                            val fcmSendData = FCMSendData(token.toString(), notificationData)

                            compositeDisposable.add(iFcmService.sendNotification(fcmSendData)
                            !!.subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ fcmResponse ->
                                        if (fcmResponse!!.success == 0){
                                            compositeDisposable.clear()
                                            Snackbar.make(view!!, requireContext.getString(R.string.accept_failed),
                                                    Snackbar.LENGTH_LONG).show()
                                        }
                                    },
                                            { t: Throwable ->
                                                compositeDisposable.clear()
                                                Snackbar.make(view!!, t.message!!, Snackbar.LENGTH_LONG).show()
                                                Log.e(TAG, t.message!!)
                                            })
                            )
                        }
                        else{
                            compositeDisposable.clear()
                            Snackbar.make(view!!, requireContext.getString(R.string.token_not_found), Snackbar.LENGTH_LONG).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Snackbar.make(view!!, error.message, Snackbar.LENGTH_LONG).show()
                        Log.e(TAG, error.message)
                    }
                })
    }
}