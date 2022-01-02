package com.greg.uberclone.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.greg.uberclone.R
import com.greg.uberclone.utils.Constant.Companion.DRIVER_INFORMATION

object UserUtils {

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
}