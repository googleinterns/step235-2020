package com.google.sps.servlets;

import com.google.sps.data.UserData;
import com.google.gson.Gson;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.*;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that will add a user with his address to the database when he sets it first.
 * Users can also edit their adrress. 
 */
@WebServlet("/user-data")
public class UserDataServlet extends HttpServlet {
  
  // Get all info about current user.
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
  
    // Initialize firebase app.
    FirebaseApp defaultApp = initializeFirebaseApp(/* setOptions */ false);
    String uid = null;
    try {
      // get the idToken from hidden input field
      String idToken = request.getParameter("idToken");
      FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
      uid = decodedToken.getUid();
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid idToken");
      return;
    }

    /* Return all info about the current user if he has been added to the database. */
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query =
        new Query("UserData")
            .setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, uid));
    PreparedQuery results = datastore.prepare(query);
    Entity entity = results.asSingleEntity();
    // Sets the address to "" and the isCourier field to false
    UserData userData = new UserData(uid);
    boolean isCourier;
    // If entity == null, user does not exist in the database. Return empty string as address.
    if (entity != null) {
      // User exists in the database.
      String address = (String) entity.getProperty("address");
      userData.setAddress(address);
      if (entity.getProperty("isCourier") != null) {
        isCourier = Boolean.parseBoolean((String) entity.getProperty("isCourier"));
        userData.setIsCourier(isCourier);
      }
    }
    
    Gson gson = new Gson();
    String json = gson.toJson(userData);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  // Add user address to the database. Handles form from address.html.
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Initialize firebase app.
    FirebaseApp defaultApp = initializeFirebaseApp(/* setOptions */ false);
    String uid = null;
    try {
      // get the idToken from hidden input field
      String idToken = request.getParameter("idToken");
      FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
      uid = decodedToken.getUid();
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid idToken");
      return;
    }

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity userEntity;
    Query query =
        new Query("UserData")
            .setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, uid));
    PreparedQuery results = datastore.prepare(query);
    userEntity = results.asSingleEntity();
    String address = request.getParameter("address");
    if (userEntity == null) {
      // User did not set adress before.
      userEntity = new Entity("UserData");
      userEntity.setProperty("uid", uid);
    }
    // User can edit the address.
    userEntity.setProperty("address", address);

    datastore.put(userEntity);
    response.sendRedirect("/loggedIn.html");
  }

  private FirebaseApp initializeFirebaseApp(boolean setOptions) {
    if (!setOptions) {
      // initialize FirebaseApp with default options for testing purposes
      return FirebaseApp.initializeApp();
    }
    FirebaseOptions options = new FirebaseOptions.Builder()
    .setDatabaseUrl("https://com-alphabooks-step-2020.firebaseio.com")
    .build();
    return FirebaseApp.initializeApp(options);
  }

}