package com.xuebingli.blackhole.activities

import android.os.Bundle
import android.view.View
import com.google.android.material.button.MaterialButton
import com.xuebingli.blackhole.R

class TetheringActivity : BaseActivity(true) {
    private lateinit var tetheringButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tethering)
        tetheringButton = findViewById(R.id.tetheringButton)
        tetheringButton.setOnClickListener(this::toggleTethering)
    }

    private fun toggleTethering(view: View) {

    }
}