package info.learncoding.twiliosdk

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import info.learncoding.twiliovideocall.TwilioSdk

@HiltAndroidApp
class TwilioSdkApp :Application() {
    override fun onCreate() {
        super.onCreate()
        TwilioSdk.initSdk(this)
    }
}