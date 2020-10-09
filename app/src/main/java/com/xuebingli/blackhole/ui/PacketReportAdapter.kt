package com.xuebingli.blackhole.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.models.PacketReport

class PacketReportAdapter(private val packetReports: List<PacketReport>) :
    RecyclerView.Adapter<PacketReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacketReportViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_packet_report, parent, false)
        return PacketReportViewHolder(view)
    }

    override fun getItemCount(): Int {
        return packetReports.size
    }

    override fun onBindViewHolder(holder: PacketReportViewHolder, position: Int) {
        val packetReport = packetReports[position]
        holder.time.text = packetReport.localTimestamp.toString()
        holder.size.text = packetReport.size.toString()
    }
}

class PacketReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val time: TextView = view.findViewById(R.id.time)
    val size: TextView = view.findViewById(R.id.size)
}

