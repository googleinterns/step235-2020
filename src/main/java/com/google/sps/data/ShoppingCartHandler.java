package com.google.sps.data;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCartHandler {
  private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  /**
   * Return an array with the current list of books of the current user.
   */

  public List<String> getUserShoppingCart(String uid) {
    Query query = new Query("UserCart").setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, uid));
    PreparedQuery results = datastore.prepare(query);
    Entity entity = results.asSingleEntity();
    ArrayList<String> books = new ArrayList<>();
    if (entity != null) {
      // User has items added to cart.
      books.addAll((ArrayList<String>) entity.getProperty("books"));
    }
    return books;
  }

  /**
   * Add a book with bookId to the user's shopping list.
   */

  public void addBookToShoppingCart(String uid, String bookId) {
    Entity cartEntity;
    Query query = new Query("UserCart").setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, uid));
    PreparedQuery results = datastore.prepare(query);
    cartEntity = results.asSingleEntity();
    ArrayList<String> books = new ArrayList<>();
    if (cartEntity == null) {
      // User cart is empty.
      cartEntity = new Entity("UserCart");
      cartEntity.setProperty("uid", uid);
    } else {
      // Add previous books to this array.
      books.addAll((ArrayList<String>) cartEntity.getProperty("books"));
    }

    books.add(bookId);
    cartEntity.setProperty("books", books);
    datastore.put(cartEntity);
  }

  /**
   * Deletes book with bookId from user's current shopping cart.
   */

  public void deleteBookFromShoppingCart(String uid, String bookId) {
    Query query = new Query("UserCart").setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, uid));
    PreparedQuery results = datastore.prepare(query);
    Entity entity = results.asSingleEntity();
    ArrayList<String> books = (ArrayList<String>) entity.getProperty("books");
    books.remove(bookId);
    // If no book remained after the deletion, delete the entity from the datastore.
    if (books.size() == 0) {
      datastore.delete(entity.getKey());
      return;
    }
    entity.setProperty("books", books);
    datastore.put(entity);
  }
}