package com.xuebingli.blackhole.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.utils.isValidIpAddress
import com.xuebingli.blackhole.picker.InterfacePicker
import com.xuebingli.blackhole.results.IperfParsedResultFragment
import com.xuebingli.blackhole.results.RawResultFragment
import com.xuebingli.blackhole.results.ResultFragmentPair
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

val fragments = arrayOf(
    ResultFragmentPair("Parsed Result") { IperfParsedResultFragment() },
    ResultFragmentPair("Raw Result") { RawResultFragment() }
)

class PingActivity : BaseActivity(true) {
    companion object {
        const val PING_RESULT_PREF_KEY = "ping_result"
        const val INTERFACE_PREF_KEY = "interface"
    }

    private lateinit var tab: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var ipInput: TextInputEditText
    private lateinit var pingButton: MaterialButton
    private lateinit var sharedPref: SharedPreferences
    private val pingResultsAdapter =
        PingResultsAdapter(this)
    private var pingThread: Thread? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_ping_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.set_interface -> {
                InterfacePicker()
                    .show(supportFragmentManager, "interface picker")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ping)
        sharedPref = getPreferences(Context.MODE_PRIVATE)
        ipInput = findViewById(R.id.ip_input)
        pingButton = findViewById(R.id.ping_action)
        tab = findViewById(R.id.result_tab)
        pager = findViewById(R.id.result_page)
        pager.adapter = pingResultsAdapter
        TabLayoutMediator(tab, pager) { tab, position ->
            tab.text = fragments[position].first
        }.attach()
    }

    fun pingAction(view: View) {
        if (pingThread == null) {
            val ip = ipInput.text
            if (!isValidIpAddress(ip.toString())) {
                Toast.makeText(this,
                    R.string.toast_invalid_ip, Toast.LENGTH_SHORT).show()
                return
            }
            val interfaceName = sharedPref.getString(INTERFACE_PREF_KEY, "wlan0")
            val command = "su -c ping -I $interfaceName $ip"
            Log.d("johnson", command)
            val process = Runtime.getRuntime().exec(command)
            val output = DataOutputStream(process.outputStream)
            val input = DataInputStream(process.inputStream)
            val error = DataInputStream(process.errorStream)
            val inputBuffer = StringBuffer(1024)
            pingThread = Thread {
                try {
                    sharedPref.edit().putString(PING_RESULT_PREF_KEY, "").apply()
                    val buffer = ByteBuffer.allocate(Short.MAX_VALUE.toInt())
                    do {
                        var length = input.read(buffer.array())
                        if (length > 0) {
                            val data = String(buffer.array(), 0, length)
                            inputBuffer.append(data)
                        }
                        sharedPref.edit()
                            .putString(PING_RESULT_PREF_KEY, inputBuffer.toString()).apply()
                        buffer.clear()
//                        length = error.read(buffer.array())
//                        if (length > 0) {
//                            val data = String(buffer.array(), 0, length)
//                            Log.e("johnson", data)
//                        }
//                        buffer.clear()
                    } while (process.isAlive && !Thread.currentThread().isInterrupted)
                } catch (e: Exception) {
                    Log.e("johnson", Log.getStackTraceString(e))
                } finally {
                    Log.d("johnson", "ping thread interrupted")
                    process.destroy()
                }
            }
            pingThread!!.start()
            pingButton.setText(R.string.button_stop)
        } else {
            pingThread!!.interrupt()
            pingThread = null
            pingButton.setText(R.string.ping_action)
        }
    }

    override fun onPause() {
        super.onPause()
        pingThread?.interrupt()
        pingThread = null
        pingButton.setText(R.string.ping_action)
    }
}

class PingResultsAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position].createInstance()
    }
}
