package com.xuebingli.blackhole.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.dialog.BitratePicker
import com.xuebingli.blackhole.dialog.DurationPicker
import com.xuebingli.blackhole.dialog.InterfacePicker
import com.xuebingli.blackhole.dialog.PourModePicker
import com.xuebingli.blackhole.network.PacketReport
import com.xuebingli.blackhole.network.UdpClient
import com.xuebingli.blackhole.restful.ControlMessage
import com.xuebingli.blackhole.restful.Request
import com.xuebingli.blackhole.restful.RequestType
import com.xuebingli.blackhole.results.PacketReportDiagramFragment
import com.xuebingli.blackhole.results.ResultFragment
import com.xuebingli.blackhole.results.ResultFragmentPair
import com.xuebingli.blackhole.utils.ConfigUtils
import com.xuebingli.blackhole.utils.Constants.Companion.LOG_TIME_FORMAT
import com.xuebingli.blackhole.utils.Preferences.Companion.DURATION_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.POUR_BITRATE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.POUR_MODE_KEY
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class PourActivity : BaseActivity(true) {
    private lateinit var tab: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var ipInput: TextInputEditText
    private lateinit var actionButton: MaterialButton
    val reports = ArrayList<PacketReport>()
    private val adapter = PacketReportAdapter(this)
    private var pouring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pour)
        ipInput = findViewById(R.id.ip_input)
        actionButton = findViewById(R.id.udp_action)
        tab = findViewById(R.id.result_tab)
        pager = findViewById(R.id.result_page)
        pager.offscreenPageLimit = 2
        pager.adapter = adapter
        TabLayoutMediator(tab, pager) { tab, position ->
            tab.text = adapter.packetReportFragments[position].first
        }.attach()
        ipInput.setText(ConfigUtils(this).getTargetIP())
        reports.clear()
    }

    @SuppressLint("SimpleDateFormat")
    @ExperimentalUnsignedTypes
    fun pourAction(view: View) {
        if (pouring) {
            return
//            disposables.clear()
//            pouring = true
//            actionButton.setText(R.string.pour_button)
        }
        pouring = true
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
                            pouring = false
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
                pouring = false
                actionButton.setText(R.string.pour_button)
            }).also { d -> disposables.add(d) }
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
                    sharedPreferences.edit(true) {
                        putString(POUR_MODE_KEY, it.name)
                    }
                }.show(supportFragmentManager, "Pour mode picker")
                true
            }
            R.id.set_bitrate -> {
                BitratePicker {
                    sharedPreferences.edit(true) {
                        putInt(POUR_BITRATE_KEY, it)
                    }
                }.show(supportFragmentManager, "Bitrate picker")
                true
            }
            R.id.set_duration -> {
                DurationPicker {
                    sharedPreferences.edit(true) {
                        putInt(DURATION_KEY, it)
                    }
                }.show(supportFragmentManager, "Duration picker")
                true
            }
            R.id.reset -> {
                reports.clear()
                for (fragment in supportFragmentManager.fragments) {
                    if (fragment is ResultFragment) {
                        fragment.onDataReset()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
    }
}

class PacketReportAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
    val packetReportFragments = arrayOf(
        ResultFragmentPair("Diagram") { PacketReportDiagramFragment() }
//        ResultFragmentPair("List") { PacketReportListFragment() }
    )

    override fun getItemCount(): Int {
        return packetReportFragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return packetReportFragments[position].createInstance()
    }
}