package com.xuebingli.blackhole.utils

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.activities.BaseActivity


class AndroidPermissionUtils(private val activity: BaseActivity) {
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    fun requestLocationPermission(): Boolean {
        val permissionCheck =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            requestPermission(
                activity, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            );
            return false
        }
    }

    private fun requestPermission(
        activity: BaseActivity, requestId: Int, permission: String, finishActivity: Boolean
    ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            RationaleDialog.newInstance(requestId, finishActivity)
                .show(activity.supportFragmentManager, "dialog")
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestId)
        }
    }

    fun permissionGranted(
        grantPermissions: Array<String>, grantResults: IntArray, permission: String
    ): Boolean {
        for (i in grantPermissions.indices) {
            if (permission == grantPermissions[i]) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED
            }
        }
        return false
    }
}

class RationaleDialog : DialogFragment() {
    companion object {
        private const val ARGUMENT_PERMISSION_REQUEST_CODE = "requestCode"

        fun newInstance(requestCode: Int, finishActivity: Boolean): RationaleDialog {
            val arguments = Bundle()
            arguments.putInt(ARGUMENT_PERMISSION_REQUEST_CODE, requestCode)
            val dialog = RationaleDialog()
            dialog.arguments = arguments
            return dialog
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = arguments
        val requestCode = arguments!!.getInt(ARGUMENT_PERMISSION_REQUEST_CODE)
        return AlertDialog.Builder(activity!!)
            .setMessage(R.string.dialog_location_permission)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ActivityCompat.requestPermissions(
                    activity!!, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    requestCode
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}