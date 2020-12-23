package com.xuebingli.blackhole.activities

import android.annotation.SuppressLint
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.models.getReport
import com.xuebingli.blackhole.network.TcpClient
import com.xuebingli.blackhole.network.UdpClient
import com.xuebingli.blackhole.restful.ControlMessage
import com.xuebingli.blackhole.restful.Request
import com.xuebingli.blackhole.restful.RequestType
import com.xuebingli.blackhole.results.ResultFragment
import com.xuebingli.blackhole.utils.ConfigUtils
import com.xuebingli.blackhole.utils.Constants.Companion.LOG_TIME_FORMAT
import com.xuebingli.blackhole.utils.FileUtils
import com.xuebingli.blackhole.utils.PourMode
import com.xuebingli.blackhole.utils.Preferences
import java.io.File
import java.util.*

class PourActivity : SinkPourActivity(
    R.layout.activity_pour,
    listOf(
        Pair(Preferences.POUR_BITRATE_KEY)
        { c -> ConfigUtils(c).getPourBitrateStr() },
        Pair(Preferences.POUR_MODE_KEY)
        { c -> ConfigUtils(c).getPourMode().name },
        Pair(Preferences.DURATION_KEY)
        { c -> ConfigUtils(c).getDurationStr() },
        Pair(Preferences.PACKET_SIZE_KEY)
        { c -> ConfigUtils(c).getPacketSizeStr() }
    )
) {
    private fun onMeasurementFinished() {
        val mode = ConfigUtils(this).getPourMode().name.toLowerCase()
        val file = File(
            ConfigUtils(this).getDataDir(),
            "${mode}_pour_${LOG_TIME_FORMAT.format(Date())}.json"
        )
        loading.visibility = View.VISIBLE
        FileUtils().dumpJson(getReport(this, reports), file) {
            loading.visibility = View.GONE
            Toast.makeText(
                this,
                getString(R.string.toast_udp_pour_finished),
                Toast.LENGTH_SHORT
            ).show()
            working = false
            actionButton.setText(R.string.pour_button)
            for (fragment in supportFragmentManager.fragments) {
                if (fragment is ResultFragment) {
                    fragment.onFinished()
                }
            }
        }
    }

    @ExperimentalUnsignedTypes
    private fun actionUDP(id: String, bitrate: Int, packetSize: Int, duration: Int) {
        serverApi.request(ControlMessage(id, Request(RequestType.UDP_POUR, bitrate))).also {
            subscribe(it, { response ->
                val ip = ConfigUtils(this).targetIP
                val port = response.port!!
                UdpClient(id, ip, port, bitrate, packetSize, duration).also { client ->
                    client.startUdpPour { packet_report, is_last, has_error ->
                        if (is_last) {
                            onMeasurementFinished()
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

    private fun actionTCP(id: String, duration: Int) {
        serverApi.request(ControlMessage(id, Request(RequestType.TCP_POUR))).also {
            subscribe(it, { response ->
                val ip = ConfigUtils(this).targetIP
                val port = response.port!!
                TcpClient(id = id, ip = ip, port = port, duration = duration).also { client ->
                    client.startTcpPour { packetReport, is_last, has_error ->
                        if (is_last) {
                            onMeasurementFinished()
                        }
                        if (has_error) {
                            Toast.makeText(this, "Error occurred!", Toast.LENGTH_SHORT).show()
                        }
                        packetReport?.also { p ->
                            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                reports.add(p)
                                for (fragment in supportFragmentManager.fragments) {
                                    if (fragment is ResultFragment) {
                                        fragment.onDataInserted(reports.size - 1)
                                    }
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

    @SuppressLint("SimpleDateFormat")
    @ExperimentalUnsignedTypes
    override fun action(view: View) {
        if (working) {
            disposables.clear()
            onMeasurementFinished()
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
        val bitrate = ConfigUtils(this).getPourBitrate()
        val packetSize = ConfigUtils(this).getPacketSize()
        val duration = ConfigUtils(this).getDuration()
        val id = UUID.randomUUID().toString()
        when (ConfigUtils(this).getPourMode()) {
            PourMode.TCP -> {
                actionTCP(id, duration)
            }
            PourMode.UDP -> {
                actionUDP(id, bitrate, packetSize, duration)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pour_activity, menu)
        return true
    }
}

