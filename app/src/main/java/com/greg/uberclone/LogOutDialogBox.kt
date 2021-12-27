package com.greg.uberclone

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.droidman.ktoasty.KToasty
import com.google.firebase.auth.FirebaseAuth
import com.greg.uberclone.ui.activity.SplashScreenActivity

class LogOutDialogBox: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
            .setTitle("Log out")
            .setMessage("Do you really want to log out?")
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setPositiveButton("Log out") { dialogInterface, _ ->
                logOut()
                dialogInterface.dismiss()
            }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
        }
        return dialog
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Log out --------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun logOut(){
        FirebaseAuth.getInstance().signOut()
        KToasty.success(requireContext(), "Log out successfully", Toast.LENGTH_SHORT).show()
        goToSplashScreenActivity()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Go to Splash screen activity -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun goToSplashScreenActivity() {
        startActivity(Intent(context, SplashScreenActivity::class.java))
    }
}

