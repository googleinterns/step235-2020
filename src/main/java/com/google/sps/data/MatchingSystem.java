// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.data;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.firebase.auth.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.errors.ApiException;
import com.google.maps.GeoApiContext;
import com.google.maps.GeoApiContext.Builder;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class MatchingSystem {
  /**
   * Returns false if the point is already in the waypoints HashMap or it adds iy to the HashMap
   * and returns true.
   */
  static boolean addPointToWaypoints(Point point, HashMap<Point, Integer> waypoints, int numberOfWaypoints) {
    if (waypoints.containsKey(point)) {
      return false;
    }
    waypoints.put(point, numberOfWaypoints);
    return true;
  }

  /**
   * Finds the order in which points must be visited such that the journey lasts minimum time and it
   * delivers all orders assigned in findOrdersForDeliveryRequest
   */
  public static Journey findOptimalDeliveryJourneyForRequest(Date deliveryDay, int startTime, int endTime, int maxWaypoints, String userId, Double startLat, Double startLng) {
    Point start = new Point(startLat, startLng);
    Journey journey = findOrdersForDeliveryRequest(deliveryDay, startTime, endTime, maxWaypoints, start);
    journey.orderedWaypoints = journey.getOrderFromMatrix(journey.findOptimalTimePaths());
    return journey;
  }

  /**
   * Assigns a set of orders for the delivery request such that they fit the [startTime, endTime]
   * timeslot and the maximum number of stops the courier will make is <= maxWaypoints
   */
  public static Journey findOrdersForDeliveryRequest(Date deliveryDay, int startTime, int endTime, int maxWaypoints, Point start) {
    int area = start.getArea();
    Journey journey = new Journey(start);

    // get unassigned orders from datastore with the area set to area;
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query orderQuery = new Query("Order")
        .setFilter(new Query.FilterPredicate("area", Query.FilterOperator.EQUAL, area))
        .setFilter(new Query.FilterPredicate("status", Query.FilterOperator.EQUAL, "ADDED"));

    PreparedQuery orders = datastore.prepare(orderQuery);
    for (Entity order : orders.asIterable()) {
      if (journey.numberOfWaypoints >= maxWaypoints) {
        break;
      }

      LibraryPoint library = new LibraryPoint((double) order.getProperty("libraryLatitude"), (double) order.getProperty("libraryLongitude"));
      Point recipient = new LibraryPoint((double) order.getProperty("recipientLatitude"), (double) order.getProperty("recipientLongitude"));

      boolean libraryIsWaypoint = !addPointToWaypoints(library, journey.waypoints, journey.numberOfWaypoints);
      if (!libraryIsWaypoint) {
        journey.numberOfWaypoints += 1;
      }
      boolean recipientIsWaypoint = !addPointToWaypoints(recipient, journey.waypoints, journey.numberOfWaypoints);
      if (!recipientIsWaypoint) {
        journey.numberOfWaypoints += 1;
      }
      if (journey.numberOfWaypoints > maxWaypoints || !journey.findJourneyForTimeslot(deliveryDay, startTime, endTime)) {
        // if the new journey doesn't satisfy user's prefrences, remove it
        if (libraryIsWaypoint) {
          // remove the library from the waypoints if it was added only for this order
          journey.waypoints.remove(library);
          journey.numberOfWaypoints -= 1;
        }
        if (recipientIsWaypoint) {
          // remove the recipient from the waypoints if he has no other order assigned to this courier
          journey.waypoints.remove(recipient);
          journey.numberOfWaypoints -= 1;
        }
      } else {
        // the library must be visited before the recipient
        journey.restrictions.add(new Pair(journey.waypoints.get(library), journey.waypoints.get(recipient)));
        // add the books from current order to the library their are assigned to
        library.addBooks((String)order.getProperty("books"));
        // assign the order to the courier and change its status in datastore
        order.setProperty("status", "ASSIGNED");
      }
    }
    return journey;
  }
}
