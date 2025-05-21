package pt.ist.cmu.chargist.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ist.cmu.chargist.data.model.NearbyPlace
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

interface GooglePlacesService {
    @GET("maps/api/place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("key") apiKey: String
    ): NearbyPlacesResponse
}

data class NearbyPlacesResponse(
    val results: List<PlaceResult>,
    val status: String
)

data class PlaceResult(
    val place_id: String,
    val name: String,
    val types: List<String>,
    val vicinity: String,
    val geometry: Geometry
)

data class Geometry(
    val location: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)

class NearbyPlacesRepository {
    private val apiKey = "AIzaSyAU_8dfYDNi471YCS6ja-gZ8Clv4iM7jB4"
    private val service: GooglePlacesService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(GooglePlacesService::class.java)
    }

    suspend fun getNearbyPlaces(location: LatLng, radius: Int = 500): List<NearbyPlace> = withContext(Dispatchers.IO) {
        try {
            val placeTypes = listOf("restaurant", "store", "gas_station", "cafe")
            val results = mutableListOf<NearbyPlace>()

            // Query for each place type
            for (type in placeTypes) {
                try {
                    val locationString = "${location.latitude},${location.longitude}"
                    val response = service.getNearbyPlaces(locationString, radius, type, apiKey)

                    if (response.status == "OK") {
                        val places = response.results.map { result ->
                            val placeLocation = LatLng(
                                result.geometry.location.lat,
                                result.geometry.location.lng
                            )

                            // calculate distance
                            val distance = calculateDistance(location, placeLocation).toInt()

                            NearbyPlace(
                                id = result.place_id,
                                name = result.name,
                                placeType = if (result.types.isNotEmpty()) result.types[0] else type,
                                distance = distance,
                                latLng = placeLocation
                            )
                        }
                        results.addAll(places)
                    }
                } catch (e: Exception) {
                    Log.e("NearbyPlacesRepo", "Error fetching $type places: ${e.message}")
                }
            }

            // Return real results if available, otherwise return mock data
            if (results.isNotEmpty()) {
                results.sortedBy { it.distance }
            } else {
                Log.d("NearbyPlacesRepo", "No real results available")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NearbyPlacesRepo", "Error fetching nearby places: ${e.message}")
            emptyList()
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val earthRadius = 6371000.0 // meters

        val startLat = Math.toRadians(start.latitude)
        val endLat = Math.toRadians(end.latitude)
        val latDiff = Math.toRadians(end.latitude - start.latitude)
        val lngDiff = Math.toRadians(end.longitude - start.longitude)

        val a = sin(latDiff / 2) * sin(latDiff / 2) +
                cos(startLat) * cos(endLat) *
                sin(lngDiff / 2) * sin(lngDiff / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}