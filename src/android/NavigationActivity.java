package com.vahn.cordova.mpbxnavigator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyBearingTracking;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.TrackingSettings;
import com.mapbox.services.Constants;
import com.vahn.cordova.mpbxnavigator.Utils;
import com.mapbox.services.android.navigation.v5.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.NavigationConstants;
import com.mapbox.services.android.navigation.v5.RouteProgress;
import com.mapbox.services.android.navigation.v5.listeners.AlertLevelChangeListener;
import com.mapbox.services.android.navigation.v5.listeners.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.listeners.OffRouteListener;
import com.mapbox.services.android.navigation.v5.listeners.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.models.RouteStepProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.directions.v5.models.StepManeuver;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.HTTP;
import timber.log.Timber;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnMapClickListener, ProgressChangeListener, NavigationEventListener, AlertLevelChangeListener,
        OffRouteListener {

    // Map variables
    private MapView mapView;
    private Polyline routeLine;
    private MapboxMap mapboxMap;
    private Marker destinationMarker;

    // Navigation related variables
    private LocationEngine locationEngine;
    private MapboxNavigation navigation;
    private Button startRouteButton;
    private DirectionsRoute route;
    private Position destination;
    public double tmp_bearing;
    private double destLatitude = 41.9107038;
    private double destLongitude =  12.476357900000039;
    public static final int LOCATION_PERMISSION_REQUEST = 20;
    public boolean permissionGranted = false;
    private boolean running = false;
    Location userLocation;
    CameraPosition.Builder cameraPositionBuilder;

    private TextView totalTime;
    private TextView totalMeters;
    private TextView nextStepMeters;
    private TextView nextStepAddress;
    private TextView closeNavigator;
    private ImageView nextDirectionArrow;
    private View followUserButton;
    private TextToSpeech voice;
    private String toSpeak = "";
    private List<LegStep> routeSteps;
    private boolean justStarted = true;
    private boolean apiMoreThanLollipop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            apiMoreThanLollipop = true;
        } else {
            apiMoreThanLollipop = false;
        }

        mapView = (MapView) findViewById(R.id.mapView);
        totalTime = (TextView) findViewById(R.id.totalTime);
        totalMeters = (TextView) findViewById(R.id.totalMeters);
        nextStepMeters = (TextView) findViewById(R.id.nextStepMeters);
        nextStepAddress = (TextView) findViewById(R.id.nextStepAddress);
        closeNavigator = (TextView) findViewById(R.id.closeNavigator);
        nextDirectionArrow = (ImageView) findViewById(R.id.directionArrow);
        followUserButton = (View) findViewById(R.id.followUserButton);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        navigation = new MapboxNavigation(this, "pk.eyJ1IjoibmV4dGFkdiIsImEiOiJjajNvNXRjMjQwMDFhMzRydGMwMzg0djYwIn0.xEk-wANTrthLdtsavpuU0g");
        navigation.addNavigationEventListener(NavigationActivity.this);
        navigation.addProgressChangeListener(NavigationActivity.this);
        navigation.addAlertLevelChangeListener(NavigationActivity.this);

        Utils.initializeTranslationArray();

        voice = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    voice.setLanguage(Locale.ITALIAN);
                }
            }
        });

        closeNavigator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        mapboxMap.getUiSettings().setAllGesturesEnabled(true);
        mapboxMap.setOnMapClickListener(this);
        //Snackbar.make(mapView, "Tap map to place destination", BaseTransientBottomBar.LENGTH_LONG).show();


        cameraPositionBuilder = new CameraPosition.Builder();
        cameraPositionBuilder.tilt(45);
        cameraPositionBuilder.zoom(16);
        mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPositionBuilder.build()));

        locationEngine = LocationSource.getLocationEngine(this);
        mapboxMap.setLocationSource(locationEngine);
        int[] paddings = { 0,0,0,0 };

        Drawable bgindicator = ContextCompat.getDrawable(this, R.drawable.circled_indicator);
        Drawable fgindicator = ContextCompat.getDrawable(this, R.drawable.ic_navigation_blue_24dp);

        mapboxMap.getMyLocationViewSettings().setBackgroundDrawable(bgindicator, paddings);
        mapboxMap.getMyLocationViewSettings().setForegroundDrawable(fgindicator,fgindicator);
        mapboxMap.getMyLocationViewSettings().setTilt(45);


        destination = Position.fromCoordinates(destLongitude, destLatitude);
        LatLng destLatLng = new LatLng(destLatitude, destLongitude);


        destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(destLatLng));

        followUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                followTheUser();
            }
        });

        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            mapboxMap.setMyLocationEnabled(true);


            mapboxMap.setOnMyLocationTrackingModeChangeListener(new MapboxMap.OnMyLocationTrackingModeChangeListener() {
                @Override
                public void onMyLocationTrackingModeChange(int myLocationTrackingMode) {
                    Log.d("TRACKING", String.valueOf(myLocationTrackingMode));

                    switch (myLocationTrackingMode){
                        case MyLocationTracking.TRACKING_FOLLOW:
                            Log.d("TRACKING", "sono in follow");
                            followUserButton.setVisibility(View.INVISIBLE);
                            break;
                        case MyLocationTracking.TRACKING_NONE:
                            Log.d("TRACKING", "NON sono in follow");
                            followUserButton.setVisibility(View.VISIBLE);

                    }
                }
            });
            locationEngine.setInterval(0);
            locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
            locationEngine.setFastestInterval(1000);
            locationEngine.activate();
            locationEngine.addLocationEngineListener(new LocationEngineListener() {
                @Override
                public void onConnected() {

                }

                @Override
                public void onLocationChanged(Location location) {
                    Log.d("NAV", "location arrived "+location.toString());
                    userLocation = location;
                    locationEngine.removeLocationEngineListener(this);

                    calculateRoute();
                }
            });


        }








    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
/*        if (destinationMarker != null) {
            mapboxMap.removeMarker(destinationMarker);
        }
        destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(point));

        startRouteButton.setVisibility(View.VISIBLE);

        this.destination = Position.fromCoordinates(point.getLongitude(), point.getLatitude());
        calculateRoute();*/
    }

    private void drawRouteLine(DirectionsRoute route) {
        List<Position> positions = LineString.fromPolyline(route.getGeometry(), Constants.PRECISION_6).getCoordinates();
        List<LatLng> latLngs = new ArrayList<LatLng>();
        for (Position position : positions) {
            latLngs.add(new LatLng(position.getLatitude(), position.getLongitude()));
        }

        // Remove old route if currently being shown on map.
        if (routeLine != null) {
            mapboxMap.removePolyline(routeLine);
        }

        routeLine = mapboxMap.addPolyline(new PolylineOptions()
                .addAll(latLngs)
                .color(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .width(5f));
    }

    private void calculateRoute() {

        Log.d("NAV", userLocation.toString());

        if (userLocation == null) {
            Location userLocation = mapboxMap.getMyLocation();
            Timber.d("calculateRoute: User location is null, therefore, origin can't be set.");
            return;
        }



        Log.d("NAV", "calculatin route");
        Position origin = (Position.fromCoordinates(userLocation.getLongitude(), userLocation.getLatitude()));
        if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
            mapboxMap.removeMarker(destinationMarker);
            //startRouteButton.setVisibility(View.GONE);
            return;
        }

        navigation.getRoute(origin, destination, new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if(response.body() != null ) {
                    Log.d("NAV", "response body is not null");
                    DirectionsRoute route = response.body().getRoutes().get(0);
                    RouteLeg routeLeg = response.body().getRoutes().get(0).getLegs().get(0);
                    routeSteps = routeLeg.getSteps();
                    if(justStarted) {
                        showNextStepInfo(routeSteps.get(0));
                        justStarted = false;
                    }

                    NavigationActivity.this.route = route;
                    drawRouteLine(route);
                    if(!running) {
                        followTheUser();
                        startNavigation();

                    }

                } else {
                    Log.d("NAV", "response body null");
                }
                // Attach all of our navigation listeners.
                // Adjust location engine to force a gps reading every second. This isn't required but gives an overall
                // better navigation experience for users. The updating only occurs if the user moves 3 meters or further
                // from the last update.

            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Timber.e("onFailure: navigation.getRoute()", throwable);
            }
        });
    }

  /*
   * Navigation listeners
   */

    @Override
    public void onRunning(boolean running) {
        this.running = running;
        if (running) {
            Timber.d("onRunning: Started");

        } else {
            Timber.d("onRunning: Stopped");
        }
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        Timber.d("onProgressChange: fraction of route traveled: %f", routeProgress.getFractionTraveled());

        int currentLegIndex = routeProgress.legIndex();
        LegStep step = routeProgress.getCurrentLeg().getSteps().get(0);


        showDurationsAndDistance(routeProgress.getDurationRemaining(), routeProgress.getDistanceRemaining());

    }

    @Override
    public void onAlertLevelChange(int alertLevel, RouteProgress routeProgress) {


        LegStep step =  routeProgress.getRoute().getLegs().get(0).getSteps().get(0);


        switch (alertLevel) {
            case NavigationConstants.HIGH_ALERT_LEVEL:
                showNextStepInfo(step);
                //Toast.makeText(MockNavigationActivity.this, "HIGH", Toast.LENGTH_LONG).show();
                break;
            case NavigationConstants.MEDIUM_ALERT_LEVEL:
                showNextStepInfo(step);
                //Toast.makeText(MockNavigationActivity.this, "MEDIUM", Toast.LENGTH_LONG).show();
                break;
            case NavigationConstants.LOW_ALERT_LEVEL:
                showNextStepInfo(step);
                //Toast.makeText(MockNavigationActivity.this, "LOW", Toast.LENGTH_LONG).show();
                break;
            case NavigationConstants.ARRIVE_ALERT_LEVEL:
                showNextStepInfo(step);
                //Toast.makeText(MockNavigationActivity.this, "ARRIVE", Toast.LENGTH_LONG).show();
                break;
            case NavigationConstants.DEPART_ALERT_LEVEL:
                showNextStepInfo(step);
                //Toast.makeText(MockNavigationActivity.this, "DEPART", Toast.LENGTH_LONG).show();
                break;
            default:
            case NavigationConstants.NONE_ALERT_LEVEL:
                //Toast.makeText(MockNavigationActivity.this, "NONE", Toast.LENGTH_LONG).show();
                break;
        }



    }

    @Override
    public void userOffRoute(Location location) {
        Position newOrigin = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
        navigation.getRoute(newOrigin, destination, location.getBearing(), new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                DirectionsRoute route = response.body().getRoutes().get(0);
                NavigationActivity.this.route = route;

                // Remove old route line from map and draw the new one.
                if (routeLine != null) {
                    mapboxMap.removePolyline(routeLine);
                }
                drawRouteLine(route);
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Timber.e("onFailure: navigation.getRoute()", throwable);
            }
        });
    }

  /*
   * Activity lifecycle methods
   */

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        navigation.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        navigation.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        justStarted = true;
        // Remove all navigation listeners
        navigation.removeAlertLevelChangeListener(this);
        navigation.removeNavigationEventListener(this);
        navigation.removeProgressChangeListener(this);
        navigation.removeOffRouteListener(this);

        // End the navigation session
        navigation.endNavigation();

        voice.shutdown();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    public void startNavigation() {

        navigation.setLocationEngine(locationEngine);
        navigation.startNavigation(route);
        mapboxMap.getTrackingSettings().setDismissAllTrackingOnGesture(true);
        showDurationsAndDistance(route.getDuration(), route.getDistance());
    }


    public void showNextStepInfo(LegStep step){

        if(step == null) {
            nextStepMeters.setVisibility(View.INVISIBLE);
            nextStepAddress.setVisibility(View.INVISIBLE);
            nextDirectionArrow.setVisibility(View.INVISIBLE);
            return;
        } else {
            nextStepMeters.setVisibility(View.VISIBLE);
            nextStepAddress.setVisibility(View.VISIBLE);
            nextDirectionArrow.setVisibility(View.VISIBLE);
        }

        //toSpeak = step.getManeuver().getInstruction();
        speak(step);

        String distance = "";
        String distanceUnit = "";

        double meters = step.getDistance();
        if(meters > 1000) {
            distance = String.valueOf((int) meters/1000);
            distanceUnit = "km";
        } else {
            distance = String.valueOf(meters);
            distanceUnit = "m";
        }
            nextStepMeters.setText(distance+" "+distanceUnit);
            nextStepAddress.setText(step.getName());

            StepManeuver maneuver = step.getManeuver();
            String type = maneuver.getType();
            String modifier = maneuver.getModifier();
            type = type.replaceAll(" ", "_").toLowerCase();
            modifier = modifier.replaceAll(" ", "_").toLowerCase();
            Log.d("ARROW", "direction"+"_"+type+"_"+modifier);
            Log.d("ADDRESS", step.getName());
            int drawableId = this.getResources().getIdentifier("direction"+"_"+type+"_"+modifier, "drawable", this.getPackageName());
            Drawable arrow = ContextCompat.getDrawable(this, drawableId);
            nextDirectionArrow.setImageDrawable(arrow);
    }

    public void showDurationsAndDistance(double secs, double meters){

        if(secs == 0) {
            return;
        }
        if(meters == 0) {
            return;
        }

        totalTime.setVisibility(View.VISIBLE);
        totalMeters.setVisibility(View.VISIBLE);
        String time= "";
        String timeUnit = "";
        String distance = "";
        String distanceUnit = "";

        long seconds = Double.valueOf(secs).longValue();
        long mins = TimeUnit.SECONDS.toMinutes(seconds);
        long hours = 0;

        if(mins >= 60) {
            time =  String.valueOf(TimeUnit.SECONDS.toHours(seconds));
            timeUnit = "ore";
        } else if(mins > 0 && mins < 60) {
            timeUnit = "min";
            time = String.valueOf(mins);
        } else if(mins <= 0) {
            timeUnit = "sec";
            time = String.valueOf(seconds);
        }

        if(meters > 1000) {
            distance = String.valueOf((int) meters/1000);
            distanceUnit = "km";
        } else {
            distance = String.valueOf(meters);
            distanceUnit = "m";
        }
        totalTime.setText(time +" "+timeUnit);
        totalMeters.setText(distance +" "+distanceUnit);

        Log.d("NAV","tempo: "+secs);
        Log.d("NAV","str: "+ timeUnit);
        Log.d("NAV","metri: "+String.valueOf(meters));
    }

    public void followTheUser() {

        cameraPositionBuilder = new CameraPosition.Builder();
        cameraPositionBuilder.tilt(45);
        cameraPositionBuilder.zoom(16);
        if(userLocation != null) {
            cameraPositionBuilder.target(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()));
        }
        mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(cameraPositionBuilder.build()), 1000, new MapboxMap.CancelableCallback() {
            @Override
            public void onCancel() {

            }

            @Override
            public void onFinish() {
                mapboxMap.getTrackingSettings().setMyBearingTrackingMode(MyBearingTracking.GPS);
                mapboxMap.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
            }
        });

/*        mapboxMap.easeCamera(
                CameraUpdateFactory.newCameraPosition
                        (cameraPositionBuilder.target(latLng).build()
                        )
        );*/
    }


    public void speak(LegStep step) {
        String toSpeak = Utils.translate(step);
        Log.d("VOICE TRANSLATED", toSpeak);
        if(!toSpeak.equals("")) {
            if(apiMoreThanLollipop) {
                voice.speak(toSpeak, TextToSpeech.QUEUE_ADD, new Bundle(), null);
            } else {
                voice.speak(toSpeak, TextToSpeech.QUEUE_ADD, null);
            }
        }
    }

}
