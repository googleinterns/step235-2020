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

import com.google.firebase.auth.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;
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
    // TODO[ak47na]: update startTime and endTime with respect to user timezone and check that the
    // time is not in the past
    LocalTime startTime = parseTime(request.getParameter("start-time"));
    LocalTime endTime = parseTime(request.getParameter("end-time"));    

    if (startTime == null || endTime == null || startTime.compareTo(endTime) > 0) {
      // Send a HTTP 400 Bad Request response if user provided an invalid time.
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid time!");
      return ;
    }
    
    int maxStops = parseNumber(request.getParameter("max-stops"));
    if (maxStops <= 0 || maxStops > 25) {
      // Send a HTTP 400 Bad Request response if user provided an invalid number of stops.
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter between 1 and 25 stops!");
      return ;
    }
    
    // initialize firebase app with Google Application Defalut Credentials
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

    DatastoreServiceFactory.getDatastoreService().put(createDeliveryRequestEntity(
      deliveryDay, startTime, endTime, maxStops, uid
    ));
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
   * create a deliveryRequest Entity and store it in the datastore.
   */
  private Entity createDeliveryRequestEntity(Date deliveryDay, LocalTime startTime, LocalTime endTime, int maxStops, String userId) {
    Entity deliveryRequest = new Entity("deliveryRequest");
    
    deliveryRequest.setProperty("deliveryDay", deliveryDay);
    deliveryRequest.setProperty("startTime", getNumberOfSeconds(startTime));
    deliveryRequest.setProperty("endTime", getNumberOfSeconds(endTime));
    deliveryRequest.setProperty("userId", userId);
   
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
  private int parseNumber(String numberString) {
    Integer number = null;
    try {
      number = Integer.parseInt(numberString);
    } catch (NumberFormatException e) {
      return -1;
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
