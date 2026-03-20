package com.reflekt.journal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
@HiltAndroidApp
class ReflektApp : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
    }
}
