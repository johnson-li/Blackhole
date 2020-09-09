package com.xuebingli.blackhole

import android.app.PendingIntent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit

class BlackHoleVpnConnection(
    private val configureIntent: PendingIntent,
    private val vpnService: VpnService
) : Runnable {
    companion object {
        const val MAX_PACKET_SIZE = Short.MAX_VALUE.toInt()
        val IDLE_INTERVAL_MS = TimeUnit.MILLISECONDS.toMillis(100)
        const val PROXY_MODE = false
    }

    lateinit var onEstablishListener: (ParcelFileDescriptor) -> Unit

    override fun run() {
        val tunnel = DatagramChannel.open()
        if (!vpnService.protect(tunnel.socket())) {
            throw IllegalStateException("Cannot protect the tunnel")
        }

        // Do something to the tunnel
        //

        val iface = handshake(tunnel)
        val inputStream = FileInputStream(iface.fileDescriptor)
        val outputStream = FileOutputStream(iface.fileDescriptor)
        val packet = ByteBuffer.allocate(MAX_PACKET_SIZE)
        var idle: Boolean
        try {
            while (true) {
                idle = true
                var length = inputStream.read(packet.array())
                if (length > 0) {
                    Log.d("johnson", "received data from APPs of $length bytes")
                    if (PROXY_MODE) {
                        packet.limit(length)
                        tunnel.write(packet)
                    }
                    packet.clear()
                    idle = false
                }
                if (tunnel.isConnected) {
                    length = tunnel.read(packet)
                    if (length > 0) {
                        Log.d("johnson", "received data from Internet of $length bytes")
                        if (PROXY_MODE) {
                            outputStream.write(packet.array(), 0, length)
                        }
                        packet.clear()
                        idle = false
                    }
                }
                if (idle) {
                    Thread.sleep(IDLE_INTERVAL_MS)
                }
            }
        } catch (e: InterruptedException) {
            Log.w("johnson", "VPN service is interrupted")
        } catch (e: Exception) {
            Log.e("johnson", Log.getStackTraceString(e))
        }
    }

    private fun handshake(tunnel: DatagramChannel): ParcelFileDescriptor {
        val builder =
            vpnService.Builder().setSession("session name").addAddress("10.10.10.0", 24)
                .addRoute("0.0.0.0", 0)
                .setConfigureIntent(configureIntent)
        synchronized(vpnService) {
            val iface = builder.establish()
            onEstablishListener(iface!!)
            return iface
        }
    }
}