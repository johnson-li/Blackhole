package com.xuebingli.blackhole.activities

import android.util.Log
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
import com.xuebingli.blackhole.utils.*
import java.io.File
import java.util.*

class SinkActivity : SinkPourActivity(
    R.layout.activity_sink,
    listOf(
        Pair(Preferences.SINK_BITRATE_KEY)
        { s -> getBitrateString(s.getInt(Preferences.SINK_BITRATE_KEY, -1)) },
        Pair(Preferences.SINK_MODE_KEY)
        { s -> s.getString(Preferences.SINK_MODE_KEY, "Not set").toString() },
        Pair(Preferences.DURATION_KEY)
        { s -> getDurationString(s.getInt(Preferences.DURATION_KEY, -1)) },
        Pair(Preferences.PACKET_SIZE_KEY)
        { s -> s.getInt(com.xuebingli.blackhole.utils.Preferences.PACKET_SIZE_KEY, -1).toString() }
    )
) {
    @ExperimentalStdlibApi
    override fun action(view: View) {
        if (working) {
            return
        }
        working = true
        actionButton.setText(R.string.button_stop)
        reports.clear()
        for (fragment in supportFragmentManager.fragments) {
            if (fragment is ResultFragment) {
                fragment.onDataReset()
            }
        }
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
                            serverApi.request(ControlMessage(id, Request(RequestType.STATICS)))
                                .also { staticsResponseSingle ->
                                    subscribe(staticsResponseSingle) { staticsResponse ->
                                        Log.d("johnson", staticsResponse.toString())
                                        val file = File(
                                            ConfigUtils(this).getDataDir(),
                                            "udp_sink_${Constants.LOG_TIME_FORMAT.format(Date())}.json"
                                        )
                                        Toast.makeText(
                                            this,
                                            getString(R.string.toast_udp_sink_finished),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        PacketReportUtils
                                            .update(reports, staticsResponse.statics!!.udp_sink!!)
                                        file.writeText(Gson().toJson(reports))
                                        working = false
                                        actionButton.setText(R.string.sink_button)
                                        for (fragment in supportFragmentManager.fragments) {
                                            if (fragment is ResultFragment) {
                                                fragment.onFinished()
                                            }
                                        }
                                    }
                                }
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
            }.also { d -> disposables.add(d) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sink_activity, menu)
        return true
    }
}