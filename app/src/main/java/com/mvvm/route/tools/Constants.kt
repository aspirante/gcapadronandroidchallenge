package com.mvvm.route.tools

const val DATABASE_NAME = "routeDB"
const val LATITUDE = "latitude"
const val LONGITUDE = "longitude"
const val TIMESTAMP = "timestamp"
const val KM = 1000
const val MILLI_SECONDS = 1000
const val NAME_LENGTH = 3
const val URL_SHARE_LOCATION = "http://maps.google.com/maps?f=d&hl=en&saddr=%f,%f&daddr=%f,%f"
const val SHARE_TEXT = "Route: %s \nDistance: %3.1f \nDuration: %s"
const val SHARE_WITH_TEXT = "Share with: "
const val UPDATE_INTERVAL = 5L * MILLI_SECONDS  /* 5 secs */
const val FASTEST_INTERVAL: Long = 2L * MILLI_SECONDS /* 2 sec */
const val JSON_NAME = "name"
const val JSON_COORDINATES = "coordinates"
