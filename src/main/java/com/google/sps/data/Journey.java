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

import java.lang.reflect.Array;
import java.util.*;

/**
 * Class that represents a delivery journey.
 */
public class Journey {
  ArrayList<Pair> restrictions;
  HashMap<Point, Integer> waypoints;
  int numberOfWaypoints;
  int minTime;
  ArrayList<Point> orderedWaypoints;
  Point start;

  /**
   *  Initializes a journey object that starts at startPoint
   */
  public Journey(Point startPoint) {
    restrictions = new ArrayList<>();
    waypoints = new HashMap<>();
    minTime = Integer.MAX_VALUE;
    numberOfWaypoints = 0;
    start = startPoint;
  }

  private ArrayList<Point> getWaypointsList() {
    ArrayList<Point> waypointsList = new ArrayList<>();
    waypointsList.addAll(Collections.nCopies(numberOfWaypoints, start));
    for (Map.Entry<Point, Integer> entry : waypoints.entrySet()) {
      waypointsList.set(entry.getValue(), entry.getKey());
    }
    return waypointsList;
  }

  private void printMatrix(ArrayList<ArrayList<Pair>> bestTime) {
    int allWaypointsConfig = (1 << numberOfWaypoints) - 1;
    for (int config = 0; config <= allWaypointsConfig; ++ config) {
      for (int i = 0; i < numberOfWaypoints; ++ i) {
        System.out.print(bestTime.get(config).get(i).first);
        System.out.print("*");
      }
      System.out.println();
    }
  }

  /**
   * Returns the reverse order in which the waypoints must be visited for an optimal journey.
   */
  public ArrayList<Point> getOrderedWaypoints() {
    return orderedWaypoints;
  }

  /**
   * Computes for each waypoint i, pointsAfter[i] = bitmask where bits set to 1 represent the
   * indices of points that must be visited after point i
   */
  public ArrayList<Integer> getPointsAfterFromRestricitions() {
    ArrayList<Integer> pointsAfter = new ArrayList<>(Collections.nCopies(numberOfWaypoints, 0));
    for (Pair restriction : restrictions) {
      // restriction.second must be visited after restriction.first
      int currentBitmask = pointsAfter.get(restriction.first);
      pointsAfter.set(restriction.first, currentBitmask | (1 << restriction.second));
    }
    return pointsAfter;
  }

  /**
   * Given bestTime matrix, it computes and returns the optimal order in which points should be
   * visited starting from the last point.
   */
  public ArrayList<Point> getOrderFromMatrix(ArrayList<ArrayList<Pair>> bestTime) {
    ArrayList<Point> orderedWaypoints = new ArrayList<>();
    // the last state of the matrix has all waypoints set to 1
    int currentState =  (1 << numberOfWaypoints) - 1;
    // get waypointsList to access the keys from waypoints given their values
    ArrayList<Point> waypointsList = getWaypointsList();
    int minTime = bestTime.get((1 << numberOfWaypoints)).get(0).first;
    // currentWaypoint is the one for which bestTime[currentState][currentWaypoint] is minimal
    int currentWaypoint = 0;
    for (int waypointIndex = 1; waypointIndex < numberOfWaypoints; ++waypointIndex) {
      if (bestTime.get(currentState).get(waypointIndex).first < minTime) {
        minTime = bestTime.get(currentState).get(waypointIndex).first;
        currentWaypoint = waypointIndex;
      }
    }
    while (currentState != 0) {
      orderedWaypoints.add(waypointsList.get(currentWaypoint));
      int prevState = currentState ^ (1 << currentWaypoint);
      int prevWaypoint = bestTime.get(currentState).get(currentWaypoint).second;
      currentWaypoint = prevWaypoint;
      currentState = prevState;
    }
    return orderedWaypoints;
  }

  /**
   * Using the bestTime matrix computed in findOptimalTimePaths(), it returns true if the minimum
   * time journey fits in the timeslot and false otherwise
   */
  public boolean findJourneyForTimeslot(Date deliveryDay, int startTime, int endTime) {
    ArrayList<ArrayList<Pair>> bestTime = findOptimalTimePaths();
    int allWaypointsConfig = (1 << numberOfWaypoints) - 1;
    int minTime = bestTime.get((1 << numberOfWaypoints)).get(0).first;
    for (int waypointIndex = 0; waypointIndex < numberOfWaypoints; ++waypointIndex) {
      if (bestTime.get(allWaypointsConfig).get(waypointIndex).first < minTime) {
        minTime = bestTime.get(allWaypointsConfig).get(waypointIndex).first;
      }
    }

    return (minTime <= endTime - startTime);
  }

  /**
   * Creates and initializes bestTime matrix, a matrix where bestTime[config][lastWaypoint] = the
   * minimum time to visit all points with bits set to 1 in config such that the last visited point
   * is lastWaypoint; config is a bitmask where bits set to 1 represent currently visited waypoints
   */
  public ArrayList<ArrayList<Pair>> initializeBestTimeMatrix() {
    ArrayList<ArrayList<Pair>> bestTime = new ArrayList<>();
    for (int config = 0; config <= (1 << numberOfWaypoints); ++config) {
      // add the config-th line to the matrix with numberOfWaypoints columns of zeros
      ArrayList<Pair> currentRow = new ArrayList<Pair>();

      for (int waypointIndex = 0; waypointIndex <= numberOfWaypoints; ++ waypointIndex) {
        currentRow.add(new Pair(Integer.MAX_VALUE, -1));
      }
      bestTime.add(currentRow);
    }

    int waypointIndex = 0;
    ArrayList <Point> waypointsList = getWaypointsList();
    for (Point waypoint : waypointsList) {
      Pair currentElement = bestTime.get(1 << waypointIndex).get(waypointIndex);
      currentElement.first = start.getTimeToArriveAtPoint(waypoint);
      waypointIndex += 1;
    }

    return bestTime;
  }

  /**
   * Computes and returns the bestTime matrix such that bestTime[config][latsWaypoint] is the min
   * time needed to visit all points with bits set to 1 in config such that the path ends in point
   * lastWaypoint.
   */
  public ArrayList<ArrayList<Pair>> findOptimalTimePaths() {
    ArrayList<ArrayList<Pair>> bestTime = initializeBestTimeMatrix();
    ArrayList<Integer> pointsAfter = getPointsAfterFromRestricitions();
    int allWaypointsConfig = (1 << numberOfWaypoints) - 1;
    // get waypointsList to access the keys from waypoints given their values
    ArrayList<Point> waypointsList = getWaypointsList();
    // start with config = 1 so that at least 1 point is set to 1 in config
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
              // TODO[ak47na]: handle case when getTimeBetween2Points returns -1
              if (bestTime.get(config).get(lastWaypoint).first == Integer.MAX_VALUE) {
                // the state is not valid, thus it should be ignored
                continue;
              }
              int newJourneyTime = bestTime.get(config).get(lastWaypoint).first +
                  waypointsList.get(lastWaypoint).getTimeToArriveAtPoint(waypointsList.get(waypointIndex));
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

  /**
   * Takes an array of Point objects and sets them as the keys of the waypoints HashMap. The values
   * of the HashMap are the indexes of the keys in points array. The method is used for testing.
   */
  public void setWaypointsFromArray(ArrayList<Point> points) {
    // clear the hashmap in case so that it only contains the given points
    waypoints.clear();
    int pointIndex = 0;
    for (Point point : points) {
      waypoints.put(point, pointIndex);
      // go to the next index
      pointIndex += 1;
    }
  }

  /**
   * Takes an array of Pair objects (a, b) representing that point with value a must be visited
   * before point with value b in waypoints HashMap and sets it to be the restrictions array for
   * current journey. The method is used for testing.
   */
  public void setRestrictions(ArrayList<Pair> newRestrictions) {
    restrictions.clear();
    restrictions.addAll(newRestrictions);
  }
}
