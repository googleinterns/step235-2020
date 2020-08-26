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

import java.lang.Math;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests the behaviour of a Journey object.
 * TODO[ak47na]: Implement brute-force algorithm that iterates over all permutations of waypoints
 * and finds the optimal one.
 */
@RunWith(JUnit4.class)
public class JourneyTest {
  public int manhattanDistance(Point A, Point B) {
    return (int)(Math.abs(A.latitude - B.latitude) + Math.abs(A.longitude - B.longitude));
  }

  @Test
  public void testOptimalTimeJourney1() {
    Point start = spy(new Point(-1, 0));
    Journey journey = new Journey(start);

    ArrayList<Point> points = new ArrayList<>();
    points.add(spy(new Point(2, 2)));
    points.add(spy(new Point(7, 3)));
    journey.numberOfWaypoints = points.size();

    journey.setWaypointsFromArray(points);
    ArrayList<Pair> restrictions = new ArrayList<>();
    for (int i = 1; i < points.size(); ++ i) {
      restrictions.add(new Pair(0, i));
    }
    journey.setRestrictions(restrictions);
    // mock the time for each pair of points
    for (Point A : points) {
      when(A.getTimeToArriveAtPoint(start)).thenReturn(manhattanDistance(A, start));
      when(start.getTimeToArriveAtPoint(A)).thenReturn(manhattanDistance(start, A));
      for (Point B: points) {
        when(A.getTimeToArriveAtPoint(B)).thenReturn(manhattanDistance(A, B));
      }
    }

    assertTrue(journey.findJourneyForTimeslot(new java.util.Date(), 0, 11));
    assertFalse(journey.findJourneyForTimeslot(new java.util.Date(), 0, 10));
  }

  @Test
  public void testOptimalTimeJourney2() {
    Point start = spy(new Point(-1, 0));
    Journey journey = new Journey(start);

    ArrayList<Point> points = new ArrayList<>();
    points.add(spy(new Point(2, 2)));
    points.add(spy(new Point(7, 3)));
    journey.numberOfWaypoints = points.size();

    journey.setWaypointsFromArray(points);
    ArrayList<Pair> restrictions = new ArrayList<>();
    for (int i = 1; i < points.size(); ++ i) {
      restrictions.add(new Pair(i, 0));
    }
    journey.setRestrictions(restrictions);
    // mock the time for each pair of points
    for (Point A : points) {
      when(A.getTimeToArriveAtPoint(start)).thenReturn(manhattanDistance(A, start));
      when(start.getTimeToArriveAtPoint(A)).thenReturn(manhattanDistance(start, A));
      for (Point B: points) {
        when(A.getTimeToArriveAtPoint(B)).thenReturn(manhattanDistance(A, B));
      }
    }

    assertTrue(journey.findJourneyForTimeslot(new java.util.Date(), 0, 17));
    assertFalse(journey.findJourneyForTimeslot(new java.util.Date(), 0, 16));
  }

  @Test
  public void testOptimalTimeJourneySortedOrder() {
    // for each point, the previous point has latitude and longitude smaller or equal
    Point start = spy(new Point(-1, 0));
    Journey journey = new Journey(start);

    ArrayList<Point> points = new ArrayList<>();
    points.add(spy(new Point(2, 2)));
    points.add(spy(new Point(3, 3)));
    points.add(spy(new Point(4, 5)));
    points.add(spy(new Point(5, 6)));
    points.add(spy(new Point(9, 8)));

    journey.numberOfWaypoints = points.size();

    journey.setWaypointsFromArray(points);
    ArrayList<Pair> restrictions = new ArrayList<>();
    for (int i = 1; i < points.size(); ++ i) {
      restrictions.add(new Pair(0, i));
    }
    journey.setRestrictions(restrictions);
    // mock the time for each pair of points
    for (Point A : points) {
      when(A.getTimeToArriveAtPoint(start)).thenReturn(manhattanDistance(A, start));
      when(start.getTimeToArriveAtPoint(A)).thenReturn(manhattanDistance(start, A));
      for (Point B: points) {
        when(A.getTimeToArriveAtPoint(B)).thenReturn(manhattanDistance(A, B));
      }
    }
    // the result should be the manhattan distance between (-1, 0) and (9, 8) = 18
    assertTrue(journey.findJourneyForTimeslot(new java.util.Date(), 0, 18));
    assertFalse(journey.findJourneyForTimeslot(new java.util.Date(), 0, 17));
  }

  @Test
  public void testOptimalTimeJourneySortedOrderRestrictions() {
    // for each point, the previous point has latitude and longitude smaller or equal
    Point start = spy(new Point(-1, 0));
    Journey journey = new Journey(start);

    ArrayList<Point> points = new ArrayList<>();
    points.add(spy(new Point(2, 2)));
    points.add(spy(new Point(3, 3)));
    points.add(spy(new Point(4, 5)));
    points.add(spy(new Point(5, 6)));
    points.add(spy(new Point(9, 8)));

    journey.numberOfWaypoints = points.size();

    journey.setWaypointsFromArray(points);
    ArrayList<Pair> restrictions = new ArrayList<>();
    restrictions.add(new Pair(4, 0));
    // (9, 8) must be visited before (2, 2)

    journey.setRestrictions(restrictions);
    // mock the time for each pair of points
    for (Point A : points) {
      when(A.getTimeToArriveAtPoint(start)).thenReturn(manhattanDistance(A, start));
      when(start.getTimeToArriveAtPoint(A)).thenReturn(manhattanDistance(start, A));
      for (Point B: points) {
        when(A.getTimeToArriveAtPoint(B)).thenReturn(manhattanDistance(A, B));
      }
    }
    // the optimal order is (-1, 0) -> (3, 3) -> (4, 5) -> (5, 6) -> (9, 8) -> (2, 2) = 31
    assertTrue(journey.findJourneyForTimeslot(new java.util.Date(), 0, 31));
    assertFalse(journey.findJourneyForTimeslot(new java.util.Date(), 0, 30));
  }
}
