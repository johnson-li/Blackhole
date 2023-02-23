package com.xuebingli.blackhole.restful

import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LaptopApi {

    @POST("/log")
    fun log(@Body request: Any): Single<HttpResponse>

    @GET("/test")
    fun test(): Single<HttpResponse>
}