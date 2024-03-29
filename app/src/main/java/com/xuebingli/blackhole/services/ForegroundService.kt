package com.xuebingli.blackhole.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.SystemClock
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
import com.wandroid.traceroute.TraceRoute
import com.wandroid.traceroute.TraceRouteCallback
import com.wandroid.traceroute.TraceRouteResult
import com.xuebingli.blackhole.NetworkUtil
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.activities.BackgroundActivity
import com.xuebingli.blackhole.models.*
import com.xuebingli.blackhole.ui.BackgroundService
import com.xuebingli.blackhole.utils.ConfigUtils
import com.xuebingli.blackhole.utils.TimeUtils
import com.xuebingli.blackhole.utils.getDnsServers
import com.xuebingli.blackhole.utils.getTimeStampAccurate
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.IOException
import java.io.InterruptedIOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.DatagramChannel
import java.util.concurrent.ConcurrentHashMap
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

        private fun stopForegroundService(context: Context) {
            Intent(context, ForegroundService::class.java).also {
                context.stopService(it)
            }
        }
    }

    private var measurementThreadPool = Executors.newCachedThreadPool()
    private lateinit var notification: Notification
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var subscriptionManager: SubscriptionManager
    private val binder = ForegroundBinder()
    private val disposables = CompositeDisposable()
    var measurementStarted = false
        private set
    private var measurement: Measurement? = null
    private var udpPingRunnable: UdpPingRunnable? = null
    private var locationCallback: MyLocationCallback? = null
    private var cellInfoCallback: MyCellInfoCallback? = null
    private var cellInfoObservable: Observable<MutableList<CellInfo>>? = null
    private var subscriptionInfoObservable: Observable<MutableList<SubscriptionInfo>>? = null
    private var networkInfoObservable: Observable<MutableList<CellNetworkInfo>>? = null
    private var tracerouteObservable: Observable<MutableList<String>>? = null
    var handler: Handler? = null
    private val observationInterval = 300L
    private val pingProcesses = ConcurrentHashMap<String, Pair<Process, Records>>()
    private val pingReaderThread = object : Thread() {
        override fun run() {
            while (!interrupted()) {
                val iterator = pingProcesses.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val process = entry.value.first
                    val records = entry.value.second
                    try {
                        val reader = process.inputStream.bufferedReader()
                        val line = reader.readLine()
                        val res = "\\d+ bytes from .* time=([0-9.]+) ms".toRegex().matchEntire(line)
                        res?.groups?.get(1)?.value?.toFloat()?.also {
                            records.appendRecord(PingRecord(it))
                        }
                    } catch (e: InterruptedIOException) {
                        Log.w("johnson", e.message, e)
                    } catch (e: IOException) {
                        Log.w("johnson", e.message, e)
                    }
                }
            }
        }
    }

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
        val setup = records.setup as PingMeasurementSetup
        val server = setup.serverIP
        if (!pingProcesses.containsKey(server)) {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("/system/bin/ping -i.2 $server")
            pingProcesses[server] = Pair(process, records)
            process.inputStream.bufferedReader()
            if (!pingReaderThread.isAlive) {
                pingReaderThread.start()
            }
        }
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
        (records.setup as UdpPingMeasurementSetup).let {
            udpPingRunnable = UdpPingRunnable(it.serverIP, it.serverPort, it.interval, records)
            measurementThreadPool.execute(udpPingRunnable)
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
                    telephonyManager.requestCellInfoUpdate(
                        measurementThreadPool,
                        cellInfoCallback!!
                    )
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

    fun maybeAddTmpPingMeasurement(ip: String) {
        if (!measurement!!.setups.filterIsInstance<PingMeasurementSetup>()
                .any { it.serverIP == ip }
        ) {
            val setup = PingMeasurementSetup().also {
                it.tmp = true
                it.serverIP = ip
            }
            if (measurement!!.addMeasurement(setup)) {
                startPingRecording(measurement!!.recordSet[setup]!!)
                handler?.dispatchMessage(Message())
            }
        }
    }

    private fun startTracerouteRecording(records: Records) {
        val setup = records.setup as TracerouteMeasurementSetup
        tracerouteObservable =
            Observable.interval(setup.interval.toLong(), TimeUnit.MILLISECONDS).flatMap {
                Observable.create {
                    try {
                        val tr = TraceRoute()
                        tr.setCallback(object : TraceRouteCallback() {
                            override fun onSuccess(traceRouteResult: TraceRouteResult) {
                                val trace = mutableListOf<TracerouteRecordItem>()
                                traceRouteResult.message.split("\n").forEach { line ->
//                                    Log.d("johnson", line)
                                    val res = "[0-9.]+ ms".toRegex().findAll(line)
                                    if (res.any()) {
                                        val rtt = res.map {
                                            it.value.substring(0, it.value.length - 3).toFloat()
                                        }.min()
                                        val hop = line.substring(1, 2).toInt()
                                        val ip =
                                            "\\d+\\.\\d+\\.\\d+\\.\\d+".toRegex().find(line)?.value
                                                ?: "Unknown"
                                        trace.add(TracerouteRecordItem(hop, rtt, ip))
                                        maybeAddTmpPingMeasurement(ip)
                                    }
                                }
                                if (trace.isNotEmpty()) {
                                    records.appendRecord(TracerouteRecord(trace))
                                }
                            }
                        })
                        tr.traceRoute(setup.serverIP)
                    } catch (e: java.lang.Exception) {
                        Log.e("johnson", e.message, e)
                    }
                }
            }
        tracerouteObservable!!.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
            .subscribe { }.also { disposables.add(it) }
    }

    private fun startNetworkInfoRecording(records: Records) {
        networkInfoObservable =
            Observable.interval(observationInterval, TimeUnit.MILLISECONDS).flatMap {
                Observable.create {
                    val cm =
                        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val netId = cm.activeNetwork.toString()
                    cm.getNetworkCapabilities(cm.activeNetwork)?.linkDownstreamBandwidthKbps
                    val downLink =
                        cm.getNetworkCapabilities(cm.activeNetwork)?.linkDownstreamBandwidthKbps
                    val upLink =
                        cm.getNetworkCapabilities(cm.activeNetwork)?.linkUpstreamBandwidthKbps
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val dnsServers =
                            getDnsServers(getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager)
                        val dnsServer =
                            if (dnsServers.isEmpty()) null else dnsServers[0].hostAddress
                        dnsServer?.let { maybeAddTmpPingMeasurement(it) }
                        CellNetworkInfo(
                            netId, downLink, upLink, telephonyManager.dataNetworkType, dnsServer
                        ).let {
                            records.appendRecord(NetworkInfoRecord(it))
                        }
                    }
                }
            }
        networkInfoObservable!!.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
            .subscribe {}.also { disposables.add(it) }
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
        measurementThreadPool = Executors.newCachedThreadPool()
        showNotification()
        measurement!!.startedAt = getTimeStampAccurate()
        measurement!!.recordSet.forEach { (setup, records) ->
            when (setup.key) {
                MeasurementKey.CellularInfo -> startCellularInfoRecording(records)
                MeasurementKey.LocationInfo -> startLocationRecording(records)
                MeasurementKey.Ping -> startPingRecording(records)
                MeasurementKey.SubscriptionInfo -> startSubscriptionInfoRecording(records)
                MeasurementKey.UdpPing -> startUdpPingRecording(records)
                MeasurementKey.NetworkInfo -> startNetworkInfoRecording(records)
                MeasurementKey.Traceroute -> startTracerouteRecording(records)
            }
        }
    }

    fun stopMeasurement() {
        if (!measurementStarted) {
            return
        }
        stopMeasurement0()
        measurementThreadPool.shutdownNow()
        measurementStarted = false
        recordMeasurement0()
    }

    private fun stopMeasurement0() {
        hideNotification()
        locationCallback?.let { locationClient.removeLocationUpdates(it) }
        cellInfoCallback?.records = null
        udpPingRunnable?.stop = true
        disposables.clear()
        pingProcesses.forEach {
            it.value.first.destroy()
        }
        pingProcesses.clear()
    }

    private fun recordMeasurement0() {
        val dir = getExternalFilesDir("Blackhole")!!
        dir.mkdirs()
        val path = "measurement_${measurement!!.createdAt}.json"
        object : Thread() {
            override fun run() {
                val data = Gson().toJson(measurement)
                dir.resolve(path).writeBytes(data.encodeToByteArray())
            }
        }.start()
    }

    override fun onBind(p0: Intent?): IBinder = binder

    inner class ForegroundBinder : Binder() {
        fun getService(): ForegroundService = this@ForegroundService
    }
}

class UdpPingRunnable(
    private val serverIP: String,
    private val serverPort: Int,
    private val interval: Int,
    private val records: Records
) : Runnable {
    var stop = false
    private var pktId = 0
    private val capacity = 5000
    private val sendTsRecord = LongArray(capacity)

    override fun run() {
        var lastTs = getTimeStampAccurate()
        val channel = DatagramChannel.open()
        channel.configureBlocking(false)
        val serverAddress = InetSocketAddress(serverIP, serverPort)
        val sendBuffer = ByteBuffer.allocate(100)
        while (!stop && !Thread.currentThread().isInterrupted) {
            if (pktId > 0) {
                val recvBuffer = ByteBuffer.allocate(100)
                try {
                    channel.receive(recvBuffer)
                } catch (e: ClosedByInterruptException) {
                    Log.w("johnson", e.message, e)
                    break
                }
                recvBuffer.flip()
                if (recvBuffer.remaining() > 0) {
                    val bytes = ByteArray(recvBuffer.remaining())
                    recvBuffer.get(bytes)
                    val id = String(bytes).toInt()
                    records.appendRecord(
                        UdpPingRecord(
                            id,
                            sendTsRecord[id % capacity],
                            getTimeStampAccurate()
                        )
                    )
                    sendTsRecord[id % capacity] = 0
//                    Log.d("johnson", "Received pkt $id")
                }
            }
            val ts = getTimeStampAccurate()
            if (ts - lastTs < interval) {
                continue
            }
            lastTs = ts
//            sendBuffer.clear()
//            sendBuffer.put(pktId.toString().encodeToByteArray())
            try {
                if (sendTsRecord[pktId % capacity] > 0) {
                    records.appendRecord(
                        UdpPingRecord(
                            pktId - capacity,
                            sendTsRecord[pktId % capacity],
                            -1
                        )
                    )
                }
                sendTsRecord[pktId % capacity] = getTimeStampAccurate()
                channel.send(ByteBuffer.wrap(pktId.toString().toByteArray()), serverAddress)
                pktId += 1
//                Log.d("johnson", "Sent pkt $pktId")
            } catch (e: SocketException) {

            } catch (e: IOException) {
                Log.e("johnson", e.message, e)
            }
        }
        for (i in (pktId - capacity + 1).coerceAtLeast(0)..capacity) {
            if (sendTsRecord[i % capacity] > 0) {
                records.appendRecord(UdpPingRecord(i, sendTsRecord[i % capacity], -1))
            }
        }
    }
}

class MyCellInfoCallback(var records: Records?) : CellInfoCallback() {
    private var lastResult: List<CellInfo>? = null
    override fun onCellInfo(p0: MutableList<CellInfo>) {
        if (lastResult != p0) {
            lastResult = p0
            p0.forEach { cellInfo ->
                records?.appendRecord(CellularRecord(getCellInfoModel(cellInfo)))
            }
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