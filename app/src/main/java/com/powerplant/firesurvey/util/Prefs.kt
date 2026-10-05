package com.powerplant.firesurvey.util

import android.content.Context

class Prefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var inspectorName: String
        get() = prefs.getString(KEY_INSPECTOR, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_INSPECTOR, value.trim()).apply()

    private companion object {
        const val KEY_INSPECTOR = "inspector_name"
    }
}
