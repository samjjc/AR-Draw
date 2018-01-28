package com.example.johnny.ar_thing

/**
 * Created by samuelcatherasoo on 2018-01-27.
 */


data class Drawing (val longitude: Double,
                    val latitude: Double,
                    val altitude: Double,
                    val bearing: Double,
                    val color:Int,
                    val pathData: List<Double>)