package com.xuebingli.blackhole.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.telephony.CellInfo
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.CellInfoCallback
import android.util.Log
import androidx.core.app.ActivityCompat
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
import com.xuebingli.blackhole.utils.TimeUtils
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.DateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ForegroundService : Service() {
    companion object {
        private const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "com.xuebingli.blackhole"
        private const val CHANNEL_NAME = "foreground service"
        private lateinit var notificationChannel: NotificationChannel

        fun updateForegroundService(context: Context) {
            val sharedPreferences = ConfigUtils(context).getSharedPreferences()
            if (BackgroundService.values().map { sharedPreferences.getBoolean(it.prefKey, false) }
                    .any { it }) {
                startForegroundService(context)
            } else {
                stopForegroundService(context)
            }
        }

        fun startForegroundService(context: Context) {
            Intent(context, ForegroundService::class.java).also {
                ContextCompat.startForegroundService(context, it)
            }
        }

        fun stopForegroundService(context: Context) {
            Intent(context, ForegroundService::class.java).also {
                context.stopService(it)
            }
        }
    }

    private val threadPool = Executors.newCachedThreadPool()
    private lateinit var notification: Notification
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var periodObservable: Observable<MeasurementResult>
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var subscriptionManager: SubscriptionManager
    private val binder = ForegroundBinder()
    private val disposables = CompositeDisposable()
    private val logFileName = "measurement_${Constants.LOG_TIME_FORMAT.format(Date())}.txt"
    private var locationEnabled = false
    private var cellInfoEnabled = false
    private var subscriptionInfoEnabled = false
    private var frequency = 100 // in milliseconds
    var measurementStarted = false
        private set
    private var latestLocation: GpsLocation? = null
    private var measurement: Measurement? = null
    private var locationCallback: MyLocationCallback? = null
    private var cellInfoCallback: MyCellInfoCallback? = null
    private var cellInfoObservable: Observable<MutableList<CellInfo>>? = null
    private var subscriptionInfoObservable: Observable<MutableList<SubscriptionInfo>>? = null
    private val observationInterval = 300L

    override fun onCreate() {
        super.onCreate()
        initNotification()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        subscriptionManager =
            getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        showNotification()
    }

    private fun initNotification() {
        notificationChannel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
        notificationChannel.lightColor = Color.BLUE
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(notificationChannel)
    }

    private fun showNotification() {
        val pendingIntent = Intent(this, BackgroundActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        notification = NotificationCompat.Builder(this, CHANNEL_ID).setOngoing(true)
            .setSmallIcon(R.drawable.ic_baseline_network_check_24)
            .setContentTitle(getString(R.string.background_notification_title))
            .setContentText(
                getString(
                    R.string.background_notification_text,
                    measurement?.setups?.map { it.key.name }?.joinToString { it } ?: "None"
                )
            )
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
    }

    private fun hideNotification() {
//        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
//            .cancel(FOREGROUND_SERVICE_NOTIFICATION_ID)
    }

    private fun startPingRecording(records: Records) {

    }

    private fun startSubscriptionInfoRecording(records: Records) {
        subscriptionInfoObservable =
            Observable.interval(observationInterval, TimeUnit.MILLISECONDS).flatMap {
                Observable.create {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        subscriptionManager.activeSubscriptionInfoList.forEach {
                            records.appendRecord(SubscriptionRecord(getSubscriptionInfoModel(it)))
                        }
                    }
                }
            }
        subscriptionInfoObservable!!.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
            .subscribe { }.also { disposables.add(it) }
    }

    private fun startUdpPingRecording(records: Records) {
        val serverIP = "195.148.127.230"
        val serverPort = 8877
        threadPool.execute {
        }
    }

    private fun startCellularInfoRecording(records: Records) {
        cellInfoCallback = MyCellInfoCallback(records)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        cellInfoObservable =
            Observable.interval(observationInterval, TimeUnit.MILLISECONDS).flatMap {
                Observable.create {
                    telephonyManager.requestCellInfoUpdate(threadPool, cellInfoCallback!!)
                }
            }
        cellInfoObservable!!.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
            .subscribe { }.also { disposables.add(it) }
    }

    private fun startLocationRecording(records: Records) {
        locationCallback = MyLocationCallback(records)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1)
            .setMaxUpdateDelayMillis(100)
            .build()
        val permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            locationClient.requestLocationUpdates(request, locationCallback!!, null)
        } else {
            Log.e("johnson", "Location permission is not granted")
        }
    }

    private fun getNetworkInfo(): CellNetworkInfo {
        val cm =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netId = cm.activeNetwork.toString()
        cm.getNetworkCapabilities(cm.activeNetwork)?.linkDownstreamBandwidthKbps
        val downLink = cm.getNetworkCapabilities(cm.activeNetwork)?.linkDownstreamBandwidthKbps
        val upLink = cm.getNetworkCapabilities(cm.activeNetwork)?.linkUpstreamBandwidthKbps
        return CellNetworkInfo(
            netId = netId,
            downLink = downLink,
            upLink = upLink,
            networkType = telephonyManager.dataNetworkType
        )
    }

    private fun getSubscriptionInfo(): List<SubscriptionInfoModel> {
        return subscriptionManager.activeSubscriptionInfoList.map {
            getSubscriptionInfoModel(it)
        }
    }

    private fun updateBackgroundTask() {
        disposables.clear()
        periodObservable =
            Observable.interval(frequency.toLong(), TimeUnit.MILLISECONDS)
                .flatMap {
                    Observable.create<MeasurementResult> {
                        it.onNext(
                            MeasurementResult(
                                location = if (locationEnabled) latestLocation else null,
//                                cellInfoList = if (cellInfoEnabled) getCellularInfo() else null,
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

    override fun onDestroy() {
        super.onDestroy()
        stopMeasurement()
    }

    fun startMeasurement(measurement: Measurement) {
        if (measurementStarted) {
            return
        }
        this.measurement = measurement
        startMeasurement0()
        measurementStarted = true
    }

    private fun startMeasurement0() {
        showNotification()
        measurement!!.startedAt = System.currentTimeMillis()
        measurement!!.recordSet.forEach { (setup, records) ->
            when (setup.key) {
                MeasurementKey.CellularInfo -> startCellularInfoRecording(records)
                MeasurementKey.LocationInfo -> startLocationRecording(records)
                MeasurementKey.Ping -> startPingRecording(records)
                MeasurementKey.SubscriptionInfo -> startSubscriptionInfoRecording(records)
                MeasurementKey.UdpPing -> startUdpPingRecording(records)
            }
        }
    }

    fun stopMeasurement(): String {
        if (!measurementStarted) {
            return ""
        }
        stopMeasurement0()
        measurementStarted = false
        return recordMeasurement0()
    }

    private fun stopMeasurement0() {
        hideNotification()
        locationCallback?.let { locationClient.removeLocationUpdates(it) }
        cellInfoCallback?.records = null
        disposables.clear()
    }

    private fun recordMeasurement0(): String {
        val dir = Environment.getExternalStorageDirectory().resolve("Blackhole")
        dir.mkdirs()
        val path = "measurement_${measurement!!.createdAt}.json"
        threadPool.execute {
            val data = Gson().toJson(measurement)
            dir.resolve(path).writeBytes(data.encodeToByteArray())
        }
        return path
    }

    override fun onBind(p0: Intent?): IBinder = binder

    inner class ForegroundBinder : Binder() {
        fun getService(): ForegroundService = this@ForegroundService
    }
}

data class MeasurementResult(
    val timeStamp: Long = TimeUtils().getTimeStamp(),
    val timeStampLocal: Long = TimeUtils().getTimeStampAccurate(),
    val dateTime: String = DateFormat.getDateTimeInstance().format(Date()),
    val location: GpsLocation? = null,
    val cellInfoList: List<CellInfoModel>? = null,
    val subscriptionInfoList: List<SubscriptionInfoModel>? = null,
    val networkInfo: CellNetworkInfo? = null
)

class UdpPingThread(private val serverIP: String, private val serverPort: Int) : Runnable {
    override fun run() {
        try {
            val socket = DatagramSocket()
            val sendData = "a".toByteArray()
            val sendPacket =
                DatagramPacket(
                    sendData,
                    sendData.size,
                    InetAddress.getByName(serverIP),
                    serverPort
                )
            val sendTs = System.currentTimeMillis()
            socket.send(sendPacket)
            val recvBuffer = ByteArray(100)
            val recvPacket = DatagramPacket(recvBuffer, recvBuffer.size)
            socket.receive(recvPacket)
            val recvTs = System.currentTimeMillis()
            Log.d("johnson", (recvTs - sendTs).toString())
        } catch (e: IOException) {
            Log.e("johnson", e.message, e)
        }
    }
}

class MyCellInfoCallback(var records: Records?) : CellInfoCallback() {
    override fun onCellInfo(p0: MutableList<CellInfo>) {
        p0.forEach { cellInfo ->
            records?.appendRecord(CellularRecord(getCellInfoModel(cellInfo)))
        }
    }
}

class MyLocationCallback(private val records: Records) : LocationCallback() {
    override fun onLocationResult(p0: LocationResult) {
        p0.lastLocation?.apply {
            records.appendRecord(LocationRecord(latitude, longitude, accuracy.toDouble()))
        }
    }
}