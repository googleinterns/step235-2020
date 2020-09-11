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
import com.google.appengine.api.datastore.KeyFactory;
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
import java.util.Date;

/**
 * Class that manages delivery slots.
 */
public class DeliverySlotManager {
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
}
