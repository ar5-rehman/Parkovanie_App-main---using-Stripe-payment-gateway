package sk.jojo.parkovanie_app.map_tools



import android.util.Log

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.core.exceptions.ServicesException

import com.mapbox.mapboxsdk.geometry.LatLng
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import timber.log.Timber

import kotlin.collections.ArrayList


class MapDatabase {
    private val TAG = "MapDatabase"

    private var auth: FirebaseAuth
    private var currentUser: FirebaseUser
    private var db: FirebaseFirestore

    var coordinatesEmpty: ArrayList<LatLng> = ArrayList()
        private set

    var coordinatesFull: ArrayList<LatLng> = ArrayList()
        private set

    init{

        // Initialize Firebase Auth
        auth = Firebase.auth
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        currentUser = auth.currentUser
        db = FirebaseFirestore.getInstance()

    }

//    fun getCoordinates(town: String): ArrayList<LatLng> {
//        var coordinates = ArrayList<LatLng>()
//
//        db.collection(town).get()
//            .addOnSuccessListener { documents ->
//                    for(document in documents){
//
//                        if(document.getBoolean("isFull") == false){
//                            Log.d(TAG, "${document.id} => ${document.data}")
//                            document.getGeoPoint("coordinates")?.let { LatLng(it.latitude,
//                                document.getGeoPoint("coordinates")!!.longitude) }
//                                ?.let { coordinates.add(it) }
//                        }
//                    }
//            }
//        return coordinates
//    }

    fun getCoordinates(town: String){
        db.collection(town).get()
            .addOnSuccessListener { documents ->
                for(document in documents){
                    Log.d(TAG, "${document.id} => ${document.data}")
                    requestGeocode(document.id, document.getBoolean("isFull")!!)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting history_of_reservation documents: ", exception)
            }
    }

    private fun requestGeocode(address: String, isFull: Boolean){
        try {
            val mapboxGeocoding = MapboxGeocoding.builder()
                .accessToken("sk.eyJ1IjoiajBobm55ayIsImEiOiJja29yZnA4NmsxNGxuMm9zajZmc3M1YnhhIn0.4W_1LQSZ-NW3Pj6nZQzaWw")
                .query(address)
                .build()
            mapboxGeocoding.enqueueCall(object : Callback<GeocodingResponse> {
                override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {

                    val results = response.body()!!.features()

                    if (results.size > 0) {
                        // Log the first results Point.
                        val firstResultPoint = results[0].center()
                        Log.d(TAG, "onResponse: " + firstResultPoint!!.toString())

                        if(isFull){
                            coordinatesFull.add(LatLng(firstResultPoint.latitude(),firstResultPoint.longitude()))
                        }
                        else{
                            coordinatesEmpty.add(LatLng(firstResultPoint.latitude(),firstResultPoint.longitude()))
                        }


                    } else {

                        // No result for your request were found.
                        Log.d(TAG, "onResponse: No result found")

                    }
                }

                override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
                    throwable.printStackTrace()
                }
            })
        } catch (servicesException: ServicesException) {
            Timber.e("Error geocoding: %s", servicesException.toString())
            servicesException.printStackTrace()
        }

    }




}

