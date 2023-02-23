package com.xuebingli.blackhole.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.InputType
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.databinding.ActivityMainBinding
import com.xuebingli.blackhole.databinding.DialogSetupInputBinding
import com.xuebingli.blackhole.databinding.ItemDialogSetupBinding
import com.xuebingli.blackhole.databinding.ItemMeasurementBinding
import com.xuebingli.blackhole.models.*
import com.xuebingli.blackhole.services.ForegroundService
import com.xuebingli.blackhole.utils.Preferences
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties

class MainActivity : BaseActivity0() {

    lateinit var binding: ActivityMainBinding
    private var measurementRunning = ObservableBoolean(false)
    private lateinit var measurement: Measurement
    private lateinit var adapter: MeasurementAdapter
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            binding.container.post {
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.measurementRunning = measurementRunning
        setContentView(binding.root)
        ForegroundService.startForegroundService(this)
        measurementRunning.set(isMeasurementRunning())
        measurement = Measurement.loadSetup(pref)
        adapter = MeasurementAdapter(measurement, this)
        binding.measurement = measurement
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
        if (!checkPermissions()) {
            Toast.makeText(this, R.string.permission_missing, Toast.LENGTH_SHORT).show()
            return
        }
        initMeasurement()
        if (measurement.empty) {
            Toast.makeText(this, R.string.measurement_no_setup, Toast.LENGTH_SHORT).show()
        } else {
            foregroundService?.handler = handler
            foregroundService?.startMeasurement(measurement)
            measurementRunning.set(isMeasurementRunning())
        }
    }

    private fun stopMeasurement() {
        foregroundService?.stopMeasurement()
        val recordSize = measurement.recordSet.values.sumOf { it.records.size }
        Toast.makeText(this, "Dumping log, $recordSize records are collected", Toast.LENGTH_SHORT)
            .show()
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
                    onMeasurementAdded()
                } else {
                    Toast.makeText(this, R.string.error_add_measurement, Toast.LENGTH_SHORT).show()
                }
            }
            MeasurementKey.CellularInfo -> {
                if (measurement.addMeasurement(CellularMeasurementSetup())) {
                    onMeasurementAdded()
                } else {
                    Toast.makeText(this, R.string.error_add_measurement, Toast.LENGTH_SHORT).show()
                }
            }
            MeasurementKey.Ping -> {
                showSetupInputDialog(MeasurementKey.Ping) {
                    if (measurement.addMeasurement(it)) {
                        onMeasurementAdded()
                    }
                }
            }
            MeasurementKey.SubscriptionInfo -> {
                if (measurement.addMeasurement(SubscriptionMeasurementSetup())) {
                    onMeasurementAdded()
                } else {
                    Toast.makeText(this, R.string.error_add_measurement, Toast.LENGTH_SHORT).show()
                }
            }
            MeasurementKey.UdpPing -> {
                showSetupInputDialog(MeasurementKey.UdpPing) {
                    if (measurement.addMeasurement(it)) {
                        onMeasurementAdded()
                    }
                }
            }
            MeasurementKey.NetworkInfo -> {
                if (measurement.addMeasurement(NetworkInfoMeasurementSetup())) {
                    onMeasurementAdded()
                }
            }
            MeasurementKey.Traceroute -> {
                showSetupInputDialog(MeasurementKey.Traceroute) {
                    if (measurement.addMeasurement(it)) {
                        onMeasurementAdded()
                    }
                }
            }
        }
    }

    private fun onMeasurementAdded() {
        adapter.notifyItemInserted(adapter.itemCount - 1)
        measurement.saveSetup(pref)
    }

    private fun showSetupInputDialog(
        key: MeasurementKey,
        callback: (MeasurementSetup) -> Unit
    ) {
        val binding =
            DialogSetupInputBinding.inflate(layoutInflater, this.binding.root as ViewGroup, false)
        val setup = key.measurementSetupClass.createInstance()

        val adapter = SetupAdapter(setup, layoutInflater, this.binding.root)
        val layoutManager = LinearLayoutManager(this)
        binding.container.let {
            it.adapter = adapter
            it.layoutManager = layoutManager
        }
        val dialog = AlertDialog.Builder(this).setView(binding.root)
            .setTitle(getString(R.string.add_measurement_detail, key.name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                callback(setup)
            }.setNegativeButton(android.R.string.cancel) { _, _ -> }.create()
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.enable_vpn -> {
                true
            }
            R.id.add_measurement -> {
                if (!isMeasurementRunning()) {
                    AlertDialog.Builder(this).setTitle(R.string.add_measurement)
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .setItems(
                            MeasurementKey.values().map { it.name }.toTypedArray()
                        ) { _, p1 -> addMeasurement(MeasurementKey.values()[p1]) }
                        .show()
                }
                true
            }
            R.id.reset_measurement -> {
                if (!isMeasurementRunning()) {
                    pref.edit {
                        putString(Preferences.MEASUREMENT_SETUP_KEY, "")
                        apply()
                    }
                    initMeasurement()
                }
                true
            }
            R.id.pour_activity -> {
                startActivity(Intent(this, PourActivity::class.java))
                true
            }
            R.id.laptop_assistant -> {
                startActivity(Intent(this, LaptopAssistantActivity::class.java))
                true
            }
            else -> false
        }
    }

}

class SetupAdapter(
    private val setup: MeasurementSetup,
    private val inflater: LayoutInflater,
    val parent: View
) : Adapter<SetupViewHolder>() {
    private val properties =
        (listOf(setup::class.declaredMemberProperties)[0] as ArrayList).filter { it is KMutableProperty1 }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetupViewHolder {
        val binding = ItemDialogSetupBinding.inflate(inflater, parent, false)
        return SetupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SetupViewHolder, position: Int) {
        val property = properties[position]
        holder.binding.layout.hint = property.name.replaceFirstChar(Char::titlecase)
        holder.binding.editText.inputType = when (property.returnType.classifier) {
            Int::class -> InputType.TYPE_CLASS_NUMBER
            String::class -> InputType.TYPE_CLASS_TEXT
            else -> InputType.TYPE_CLASS_TEXT
        }
        when (property.returnType.classifier) {
            Int::class ->
                (property as KMutableProperty1<MeasurementSetup, Int>).let { p ->
                    holder.binding.editText.setText(p.get(setup).toString())
                    holder.binding.editText.addTextChangedListener {
                        p.set(setup, it.toString().toIntOrNull() ?: 0)
                    }
                }
            String::class ->
                (property as KMutableProperty1<MeasurementSetup, String>).let { p ->
                    holder.binding.editText.setText(p.get(setup))
                    holder.binding.editText.addTextChangedListener {
                        p.set(setup, it.toString())
                    }
                }
            else ->
                throw IllegalArgumentException("Unsupported property type: ${property.returnType}")
        }
    }

    override fun getItemCount(): Int = properties.size
}

class SetupViewHolder(val binding: ItemDialogSetupBinding) : ViewHolder(binding.root)

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
        holder.binding.records = measurement.recordSet[setup]!!
        holder.binding.root.setOnLongClickListener {
            if (!activity.isMeasurementRunning()) {
                AlertDialog.Builder(activity)
                    .setTitle(R.string.delete_measurement_title)
                    .setMessage(R.string.delete_measurement_message)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        measurement.removeMeasurement(setup)
                        measurement.saveSetup(activity.pref)
                        notifyItemRemoved(position)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
            }
            true
        }
    }

    override fun getItemCount(): Int {
        return measurement.setups.size
    }
}

class MeasurementViewHolder(val binding: ItemMeasurementBinding) : ViewHolder(binding.root)
