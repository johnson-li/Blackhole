package com.xuebingli.blackhole

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xuebingli.blackhole.activities.*

class NetworkUtilsAdapter : RecyclerView.Adapter<NetworkUtilsItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkUtilsItemViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_network_utils, parent, false)
        return NetworkUtilsItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return NetworkUtil.values().size
    }

    override fun onBindViewHolder(holder: NetworkUtilsItemViewHolder, position: Int) {
        val util = NetworkUtil.values()[position]
        holder.title.text = util.utilName
        holder.description.text = util.utilDescription
        holder.itemView.setOnClickListener {
            onItemClick(
                holder.itemView.context,
                NetworkUtil.values()[holder.adapterPosition]
            )
        }
    }

    private fun onItemClick(context: Context, networkUtil: NetworkUtil) {
        context.startActivity(Intent(context, networkUtil.activity))
    }
}

class NetworkUtilsItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title: TextView = view.findViewById(R.id.network_util_name)
    val description: TextView = view.findViewById(R.id.network_util_description)
}

enum class NetworkUtil(val utilName: String, val utilDescription: String, val activity: Class<out BaseActivity>) {
    PING("Ping", "Test the end-to-end latency", PingActivity::class.java),
    TRACEROUTE("Traceroute", "Test the route path", TracerouteActivity::class.java),
    IPERF("iPerf", "Test the end-to-end bandwidth", IperfActivity::class.java),
    SINK("Sink", "Test the uplink bandwidth and latency", SinkActivity::class.java),
    POUR("Pour", "Test the downlink bandwidth and latency", PourActivity::class.java),
    SYNC("Sync", "Synchronize the clock between the UE and the remote server", SyncActivity::class.java),
    BACKGROUND("Background", "Background services including location, signal strength", BackgroundActivity::class.java),
    TETHERING("Tethering", "USB tethering that exposes the phone's network to a USB-connected PC", TetheringActivity::class.java),
    ECHO("Echo", "A UDP echo client to test RTT", EchoActivity::class.java),
}