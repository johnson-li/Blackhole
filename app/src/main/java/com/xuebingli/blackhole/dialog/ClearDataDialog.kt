package com.xuebingli.blackhole.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.R

class ClearDataDialog(val callback: () -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        builder.setMessage(R.string.dialog_clear_data_message)
        builder.setTitle(R.string.dialog_clear_data_title)
        builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            callback()
        }
        return builder.create()
    }
}