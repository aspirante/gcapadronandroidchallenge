package com.mvvm.route.tools

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mvvm.route.data.model.Coordinates
import java.util.*

class ConverterHelper {
    private val gson = Gson()
    @TypeConverter
    fun stringToList(data: String?): List<Coordinates> {
        if (data == null) {
            return Collections.emptyList()
        }

        val listType = object : TypeToken<List<Coordinates>>() {}.type

        return gson.fromJson(data, listType)
    }

    @TypeConverter
    fun listToString(someObjects: List<Coordinates>): String {
        return gson.toJson(someObjects)
    }

}