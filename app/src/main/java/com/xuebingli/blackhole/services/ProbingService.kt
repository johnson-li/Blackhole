package com.xuebingli.blackhole.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.xuebingli.blackhole.BuildConfig
import com.xuebingli.blackhole.MyApplication
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.activities.BackgroundActivity
import com.xuebingli.blackhole.activities.ProbingActivity
import com.xuebingli.blackhole.network.ProbingClient
import com.xuebingli.blackhole.restful.*
import com.xuebingli.blackhole.ui.BackgroundService
import com.xuebingli.blackhole.utils.ConfigUtils
import com.xuebingli.blackhole.utils.Constants
import com.xuebingli.blackhole.utils.FileUtils
import com.xuebingli.blackhole.utils.SinkMode
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.*

class ProbingService : Service() {
    companion object {
        private const val PROBING_SERVICE_NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "com.xuebingli.blackhole"
        private const val CHANNEL_NAME = "probing service"
    }

    private val serverApi: ServerApi
        get() = (application as MyApplication).serverApi
    private val binder = ForegroundBinder()
    var isProbing = false
    var listener: ProbingServiceListener? = null
    private lateinit var notification: Notification
    private val disposables = CompositeDisposable()
    private lateinit var channel: NotificationChannel
    private lateinit var probingClient: ProbingClient
    private lateinit var clientID: UUID
    var result = ProbingResult()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    @ExperimentalUnsignedTypes
    fun startProbing(context: Context) {
        result = ProbingResult()
        Intent(context, ProbingService::class.java).also {
            ContextCompat.startForegroundService(context, it)
        }
        clientID = UUID.randomUUID()
        serverApi.request(
            ControlMessage(
                clientID.toString(),
                Request(RequestType.PROBING, delay = ConfigUtils(context).probingDelay)
            )
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (response.status == Status.SUCCESS) {
                    isProbing = true
                    val port = response.port
                    listener?.onStarted()
                    probingClient = ProbingClient(
                        BuildConfig.TARGET_IP, port!!, response.id,
                        ConfigUtils(context).probingDelay
                    ) {
                        if (it.type == ProbingRecordType.RECEIVED) {
                            result.clientResult.received.add(it)
                            listener?.onRecord(it)
                        } else {
                            result.clientResult.sent.add(it)
                            listener?.onRecord(it)
                        }
                    }
                    probingClient.start()
                } else {
                    Toast.makeText(this, R.string.probing_error, Toast.LENGTH_SHORT).show()
                }
            }, {
                Log.e("johnson", it.message, it)
                Toast.makeText(this, R.string.probing_error, Toast.LENGTH_SHORT).show()
            })

        val pendingIntent = Intent(this, ProbingActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, 0)
        }
        notification = NotificationCompat.Builder(this, CHANNEL_ID).setOngoing(true)
            .setSmallIcon(R.drawable.ic_baseline_network_check_24)
            .setContentTitle(getString(R.string.probing_notification_title))
            .setContentText(getString(R.string.probing_notification_text))
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(PROBING_SERVICE_NOTIFICATION_ID, notification)
    }

    fun stopProbing(context: Context) {
        Intent(context, ProbingService::class.java).also {
            context.stopService(it)
        }
        if (::probingClient.isInitialized) {
            probingClient.stop()
        }
        isProbing = false
        serverApi.request(ControlMessage(clientID.toString(), Request(RequestType.STATICS)))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
                result.serverResult.sent.addAll(it.statics!!.probing_sent)
                result.serverResult.received.addAll(it.statics!!.probing_received)
                val file = File(
                    ConfigUtils(this).getDataDir(),
                    "probing_${Constants.LOG_TIME_FORMAT.format(Date())}.json"
                )
                FileUtils().dumpJson(result, file) {
                    Toast.makeText(
                        this, getString(R.string.toast_probing_finished),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, {
                Log.e("johnson", it.message, it)
                Toast.makeText(context, "Failed to read statics from server", Toast.LENGTH_SHORT)
                    .show()
            }).also { disposables.add(it) }
        listener?.onStopped()
    }

    inner class ForegroundBinder : Binder() {
        fun getService(): ProbingService = this@ProbingService
    }

    fun registerListener(listener: ProbingServiceListener) {
        this.listener = listener
    }

    override fun onDestroy() {
        super.onDestroy()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(PROBING_SERVICE_NOTIFICATION_ID)
        disposables.clear()
    }
}

open class ProbingServiceListener {
    open fun onStarted() {}
    open fun onStopped() {}
    open fun onRecord(record: ProbingRecord) {}
}
