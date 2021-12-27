package com.greg.uberclone

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

class PhotoChoiceDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        val inflater = requireActivity().layoutInflater
        val view: View = inflater.inflate(R.layout.dialog_photo_choice, null)
        builder.setView(view)

        builder.setTitle(R.string.choose_photo)
                .setNegativeButton(R.string.cancel){ dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        }
        return dialog
    }
}