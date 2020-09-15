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
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.firebase.auth.FirebaseAuthException;
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
import com.google.sps.data.DeliverySystem;
import com.google.sps.data.Journey;
import com.google.sps.data.MapsRequest;
import com.google.sps.data.Point;
import com.google.sps.data.DataNotFoundException;
import com.google.sps.data.BadRequestException;
import com.google.sps.data.DeliverySlot;
import com.google.sps.data.DeliverySlotManager;
import com.google.sps.data.FirebaseAuthentication;
import com.google.sps.data.FirebaseSingletonApp;
import com.google.sps.data.JourneyHandler;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;


// TODO[ak47na]: Add helper classes to make the code testable without mocking HttpServletRequest and HttpServletResponse.
@WebServlet("/new-delivery-request")
public class NewRequestServlet extends HttpServlet {
  private static final String dateFormat = "yyyy-MM-dd";
  private static final String timeFormat = "HH:mm";
  private FirebaseAuthentication firebaseAuth;

  private enum DeliverySlotParameters {
    DELIVERY_DATE("delivery-date"),
    TIMEZONE_OFFSET_MINUTES("timezone-offset-minutes"),
    START_TIME("start-time"),
    END_TIME("end-time"),
    LAT("latitude"),
    LNG("longitude"),
    START_ADDRESS("start-address");

    public final String label;

    private DeliverySlotParameters(String label) {
      this.label = label;
    }
  }

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
    DeliverySlot deliverySlot = null;
    try {
      // Get the delivery slot details as parameters from the form in deliveryRequest.html.
      long timezoneOffsetMiliseconds = parseInt(request.getParameter(DeliverySlotParameters.TIMEZONE_OFFSET_MINUTES.label)) * 60000;
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormat);
      long startTimeInMiliseconds = LocalTime.parse(request.getParameter(DeliverySlotParameters.START_TIME.label), timeFormatter)
          .toNanoOfDay() / 1000000 + timezoneOffsetMiliseconds;
      long endTimeInMiliseconds = LocalTime.parse(request.getParameter(DeliverySlotParameters.END_TIME.label), timeFormatter)
          .toNanoOfDay() / 1000000 + timezoneOffsetMiliseconds;
      SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
      Date deliveryDate = dateFormatter.parse(request.getParameter(DeliverySlotParameters.DELIVERY_DATE.label));
      deliverySlot = new DeliverySlot(deliveryDate,
          startTimeInMiliseconds,
          endTimeInMiliseconds,
          userId);
      if (request.getParameter(DeliverySlotParameters.LAT.label) == null ||
          request.getParameter(DeliverySlotParameters.LNG.label) == null) {
        // the user has set the address himself
        deliverySlot.setStartPoint(request.getParameter(DeliverySlotParameters.START_ADDRESS.label));
      } else {
        // the starting point is the current location of the user
        deliverySlot.setStartPoint(parseDouble(request.getParameter(DeliverySlotParameters.LAT.label)),
            parseDouble(request.getParameter(DeliverySlotParameters.LNG.label)));
      }
    } catch (BadRequestException | NumberFormatException | ParseException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      return;
    } catch (DataNotFoundException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
      return;
    } catch (Exception e) {
      // Send server error for ApiException or InterruptedException.
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
    slotManager.createDeliverySlot(deliverySlot);
    markUserAsCourier(userId);
    JourneyHandler journeyHandler = new JourneyHandler();
    try {
      // Create journey for deliverySlot and add it to datastore.
      journeyHandler.processDeliveryRequest(deliverySlot);
    } catch (ApiException | InterruptedException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
  }

  /**
   * Displays the delivery slots of the current user in JSON format.
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String userId = null;
    try {
      // Get the idToken from query string.
      userId = firebaseAuth.getUserIdFromIdToken(request.getParameter("idToken"));
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      return;
    }
    
    DeliverySlotManager slotManager = new DeliverySlotManager();
    List<DeliverySlot> deliverySlots;
    try {
      deliverySlots = slotManager.getUsersDeliverySlotRequests(userId);
    } catch (Exception e) {
      // Creating an invalid DeliverySlot can throw ApiException, IOException, InterruptedException
      // DataNotFoundException or BadRequestException, but slots retrieved from datastore are valid
      // ,thus the only possible exception is EntityNotFoundException.
      response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
      return;
    }

    Gson gson = new Gson();
    String json = gson.toJson(deliverySlots);
    response.setContentType("application/json;");
    response.getWriter().println(json);
    System.out.println(json);
    response.setStatus(HttpServletResponse.SC_OK);
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
