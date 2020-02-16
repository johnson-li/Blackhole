package com.xuebingli.blackhole

import android.os.Bundle
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

