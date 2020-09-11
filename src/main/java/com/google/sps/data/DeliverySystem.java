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
import com.google.maps.errors.ApiException;
import com.google.sps.data.Point;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/** 
 * Class that assigns orders and creates delivery journeys for delivery slots.
 */
public class DeliverySystem {
  /**
   * Finds the order in which points must be visited such that the journey lasts minimum time and it
   * delivers all orders assigned in findOrdersForDeliveryRequest.
   */
  public ArrayList<Point> findOptimalDeliveryJourneyForDeliverySlot(DeliverySlot deliverySlot) throws ApiException, IOException, InterruptedException {
    throw new UnsupportedOperationException("TODO: Implement this method.");
  }
}
