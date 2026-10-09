package com.offgrid.live

import android.view.Surface

interface OffgridHost {
    fun readSetting(key: String): String?
    fun writeSetting(key: String, value: String)
    fun fire(action: String)
    fun openApp()
    fun frameSurfaceSink(): FrameSink
}

fun interface FrameSink {
    fun attach(surface: Surface, width: Int, height: Int)

    fun detach(surface: Surface) = Unit
}

object LiveHostRegistry {
    @Volatile
    var host: OffgridHost? = null
}
