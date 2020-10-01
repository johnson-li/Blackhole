package com.xuebingli.blackhole.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.utils.Preferences.Companion.FREQUENCY_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.PREFERENCE_NAME

class FrequencyPicker() : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPref = context!!.applicationContext
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val builder = AlertDialog.Builder(activity!!)
        val frequencies = listOf(10, 100, 1000, 2000, 10000, 30000)
        builder.setTitle(R.string.dialog_set_frequency)
            .setItems(frequencies.map { i -> if (i >= 1000) "${i/1000} s" else "$i ms" }
                .toTypedArray()) { _, which ->
                sharedPref.edit().putInt(FREQUENCY_KEY, frequencies[which]).apply()
                Toast.makeText(
                    context,
                    getString(R.string.toast_set_frequency, frequencies[which]),
                    Toast.LENGTH_SHORT
                ).show()
            }
        return builder.create()
    }
}