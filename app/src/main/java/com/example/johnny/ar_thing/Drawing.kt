package com.example.johnny.ar_thing

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.vecmath.Vector3d

/**
 * Created by samuelcatherasoo on 2018-01-27.
 */
data class Drawing (val longitude: Double,
                    val latitude: Double,
                    val altitude: Double,
                    val bearing: Double,
                    val color:Int,
                    val pathData: String) {
    fun getPoints(): ArrayList<Vector3d> =
            Gson().fromJson<ArrayList<Vector3d>>(pathData, object : TypeToken<ArrayList<Vector3d>>() {}.type)
}