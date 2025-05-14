package pt.ist.cmu.chargist.data.model

import com.google.android.gms.maps.model.LatLng

data class NearbyPlace(
    val id: String,
    val name: String,
    val placeType: String,
    val distance: Int,
    val latLng: LatLng? = null
)