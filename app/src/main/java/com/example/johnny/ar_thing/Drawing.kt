package com.example.johnny.ar_thing

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.vecmath.Vector3f

/**
 * Created by samuelcatherasoo on 2018-01-27.
 */
data class Drawing (val longitude: Double,
                    val latitude: Double,
                    val altitude: Double,
                    val bearing: Double,
                    val color:Int,
                    var pathData: String) {
    fun getPoints(): ArrayList<Vector3f> =
            Gson().fromJson<ArrayList<Vector3f>>(pathData, object : TypeToken<ArrayList<Vector3f>>() {}.type)

    fun setPoints(points: ArrayList<Vector3f>) {
        pathData = Gson().toJson(points)
    }
}