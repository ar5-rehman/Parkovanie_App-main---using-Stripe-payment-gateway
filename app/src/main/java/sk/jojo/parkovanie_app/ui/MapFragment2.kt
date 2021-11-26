package sk.jojo.parkovanie_app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.layers.TransitionOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import sk.jojo.parkovanie_app.R
import sk.jojo.parkovanie_app.databinding.FragmentMap2Binding
import sk.jojo.parkovanie_app.databinding.FragmentMapBinding
import java.net.URI
import java.net.URISyntaxException


class MapFragment2 : Fragment() {
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(requireActivity(), getString(R.string.mapbox_access_token));

        val binding: FragmentMap2Binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_map2, container, false)

        mapView = binding.mapView2
        mapView?.onCreate(savedInstanceState)

        mapView!!.getMapAsync { map ->
            mapboxMap = map
            map.setStyle(Style.LIGHT, object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {

                    // Disable any type of fading transition when icons collide on the map. This enhances the visual
                    // look of the data clustering together and breaking apart.
                    style.setTransition(TransitionOptions(0, 0, false))
                    mapboxMap!!.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                12.099, -79.045
                            ), 3.0
                        )
                    )
                    addClusteredGeoJsonSource(style)
                    BitmapUtils.getBitmapFromDrawable(resources.getDrawable(R.drawable.ic_cross))?.let {
                        style.addImage(
                            "cross-icon-id",
                            it,
                            true
                        )
                    }
//                    Toast.makeText(
//                        this@CircleLayerClusteringActivity,
//                        R.string.zoom_map_in_and_out_instruction,
//                        Toast.LENGTH_SHORT
//                    ).show()
                }
            })
        }

        return binding.root
    }

    private fun addClusteredGeoJsonSource(loadedMapStyle: Style) {

// Add a new source from the GeoJSON data and set the 'cluster' option to true.
        try {
            loadedMapStyle.addSource( // Point to GeoJSON data. This example visualizes all M1.0+ earthquakes from
                // 12/22/15 to 1/21/16 as logged by USGS' Earthquake hazards program.
                GeoJsonSource(
                    "earthquakes",
                    URI("https://www.mapbox.com/mapbox-gl-js/assets/earthquakes.geojson"),
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                )
            )
        } catch (uriSyntaxException: URISyntaxException) {
//            Timber.e("Check the URL %s", uriSyntaxException.getMessage())
        }

//Creating a marker layer for single data points
        val unclustered = SymbolLayer("unclustered-points", "earthquakes")
        unclustered.setProperties(
            iconImage("cross-icon-id"),
            iconSize(
                division(
                    get("mag"), literal(4.0f)
                )
            ),
            iconColor(
                interpolate(
                    exponential(1), get("mag"),
                    stop(2.0, rgb(0, 255, 0)),
                    stop(4.5, rgb(0, 0, 255)),
                    stop(7.0, rgb(255, 0, 0))
                )
            )
        )
        unclustered.setFilter(has("mag"))
        loadedMapStyle.addLayer(unclustered)

// Use the earthquakes GeoJSON source to create three layers: One layer for each cluster category.
// Each point range gets a different fill color.
        val layers = arrayOf(
            intArrayOf(150, ContextCompat.getColor(requireActivity(), R.color.mapboxRed)),
            intArrayOf(20, ContextCompat.getColor(requireActivity(), R.color.mapboxGreen)),
            intArrayOf(0, ContextCompat.getColor(requireActivity(), R.color.mapbox_blue))
        )
        for (i in layers.indices) {
//Add clusters' circles
            val circles = CircleLayer("cluster-$i", "earthquakes")
            circles.setProperties(
                circleColor(layers[i][1]),
                circleRadius(18f)
            )
            val pointCount: Expression = toNumber(get("point_count"))

// Add a filter to the cluster layer that hides the circles based on "point_count"
            circles.setFilter(
                if (i == 0) all(
                    has("point_count"),
                    gte(pointCount, literal(layers[i][0]))
                ) else all(
                    has("point_count"),
                    gte(pointCount, literal(layers[i][0])),
                    lt(pointCount, literal(layers[i - 1][0]))
                )
            )
            loadedMapStyle.addLayer(circles)
        }

//Add the count labels
        val count = SymbolLayer("count", "earthquakes")
        count.setProperties(
            textField(Expression.toString(get("point_count"))),
            textSize(12f),
            textColor(Color.WHITE),
            textIgnorePlacement(true),
            textAllowOverlap(true)
        )
        loadedMapStyle.addLayer(count)
    }




}