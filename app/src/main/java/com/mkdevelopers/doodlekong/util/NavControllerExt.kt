package com.mkdevelopers.doodlekong.util

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

@Deprecated("No longer needed")
fun NavController.navigateSafely(
    @IdRes resId: Int,
    args: Bundle? = null,
    navOptions: NavOptions? = null,
    navExtras: Navigator.Extras? = null
) {
    val action = currentDestination?.getAction(resId) ?: graph.getAction(resId)

    println("navigation currentDestination >> ${currentDestination?.getAction(resId)}")
    println("navigation action >> ${graph.getAction(resId)}")

    if(action != null && currentDestination?.id != action.destinationId) {
        navigate(
            resId = resId,
            args = args,
            navOptions = navOptions,
            navigatorExtras = navExtras
        )
    }
}