package pt.ist.cmu.chargist.di

/* Kotlin / Android */
import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room

/* Google Play services & Firebase */
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/* Networking */
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/* Koin */
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/* Project – data layer */
import pt.ist.cmu.chargist.data.ChargISTDatabase
import pt.ist.cmu.chargist.data.api.ChargISTApi
import pt.ist.cmu.chargist.data.api.ChargISTApiService
import pt.ist.cmu.chargist.data.repository.AuthRepository
import pt.ist.cmu.chargist.data.repository.ChargerRepository
import pt.ist.cmu.chargist.data.repository.FirestoreChargerRepository     // ← NEW
import pt.ist.cmu.chargist.data.repository.FirebaseAuthRepository
import pt.ist.cmu.chargist.data.repository.UserRepository
import pt.ist.cmu.chargist.data.repository.UserRepositoryImpl

/* Project – view‑models */
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
import pt.ist.cmu.chargist.ui.viewmodel.UserViewModel

import java.util.concurrent.TimeUnit

/* ───────────────────── APP‑WIDE SINGLETONS ───────────────────── */

val appModule = module {
    single     { androidContext().getSharedPreferences("chargist_prefs", Context.MODE_PRIVATE) }
    single<FusedLocationProviderClient> { LocationServices.getFusedLocationProviderClient(androidContext()) }
    single     { androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
}

/* ───────────────────────────── ROOM (local cache) ───────────────────────────
   You can keep these beans if you still use Room elsewhere; they no longer feed
   the ChargerRepository. Feel free to delete once the whole project is on Firestore.
────────────────────────────────────────────────────────────────────────────── */
val dataModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            ChargISTDatabase::class.java,
            "chargist-db"
        ).build()
    }

    // DAOs (leave or remove – not referenced by the Firestore repo)
    single { get<ChargISTDatabase>().userDao() }

    // User repo still backed by Room
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
}

/* ───────────────────────────── NETWORK (Retrofit) ─────────────────────────── */
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
        Retrofit.Builder()
            .baseUrl("https://your-backend-url.com/api/")   // TODO replace when you have one
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single { get<Retrofit>().create(ChargISTApiService::class.java) }
    single { ChargISTApi(get()) }
}

/* ───────────────────────────── FIREBASE  ──────────────────────────────── */
val firebaseModule = module {

    /* Core Firebase singletons */
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    /* Auth repository */
    single<AuthRepository> {
        FirebaseAuthRepository(
            androidContext(),
            get(),               // FirebaseAuth
            get()                // FirebaseFirestore
        )
    }

    /* --------------  🔥  NEW: ChargerRepository over Firestore -------------- */
    single<ChargerRepository> { FirestoreChargerRepository(get()) }
    //                         ^ maps interface to your new implementation
}

/* ───────────────────────────── VIEW‑MODELS ──────────────────────────────── */
val viewModelModule = module {

    viewModel { UserViewModel(get()) }

    viewModel {
        MapViewModel(
            get(),   // ChargerRepository (now backed by Firestore)
            get(),   // FusedLocationProviderClient
            get()    // Context
        )
    }

    viewModel { ChargerViewModel(get(), get()) }
}
