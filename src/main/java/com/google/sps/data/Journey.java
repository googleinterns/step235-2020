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
import java.util.HashMap;

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
    return waypoints.size();
  }
  public ArrayList<Pair> getRestrictions() {
    return restrictions;
  }
  public List<CourierStop> getWaypoints() {
    return waypoints;
  }

  public List<String> getOrders() {
    return orderKeys;
  }

  /**
   * Returns true if the minimum time journey fits in the timeslot and false otherwise.
   */
  public boolean findJourneyForTimeslot(DeliverySlot deliverySlot) throws ApiException, DataNotFoundException, IOException, InterruptedException {
    ArrayList<ArrayList<Pair>> bestTime = findOptimalTimePaths();
    int numberOfWaypoints = getNumberOfWaypoints();
    int allWaypointsConfig = (1 << numberOfWaypoints) - 1;
    int minTime = bestTime.get(allWaypointsConfig).get(0).first;
    for (int waypointIndex = 0; waypointIndex < numberOfWaypoints; ++waypointIndex) {
      if (bestTime.get(allWaypointsConfig).get(waypointIndex).first < minTime) {
        minTime = bestTime.get(allWaypointsConfig).get(waypointIndex).first;
      }
    }

    long deliverySlotDurationInSeconds = Duration.between(deliverySlot.getStartTime().toInstant(),
        deliverySlot.getEndTime().toInstant()).getSeconds();

    return (minTime <= (int)deliverySlotDurationInSeconds);
  }

  /**
   * Computes and returns the optimal order in which points should be visited starting from the
   * first point to the last one.
   */
  public ArrayList<CourierStop> findOptimalOrderForWaypoints() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    ArrayList<ArrayList<Pair>> bestTime = findOptimalTimePaths();
    ArrayList<CourierStop> orderedWaypoints = new ArrayList<>();
    int numberOfWaypoints = getNumberOfWaypoints();
    
    if (numberOfWaypoints == 0) {
      // Return empty array if there are no waypoints.
      return orderedWaypoints;
    }
    // the last state of the matrix has all waypoints set to 1
    int currentState =  (1 << numberOfWaypoints) - 1;
    
    int minTime = bestTime.get(currentState).get(0).first;
    // currentWaypoint is the one for which bestTime[currentState][currentWaypoint] is minimal
    int currentWaypoint = 0;
    for (int waypointIndex = 1; waypointIndex < numberOfWaypoints; ++waypointIndex) {
      if (bestTime.get(currentState).get(waypointIndex).first < minTime) {
        minTime = bestTime.get(currentState).get(waypointIndex).first;
        currentWaypoint = waypointIndex;
      }
    }
    
    if (minTime == Integer.MAX_VALUE) {
      // No solution was found.
      throw new BadRequestException("Impossible to create journey with the given constraints!");
    }
    
    while (currentState != 0) {
      orderedWaypoints.add(waypoints.get(currentWaypoint));
      int prevState = currentState ^ (1 << currentWaypoint);
      int prevWaypoint = bestTime.get(currentState).get(currentWaypoint).second;
      currentWaypoint = prevWaypoint;
      currentState = prevState;
    }
    // add the start of the journey at the end of orderedWaypoints.
    orderedWaypoints.add(start);
    Collections.reverse(orderedWaypoints);
    return orderedWaypoints;
  }

  /**
   * Creates and initializes bestTime matrix, a matrix where bestTime[config][lastWaypoint] = the
   * minimum time to visit all points with bits set to 1 in config such that the last visited point
   * is lastWaypoint; config is a bitmask where bits set to 1 represent currently visited waypoints.
   */
  private ArrayList<ArrayList<Pair>> initializeBestTimeMatrix() throws ApiException, IOException, InterruptedException, DataNotFoundException {
    ArrayList<ArrayList<Pair>> bestTime = new ArrayList<>();
    int numberOfWaypoints = getNumberOfWaypoints();
    for (int config = 0; config < (1 << numberOfWaypoints); ++config) {
      // add the config-th line to the matrix with numberOfWaypoints columns of Integer.MAX_VALUE
      ArrayList<Pair> currentRow = new ArrayList<Pair>();

      for (int waypointIndex = 0; waypointIndex < numberOfWaypoints; ++ waypointIndex) {
        currentRow.add(new Pair(Integer.MAX_VALUE, -1));
      }
      bestTime.add(currentRow);
    }

    int waypointIndex = 0;
    for (CourierStop waypoint : waypoints) {
      Pair currentElement = bestTime.get(1 << waypointIndex).get(waypointIndex);
      currentElement.first = pathFinder.getTimeInSecondsBetweenPoints(start.getPoint(), waypoint.getPoint());
      waypointIndex += 1;
    }

    return bestTime;
  }

  /**
   * Computes and returns the bestTime matrix such that bestTime[config][latsWaypoint].first is the
   * min time needed to visit all points with bits set to 1 in config such that the path ends in 
   * lastWaypoint and bestTime[config][latsWaypoint].second represents the point that was visited 
   * right before lastWaypoint. 
   *
   * for each configuration config from 1 to (1<<(numberOfWaypoints - 1)) - 1, AND 
   *   for each waypointIndex i from 0 to numberOfWaypoints - 1 that is not set to 1 in config
   *   AND there is no index k set to 1 in config such that waypoint k must be visited after i
   *     newConfig = config + (1<<i), i.e. newConfig has bits in config plus the i-th bit set to 1
   *     update bestTime[newConfig][i] with each matrix element bestTime[config][j] for which
   *     j is set to 1 in config, i.e. consider each index j set to 1 in config as the last visited
   *     waypoint before i and update bestTime[newConfig][i].first with bestTime[config][j].first +
   *     the time to get from waypoint j to waypoint i.
   *
   *     The recurrence is: 
   *     bestTime[newConfig][i].first = min(bestTime[config][j].first + 
   *       getTimeInSecondsBetweenPoints(waypoint[j], waypoint[i]))
   *     bestTime[newConfig][j].second = j, such that bestTime[newConfig][i].first ==
   *       bestTime[config][j].first + getTimeInSecondsBetweenPoints(waypoint[j], waypoint[i])   
   */
  public ArrayList<ArrayList<Pair>> findOptimalTimePaths()  throws ApiException, DataNotFoundException, IOException, InterruptedException {
    ArrayList<ArrayList<Pair>> bestTime = initializeBestTimeMatrix();
    ArrayList<Integer> pointsAfter = getPointsAfterFromRestrictions();
    int numberOfWaypoints = getNumberOfWaypoints();
    int allWaypointsConfig = (1 << numberOfWaypoints) - 1;
    // Start with config = 1, since config=0 means no points should be visited, so nothing to do.
    for (int config = 1; config < allWaypointsConfig; ++config) {
      for (int waypointIndex = 0; waypointIndex < numberOfWaypoints; ++ waypointIndex) {
        // check for waypointIndex, the next unvisited point for which none of the points that must
        // be visited after him have been visited.
        // TODO[ak47na]: add pointsBefore array and check that all points that must be visited
        // before the current point have already been visited.
        if ((config & (1 << waypointIndex)) == 0 && ((pointsAfter.get(waypointIndex) & config) == 0)) {
          // Set bit wayPointIndex to 1 in newConfig if waypointIndex is set to zero in config AND
          // all restrictions for waypointIndex are satisfied
          int newConfig = config | (1 << waypointIndex);
          for (int lastWaypoint = 0; lastWaypoint < numberOfWaypoints; ++lastWaypoint) {
            // the lastWaypoint visited in config must be set to 1
            if ((config & (1 << lastWaypoint)) != 0) {
              // add the time needed to get from lastWaypoint to waypointIndex to bestTime[config][lastWaypoint]
              if (bestTime.get(config).get(lastWaypoint).first == Integer.MAX_VALUE) {
                // the state is not valid, thus it should be ignored
                continue;
              }
              int newJourneyTime = bestTime.get(config).get(lastWaypoint).first +
                  pathFinder.getTimeInSecondsBetweenPoints(waypoints.get(lastWaypoint).getPoint(), waypoints.get(waypointIndex).getPoint());
              if (newJourneyTime < bestTime.get(newConfig).get(waypointIndex).first) {
                // update the time need for visiting all points in newConfig, ending in waypointIndex
                bestTime.get(newConfig).set(waypointIndex, new Pair(newJourneyTime, lastWaypoint));
              }
            }
          }
        }
      }
    }
    return bestTime;
  }

  public void addRestriction(int first, int second) {
    restrictions.add(new Pair(first, second));
  }

  public void addOrder(String orderKey, LibraryPoint library, Point recipient) throws DataNotFoundException {
    orderKeys.add(orderKey);
    // Add the order key to the library and recipient waypoints.
    waypoints.get(getWaypointIndex(library)).addOrderKey(orderKey);
    waypoints.get(getWaypointIndex(recipient)).addOrderKey(orderKey);
  }

  /**
   * Computes for each waypoint i, pointsAfter[i] = bitmask where bits set to 1 represent the
   * indices of points that must be visited after point i
   */
  private ArrayList<Integer> getPointsAfterFromRestrictions() {
    ArrayList<Integer> pointsAfter = new ArrayList<>(Collections.nCopies(getNumberOfWaypoints(), 0));
    for (Pair restriction : restrictions) {
      // restriction.second must be visited after restriction.first
      int currentBitmask = pointsAfter.get(restriction.first);
      pointsAfter.set(restriction.first, currentBitmask | (1 << restriction.second));
    }
    return pointsAfter;
  }

  /**
   * Returns the index of point in waypoints array.
   */
  public int getWaypointIndex(Point point) throws DataNotFoundException {
    for (int index = 0; index < waypoints.size(); ++ index) {
      if (point.equals(waypoints.get(index).getPoint())) {
        return index;
      }
    }
    throw new DataNotFoundException("The point is not a waypoint!");
  }

  /**
   * Returns false if the point is already in the waypoints list or it adds it to the list and
   * returns true.
   */
  public boolean addPointToWaypoints(Point point) {
    try {
      int index = getWaypointIndex(point);
      // The point is already a waypoint
      return false;
    } catch(DataNotFoundException e) {
      // Add the point at the end of waypoints array.
      waypoints.add(new CourierStop(point));
      return true;
    }
  }

  /**
   * Removes point from waypoints and decreases the number of waypoints.
   */
  public void removeWaypoint(Point point) throws DataNotFoundException {
    waypoints.remove(getWaypointIndex(point));
  }
}
