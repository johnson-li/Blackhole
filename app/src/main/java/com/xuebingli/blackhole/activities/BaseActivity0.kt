package com.xuebingli.blackhole.activities

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.xuebingli.blackhole.services.ForegroundService

open class BaseActivity0 : AppCompatActivity() {
    private var foregroundService: ForegroundService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            foregroundService = (service as ForegroundService.ForegroundBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            foregroundService = null
        }

    }

    fun isForegroundServiceRunning(): Boolean {
        return (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Int.MAX_VALUE).any {
                it.service.className.equals(ForegroundService::class)
            }
    }

    fun isMeasurementRunning(): Boolean {
//        return foregroundService?.measurementRunning() ?: false
        return false
    }
}