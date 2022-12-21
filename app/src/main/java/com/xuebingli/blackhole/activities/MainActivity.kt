package com.xuebingli.blackhole.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.databinding.ActivityMainBinding
import com.xuebingli.blackhole.databinding.ItemMeasurementBinding
import com.xuebingli.blackhole.models.*
import com.xuebingli.blackhole.services.ForegroundService
import com.xuebingli.blackhole.utils.Preferences

class MainActivity : BaseActivity0() {
    private lateinit var binding: ActivityMainBinding
    private var measurementRunning = ObservableBoolean(false)
    private var measurement: Measurement? = null
    private lateinit var adapter: MeasurementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.measurementRunning = measurementRunning
        setContentView(binding.root)
        ForegroundService.updateForegroundService(this)

        measurementRunning.set(isMeasurementRunning())

        measurement = Measurement.loadSetup(pref.getString(Preferences.MEASUREMENT_SETUP_KEY, null))
        adapter = MeasurementAdapter(measurement!!, this)
        val layoutManager = LinearLayoutManager(this)
        binding.container.also {
            it.adapter = adapter
            it.layoutManager = layoutManager
            it.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
        }
    }

    fun onStartButtonClick(view: View) {
        if (isMeasurementRunning()) {
            stopMeasurement()
        } else {
            startMeasurement()
        }
    }

    private fun startMeasurement() {
        foregroundService?.startMeasurement()
        measurementRunning.set(isMeasurementRunning())
    }

    private fun stopMeasurement() {
        foregroundService?.stopMeasurement()
        measurementRunning.set(isMeasurementRunning())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun addMeasurement(key: MeasurementKey) {
        when (key) {
            MeasurementKey.LocationInfo -> {
                measurement?.recordSet?.put(
                    LocationMeasurementSetup(),
                    Records(mutableListOf<LocationRecord>())
                )
                adapter.notifyItemInserted(adapter.itemCount - 1)
            }
            MeasurementKey.CellularInfo -> {
                measurement?.recordSet?.put(
                    CellularMeasurementSetup(),
                    Records(mutableListOf<CellularRecord>())
                )
                adapter.notifyItemInserted(adapter.itemCount - 1)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.enable_vpn -> {
                true
            }
            R.id.add_measurement -> {
                AlertDialog.Builder(this).setTitle(R.string.add_measurement)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .setItems(
                        MeasurementKey.values().map { it.name }.toTypedArray()
                    ) { _, p1 -> addMeasurement(MeasurementKey.values()[p1]) }
                    .show()
                true
            }
            else -> false
        }
    }
}

class MeasurementAdapter(val measurement: Measurement, val activity: MainActivity) :
    Adapter<MeasurementViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeasurementViewHolder {
        val binding = DataBindingUtil.inflate<ItemMeasurementBinding>(
            activity.layoutInflater, R.layout.item_measurement, null, false
        )
        return MeasurementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MeasurementViewHolder, position: Int) {
    }

    override fun getItemCount(): Int {
        return measurement.setups.size
    }
}

class MeasurementViewHolder(binding: ViewDataBinding) : ViewHolder(binding.root)
