package com.greg.uberclone.model

data class FCMSendData(
        var to: String,
        var data: Map<String, String>
)
