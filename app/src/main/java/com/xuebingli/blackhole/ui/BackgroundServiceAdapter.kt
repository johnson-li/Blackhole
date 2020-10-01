package com.xuebingli.blackhole.ui

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.activities.BaseActivity
import com.xuebingli.blackhole.services.ForegroundService
import com.xuebingli.blackhole.utils.AndroidPermissionUtils
import com.xuebingli.blackhole.utils.Preferences

class BackgroundServiceAdapter(
    private val context: BaseActivity,
    private val sharedPreferences: SharedPreferences
) :
    RecyclerView.Adapter<BackgroundServiceViewHolder>() {
    private val services = ArrayList<BackgroundService>(10)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackgroundServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_background_service, parent, false)
        return BackgroundServiceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return services.size
    }

    override fun onBindViewHolder(holder: BackgroundServiceViewHolder, position: Int) {
        val service = services[position]
        holder.name.text = service.getName(context)
        holder.switch.isChecked = sharedPreferences.getBoolean(service.prefKey, false)
        holder.description.text = service.getDescription(context)
        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            if (service == BackgroundService.LOCATION && isChecked) {
                if (AndroidPermissionUtils(context).requestLocationPermission()) {
                    sharedPreferences.edit(true) {
                        putBoolean(service.prefKey, isChecked)
                    }
                } else {
                    holder.switch.isChecked = false
                }
            } else {
                sharedPreferences.edit(true) {
                    putBoolean(service.prefKey, isChecked)
                }
            }
            ForegroundService.updateForegroundService(context)
        }
    }

    fun locationPermissionGranted() {
        sharedPreferences.edit(true) {
            putBoolean(BackgroundService.LOCATION.prefKey, true)
        }
        services.forEachIndexed { index, backgroundService ->
            if (backgroundService == BackgroundService.LOCATION) {
                notifyItemChanged(index)
                ForegroundService.updateForegroundService(context)
            }
        }
    }

    fun setup() {
        for (service in BackgroundService.values()) {
            services.add(service)
            notifyItemInserted(services.size - 1)
        }
    }
}

class BackgroundServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val name: TextView = view.findViewById(R.id.name)
    val switch: SwitchMaterial = view.findViewById(R.id.switchButton)
    val description: TextView = view.findViewById(R.id.description)
}

enum class BackgroundService(
    private val nameResId: Int,
    private val descResId: Int,
    val prefKey: String
) {
    LOCATION(R.string.location, R.string.location_desc, Preferences.SERVICE_LOCATION),
    RSSI(R.string.cell_info, R.string.cell_info_desc, Preferences.SERVICE_CELL_INFO);

    fun getName(context: Context): String {
        return context.getString(nameResId)
    }

    fun getDescription(context: Context): String {
        return context.getString(descResId)
    }
}
