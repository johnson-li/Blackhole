package com.xuebingli.blackhole.picker

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.activities.PingActivity
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.utils.getInterfaces

class InterfacePicker : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sharedPref = activity!!.getPreferences(Context.MODE_PRIVATE)
        val builder = AlertDialog.Builder(activity!!)
        val interfaces = getInterfaces()
        builder.setTitle(R.string.dialog_set_interface_title).setItems(interfaces) { _, which ->
            val name = interfaces[which]
            sharedPref.edit().putString(PingActivity.INTERFACE_PREF_KEY, name).apply()
            Toast.makeText(
                context,
                getString(R.string.toast_set_interface, name),
                Toast.LENGTH_SHORT
            ).show()
        }
        return builder.create()
    }
}