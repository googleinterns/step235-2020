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
import com.google.maps.model.LatLng;

import java.io.IOException;
import java.util.Date;
import java.time.LocalTime;

/**
 * Class that represents a delivery timeslot using delivery date, start and end times.
 */
public class DeliverySlot {
  /**
   * Enum with the properties of a delivery slot in datastore.
   */
  public enum Property {
    START_TIME("starTime"),
    END_TIME("endTime"),
    START_LAT("startLat"),
    START_LNG("startLng"),
    USER_ID("uid");

    public final String label;

    private Property(String label) {
      this.label = label;
    }
  }

  // The date and time that the courier starts their delivery run.
  private Date startTime;
  // The date and time that the courier ends their delivery run.
  private Date endTime;
  // The point from where the courier starts their delivery run.
  private Point startPoint;
  // The userId of the courier.
  private String userId;
  // The unique id of this delivery slot.
  private String slotId;

  /**
   * Creates a delivery slot given a date, start and end times in miliseconds and the user id.
   */
  public DeliverySlot(Date deliveryDay, long startTimeInMiliseconds, long endTimeInMiliseconds, String userId) throws BadRequestException {
    if (startTimeInMiliseconds > endTimeInMiliseconds) {
      throw new BadRequestException("Please enter a valid time!");
    }
    if (startTimeInMiliseconds < System.currentTimeMillis() - deliveryDay.getTime()) {
      throw new BadRequestException("Please enter a valid date!");
    }
    this.startTime = new Date(deliveryDay.getTime() + startTimeInMiliseconds);
    this.endTime = new Date(deliveryDay.getTime() + endTimeInMiliseconds);
    this.userId = userId;
  }

  /**
   * Creates a delivery slot given a date, start and end times in miliseconds and the user id.
   */
  public DeliverySlot(Date startTime, Date endTime, String userId) throws BadRequestException {
    if (startTime.compareTo(endTime) > 0) {
      throw new BadRequestException("Please enter a valid time!");
    }
    if (startTime.getTime() < System.currentTimeMillis()) {
      throw new BadRequestException("Please enter a valid date!");
    }
    this.startTime = startTime;
    this.endTime = endTime;
    this.userId = userId;
  }

  /**
   * Given latitude and longitude, sets the coordinates for the starting point of the delivery.
   */
  public void setStartPoint(double latitude, double longitude) throws BadRequestException {
    this.startPoint = new Point(latitude, longitude);
  }

  /**
   * Given a string address, sets the coordinates for the starting point of the delivery.
   */
  public void setStartPoint(String startAddress) throws ApiException, BadRequestException, InterruptedException, IOException, DataNotFoundException {
    this.startPoint = Point.fromAddress(startAddress);
  }

  public Date getStartTime() {
    return startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public String getUserId() {
    return userId;
  }

  public String getSlotId() {
    return slotId;
  }

  public Point getStartPoint(){
    return startPoint;
  }

  public double getStartLatitude() {
    return startPoint.latitude;
  }

  public double getStartLongitude() {
    return startPoint.longitude;
  }

  public void setSlotId(String slotId) {
    this.slotId = slotId;
  }
}
