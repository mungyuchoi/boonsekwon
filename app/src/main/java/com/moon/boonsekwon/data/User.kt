package com.moon.boonsekwon.data

import androidx.annotation.Keep

@Keep
data class User(
    var name: String? = null,
    var imageUrl: String? = null,
    var point: Int = 0
)