package com.xuebingli.blackhole

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.ImageView
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    companion object {
        const val VPN_SERVICE_REQUEST_CODE = 1
    }

    private lateinit var switchButton: ImageView
    private lateinit var switchText: TextView
    private var vpnService: BlackHoleVpnService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            vpnService = (p1 as BlackHoleVpnService.LocalBinder).getService()
            isVpnRunning()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            vpnService = null
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, BlackHoleVpnService::class.java).also {
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        isVpnRunning()
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchButton = findViewById(R.id.switch_button)
        switchText = findViewById(R.id.switch_text)

        switchButton.setOnClickListener { toggleVpnSwitch() }
    }

    private fun isVpnRunning(): Boolean {
        Log.d("johnson", "vpn service: $vpnService")
        val running = vpnService?.isConnected() ?: false
        switchButton.setImageResource(if (running) R.drawable.switch_off else R.drawable.switch_on)
        switchText.setText(if (running) R.string.vpn_started else R.string.vpn_stopped)
        Log.d("johnson", "vpn is running: $running")
        return running
    }


    private fun toggleVpnSwitch() {
        val vpnRunning = isVpnRunning()
        if (vpnRunning) {
            startService(
                Intent(
                    this,
                    BlackHoleVpnService::class.java
                ).setAction(BlackHoleVpnService.ACTION_DISCONNECT)
            )
        } else {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                onActivityResult(VPN_SERVICE_REQUEST_CODE, Activity.RESULT_OK, null)
            } else {
                startActivityForResult(intent, VPN_SERVICE_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            VPN_SERVICE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    startService(
                        Intent(this, BlackHoleVpnService::class.java).setAction(
                            BlackHoleVpnService.ACTION_CONNECT
                        )
                    )
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
