package za.ac.imali

import android.app.Application
import androidx.work.Configuration

class ImaliApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
