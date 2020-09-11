package com.xuebingli.blackhole.results

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.activities.PourActivity
import com.xuebingli.blackhole.ui.PacketReportAdapter

class PacketReportListFragment : ResultFragment() {
    private lateinit var reportsContainer: RecyclerView
    private lateinit var adapter: PacketReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_packet_report_list, container, false)
        reportsContainer = view.findViewById(R.id.reports_container)
        adapter = PacketReportAdapter((activity as PourActivity).reports)
        reportsContainer.adapter = adapter
        val layoutManager = LinearLayoutManager(context)
        reportsContainer.layoutManager = layoutManager
        reportsContainer
            .addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
        return view
    }

    override fun onDataInserted(index: Int) {
        adapter.notifyItemInserted(index)
    }

    override fun onDataReset() {
        adapter.notifyDataSetChanged()
    }
}