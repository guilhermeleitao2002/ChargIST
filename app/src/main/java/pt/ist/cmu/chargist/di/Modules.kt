package pt.ist.cmu.chargist.di

import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import pt.ist.cmu.chargist.data.ChargISTDatabase
import pt.ist.cmu.chargist.data.api.ChargISTApi
import pt.ist.cmu.chargist.data.api.ChargISTApiService
import pt.ist.cmu.chargist.data.repository.ChargerRepository
import pt.ist.cmu.chargist.data.repository.ChargerRepositoryImpl
import pt.ist.cmu.chargist.data.repository.UserRepository
import pt.ist.cmu.chargist.data.repository.UserRepositoryImpl
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
import pt.ist.cmu.chargist.ui.viewmodel.UserViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    single { androidContext().getSharedPreferences("chargist_prefs", Context.MODE_PRIVATE) }
    single<FusedLocationProviderClient> { LocationServices.getFusedLocationProviderClient(androidContext()) }
}

val dataModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            ChargISTDatabase::class.java,
            "chargist-db"
        ).build()
    }

    // DAOs
    single { get<ChargISTDatabase>().chargerDao() }
    single { get<ChargISTDatabase>().userDao() }

    // Repositories
    single<ChargerRepository> { ChargerRepositoryImpl(get(), get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
}

val networkModule = module {
    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://your-backend-url.com/api/") // Replace with your actual backend URL
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single { get<Retrofit>().create(ChargISTApiService::class.java) }
    single { ChargISTApi(get()) }
}

val viewModelModule = module {
    viewModel { UserViewModel(get()) }
    viewModel { MapViewModel(get(), get(), get()) }
    viewModel { ChargerViewModel(get(), get()) }
}