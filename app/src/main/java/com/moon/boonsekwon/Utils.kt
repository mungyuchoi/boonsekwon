package com.moon.boonsekwon

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log

object Utils {
    fun getVersionInfo(context: Context?): String? {
        var version = "1.14"
        val packageInfo: PackageInfo
        if (context == null) {
            return version
        }
        try {
            packageInfo = context.applicationContext
                .packageManager
                .getPackageInfo(context.applicationContext.packageName, 0)
            version = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("Utils", "getVersionInfo :" + e.message)
        }
        return version
    }
}