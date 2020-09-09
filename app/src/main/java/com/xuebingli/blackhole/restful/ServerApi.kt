package com.xuebingli.blackhole.restful

import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.POST

interface ServerApi {

    @POST("/request")
    fun request(@Body request: ControlMessage): Single<Response>
}