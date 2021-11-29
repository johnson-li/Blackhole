package com.xuebingli.blackhole.activities

import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.network.Controller
import com.xuebingli.blackhole.network.UdpClient
import com.xuebingli.blackhole.utils.ConfigUtils
import com.xuebingli.blackhole.utils.Preferences

class EchoActivity : BaseActivity(true, parameters = listOf(
    Pair(Preferences.DURATION_KEY)
    { c -> ConfigUtils(c).getDurationStr() },
    Pair(Preferences.DATARATE_KEY)
    { c -> ConfigUtils(c).getDatarateStr() },
    Pair(Preferences.TARGET_IP_KEY)
    { c -> ConfigUtils(c).targetIP },
    Pair(Preferences.LOGGING_KEY)
    { c -> ConfigUtils(c).logging.toString().toUpperCase() },
    Pair(Preferences.PACKET_SIZE_KEY)
    { c -> ConfigUtils(c).getPacketSizeStr() }
)) {

    companion object {
        private var thread: Thread? = null
    }

    private lateinit var button: Button
    private lateinit var latencyText: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_echo)
        button = findViewById(R.id.button)
        latencyText = findViewById(R.id.latencyText)
        button.setOnClickListener {
            if (thread == null) {
                Toast.makeText(this, R.string.udp_echo_start, Toast.LENGTH_SHORT).show()
                ProcessBuilder().command()
                thread = object : Thread() {
                    override fun run() {
                        val cfg = ConfigUtils(this@EchoActivity)
                        UdpClient(
                            "x", cfg.targetIP, cfg.getUdpEchoPort(), cfg.getDatarate(),
                            cfg.getPacketSize(), cfg.getDuration()
                        ).startUdpEcho(object : Controller {
                            override fun terminated(): Boolean {
                                return isInterrupted
                            }
                        }, ConfigUtils(this@EchoActivity).logging)
                    }
                }
                thread!!.start()
                button.text = getString(R.string.stop)
            } else {
                Toast.makeText(this, R.string.udp_echo_stop, Toast.LENGTH_SHORT).show()
                thread!!.interrupt()
                thread = null
                button.text = getString(R.string.start)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (thread != null) {
            button.text = getString(R.string.stop)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_echo_activity, menu)
        return true
    }
}