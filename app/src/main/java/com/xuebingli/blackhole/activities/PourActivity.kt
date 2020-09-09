package com.xuebingli.blackhole.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.edit
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.network.PacketReport
import com.xuebingli.blackhole.network.UdpClient
import com.xuebingli.blackhole.picker.BitratePicker
import com.xuebingli.blackhole.picker.InterfacePicker
import com.xuebingli.blackhole.picker.PourModePicker
import com.xuebingli.blackhole.restful.ControlMessage
import com.xuebingli.blackhole.restful.Request
import com.xuebingli.blackhole.restful.RequestType
import com.xuebingli.blackhole.utils.ConfigUtils
import com.xuebingli.blackhole.utils.Preferences.Companion.POUR_BITRATE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.POUR_MODE_KEY
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PourActivity : BaseActivity(true) {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var ipInput: TextInputEditText
    private lateinit var actionButton: MaterialButton
    private val reports = ArrayList<PacketReport>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pour)
        sharedPref = getPreferences(Context.MODE_PRIVATE)
        ipInput = findViewById(R.id.ip_input)
        actionButton = findViewById(R.id.udp_action)
        ipInput.setText(ConfigUtils(this).getTargetIP())
        reports.clear()
    }

    fun pourAction(view: View) {
        val bitrate = ConfigUtils(this).getPourBitrate()
        val packetSize = ConfigUtils(this).getPacketSize()
        val duration = ConfigUtils(this).getDuration()
        val id = UUID.randomUUID().toString()
        serverApi.request(ControlMessage(id, Request(RequestType.UDP_POUR, bitrate))).also {
            subscribe(it) { response ->
                val ip = ConfigUtils(this).getTargetIP()
                val port = response.port!!
                UdpClient(id, ip, port, bitrate, packetSize, duration).also { client ->
                    client.startUdpPour { packet_report, is_last, has_error ->
                        if (is_last) {
                            val file = File(
                                ConfigUtils(this).getDataDir(),
                                "udp_pour_${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())}.json"
                            )
                            Log.d("johnson", "UDP pour finished, export to ${file.absoluteFile}")
//                            file.createNewFile()
                            file.writeText(Gson().toJson(reports))
                        }
                        if (has_error) {
                            Toast.makeText(this, "Error occurred!", Toast.LENGTH_SHORT).show()
                        }
                        packet_report?.also { p -> reports.add(p) }
                    }.also { d -> disposables.add(d) }
                }
            }.also { d -> disposables.add(d) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_pour_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.set_interface -> {
                InterfacePicker()
                    .show(supportFragmentManager, "interface picker")
                true
            }
            R.id.set_pour_mode -> {
                PourModePicker {
                    sharedPref.edit(true) {
                        putString(POUR_MODE_KEY, it.name)
                    }
                }.show(supportFragmentManager, "Pour mode picker")
                true
            }
            R.id.set_bitrate -> {
                BitratePicker {
                    sharedPref.edit(true) {
                        putInt(POUR_BITRATE_KEY, it)
                    }
                }.show(supportFragmentManager, "Bitrate picker")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
    }
}