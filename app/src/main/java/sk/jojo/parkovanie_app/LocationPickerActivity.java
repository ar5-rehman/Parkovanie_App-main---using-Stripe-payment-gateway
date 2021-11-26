package sk.jojo.parkovanie_app;

import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolLongClickListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

// classes needed to initialize map
// classes needed to add the location component
// classes needed to add a marker
// classes to calculate a route
// classes needed to launch navigation UI


public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {

    private static final float BASE_CIRCLE_INITIAL_RADIUS = 3.4f;
    private static final float RADIUS_WHEN_CIRCLES_MATCH_ICON_RADIUS = 14f;
    private static final float ZOOM_LEVEL_FOR_START_OF_BASE_CIRCLE_EXPANSION = 11f;
    private static final float ZOOM_LEVEL_FOR_SWITCH_FROM_CIRCLE_TO_ICON = 12f;
    private static final float FINAL_OPACITY_OF_SHADING_CIRCLE = .5f;
    private static final String BASE_CIRCLE_COLOR = "#3BC802";
    private static final String SHADING_CIRCLE_COLOR = "#858585";
    private static final String SOURCE_ID = "SOURCE_ID";
    private static final String ICON_LAYER_ID = "ICON_LAYER_ID";
    private static final String BASE_CIRCLE_LAYER_ID = "BASE_CIRCLE_LAYER_ID";
    private static final String SHADOW_CIRCLE_LAYER_ID = "SHADOW_CIRCLE_LAYER_ID";
    private static final String ICON_IMAGE_ID = "ICON_ID";

    // variables for adding location layer
    private MapView mapView;
    private MapboxMap mapboxMap;
    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private static final String TAG = "LocationPickerActivity";

    // variables needed to initialize navigation
    private Button button;

    private GeoJsonSource source;
    private Point destinationPoint;

    private static final String MAKI_ICON_CAFE = "cafe-15";
    private static final String MAKI_ICON_HARBOR = "harbor-15";
    private static final String MAKI_ICON_AIRPORT = "airport-15";
    private SymbolManager symbolManager;
    private Symbol symbol;
    private Symbol symbol2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_lab_location_picker);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {

        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);


                addDestinationIconSymbolLayer(style);

                source = mapboxMap.getStyle().getSourceAs("destination-source-id");
//                refreshSource();
                mapboxMap.addOnMapClickListener(LocationPickerActivity.this);

                // Set up a SymbolManager instance
                symbolManager = new SymbolManager(mapView, mapboxMap, style);

                symbolManager.create(FeatureCollection.fromFeatures(initFeatureArray()));
                symbolManager.setIconAllowOverlap(true);
                symbolManager.setTextAllowOverlap(true);

// Add symbol at specified lat/lon
                symbol = symbolManager.create(new SymbolOptions()
                        .withLatLng(new LatLng(37.3983133, -122.0854615))
                        .withIconImage(MAKI_ICON_HARBOR)
                        .withIconSize(2.0f)
                        .withDraggable(true));

                symbol = symbolManager.create(new SymbolOptions()
                        .withLatLng(new LatLng(37.4172542,-122.1026262))
                        .withIconImage(MAKI_ICON_HARBOR)
                        .withIconSize(2.0f)
                        .withDraggable(true));

// Add click listener and change the symbol to a cafe icon on click
                symbolManager.addClickListener(new OnSymbolClickListener() {
                    @Override
                    public boolean onAnnotationClick(Symbol symbol) {
                        reverseGeocode(symbol.getGeometry());
                        Toast.makeText(LocationPickerActivity.this,
                                getString(R.string.clicked_symbol_toast), Toast.LENGTH_SHORT).show();
                        symbol.setIconImage(MAKI_ICON_CAFE);
                        symbolManager.update(symbol);
                        return false;
                    }
                });

// Add long click listener and change the symbol to an airport icon on long click
                symbolManager.addLongClickListener((new OnSymbolLongClickListener() {
                    @Override
                    public boolean onAnnotationLongClick(Symbol symbol) {
                        Toast.makeText(LocationPickerActivity.this,
                                getString(R.string.long_clicked_symbol_toast), Toast.LENGTH_SHORT).show();
                        symbol.setIconImage(MAKI_ICON_AIRPORT);
                        symbolManager.update(symbol);
                        return false;
                    }
                }));

                symbolManager.addDragListener(new OnSymbolDragListener() {
                    @Override
// Left empty on purpose
                    public void onAnnotationDragStarted(Symbol annotation) {
                    }

                    @Override
// Left empty on purpose
                    public void onAnnotationDrag(Symbol symbol) {
                    }

                    @Override
// Left empty on purpose
                    public void onAnnotationDragFinished(Symbol annotation) {
                    }
                });
                Toast.makeText(LocationPickerActivity.this,
                        getString(R.string.symbol_listener_instruction_toast), Toast.LENGTH_SHORT).show();


            }
        });
    }

    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    private void refreshSource() {
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
            //Zobrazi viac bodov, ktore su predom definovane
            source.setGeoJson(FeatureCollection.fromFeatures(initFeatureArray()));

        }
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        //Cez toto nastavujes zobrazenie ikony kde clovek klikol
        source = mapboxMap.getStyle().getSourceAs("destination-source-id");
//        if (source != null) {
//            source.setGeoJson(Feature.fromGeometry(destinationPoint));
//            //Zobrazi viac bodov, ktore su predom definovane
//            source.setGeoJson(FeatureCollection.fromFeatures(initFeatureArray()));
//
//        }

        Log.i(TAG, "Latitude " + destinationPoint.latitude() + " Longitude " + destinationPoint.longitude());



        /**
         * Querry to z internetu
         */

//        // Convert LatLng coordinates to screen pixel and only query the rendered features.
//        final PointF pixel = mapboxMap.getProjection().toScreenLocation(point);

//        List<Feature> features = mapboxMap.queryRenderedFeatures(pixel);

//// Get the first feature within the list if one exist
//        if (features.size() > 0) {
//            Feature feature = features.get(0);
//
//// Ensure the feature has properties defined
//            if (feature.properties() != null) {
//                for (Map.Entry<String, JsonElement> entry : feature.properties().entrySet()) {
//// Log all the properties
//                    Log.d(TAG, String.format("%s = %s", entry.getKey(), entry.getValue()));
//                }
//            }
//        }




        reverseGeocode(destinationPoint);


        return handleClickIcon(mapboxMap.getProjection().toScreenLocation(point));
    }

    private boolean handleClickIcon(PointF screenPoint) {
        List<Feature> feature = mapboxMap.queryRenderedFeatures(screenPoint,"destination-symbol-layer-id");
        if (!feature.isEmpty()) {
            Feature[] features = initFeatureArray();
            for(Feature f : features){
                if(f.geometry() == feature.get(0).geometry()){
                    Log.i(TAG,"klikol si spravne");
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Funkcia ktoru som skusal pre appku parkovanie
     * vracia nazov mesta kde klikne clovek
     */
    private void reverseGeocode(final Point point) {
        try {
            MapboxGeocoding client = MapboxGeocoding.builder()
                    .accessToken(getString(R.string.mapbox_access_token))
                    .query(Point.fromLngLat(point.longitude(), point.latitude()))
                    .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                    .build();

            client.enqueueCall(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

                    if (response.body() != null) {
                        List<CarmenFeature> results = response.body().features();
                        if (results.size() > 0) {
                            CarmenFeature feature = results.get(0);
                            Log.i(TAG, feature.placeName());
                            // If the geocoder returns a result, we take the first in the list and show a Toast with the place name.


                        } else {
                            Toast.makeText(LocationPickerActivity.this,
                                    getString(R.string.location_picker_dropped_marker_snippet_no_results), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                    timber.log.Timber.e("Geocoding Failure: %s", throwable.getMessage());
                }
            });
        } catch (ServicesException servicesException) {
            Timber.e("Error geocoding: %s", servicesException.toString());
            servicesException.printStackTrace();
        }
    }

    private Feature[] initFeatureArray() {
        return new Feature[] {
                Feature.fromGeometry(Point.fromLngLat(
                        -122.0854615,37.3983133)),
                Feature.fromGeometry(Point.fromLngLat(
                        -122.1026262,37.4172542)),
                Feature.fromGeometry(Point.fromLngLat(
                        -122.0846489,37.4147256)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.479682,
                        34.698283)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.499368,
                        34.708894)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.469701,
                        34.691089)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.471265,
                        34.672435)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.485418,
                        34.704285)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.493762,
                        34.669337)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.509407,
                        34.696032)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.492719,
                        34.68424)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.51045,
                        34.684133)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.500802,
                        34.700212)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.519576,
                        34.698712)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.502888,
                        34.67888)),
                Feature.fromGeometry(Point.fromLngLat(
                        135.518533,
                        34.67116))
        };
    }



    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            //ziskanie nazvu mesta kde sa nachadza uzivatel
//            assert locationComponent.getLastKnownLocation() != null;
            if (locationComponent.getLastKnownLocation() != null) {
                reverseGeocode(Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(), locationComponent.getLastKnownLocation().getLatitude()));
            }
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}