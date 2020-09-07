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
import com.google.sps.data.BadRequestException;
import com.google.sps.data.DeliverySlot;
import com.google.sps.data.DeliverySlotManager;
import com.google.sps.data.FirebaseAuthentication;
import com.google.sps.data.FirebaseSingletonApp;
import com.google.sps.data.MapsRequest;
import java.io.IOException;
import java.lang.Double;
import java.lang.Thread;
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

// TODO[ak47na]: Add helper classes to make the code testable without mocking HttpServletRequest and HttpServletResponse.
@WebServlet("/new-delivery-request")
public class NewRequestServlet extends HttpServlet {
  private FirebaseAuthentication firebaseAuth;
  
  @Override
  public void init() throws ServletException {
    // initialize firebase app 
    try {
      setFirebaseAuth(new FirebaseAuthentication(FirebaseSingletonApp.getInstance()));
    } catch (IOException e) {
      System.out.println(e.getMessage());
      // TODO: refator code to throw IOException; the exception must be catched because init() does
      // not throw IOException()
    }
  }

  public void setFirebaseAuth(FirebaseAuthentication firebaseAuth) {
    this.firebaseAuth = firebaseAuth;
  }

  /**
   * The function handles POST requests sent by delivery-form in deliveryRequest.html 
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String userId = null;
    try {
      // get the idToken from hidden input field 
      userId = firebaseAuth.getUserIdFromIdToken(request.getParameter("idToken"));
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      return;
    }

    DeliverySlotManager slotManager = new DeliverySlotManager();
    try {
      slotManager.createSlot(request.getParameter("delivery-date"),
          request.getParameter("timezone-offset-minutes"),
          request.getParameter("start-time"),
          request.getParameter("end-time"),
          request.getParameter("max-stops"),
          userId);
    } catch (BadRequestException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      return;
    }
    addDeliveryRequestResponse(slotManager.getDeliverySlot(), request, response);
    markUserAsCourier(userId);

    if (slotManager.getDeliverySlot().getDeliveryDay().compareTo(new Date(System.currentTimeMillis())) == 0) {
      // if the delivery request is not sent with at least one day in advance, then procees it as soon as possible
      processDeliveryRequest(slotManager);
    } else {
      // the delivery request is added to datastore and processed the day before deliveryDay
      // delayed processing optimizes the order matching algorithm by taking into account other
      // or future delivery requests and couriers
      slotManager.addSlotToDatastore();
    }

  }

  protected void addDeliveryRequestResponse(DeliverySlot deliverySlot, HttpServletRequest request, HttpServletResponse response) throws IOException {
    Gson gson = new Gson();
    String json = gson.toJson(deliverySlot);
    response.setContentType("application/json;");

    response.getWriter().println(json);
  }

  /**
   * Finds a set of orders that can be solved by the current delivery request, creates an optimal 
   * delivery journey for the orders and adds it to user's journeys.
   */
  private void processDeliveryRequest(DeliverySlotManager slotManager) {
    // findOrdersForDeliveryRequest(); will return an arrayList of Waypoint objects representing
    // the stops the courier must make; the Waypoint objects provide information such as the books 
    // that should be taken/delivered
    // addDeliveryJourney(MatchingSystem.findOrdersForDeliveryRequest());
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
    if (userEntity != null) {
      if(userEntity.getProperty("isCourier") != null && userEntity.getProperty("isCourier").equals("true")) {
        // This is not the first request, so user is already marked as courier.
        return;
      }
    } else {
      // User does not exist in the database.
      userEntity = new Entity("UserData");
      userEntity.setProperty("uid", uid);
    }
    userEntity.setProperty("isCourier", "true");
    datastore.put(userEntity);
  }
}
