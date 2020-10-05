package com.xuebingli.blackhole.ui

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.xuebingli.blackhole.activities.BaseActivity
import com.xuebingli.blackhole.results.PacketReportDiagramFragment
import com.xuebingli.blackhole.results.ResultFragmentPair

class SinkPourPagerAdapter(activity: BaseActivity) : FragmentStateAdapter(activity) {
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
