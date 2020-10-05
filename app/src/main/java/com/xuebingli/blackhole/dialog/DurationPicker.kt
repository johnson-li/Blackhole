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
import com.xuebingli.blackhole.utils.getDurationString

class DurationPicker(val callback: (Int) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPref = context!!.applicationContext
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val builder = AlertDialog.Builder(activity!!)
        val durations = listOf(5, 10, 15, 20, 30, 45, 60, 2 * 60, 5 * 60, 10 * 60,
            20 * 60, 30 * 60, 60 * 60, Int.MAX_VALUE)
        builder.setTitle(R.string.dialog_set_pour_bitrate_title)
            .setItems(durations.map { getDurationString(requireContext(), it) }
                .toTypedArray()) { _, which ->
                sharedPref.edit().putInt(DURATION_KEY, durations[which]).apply()
                Toast.makeText(
                    context,
                    getString(
                        R.string.toast_set_duration,
                        getDurationString(requireContext(), durations[which])
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                callback(durations[which])
            }
        return builder.create()
    }
}