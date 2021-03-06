package com.xuebingli.blackhole.activities

import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xuebingli.blackhole.NetworkUtilsAdapter
import com.xuebingli.blackhole.R

class NetworkUtilsActivity: BaseActivity(true) {

    lateinit var networkUtilsList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_utils)

        networkUtilsList = findViewById(R.id.network_utils_list)
        networkUtilsList.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        networkUtilsList.layoutManager = layoutManager
        networkUtilsList.adapter = NetworkUtilsAdapter()
        networkUtilsList.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
    }
}