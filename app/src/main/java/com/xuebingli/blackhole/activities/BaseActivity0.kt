package com.xuebingli.blackhole.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.xuebingli.blackhole.services.ForegroundService
import com.xuebingli.blackhole.utils.Preferences

open class BaseActivity0 : AppCompatActivity() {
    val pref: SharedPreferences
        get() = getSharedPreferences(Preferences.PREFERENCE_NAME, Context.MODE_PRIVATE)
    var foregroundService: ForegroundService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            foregroundService = (service as ForegroundService.ForegroundBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            foregroundService = null
        }
    }

    fun isMeasurementRunning(): Boolean {
        return foregroundService?.measurementStarted ?: false
    }

    override fun onResume() {
        super.onResume()
        Intent(this, ForegroundService::class.java).also {
            this.bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }
}