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
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.sps.data.UserData;
import com.google.gson.Gson;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.sps.data.FirebaseAuthentication;
import com.google.sps.data.FirebaseSingletonApp;
import com.google.sps.data.GoogleMapsPathFinder;
import com.google.sps.data.JourneyHandler;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that handles requests for the delivery journeys of users.
 */
@WebServlet("/see-journeys")
public class SeeJourneyServlet extends HttpServlet {
  private FirebaseAuthentication firebaseAuth;

  public void setFirebaseAuth(FirebaseAuthentication firebaseAuth) {
    this.firebaseAuth = firebaseAuth;
  }

  /**
   * Displays the journeys of the user in JSON format on "/see-journeys".
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Initialize firebase app.
    setFirebaseAuth(new FirebaseAuthentication(FirebaseSingletonApp.getInstance()));
    String userId = null;
    try {
      // Get the idToken from query string.
      String idToken = request.getParameter("idToken");
      if (idToken == null) {
        // The idToken was not sent.
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return ;
      }
      userId = firebaseAuth.getUserIdFromIdToken(request.getParameter("idToken"));
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      return;
    }

    // Return all journeys of the current user.
    JourneyHandler journeyHandler = new JourneyHandler(new GoogleMapsPathFinder());
    List<Entity> journeys = journeyHandler.getJourneysForUser(userId);
    Gson gson = new Gson();
    String json = gson.toJson(journeys);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }
}
