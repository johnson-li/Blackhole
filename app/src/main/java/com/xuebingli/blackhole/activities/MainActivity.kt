package com.xuebingli.blackhole.activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
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
    lateinit var binding: ActivityMainBinding
    private var measurementRunning = ObservableBoolean(false)
    private lateinit var measurement: Measurement
    private lateinit var adapter: MeasurementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.measurementRunning = measurementRunning
        setContentView(binding.root)
        ForegroundService.startForegroundService(this)
        measurementRunning.set(isMeasurementRunning())
        measurement = Measurement.loadSetup(pref)
        adapter = MeasurementAdapter(measurement, this)
        val layoutManager = LinearLayoutManager(this)
        binding.container.also {
            it.adapter = adapter
            it.layoutManager = layoutManager
            it.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
        }
    }

    private fun initMeasurement() {
        measurement = Measurement.loadSetup(pref)
        adapter.updateMeasurement(measurement)
        binding.measurement = measurement
    }

    fun onStartButtonClick(view: View) {
        if (isMeasurementRunning()) {
            stopMeasurement()
        } else {
            startMeasurement()
        }
    }

    private fun startMeasurement() {
        foregroundService?.startMeasurement(measurement)
        measurementRunning.set(isMeasurementRunning())
    }

    private fun stopMeasurement() {
        foregroundService?.stopMeasurement()
        val recordSize = measurement.recordSet.values.sumOf { it.records.size }
        Toast.makeText(this, "Dumping log, $recordSize records are collected", Toast.LENGTH_SHORT)
            .show()
        initMeasurement()
        measurementRunning.set(isMeasurementRunning())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun addMeasurement(key: MeasurementKey) {
        when (key) {
            MeasurementKey.LocationInfo -> {
                if (measurement.addMeasurement(LocationMeasurementSetup())) {
                    adapter.notifyItemInserted(adapter.itemCount - 1)
                } else {
                    Toast.makeText(this, R.string.error_add_measurement, Toast.LENGTH_SHORT).show()
                }
            }
            MeasurementKey.CellularInfo -> {
                if (measurement.addMeasurement(CellularMeasurementSetup())) {
                    adapter.notifyItemInserted(adapter.itemCount - 1)
                } else {
                    Toast.makeText(this, R.string.error_add_measurement, Toast.LENGTH_SHORT).show()
                }
            }
            MeasurementKey.Ping -> {
                if (measurement.addMeasurement(PingMeasurementSetup())) {
                    adapter.notifyItemInserted(adapter.itemCount - 1)
                }
            }
            MeasurementKey.SubscriptionInfo -> {
                if (measurement.addMeasurement(SubscriptionMeasurementSetup())) {
                    adapter.notifyItemInserted(adapter.itemCount - 1)
                } else {
                    Toast.makeText(this, R.string.error_add_measurement, Toast.LENGTH_SHORT).show()
                }
            }
        }
        measurement.saveSetup(pref)
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
            R.id.reset_measurement -> {
                pref.edit {
                    putString(Preferences.MEASUREMENT_SETUP_KEY, "")
                    apply()
                }
                initMeasurement()
                true
            }
            else -> false
        }
    }
}

class MeasurementAdapter(var measurement: Measurement, val activity: MainActivity) :
    Adapter<MeasurementViewHolder>() {
    fun updateMeasurement(measurement: Measurement) {
        this.measurement = measurement
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeasurementViewHolder {
        val binding = DataBindingUtil.inflate<ItemMeasurementBinding>(
            activity.layoutInflater, R.layout.item_measurement, activity.binding.container, false
        )
        return MeasurementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MeasurementViewHolder, position: Int) {
        val setup = measurement.setups[position]
        holder.binding.measurementSetup = setup
        holder.binding.measurementRecords = measurement.recordSet[setup]
//        holder.binding.updated.text = activity.getString(R.string.last_modified, 100)
        holder.binding.root.setOnLongClickListener {
            AlertDialog.Builder(activity)
                .setTitle(R.string.delete_measurement_title)
                .setMessage(R.string.delete_measurement_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    measurement.recordSet.remove(setup)
                    notifyItemRemoved(position)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->

                }
                .show()
            true
        }
    }

    override fun getItemCount(): Int {
        return measurement.setups.size
    }
}

class MeasurementViewHolder(val binding: ItemMeasurementBinding) : ViewHolder(binding.root)
