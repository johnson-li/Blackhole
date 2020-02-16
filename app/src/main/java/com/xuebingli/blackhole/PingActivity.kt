package com.xuebingli.blackhole

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
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
    private lateinit var tab: TabLayout
    private lateinit var pager: ViewPager2
    private val pingResultsAdapter = PingResultsAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ping)
        tab = findViewById(R.id.result_tab)
        pager = findViewById(R.id.result_page)
        pager.adapter = pingResultsAdapter
        TabLayoutMediator(tab, pager) { tab, position ->
            tab.text = fragments[position].first
        }.attach()
        RawResultFragment.name
    }

    fun pingAction(view: View) {
        val process = Runtime.getRuntime().exec("ping 1.1.1.1")
        val output = DataOutputStream(process.outputStream)
        val input = DataInputStream(process.inputStream)
        val error = DataInputStream(process.errorStream)
        Thread {
            val buffer = ByteBuffer.allocate(Short.MAX_VALUE.toInt())
            while (process.isAlive) {
                val length = input.read(buffer.array())
                if (length > 0) {
                    val data = String(buffer.array(), 0, length)
                    Log.d("johnson", "data: $data")
                }
            }
        }.start()
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

