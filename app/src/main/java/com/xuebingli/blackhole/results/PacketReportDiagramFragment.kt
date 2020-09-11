package com.xuebingli.blackhole.results

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.activities.PourActivity
import com.xuebingli.blackhole.utils.Constants

class PacketReportDiagramFragment : ResultFragment() {
    private lateinit var chart: LineChart
    private val entries: MutableList<Entry> = ArrayList()
    private lateinit var dataset: LineDataSet
    private lateinit var lineData: LineData
    private lateinit var currentLatency: TextView
    private lateinit var averageLatency: TextView
    private lateinit var medianLatency: TextView
    private lateinit var currentBandwidth: TextView
    private lateinit var averageBandwidth: TextView
    private lateinit var medianBandwidth: TextView
    private var dataSizes = ArrayList<Float>(120)
    private var startTimestamp = 0L
    private var totalDataSize = 0
    private var maxBandwidth = 0f
    private val measurementGranularity = 500 // In milliseconds

    init {
        for (i in 1..120) {
            dataSizes.add(0.toFloat())
        }
    }

    private fun initLineChart() {
        chart.clear()
        chart.setDrawGridBackground(false)
        chart.axisLeft.axisMinimum = 0f
        chart.setNoDataText(requireContext().getString(R.string.no_data))
        chart.setTouchEnabled(false)
        chart.description.text = ""
        dataset = LineDataSet(entries, "packet_report_diagram")
            .apply { axisDependency = YAxis.AxisDependency.LEFT }
        lineData = LineData(dataset)
        chart.data = lineData
        chart.invalidate()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_packet_report_diagram, container, false)
        chart = view.findViewById(R.id.chart)
        currentLatency = view.findViewById(R.id.latency_current)
        averageLatency = view.findViewById(R.id.latency_average)
        medianLatency = view.findViewById(R.id.latency_median)
        currentBandwidth = view.findViewById(R.id.bandwidth_current)
        averageBandwidth = view.findViewById(R.id.bandwidth_average)
        medianBandwidth = view.findViewById(R.id.bandwidth_median)
        initLineChart()
        return view
    }

    override fun onDataInserted(index: Int) {
        val report = (activity as PourActivity).reports[index]
        totalDataSize += report.size
        if (index == 0) {
            startTimestamp = report.timestamp
        }
        val timestamp = report.timestamp - startTimestamp
        val i = (timestamp / measurementGranularity).toInt()
        dataSizes[i] += report.size.toFloat() * Constants.BYTE_BITS / Constants.M
        if (i > entries.size) {
            val j = entries.size
            val data = dataSizes[j] * 1000 / measurementGranularity
            if (data > maxBandwidth) {
                maxBandwidth = data
                chart.axisLeft.axisMaximum = maxBandwidth * 1.5f
            }
            lineData.addEntry(Entry(j.toFloat() / 1000 * measurementGranularity, data), 0)
            chart.notifyDataSetChanged()
            chart.invalidate()
            currentBandwidth.text = requireContext().getString(R.string.statics_mbps, data)
        }
        val bd =
            totalDataSize.toDouble() * 8 / (System.currentTimeMillis() - startTimestamp) * 1000 / Constants.M
        averageBandwidth.text = requireContext().getString(R.string.statics_mbps, bd)
    }

    override fun onDataReset() {
        initLineChart()
    }
}