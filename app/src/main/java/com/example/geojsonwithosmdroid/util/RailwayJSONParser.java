package com.example.geojsonwithosmdroid.util;

import android.annotation.SuppressLint;
import android.widget.Toast;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Position;
import com.example.geojsonwithosmdroid.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class RailwayJSONParser {

    private final AppCompatActivity activity;

    private GeoJSONObject rawStationGeoInfo;

    private GeoJSONObject rawNetworkGeoInfo;

    private Map<String, Marker> mNonStartAndEndStations;

    private Map<String, Marker> mStartAndEndStations;

    private NavigationManager navigationManager;

    public Map<String, Marker> getNonStartAndEndStations() {
        return mNonStartAndEndStations;
    }

    public RailwayJSONParser(AppCompatActivity appCompatActivity) {
        activity = appCompatActivity;
        mNonStartAndEndStations = new Hashtable<>();
        mStartAndEndStations = new Hashtable<>();
        navigationManager = new NavigationManager();
    }

    public NavigationManager getNavigationManager() {
        return navigationManager;
    }

    private JSONObject getInfoJSON(OverlayWithIW overlay)
    {
        return (JSONObject) overlay.getRelatedObject();
    }

    public GeoJSONObject getGeoJSONfromRawData(@RawRes int id) {
        InputStream fileIn = activity.getResources().openRawResource(id);
        GeoJSONObject geoJSON = null;
        try {
            geoJSON = GeoJSON.parse(fileIn);
            fileIn.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return geoJSON;
    }

    public List<Polyline> parseNetwork(GeoJSONObject networkJSON)
    {
        List<Feature> features = ((FeatureCollection) networkJSON).getFeatures();
        List<Polyline> railwayLines = new ArrayList<>();
        for(Feature feature : features)
        {
            Polyline railwayLine = new Polyline();
            //convert points to GeoPoint for polyline
            List<Position> positions = ((LineString) feature.getGeometry()).getPositions();
            List<GeoPoint> geoPoints = new ArrayList<>();
            for(Position position : positions)
            {
                double latitude = position.getLatitude();
                double longitude = position.getLongitude();
                navigationManager.getMapManager().addToChain(latitude, longitude);
                geoPoints.add(new GeoPoint(latitude, longitude));
            }
            railwayLine.setPoints(geoPoints);
            //attach info to polyline
            JSONObject properties = feature.getProperties();
            railwayLine.setRelatedObject(properties);
            railwayLine.setOnClickListener(new Polyline.OnClickListener() {
                @Override
                public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                    JSONObject info = getInfoJSON(polyline);
                    String start = null;
                    String end = null;
                    try {
                        start = info.getString("start_stat");
                        end = info.getString("end_statio");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(mapView.getContext(), "start: " + start + "\nend: " + end, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            //record start and end stations for later use
            String start = null;
            String end = null;
            try {
                start = properties.getString("start_stat");
                end = properties.getString("end_statio");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //TODO: 由于给的JSON匹配差，这里做了很多没必要的判断
            //  未来要求尽量在原始数据中消除这些不匹配，并重写此段
            if(mStartAndEndStations.get(start) == null && !start.equals("")) {
//                System.out.println("Move marker of start: " + start);
                if(mNonStartAndEndStations.get(start) == null) {
//                    System.out.println("Can not find station + '" + start + "'");
                } else {
                    mStartAndEndStations.put(start, mNonStartAndEndStations.get(start));
                    mNonStartAndEndStations.remove(start);
                }
            }
            if(mStartAndEndStations.get(end) == null && !end.equals("")) {
//                System.out.println("Move marker of end: " + end);
                if(mNonStartAndEndStations.get(end) == null) {
//                    System.out.println("Can not find station + '" + end + "'");
                } else {
                    mStartAndEndStations.put(end, mNonStartAndEndStations.get(end));
                    mNonStartAndEndStations.remove(end);
                }
            }
            //relate marker and node to each other
            //the same Marker will share by multiple starts/ends of lines
            if(mStartAndEndStations.get(start) != null && !start.equals("")) {
                navigationManager.getMapManager().setRelatedMarkerOfHead(mStartAndEndStations.get(start));
            }
            if(mStartAndEndStations.get(end) != null && !end.equals("")) {
                navigationManager.getMapManager().setRelatedMarkerOfEnd(mStartAndEndStations.get(end));
            }
            //from now on, the markers holds these nodes, old chain can be freed
            navigationManager.getMapManager().clear();
//            mNonStartAndEndStations.remove(start);
//            mNonStartAndEndStations.remove(end);

            //TODO: TODO END

            //Polyline contains POINTS
            //Not every point has a meaning, most of them are just part of a Polyline

            //jobs done, add to list
            railwayLines.add(railwayLine);
        }
        return railwayLines;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public List<Marker> parseStation(GeoJSONObject stationJSON)
    {
        List<Feature> features = ((FeatureCollection) stationJSON).getFeatures();
        List<Marker> stations = new ArrayList<>();
        for(Feature feature : features)
        {
            //set location of station
            Position position = ((Point) feature.getGeometry()).getPosition();
            Marker station = new Marker(activity.findViewById(R.id.mapview));
            //attach info
            JSONObject info = feature.getProperties();
            String stationName = null;
            try {
                stationName = info.getString("station_na");
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println("Get null station name!");
                System.exit(-1);
            }
            station.setPosition(new GeoPoint(position.getLatitude(), position.getLongitude()));
            station.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//            assert stationName != null;
            station.setTitle("name: " + stationName);
            station.setIcon(activity.getResources().getDrawable(R.drawable.ic_ditu_keypoint));
            //this list will be used to record related nodes in internal map
            station.setRelatedObject(new ArrayList<NavigationManager.InternalMap.Node>());
            //special configuration for start and end station
            mNonStartAndEndStations.put(stationName, station);
            //jobs done, add to list
            stations.add(station);
        }
        return stations;
    }

    private boolean hasParsedRawData = false;

    public List<Overlay> getOverlaysFromRawData(@RawRes int station, @RawRes int network) {
        rawStationGeoInfo = getGeoJSONfromRawData(station);
        rawNetworkGeoInfo = getGeoJSONfromRawData(network);
        List<Marker> stations = parseStation(rawStationGeoInfo);
        List<Polyline> networks = parseNetwork(rawNetworkGeoInfo);
        List<Overlay> overlays = new ArrayList<>();
        overlays.addAll(networks);
        overlays.addAll(stations);
        hasParsedRawData = true;
        return overlays;
    }

    public NavigationManager finishInternalMap() {
        if(!hasParsedRawData) {
            System.out.println("Must finish the map after parsing.");
            System.exit(-1);
        }
        Collection<Marker> stationSE = mStartAndEndStations.values();
        List<NavigationManager.InternalMap.Node> finalNodes = new ArrayList<>();
        for(Marker marker : stationSE) {
            //regain nodes from marker
            List<NavigationManager.InternalMap.Node> keyStationList =
                    (List<NavigationManager.InternalMap.Node>) marker.getRelatedObject();
            //generate a new node as final node to be added in the internal map
            NavigationManager.InternalMap.Node finalNode =
                    new NavigationManager.InternalMap.Node(
                            keyStationList.get(0).getLatitude(),
                            keyStationList.get(0).getLongitude()
                    );
            finalNode.setRelatedMarkers(marker);
            //connect nearby nodes to the final node and disconnect them from old node
            for(NavigationManager.InternalMap.Node relatedNode : keyStationList) {
                List<NavigationManager.InternalMap.Node> nearbyNodeList = relatedNode.getNodeNearby();
                for(NavigationManager.InternalMap.Node nearbyNode : nearbyNodeList) {
                    //connect nearbyNode and finalNode to each other
                    NavigationManager.InternalMap.Node.linkNodes(finalNode, nearbyNode);
                    //but only remove reference from nearbyNode at first, avoid changing volume of circulation
                    nearbyNode.removeNodeNearby(relatedNode);
                }
                //clear the list
                nearbyNodeList.clear();
            }
            //TODO: May be unnecessary? If there is other info attached to marker, replace it.
            //  At least need to set it as 'null'
            marker.setRelatedObject(finalNode);
            //add final node to list for later use
            finalNodes.add(finalNode);
        }
//        //test
//        System.out.println(finalNodes.size());

        //DFS the map and remove extra nodes come from the same subgraph
        List<NavigationManager.InternalMap.Node> rootNodes = new ArrayList<>();
        while(!finalNodes.isEmpty()) {
            NavigationManager.InternalMap.Node root = finalNodes.get(0);
            navigationManager.getInternalMap().addNodeToGuardian(root);
            navigationManager.DFS(new NavigationManager.FunctionalOnNode() {
                @Override
                public void apply(NavigationManager.InternalMap.Node node, NavigationManager manager) {
                    finalNodes.remove(node);
                }
            });
            navigationManager.getInternalMap().headGuardian.getNodeNearby().remove(root);
            rootNodes.add(root);
        }
        //add all roots to the guardian node and the map is completed
        navigationManager.getInternalMap().headGuardian.getNodeNearby().addAll(rootNodes);
        return navigationManager;
    }

    public GeoJSONObject getRawStationGeoInfo() {
        return rawStationGeoInfo;
    }

    public GeoJSONObject getRawNetworkGeoInfo() {
        return rawNetworkGeoInfo;
    }

    public Map<String, Marker> getStartAndEndStations() {
        return mStartAndEndStations;
    }
}
