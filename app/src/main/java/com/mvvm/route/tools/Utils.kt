package com.mvvm.route.tools

import android.graphics.Bitmap
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class Utils {

    fun addInfoWindow(position: LatLng, map: GoogleMap, title: String, message: String) {
        val invisibleMarker =
            BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        val marker = map.addMarker(
            MarkerOptions()
                .position(position)
                .title(title)
                .snippet(message)
                .alpha(0f)
                .icon(invisibleMarker)
                .anchor(0.5f, 0.5f)
        )
        marker!!.showInfoWindow()
    }

    fun getHumanTimeFormatFromMilliseconds(milliseconds: Long?): String {
        val message: String = if (milliseconds!! >= MILLI_SECONDS) {
            val seconds = (milliseconds / MILLI_SECONDS).toInt() % 60
            val minutes = (milliseconds / (MILLI_SECONDS * 60) % 60).toInt()
            val hours = (milliseconds / (MILLI_SECONDS * 60 * 60) % 24).toInt()
            val days = (milliseconds / (MILLI_SECONDS * 60 * 60 * 24)).toInt()
            if (days == 0 && hours != 0) {
                String.format("%d hr %d min %d sec", hours, minutes, seconds)
            } else if (hours == 0 && minutes != 0) {
                String.format("%d min %d sec", minutes, seconds)
            } else if (days == 0 && hours == 0 && minutes == 0) {
                String.format("%d sec", seconds)
            } else {
                String.format(
                    "%d day %d hr %d m",
                    days,
                    hours,
                    minutes,
                    seconds
                )
            }
        } else {
            "< 0 sec"
        }
        return message
    }

}