package com.google.sps.servlets;

import com.google.sps.data.ShoppingCartHandler;

import com.google.gson.Gson;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.sps.data.FirebaseAuthentication;
import com.google.sps.data.FirebaseSingletonApp;

import java.io.IOException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Servlet that manipulates the list of books in the shopping cart.
 * Provides GET, POST and DELETE methods.
 */

@WebServlet("/shopping-cart")
public class ShoppingCartServlet extends HttpServlet {
  
  private FirebaseAuthentication firebaseAuth;
  private final ShoppingCartHandler cartHandler = new ShoppingCartHandler();

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
   * Returns the current list of books the user has in the cart.
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
    String json = gson.toJson(cartHandler.getUserShoppingCart(uid));
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  /**
   * Adds a new book to the current cart of the user.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get current user.
    String uid = null;
    JSONObject jsonObject;
    try {
      jsonObject = new JSONObject(request.getReader().lines().collect(Collectors.joining(System.lineSeparator())));
    } catch (JSONException e){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      return;
    }
    String bookId = jsonObject.get("bookId").toString();
    try {
      // get the idToken from jsonObject.
      uid = firebaseAuth.getUserIdFromIdToken(jsonObject.get("idToken").toString());
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      return;
    }
    cartHandler.addBookToShoppingCart(uid, bookId);
    response.sendRedirect("/bookDetails.html?id=" + bookId);
  }

  /**
   * Removes a book from the current cart of the user.
   */

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get current user.
    String uid = null;
    try {
      // get the idToken from query string
      uid = firebaseAuth.getUserIdFromIdToken(request.getParameter("idToken"));
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      return;
    }
    String bookId = request.getParameter("bookId");
    cartHandler.deleteBookFromShoppingCart(uid, bookId);
  }
}
