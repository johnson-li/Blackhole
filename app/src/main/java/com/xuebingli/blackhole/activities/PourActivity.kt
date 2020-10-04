package com.xuebingli.blackhole.activities

import android.annotation.SuppressLint
import android.view.Menu
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.network.UdpClient
import com.xuebingli.blackhole.restful.ControlMessage
import com.xuebingli.blackhole.restful.Request
import com.xuebingli.blackhole.restful.RequestType
import com.xuebingli.blackhole.results.ResultFragment
import com.xuebingli.blackhole.utils.ConfigUtils
import com.xuebingli.blackhole.utils.Constants.Companion.LOG_TIME_FORMAT
import java.io.File
import java.util.*

class PourActivity : SinkPourActivity(R.layout.activity_pour) {

    @SuppressLint("SimpleDateFormat")
    @ExperimentalUnsignedTypes
    override fun action(view: View) {
        if (working) {
            return
//            disposables.clear()
//            pouring = true
//            actionButton.setText(R.string.pour_button)
        }
        working = true
        actionButton.setText(R.string.button_stop)
        reports.clear()
        for (fragment in supportFragmentManager.fragments) {
            if (fragment is ResultFragment) {
                fragment.onDataReset()
            }
        }
        val bitrate = ConfigUtils(this).getPourBitrate()
        val packetSize = ConfigUtils(this).getPacketSize()
        val duration = ConfigUtils(this).getDuration()
        val id = UUID.randomUUID().toString()
        serverApi.request(ControlMessage(id, Request(RequestType.UDP_POUR, bitrate))).also {
            subscribe(it, { response ->
                val ip = ConfigUtils(this).getTargetIP()
                val port = response.port!!
                UdpClient(id, ip, port, bitrate, packetSize, duration).also { client ->
                    client.startUdpPour { packet_report, is_last, has_error ->
                        if (is_last) {
                            val file = File(
                                ConfigUtils(this).getDataDir(),
                                "udp_pour_${LOG_TIME_FORMAT.format(Date())}.json"
                            )
                            Toast.makeText(
                                this,
                                getString(R.string.toast_udp_pour_finished),
                                Toast.LENGTH_SHORT
                            ).show()
                            file.writeText(Gson().toJson(reports))
                            working = false
                            actionButton.setText(R.string.pour_button)
                        }
                        if (has_error) {
                            Toast.makeText(this, "Error occurred!", Toast.LENGTH_SHORT).show()
                        }
                        packet_report?.also { p ->
                            reports.add(p)
                            for (fragment in supportFragmentManager.fragments) {
                                if (fragment is ResultFragment) {
                                    fragment.onDataInserted(reports.size - 1)
                                }
                            }
                        }
                    }.also { d -> disposables.add(d) }
                }
            }, {
                working = false
                actionButton.setText(R.string.pour_button)
            }).also { d -> disposables.add(d) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pour_activity, menu)
        return true
    }
}

