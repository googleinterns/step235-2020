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

import com.google.sps.data.DeliverySlot;
import java.util.Date; 
import java.time.LocalTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;

/**
 * Tests that DeliverSlot objects are created correctly.
 */
@RunWith(JUnit4.class)
public class DeliverySlotTest {
  @Test
  public void testDeliverySlot() throws BadRequestException {
    int startTimeInMiliseconds = 3600000;
    int endTimeInMiliseconds = 3600000 * 4;
    Date deliveryDay = new Date(2020, 9, 26);
    DeliverySlot slot = new DeliverySlot(deliveryDay, startTimeInMiliseconds, endTimeInMiliseconds, "user0");
    assertEquals(new Date(2020, 9, 26, 1, 0), slot.getStartTime());
    assertEquals(new Date(2020, 9, 26, 4, 0), slot.getEndTime());
  }
}
