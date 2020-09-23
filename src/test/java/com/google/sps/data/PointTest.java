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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/** 
 * Classed used for testing Point objects;
 */
@RunWith(JUnit4.class)
public class PointTest {
  @Test
  public void testEquals() throws BadRequestException {
    // Two points are equal if both of their coordinates match up to the 4th decimal.
    Point point1 = new Point(-19.123401, 10.1234);
    Point point2 = new Point(-19.123405, 10.1234);
    Point point3 = new Point(19, 10);
    Assert.assertTrue(point1.equals(point2));
    Assert.assertFalse(point2.equals(point3));
    Assert.assertFalse(point1.equals(point3));
  }

  @Test
  public void testHashCode() throws BadRequestException {
    Point point1 = new Point(-19.1234, 10.1234);
    Point point2 = new Point(-19.12345, 10.1234);
    Point point3 = new Point(-19.1236, 10.1234);

    Assert.assertEquals(point1.hashCode(), point2.hashCode());
    Assert.assertNotEquals(point1.hashCode(), point3.hashCode());
  }

  @Test(expected = BadRequestException.class) 
  public void testInvalidLatitude() throws BadRequestException{
    Point point = new Point(90.1234, 10.1234);
  }

  @Test(expected = BadRequestException.class)
  public void testInvalidLongitude() throws BadRequestException{
    Point point = new Point(0.1234, 180.1234);
  }

  @Test(expected = BadRequestException.class) 
  public void testInvalidLatitude2() throws BadRequestException{
    Point point = new Point(-90.1234, 10.1234);
  }

  @Test(expected = BadRequestException.class)
  public void testInvalidLongitude2() throws BadRequestException{
    Point point = new Point(0.1234, -180.1234);
  }
}
