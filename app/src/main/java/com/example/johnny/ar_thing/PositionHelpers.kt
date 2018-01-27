package com.example.johnny.ar_thing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager

/**
 * Created by derek on 1/27/2018.
 * A helper object for position-related functions (lat/lon, bearing)
 */
object PositionHelpers {

    fun getCurrentLocation(context: Context): Location? {
        val locationService = context.getSystemService(LocationManager::class.java)

        return if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationService.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else {
            null
        }
    }

    fun scheduleLocationUpdates(context: Context, listener: LocationListener) {
        val locationService = context.getSystemService(LocationManager::class.java)

        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationService.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0F, listener)
        }
    }
}