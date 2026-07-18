package com.godwin.orbitlauncher

import android.app.Application
import com.godwin.orbitlauncher.di.AppGraph

class OrbitLauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
