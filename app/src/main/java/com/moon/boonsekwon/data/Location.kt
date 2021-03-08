package com.moon.boonsekwon.data

data class Location(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var title: String = "",
    var description: String? = "",
    var address: String? = "",
    var registerKey: String? = ""
)