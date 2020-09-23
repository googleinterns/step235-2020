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
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.maps.errors.ApiException;
import com.google.sps.data.DeliverySlot;
import java.util.Arrays;
import java.util.Date; 
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import com.google.sps.data.BadRequestException;
import com.google.sps.data.DataNotFoundException;
import com.google.sps.data.DeliverySlotManager;
import com.google.sps.data.FirebaseAuthentication;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.assertEquals;

/**
 * Tests that delivery slots are added to 
 */
@RunWith(JUnit4.class)
public class DeliverySlotManagerTest {
  private static final long ONE_HOUR = 3600000;
  private static final long THIRTY_DAYS = TimeUnit.DAYS.toMillis(30);
  private final LocalServiceTestHelper helper =
    new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before
  public void setUp() {
    helper.setUp();
    // initialize Mock objects
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  private Entity addDeliverySlotRequestEntity(DeliverySlot deliverySlot) {
    Entity deliveryRequest = new Entity("deliverySlotRequest");
    deliveryRequest.setProperty(DeliverySlot.Property.START_TIME.label, deliverySlot.getStartTime());
    deliveryRequest.setProperty(DeliverySlot.Property.END_TIME.label, deliverySlot.getEndTime());
    deliveryRequest.setProperty(DeliverySlot.Property.USER_ID.label, deliverySlot.getUserId());
    deliveryRequest.setProperty(DeliverySlot.Property.START_LAT.label, deliverySlot.getStartLatitude());
    deliveryRequest.setProperty(DeliverySlot.Property.START_LNG.label, deliverySlot.getStartLongitude());
    // Add the delivery slot to datastore and set the slotId to the keyString from datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    ds.put(deliveryRequest);
    deliverySlot.setSlotId(KeyFactory.keyToString(deliveryRequest.getKey()));
    return deliveryRequest;
  }
  
  @Test
  public void testCreateSlotSuccessfully() throws ApiException, IOException, InterruptedException, BadRequestException, DataNotFoundException {
    DeliverySlotManager slotManager = new DeliverySlotManager();
    DeliverySlot deliverySlot = new DeliverySlot(new Date(2020, 9, 26), 0, 3600000, "user0");
    deliverySlot.setStartPoint(0.0, 0.0);
    slotManager.createDeliverySlot(deliverySlot);
    // Query datastore and verify that the request is added.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Query deliveryRequestQuery = new Query("deliverySlotRequest")
          .setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, "user0"));
    List<Entity> deliverySlots = ds.prepare(deliveryRequestQuery).asList(FetchOptions.Builder.withLimit(10));
    
    // Test that only one delivery slot request is added to datastore and the properties of it are
    // set correctly.
    assertEquals(1, deliverySlots.size());
    assertEquals("user0", deliverySlots.get(0).getProperty(DeliverySlot.Property.USER_ID.label));
    assertEquals(0.0, deliverySlots.get(0).getProperty(DeliverySlot.Property.START_LAT.label));
    assertEquals(0.0, deliverySlots.get(0).getProperty(DeliverySlot.Property.START_LNG.label));
    assertEquals(new Date(2020, 9, 26), deliverySlots.get(0).getProperty(DeliverySlot.Property.START_TIME.label));
    assertEquals(new Date(2020, 9, 26, 1, 0), deliverySlots.get(0).getProperty(DeliverySlot.Property.END_TIME.label));
  }

  @Test
  public void testGetSlotSuccessfully() throws ApiException, IOException, InterruptedException, BadRequestException, DataNotFoundException, EntityNotFoundException {
    DeliverySlotManager slotManager = new DeliverySlotManager();
    DeliverySlot deliverySlot = new DeliverySlot(new Date(2020, 9, 26), 0, 3600000, "user0");
    deliverySlot.setStartPoint(0.0, 0.0);
    slotManager.createDeliverySlot(deliverySlot);
    // The delivery slot request in datastore should be the one added in the previous line.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Query deliveryRequestQuery = new Query("deliverySlotRequest")
          .setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, "user0"));
    Entity deliverySlotRequest = ds.prepare(deliveryRequestQuery).asSingleEntity();
    // Test that the slotId of the current delivery slot matches the string encoding of 
    // deliverySlotRequest's key. 
    assertEquals(deliverySlot.getSlotId(), KeyFactory.keyToString(deliverySlotRequest.getKey()));
  }

  @Test(expected = BadRequestException.class)
  public void testCreateSlotInvalidDate() throws IOException, BadRequestException {
    DeliverySlotManager slotManager = new DeliverySlotManager();
    DeliverySlot deliverySlot = new DeliverySlot(new Date(2020, 9, 26), 3600000, 0, "user0");
    deliverySlot.setStartPoint(0.0, 0.0);
    slotManager.createDeliverySlot(deliverySlot);
  }

  @Test
  public void testGetUsersDeliverySlotRequests() throws EntityNotFoundException, ApiException, IOException, InterruptedException, DataNotFoundException, BadRequestException {
    DeliverySlot deliverySlot1 = new DeliverySlot(new Date(), 0, 2 * ONE_HOUR, "user1");
    deliverySlot1.setStartPoint(0, 0);
    addDeliverySlotRequestEntity(deliverySlot1);
    DeliverySlot deliverySlot2 = new DeliverySlot(new Date(), 2 * ONE_HOUR, 3 * ONE_HOUR, "user1");
    deliverySlot2.setStartPoint(0, 0);
    addDeliverySlotRequestEntity(deliverySlot2);
    DeliverySlotManager slotManager = new DeliverySlotManager();
    assertEquals(Arrays.asList(deliverySlot1, deliverySlot2), slotManager.getUsersDeliverySlotRequests("user1"));
  }

  @Test
  public void testGetUsersDeliverySlotRequestsWithExpiredSlots() throws EntityNotFoundException, ApiException, IOException, InterruptedException, DataNotFoundException, BadRequestException {
    DeliverySlot deliverySlot1 = new DeliverySlot(new Date(), 0, 2 * ONE_HOUR, "user1");
    deliverySlot1.setStartPoint(0, 0);
    addDeliverySlotRequestEntity(deliverySlot1);
    DeliverySlot deliverySlot2 = new DeliverySlot(new Date(System.currentTimeMillis() - THIRTY_DAYS - 3 * ONE_HOUR), 
      new Date(System.currentTimeMillis() - THIRTY_DAYS - 2 * ONE_HOUR), 
      "user1", 
      /** canBeInThePast = */ true);
    deliverySlot2.setStartPoint(0, 0);
    addDeliverySlotRequestEntity(deliverySlot2);
    DeliverySlot deliverySlot3= new DeliverySlot(new Date(System.currentTimeMillis() - THIRTY_DAYS + 2 * ONE_HOUR), 
      new Date(System.currentTimeMillis() - THIRTY_DAYS + 3 * ONE_HOUR), 
      "user1", 
      /** canBeInThePast = */ true);
    deliverySlot3.setStartPoint(0, 0);
    addDeliverySlotRequestEntity(deliverySlot3);
    DeliverySlotManager slotManager = new DeliverySlotManager();
    assertEquals(Arrays.asList(deliverySlot1, deliverySlot3), slotManager.getUsersDeliverySlotRequests("user1"));
  }
}
