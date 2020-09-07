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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.errors.ApiException;
import com.google.maps.GeoApiContext;
import com.google.maps.GeoApiContext.Builder;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

/** 
 * Class that create a GeoApiContext variable and handles requests to maps API.
 */
public class MapsRequest {
  private static GeoApiContext geoApiContext = null;

  /** 
   * Returns the geoApiContext or creates one if it wasn't already created.
   */
  public static GeoApiContext getGeoApiContext() {
    if (geoApiContext == null) {
      geoApiContext = new GeoApiContext
          .Builder()
          .apiKey("")
          .build();
    }
    return geoApiContext;
  }

  /**
   * Makes a request to Geocoding API and returns the latitude and longitude associated with the
   * address String.
   */
  public static LatLng getLocationFromAddress(String address) {
    try {
      GeocodingResult[] results =  GeocodingApi.geocode(getGeoApiContext(), address).await();
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      return results[0].geometry.location;
    } catch(Exception e) {
      return null;
    }
  }
}
