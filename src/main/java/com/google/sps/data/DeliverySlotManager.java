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

import com.google.api.gax.rpc.NotFoundException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.model.LatLng;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Class that manages delivery slots.
 */
public class DeliverySlotManager {

  public class CreateSlotManager {
    private int timezoneOffset;
    private DeliverySlot deliverySlot;
    private int maxStops;
    private double latitude;
    private double longitude;

    public CreateSlotManager(String deliveryDay, String timezoneOffsetMinutes, String startTime, String endTime, String maxStops) throws BadRequestException {
      this.timezoneOffset = parseInteger(timezoneOffsetMinutes) * 60;
      int startTimeSeconds = getNumberOfSeconds(parseTime(startTime)) + this.timezoneOffset;
      int endTimeSeconds = getNumberOfSeconds(parseTime(endTime)) + this.timezoneOffset;
      if (startTimeSeconds > endTimeSeconds) {
        throw new BadRequestException("Please enter a valid time!");
      }
      this.deliverySlot = new DeliverySlot(validateDate(deliveryDay, startTimeSeconds), startTimeSeconds, endTimeSeconds);
      this.maxStops = parseInteger(maxStops);
      if (this.maxStops > 25 || this.maxStops <= 0) {
        throw new BadRequestException("Please enter between 1 and 25 stops!");
      }
    }

    public void setCoordinates(String latitude, String longitude) throws BadRequestException {
      this.latitude = parseDouble(latitude);
      this.longitude = parseDouble(longitude);
    }

    public void setCoordinatesFromAddress(String startAddress) throws ApiException, InterruptedException, IOException, DataNotFoundException {
      LatLng point = MapsRequest.getLocationFromAddress(startAddress);
      if (point == null) {
        // Geocoding API didn't find the coordinates corresponding to given address.
        throw new DataNotFoundException("Unable to find address!");
      }
      this.latitude = point.lat;
      this.longitude = point.lng;
    }

    /**
     * Return a Date object from dateString.
     */
    private Date validateDate(String dateString, int startTimeSeconds) throws BadRequestException {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      Date date = null;
      try {
        date = formatter.parse(dateString);
      } catch (ParseException e) {
        throw new BadRequestException("Please enter a valid date!");
      }
      // get the currentDate to check that date is not set for a past date
      Date currentDay = new Date(System.currentTimeMillis());
      if (date.compareTo(currentDay) < 0) {
        throw new BadRequestException("Please enter a valid date!");
      }
      if (date.getTime() + startTimeSeconds * 1000 < currentDay.getTime()) {
        throw new BadRequestException("Please enter a time in the future!");
      }
      return date;
    }

    /**
     * Returns the integer value of numberString
     */
    private Integer parseInteger(String numberString) throws BadRequestException {
      Integer number = null;
      try {
        number = Integer.parseInt(numberString);
      } catch (Exception e) {
        throw new BadRequestException(e.getMessage());
      }
      return number;
    }

    /**
     * Returns the Double value of numberString
     */
    private Double parseDouble(String numberString) throws BadRequestException {
      Double number = null;
      try {
        number = Double.parseDouble(numberString);
      } catch (Exception e) {
        throw new BadRequestException(e.getMessage());
      }
      return number;
    }

    /**
     * Returns LocalTime object from timeString
     */
    private LocalTime parseTime(String timeString) throws BadRequestException {
      LocalTime time = null;
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
      try {
        time = LocalTime.parse(timeString, formatter);
      } catch (DateTimeParseException e) {
        throw new BadRequestException(e.getMessage());
      }
      return time;
    }

    private int getNumberOfSeconds(LocalTime time) {
      return time.getHour() * 3600 + time.getMinute() * 60;
    }
  }

  private CreateSlotManager slotRequest;
  private String userId;

  public void createSlot(String deliveryDay, String timezoneOffsetMinutes, String startTime, String endTime, String maxStops, String userId) throws BadRequestException {
    this.slotRequest = new CreateSlotManager(deliveryDay, timezoneOffsetMinutes, startTime, endTime, maxStops);
    this.userId = userId;
  }

  public DeliverySlot getDeliverySlot() {
    return slotRequest.deliverySlot;
  }
  public double getStartLatitude() {
    return slotRequest.latitude;
  }
  public double getStartLongitude() {
    return slotRequest.longitude;
  }
  public String getUserId() {
    return userId;
  }

  public void addSlotToDatastore() {
    DatastoreServiceFactory.getDatastoreService().put(createDeliveryRequestEntity());
  }

  /**
   * create a deliveryRequest Entity and store it in the datastore.
   */
  private Entity createDeliveryRequestEntity() {
    Entity deliveryRequest = new Entity("deliveryRequest");

    deliveryRequest.setProperty("deliveryDay", slotRequest.deliverySlot.getDeliveryDay());
    deliveryRequest.setProperty("startTime", slotRequest.deliverySlot.getStartTime());
    deliveryRequest.setProperty("endTime", slotRequest.deliverySlot.getEndTime());
    deliveryRequest.setProperty("uid", userId);
    deliveryRequest.setProperty("startLat", slotRequest.latitude);
    deliveryRequest.setProperty("startLng", slotRequest.longitude);

    return deliveryRequest;
  }
}