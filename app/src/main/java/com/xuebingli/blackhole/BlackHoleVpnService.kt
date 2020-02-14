package com.xuebingli.blackhole

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.util.Pair
import java.util.concurrent.atomic.AtomicReference

class BlackHoleVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "connect"
        const val ACTION_DISCONNECT = "disconnect"
        const val NOTIFICATION_CHANNEL_ID = "BlackholeVPN"
    }

    private val binder = LocalBinder()
    private lateinit var configureIntent: PendingIntent
    private val connectingThread = object : AtomicReference<Thread>() {
        fun set2(newValue: Thread?) {
            val oldThread = getAndSet(newValue)
            oldThread?.interrupt()
        }
    }
    private val ongoingConnection = object : AtomicReference<Connection>() {
        fun set2(newValue: Connection?) {
            val oldConnection = getAndSet(newValue)
            oldConnection?.apply {
                first!!.interrupt()
                second!!.close()
            }
        }
    }

    override fun onCreate() {
        configureIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                connect()
                return Service.START_NOT_STICKY
            }
            ACTION_DISCONNECT -> {
                disconnect()
                return Service.START_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun isConnected(): Boolean {
        return ongoingConnection.get() != null
    }

    private fun connect() {
        updateForegroundNotification(getString(R.string.notification_vpn_connecting))
        val connection = BlackHoleVpnConnection(configureIntent, this)
        val thread = Thread(connection, "BlackholeVpnThread")
        connectingThread.set2(thread)
        connection.onEstablishListener = {
            connectingThread.compareAndSet(thread, null)
            ongoingConnection.set2(Connection(thread, it))
        }
        connectingThread.get().start()
    }

    private fun disconnect() {
        connectingThread.set2(null)
        ongoingConnection.set2(null)
        stopForeground(true)
    }

    private fun updateForegroundNotification(message: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        startForeground(
            1,
            Notification.Builder(
                this,
                NOTIFICATION_CHANNEL_ID
            ).setSmallIcon(R.mipmap.ic_launcher).setContentText(message).setContentIntent(
                configureIntent
            ).build()
        )
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    inner class LocalBinder: Binder() {
        fun getService(): BlackHoleVpnService = this@BlackHoleVpnService
    }
}

class Connection(first: Thread, second: ParcelFileDescriptor) :
    Pair<Thread, ParcelFileDescriptor>(first, second)