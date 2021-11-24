package com.moon.boonsekwon.data

import androidx.annotation.Keep

@Keep
data class Location(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var title: String = "",
    var description: String? = "",
    var address: String? = "",
    var registerKey: String? = "",
    var typeImageId : Int = 0
)