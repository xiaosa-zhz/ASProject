package com.example.geojsonwithosmdroid;

import android.content.Context;

import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class NavigationManager {
    Context mContext;
    public NavigationManager(Context context) {
        mContext = context;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    //TODO:
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

            Node(double latitude, double longitude) {
                this.mLatitude = latitude;
                this.mLongitude = longitude;
                mNodeNearby = new ArrayList<>();
                mRelatedMarkers = null;
            }

            public double distanceFrom(Node otherNode) {
                return Math.sqrt(
                        (this.mLatitude - otherNode.mLatitude) * (this.mLatitude - otherNode.mLatitude)
                      + (this.mLongitude - otherNode.mLongitude) * (this.mLongitude - otherNode.mLongitude));
            }

            public double normalizedInnerProduct(Node node1, Node node2) {
                double lengthN1toN2 = node1.distanceFrom(node2);
                return ((node1.getLongitude() - this.getLongitude())
                        * (node1.getLongitude() - node2.getLongitude())
                        - (node1.getLatitude() - this.getLatitude())
                        * (node2.getLatitude() - node1.getLatitude()))
                        / (lengthN1toN2 * lengthN1toN2);
            }

            public double normalizedOuterProduct(Node node1, Node node2) {
                double lengthN1toN2 = node1.distanceFrom(node2);
                return ((node1.getLongitude() - this.getLongitude())
                        * (node2.getLatitude() - node1.getLatitude())
                        - (node1.getLatitude() - this.getLatitude())
                        * (node2.getLongitude() - node1.getLongitude())
                        / (lengthN1toN2 * lengthN1toN2));
            }

            //distance from the line formed by node1 and node2
            public double distanceFromLine(Node node1, Node node2) {
                if(node1 == node2) {
                    return this.distanceFrom(node1);
                }
                double lengthN1toN2 = node1.distanceFrom(node2);
                double r = this.normalizedOuterProduct(node1, node2);
                if(r > 1 || r < 0) {
                    return Math.min(this.distanceFrom(node1), this.distanceFrom(node2));
                }
                double s = this.normalizedOuterProduct(node1, node2);
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

    public NavigationManager() {
        internalMap = new InternalMap();
        mapManager = new MapManager();
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

    private static final double HAS_NOT_BEEN_INITIALIZED = 1080;
    private static final double OUT_OF_MAP_THRESHOLD = 0.05;
    private double mLatitude = HAS_NOT_BEEN_INITIALIZED;
    private double mLongitude = HAS_NOT_BEEN_INITIALIZED;

    private InternalMap.Node locationLineNodeFrom = null;
    private InternalMap.Node locationLineNodeTo = null;
    private InternalMap.Node lastProcessedUserPosition = null;

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
        public void onOutOfTrack(InternalMap.Node lastProcessedUserLocation);
        public void onTurnDirection(InternalMap.Node processedUserLocation,
                                    InternalMap.Node from,
                                    InternalMap.Node via,
                                    InternalMap.Node to);
    }

    public void reloadNextTime() {
        locationLineNodeFrom = null;
    }

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
        return processedUserPosition;
    }

    //Algorithm to track position of user
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
                navigationInformListener.onOutOfTrack(lastProcessedUserPosition);
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
                        reloadNextTime();
                        return;
                    }
                }
            }

            if(processedUserPosition == locationLineNodeTo) {
                if(lastProcessedUserPosition == locationLineNodeTo) {
                    //reach an end and seem not to move
                    if(locationFromNavi.distanceFrom(locationLineNodeTo) > OUT_OF_MAP_THRESHOLD) {
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

            lastProcessedUserPosition = processedUserPosition;
            navigationInformListener.onTrackSucceeded(
                    lastProcessedUserPosition,
                    locationLineNodeFrom,
                    locationLineNodeTo);
        }
    }
}
