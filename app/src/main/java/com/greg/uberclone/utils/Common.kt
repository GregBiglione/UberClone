package com.greg.uberclone.utils

import com.greg.uberclone.model.Driver
import java.lang.StringBuilder

object Common {
    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome ")
            .append(currentDriver!!.firstName)
            .append(" ")
            .append(currentDriver!!.lastName)
            .toString()
    }

    var currentDriver: Driver? = null
}