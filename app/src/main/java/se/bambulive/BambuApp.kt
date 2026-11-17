package se.bambulive

import android.app.Application
import se.bambulive.data.Prefs
import se.bambulive.data.PrinterRepository
import se.bambulive.widget.RefreshWorker

class BambuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PrinterRepository.init(this)
        if (Prefs(this).hasDevice) RefreshWorker.schedule(this)
    }
}
