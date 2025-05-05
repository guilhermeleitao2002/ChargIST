package pt.ist.cmu.chargist.data.api

import pt.ist.cmu.chargist.data.model.Charger
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.NearbyService
import pt.ist.cmu.chargist.data.model.User
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API endpoints for ChargIST backend
 */
interface ChargISTApiService {
    // User endpoints
    @POST("users")
    suspend fun createUser(@Body user: User): User

    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): User

    // Charger endpoints
    @GET("chargers")
    suspend fun getChargers(
        @Query("lat") latitude: Double?,
        @Query("lng") longitude: Double?,
        @Query("radius") radiusKm: Double?
    ): List<Charger>

    @GET("chargers/{id}")
    suspend fun getCharger(@Path("id") chargerId: String): Charger

    @POST("chargers")
    suspend fun createCharger(@Body charger: Charger): Charger

    @PUT("chargers/{id}")
    suspend fun updateCharger(
        @Path("id") chargerId: String,
        @Body charger: Charger
    ): Charger

    @PUT("chargers/{id}/favorite")
    suspend fun updateFavoriteStatus(
        @Path("id") chargerId: String,
        @Query("favorite") isFavorite: Boolean
    ): Charger

    // Charging Slot endpoints
    @GET("chargers/{chargerId}/slots")
    suspend fun getChargingSlots(@Path("chargerId") chargerId: String): List<ChargingSlot>

    @POST("chargers/{chargerId}/slots")
    suspend fun createChargingSlot(
        @Path("chargerId") chargerId: String,
        @Body chargingSlot: ChargingSlot
    ): ChargingSlot

    @PUT("slots/{id}")
    suspend fun updateChargingSlot(
        @Path("id") slotId: String,
        @Body chargingSlot: ChargingSlot
    ): ChargingSlot

    @PUT("slots/{id}/damage")
    suspend fun reportDamage(
        @Path("id") slotId: String,
        @Query("damaged") isDamaged: Boolean
    ): ChargingSlot

    // Nearby Services endpoints
    @GET("chargers/{chargerId}/services")
    suspend fun getNearbyServices(@Path("chargerId") chargerId: String): List<NearbyService>

    @POST("chargers/{chargerId}/services")
    suspend fun addNearbyService(
        @Path("chargerId") chargerId: String,
        @Body service: NearbyService
    ): NearbyService

    @DELETE("services/{id}")
    suspend fun removeNearbyService(@Path("id") serviceId: String)
}