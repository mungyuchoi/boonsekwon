package com.moon.boonsekwon.data

data class Location(
    var latitude: Double,
    var longitude: Double,
    var title: String,
    var description: String?,
    var address: String?,
    var registerKey: String?
)