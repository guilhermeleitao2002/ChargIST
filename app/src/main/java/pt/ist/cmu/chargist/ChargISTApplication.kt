package pt.ist.cmu.chargist

import android.app.Application
import android.util.Log
import com.google.android.libraries.places.api.Places
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pt.ist.cmu.chargist.di.appModule
import pt.ist.cmu.chargist.di.dataModule
import pt.ist.cmu.chargist.di.firebaseModule
import pt.ist.cmu.chargist.di.networkModule
import pt.ist.cmu.chargist.di.viewModelModule

class ChargISTApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            // Note: Use the exact same key that's in your AndroidManifest.xml
            Places.initialize(applicationContext, "AIzaSyD8UGEnWmk0Tkt9TGA25e0Uod2QN56r_mo")
            Log.d("ChargISTApp", "Places API initialized successfully")
        } catch (e: Exception) {
            Log.e("ChargISTApp", "Failed to initialize Places API: ${e.message}")
        }

        startKoin {
            androidContext(this@ChargISTApplication)
            modules(listOf(
                appModule,
                dataModule,
                networkModule,
                viewModelModule,
                firebaseModule
            ))
        }
    }
}