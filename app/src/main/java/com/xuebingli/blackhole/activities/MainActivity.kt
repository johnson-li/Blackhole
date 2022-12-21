package com.xuebingli.blackhole.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.databinding.ActivityMainBinding
import com.xuebingli.blackhole.services.ForegroundService

class MainActivity : BaseActivity0() {
    private lateinit var binding: ActivityMainBinding
    private var measurementRunning: Boolean = false

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.measurementRunning = measurementRunning
        setContentView(binding.root)
        ForegroundService.updateForegroundService(this)

        measurementRunning = isMeasurementRunning()

        binding.startButton.setOnClickListener {
            if (isMeasurementRunning()) {
                startMeasurement()
            } else {
                stopMeasurement()
            }
        }
    }

    private fun startMeasurement() {

    }

    private fun stopMeasurement() {

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.enable_vpn -> {
                true
            }
            else -> false
        }
    }
}
