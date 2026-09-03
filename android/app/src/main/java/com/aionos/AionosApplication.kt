package com.aionos

import android.app.Application
import com.aionos.security.EncryptedPrefs

class AionosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EncryptedPrefs.getInstance(this)
    }
}
