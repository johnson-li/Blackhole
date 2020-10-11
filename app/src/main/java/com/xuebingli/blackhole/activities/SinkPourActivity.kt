package com.xuebingli.blackhole.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.models.PacketReport
import com.xuebingli.blackhole.ui.SinkPourPagerAdapter
import com.xuebingli.blackhole.utils.ConfigUtils

abstract class SinkPourActivity(
    private val layout: Int,
    parameters: List<Pair<String, (SharedPreferences) -> String>>
) :
    BaseActivity(true, parameters = parameters) {
    lateinit var tab: TabLayout
    lateinit var pager: ViewPager2
    lateinit var loading: View
    lateinit var ipInput: TextInputEditText
    lateinit var actionButton: MaterialButton
    lateinit var adapter: SinkPourPagerAdapter
    val reports = ArrayList<PacketReport>()
    var working = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        ipInput = findViewById(R.id.ip_input)
        actionButton = findViewById(R.id.udp_action)
        tab = findViewById(R.id.result_tab)
        pager = findViewById(R.id.result_page)
        loading = findViewById(R.id.loading)
        adapter = SinkPourPagerAdapter(this)
        actionButton.setOnClickListener(this::action)
        pager.offscreenPageLimit = 2
        pager.adapter = adapter
        TabLayoutMediator(tab, pager) { tab, position ->
            tab.text = adapter.packetReportFragments[position].first
        }.attach()
        ipInput.setText(ConfigUtils(this).getTargetIP())
        reports.clear()
    }

    abstract fun action(view: View)
}

