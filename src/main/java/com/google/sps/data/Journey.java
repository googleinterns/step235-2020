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
import java.util.Map;
import java.lang.reflect.Array;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.lang.InterruptedException;

import java.util.HashMap;

/**
 * Class that represents a delivery journey.
 */
public class Journey {
  // The number of seconds the journey lasts.
  private int minTime;
  // Array of pairs (a, b) meaning that waypoint with value a in waypoints must be visited before
  // waypoint b.
  private ArrayList<Pair> restrictions;
  // The starting point for the journey.
  private Point start;
  // Hashmap with keys representing Point objects and values representing their index. E.g. when
  // a new Point is added to the hashmap, its value will be the equal to the number of elements.
  // The hashmap is preferred to an ArrayList so that Point objects can be removed efficiently.
  private HashMap<Point, Integer> waypoints;
  // TODO[ak47na]: add PathFinder object, interface and the classes that implement it: 
  // GoogleMapsPathFinder used for production and ManhattanDistancePathFinder for testing.

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
   *  Initializes a journey object that starts at start.
   */
  public Journey(Point starter) {
    this.start = start;
    waypoints = new HashMap<>();
    restrictions = new ArrayList<>();
    minTime = 0;
  }
}
