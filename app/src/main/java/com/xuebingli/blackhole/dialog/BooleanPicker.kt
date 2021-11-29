package com.xuebingli.blackhole.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.utils.Preferences.Companion.LOGGING_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.PREFERENCE_NAME
import com.xuebingli.blackhole.utils.Preferences.Companion.SINK_MODE_KEY
import com.xuebingli.blackhole.utils.SinkMode

class BooleanPicker(val callback: (Boolean) -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(R.string.dialog_boolean_title)
            .setItems(arrayOf("True", "False")) { _, which ->
                val value = which == 0
                Toast.makeText(
                    context,
                    getString(R.string.toast_set_enabled, value),
                    Toast.LENGTH_SHORT
                ).show()
                callback(value)
            }
        return builder.create()
    }
}