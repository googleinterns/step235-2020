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
import java.util.Date; 
import java.time.LocalTime;
import java.util.List;
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
}
