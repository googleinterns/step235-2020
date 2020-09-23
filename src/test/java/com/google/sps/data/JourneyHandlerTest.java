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

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.gson.Gson;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;

/**
 * Tests that orders are correctly assigned to delivery slots.
 */
@RunWith(JUnit4.class)
public class JourneyHandlerTest {
  private final LocalServiceTestHelper helper =
    new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private JourneyHandler journeyHandler;
  private OrderHandler orderHandler;
  private ManhattanDistancePathFinder pathFinder;
  private ArrayList<Point> points;
  private Point startPoint;
  private List<CourierStop> courierStops;

  @Before
  public void setUp() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    helper.setUp();
    pathFinder = new ManhattanDistancePathFinder();
    journeyHandler = new JourneyHandler(pathFinder);
    orderHandler = new OrderHandler(pathFinder);
    points = new ArrayList();
    points.add(new LibraryPoint(0.0002, 0.0002, 0));
    points.add(new Point(0.0003, 0.0003));
    points.add(new LibraryPoint(0.0004, 0.0005, 1));
    points.add(new Point(0.0005, 0.0006));
    points.add(new Point(0.0009, 0.0008));
    points.add(new Point(1, 1));
    // startPoint will be the starting point for all delivery journeys.
    startPoint = new Point(-0.0001, 0);
    // Create courier stops from points.
    courierStops = Arrays.asList(startPoint, points.get(0), points.get(1), points.get(2), 
      points.get(3), points.get(4), points.get(5))
      .stream()
      .map(point -> new CourierStop((Point)point))
      .collect(Collectors.toList());
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  private Entity createJourneyEntity(DeliverySlot deliverySlot, List<CourierStop> points) {
    Entity journeyEntity = new Entity("Journey");
    journeyEntity.setProperty("startDate", deliverySlot.getStartTime());
    journeyEntity.setProperty("endDate", deliverySlot.getEndTime());
    journeyEntity.setProperty("uid", deliverySlot.getUserId());
    Gson gson = new Gson();
    journeyEntity.setProperty("waypoints", gson.toJson(points));
    DatastoreServiceFactory.getDatastoreService().put(journeyEntity);
    return journeyEntity;
  }

  private void createOrder(CourierStop library, CourierStop recipient, List<String> bookIds, String userId, boolean addOrderKey) {
    String orderKey = orderHandler.addOrderToDatastore((LibraryPoint)library.getPoint(), bookIds, userId, recipient.getPoint());
    if (addOrderKey) {
      library.addOrderKey(orderKey);
      recipient.addOrderKey(orderKey);
    }
  }

  @Test
  public void testProcessDeliveryNoOrders() throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException {
    DeliverySlot deliverySlot = new DeliverySlot(new Date(2020, 9, 26), 0,  36000, "user0");
    // Start all delivery journey at point (-0.0001, 0).
    deliverySlot.setStartPoint(-0.0001, 0);
    // There are no orders added to datastore, thus there will be no journey for the delivery slot.
   Assert.assertFalse(journeyHandler.processDeliveryRequest(deliverySlot));
  }

  @Test
  public void testProcessDelivery() throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException {
    createOrder(courierStops.get(1), courierStops.get(2), Arrays.asList("buc0AAAAMAAJ", "zyTCAlFPjgYC"), "user0", /** addOrderKey = */ true);
    createOrder(courierStops.get(3), courierStops.get(4), Arrays.asList("buc0AAAAMAAJ", "zyTCAlFPjgYC"), "user0", /** addOrderKey = */ true);
    createOrder(courierStops.get(3), courierStops.get(5), Arrays.asList("buc0AAAAMAAJ", "zyTCAlFPjgYC"), "user0", /** addOrderKey = */ true);
    createOrder(courierStops.get(1), courierStops.get(6), Arrays.asList("buc0AAAAMAAJ", "zyTCAlFPjgYC"), "user0", /** addOrderKey = */ false);
    
    long totalSeconds = 0;
    List<CourierStop> waypoints = new ArrayList<>();
    for (int i = 0; i < 6; ++ i) {
      waypoints.add(courierStops.get(i));
    }
    for (int i = 0; i < waypoints.size() - 1; ++ i) {
      totalSeconds += pathFinder.distance(waypoints.get(i).getPoint(), waypoints.get(i + 1).getPoint());
    }
    DeliverySlot deliverySlot = new DeliverySlot(new Date(2020, 9, 26), 0, totalSeconds * 1000, "user0");
    // Start all delivery journey at point (-0.0001, 0).
    deliverySlot.setStartPoint(-0.0001, 0);
    journeyHandler.processDeliveryRequest(deliverySlot);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity journeyEntity = ds.prepare(new Query("Journey")).asSingleEntity();
    Assert.assertEquals(deliverySlot.getStartTime(), journeyEntity.getProperty("startDate"));
    Assert.assertEquals(deliverySlot.getEndTime(), journeyEntity.getProperty("endDate"));
    Assert.assertEquals("user0", journeyEntity.getProperty("uid"));
    Gson gson = new Gson();
    Assert.assertEquals(gson.toJson(waypoints), journeyEntity.getProperty("waypoints"));
  }

  @Test
  public void testGetJourneysForUser() throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException {
    // Create journey for user0 visiting courierStops in a 1 hour delivery slot.
    Entity journey1 = createJourneyEntity(new DeliverySlot(new Date(2020, 9, 26), 0, 3600000, "user0"),
      courierStops);
    // Create journey for user0 visiting courierStops in a 2 hours delivery slot.
    Entity journey2 = createJourneyEntity(new DeliverySlot(new Date(2020, 9, 26), 0, 2 * 3600000, "user0"),
      courierStops);
    // Create journey for user1 visiting courierStops in a 1 hour delivery slot.
    Entity journey3 = createJourneyEntity(new DeliverySlot(new Date(2020, 9, 26), 0, 3600000, "user1"),
      courierStops);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Assert.assertEquals(Arrays.asList(journey1, journey2), journeyHandler.getJourneysForUser("user0"));
    Assert.assertEquals(Arrays.asList(journey3), journeyHandler.getJourneysForUser("user1"));
  }
}
