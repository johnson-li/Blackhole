package com.xuebingli.blackhole.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.utils.Preferences.Companion.PACKET_SIZE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.PREFERENCE_NAME

class PacketSizePicker : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPref = context!!.applicationContext
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val builder = AlertDialog.Builder(activity!!)
        val input = EditText(activity)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        resources.displayMetrics.density.also {
            input.setPadding(
                (32 * it + 0.5).toInt(),
                input.paddingTop,
                (32 * it + 0.5).toInt(),
                input.paddingBottom
            )
        }
        input.setText(sharedPref.getInt(PACKET_SIZE_KEY, -1).toString())
        builder.setTitle(R.string.dialog_set_packet_size_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val packetSize = input.text.toString().toInt()
                sharedPref.edit(true) {
                    putInt(PACKET_SIZE_KEY, packetSize)
                }
                Toast.makeText(
                    context,
                    getString(R.string.toast_set_packet_size, packetSize),
                    Toast.LENGTH_SHORT
                ).show()
            }
        return builder.create()
    }
}