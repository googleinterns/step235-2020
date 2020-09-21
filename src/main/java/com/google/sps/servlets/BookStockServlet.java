package com.google.sps.servlets;

import com.google.sps.data.BooksManager;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 * Servlet that communicates to JavaScript the status regarding a book's stock.
 */

@WebServlet("/book-stock")
public class BookStockServlet extends HttpServlet {

  /**
   * Will provide a HTTP response containing a JSON like this:
   * {"isInStock": true/false};
   */

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    String bookId = request.getParameter("bookId");
    JSONObject json = new JSONObject();
    if (BooksManager.getBookStock(bookId) > 0) {
      json.put("isInStock", true);
    } else {
      json.put("isInStock", false);
    }

    response.setContentType("application/json;");
    response.getWriter().println(json);
  }
}
