package tech.ardraw.app

import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


/**
 * Created by samuelcatherasoo on 2018-01-27.
 */
interface Api {

    @GET("api/values")
    fun allDrawings(): Observable<List<Drawing>>

    @GET("api/values")
    fun searchDrawings(@Query("lat") latitude: Double, @Query("lon") longitude: Double): Observable<List<Drawing>>

    @POST("api/values")
    fun addDrawing(@Body drawing: Drawing): Observable<Int>
}