package com.mkdevelopers.doodlekong.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mkdevelopers.doodlekong.R

class LeaveDialog: DialogFragment() {

    private var onPositiveButtonClickListener: (() -> Unit)? = null

    fun setPositiveClickListener(listener: () -> Unit) {
        onPositiveButtonClickListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_leave_title)
            .setMessage(R.string.dialog_leave_message)
            .setPositiveButton(R.string.dialog_yes) { _, _ ->
                onPositiveButtonClickListener?.invoke()
            }
            .setNegativeButton(R.string.dialog_leave_no) { dialogInterface, _ ->
                dialogInterface?.cancel()
            }
            .create()
    }
}