package com.xuebingli.blackhole

import android.app.Application
import com.xuebingli.blackhole.restful.ServerApi
import com.xuebingli.blackhole.utils.ConfigUtils
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MyApplication : Application() {

    lateinit var serverApi: ServerApi

    override fun onCreate() {
        super.onCreate()
        serverApi = createServerApi()
    }

    private fun createServerApi(): ServerApi {
        val interceptor =
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .build()
        val ip = ConfigUtils(applicationContext).targetIP
        val port = ConfigUtils(applicationContext).getTargetPort()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://$ip:$port")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .client(client)
            .build()
        return retrofit.create(ServerApi::class.java)
    }
}