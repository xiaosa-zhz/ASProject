package com.example.geojsonwithosmdroid.util;

import android.content.Context;

import com.example.geojsonwithosmdroid.MainActivity;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class NavigationManager {
    Context mContext;

    public MapManager getMapManager() {
        return mapManager;
    }

    //  InternalMap:
    //      Contain a linked list
    //      Every node of list records its location, and holds references to the node nearby
    //      Every node related to a meaningful point holds a reference to the marker
    //      Generate map by using the 'Parser'
    //  Provide function which is able to find possible location on lines within map:
    //      algorithm to find nearest line for point got from GPS service
    //      Set a threshold, report when GPS provide location to far from lines
    //  Record current location and direction of user
    //  Use these info to calculate and update location next time
    //  Send alert when user approach alert point

    public static class InternalMap {

        public static class Node {

            private double mLatitude;
            private double mLongitude;
            private List<Node> mNodeNearby;
            private Marker mRelatedMarkers;
            private boolean hasBeenReached = false;

            public Node(double latitude, double longitude) {
                this.mLatitude = latitude;
                this.mLongitude = longitude;
                mNodeNearby = new ArrayList<>();
                mRelatedMarkers = null;
            }

            //return by real distance(m)
            //Navi:
            //  1.Use Math.toRadians to get la & lo in radians
            //  2.
            //TODO: change every distance calculation into real distance calculation
            public double realDistanceFrom(Node otherNode) {
                double dLa = Math.toRadians(this.mLatitude - otherNode.mLatitude);
                double dLo = Math.toRadians(this.mLongitude - otherNode.mLongitude);
                double lo = Math.toRadians(this.mLatitude);
                //TODO: use different setting of EARTH_RADIUS in release and debug
                //  actual radius used in release = 6371393(m)
                //  modify to a larger number if not sensitive in debug
                //  note: use float literal to avoid being out of maximum
                final double EARTH_RADIUS = 6371393;
                return Math.sqrt(
                        (EARTH_RADIUS * dLa) * (EARTH_RADIUS * dLa) +
                        ((EARTH_RADIUS * Math.cos(lo) * dLo)) * (EARTH_RADIUS * Math.cos(lo) * dLo));
            }

            public double distanceFrom(Node otherNode) {
                return Math.sqrt(
                        (this.mLatitude - otherNode.mLatitude) * (this.mLatitude - otherNode.mLatitude)
                      + (this.mLongitude - otherNode.mLongitude) * (this.mLongitude - otherNode.mLongitude));
            }

            public double normalizedInnerProduct(Node node1, Node node2) {
                double lengthN1toN2 = node1.distanceFrom(node2);
                double result = ((node1.getLongitude() - this.getLongitude())
                        * (node1.getLongitude() - node2.getLongitude())
                        - (node1.getLatitude() - this.getLatitude())
                        * (node2.getLatitude() - node1.getLatitude()))
                        / (lengthN1toN2 * lengthN1toN2);
                return result;
            }

            public double normalizedOuterProduct(Node node1, Node node2) {
                double lengthN1toN2 = node1.distanceFrom(node2);
                double result = ((node1.getLongitude() - this.getLongitude())
                        * (node2.getLatitude() - node1.getLatitude())
                        - (node1.getLatitude() - this.getLatitude())
                        * (node2.getLongitude() - node1.getLongitude()))
                        / (lengthN1toN2 * lengthN1toN2);
                return result;
            }

            //distance from the line formed by node1 and node2
            public double distanceFromLine(Node node1, Node node2) {
                if(node1 == node2) {
                    return this.distanceFrom(node1);
                }
                double lengthN1toN2 = node1.distanceFrom(node2);
                double r = this.normalizedInnerProduct(node1, node2);
                if(r > 1 || r < 0) {
                    return Math.min(this.distanceFrom(node1), this.distanceFrom(node2));
                }
                double s = this.normalizedOuterProduct(node1, node2);
//                System.out.println("r: " + r + "\ns: " + s);
                return Math.abs(s * lengthN1toN2);
            }

            public List<Node> getNodeNearby() {
                return mNodeNearby;
            }

            //This method DO NOT provide connection vice versa
            public void addNodeNearby(Node nearbyNode) {
                this.mNodeNearby.add(nearbyNode);
            }

            public void removeNodeNearby(Node nearbyNode) {
                this.mNodeNearby.remove(nearbyNode);
            }

            //Make two nodes hold(nearby) each other
            public static void linkNodes(Node node1, Node node2) {
                node1.addNodeNearby(node2);
                node2.addNodeNearby(node1);
            }

            public static void unLinkNodes(Node node1, Node node2) {
                node1.removeNodeNearby(node2);
                node2.removeNodeNearby(node1);
            }

            public Marker getRelatedMarkers() {
                return mRelatedMarkers;
            }

            public void setRelatedMarkers(Marker mRelatedMarkers) {
                this.mRelatedMarkers = mRelatedMarkers;
            }

            public double getLongitude() {
                return mLongitude;
            }

            public void setLongitude(double mLongitude) {
                this.mLongitude = mLongitude;
            }

            public double getLatitude() {
                return mLatitude;
            }

            public void setLatitude(double mLatitude) {
                this.mLatitude = mLatitude;
            }
        }

        //The head node is not an actual point on the map
        //The whole map is an unconnected graph(forest) composed with sub-graphs(trees)
        //Head guardian node holds reference to roots of all those trees
        Node headGuardian;

        public InternalMap() {
            headGuardian = new Node(0, 0);
        }

        //TODO:add a point? where? who are the points near it?

        public void addNodeToGuardian(Node node) {
            headGuardian.addNodeNearby(node);
        }
    }

    public class MapManager {
        private InternalMap.Node chainHead;
        private InternalMap.Node chainEnd;

        MapManager() {
            chainHead = null;
            chainEnd = null;
        }

        public InternalMap.Node addToChain(double latitude, double longitude) {
            if(chainHead == null && chainEnd == null) {//deal with the first node in the chain
                chainHead = new InternalMap.Node(latitude, longitude);
                chainEnd = chainHead;
            } else {//deal with rest nodes added later
                InternalMap.Node newNode = new InternalMap.Node(latitude, longitude);
                InternalMap.Node.linkNodes(chainEnd, newNode);
                chainEnd = newNode;
            }
            return chainEnd;
        }

        public void clear() {
            chainHead = null;
            chainEnd = null;
        }

        //relate marker and node to each other
        public void setRelatedMarkerOfHead(Marker relatedMarker) {
            chainHead.setRelatedMarkers(relatedMarker);
            ((List<InternalMap.Node>)relatedMarker.getRelatedObject()).add(chainHead);
        }

        //relate marker and node to each other
        public void setRelatedMarkerOfEnd(Marker relatedMarker) {
            chainEnd.setRelatedMarkers(relatedMarker);
            ((List<InternalMap.Node>)relatedMarker.getRelatedObject()).add(chainEnd);
        }
    }

    public interface FunctionalOnNode {
        public void apply(InternalMap.Node node, NavigationManager manager);
    }

    private InternalMap internalMap;
    private MapManager mapManager;

    public InternalMap getInternalMap() {
        return internalMap;
    }

    public NavigationManager(Context context) {
        internalMap = new InternalMap();
        mapManager = new MapManager();
        mContext = context;
        alertSettings = new double[]{500, 1000, 2000};
    }

    private void implDFS(InternalMap.Node prev,
                         InternalMap.Node now,
                         FunctionalOnNode functional) {
        functional.apply(now, this);
        now.hasBeenReached = true;
        for(InternalMap.Node nextNode : now.getNodeNearby()) {
            if(!nextNode.hasBeenReached) {
                implDFS(now, nextNode, functional);
            }
        }
    }

    private void cleanLoopDFS(InternalMap.Node prev, InternalMap.Node now) {
        now.hasBeenReached = false;
        for(InternalMap.Node nextNode : now.getNodeNearby()) {
            if(nextNode.hasBeenReached) {
                cleanLoopDFS(now, nextNode);
            }
        }
    }

    public void DFS(FunctionalOnNode functional) {
        if(internalMap.headGuardian.mNodeNearby.isEmpty()) {
            System.out.println("There is no node in the map!");
            System.exit(-1);
        }
        for(InternalMap.Node root : getInternalMap().headGuardian.getNodeNearby()) {
            implDFS(null, root, functional);
        }
        for(InternalMap.Node root : getInternalMap().headGuardian.getNodeNearby()) {
            cleanLoopDFS(null, root);
        }
    }

    public void testInternalMap() { }

    private static final double HAS_NOT_BEEN_INITIALIZED = 1145141919;
    private static final double OUT_OF_MAP_THRESHOLD = 0.01;
//    private double mLatitude = HAS_NOT_BEEN_INITIALIZED;
//    private double mLongitude = HAS_NOT_BEEN_INITIALIZED;

    private InternalMap.Node locationLineNodeFrom = null;
    private InternalMap.Node locationLineNodeTo = null;
    private InternalMap.Node lastProcessedUserPosition = null;

    private double[] alertSettings;

    public void setAlertSettings(double[] alertSettings) {
        this.alertSettings = alertSettings;
    }

    public double[] getAlertSettings() {
        return alertSettings;
    }

    public interface AlertDistanceListener {
        void onGetInAlertDistance(double alertDistance, InternalMap.Node me, InternalMap.Node destination);
    }

    double alertLastTime = HAS_NOT_BEEN_INITIALIZED;

    public void resetAlert() {
        alertLastTime = HAS_NOT_BEEN_INITIALIZED;
    }

    //TODO: refactor to use input box
    public void isWithinAlertDistance(InternalMap.Node me, InternalMap.Node destination,
                                        AlertDistanceListener listener) {
        double minDistance = HAS_NOT_BEEN_INITIALIZED;
        for(double alertDistance : alertSettings) {
            if(me.realDistanceFrom(destination) < alertDistance && alertDistance < minDistance) {
                minDistance = alertDistance;
            }
        }
        if(minDistance != HAS_NOT_BEEN_INITIALIZED && minDistance != alertLastTime) {
            listener.onGetInAlertDistance(minDistance, me, destination);
            alertLastTime = minDistance;
        }
    }

    //search the whole map for the nearest node
    private void reloadNearestNode(InternalMap.Node locationFromNavi) {
        final double[] minDistance = {HAS_NOT_BEEN_INITIALIZED};
        DFS((node, manager) -> {
            double distance = node.distanceFrom(locationFromNavi);
            if(distance < minDistance[0] || locationLineNodeFrom == null) {
                minDistance[0] = distance;
                locationLineNodeFrom = node;
            }
        });
    }

    public interface NavigationInformListener {
        public void onTrackSucceeded(InternalMap.Node processedUserLocation,
                                     InternalMap.Node from,
                                     InternalMap.Node to);
        public void onOutOfTrack(InternalMap.Node lastProcessedUserLocation,
                                 InternalMap.Node lastFrom,
                                 InternalMap.Node lastTo);
        public void onTurnDirection(InternalMap.Node processedUserLocation,
                                    InternalMap.Node from,
                                    InternalMap.Node via,
                                    InternalMap.Node to);
    }

    public void reloadNextTime() {
        locationLineNodeFrom = null;
        locationLineNodeTo = null;
    }

    //perfect performance
    public InternalMap.Node calculatePositionOnLine(InternalMap.Node locationFromNavi) {
        InternalMap.Node processedUserPosition = null;
        double factor = locationFromNavi.normalizedInnerProduct(locationLineNodeFrom, locationLineNodeTo);
        if(factor < 0) {
            processedUserPosition = locationLineNodeFrom;
        } else if(factor > 1) {
            processedUserPosition = locationLineNodeTo;
        } else {
            processedUserPosition = new InternalMap.Node(
                    locationLineNodeFrom.getLatitude() * (1 - factor) + locationLineNodeTo.getLatitude() * factor,
                    locationLineNodeFrom.getLongitude() * (1 - factor) + locationLineNodeTo.getLongitude() * factor
            );
        }
//        //DEBUG:
//        {
//            double sfactor = locationFromNavi.normalizedOuterProduct(locationLineNodeFrom, locationLineNodeTo);
//            Polyline polyline = new Polyline();
//            polyline.addPoint(new GeoPoint(locationFromNavi.mLatitude, locationFromNavi.mLongitude));
//            GeoPoint end = new GeoPoint(
//                    locationFromNavi.mLatitude + (locationLineNodeFrom.getLongitude() - locationLineNodeTo.getLongitude()) * sfactor,
//                    locationFromNavi.mLongitude + (locationLineNodeTo.getLatitude() - locationLineNodeFrom.getLatitude()) * sfactor
//            );
//            polyline.addPoint(end);
//            if(mContext != null) {
//                ((MainActivity) mContext).map.getOverlayManager().add(polyline);
//            }
//        }
//        //DEBUG END
        return processedUserPosition;
    }

    //Algorithm to track position of user
    //TODO:
    //  distance algorithm may not be accurate enough, HOWEVER!!!!
    //      WARNING: move distance algorithm from 'simple treat degrees as rec coordinates'
    //      to 'deal with real arc length and big circle relations' may improve accuracy,
    //      but the line-drawing methods used by map engine is exactly the previous one,
    //      which means when the real and accurate coordinates are convert back to degrees on map,
    //      the result point may not on the line drawn by map engine
    //  possible solution: only use real distance in line determination in corner, however,
    //  still apply simple algorithm in marker rendering
    public void trackPosition(double latitude, double longitude,
                                    NavigationInformListener navigationInformListener) {
        InternalMap.Node locationFromNavi = new InternalMap.Node(latitude, longitude);
        double distanceFromMap = HAS_NOT_BEEN_INITIALIZED;

        //initialize location line if not
        if(locationLineNodeFrom == null) {
            //find nearest node as one side of location line
            reloadNearestNode(locationFromNavi);
            //check all nearby node to decide the other side of location line
            double minDistance = HAS_NOT_BEEN_INITIALIZED;
            for(InternalMap.Node nearbyNode : locationLineNodeFrom.getNodeNearby()) {
                double newDistance = locationFromNavi.distanceFromLine(locationLineNodeFrom, nearbyNode);
                if(newDistance < minDistance || locationLineNodeTo == null) {
                    minDistance = newDistance;
                    locationLineNodeTo = nearbyNode;
                }
            }
            if(minDistance > OUT_OF_MAP_THRESHOLD) {
                navigationInformListener.onOutOfTrack(
                        lastProcessedUserPosition,
                        locationLineNodeFrom,
                        locationLineNodeTo);
                reloadNextTime();
                return;
            }
        }

        InternalMap.Node processedUserPosition = calculatePositionOnLine(locationFromNavi);

        if(lastProcessedUserPosition == null) {
            //if not yet, initialize user position
            lastProcessedUserPosition = processedUserPosition;
            //not until the 2nd time tracking position can determine direction and make report
            return;
        } else {
            if(lastProcessedUserPosition == locationLineNodeFrom) {
                if(processedUserPosition == locationLineNodeFrom) {
                    //last and now are at the beginning of the line
                    if(locationFromNavi.distanceFrom(locationLineNodeFrom) > OUT_OF_MAP_THRESHOLD) {
                        navigationInformListener.onOutOfTrack(
                                lastProcessedUserPosition,
                                locationLineNodeFrom,
                                locationLineNodeTo);
                        reloadNextTime();
                        return;
                    }
                }
            }

            if(processedUserPosition == locationLineNodeTo) {
                if(lastProcessedUserPosition == locationLineNodeTo) {
                    //reach an end and seem not to move
                    if(locationFromNavi.distanceFrom(locationLineNodeTo) > OUT_OF_MAP_THRESHOLD) {
                        navigationInformListener.onOutOfTrack(
                                lastProcessedUserPosition,
                                locationLineNodeFrom,
                                locationLineNodeTo);
                        reloadNextTime();
                        return;
                    }
                }
                //reach an end first time, try to move to another line
                InternalMap.Node fvtFrom = locationLineNodeFrom;
                InternalMap.Node fvtVia = locationLineNodeTo;
                InternalMap.Node nodeNow = locationLineNodeTo;
                locationLineNodeTo = null;
                locationLineNodeFrom = nodeNow;
                double minDistance = HAS_NOT_BEEN_INITIALIZED;
                //search all lines nearby and move to the nearest one
                for(InternalMap.Node nearbyNode : locationLineNodeFrom.getNodeNearby()) {
                    double newDistance = locationFromNavi.distanceFromLine(locationLineNodeFrom, nearbyNode);
                    if(newDistance < minDistance || locationLineNodeTo == null) {
                        minDistance = newDistance;
                        locationLineNodeTo = nearbyNode;
                    }
                }
                InternalMap.Node fvtTo = locationLineNodeTo;
                //after moving to a new line, recalculate user position (old one is on the old line)
                processedUserPosition = calculatePositionOnLine(locationFromNavi);
                navigationInformListener.onTurnDirection(processedUserPosition, fvtFrom, fvtVia, fvtTo);
            }

            if(lastProcessedUserPosition.distanceFrom(locationLineNodeTo)
             < processedUserPosition.distanceFrom(locationLineNodeTo)) {
                //turn your head. turn your head is also turning direction
                InternalMap.Node fvtFrom = locationLineNodeFrom;
                InternalMap.Node fvtVia = locationLineNodeTo;
                InternalMap.Node temp = locationLineNodeTo;
                locationLineNodeTo = locationLineNodeFrom;
                locationLineNodeFrom = temp;
                InternalMap.Node fvtTo = locationLineNodeTo;
                navigationInformListener.onTurnDirection(processedUserPosition, fvtFrom, fvtVia, fvtTo);
            }

            if(locationFromNavi.distanceFromLine(locationLineNodeFrom, locationLineNodeTo)
                    > OUT_OF_MAP_THRESHOLD) {
                navigationInformListener.onOutOfTrack(
                        processedUserPosition, locationLineNodeFrom, locationLineNodeTo);
                reloadNextTime();
                return;
            }

            lastProcessedUserPosition = processedUserPosition;
            navigationInformListener.onTrackSucceeded(
                    lastProcessedUserPosition,
                    locationLineNodeFrom,
                    locationLineNodeTo);
        }
    }
}
