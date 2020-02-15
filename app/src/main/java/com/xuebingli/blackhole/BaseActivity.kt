package com.xuebingli.blackhole

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity(private val displayHomeAsUp: Boolean): AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(displayHomeAsUp)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}