package com.xuebingli.blackhole.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telephony.CellInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.activities.BackgroundActivity
import com.xuebingli.blackhole.models.*
import com.xuebingli.blackhole.ui.BackgroundService
import com.xuebingli.blackhole.utils.ConfigUtils
import com.xuebingli.blackhole.utils.Constants
import com.xuebingli.blackhole.utils.Preferences
import com.xuebingli.blackhole.utils.TimeUtils
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.lang.Exception
import java.text.DateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ForegroundService : Service() {
    companion object {
        private const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "com.xuebingli.blackhole"
        private const val CHANNEL_NAME = "foreground service"
        private lateinit var channel: NotificationChannel

        fun updateForegroundService(context: Context) {
            val sharedPreferences = ConfigUtils(context).getSharedPreferences()
            if (BackgroundService.values().map { sharedPreferences.getBoolean(it.prefKey, false) }
                    .any { it }) {
                startForegroundService(context)
            } else {
                stopForegroundService(context)
            }
        }

        private fun startForegroundService(context: Context) {
            Intent(context, ForegroundService::class.java).also {
                ContextCompat.startForegroundService(context, it)
            }
        }

        private fun stopForegroundService(context: Context) {
            Intent(context, ForegroundService::class.java).also {
                context.stopService(it)
            }
        }
    }

    private lateinit var notification: Notification
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var periodObservable: Observable<MeasurementResult>
    private lateinit var client: FusedLocationProviderClient
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var subscriptionManager: SubscriptionManager
    private val disposables = CompositeDisposable()
    private val logFileName = "measurement_${Constants.LOG_TIME_FORMAT.format(Date())}.txt"
    private var locationEnabled = false
    private var cellInfoEnabled = false
    private var subscriptionInfoEnabled = false
    private var frequency = 100 // in milliseconds
    private var latestLocation: GpsLocation? = null
    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            p0?.lastLocation?.apply {
                latestLocation =
                    GpsLocation(
                        time, System.currentTimeMillis(), TimeUtils().elapsedRealTime(),
                        latitude, longitude, accuracy
                    )
                LocationManager.GPS_PROVIDER
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        subscriptionManager =
            getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        client = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = ConfigUtils(this).getSharedPreferences()
        channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        updateParameters()
        updateBackgroundTask()
    }

    private fun updateParameters(): Boolean {
        locationEnabled =
            sharedPreferences.getBoolean(BackgroundService.LOCATION.prefKey, locationEnabled)
        cellInfoEnabled =
            sharedPreferences.getBoolean(BackgroundService.CELL_INFO.prefKey, cellInfoEnabled)
        subscriptionInfoEnabled = sharedPreferences.getBoolean(
            BackgroundService.SUBSCRIPTION_INFO.prefKey,
            subscriptionInfoEnabled
        )
        val frequencyNew = sharedPreferences.getInt(Preferences.FREQUENCY_KEY, frequency)
        val frequencyUpdated = frequencyNew == frequency
        frequency = frequencyNew
        return frequencyUpdated
    }

    private fun getCellularInfo(): List<CellInfoModel> {
        telephonyManager.requestCellInfoUpdate(
            Executors.newCachedThreadPool(),
            object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                }
            })
        return telephonyManager.allCellInfo.map {
            getCellInfoModel(it)
        }
    }

    private fun getNetworkInfo(): CellNetworkInfo {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netId = cm.activeNetwork.toString()
        cm.getNetworkCapabilities(cm.activeNetwork)?.linkDownstreamBandwidthKbps
        val downLink = cm.getNetworkCapabilities(cm.activeNetwork)?.linkDownstreamBandwidthKbps
        val upLink = cm.getNetworkCapabilities(cm.activeNetwork)?.linkUpstreamBandwidthKbps
        return CellNetworkInfo(netId = netId, downLink=downLink, upLink = upLink,
            networkType = telephonyManager.dataNetworkType)
    }

    private fun getSubscriptionInfo(): List<SubscriptionInfoModel> {
        return subscriptionManager.activeSubscriptionInfoList.map {
            getSubscriptionInfoModel(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateBackgroundTask() {
        disposables.clear()
        periodObservable =
            Observable.interval(frequency.toLong(), TimeUnit.MILLISECONDS)
                .flatMap {
                    Observable.create<MeasurementResult> {
                        it.onNext(
                            MeasurementResult(
                                location = if (locationEnabled) latestLocation else null,
                                cellInfoList = if (cellInfoEnabled) getCellularInfo() else null,
                                subscriptionInfoList = if (cellInfoEnabled) getSubscriptionInfo() else null,
                                networkInfo = getNetworkInfo(),
                            )
                        )
                    }
                }
        periodObservable.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
            .subscribe({
                File(ConfigUtils(this).getDataDir(), logFileName).apply {
                    appendText(Gson().toJson(it) + "\n")
                }
            }, {

            }).also { disposables.add(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        onPreferenceUpdated()
        if (updateParameters()) {
            updateBackgroundTask()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(FOREGROUND_SERVICE_NOTIFICATION_ID)
        client.removeLocationUpdates(locationCallback)
        disposables.clear()
    }

    private fun onPreferenceUpdated() {
        val pendingIntent = Intent(this, BackgroundActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, 0)
        }
        val enabledServices =
            BackgroundService.values().filter { sharedPreferences.getBoolean(it.prefKey, false) }
                .map { it.getName(this) }
        notification = NotificationCompat.Builder(this, CHANNEL_ID).setOngoing(true)
            .setSmallIcon(R.drawable.ic_baseline_network_check_24)
            .setContentTitle(getString(R.string.background_notification_title))
            .setContentText(
                getString(
                    R.string.background_notification_text,
                    enabledServices.joinToString { it.toLowerCase(Locale.getDefault()) }
                )
            )
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
        if (sharedPreferences.getBoolean(BackgroundService.LOCATION.prefKey, false)) {
            requestLocation()
        }
    }

    private fun requestLocation() {
        val request = LocationRequest().apply {
            interval = 5
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            client.requestLocationUpdates(request, locationCallback, null)
        } else {
            Log.e("johnson", "Location permission is not granted")
        }
    }

    private val binder = ForegroundBinder()

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    inner class ForegroundBinder : Binder() {
        fun getService(): ForegroundService = this@ForegroundService
    }
}

data class MeasurementResult(
    val timeStamp: Long = System.currentTimeMillis(),
    val dateTime: String = DateFormat.getDateTimeInstance().format(Date()),
    val location: GpsLocation? = null,
    val cellInfoList: List<CellInfoModel>? = null,
    val subscriptionInfoList: List<SubscriptionInfoModel>? = null,
    val networkInfo: CellNetworkInfo? = null
)