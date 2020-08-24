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

import com.google.maps.model.LatLng;
import com.google.sps.data.MapsRequest;

/**
 * Class that represents points on the map using latitude and longitude coordinates.
 */
public class Point {
  public double latitude;
  public double longitude;
  public Point(String address) {
    LatLng point = MapsRequest.getLocationFromAddress(address);
    this.latitude = point.lat;
    this.longitude = point.lng;
  }
  public Point(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  /**
   * Returns the area of the point from its latitude and longitude.
   */
  public int getArea() {
    return 1;
  }
}
