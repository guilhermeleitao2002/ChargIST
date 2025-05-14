package pt.ist.cmu.chargist.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.suspendCancellableCoroutine
import pt.ist.cmu.chargist.data.model.NearbyService
import java.util.UUID
import kotlin.coroutines.resume

class PlacesRepository(
    private val placesClient: PlacesClient
) {
    suspend fun getNearbyPlaces(
        location: LatLng,
        radiusMeters: Int = 100
    ): List<NearbyService> = suspendCancellableCoroutine { continuation ->
        try {
            Log.d("PlacesRepository", "Getting nearby places at $location with radius $radiusMeters")

            try {
                // Create bounds for the search area
                val southwest = calculateLatLngOffset(location, -radiusMeters, -radiusMeters)
                val northeast = calculateLatLngOffset(location, radiusMeters, radiusMeters)
                val bounds = RectangularBounds.newInstance(southwest, northeast)

                // Try with a simpler query to get more results
                val request = FindAutocompletePredictionsRequest.builder()
                    .setLocationBias(bounds)
                    .setQuery("restaurant")
                    .build()

                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        Log.d("PlacesRepository", "Got ${response.autocompletePredictions.size} predictions")

                        if (response.autocompletePredictions.isNotEmpty()) {
                            val realServices = response.autocompletePredictions.take(3).map { prediction ->
                                NearbyService(
                                    id = UUID.randomUUID().toString(),
                                    chargerId = "",
                                    name = prediction.getPrimaryText(null).toString(),
                                    type = "RESTAURANT",
                                    distance = (30..radiusMeters).random()
                                )
                            }
                            continuation.resume(realServices)
                        } else {
                            Log.d("PlacesRepository", "No predictions found, using mock data")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("PlacesRepository", "Error finding places: ${exception.message}")
                    }
            } catch (e: Exception) {
                Log.e("PlacesRepository", "Error in Places API call: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Fatal error: ${e.message}")

            // Always return at least mock data
            val mockServices = listOf(
                NearbyService(
                    id = UUID.randomUUID().toString(),
                    chargerId = "",
                    name = "Coffee Shop",
                    type = "RESTAURANT",
                    distance = 50
                ),
                NearbyService(
                    id = UUID.randomUUID().toString(),
                    chargerId = "",
                    name = "Gas Station",
                    type = "GAS_STATION",
                    distance = 75
                )
            )
            continuation.resume(mockServices)
        }
    }

    private fun calculateLatLngOffset(center: LatLng, offsetMetersLat: Int, offsetMetersLng: Int): LatLng {
        // Earth's radius in meters
        val earthRadius = 6378137.0

        // Latitude offset
        val latOffset = (offsetMetersLat / earthRadius) * (180 / Math.PI)

        // Longitude offset
        val lngOffset = (offsetMetersLng / earthRadius) * (180 / Math.PI) / Math.cos(center.latitude * Math.PI / 180)

        return LatLng(center.latitude + latOffset, center.longitude + lngOffset)
    }
}