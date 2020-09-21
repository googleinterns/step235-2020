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
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.Duration;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.Geometry;
import com.google.maps.model.TravelMode;
import com.google.maps.model.LatLng;
import com.google.sps.data.DeliverySlot;
import java.io.IOException;
import java.util.Date;
import java.time.LocalTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests that responses from Google Maps API are parsed successfully.
 */
@RunWith(JUnit4.class)
public class MapsRequestTest {
  public DirectionsLeg getDirectionsLeg(int timeInSeconds) {
    DirectionsLeg leg = new DirectionsLeg();
    leg.duration = new Duration();
    leg.duration.inSeconds = timeInSeconds;
    return leg;
  }

  @Test
  public void testDirectionsBetween2Points() throws BadRequestException, InterruptedException, DataNotFoundException, ApiException, IOException {
    DirectionsResult result = new DirectionsResult();
    result.routes = new DirectionsRoute[1];
    result.routes[0] = new DirectionsRoute();
    result.routes[0].legs = new DirectionsLeg[2];
    result.routes[0].legs[0] = getDirectionsLeg(10);
    result.routes[0].legs[1] = getDirectionsLeg(15);
    Assert.assertEquals(25, MapsRequest.getDurationFromDirectionsResult(result));
  }

  @Test(expected = DataNotFoundException.class)
  public void testUnsuccessfulDirectionsRequest() throws BadRequestException, InterruptedException, DataNotFoundException, ApiException, IOException {
    DirectionsResult result = new DirectionsResult();
    result.routes = new DirectionsRoute[0];
    MapsRequest.getDurationFromDirectionsResult(result);
  }

  @Test
  public void testGeoCodingResult() throws DataNotFoundException {
    GeocodingResult[] results = new GeocodingResult[1];
    results[0] = new GeocodingResult();
    results[0].geometry = new Geometry();
    results[0].geometry.location = new LatLng(0.1, 0.1);
    Assert.assertEquals(results[0].geometry.location, MapsRequest.getLocationFromResult(results));
  }
}
