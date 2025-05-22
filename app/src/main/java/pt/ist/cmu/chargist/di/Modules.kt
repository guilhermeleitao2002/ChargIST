package pt.ist.cmu.chargist.di

import android.content.Context
import android.net.ConnectivityManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import pt.ist.cmu.chargist.data.repository.AuthRepository
import pt.ist.cmu.chargist.data.repository.ChargerRepository
import pt.ist.cmu.chargist.data.repository.FirestoreChargerRepository
import pt.ist.cmu.chargist.data.repository.FirebaseAuthRepository
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
import pt.ist.cmu.chargist.ui.viewmodel.UserViewModel
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.FirebaseFirestoreSettings
import pt.ist.cmu.chargist.data.repository.ImageStorageRepository
import pt.ist.cmu.chargist.data.repository.NearbyPlacesRepository

val appModule = module {
    single     { androidContext().getSharedPreferences("chargist_prefs", Context.MODE_PRIVATE) }
    single<FusedLocationProviderClient> { LocationServices.getFusedLocationProviderClient(androidContext()) }
    single     { androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
}


val networkModule = module {
    single {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }


    single {
        NearbyPlacesRepository()
    }
}

val firebaseModule = module {

    single { FirebaseAuth.getInstance() }

    single {
        FirebaseFirestore.getInstance().apply {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)   // allow for local cache
                .build()
            firestoreSettings = settings
        }
    }

    single { com.google.firebase.storage.FirebaseStorage.getInstance() }
    single { ImageStorageRepository(get()) }

    single<ChargerRepository> { FirestoreChargerRepository(get()) }

    /* Auth repository */
    single<AuthRepository> {
        FirebaseAuthRepository(
            androidContext(),
            get(),               // FirebaseAuth
            get()                // FirebaseFirestore
        )
    }

    single<ChargerRepository> { FirestoreChargerRepository(get()) }
}


val viewModelModule = module {

    viewModel { UserViewModel(get()) }

    viewModel {
        MapViewModel(
            get(),   // ChargerRepository
            get(),   // FusedLocationProviderClient
            get()    // Context
        )
    }

    viewModel {
        ChargerViewModel(
            get(),   // ChargerRepository
            get(),   // UserRepository
            get()    // ImageStorageRepository
        )
    }
}

