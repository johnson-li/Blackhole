package com.xuebingli.blackhole.activities

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.xuebingli.blackhole.services.ForegroundService
import com.xuebingli.blackhole.utils.Preferences
import io.reactivex.rxjava3.disposables.CompositeDisposable

open class BaseActivity0 : AppCompatActivity() {
    val pref: SharedPreferences
        get() = getSharedPreferences(Preferences.PREFERENCE_NAME, Context.MODE_PRIVATE)
    var foregroundService: ForegroundService? = null
    lateinit var localDisposable: CompositeDisposable
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            foregroundService = (service as ForegroundService.ForegroundBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            foregroundService = null
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    fun isMeasurementRunning(): Boolean {
        return foregroundService?.measurementStarted ?: false
    }

    override fun onResume() {
        super.onResume()
        localDisposable = CompositeDisposable()
        Intent(this, ForegroundService::class.java).also {
            this.bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    override fun onPause() {
        localDisposable.dispose()
        super.onPause()
    }

    fun checkPermissions(): Boolean {
        val permissionList = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
        )
        val permissionsToBeGrant = permissionList.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToBeGrant.isEmpty()) {
            return true
        }
        requestPermissions(
            permissionsToBeGrant.toTypedArray(),
            PERMISSION_REQUEST_CODE
        )
        return false
    }
}