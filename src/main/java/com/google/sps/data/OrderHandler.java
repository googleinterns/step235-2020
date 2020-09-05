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
  private enum OrderProperty {
    LIBRARY_ID("libraryId"),
    LIBRARY_LAT("libraryLatitude"),
    LIBRARY_LNG("libraryLongitude"),
    RECIPIENT_LAT("recipientLatitude"),
    RECIPIENT_LNG("recipientLongitude"),
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
  private void addOrderToDatastore(LibraryPoint library, ArrayList<String> bookIds, String userId, Point address) {
    Entity order = new Entity("Order");
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
  public Collection<String> makeOrders(String userId, Point address, List<String> bookIds) throws ApiException, IOException, InterruptedException, DataNotFoundException {
    HashMap <LibraryPoint, ArrayList<String>> libraryBookIds = new HashMap<>();
    List<String> outOfStookBookIds = new ArrayList<String>();

    for (String bookId : bookIds) {
      Entity closestLibrary = getClosestLibrary(
          BooksManager.getLibrariesForBook(bookId, /** setLimit = */ false), address);
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
      BooksManager.removeBooksFromLibrary(library, booksIds);
    }

    return outOfStookBookIds;
  }

  /**
   * Given a List of "Library" Entity objects, it returns the one for which the time to get from it
   * to address is minimised.
   */
  Entity getClosestLibrary(List<Entity> libraries, Point address) throws ApiException, IOException, InterruptedException, DataNotFoundException {
    int minTimeFromAddressToLibrary = Integer.MAX_VALUE;
    Entity closestLibrary = null;

    for (Entity library : libraries) {
      int timeFromAddressToLibrary = pathFinder.getTimeBetweenPoints(address,
          new LibraryPoint((double) library.getProperty(OrderProperty.LIBRARY_LAT.label), (double) library.getProperty(OrderProperty.LIBRARY_LNG.label),
              ((Number)library.getProperty(OrderProperty.LIBRARY_ID.label)).intValue()));

      if (timeFromAddressToLibrary < minTimeFromAddressToLibrary) {
        minTimeFromAddressToLibrary = timeFromAddressToLibrary;
        closestLibrary = library;
      }
    }
    return closestLibrary;
  }
}
