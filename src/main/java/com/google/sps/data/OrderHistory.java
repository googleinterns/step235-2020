package com.google.sps.data;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that manipulates the order history of users.
 */

public class OrderHistory {
  private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  /**
   * Return an array with the current list of books of the current user.
   */

  public List<String> getUserOrderHistory(String uid) {
    Query query = new Query("UserHistory").setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, uid));
    PreparedQuery results = datastore.prepare(query);
    Entity entity = results.asSingleEntity();
    ArrayList<String> books = new ArrayList<>();
    if (entity != null) {
      // User has ordered books in the past.
      books.addAll((ArrayList<String>) entity.getProperty("books"));
    }
    return books;
  }

  /**
   * Add a list of books to the user's order history.
   */

  public void addBooksToOrderHistory(String uid, List<String> bookIds) {
    Entity historyEntity;
    Query query = new Query("UserHistory").setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, uid));
    PreparedQuery results = datastore.prepare(query);
    historyEntity = results.asSingleEntity();
    ArrayList<String> books = new ArrayList<>();
    if (historyEntity == null) {
      // User never ordered a book before.
      historyEntity = new Entity("UserHistory");
      historyEntity.setProperty("uid", uid);
    } else {
      // Add previous books to this array.
      books.addAll((ArrayList<String>) historyEntity.getProperty("books"));
    }

    for (String book : bookIds) {
      // Do not add duplicates to the book history.
      if (books.contains(book)) {
        continue;
      } else {
        books.add(book);
      }
    }

    historyEntity.setProperty("books", books);
    datastore.put(historyEntity);
  }
}
