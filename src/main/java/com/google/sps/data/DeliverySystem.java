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

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.maps.errors.ApiException;
import com.google.sps.data.Point;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/** 
 * Class that assigns orders and creates delivery journeys for delivery slots.
 */
public class DeliverySystem {
  private final Integer MAX_WAYPOINTS = 25;
  private PathFinder pathFinder;

  public DeliverySystem(PathFinder pathFinder) {
    this.pathFinder = pathFinder;
  }

  /**
   * Assigns a set of orders for the delivery request such that they fit the [startTime, endTime]
   * timeslot. It creates and returns a Journey object that contains the ids of orders and the 
   * waypoints that must be visited.
   */
  public Journey createJourneyForDeliveryRequest(DeliverySlot deliverySlot)  throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException {
    Point startPoint = deliverySlot.getStartPoint();
    Journey journey = new Journey(new CourierStop(startPoint), pathFinder);
    OrderHandler orderHandler = new OrderHandler(pathFinder);
    // Get unassigned orders from datastore which are in the area of the starting point.
    List<String> orders = orderHandler.getAvailableOrders(startPoint.getArea());
    for (String orderKey : orders) {
      if (journey.getNumberOfWaypoints() >= MAX_WAYPOINTS) {
        break;
      }

      LibraryPoint library = new LibraryPoint((double) orderHandler.getProperty(orderKey, OrderHandler.OrderProperty.LIBRARY_LAT.label), 
        (double) orderHandler.getProperty(orderKey, OrderHandler.OrderProperty.LIBRARY_LNG.label),
        ((Number) orderHandler.getProperty(orderKey, OrderHandler.OrderProperty.LIBRARY_ID.label)).intValue());
      Point recipient = new Point((double) orderHandler.getProperty(orderKey, OrderHandler.OrderProperty.RECIPIENT_LAT.label), 
        (double) orderHandler.getProperty(orderKey, OrderHandler.OrderProperty.RECIPIENT_LNG.label));
      
      // Check if the library is already a waypoint in the journey, and add it in case it's not.
      boolean libraryIsWaypoint = !journey.addPointToWaypoints(library);
      // Check if the recipient is already a waypoint in the journey, and add it in case it's not.
      boolean recipientIsWaypoint = !journey.addPointToWaypoints(recipient);

      if (journey.getNumberOfWaypoints() > MAX_WAYPOINTS || !journey.findJourneyForTimeslot(deliverySlot)) {
        // If the new journey doesn't satisfy user's prefrences, remove the order.
        if (!libraryIsWaypoint) {
          // Remove the library from waypoints if it was added only for this order.
          journey.removeWaypoint(library);
        }
        if (!recipientIsWaypoint) {
          // Remove the recipient from the waypoints if he has no other order assigned to this 
          // delivery slot.
          journey.removeWaypoint(recipient);
        }
      } else {
        // The library must be visited before the recipient.
        journey.addRestriction(journey.getWaypointIndex(library), journey.getWaypointIndex(recipient));
        // The order is added to the journey
        journey.addOrder(orderKey, library, recipient);
      }
    }
    return journey;
  }
}
