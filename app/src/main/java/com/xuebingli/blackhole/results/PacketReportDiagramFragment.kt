package com.xuebingli.blackhole.results

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.activities.PourActivity
import io.reactivex.rxjava3.core.Observable

class PacketReportDiagramFragment : ResultFragment() {
    private lateinit var chart: LineChart
    private val entries: MutableList<Entry> = ArrayList()
    private val dataset = LineDataSet(entries, "label")
    private var dataSizes = ArrayList<Int>(120)
    private var startTimestamp = 0L

    init {
        for (i in 1..120) {
            dataSizes.add(0)
        }
        for (i in 1..3) {
            entries.add(Entry(-i.toFloat(), 0.toFloat()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_packet_report_diagram, container, false)
        chart = view.findViewById(R.id.chart)
        chart.data = LineData(dataset)
        chart.invalidate()
        return view
    }

    fun updateChart() {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Observable.create<Unit> {

        }
    }

    override fun onDataInserted(index: Int) {
        val report = (activity as PourActivity).reports[index]
        if (index == 0) {
            startTimestamp = report.timestamp
        }
        val timestamp = report.timestamp - startTimestamp
        val i = (timestamp / 1000).toInt()
        dataSizes[i] += report.size
        if (i > entries.size) {
            val j = entries.size
            dataset.addEntry(Entry(j.toFloat(), dataSizes[j].toFloat()))
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }
}