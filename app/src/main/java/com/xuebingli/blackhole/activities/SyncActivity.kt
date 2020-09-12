package com.xuebingli.blackhole.activities

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.network.TcpClient
import com.xuebingli.blackhole.utils.ConfigUtils

class SyncActivity : BaseActivity(true) {
    lateinit var syncButton: ImageView
    lateinit var clockDriftTextView: TextView
    lateinit var confidenceTextView: TextView
    var syncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync)
        syncButton = findViewById(R.id.syncButton)
        clockDriftTextView = findViewById(R.id.clockDrift)
        confidenceTextView = findViewById(R.id.confidence)
    }

    fun syncAction(view: View) {
        if (syncing) {
            return
        }
        syncing = true
        syncButton.isClickable = false
        val rotate = RotateAnimation(0f, 180f)
        rotate.interpolator = LinearInterpolator()
        syncButton.startAnimation(rotate)
        syncButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate))
        val ip = ConfigUtils(this).getTargetIP()
        val port = ConfigUtils(this).getSyncPort()
        val client = TcpClient(ip, port, ConfigUtils(this).getDataDir())
        client.startTcpSync {
            if (it == null) {
                Toast.makeText(
                    this,
                    getString(R.string.toast_sync_error, ip, port),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                ConfigUtils(applicationContext).clockDrift = it.clockDrift
                clockDriftTextView.text =
                    getString(R.string.sync_activity_clock_drift, it.clockDrift)
                confidenceTextView.text =
                    getString(R.string.sync_activity_clock_confidence, it.confidence)
            }
            syncing = false
            syncButton.clearAnimation()
            syncButton.isClickable = true
        }.also { disposables.add(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sync_activity, menu)
        return true
    }
}