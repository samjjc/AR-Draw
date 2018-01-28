package com.example.johnny.ar_thing

import retrofit2.http.GET
import io.reactivex.Flowable
import io.reactivex.Observable
import retrofit2.http.POST
import retrofit2.http.Query


/**
 * Created by samuelcatherasoo on 2018-01-27.
 */
interface Api {

    @GET("endpoint")
    fun searchDrawings(@Query("q") query: String): Flowable<List<Drawing>>

    @POST("endpoint")
    fun AddDrawing(): Observable<Drawing>
}