package com.xuebingli.blackhole.picker

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.utils.Preferences.Companion.POUR_BITRATE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.PREFERENCE_NAME

class BitratePicker(val callback: (Int) -> Unit) : DialogFragment() {
    val K = 1024
    val M = 1024 * K
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPref = context!!.applicationContext
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val builder = AlertDialog.Builder(activity!!)
        val bitrateValues = listOf(K, 10 * K, 100 * K, 1 * M, 10 * M, 100 * M, 200 * M)
        val bitrateNames =
            listOf("1 kbps", "10 kbps", "100 kbps", "1 mbps", "10 mbps", "100 mbps", "200 mbps")
        builder.setTitle(R.string.dialog_set_pour_bitrate_title)
            .setItems(bitrateNames.toTypedArray()) { _, which ->
                sharedPref.edit().putInt(POUR_BITRATE_KEY, bitrateValues[which]).apply()
                Toast.makeText(
                    context,
                    getString(R.string.toast_set_pour_bitrate, bitrateValues[which]),
                    Toast.LENGTH_SHORT
                ).show()
                callback(bitrateValues[which])
            }
        return builder.create()
    }
}