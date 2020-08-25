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
import java.util.Date; 

/** 
 * Class that represents a delivery timeslot using delivery date, start and end times.
 */
public class DeliverySlot {
  private Date deliveryDay;
  private int startTime;
  private int endTime;
  /** 
   * Creates a delivery slot given a date, and start and end times. 
   */
  public DeliverySlot(Date deliveryDay, int startTime, int endTime) {
    this.deliveryDay = deliveryDay;
    this.startTime = startTime;
    this.endTime = endTime;
  }
  public Date getDeliveryDay() {
    return deliveryDay;
  }
  public int getStartTime() {
    return startTime;
  }
  public int getEndTime() {
    return endTime;
  }
}