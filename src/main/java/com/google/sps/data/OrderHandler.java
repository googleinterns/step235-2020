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

package com.google.sps.data;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Class used for creating and storing orders.
 */
public class OrderHandler {
  private PathFinder pathFinder;
  // The properties of orders in datastore:
  public enum OrderProperty {
    LIBRARY_ID("libraryId"),
    LIBRARY_LAT("libraryLatitude"),
    LIBRARY_LNG("libraryLongitude"),
    RECIPIENT_LAT("recipientLatitude"),
    RECIPIENT_LNG("recipientLongitude"),
    BOOK_IDS("books"),
    STATUS("status"),
    AREA("area"),
    USER_ID("userId");

    public final String label;

    private OrderProperty(String label) {
      this.label = label;
    }
  }

  public enum OrderStatus {
    ADDED, ASSIGNED;
  }

  public OrderHandler(PathFinder pathFinder) {
    this.pathFinder = pathFinder;
  }

  /**
   * Creates an "Order" Entity and adds it to datastore. Each order contains a set of books and the
   * library that has all the requested books.
   */
  public void addOrderToDatastore(LibraryPoint library, List<String> bookIds, String userId, Point address) {
    Entity order = new Entity("Order");
    order.setProperty(OrderProperty.BOOK_IDS.label, bookIds);
    order.setProperty(OrderProperty.LIBRARY_ID.label, (int)library.getLibraryId());
    order.setProperty(OrderProperty.LIBRARY_LAT.label, library.latitude);
    order.setProperty(OrderProperty.LIBRARY_LNG.label, library.longitude);
    order.setProperty(OrderProperty.RECIPIENT_LAT.label, address.latitude);
    order.setProperty(OrderProperty.RECIPIENT_LNG.label, address.longitude);
    order.setProperty(OrderProperty.STATUS.label, OrderStatus.ADDED.toString());
    order.setProperty(OrderProperty.AREA.label, library.getArea());
    order.setProperty(OrderProperty.USER_ID.label, userId);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(order);
  }

  /**
   * Given a user's Id, the shipping address and an array of book ids, if all books are in stock,
   * it creates a set of orders and adds them to datastore. Otherwise, it returns a Collection with
   * the ids of the books that are out of stock.
   */
  public Collection<String> makeOrders(String userId, Point address, List<String> bookIds) throws ApiException, IOException, InterruptedException, DataNotFoundException, BadRequestException {
    HashMap <LibraryPoint, ArrayList<String>> libraryBookIds = new HashMap<>();
    BooksManager booksManager = new BooksManager();
    List<String> outOfStookBookIds = new ArrayList<String>();

    for (String bookId : bookIds) {
      Entity closestLibrary = getClosestLibrary(
          booksManager.getLibrariesForBook(bookId, /** setLimit = */ false), address);
      if (closestLibrary == null) {
        // there is no library that has the book bookId, thus the book can't be ordered.
        outOfStookBookIds.add(bookId);
      } else {
        LibraryPoint library = new LibraryPoint((double) closestLibrary.getProperty(OrderProperty.LIBRARY_LAT.label),
            (double) closestLibrary.getProperty(OrderProperty.LIBRARY_LNG.label),
            ((Number)closestLibrary.getProperty(OrderProperty.LIBRARY_ID.label)).intValue());

        if (!libraryBookIds.containsKey(library)) {
          libraryBookIds.put(library, new ArrayList<>());
        }
        // add bookId to the books that must be rented from library for userId
        libraryBookIds.get(library).add(bookId);
      }
    }

    for (LibraryPoint library : libraryBookIds.keySet()) {
      ArrayList<String> booksIds = libraryBookIds.get(library);
      addOrderToDatastore(library, booksIds, userId, address);
      booksManager.removeBooksFromLibrary(library, booksIds);
    }

    return outOfStookBookIds;
  }

   /**
   * Given a List of "Library" Entity objects, it returns the one for which the time to get from it
   * to address is minimised. 
   */
  Entity getClosestLibrary(List<Entity> libraries, Point address) throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException, DataNotFoundException {
    if (libraries.size() == 0) {
      // There is no library, thus no library is closest to address.
      return null;
    }
    int minTimeFromAddressToLibrary = Integer.MAX_VALUE;
    // The index in the libraries array of the closest library to address.
    int closestLibraryIndex = -1;
    int index = 0;
    for (Entity library : libraries) {
      int timeFromAddressToLibrary = pathFinder.getTimeInSecondsBetweenPoints(address,
          new LibraryPoint((double) library.getProperty("libraryLatitude"), (double) library.getProperty("libraryLongitude"),
              ((Number)library.getProperty("libraryId")).intValue()));

      if (timeFromAddressToLibrary < minTimeFromAddressToLibrary) {
        minTimeFromAddressToLibrary = timeFromAddressToLibrary;

        closestLibraryIndex = index;
      }
      ++ index;
    }

    if (closestLibraryIndex != -1) {
      return libraries.get(closestLibraryIndex);
    } else {
      return null;
    }
  }

  /**
   * Returns the keyString of unassigned orders from datastore with the area set to area.
   */
  List<String> getAvailableOrders(int area) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query orderQuery = new Query("Order")
        .setFilter(new Query.FilterPredicate(OrderProperty.AREA.label, Query.FilterOperator.EQUAL, area))
        .setFilter(new Query.FilterPredicate(OrderProperty.STATUS.label, Query.FilterOperator.EQUAL, OrderStatus.ADDED.toString()));

    List<Entity> results = datastore.prepare(orderQuery).asList(FetchOptions.Builder.withDefaults());
    List <String> resultsKeyStrings = new ArrayList<>();
    for (Entity order : results) {
      resultsKeyStrings.add(KeyFactory.keyToString(order.getKey()));
    }
    return resultsKeyStrings;
  }

  /** 
   * Returns the property propertyName of order with keyString representation orderKeyStr.
   */
  public Object getProperty(String orderKeyStr, String propertyName) throws EntityNotFoundException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity order = datastore.get(KeyFactory.stringToKey(orderKeyStr));
    return order.getProperty(propertyName);
  }

  /** 
   * Updates the status property of orders with the keyString from orderKeys list to status.
   * TODO[ak47na]: use transactions to update the state of orders.
   */
  public void updateStatusForOrders(List<String> orderKeys, String status) throws EntityNotFoundException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    for (String orderKey : orderKeys) {
      Entity order = datastore.get(KeyFactory.stringToKey(orderKey));
      order.setProperty(OrderProperty.STATUS.label, status);
    }
  }
}
