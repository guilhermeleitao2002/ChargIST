package pt.ist.cmu.chargist.di

/* Android / Kotlin */
import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room

/* Google Play services & Firebase */
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

/* Data layer */
import pt.ist.cmu.chargist.data.ChargISTDatabase
import pt.ist.cmu.chargist.data.api.ChargISTApi
import pt.ist.cmu.chargist.data.api.ChargISTApiService
import pt.ist.cmu.chargist.data.repository.*

/* View‑models */
import pt.ist.cmu.chargist.ui.viewmodel.*

import java.util.concurrent.TimeUnit

/* ─────────────────────────  APP‑WIDE SINGLETONS ───────────────────────── */

val appModule = module {

    single {
        androidContext().getSharedPreferences("chargist_prefs", Context.MODE_PRIVATE)
    }

    single<FusedLocationProviderClient> {
        LocationServices.getFusedLocationProviderClient(androidContext())
    }

    single {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}

/* ───────────────────────────────  ROOM  ──────────────────────────────── */

val dataModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            ChargISTDatabase::class.java,
            "chargist-db"
        ).build()
    }

    /* DAOs – repositories need them */
    single { get<ChargISTDatabase>().chargerDao() }
    single { get<ChargISTDatabase>().userDao() }

    single<ChargerRepository> { ChargerRepositoryImpl(get(), get(), get()) }
    single<UserRepository>    { UserRepositoryImpl(get(), get()) }
}

/* ─────────────────────────────  NETWORK  ─────────────────────────────── */

val networkModule = module {

    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://your-backend-url.com/api/")   // TODO replace
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single { get<Retrofit>().create(ChargISTApiService::class.java) }
    single { ChargISTApi(get()) }
}

/* ────────────────────────  FIREBASE AUTH / DB  ──────────────────────── */

val firebaseModule = module {

    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    single<AuthRepository> {
        FirebaseAuthRepository(
            androidContext(),  // pass Context directly
            get(),             // FirebaseAuth
            get()              // FirebaseFirestore
        )
    }
}

/* ─────────────────────────────  VIEW‑MODELS  ─────────────────────────── */

val viewModelModule = module {

    viewModel { UserViewModel(get()) }

    /** Map screen VM */
    viewModel {
        MapViewModel(
            get(),                 // ChargerRepository
            get(),                 // FusedLocationProviderClient
            androidContext()       // ← Context injected without a bean
        )
    }

    viewModel { ChargerViewModel(get(), get()) }
}
