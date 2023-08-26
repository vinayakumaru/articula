package com.example.articula.utils.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class Permission(private val context: Context) {

    companion object {
        const val RECORD_AUDIO = android.Manifest.permission.RECORD_AUDIO
        const val WRITE_EXTERNAL_STORAGE = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        private const val READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private const val READ_MEDIA_IMAGES = android.Manifest.permission.READ_MEDIA_IMAGES

        fun getImagePermissions(): List<String> {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return listOf(READ_MEDIA_IMAGES)
            }
            return listOf(READ_EXTERNAL_STORAGE)
        }
    }

    interface PermissionListener {
        fun onPermissionsGranted()
        fun onPermissionsDenied(deniedPermissions: List<String>)
    }

    fun requestPermissions(
        permissions: List<String>,
        listener: PermissionListener
    ) {
        Dexter.withContext(context)
            .withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if ((report != null) && report.areAllPermissionsGranted()) {
                        listener.onPermissionsGranted()
                    } else {
                        val deniedPermissions = report?.deniedPermissionResponses?.map { it.permissionName }
                            ?: emptyList()
                        listener.onPermissionsDenied(deniedPermissions)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isPermissionsGranted(permissions: List<String>): Boolean {
        for(permission in permissions) {
            if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}
