package com.greg.uberclone.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.droidman.ktoasty.KToasty
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.greg.uberclone.R
import com.greg.uberclone.model.Token
import com.greg.uberclone.utils.Constant.Companion.DRIVER_INFORMATION
import com.greg.uberclone.utils.Constant.Companion.TOKEN

object UserUtils {

    private var currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid

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
}