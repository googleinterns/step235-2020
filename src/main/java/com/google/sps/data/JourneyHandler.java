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
import com.google.appengine.api.datastore.Entity;
import com.google.gson.Gson;
import com.google.maps.errors.ApiException;

import java.io.IOException;
import java.util.ArrayList;

/** 
 * Class that stores and creates journeys for delivery slots.
 */
public class JourneyHandler {
  /**
   * Finds a set of orders and an optimal journey for deliverySlot.
   */
  public void processDeliveryRequest(DeliverySlot deliverySlot) throws ApiException, IOException, InterruptedException {
    DeliverySystem deliverySystem = new DeliverySystem();
    ArrayList<Point> orderedWaypoints = deliverySystem.findOptimalDeliveryJourneyForDeliverySlot(deliverySlot);
    addDeliveryJourney(orderedWaypoints, deliverySlot);
  }

  /**
   * Adds the details of the delivery journey assigned to deliverySlot to datastore.
   */
  protected void addDeliveryJourney(ArrayList<Point> waypoints, DeliverySlot deliverySlot) {
    throw new UnsupportedOperationException("TODO: Implement this method.");
  }
}
