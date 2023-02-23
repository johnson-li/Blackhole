package com.xuebingli.blackhole.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.databinding.ActivityLaptopAssistantBinding
import com.xuebingli.blackhole.restful.LaptopApi
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LaptopAssistantActivity : BaseActivity() {
    private lateinit var binding: ActivityLaptopAssistantBinding
    private lateinit var locationClient: FusedLocationProviderClient
    private var api: LaptopApi? = null
    private var logging = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_laptop_assistant)
        locationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun createServerApi(): LaptopApi {
        val interceptor =
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .build()
        val ip = binding.serverIp.text
        val retrofit = Retrofit.Builder()
            .baseUrl("http://$ip:8085")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .client(client)
            .build()
        return retrofit.create(LaptopApi::class.java)
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }

    fun toggle(view: View) {
        if (api == null) {
            val tmpApi = createServerApi()
            subscribe(tmpApi.test()) {
                api = tmpApi
                binding.button.text = "Start"
                logging = false
            }
        } else {
            if (logging) {
                logging = false
                binding.button.text = "Start"
            } else {
                cellInfoObservable =
                    Observable.interval(observationInterval, TimeUnit.MILLISECONDS).flatMap {
                        Observable.create {
                            telephonyManager.requestCellInfoUpdate(
                                measurementThreadPool,
                                cellInfoCallback!!
                            )
                        }
                    }
                cellInfoObservable!!.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .subscribe { }.also { disposables.add(it) }
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(1)
                    .setMaxUpdateDelayMillis(100)
                    .build()
                val permission =
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                if (permission == PackageManager.PERMISSION_GRANTED) {
                    locationClient.requestLocationUpdates(request, object : LocationCallback() {
                        override fun onLocationResult(p0: LocationResult) {
                            p0.lastLocation?.also {
                                subscribe(api!!.log(it)) {
                                    binding.logInfo.text = "Logging GPS: $it"
                                }
                            }
                        }
                    }, null)
                } else {
                    Log.e("johnson", "Location permission is not granted")
                }
                logging = true
                binding.button.text = "Stop"
            }
        }
    }
}