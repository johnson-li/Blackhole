package com.xuebingli.blackhole.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.CellInfo
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.CellInfoCallback
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.databinding.ActivityLaptopAssistantBinding
import com.xuebingli.blackhole.models.CellularRecord
import com.xuebingli.blackhole.models.LocationRecord
import com.xuebingli.blackhole.models.getCellInfoModel
import com.xuebingli.blackhole.restful.LaptopApi
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.BiConsumer
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LaptopAssistantActivity : BaseActivity0() {
    private lateinit var binding: ActivityLaptopAssistantBinding
    private lateinit var locationClient: FusedLocationProviderClient
    private var api: LaptopApi? = null
    private var logging = false
    private val observationInterval = 300L
    private lateinit var telephonyManager: TelephonyManager
    private var measurementThreadPool = Executors.newCachedThreadPool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_laptop_assistant)
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
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

    fun toggle(view: View) {
        if (api == null) {
            val tmpApi = createServerApi()
            tmpApi.test().subscribeOn(Schedulers.io()).subscribe({
                api = tmpApi
                binding.button.text = "Start"
                logging = false
            }, {
                Toast.makeText(this, "Failed to connect to the server", Toast.LENGTH_SHORT).show()
            }).also { localDisposable.add(it) }
        } else {
            if (logging) {
                logging = false
                binding.button.text = "Start"
                localDisposable.dispose()
                localDisposable = CompositeDisposable()
            } else {
                if (!checkPermissions()) {
                    Toast.makeText(this, R.string.permission_missing, Toast.LENGTH_SHORT).show()
                    return
                }
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
                            if (logging) {
                                p0.lastLocation?.also {
                                    val loc = LocationRecord(
                                        it.latitude,
                                        it.longitude,
                                        it.accuracy.toDouble()
                                    )
                                    Log.d("johnson", "Logging GPS: $loc")
                                    api?.log(loc)?.subscribe({
                                        binding.logInfo.text = "Logging GPS: $loc"
                                    }, {})?.also { localDisposable.add(it) }
                                }
                            }
                        }
                    }, null)
                    val cellInfoObservable: Observable<Void> =
                        Observable.interval(observationInterval, TimeUnit.MILLISECONDS).flatMap {
                            Observable.create {
                                telephonyManager.requestCellInfoUpdate(
                                    measurementThreadPool,
                                    object : CellInfoCallback() {
                                        override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                                            if (logging) {
                                                val data = cellInfo.map {
                                                    CellularRecord(
                                                        getCellInfoModel(it)
                                                    )
                                                }
                                                Log.d("johnson", "Logging CellInfo: $data")
                                                api?.log(data)?.subscribe({
                                                    binding.logInfo.text = "Logging CellInfo: $data"
                                                }, {})?.also { localDisposable.add(it) }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    cellInfoObservable.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .subscribe { }.also { localDisposable.add(it) }
                } else {
                    Log.e("johnson", "Location permission is not granted")
                }
                logging = true
                binding.button.text = "Stop"
            }
        }
    }
}