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

import com.google.maps.errors.ApiException;
import java.lang.Math;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.lang.InterruptedException;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests the behaviour of a Journey object.
 * TODO[ak47na]: Implement brute-force algorithm that iterates over all permutations of waypoints
 * and finds the optimal one.
 */
@RunWith(JUnit4.class)
public class JourneyTest {
  private ManhattanDistancePathFinder pathFinder = new ManhattanDistancePathFinder();
  
  @Test
  public void testEmptyJourney() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    CourierStop start = new CourierStop(new Point(-0.0001, 0));
    Journey journey = new Journey(start, pathFinder);
    // No waypoints are added, thus the array of waypoints should be empty.
    assertEquals(Arrays.asList(), journey.findOptimalOrderForWaypoints());
  }
  @Test
  public void testOptimalTimeJourneyRestrictionsDoNotAffectOptimalOrder() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    CourierStop start = new CourierStop(new Point(-0.0001, 0));
    Journey journey = new Journey(start, pathFinder);
    
    Point point0 = new Point(0.0002, 0.0002);
    Point point1 = new Point(0.0007, 0.0003);
    journey.addPointToWaypoints(point0);
    journey.addPointToWaypoints(point1);

    for (int i = 1; i < journey.getNumberOfWaypoints(); ++ i) {
      // Point 0 must be visited first.
      journey.addRestriction(0, i);
    }
    long totalSeconds = pathFinder.distance(point0, point1) + pathFinder.distance(start.getPoint(), point0);
    assertTrue(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds + 1) * 1000, "user0")));
    assertFalse(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds - 1) * 1000, "user0")));
    // Check that algorithm finds the optimal order for waypoints.
    assertEquals(Arrays.asList(start, new CourierStop(point0), new CourierStop(point1)),
      journey.findOptimalOrderForWaypoints());
  }

  @Test
  public void testOptimalTimeJourneyRestrictionsAffectOptimalOrder() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    CourierStop start = new CourierStop(new Point(-0.0001, 0));
    Journey journey = new Journey(start, new ManhattanDistancePathFinder());

   Point point0 = new Point(0.0002, 0.0002);
    Point point1 = new Point(0.0007, 0.0003);
    journey.addPointToWaypoints(point0);
    journey.addPointToWaypoints(point1);

    for (int i = 1; i < journey.getNumberOfWaypoints(); ++ i) {
      journey.addRestriction(i, 0);
    }

    long totalSeconds = pathFinder.distance(point1, point0) + pathFinder.distance(start.getPoint(), point1);
    assertTrue(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds + 1) * 1000, "user0")));
    assertFalse(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds - 1) * 1000, "user0")));
    // Check that algorithm finds the optimal order for waypoints.
    assertEquals(Arrays.asList(start, new CourierStop(point1), new CourierStop(point0)),
      journey.findOptimalOrderForWaypoints());
  }

  @Test
  public void testOptimalTimeJourneySortedOrder() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    // For each point, the previous point has latitude and longitude smaller or equal.
    CourierStop start = new CourierStop(new Point(-0.0001, 0));
    Journey journey = new Journey(start, new ManhattanDistancePathFinder());

    List<Point> points = Arrays.asList(new Point(0.0002, 0.0002), 
      new Point(0.0003, 0.0003),
      new Point(0.0004, 0.0005),
      new Point(0.0005, 0.0006), 
      new Point(0.0009, 0.0008));
    for (Point point : points) {
      journey.addPointToWaypoints(point);
    }

    for (int i = 1; i < journey.getNumberOfWaypoints(); ++ i) {
      journey.addRestriction(0, i);
    }

    List<CourierStop> expected = new ArrayList<>();
    expected.add(start);
    for (Point point : points) {
      expected.add(new CourierStop(point));
    }
    long totalSeconds = 0;
    for (int i = 0; i < expected.size() - 1; ++ i) {
      totalSeconds += pathFinder.distance(expected.get(i).getPoint(), expected.get(i + 1).getPoint());
    }

    assertTrue(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds + 1) * 1000, "user0")));
    assertFalse(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds - 1) * 1000, "user0")));
    // Check that algorithm finds the optimal order for waypoints.
    assertEquals(expected, journey.findOptimalOrderForWaypoints());
  }

  @Test
  public void testOptimalTimeJourneyReverseSortedOrder() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    // For each point, the previous point has latitude and longitude smaller or equal.
    CourierStop start = new CourierStop(new Point(-0.0001, 0));
    Journey journey = new Journey(start, new ManhattanDistancePathFinder());

    List<Point> points = Arrays.asList(new Point(0.0002, 0.0002), 
      new Point(0.0003, 0.0003),
      new Point(0.0004, 0.0005),
      new Point(0.0005, 0.0006), 
      new Point(0.0009, 0.0008));
    for (Point point : points) {
      journey.addPointToWaypoints(point);
    }

    journey.addRestriction(4, 0);
    // (0.0009, 0.0008) must be visited before (0.0002, 0.0002)
    // The optimal order is (-0.0001, 0) -> (0.0009, 0.0008) -> (0.0005, 0.0006) -> (0.0004, 0.0005) -> (0.0003, 0.0003) -> (0.0002, 0.0002)
    List<CourierStop> expected = Arrays.asList(start.getPoint(), points.get(4), points.get(3), points.get(2),
      points.get(1), points.get(0))
      .stream()
      .map(point -> new CourierStop((Point)point))
      .collect(Collectors.toList());
 
    long totalSeconds = 0;
    for (int i = 0; i < expected.size() - 1; ++ i) {
      totalSeconds += pathFinder.distance(expected.get(i).getPoint(), expected.get(i + 1).getPoint());
    }

    assertTrue(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds + 1) * 1000, "user0")));
    assertFalse(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds - 1) * 1000, "user0")));
    // Check that algorithm finds the optimal order for waypoints.
    assertEquals(expected, journey.findOptimalOrderForWaypoints());
  }

  @Test
  public void testOptimalTimeJourneyFirstPointVisitedLast() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    // For each point, the previous point has latitude and longitude smaller or equal.
    CourierStop start = new CourierStop(new Point(-0.0001, 0));
    Journey journey = new Journey(start, new ManhattanDistancePathFinder());
    List<Point> points = Arrays.asList(new Point(0.0002, 0.0002), 
      new Point(0.0003, 0.0003),
      new Point(0.0004, 0.0005),
      new Point(0.0005, 0.0006), 
      new Point(0.0009, 0.0008));
    for (Point point : points) {
      journey.addPointToWaypoints(point);
    }

    journey.addRestriction(4, 0);
    journey.addRestriction(1, 4);
    journey.addRestriction(3, 4);
    // (0.0009, 0.0008) must be visited before (0.0002, 0.0002) and after (0.0003, 0.0003), (0.0005, 0.0006)
    // The optimal order is (-0.0001, 0) -> (0.0003, 0.0003) -> (0.0004, 0.0005) -> (0.0005, 0.0006) -> (0.0009, 0.0008) -> (0.0002, 0.0002) 
    List<CourierStop> expected = Arrays.asList(start.getPoint(), points.get(1), points.get(2), points.get(3),
      points.get(4), points.get(0))
      .stream()
      .map(point -> new CourierStop((Point)point))
      .collect(Collectors.toList());

    long totalSeconds = 0;
    for (int i = 0; i < expected.size() - 1; ++ i) {
      totalSeconds += pathFinder.distance(expected.get(i).getPoint(), expected.get(i + 1).getPoint());
    }
    
    assertTrue(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds + 1) * 1000, "user0")));
    assertFalse(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds - 1) * 1000, "user0")));
    // Check that algorithm finds the optimal order for waypoints.
    assertEquals(expected, journey.findOptimalOrderForWaypoints());
  }

  @Test
  public void testOptimalTimeJourneyUniqueValidOrder() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    // For each point, the previous point has latitude and longitude smaller or equal.
    CourierStop start = new CourierStop(new Point(-0.0001, 0));
    Journey journey = new Journey(start, new ManhattanDistancePathFinder());
    List<Point> points = Arrays.asList(new Point(0.0002, 0.0002), 
      new Point(0.0003, 0.0003),
      new Point(0.0004, 0.0005),
      new Point(0.0005, 0.0006), 
      new Point(0.0009, 0.0008));
    for (Point point : points) {
      journey.addPointToWaypoints(point);
    }
    List<CourierStop> expected = Arrays.asList(start.getPoint(), points.get(0), points.get(2), points.get(1),
      points.get(4), points.get(3))
      .stream()
      .map(point -> new CourierStop((Point)point))
      .collect(Collectors.toList());

    // indexOrder is a list with the indexes of waypoints in the unique valid order.
    List<Integer> indexOrder = Arrays.asList(0, 2, 1, 4, 3);
    // Add restrictions between every 2 elements in indexOrder so that there will be only one valid ordering.
    for (int i = 0; i < indexOrder.size(); ++ i) {
      for (int j = i + 1; j < indexOrder.size(); ++ j) {
        // indexOrder[i] must be visited before indexOrder[j] for each j > i
        journey.addRestriction(indexOrder.get(i), indexOrder.get(j));
      }
    }
    long totalSeconds = 0;
    for (int i = 0; i < expected.size() - 1; ++ i) {
      totalSeconds += pathFinder.distance(expected.get(i).getPoint(), expected.get(i + 1).getPoint());
    }

    assertTrue(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds + 1) * 1000, "user0")));
    assertFalse(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds - 1) * 1000, "user0")));
    // Check that algorithm finds the optimal order for waypoints.
    assertEquals(expected, journey.findOptimalOrderForWaypoints());
  }

  @Test
  public void testOptimalTimeOneLibrary() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    CourierStop start = new CourierStop(new Point(0.0008, 0));
    Journey journey = new Journey(start, new ManhattanDistancePathFinder());
    // The point that must be visited before all other points("the library") is (0.0005, 0), i.e. 
    // the 2nd element in points.
    List<Point> points = Arrays.asList(new Point(0.0001, 0), 
      new Point(0.0003, 0),
      new Point(0.0005, 0),
      new Point(0.0007, 0));
    for (Point point : points) {
      journey.addPointToWaypoints(point);
    }
    for (int i = 0; i < points.size(); ++ i) {
      if (i != 2) {
        // The 2nd point must be visited before all the others.
        journey.addRestriction(2, i);
      }
    }
    // The optimal order is (0.0008, 0) -> (0.0005, 0) -> (0.0007, 0) -> (0.0003, 0) -> (0.0001, 0)
    List<CourierStop> expected = Arrays.asList(start.getPoint(), points.get(2), points.get(3), points.get(1),
      points.get(0))
      .stream()
      .map(point -> new CourierStop((Point)point))
      .collect(Collectors.toList());
    
    long totalSeconds = 0;
    for (int i = 0; i < expected.size() - 1; ++ i) {
      totalSeconds += pathFinder.distance(expected.get(i).getPoint(), expected.get(i + 1).getPoint());
    }

    assertTrue(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds + 1) * 1000, "user0")));
    assertFalse(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds - 1) * 1000, "user0")));
    // Check that algorithm finds the optimal order for waypoints.
    assertEquals(expected, journey.findOptimalOrderForWaypoints());
  }

  @Test
  public void testOptimalTimeStartPointIsWaypoint() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    // The start point is also a waypoint that has to be visited after the 3rd waypoint, i.e. it 
    // will be visited twice.
    CourierStop start = new CourierStop(new Point(0.0008, 0));
    Journey journey = new Journey(start, new ManhattanDistancePathFinder());
    // The point that must be visited before all other points("the library") is (0.0005, 0), i.e. 
    // the 3rd element in points.
    List<Point> points = Arrays.asList(start.getPoint(),
      new Point(0, 0), 
      new Point(0.0003, 0),
      new Point(0.0005, 0),
      new Point(0.0007, 0));

    for (Point point : points) {
      journey.addPointToWaypoints(point);
    }
    for (int i = 0; i < points.size(); ++ i) {
      if (i != 3) {
        // The 3rd point must be visited before all the others.
        journey.addRestriction(3, i);
      }
    }
    // The optimal order is (0.0008, 0) -> (0.0005, 0) -> (0.0007, 0) -> (0.0008, 0) -> (0.0003, 0) -> (0, 0)
    List<CourierStop> expected = Arrays.asList(start.getPoint(), points.get(3), points.get(4), points.get(0),
      points.get(2), points.get(1))
      .stream()
      .map(point -> new CourierStop((Point)point))
      .collect(Collectors.toList());
    
    long totalSeconds = 0;
    for (int i = 0; i < expected.size() - 1; ++ i) {
      totalSeconds += pathFinder.distance(expected.get(i).getPoint(), expected.get(i + 1).getPoint());
    }
    assertTrue(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds + 1) * 1000, "user0")));
    assertFalse(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, (totalSeconds - 1) * 1000, "user0")));
    // Check that algorithm finds the optimal order for waypoints.
    assertEquals(expected, journey.findOptimalOrderForWaypoints());
  }

  @Test(expected = BadRequestException.class)
  public void testOptimalTimeNoSolution() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    CourierStop start = new CourierStop(new Point(-0.0001, 0));
    Journey journey = new Journey(start, new ManhattanDistancePathFinder());
    List<Point> points = Arrays.asList(new Point(0, 0), 
      new Point(0.0003, 0),
      new Point(0.0005, 0),
      new Point(0.0007, 0));
    for (Point point : points) {
      journey.addPointToWaypoints(point);
    }
    // There is no possible order in which point0 is visited both before and after point1.
    journey.addRestriction(1, 0);
    journey.addRestriction(0, 1);
    long totalSeconds = 3600 * 24;
    assertFalse(journey.findJourneyForTimeslot(new DeliverySlot(new java.util.Date(2020, 26, 9), 0, totalSeconds * 1000, "user0")));
    journey.findOptimalOrderForWaypoints();
  }
}
