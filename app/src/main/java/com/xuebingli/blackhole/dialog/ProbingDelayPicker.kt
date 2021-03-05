package com.xuebingli.blackhole.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.utils.Preferences.Companion.DURATION_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.PREFERENCE_NAME
import com.xuebingli.blackhole.utils.Preferences.Companion.PROBING_DELAY_KEY
import com.xuebingli.blackhole.utils.getDurationString
import com.xuebingli.blackhole.utils.getProbingDelayString

class ProbingDelayPicker(val callback: (Int) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPref = context!!.applicationContext
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val builder = AlertDialog.Builder(activity!!)
        val delays = listOf(
            1, 2, 3, 4, 5, 8, 10, 20, 30, 40, 50, 80, 100, 200, 300, 400, 500, 800,
        )
        builder.setTitle(R.string.dialog_set_probing_delay_title)
            .setItems(delays.map(::getProbingDelayString).toTypedArray()) { _, which ->
                sharedPref.edit().putInt(PROBING_DELAY_KEY, delays[which]).apply()
                Toast.makeText(
                    context,
                    getString(
                        R.string.toast_set_probing_delay,
                        getProbingDelayString(delays[which])
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                callback(delays[which])
            }
        return builder.create()
    }
}