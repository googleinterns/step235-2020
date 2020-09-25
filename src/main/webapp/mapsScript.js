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
 * Returns the latitude and longitude of the map center as the average of coordinates for all the waypoints .
 */
function getMapCenter(waypoints) {
  centerLat = 0;
  centerLng = 0;
  numberOfWaypoints = waypoints.length;
  for (const waypoint in waypoints) {
    centerLat += waypoints[waypoint].point.latitude;
    centerLng += waypoints[waypoint].point.longitude;
  }
  return {
    'latitude': centerLat / numberOfWaypoints,
    'longitude': centerLng / numberOfWaypoints
  };
}

/** 
 * Adds the waypoint as markers to the map and draws a polyline between them to show the route.
 */
function addMarkersAndDetailsToMap(waypoints) {
  let waypointsCoordinates = [];
  const center = getMapCenter(waypoints);
  map = initMap(center.latitude, center.longitude);
  for (const waypoint in waypoints) {
    waypointCoordinates = {
      lat: waypoints[waypoint].point.latitude, 
      lng: waypoints[waypoint].point.longitude
    };
    // Add the current waypoint to the map and show the instructions for it onclick.
    addMarkerToMap(waypointCoordinates, map, `waypoint${waypoint+1}`, getWaypointDescription(waypoint, waypoints));
    waypointsCoordinates.push(waypointCoordinates);
  }
  // Show the path between waypoints on the map.
  addPolyLineBetweenWaypoints(map, waypointsCoordinates);
}

/**
 * Returns a string with the delivery instructions for the waypoint-th element in waypoints.
 */
function getWaypointDescription(waypoint, waypoints) {
  let description;
  if (waypoint == 0 && waypoints[waypoint].orderKeys.length == 0) {
    // The point is the starting point.
    description = 'Start Here';
  } else {
    if (waypoints[waypoint].point.hasOwnProperty('libraryId')) {
    // The point is a library, thus, order must be taken from it.
    description = 'Borrow books for orders:';
    } else {
      // The point is the home of a recipient, thus, orders must be delivered.
      description = 'Deliver orders:'
    }
  }
  // Add the IDs of orders to the description of the waypoint.
  for (const index in waypoints[waypoint].orderKeys) {
    description += `\n${waypoints[waypoint].orderKeys[index]}`;
  }
  return description;
}

/**
 * Adds a market on the map with a title and a description.
 * @param {LatLng} position 
 * @param {Map} map 
 * @param {String} title 
 * @param {String} description 
 */
function addMarkerToMap(position, map, title, description) {
  const marker = new google.maps.Marker({
    position: position,
    map: map, 
    title: title
  });

  const infoWindow = new google.maps.InfoWindow({content: description});
  marker.addListener('click', () => {
    infoWindow.open(map, marker);
  });
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
