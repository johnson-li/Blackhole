package com.xuebingli.blackhole.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.network.UdpClient
import com.xuebingli.blackhole.picker.InterfacePicker
import com.xuebingli.blackhole.picker.SinkModePicker
import com.xuebingli.blackhole.restful.ControlMessage
import com.xuebingli.blackhole.restful.Request
import com.xuebingli.blackhole.restful.RequestType
import com.xuebingli.blackhole.utils.ConfigUtils
import java.util.*

class SinkActivity : BaseActivity(true) {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var ipInput: TextInputEditText
    private lateinit var actionButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sink)
        sharedPref = getPreferences(Context.MODE_PRIVATE)
        ipInput = findViewById(R.id.ip_input)
        actionButton = findViewById(R.id.udp_action)
        ipInput.setText(ConfigUtils(this).getTargetIP())
    }

    @ExperimentalStdlibApi
    fun sinkAction(view: View) {
        val bitrate = ConfigUtils(this).getSinkBitrate()
        val packetSize = ConfigUtils(this).getPacketSize()
        val duration = ConfigUtils(this).getDuration()
        val id = UUID.randomUUID().toString()
        serverApi.request(ControlMessage(id, Request(RequestType.UDP_SINK, bitrate))).also {
            subscribe(it) { response ->
                val ip = ConfigUtils(this).getTargetIP()
                val port = response.port!!
                UdpClient(id, ip, port, bitrate, packetSize, duration).also { client ->
                    client.startUdpSink { packet_report, is_last, has_error ->
                        if (is_last) {
                            Log.d("johnson", "UDP sink finished")
                        }
                    }.also { d -> disposables.add(d) }
                }
            }.also { d -> disposables.add(d) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_sink_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.set_interface -> {
                InterfacePicker()
                    .show(supportFragmentManager, "interface picker")
                true
            }
            R.id.set_sink_mode -> {
                SinkModePicker { }
                    .show(supportFragmentManager, "Sink mode picker")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
    }
}