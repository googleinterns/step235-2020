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
import static org.junit.Assert.assertEquals;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;
import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests that orders are correctly assigned to delivery slots.
 */
@RunWith(JUnit4.class)
public class DeliverySystemTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private ArrayList<Point> points;
  private Point start;
  private DeliverySystem deliverySystem;
  private OrderHandler orderHandler;
  private ManhattanDistancePathFinder pathFinder;

  @Before
  public void setUp() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    helper.setUp();
    pathFinder = new ManhattanDistancePathFinder();
    deliverySystem = new DeliverySystem(pathFinder);
    orderHandler = new OrderHandler(pathFinder);

    points = new ArrayList();
    points.add(new LibraryPoint(0.0002, 0.0002, 0));
    points.add(new Point(0.0003, 0.0003));
    points.add(new LibraryPoint(0.0004, 0.0005, 1));
    points.add(new Point(0.0005, 0.0006));
    points.add(new Point(0.0009, 0.0008));
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testTakeAllOrders() throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException  {
    orderHandler.addOrderToDatastore((LibraryPoint)points.get(0), Arrays.asList("buc0AAAAMAAJ", "zyTCAlFPjgYC"), "user0", points.get(1));
    orderHandler.addOrderToDatastore((LibraryPoint)points.get(2), Arrays.asList("buc0AAAAMAAJ", "zyTCAlFPjgYC"), "user0", points.get(3));
    orderHandler.addOrderToDatastore((LibraryPoint)points.get(2), Arrays.asList("buc0AAAAMAAJ", "zyTCAlFPjgYC"), "user0", points.get(4));
    long totalSeconds = pathFinder.distance(points.get(0), points.get(1)) +
      pathFinder.distance(points.get(1), points.get(2)) +
      pathFinder.distance(points.get(2), points.get(3)) + 
      pathFinder.distance(points.get(3), points.get(4)) +
      pathFinder.distance(new Point(-0.0001, 0), points.get(0)) + 1;

    DeliverySlot deliverySlot = new DeliverySlot(new Date(2020, 9, 26), 0, totalSeconds * 1000, "user0");
    // Start all delivery journey at point (-0.0001, 0).
    deliverySlot.setStartPoint(-0.0001, 0);
    Journey journey = deliverySystem.createJourneyForDeliveryRequest(deliverySlot);
    assertEquals(5, journey.getNumberOfWaypoints());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testTakeNoOrder() throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException {
    orderHandler.addOrderToDatastore((LibraryPoint)points.get(0), Arrays.asList("buc0AAAAMAAJ", "zyTCAlFPjgYC"), "user0", points.get(1));
    orderHandler.addOrderToDatastore((LibraryPoint)points.get(2), Arrays.asList("buc0AAAAMAAJ", "NRWlitmahXkC"), "user0", points.get(3));
    orderHandler.addOrderToDatastore((LibraryPoint)points.get(2), Arrays.asList("NRWlitmahXkC", "zyTCAlFPjgYC"), "user0", points.get(4));
    
    DeliverySlot deliverySlot = new DeliverySlot(new Date(2020, 9, 26), 0, 1, "user0");
    // Start all delivery journey at point (-0.0001, 0).
    deliverySlot.setStartPoint(-0.0001, 0);
    Journey journey = deliverySystem.createJourneyForDeliveryRequest(deliverySlot);

    assertEquals(0, journey.getNumberOfWaypoints());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testTakeOneOrder() throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException {
    orderHandler.addOrderToDatastore((LibraryPoint)points.get(0), Arrays.asList("buc0AAAAMAAJ", "zyTCAlFPjgYC"), "user0", points.get(1));
    orderHandler.addOrderToDatastore((LibraryPoint)points.get(2), Arrays.asList("buc0AAAAMAAJ", "NRWlitmahXkC"), "user0", points.get(3));
    orderHandler.addOrderToDatastore((LibraryPoint)points.get(0), Arrays.asList("NRWlitmahXkC", "zyTCAlFPjgYC"), "user0", points.get(4));

    long totalSeconds = pathFinder.distance(points.get(0), points.get(1)) +
      pathFinder.distance(new Point(-0.0001, 0), points.get(0)) + 1;
    DeliverySlot deliverySlot = new DeliverySlot(new Date(2020, 9, 26), 0, totalSeconds * 1000, "user0");
    // Start all delivery journey at point (-0.0001, 0).
    deliverySlot.setStartPoint(-0.0001, 0);
    Journey journey = deliverySystem.createJourneyForDeliveryRequest(deliverySlot);
    assertEquals(2, journey.getNumberOfWaypoints());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void noAvailableOrder() throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException {
    long totalSeconds = pathFinder.distance(points.get(0), points.get(1)) +
      pathFinder.distance(points.get(2), points.get(3)) + 
      pathFinder.distance(points.get(2), points.get(4));

    DeliverySlot deliverySlot = new DeliverySlot(new Date(2020, 9, 26), 0, totalSeconds * 1000 + 1, "user0");
    // Start all delivery journey at point (-0.0001, 0).
    deliverySlot.setStartPoint(-0.0001, 0);
    Journey journey = deliverySystem.createJourneyForDeliveryRequest(deliverySlot);
    assertEquals(0, journey.getNumberOfWaypoints());
  }
}
