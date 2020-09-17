package com.google.sps.servlets;


import com.google.sps.data.DatabaseHandler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;


import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

/**
 * When user goes to /load-database, he needs to wait for the database to get loaded.
 * Will parse the CSV file to create fictive book stocks.
 * The CSV format is :
 * title", "author", "googlebooks_id1", "googlebooks_id2", "googlebooks_id3", "googlebooks_id4", "googlebooks_id5"
 * so we will add these IDs in our database.
 * 
 * Can also be loaded as a cron job from here:
 * https://pantheon.corp.google.com/appengine/cronjobs?project=alphabooks-step-2020
 */

@WebServlet("/load-database")
public class DatabaseLoaderServlet extends HttpServlet {

  private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    File csvFile;
    try {
      csvFile = new File(this.getClass().getResource("/availableBooks.csv").toURI());
    } catch (URISyntaxException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found!");
      e.printStackTrace();
      return;
    }
    DatabaseHandler databaseHandler = new DatabaseHandler();
    databaseHandler.loadBookStocksFromCSV(csvFile);
    // TODO load Libraries coordinates

    response.setContentType("text/html");
    // User will be notified when the database is fully loaded.
    response.getWriter().println("<h1>Done loading database.</h1>");
  }
}
