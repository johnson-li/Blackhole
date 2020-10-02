package com.xuebingli.blackhole.utils

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.activities.BaseActivity


class AndroidPermissionUtils(private val activity: BaseActivity) {
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val PHONE_STATE_PERMISSION_REQUEST_CODE = 2

        fun permissionGranted(context: Context, permission: String): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun permissionGranted(permission: String): Boolean {
        return permissionGranted(activity, permission)
    }

    fun requestPermission(permission: String): Boolean {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> requestPermission(
                permission, LOCATION_PERMISSION_REQUEST_CODE
            )
            Manifest.permission.READ_PHONE_STATE -> requestPermission(
                permission, PHONE_STATE_PERMISSION_REQUEST_CODE
            )
            else -> {
                Log.e("johnson", "Unsupported permission: $permission")
                false
            }
        }
    }

    private fun requestPermission(permission: String, requestCode: Int): Boolean {
        return if (permissionGranted(permission)) {
            true
        } else {
            requestPermission(activity, requestCode, permission, true)
            false
        }
    }

    private fun requestPermission(
        activity: BaseActivity, requestId: Int, permission: String, finishActivity: Boolean
    ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            RationaleDialog.newInstance(requestId)
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

class RationaleDialog() : DialogFragment() {
    companion object {
        private const val ARGUMENT_PERMISSION_REQUEST_CODE = "requestCode"

        fun newInstance(requestCode: Int): RationaleDialog {
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
        val message = when (requestCode) {
            AndroidPermissionUtils.LOCATION_PERMISSION_REQUEST_CODE -> R.string.dialog_location_permission
            AndroidPermissionUtils.PHONE_STATE_PERMISSION_REQUEST_CODE -> R.string.dialog_phone_state_permission
            else -> R.string.error
        }
        val permission = when (requestCode) {
            AndroidPermissionUtils.LOCATION_PERMISSION_REQUEST_CODE -> Manifest.permission.ACCESS_FINE_LOCATION
            AndroidPermissionUtils.PHONE_STATE_PERMISSION_REQUEST_CODE -> Manifest.permission.READ_PHONE_STATE
            else -> null
        }
        return AlertDialog.Builder(activity!!)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ActivityCompat.requestPermissions(
                    activity!!, arrayOf(permission),
                    requestCode
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}