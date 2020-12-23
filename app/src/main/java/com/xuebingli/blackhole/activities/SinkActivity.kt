package com.xuebingli.blackhole.activities

import android.view.Menu
import android.view.View
import android.widget.Toast
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.models.getReport
import com.xuebingli.blackhole.network.TcpClient
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
        { c -> ConfigUtils(c).getSinkBitrateStr() },
        Pair(Preferences.SINK_MODE_KEY)
        { c -> ConfigUtils(c).getSinkMode().name },
        Pair(Preferences.DURATION_KEY)
        { c -> ConfigUtils(c).getDurationStr() },
        Pair(Preferences.PACKET_SIZE_KEY)
        { c -> ConfigUtils(c).getPacketSizeStr() }
    )
) {
    @ExperimentalStdlibApi
    override fun action(view: View) {
        if (working) {
            return
        }
        working = true
        actionButton.setText(R.string.button_stop)
        ConfigUtils(this).targetIP = ipInput.text.toString()
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
        when (ConfigUtils(this).getSinkMode()) {
            SinkMode.TCP -> {
                actionTCP(id, duration)
            }
            SinkMode.UDP -> {
                actionUDP(id, bitrate, packetSize, duration)
            }
        }
    }

    private fun onMeasurementFinished(id: String) {
        loading.visibility = View.VISIBLE
        serverApi.request(ControlMessage(id, Request(RequestType.STATICS)))
            .also { staticsResponseSingle ->
                subscribe(staticsResponseSingle) { staticsResponse ->
                    val mode = ConfigUtils(this).getSinkMode()
                    val file = File(
                        ConfigUtils(this).getDataDir(),
                        "${mode.name.toLowerCase()}_sink_${Constants.LOG_TIME_FORMAT.format(Date())}.json"
                    )
                    PacketReportUtils.update(reports, staticsResponse.statics!!
                        .run { if (mode == SinkMode.UDP) udp_sink!! else tcp_sink!! })

                    FileUtils().dumpJson(getReport(this, reports), file) {
                        loading.visibility = View.GONE
                        Toast.makeText(
                            this,
                            getString(
                                if (mode == SinkMode.UDP) R.string.toast_udp_sink_finished else
                                    R.string.toast_tcp_sink_finished
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
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
    }

    @ExperimentalStdlibApi
    private fun actionUDP(id: String, bitrate: Int, packetSize: Int, duration: Int) {
        serverApi.request(ControlMessage(id, Request(RequestType.UDP_SINK, bitrate))).also {
            subscribe(it) { response ->
                val ip = ConfigUtils(this).targetIP
                val port = response.port!!
                UdpClient(id, ip, port, bitrate, packetSize, duration).also { client ->
                    client.startUdpSink { packet_report, is_last, has_error ->
                        if (is_last) {
                            onMeasurementFinished(id)
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

    private fun actionTCP(id: String, duration: Int) {
        serverApi.request(ControlMessage(id, Request(RequestType.TCP_SINK))).also {
            subscribe(it) { response ->
                val ip = ConfigUtils(this).targetIP
                val port = response.port!!
                TcpClient(id, ip, port, duration).also { client ->
                    client.startTcpSink { packet_report, is_last, has_error ->
                        if (is_last) {
                            onMeasurementFinished(id)
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
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sink_activity, menu)
        return true
    }
}