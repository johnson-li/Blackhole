package com.xuebingli.blackhole.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.utils.Constants.Companion.G
import com.xuebingli.blackhole.utils.Constants.Companion.K
import com.xuebingli.blackhole.utils.Constants.Companion.M
import com.xuebingli.blackhole.utils.Preferences.Companion.POUR_BITRATE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.PREFERENCE_NAME
import com.xuebingli.blackhole.utils.getBitrateString

class BitratePicker(val callback: (Int) -> Unit, ) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPref = context!!.applicationContext
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val builder = AlertDialog.Builder(activity!!)
        val bitrateValues =
            listOf(
                K, 5 * K, 10 * K, 50 * K, 100 * K, 200 * K, 300 * K, 500 * K, 800 * K,
                M, 5 * M, 10 * M, 50 * M, 100 * M, 200 * M, 300 * M, 500 * M, 800 * M,
                G, (1.2 * G).toInt(), (1.5 * G).toInt(), (1.8 * G).toInt(),
            )
        builder.setTitle(R.string.dialog_set_bitrate_title)
            .setItems(bitrateValues.map { getBitrateString(it) }.toTypedArray()) { _, which ->
                sharedPref.edit().putInt(POUR_BITRATE_KEY, bitrateValues[which]).apply()
                Toast.makeText(
                    context,
                    getString(
                        R.string.toast_set_bitrate, getBitrateString(bitrateValues[which])
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                callback(bitrateValues[which])
            }
        return builder.create()
    }
}