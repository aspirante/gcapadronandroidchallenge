package com.mvvm.route.data.model

import androidx.room.Entity

@Entity
data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    var timestamp: Long = 0L
)
