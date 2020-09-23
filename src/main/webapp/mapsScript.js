 /*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Fetch apiKey and dynamically load the Maps JavaScript API.
 */
function loadMapsScript() {
  fetch('apiKey.json').then(response => response.json()).then(jsonResponse => {
    GoogleMapsApiKey = jsonResponse.apiKey;
    // Create the Google Maps tag with the fetched api key.
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${GoogleMapsApiKey}`;
    // Append the 'script' element to 'head'.
    document.body.appendChild(script);
  });
}

/**
 * Creates and returns a Map centered at (centerLat, centerLng). 
 */
function initMap(centerLat, centerLng, zoomLevel = 12) {
  const map = new google.maps.Map(
      document.getElementById('map'),
      {center: {lat: centerLat, lng: centerLng}, zoom: zoomLevel});
  return map;
}

/** 
 * Adds the waypoint as markers to the map and draws a polyline between them to show the route.
 */
function addMarkersToMap(waypoints) {
  centerLat = 0;
  centerLng = 0;
  numberOfWaypoints = waypoints.length;
  for (const waypoint in waypoints) {
    centerLat += waypoints[waypoint].point.latitude;
    centerLng += waypoints[waypoint].point.longitude;
  }

  let waypointsCoordinates = [];
  map = initMap(centerLat / numberOfWaypoints, centerLng / numberOfWaypoints);
  for (const waypoint in waypoints) {
    waypointCoordinates = {
      lat: waypoints[waypoint].point.latitude, 
      lng: waypoints[waypoint].point.longitude
    };
    // Add the current waypoint to the map.
    const waypointMarker = new google.maps.Marker({
      position: waypointCoordinates,
      map: map,
      title: `waypoint${waypoint+1}`
    });
    waypointsCoordinates.push(waypointCoordinates);
  }

  addPolyLineBetweenWaypoints(map, waypointsCoordinates);
}

/** 
 * Given a map and an array of point coordinates, it draws a polyline between them.
 */
function addPolyLineBetweenWaypoints(map, waypointsCoordinates, color = "#FF0000", opacity = 1.0, weight = 2) {
  const journeyPath = new google.maps.Polyline({
    path: waypointsCoordinates,
    geodesic: true,
    strokeColor: color,
    strokeOpacity:opacity,
    strokeWeight:weight
  });
  journeyPath.setMap(map);
}
