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

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.firebase.auth.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.errors.ApiException;
import com.google.maps.GeoApiContext;
import com.google.maps.GeoApiContext.Builder;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.google.sps.data.Point;
import com.google.sps.data.MapsRequest;
import java.io.IOException;
import java.lang.Double;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.time.format.DateTimeFormatter; 
import java.time.format.DateTimeParseException; 
import java.time.LocalTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date; 

@WebServlet("/new-delivery-request")
public class NewRequestServlet extends HttpServlet {
  /**
   * The function handles POST requests sent by delivery-form in deliveryRequest.html 
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    Date deliveryDay = parseDate(request.getParameter("delivery-date"));

    // get the currentDate to check that deliveryDay is not set for a past date
    Date currentDay = new Date(System.currentTimeMillis());
    if (deliveryDay == null || deliveryDay.compareTo(currentDay) < 0) {
      // Send a HTTP 400 Bad Request response if user provided an invalid date.
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid date!");
      return ;
    }

    // get the timezone offset from the current request in seconds
    Integer timezoneOffset = parseInteger(request.getParameter("timezone-offset")) * 60;
    
    LocalTime startTime = parseTime(request.getParameter("start-time"));
    LocalTime endTime = parseTime(request.getParameter("end-time"));    

    if (startTime == null || endTime == null || startTime.compareTo(endTime) > 0) {
      // Send a HTTP 400 Bad Request response if user provided an invalid time.
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid time!");
      return ;
    }
    // convert startTime and endTime to seconds and add the timezoneOffset 
    int startTimeSeconds = getNumberOfSeconds(startTime) + timezoneOffset;
    int endTimeSeconds = getNumberOfSeconds(endTime) + timezoneOffset;

    if (deliveryDay.getTime() + (getNumberOfSeconds(startTime) + timezoneOffset) * 1000 < currentDay.getTime()) {
      // Send a HTTP 400 Bad Request response if user provided a time in the past.
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter a time in the future!");
      return ;
    }
    
    Integer maxStops = parseInteger(request.getParameter("max-stops"));
    if (maxStops == null || maxStops <= 0 || maxStops > 25) {
      // Send a HTTP 400 Bad Request response if user provided an invalid number of stops.
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter between 1 and 25 stops!");
      return ;
    }

    Double lat = parseDouble(request.getParameter("latitude"));
    Double lng = parseDouble(request.getParameter("longitude"));
    if (lat == null || lng == null) {
      // the user has set the address himself
      LatLng point = MapsRequest.getLocationFromAddress(request.getParameter("address"));
      if (point == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid address!");
        return ;
      }
      lat = point.lat;
      lng = point.lng;
    }
    System.out.println(lat);
    System.out.println(lng);

    // initialize firebase app 
    FirebaseApp defaultApp = initializeFirebaseApp();
    //check the name of defaultApp to make sure that the correct app is connected
    System.out.println(defaultApp.getName());

    String uid = null;
    try {
      // get the idToken from hidden input field
      String idToken = request.getParameter("idToken");
      FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
      uid = decodedToken.getUid();
      System.out.println(uid);
      UserRecord user = FirebaseAuth.getInstance().getUser(uid);
      System.out.println(user.getEmail());
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid idToken");
      return ;
    }

    markUserAsCourier(uid);
    
    if (deliveryDay.compareTo(currentDay) == 0) {
      // if the delivery request is not sent with at least one day in advance, then procees it as soon as possible
      processDeliveryRequest(deliveryDay, startTimeSeconds, endTimeSeconds, maxStops, uid, lat, lng);
    } else {
    // the delivery request is added to datastore and processed the day before deliveryDay
    // delayed processing optimizes the order matching algorithm by taking into account other
    // or future delivery requests and couriers
    DatastoreServiceFactory.getDatastoreService().put(createDeliveryRequestEntity(
      deliveryDay, startTimeSeconds, endTimeSeconds, maxStops, uid, lat, lng
    ));
    }

  }

  /** 
   * Finds a set of orders that can be solved by the current delivery request, creates an optimal 
   * delivery journey for the orders and adds it to user's journeys.
   */
  private void processDeliveryRequest(Date deliveryDay, int startTime, int endTime, int maxStops, String userId, Double startLat, Double startLng) {
    // findOrdersForDeliveryRequest(); will return an arrayList of Waypoint objects representing
    // the stops the courier must make; the Waypoint objects provide information such as the books 
    // that should be taken/delivered
    // addDeliveryJourney(MatchingSystem.findOrdersForDeliveryRequest());
  }

  /**
   * Initialize firebase SDK and return the FirebaseApp object
   */
  private FirebaseApp initializeFirebaseApp() {
    FirebaseOptions options = new FirebaseOptions.Builder()
    //.setCredentials(GoogleCredentials.getApplicationDefault()) 
    .setDatabaseUrl("https://com-alphabooks-step-2020.firebaseio.com")
    .build();
    return FirebaseApp.initializeApp(options);
  }

  /** 
   * If this is the first time the user makes a delivery request, he will be marked in
   * the data store as a courier to be able to view "See journeys" page.  
   */

  private void markUserAsCourier(String uid) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity userEntity;
    Query query =
        new Query("UserData")
            .setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, uid));
    PreparedQuery results = datastore.prepare(query);
    userEntity = results.asSingleEntity();
    if (userEntity != null && userEntity.getProperty("isCourier").equals("true")) {
      // This is not the first request, so user is already marked as courier.
      return;
    }
    if (userEntity == null) {
      // User does not exist in the database.
      userEntity = new Entity("UserData");
      userEntity.setProperty("uid", uid);
    }
    userEntity.setProperty("isCourier", "true");
    datastore.put(userEntity);
  }

  /**
   * create a deliveryRequest Entity and store it in the datastore.
   */
  private Entity createDeliveryRequestEntity(Date deliveryDay, int startTime, int endTime, int maxStops, String userId, Double startLat, Double startLng) {
    Entity deliveryRequest = new Entity("deliveryRequest");
    
    deliveryRequest.setProperty("deliveryDay", deliveryDay);
    deliveryRequest.setProperty("startTime", startTime);
    deliveryRequest.setProperty("endTime", endTime);
    deliveryRequest.setProperty("userId", userId);
    deliveryRequest.setProperty("startLat", startLat);
    deliveryRequest.setProperty("startLng", startLng);
   
    return deliveryRequest;
  }

  private int getNumberOfSeconds(LocalTime time) {
    return time.getHour() * 3600 + time.getMinute() * 60; 
  }

  /**
   * Return a Date object from dateString.
   */
  private Date parseDate(String dateString) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    Date date = null;
    try {
      date = formatter.parse(dateString);
    } catch (ParseException e) {
      // Send a HTTP 400 Bad Request response if user provided an invalid date.
      return null;
    }
    return date;
  }

  /**
   * Returns the integer value of numberString
   */
  private Integer parseInteger(String numberString) {
    Integer number = null;
    try {
      number = Integer.parseInt(numberString);
    } catch (Exception e) {
      return null;
    }
    return number;
  }

  /**
   * Returns the Double value of numberString
   */
  private Double parseDouble(String numberString) {
    Double number = null;
    try {
      number = Double.parseDouble(numberString);
    } catch (Exception e) {
      return null;
    }
    return number;
  }

  /**
   * Returns LocalTime object from timeString
   */
  private LocalTime parseTime(String timeString) {
    LocalTime time = null;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    try {
      time = LocalTime.parse(timeString, formatter);
    } catch (DateTimeParseException e) {
      return null;
    }
    return time;
  }

}
