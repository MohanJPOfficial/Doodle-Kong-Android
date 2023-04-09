package com.mkdevelopers.doodlekong.util

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

fun Fragment.showSnackBar(message: String) {
    Snackbar.make(
        /* view = */ requireView(),
        /* text = */ message,
        /* duration = */ Snackbar.LENGTH_LONG
    ).show()
}

fun Fragment.showSnackBar(@StringRes res: Int) {
    Snackbar.make(
        /* view = */ requireView(),
        /* resId = */ res,
        /* duration = */ Snackbar.LENGTH_LONG
    ).show()
}