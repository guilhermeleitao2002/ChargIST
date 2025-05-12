//package pt.ist.cmu.chargist.util
//
//import android.content.Context
//import android.content.Intent
//import android.util.Log
//import androidx.activity.result.contract.ActivityResultContract
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.libraries.places.api.Places
//import com.google.android.libraries.places.api.model.Place
//import com.google.android.libraries.places.api.model.TypeFilter
//import com.google.android.libraries.places.widget.Autocomplete
//import com.google.android.libraries.places.widget.AutocompleteActivity
//import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
//
//class PlaceSearch : ActivityResultContract<Unit, LatLng?>() {
//
//    override fun createIntent(context: Context, input: Unit): Intent {
//        try {
//            // Verify Places API is initialized
//            if (!Places.isInitialized()) {
//                Log.e("PlaceSearch", "Places API is not initialized! Falling back to manual search.")
//                throw IllegalStateException("Places API not initialized")
//            }
//
//            // Define the place fields to return
//            val fields = listOf(
//                Place.Field.ID,
//                Place.Field.NAME,
//                Place.Field.ADDRESS,
//                Place.Field.LAT_LNG
//            )
//
//            // Create autocomplete intent with more options
//            return Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
//                .setTypeFilter(TypeFilter.ADDRESS)
//                .setCountries(listOf("PT")) // Restrict to Portugal
//                .build(context)
//        } catch (e: Exception) {
//            Log.e("PlaceSearch", "Error creating intent: ${e.message}", e)
//            // Instead of returning a dummy intent, throw the exception
//            // so the calling code can handle it properly
//            throw e
//        }
//    }
//
//    override fun parseResult(resultCode: Int, intent: Intent?): LatLng? {
//        Log.d("PlaceSearch", "Parsing result, code: $resultCode")
//        return try {
//            when (resultCode) {
//                AutocompleteActivity.RESULT_OK -> {
//                    val place = Autocomplete.getPlaceFromIntent(intent!!)
//                    Log.d("PlaceSearch", "Selected place: ${place.name}, LatLng: ${place.latLng}")
//                    place.latLng
//                }
//                AutocompleteActivity.RESULT_ERROR -> {
//                    val status = Autocomplete.getStatusFromIntent(intent!!)
//                    Log.e("PlaceSearch", "Error: ${status.statusMessage}")
//                    null
//                }
//                else -> {
//                    Log.d("PlaceSearch", "Selection cancelled")
//                    null
//                }
//            }
//        } catch (e: Exception) {
//            Log.e("PlaceSearch", "Error parsing result: ${e.message}", e)
//            null
//        }
//    }
//}