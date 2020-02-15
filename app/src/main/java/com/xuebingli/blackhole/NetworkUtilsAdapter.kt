package com.xuebingli.blackhole

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NetworkUtilsAdapter : RecyclerView.Adapter<NetworkUtilsItemViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkUtilsItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_network_utils, parent, false)
        return NetworkUtilsItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return NetworkUtil.values().size
    }

    override fun onBindViewHolder(holder: NetworkUtilsItemViewHolder, position: Int) {
        val util = NetworkUtil.values()[position]
        holder.title.text = util.utilName
        holder.description.text = util.utilDescription
        holder.itemView.setOnClickListener { onItemClick(holder.itemView.context, NetworkUtil.values()[holder.adapterPosition]) }
    }

    private fun onItemClick(context: Context, networkUtil: NetworkUtil) {
        when(networkUtil) {
            NetworkUtil.PING -> {context.startActivity(Intent(context, PingActivity::class.java))}
            NetworkUtil.TRACEROUTE -> {context.startActivity(Intent(context, TracerouteActivity::class.java))}
        }
    }
}

class NetworkUtilsItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val title: TextView = view.findViewById(R.id.network_util_name)
    val description: TextView = view.findViewById(R.id.network_util_description)
}

enum class NetworkUtil(val utilName: String, val utilDescription: String) {
    PING("Ping", "Ping description"),
    TRACEROUTE("Traceroute", "Traceroute description")
}