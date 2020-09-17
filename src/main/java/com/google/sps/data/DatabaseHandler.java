package com.google.sps.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.opencsv.CSVReader;

/**
 * Helper class for DatabaseLoaderServlet.
 */

public class DatabaseHandler {
  private final Integer BOOK_STOCK = 5;
  private final Integer TOTAL_STOCK = 185;
  private final Integer NO_OF_LIBRARIES = 37;
  private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  /**
   * Given an ID, it creates a book Entity for the ID and adds it to Datastore.
   * @param String{bookId}
   */

  public void addBookStockToDatastore(String bookId) {
    Entity bookEntity = new Entity("Book");
    bookEntity.setProperty("bookId", bookId);
    bookEntity.setProperty("totalStock", TOTAL_STOCK);
    datastore.put(bookEntity);
    // Every library has 5 copies of each volume.
    for (int i = 0; i < NO_OF_LIBRARIES; i++) {
      // LibraryStock entities will be the ancestors of Book entity,
      // each Book entity will have a number of LibraryStock entities as children
      // equal to the total number of libraries
      Entity libraryEntity = new Entity("LibraryStock", bookEntity.getKey());
      libraryEntity.setProperty("libraryId", i);
      libraryEntity.setProperty("stock", BOOK_STOCK);
      datastore.put(libraryEntity);
    }
  }
  
  /**
   * For each Google Books Api ID from the CSV file, it will create a Book entity
   * and will add it to Datastore.
   * @param HttpServletResponse{response}
   * @throws IOException
   */

  public void loadBookStocksFromCSV(File csvFile) throws IOException {
  
    CSVReader reader;
    reader = new CSVReader(new FileReader(csvFile));
    String[] line;
    // Jump over first line, since it has the titles.
    line = reader.readNext();
    while ((line = reader.readNext()) != null) {
      // If any error occured in the current line of the CSV file, skip it.
      if (line.length <= 2) {
        continue;
      }
      /**
       * The CSV format is :
       * title", "author", "googlebooks_id1", "googlebooks_id2", "googlebooks_id3", "googlebooks_id4", "googlebooks_id5"
       * so this is why we are starting from the the 3th entry, we only want the IDs from Google Books API.
       */
      for (int i = 2; i < line.length; i++) {
        // Check if the volume already exists in the Datastore. If it does exist, it just skips it bacause we
        // don't want duplicates in our database and the old stocks remain the same.
        Query query = new Query("Book").setFilter(new Query.FilterPredicate("bookId", Query.FilterOperator.EQUAL, line[i]));
        PreparedQuery results = datastore.prepare(query);
        if (results.asSingleEntity() == null) {
          // If not, add it.
          addBookStockToDatastore(line[i]);
        }
      }
    }
  }
}
