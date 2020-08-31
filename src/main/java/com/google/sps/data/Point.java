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
import com.google.maps.errors.ApiException;
import com.google.sps.data.MapsRequest;
import java.io.IOException;
import java.lang.InterruptedException;

/**
 * Class that represents points on the map using latitude and longitude coordinates.
 */
public class Point {
  public double latitude;
  public double longitude;

  /** 
   * Given an address, the function requests the coordinates corresponding to it from Geolocation
   * API. Then it creates and returns a Point object.
   */
  public static Point fromAddress(String address) throws ApiException, IOException, InterruptedException {
    LatLng point = MapsRequest.getLocationFromAddress(address);
    return new Point(point.lat, point.lng);
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

  /**
   * Returns true if .this Point has latitude and longitude equal to the ones of B
   */
  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Point)) {
      return false;
    }
    Point B = (Point) object;
    return (this.latitude == B.latitude) && (this.longitude == B.longitude);
  }

  @Override
  public int hashCode() {
    int value = (int)(10000.0 * this.latitude) * 90 + (int)(10000.0 * this.longitude);
    return value;
  }
}
