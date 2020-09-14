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

public class ManhattanDistanceTest{
  private ManhattanDistancePathFinder manhattanDistancePathFinder = new ManhattanDistancePathFinder();
  @Test
  public void testDistanceIntegers() throws BadRequestException {
    Point point1 = new Point(-1, 2);
    Point point2 = new Point(5, 3);
    Assert.assertEquals(700000, manhattanDistancePathFinder.distance(point1, point2));
  }

  @Test
  public void testDistanceDoubles() throws BadRequestException {
    Point point1 = new Point(-1.23, 2.4);
    Point point2 = new Point(5, 3.2);
    int distance = (int)((5 - (-1.23) + (3.2 - 2.4)) * 100000);
    Assert.assertEquals(distance, manhattanDistancePathFinder.distance(point1, point2));
  }

  @Test
  public void testCloseDoubles() throws BadRequestException {
    Point point1 = new Point(51.5206431, -0.135168);
    Point point2 = new Point(51.5134121, -0.1407977);
    int distance = (int)(0.012860700000004416 * 100000);
    Assert.assertEquals(distance, manhattanDistancePathFinder.distance(point1, point2));
  }
}
