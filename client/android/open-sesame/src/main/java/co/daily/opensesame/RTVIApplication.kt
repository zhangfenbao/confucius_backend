package co.daily.opensesame

import android.app.Application

class RTVIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Preferences.initAppStart(this)
    }
}