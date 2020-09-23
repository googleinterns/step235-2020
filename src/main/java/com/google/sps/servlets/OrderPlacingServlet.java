package com.google.sps.servlets;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.maps.errors.ApiException;
import com.google.sps.data.BadRequestException;
import com.google.sps.data.DataNotFoundException;
import com.google.sps.data.FirebaseAuthentication;
import com.google.sps.data.FirebaseSingletonApp;

import java.io.IOException;
import com.google.sps.data.OrderHandler;
import com.google.sps.data.GoogleMapsPathFinder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that will handle users placing an order in the UI.
 * Will create an order with the current books that can be found in the cart
 * and will use the current user's addres saved in Datastore.
 */

@WebServlet("/place-order")
public class OrderPlacingServlet extends HttpServlet {

  private FirebaseAuthentication firebaseAuth;

  @Override
  public void init() throws ServletException {
    // initialize firebase app
    try {
      setFirebaseAuth(new FirebaseAuthentication(FirebaseSingletonApp.getInstance()));
    } catch (IOException e) {
      System.out.println(e.getMessage());
      // the exception must be catched because init() does not throw IOException()
    }
  }

  public void setFirebaseAuth(FirebaseAuthentication firebaseAuth) {
    this.firebaseAuth = firebaseAuth;
  }

  /**
   * Add the order to datastore. 
   */

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get current user.
    String uid = null;
    try {
      // get the idToken from query string
      uid = firebaseAuth.getUserIdFromIdToken(request.getParameter("idToken"));
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      return;
    }
    OrderHandler orderHandler = new OrderHandler(new GoogleMapsPathFinder());
    try {
      orderHandler.placeOrder(uid);
    } catch (ApiException | InterruptedException | DataNotFoundException | BadRequestException e) {
      // Cannot throw exceptions because DoPost only throws IOException.
      e.printStackTrace();
      return;
    }

    response.sendRedirect("/cart.html");
  }
}