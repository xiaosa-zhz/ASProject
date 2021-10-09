package com.example.geojsonwithosmdroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.MultiPolygon;
import com.cocoahero.android.geojson.Polygon;
import com.cocoahero.android.geojson.Position;
import com.cocoahero.android.geojson.Ring;
import com.example.geojsonwithosmdroid.util.AudioManager;
import com.example.geojsonwithosmdroid.util.LocationTrack;
import com.example.geojsonwithosmdroid.util.NavigationManager;
import com.example.geojsonwithosmdroid.util.RailwayJSONParser;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase sqLiteDatabase;
    boolean NonStartAndEndStationIsVisible;
    LocationTrack locationTrackService;
    NavigationManager navigationManager;
    AudioManager audioManager;
    MainActivity mainThis;

    private void addOverlays(List<?> overlays)
    {
        for(Object overlay : overlays)
        {
            map.getOverlayManager().add((Overlay) overlay);
        }
    }

    @Deprecated
    private GeoJSONObject getGeoJSONFromRawData(@RawRes int id)
    {
        //从json文件获取地图信息
        InputStream file_in = getResources().openRawResource(id);
        FeatureCollection geoJSON = null;
        try {
            geoJSON = (FeatureCollection) GeoJSON.parse(file_in);
            file_in.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return geoJSON;
    }

    @Deprecated
    private List<org.osmdroid.views.overlay.Polygon> transToOsmPolygonsFromGeoJSON(GeoJSONObject geoJSON)
    {
        //使用到的GeoJSON对象的结构：
        //FeatureCollection : {
        //    [
        //        Feature : {
        //            Properties : { ... }
        //            MultiPolygon : [
        //                    Polygon : [
        //                        Ring : [
        //                            Position : { Latitude, Longtitude }
        //                            ...
        //                        ]
        //                        ...
        //                    ]
        //                    ...
        //                ]
        //            }
        //        }
        //        ...
        //    ]
        //}
        //每个Polygon里可以有多个Ring，不包含洞的情况下只有一个Ring
        //每个Position可以包括经度纬度和高度三个数据，高度数据是可选的
        //由于区域不具有洞，GeoJSON的Polygon等价于Ring，可以输出为一个osmdroid的Polygon
        //为每个GeoJSON.Polygon创建一个osmdroid.Polygon，组成一个List<osmdroid.Polygon>
        //Polygon需要用一个List<GeoPoint>初始化
        Vector<Integer> colors = new Vector<>();
        colors.add(Color.BLUE);
        colors.add(Color.WHITE);
        colors.add(Color.BLACK);
        colors.add(Color.CYAN);
        colors.add(Color.DKGRAY);
        colors.add(Color.GREEN);
        colors.add(Color.RED);
        colors.add(Color.MAGENTA);
        colors.add(Color.YELLOW);
        colors.add(Color.LTGRAY);
        List<org.osmdroid.views.overlay.Polygon> Polygons = new ArrayList<>();
        FeatureCollection features = (FeatureCollection) geoJSON;
        List<Feature> featureList = features.getFeatures();
        int count = 0;
        for(Feature feature : featureList)
        {
            MultiPolygon geometry = (MultiPolygon) feature.getGeometry();
            List<Polygon> subGeometries = geometry.getPolygons();
            JSONObject properties = feature.getProperties();
            for(Polygon subGeometry : subGeometries)
            {
                org.osmdroid.views.overlay.Polygon polygon = new org.osmdroid.views.overlay.Polygon();
                List<GeoPoint> geoPoints = new ArrayList<>();
                List<Ring> rings = subGeometry.getRings();
                for(Ring ring : rings)
                {
                    List<Position> positions = ring.getPositions();
                    for(Position position : positions)
                    {
                        geoPoints.add(new GeoPoint(position.getLatitude(), position.getLongitude()));
                    }
                }
                //force to close
                geoPoints.add(geoPoints.get(0));
                polygon.setPoints(geoPoints);
                //deal with features
                polygon.getFillPaint().setColor(colors.get(count % colors.size()));
                polygon.setRelatedObject(properties);
                polygon.setOnClickListener(new org.osmdroid.views.overlay.Polygon.OnClickListener() {
                    @Override
                    public boolean onClick(org.osmdroid.views.overlay.Polygon polygon, MapView mapView, GeoPoint eventPos) {
                        JSONObject properties = (JSONObject) polygon.getRelatedObject();
                        String adcode = null;
                        String name = null;
                        try {
                            adcode = properties.get("adcode").toString();
                            name = properties.get("name").toString();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(mapView.getContext(), "adcode: " + adcode + "\nname: " + name, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });
                Polygons.add(polygon);
            }
            ++count;
        }
        return Polygons;
    }

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    public MapView map = null;

    private void mapViewConfiguration()
    {
        map = (MapView) findViewById(R.id.mapview);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(9.5);
        GeoPoint startPoint = new GeoPoint(34.2925, 117.1517);
        mapController.setCenter(startPoint);
    }

    //Generate railway network overlays from JSON and add them to the map.
    //Initialize navigation manager for later use
    private void railwayNetworkConfiguration()
    {
        RailwayJSONParser parser = new RailwayJSONParser(this);
        addOverlays(parser.getOverlaysFromRawData(R.raw.station, R.raw.network));

        NonStartAndEndStationIsVisible = true;

        map.getZoomController().setOnZoomListener(new CustomZoomButtonsController.OnZoomListener() {

            @Override
            public void onVisibilityChanged(boolean b) {
            }

            @Override
            public void onZoom(boolean zoomIn) {
                double zoomLevelDetect;
                double threshold = 7.5;
                if (zoomIn) {
                    map.getController().zoomIn();
                    zoomLevelDetect = Math.abs(map.getZoomLevelDouble() - threshold);
                } else {
                    map.getController().zoomOut();
                    zoomLevelDetect = Math.abs(map.getZoomLevelDouble() - (threshold + 1));
                }
                if(zoomLevelDetect < 0.5)
                {
                    Collection<Marker> nonStartAndEndStation = parser.getNonStartAndEndStations().values();
                    if(zoomIn && !NonStartAndEndStationIsVisible)
                    {
                        NonStartAndEndStationIsVisible = true;
//                        System.out.println("Visible");
                        for(Marker station : nonStartAndEndStation)
                        {
                            station.setVisible(true);
                        }
                    }
                    if(!zoomIn && NonStartAndEndStationIsVisible)
                    {
                        NonStartAndEndStationIsVisible = false;
//                        System.out.println("Not Visible");
                        for(Marker station : nonStartAndEndStation)
                        {
                            station.setVisible(false);
                        }
                    }
                }
            }
        });

        navigationManager = parser.finishInternalMap();
//        //test
//        System.out.println(navigationManager.getInternalMap().headGuardian.getNodeNearby().size());
//        for(NavigationManager.InternalMap.Node node :
//                navigationManager.getInternalMap().headGuardian.getNodeNearby()) {
//            System.out.println(node.getRelatedMarkers().getTitle());
//        }
    }

    //TODO: here and AudioManager.java
    private void audioConfiguration() {
        audioManager = new AudioManager(this);
    }

    private void freshMarkerTitle(Marker me) {
        me.setTitle("It's me!\n" + me.getPosition().getLatitude() + "\n" + me.getPosition().getLongitude());
    }

    private void locationChanged(Marker me) {
        double latitude = 0;
        double longitude = 0;
        if(locationTrackService.canGetLocation()) {
            latitude = locationTrackService.getLatitude();
            longitude = locationTrackService.getLongitude();
            double finalLatitude = latitude;
            double finalLongitude = longitude;

            Marker debugMarker = new Marker(map);
//            //DEBUG
//            debugMarker.setIcon(getDrawable(R.drawable.ic_ditu_outoftrack));
//            map.getOverlayManager().add(debugMarker);
//            //DEBUG END

            navigationManager.trackPosition(latitude, longitude,
                    new NavigationManager.NavigationInformListener() {
                        @SuppressLint("UseCompatLoadingForDrawables")
                        @Override
                        public void onTrackSucceeded(
                                NavigationManager.InternalMap.Node processedUserLocation,
                                NavigationManager.InternalMap.Node from,
                                NavigationManager.InternalMap.Node to) {
                            //update user location
//                            //DEBUG:
//                            NavigationManager.InternalMap.Node originLocation
//                                    = new NavigationManager.InternalMap.Node(finalLatitude, finalLongitude);
//                            debugMarker.setPosition(new GeoPoint(finalLatitude, finalLongitude));
//                            System.out.println(originLocation.distanceFromLine(from, to));
//                            //DEBUG END
                            GeoPoint positionMe = new GeoPoint(
                                    processedUserLocation.getLatitude(),
                                    processedUserLocation.getLongitude());
                            me.setPosition(positionMe);
                            me.setIcon(getDrawable(R.drawable.ic_ditu_me));
                            freshMarkerTitle(me);
                            //check and send alert if needed
                            if(to.getRelatedMarkers() != null){
                                navigationManager.isWithinAlertDistance(processedUserLocation, to,
                                        new NavigationManager.AlertDistanceListener() {
                                            @Override
                                            public void onGetInAlertDistance(
                                                    double alertDistance,
                                                    NavigationManager.InternalMap.Node me,
                                                    NavigationManager.InternalMap.Node destination) {
                                                destination.getRelatedMarkers().setIcon(
                                                        getDrawable(R.drawable.ic_ditu_alert));
                                                audioManager.sendVoiceAlert(
                                                        alertDistance, to.getRelatedMarkers().getTitle());
                                            }
                                        });
                            }
                            if(isLockMe) {
                                map.getController().setCenter(positionMe);
                            }
                            map.postInvalidate();
                        }

                        @SuppressLint("UseCompatLoadingForDrawables")
                        @Override
                        public void onOutOfTrack(NavigationManager.InternalMap.Node lastProcessedUserLocation,
                                                 NavigationManager.InternalMap.Node lastFrom,
                                                 NavigationManager.InternalMap.Node lastTo) {
//                            //DEBUG:
//                            NavigationManager.InternalMap.Node originLocation
//                                    = new NavigationManager.InternalMap.Node(finalLatitude, finalLongitude);
//                            System.out.println(originLocation.distanceFromLine(lastFrom, lastTo));
//                            //DEBUG END
                            Toast.makeText(mainThis , "Out of track!", Toast.LENGTH_SHORT).show();
                            Marker marker;
                            marker = lastFrom.getRelatedMarkers();
                            if(marker != null) {
                                marker.setIcon(getDrawable(R.drawable.ic_ditu_keypoint));
                            }
                            marker = lastTo.getRelatedMarkers();
                            if(marker != null) {
                                marker.setIcon(getDrawable(R.drawable.ic_ditu_keypoint));
                            }
                            navigationManager.resetAlert();
                            GeoPoint positionMe = new GeoPoint(finalLatitude, finalLongitude);
                            me.setPosition(positionMe);
                            if(isLockMe) {
                                map.getController().setCenter(positionMe);
                            }
                            me.setIcon(getDrawable(R.drawable.ic_ditu_outoftrack));
                            freshMarkerTitle(me);
                            map.postInvalidate();
                        }

                        @SuppressLint("UseCompatLoadingForDrawables")
                        @Override
                        public void onTurnDirection(NavigationManager.InternalMap.Node processedUserLocation,
                                                    NavigationManager.InternalMap.Node from,
                                                    NavigationManager.InternalMap.Node via,
                                                    NavigationManager.InternalMap.Node to) {
                            Marker marker = from.getRelatedMarkers();
                            if(marker != null) {
                                marker.setIcon(getDrawable(R.drawable.ic_ditu_keypoint));
                            }
                            marker = via.getRelatedMarkers();
                            if(marker != null) {
                                marker.setIcon(getDrawable(R.drawable.ic_ditu_keypoint));
                            }
                            navigationManager.resetAlert();
                        }
                    });
        } else {
            Toast.makeText(mainThis, "Can not get location now.", Toast.LENGTH_SHORT).show();
        }
    }

    //TODO: make 'me' a part of NavigationManager
    @SuppressLint("UseCompatLoadingForDrawables")
    private void navigationConfiguration() {
        isLockMe = true;
        locationTrackService = new LocationTrack(this);
        Marker me = new Marker(map);
        double latitude = 0;
        double longitude = 0;
        if(locationTrackService.canGetLocation()) {
            latitude = locationTrackService.getLatitude();
            longitude = locationTrackService.getLongitude();
        }
//        System.out.println(latitude);
//        System.out.println(longitude);
        freshMarkerTitle(me);
        me.setIcon(getResources().getDrawable(R.drawable.ic_ditu_me));
        GeoPoint positionMe = new GeoPoint(latitude, longitude);
        me.setPosition(positionMe);
        if(isLockMe) {
            map.getController().setCenter(positionMe);
        }
        map.getOverlayManager().add(me);

        locationTrackService.setLocationChangedListener(new LocationTrack.LocationChangedListener() {
            @Override
            public void onUpdated(MainActivity mainActivity) {
                locationChanged(me);
            }
        });

        Button locationButton = findViewById(R.id.location_button);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationChanged(me);
            }
        });
    }

    public static final int MENU_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        DBHelper dbHelper = new DBHelper(MainActivity.this, "MapMarker.db", null, 1);
//        sqLiteDatabase = dbHelper.getWritableDatabase();

//        GeoJSONObject geoJSON = getGeoJSONFromRawData(R.raw.xuzhou);
//        List<org.osmdroid.views.overlay.Polygon> polygons = transToOsmPolygonsFromGeoJSON(geoJSON);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's
        //tile servers will get you banned based on this string

        //inflate and create the map
        setContentView(R.layout.activity_main);

        mapViewConfiguration();
        railwayNetworkConfiguration();

        requestPermissionsIfNecessary(new String[] {
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        //TODO: AudioManager
        audioConfiguration();
        //SHALL make configuration of navigation after dynamic request of permission
        navigationConfiguration();
        //menu setting
        mainThis = this;
        Button toMenu = findViewById(R.id.menu_button);
        toMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mainThis, MenuActivity.class);
                Bundle oldSettings = new Bundle();
                oldSettings.putDoubleArray("AlertSettings", navigationManager.getAlertSettings());
                oldSettings.putBoolean("IsVoiceEnabled", audioManager.isVoiceEnabled());
                intent.putExtras(oldSettings);
                startActivityForResult(intent, MENU_REQUEST);
            }
        });
        Button voiceButton = findViewById(R.id.voice_main_button);
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioManager.isVoiceEnabled()) {
                    audioManager.setVoiceEnabled(false);
                    voiceButton.setText(R.string.menu_voice_setting_button_off);
                } else {
                    audioManager.setVoiceEnabled(true);
                    voiceButton.setText(R.string.menu_voice_setting_button_on);
                }
            }
        });
        Button lockMeButton = findViewById(R.id.lock_me_button);
        lockMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isLockMe) {
                    lockMeButton.setText(R.string.lock_me_off);
                    isLockMe = false;
                } else {
                    lockMeButton.setText(R.string.lock_me_on);
                    isLockMe = true;
                }
            }
        });
    }

    boolean isLockMe;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            Bundle newSettings = data.getExtras();
            navigationManager.setAlertSettings(newSettings.getDoubleArray("AlertSettings"));
            audioManager.setVoiceEnabled(newSettings.getBoolean("IsVoiceEnabled"));
            Button voiceButton = findViewById(R.id.voice_main_button);
            if(audioManager.isVoiceEnabled()) {
                voiceButton.setText(R.string.menu_voice_setting_button_on);
            } else {
                voiceButton.setText(R.string.menu_voice_setting_button_off);
            }
//            System.out.println("预警距离 "
//                    + navigationManager.getAlertSettings()[0]
//                    + navigationManager.getAlertSettings()[1]
//                    + navigationManager.getAlertSettings()[2]);
//            System.out.println("语音启用 " + audioManager.isVoiceEnabled());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
//        setContentView(R.layout.activity_main);
    }
}