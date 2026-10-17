package com.offgrid.live

import android.content.Context
import androidx.core.content.edit

enum class RemoteRole(val settingValue: String) {
    OFF("off"),
    CAMERA("camera"),
    REMOTE("remote");

    companion object {
        fun from(value: String?): RemoteRole =
            entries.firstOrNull { it.settingValue == value } ?: OFF
    }
}

class RemoteRoleStore(context: Context) {
    private val preferences = context.getSharedPreferences("offgrid_live_remote", Context.MODE_PRIVATE)

    var role: RemoteRole
        get() = RemoteRole.from(preferences.getString(ROLE_KEY, RemoteRole.OFF.settingValue))
        set(value) = preferences.edit { putString(ROLE_KEY, value.settingValue) }

    private companion object {
        const val ROLE_KEY = "fx.remote.role"
    }
}
