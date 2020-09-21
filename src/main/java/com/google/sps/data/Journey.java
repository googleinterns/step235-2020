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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.lang.reflect.Array;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.lang.InterruptedException;
import java.time.Duration;
import java.time.Instant;

/**
 * Class that represents a delivery journey.
 */
public class Journey {
  // The number of seconds the journey lasts.
  private int minTime;
  // Array of pairs (a, b) meaning that waypoint with index a in waypoints must be visited before
  // waypoint with index b.
  private ArrayList<Pair> restrictions;
  // The starting point for the journey.
  private CourierStop start;
  // Array of Point objects representing the waypoints that must be visited not including the start
  // point. The index of each waypoint in the array will be used to mark order restrictions.
  private List<CourierStop> waypoints;
  // Array of encoded string representations of the keys of orders from Datastore assigned to the journey.
  private List<String> orderKeys;
  // PathFinder Object that finds paths between 2 points by calling Directions API.
  private PathFinder pathFinder;

  /**
   * Helper class used for computing the bestTime matrix and order restrictions between pairs of
   * waypoints.
   */
  private class Pair {
    public int first;
    public int second;
    public Pair(int first, int second) {
      this.first = first;
      this.second = second;
    }
  }

  /**
   *  Initializes a journey object that starts at start and computes paths using pathFinder.
   */
  public Journey(CourierStop start, PathFinder pathFinder) {
    this.start = start;
    this.pathFinder = pathFinder;
    waypoints = new ArrayList<>();
    restrictions = new ArrayList<>();
    orderKeys = new ArrayList<>();
    minTime = 0;
  }

  public int getNumberOfWaypoints() {
    throw new UnsupportedOperationException("TODO: Implement this method.");
  }

  public List<String> getOrders() {
    return orderKeys;
  }

  /**
   * Returns true if the minimum time journey fits in the timeslot and false otherwise.
   */
  public boolean findJourneyForTimeslot(DeliverySlot deliverySlot) throws ApiException, IOException, InterruptedException {
    throw new UnsupportedOperationException("TODO: Implement this method.");
  }

  /**
   * Computes and returns the optimal order in which points should be visited starting from the
   * last point to the fist one (the start Point).
   */
  public ArrayList<CourierStop> findOptimalOrderForWaypoints() {
    throw new UnsupportedOperationException("TODO: Implement this method.");
  }

  public void addRestriction(int first, int second) {
    throw new UnsupportedOperationException("TODO: Implement this method.");
  }

  public void addOrder(String orderKey, Point library, Point recipient) throws DataNotFoundException {
    orderKeys.add(orderKey);
    // Add the order key to the library and recipient waypoints.
    waypoints.get(getWaypointIndex(library)).addOrderKey(orderKey);
    waypoints.get(getWaypointIndex(recipient)).addOrderKey(orderKey);
  }

  /**
   * Returns the index of point in waypoints array.
   */
  public int getWaypointIndex(Point point) throws DataNotFoundException {
    throw new UnsupportedOperationException("TODO: Implement this method.");
  }

  /**
   * Returns false if the point is already in the waypoints List or it adds it to the List
   * and returns true.
   */
  public boolean addPointToWaypoints(Point point) {
    throw new UnsupportedOperationException("TODO: Implement this method.");
  }

  /**
   * Removes point from waypoints and decreases the number of waypoints.
   */
  public void removeWaypoint(Point point) {
    throw new UnsupportedOperationException("TODO: Implement this method.");
  }
}
