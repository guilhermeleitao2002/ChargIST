package pt.ist.cmu.chargist

import android.app.Application
import android.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pt.ist.cmu.chargist.di.appModule
import pt.ist.cmu.chargist.di.firebaseModule
import pt.ist.cmu.chargist.di.networkModule
import pt.ist.cmu.chargist.di.viewModelModule

/**
 * Application entry‑point.
 */
class ChargISTApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.i("ChargISTApp", "Starting Koin…")
        startKoin {
            androidContext(this@ChargISTApplication)
            modules(
                listOf(
                    appModule,
                    networkModule,
                    viewModelModule,
                    firebaseModule
                )
            )
        }
        Log.i("ChargISTApp", "Koin initialised")
    }
}
