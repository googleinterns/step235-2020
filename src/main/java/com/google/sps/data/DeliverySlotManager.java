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

import com.google.api.gax.rpc.NotFoundException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.model.LatLng;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Class that manages delivery slots.
 */
public class DeliverySlotManager {
  private static final long THIRTY_DAYS = TimeUnit.DAYS.toMillis(30);
  /**
   * Creates a delivery slot request and adds it to datastore.
   */
  public void createDeliverySlot(DeliverySlot deliverySlot) {
    addDeliverySlotRequestToDatastore(deliverySlot);
  }

  /**
   * Create a deliverySlotRequest Entity with properties' values from deliverySlot.
   */
  private Entity createDeliverySlotRequestEntity(DeliverySlot deliverySlot) {
    Entity deliveryRequest = new Entity("deliverySlotRequest");

    deliveryRequest.setProperty(DeliverySlot.Property.START_TIME.label, deliverySlot.getStartTime());
    deliveryRequest.setProperty(DeliverySlot.Property.END_TIME.label, deliverySlot.getEndTime());
    deliveryRequest.setProperty(DeliverySlot.Property.USER_ID.label, deliverySlot.getUserId());
    deliveryRequest.setProperty(DeliverySlot.Property.START_LAT.label, deliverySlot.getStartLatitude());
    deliveryRequest.setProperty(DeliverySlot.Property.START_LNG.label, deliverySlot.getStartLongitude());

    return deliveryRequest;
  }

  /**
   * Adds the deliverySlotRequest entity created by createDeliverySlotRequestEntity and sets the
   * slotId of deliverySlot to be the encoded string of deliverySlotRequest's key in datastore.
   */
  private void addDeliverySlotRequestToDatastore(DeliverySlot deliverySlot) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity deliverySlotRequest = createDeliverySlotRequestEntity(deliverySlot);
    datastore.put(deliverySlotRequest);
    deliverySlot.setSlotId(KeyFactory.keyToString(deliverySlotRequest.getKey()));
  }

  /**
   * Returns true if the delivery slot started at least 30 days ago.
   */
  private boolean isExpired(DeliverySlot deliverySlot) {
    return (deliverySlot.getStartTime().getTime() + THIRTY_DAYS <= System.currentTimeMillis());
  }

  /**
   * Returns a list with all the deliverySlotRequests of user with id userId.
   */
  public List<DeliverySlot> getUsersDeliverySlotRequests(String userId) throws EntityNotFoundException, ApiException, IOException, InterruptedException, DataNotFoundException, BadRequestException {
    List<DeliverySlot> deliverySlots = new ArrayList<DeliverySlot>();

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query = new Query("deliverySlotRequest").setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, userId));
    List<Entity> results = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
    
    for (Entity deliverySlotEntity : results) {
      DeliverySlot deliverySlot = new DeliverySlot((Date)deliverySlotEntity.getProperty(DeliverySlot.Property.START_TIME.label),
      (Date)deliverySlotEntity.getProperty(DeliverySlot.Property.END_TIME.label),
      (String)deliverySlotEntity.getProperty(DeliverySlot.Property.USER_ID.label),
      /** canBeInThePast = */ true);
      if (isExpired(deliverySlot)) {
        datastore.delete(deliverySlotEntity.getKey());
      } else {
        deliverySlot.setStartPoint((double)deliverySlotEntity.getProperty(DeliverySlot.Property.START_LAT.label),
          (double)deliverySlotEntity.getProperty(DeliverySlot.Property.START_LNG.label));
        deliverySlot.setSlotId(KeyFactory.keyToString(deliverySlotEntity.getKey()));
        deliverySlots.add(deliverySlot);
      }
    }
    
    return deliverySlots;
  }
}
