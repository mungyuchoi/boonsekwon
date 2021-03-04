package com.moon.boonsekwon.data

data class Location(
    var latitude: String,
    var longitude: String,
    var title: String,
    var description: String?,
    var address: String?,
    var registerKey: String?
)