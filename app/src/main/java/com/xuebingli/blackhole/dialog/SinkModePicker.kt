package com.xuebingli.blackhole.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.utils.Preferences.Companion.PREFERENCE_NAME
import com.xuebingli.blackhole.utils.Preferences.Companion.SINK_MODE_KEY
import com.xuebingli.blackhole.utils.SinkMode

class SinkModePicker(val callback: (SinkMode) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPref = context!!.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(R.string.dialog_set_sink_mode_title)
            .setItems(SinkMode.values().map { it.name }.toTypedArray()) { _, which ->
                val name = SinkMode.values()[which].name
                sharedPref.edit().putString(SINK_MODE_KEY, name).apply()
                Toast.makeText(
                    context,
                    getString(R.string.toast_set_udp_mode, name),
                    Toast.LENGTH_SHORT
                ).show()
                callback(SinkMode.values()[which])
            }
        return builder.create()
    }
}