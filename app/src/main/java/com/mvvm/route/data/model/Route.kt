package com.mvvm.route.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mvvm.route.tools.ConverterHelper

@Entity
data class Route(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    @field:TypeConverters(ConverterHelper::class)
    val coordinates: List<Coordinates> = listOf()
)
