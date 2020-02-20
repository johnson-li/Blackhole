package com.xuebingli.blackhole

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.xuebingli.blackhole.results.PingParsedResultFragment
import com.xuebingli.blackhole.results.RawResultFragment
import com.xuebingli.blackhole.results.ResultFragmentPair
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

val fragments = arrayOf(
    ResultFragmentPair("Raw Result") { RawResultFragment() },
    ResultFragmentPair("Parsed Result") { PingParsedResultFragment() }
)

class PingActivity : BaseActivity(true) {
    companion object {
        val PING_RESULT_PREF_KEY = "ping_result"
    }

    private lateinit var tab: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var pingButton: MaterialButton
    private lateinit var sharedPref: SharedPreferences
    private val pingResultsAdapter = PingResultsAdapter(this)
    private var pingThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ping)
        sharedPref = getPreferences(Context.MODE_PRIVATE)
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
            val command = "ping 1.1.1.1"
            val process = Runtime.getRuntime().exec(command)
            val output = DataOutputStream(process.outputStream)
            val input = DataInputStream(process.inputStream)
            val error = DataInputStream(process.errorStream)
            val inputBuffer = StringBuffer(1024)
            pingThread = Thread {
                try {
                    val buffer = ByteBuffer.allocate(Short.MAX_VALUE.toInt())
                    while (process.isAlive && !Thread.currentThread().isInterrupted) {
                        val length = input.read(buffer.array())
                        if (length > 0) {
                            val data = String(buffer.array(), 0, length)
                            inputBuffer.append(data)
                        }
                        sharedPref.edit()
                            .putString(PING_RESULT_PREF_KEY, inputBuffer.toString()).apply()
                    }
                } finally {
                    process.destroy()
                    Log.d("johnson", "ping thread interrupted")
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

