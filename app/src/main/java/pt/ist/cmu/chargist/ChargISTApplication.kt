package pt.ist.cmu.chargist

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pt.ist.cmu.chargist.di.appModule
import pt.ist.cmu.chargist.di.dataModule
import pt.ist.cmu.chargist.di.networkModule
import pt.ist.cmu.chargist.di.viewModelModule

class ChargISTApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ChargISTApplication)
            modules(listOf(
                appModule,
                dataModule,
                networkModule,
                viewModelModule
            ))
        }
    }
}