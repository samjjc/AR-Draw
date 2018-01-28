package com.example.johnny.ar_thing

import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


/**
 * Created by samuelcatherasoo on 2018-01-27.
 */
interface Api {

    @GET("endpoint")
    fun allDrawings(): Observable<List<Drawing>>

    @GET("endpoint")
    fun searchDrawings(@Query("lat") latitude: Double, @Query("lon") longitude: Double): Observable<List<Drawing>>

    @POST("endpoint")
    fun addDrawing(@Body drawing: Drawing): Observable<Int>
}