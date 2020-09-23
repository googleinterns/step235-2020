package com.google.sps.servlets;

import com.google.sps.data.OrderHistory;

import com.google.gson.Gson;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.sps.data.FirebaseAuthentication;
import com.google.sps.data.FirebaseSingletonApp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlets that returns to JavaScript the list of books user has ordered in the past.
 */

@WebServlet("/order-history")
public class OrderHistoryServlet extends HttpServlet {
  private FirebaseAuthentication firebaseAuth;
  private final OrderHistory orderHistory = new OrderHistory();

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
   * Returns the current list of books the user has in the order history.
   */
  
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get current user.
    String uid = null;
    try {
      // get the idToken from query string
      uid = firebaseAuth.getUserIdFromIdToken(request.getParameter("idToken"));
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      return;
    }
    
    Gson gson = new Gson();
    String json = gson.toJson(orderHistory.getUserOrderHistory(uid));
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }
}
