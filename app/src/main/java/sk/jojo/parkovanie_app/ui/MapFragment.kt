package sk.jojo.parkovanie_app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sk.jojo.parkovanie_app.R
import sk.jojo.parkovanie_app.databinding.FragmentMapBinding
import sk.jojo.parkovanie_app.view_model.IdNumberViewModel
import sk.jojo.parkovanie_app.view_model.MainViewModel
import sk.jojo.parkovanie_app.view_model.PriceViewModel
import timber.log.Timber


class MapFragment : Fragment(), OnMapReadyCallback, PermissionsListener{

    // variables for adding location layer
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null

    // variables for adding location layer
    private var permissionsManager: PermissionsManager? = null
    private var locationComponent: LocationComponent? = null
    private lateinit var binding: FragmentMapBinding

    private val TAG = "MapFragment"

    private val RED_MARKER_ICON_ID = "RED_MARKER_ICON_ID"
    private val GREEN_MARKER_ICON_ID = "GREEN_MARKER_ICON_ID"
    private var symbolManagerEmpty: SymbolManager? = null
    private var symbolManagerFull: SymbolManager? = null
    private var symbol: Symbol? = null
    private var address: String = ""
    private var city: String = ""


    private lateinit var db: FirebaseFirestore

    private lateinit var mainViewModel: MainViewModel
    private lateinit var idNumberViewModel: IdNumberViewModel
    private lateinit var priceViewModel: PriceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Mapbox.getInstance(requireActivity(), getString(R.string.mapbox_access_token))

        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        idNumberViewModel = ViewModelProvider(requireActivity()).get(IdNumberViewModel::class.java)
        priceViewModel = ViewModelProvider(requireActivity()).get(PriceViewModel::class.java)

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false)


        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync (this)

        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onMapReady(mapboxMap: MapboxMap) {

        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
            Style.MAPBOX_STREETS
        ) { style ->
            enableLocationComponent(style)

            //Nastavovanie farebnych gulocok
            BitmapUtils.getBitmapFromDrawable(getResources().getDrawable(R.drawable.ic_red_circle))
                ?.let { style.addImage(RED_MARKER_ICON_ID, it) }

            BitmapUtils.getBitmapFromDrawable(getResources().getDrawable(R.drawable.ic_green_circle))
                ?.let { style.addImage(GREEN_MARKER_ICON_ID, it) }

            // Set up a SymbolManager instance
            symbolManagerEmpty = SymbolManager(mapView!!, mapboxMap, style)
            symbolManagerEmpty!!.setIconAllowOverlap(true)
            symbolManagerEmpty!!.setTextAllowOverlap(true)

            symbolManagerFull = SymbolManager(mapView!!, mapboxMap, style)
            symbolManagerFull!!.setIconAllowOverlap(true)
            symbolManagerFull!!.setTextAllowOverlap(true)

            // Add click listener to empty parking
            symbolManagerEmpty!!.addClickListener(OnSymbolClickListener { symbol ->
                reverseGeocode(symbol.geometry)

                mainViewModel.isTimerStarted.observe(viewLifecycleOwner, Observer {
                    if(it){
                        view?.findNavController()?.navigate(
                            MapFragmentDirections.actionMapFragmentToSecondBuyingTicketFragment()
                        )
                    }
                    else{
                        view?.findNavController()?.navigate(
                            MapFragmentDirections.actionMapFragmentToBuyingTicketFragment()
                        )
                    }
                })

                false
            })

            // Add click listener to full parking
            symbolManagerFull!!.addClickListener(OnSymbolClickListener { symbol ->
                reverseGeocode(symbol.geometry)

                val builder = AlertDialog.Builder(context)
                //set title for alert dialog
                builder.setTitle("POZOR")
                //set message for alert dialog
                builder.setMessage("Zvolil si parkovisko, ktoré môže byť plné. Si si istý voľbou?")
                builder.setIcon(R.drawable.ic_alert)

                //performing positive action
                builder.setPositiveButton("Áno"){dialogInterface, which ->
                    mainViewModel.isTimerStarted.observe(viewLifecycleOwner, Observer {
                        if(it){
                            view?.findNavController()?.navigate(
                                MapFragmentDirections.actionMapFragmentToSecondBuyingTicketFragment()
                            )
                        }
                        else{
                            view?.findNavController()?.navigate(
                                MapFragmentDirections.actionMapFragmentToBuyingTicketFragment()
                            )
                        }
                    })
                }
                //performing negative action
                builder.setNegativeButton("Nie"){dialogInterface, which ->
                    Toast.makeText(context,"Vyber si parkovisko",Toast.LENGTH_LONG).show()
                }
                // Create the AlertDialog
                val alertDialog: AlertDialog = builder.create()
                // Set other dialog properties
                alertDialog.setCancelable(false)
                alertDialog.show()

                false
            })

        }
    }

    private fun addEmptyParking(latLng: LatLng){
        symbol = symbolManagerEmpty!!.create(
                SymbolOptions()
                    .withLatLng(LatLng(latLng.latitude, latLng.longitude))
                    .withIconImage(GREEN_MARKER_ICON_ID)
                    .withIconSize(2.0f)
                    .withDraggable(false)
            )
    }

    private fun addFullParking(latLng: LatLng){
        symbol = symbolManagerFull!!.create(
            SymbolOptions()
                .withLatLng(LatLng(latLng.latitude, latLng.longitude))
                .withIconImage(RED_MARKER_ICON_ID)
                .withIconSize(2.0f)
                .withDraggable(false)
        )
    }

    /**
     * Funkcia ktoru som skusal pre appku parkovanie
     * vracia nazov mesta kde klikne clovek
     */
    private fun reverseGeocode(point: Point) {
        try {
            val client = MapboxGeocoding.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .query(Point.fromLngLat(point.longitude(), point.latitude()))
                .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                .build()
            client.enqueueCall(object : Callback<GeocodingResponse?> {
                override fun onResponse(
                    call: Call<GeocodingResponse?>,
                    response: Response<GeocodingResponse?>
                ) {
                    if (response.body() != null) {
                        val results = response.body()!!.features()
                        if (results.size > 0) {
                            val feature = results[0]
                            Log.i(TAG, feature.placeName()!!)
                            Log.i(TAG, "Latitude " + point.latitude() + " Longitude " + point.longitude())
//                            Log.i(TAG, feature.address()!!)
                            Log.i(TAG, feature.text()!!)
//                            Log.i(TAG,"${feature.address()!!} ${feature.text()!!}")

                            val str = feature.placeName().toString().split(", ")
                            city = str.get(1)
                            Log.i(TAG,city)
                            if(feature.address() == null){
                                address = "${feature.text()}"
                                Log.i(TAG,"${address}")
                            }
                            else{
                                address = "${feature.address()} ${feature.text()}"
                                Log.i(TAG,"${address}")
                            }

                            mainViewModel.setAddress(address)
                            mainViewModel.setCity(city)

                            priceViewModel.setAddress(address)
                            priceViewModel.setCity(city)
                            priceViewModel.readPriceFromDB()

                            idNumberViewModel.setAddress(address)
                            idNumberViewModel.setCity(city)
                            idNumberViewModel.readFromDB()

//                            priceViewModel.setAddress(address)
//                            priceViewModel.setCity(city)
//                            priceViewModel.readPriceFromDB(city, address)

                            // If the geocoder returns a result, we take the first in the list and show a Toast with the place name.
                        } else {
                            Toast.makeText(
                                context,
                                getString(R.string.location_picker_dropped_marker_snippet_no_results),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                override fun onFailure(call: Call<GeocodingResponse?>, throwable: Throwable) {
                    Timber.e("Geocoding Failure: %s", throwable.message)
                }
            })
        } catch (servicesException: ServicesException) {
            Timber.e("Error geocoding: %s", servicesException.toString())
            servicesException.printStackTrace()
        }
    }

    private fun reverseGeocodeToCity(point: Point) {
        try {
            val client = MapboxGeocoding.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .query(Point.fromLngLat(point.longitude(), point.latitude()))
                .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                .build()
            client.enqueueCall(object : Callback<GeocodingResponse?> {
                override fun onResponse(
                    call: Call<GeocodingResponse?>,
                    response: Response<GeocodingResponse?>
                ) {
                    if (response.body() != null) {
                        val results = response.body()!!.features()
                        if (results.size > 0) {
                            val feature = results[0]
                            Log.i(TAG, feature.placeName()!!)
                            Log.i(TAG, feature.address()!!)
                            Log.i(TAG, feature.text()!!)
                            Log.i(TAG,"${feature.address()!!} ${feature.text()!!}")

                            val str = feature.placeName().toString().split(", ")
                            city = str.get(1)
                            Log.i(TAG,city)

                            getCoordinates(city)
                        } else {
                            Toast.makeText(
                                context,
                                getString(R.string.location_picker_dropped_marker_snippet_no_results),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                override fun onFailure(call: Call<GeocodingResponse?>, throwable: Throwable) {
                    Timber.e("Geocoding Failure: %s", throwable.message)
                }
            })
        } catch (servicesException: ServicesException) {
            Timber.e("Error geocoding: %s", servicesException.toString())
            servicesException.printStackTrace()
        }
    }

//    private fun requestGeocode(address: String, isFull: Boolean) {
//        try {
//            val mapboxGeocoding = MapboxGeocoding.builder()
//                .accessToken(getString(R.string.mapbox_access_token))
//                .query(address)
//                .build()
//            mapboxGeocoding.enqueueCall(object : Callback<GeocodingResponse> {
//                override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
//
//                    val results = response.body()!!.features()
//
//                    if (results.size > 0) {
//                        // Log the first results Point.
//                        val firstResultPoint = results[0].center()
//                        Log.d(TAG, "onResponse: " + firstResultPoint!!.toString())
//                        if(isFull){
//                            val latLng = LatLng(firstResultPoint.latitude(),firstResultPoint.longitude())
//                            addFullParking(latLng)
//                        }
//                        else{
//                            val latLng = LatLng(firstResultPoint.latitude(),firstResultPoint.longitude())
//                            addEmptyParking(latLng)
//                        }
//                    } else {
//                        // No result for your request were found.
//                        Log.d(TAG, "onResponse: No result found")
//
//                    }
//                }
//                override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
//                    throwable.printStackTrace()
//                }
//            })
//        } catch (servicesException: ServicesException) {
//            Timber.e("Error geocoding: %s", servicesException.toString())
//            servicesException.printStackTrace()
//        }
//    }


    fun getCoordinates(town: String){
        db.collection(town).get()
            .addOnSuccessListener { documents ->
                for(document in documents){
                    Log.d(TAG, "${document.id} => ${document.data}")
                    if(document.getBoolean("isFull") == false){
                            Log.d(TAG, "${document.id} => ${document.data}")
                            document.getGeoPoint("coordinates")?.let { LatLng(it.latitude,
                                document.getGeoPoint("coordinates")!!.longitude) }
                                ?.let { addEmptyParking(it) }
                    }
                    else{
                        Log.d(TAG, "${document.id} => ${document.data}")
                        document.getGeoPoint("coordinates")?.let { LatLng(it.latitude,
                            document.getGeoPoint("coordinates")!!.longitude) }
                            ?.let { addFullParking(it) }
                    }

//                    requestGeocode("${document.id},${town}", document.getBoolean("isFull")!!)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting history_of_reservation documents: ", exception)
            }
    }





    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap!!.locationComponent
            context?.let { locationComponent!!.activateLocationComponent(it, loadedMapStyle) }
            // Activate with a built LocationComponentActivationOptions object
            // Activate with a built LocationComponentActivationOptions object
            locationComponent?.activateLocationComponent(LocationComponentActivationOptions.builder(requireContext(), loadedMapStyle).build())
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            locationComponent?.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent!!.setCameraMode(CameraMode.TRACKING_COMPASS)
            // Set the component's render mode
            locationComponent?.renderMode = RenderMode.COMPASS

            //ziskanie nazvu mesta kde sa nachadza uzivatel
            assert(locationComponent!!.lastKnownLocation != null)
            reverseGeocodeToCity(
                Point.fromLngLat(
                    locationComponent!!.getLastKnownLocation()!!.longitude,
                    locationComponent!!.getLastKnownLocation()!!
                        .latitude
                )
            )

            binding.findMeBtn.setOnClickListener {
                val lastKnownLocation: Location? = mapboxMap!!.locationComponent.lastKnownLocation
                if (lastKnownLocation != null) {

                    val position = CameraPosition.Builder()
                        .target(
                            LatLng(
                                lastKnownLocation.getLatitude(),
                                lastKnownLocation.getLongitude()
                            )
                        ) // Sets the new camera position
                        .zoom(13.5)
                        .bearing(0.0)
                        .tilt(0.0)
                        .build(); // Creates a CameraPosition from the builder

                    mapboxMap!!.animateCamera(
                        CameraUpdateFactory
                            .newCameraPosition(position), 7000
                    );
                }
            }

        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(requireActivity())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String?>?) {
        Toast.makeText(context, R.string.user_location_permission_explanation, Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap!!.style!!)
        } else {
            Toast.makeText(context, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG)
                .show()
            requireActivity().finish()
        }
    }
}