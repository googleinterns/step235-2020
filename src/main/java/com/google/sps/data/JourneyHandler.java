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

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.gson.Gson;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 
 * Class that stores and creates journeys for delivery slots.
 */
public class JourneyHandler {
  private PathFinder pathFinder;

  public JourneyHandler(PathFinder pathFinder) {
    this.pathFinder = pathFinder;
  }

  /**
   * Finds a set of orders and an optimal journey for deliverySlot. If no orders have been found,
   * for the given deliverySlot, it returns false, otherwise it returns true.
   */
  public boolean processDeliveryRequest(DeliverySlot deliverySlot) throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException {
    DeliverySystem deliverySystem = new DeliverySystem(pathFinder);
    OrderHandler orderHandler = new OrderHandler(pathFinder);
    // Assign orders for the slot and create a journey with them. 
    Journey journey = deliverySystem.createJourneyForDeliveryRequest(deliverySlot);
    List<String> orderKeys = journey.getOrders();
    if (orderKeys == null || orderKeys.size() == 0) {
      // There are no orders that can be assigned to deliverSlot.
      return false;
    }
    //TODO[ak47na]: when updateStatusForOrders will use transactions, return false if transaction failed.
    orderHandler.updateStatusForOrders(orderKeys, OrderHandler.OrderStatus.ASSIGNED.toString());
    addDeliveryJourney(journey, deliverySlot);
    return true;
  }

  /**
   * Adds the details of the delivery journey assigned to deliverySlot to datastore.
   */
  private void addDeliveryJourney(Journey journey, DeliverySlot deliverySlot) throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException {
    // Find the optimal order in which points of the journey should be visited. 
    ArrayList<CourierStop> orderedWaypoints = journey.findOptimalOrderForWaypoints();
    Entity journeyEntity = new Entity("Journey");
    journeyEntity.setProperty("startDate", deliverySlot.getStartTime());
    journeyEntity.setProperty("endDate", deliverySlot.getEndTime());
    journeyEntity.setProperty("uid", deliverySlot.getUserId());
    Gson gson = new Gson();
    // Store the array of waypoints as a Json string so that when the journey is displayed, the data
    // for each point is shown (e.g. it shows the books to be rented from LibraryPoint poits).
    journeyEntity.setProperty("waypoints", gson.toJson(orderedWaypoints));
    DatastoreServiceFactory.getDatastoreService().put(journeyEntity);
  }

  public List<Entity> getJourneysForUser(String userId) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query =
        new Query("Journey")
            .setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, userId));
    PreparedQuery results = datastore.prepare(query);
    return results.asList(FetchOptions.Builder.withDefaults());
  }
}
