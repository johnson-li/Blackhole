package com.xuebingli.blackhole.activities

import android.Manifest
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.edit
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.dialog.FrequencyPicker
import com.xuebingli.blackhole.services.ForegroundService
import com.xuebingli.blackhole.ui.BackgroundService
import com.xuebingli.blackhole.ui.BackgroundServiceAdapter
import com.xuebingli.blackhole.utils.AndroidPermissionUtils

class BackgroundActivity : BaseActivity(true) {
    private lateinit var reportsContainer: RecyclerView
    private lateinit var adapter: BackgroundServiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background)
        reportsContainer = findViewById(R.id.container)
        adapter = BackgroundServiceAdapter(this, sharedPreferences)
        reportsContainer.adapter = adapter
        val layoutManager = LinearLayoutManager(this)
        reportsContainer.layoutManager = layoutManager
        reportsContainer.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
        adapter.setup()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_background_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.set_frequency -> {
                FrequencyPicker().show(supportFragmentManager, "interface picker")
                true
            }
            R.id.start_all -> {
                sharedPreferences.edit(true) {
                    BackgroundService.values().forEachIndexed { index, backgroundService ->
                        putBoolean(backgroundService.prefKey, true)
                        adapter.notifyItemChanged(index)
                    }
                }
                ForegroundService.updateForegroundService(this)
                true
            }
            R.id.stop_all -> {
                sharedPreferences.edit(true) {
                    BackgroundService.values().forEachIndexed { index, backgroundService ->
                        putBoolean(backgroundService.prefKey, false)
                        adapter.notifyItemChanged(index)
                    }
                }
                ForegroundService.updateForegroundService(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onPermissionUpdated(
        permissions: Array<String>,
        grantResults: IntArray,
        targetPermission: String
    ) {
        if (AndroidPermissionUtils(this).permissionGranted(
                permissions,
                grantResults,
                targetPermission
            )
        ) {
            adapter.permissionGranted(targetPermission)
        } else {
            Toast.makeText(this, R.string.toast_location_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            AndroidPermissionUtils.LOCATION_PERMISSION_REQUEST_CODE -> {
                onPermissionUpdated(
                    permissions,
                    grantResults,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            AndroidPermissionUtils.PHONE_STATE_PERMISSION_REQUEST_CODE -> {
                onPermissionUpdated(permissions, grantResults, Manifest.permission.READ_PHONE_STATE)
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}