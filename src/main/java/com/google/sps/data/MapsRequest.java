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
import com.google.maps.DirectionsApi;
import com.google.maps.errors.ApiException;
import com.google.maps.GeoApiContext;
import com.google.maps.GeoApiContext.Builder;
import com.google.maps.GeocodingApi;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.Duration;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.TravelMode;
import com.google.maps.model.LatLng;
import java.io.IOException;

/**
 * Class that creates a GeoApiContext variable and handles requests to maps API.
 */
public class MapsRequest {
  private static GeoApiContext geoApiContext = null;
  public static GeoApiContext getGeoApiContext() {
    // TODO[ak47na]: provide Maps API key
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
  public static LatLng getLocationFromAddress(String address) throws IOException, ApiException, InterruptedException, DataNotFoundException {
    GeocodingResult[] results =  GeocodingApi.geocode(getGeoApiContext(), address).await();
    return getLocationFromResult(results);
  }

  public static LatLng getLocationFromResult(GeocodingResult[] results) throws DataNotFoundException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    if (results == null || results.length == 0) {
      throw new DataNotFoundException("GeocodingApi was unable to find the address!");
    }
    return results[0].geometry.location;
  }

  /**
   * Makes a request to Geocoding API and returns the duration in seconds to get from point A to point B
   * TODO[ak47na]: update function to get the duration if the journey starts at a given time.
   * TODO[ak47na]: add function parameter to support multiple transportation modes.
   */
  public static int getTimeInSecondsBetween2Points(Point a, Point b) throws ApiException, IOException, InterruptedException, DataNotFoundException{
    DirectionsResult result = DirectionsApi.newRequest(getGeoApiContext())
        .origin(new LatLng(a.latitude, a.longitude))
        .destination(new LatLng(b.latitude, b.longitude))
        .mode(TravelMode.DRIVING)
        .await();
    return getDurationFromDirectionsResult(result);
  }

  public static int getDurationFromDirectionsResult(DirectionsResult result) throws ApiException, IOException, InterruptedException, DataNotFoundException {
    int timeInSeconds = 0;
    if (result == null || result.routes.length == 0) {
      throw new DataNotFoundException("DirectionsApI was unable to find a path between chosen points!");
    }
    for (DirectionsLeg leg : result.routes[0].legs) {
      timeInSeconds += leg.duration.inSeconds;
    }
    return timeInSeconds;
  }
}
