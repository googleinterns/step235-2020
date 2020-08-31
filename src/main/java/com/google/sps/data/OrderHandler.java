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
import java.lang.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OrderHandler {
  private PathFinder pathFinder;

  public OrderHandler(PathFinder pathFinder) {
    this.pathFinder = pathFinder;
  }

  private void addOrderToDatastore(LibraryPoint library, ArrayList<String> bookIds, String userId, Point address) {
    Entity order = new Entity("Order");
    order.setProperty("libraryId", (int)library.getLibraryId());
    order.setProperty("libraryLatitude", library.latitude);
    order.setProperty("libraryLongitude", library.longitude);
    order.setProperty("recipientLatitude", address.latitude);
    order.setProperty("recipientLongitude", address.longitude);
    order.setProperty("status", "ADDED");
    order.setProperty("area", library.getArea());
    order.setProperty("userId", userId);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(order);
  }

  /**
   * Given a user's Id, the shipping address and an array of book ids, it creates a set of orders
   * and adds them to datastore. Each order contains a set of books and the library that contains
   * all the requested books.
   */
  public boolean makeOrders(String userId, Point address, List<String> bookIds) throws ApiException, IOException, InterruptedException {
    HashMap <LibraryPoint, ArrayList<String>> libraryBookIds = new HashMap<>();

    for (String bookId : bookIds) {
      Entity closestLibrary = getClosestLibrary(
          BooksManager.getLibrariesForBook(bookId, /** setLimit = */false), address);
      if (closestLibrary == null) {
        // there is no library that has bookId, thus the order can't be made
        return false;
      }
      LibraryPoint library = new LibraryPoint((double) closestLibrary.getProperty("libraryLatitude"),
          (double) closestLibrary.getProperty("libraryLongitude"), ((Number)closestLibrary.getProperty("libraryId")).intValue());
      
      if (!libraryBookIds.containsKey(library)) {
        libraryBookIds.put(library, new ArrayList<>());
      }
      // add bookId to the books that must be rented from library for userId
      libraryBookIds.get(library).add(bookId);
    }

    for (LibraryPoint library : libraryBookIds.keySet()) {
      ArrayList<String> booksIds = libraryBookIds.get(library);
      addOrderToDatastore(library, booksIds, userId, address);
      BooksManager.removeBooksFromLibrary(library, booksIds);
    }

    return true;
  }

  Entity getClosestLibrary(List<Entity> libraries, Point address) throws ApiException, IOException, InterruptedException {
    int minTimeFromAddressToLibrary = Integer.MAX_VALUE;
    Entity closestLibrary = null;

    for (Entity library : libraries) {
      int timeFromAddressToLibrary = pathFinder.getTimeBetweenPoints(address,
          new LibraryPoint((double) library.getProperty("libraryLatitude"), (double) library.getProperty("libraryLongitude"), 
          ((Number)library.getProperty("libraryId")).intValue()));

      if (timeFromAddressToLibrary < minTimeFromAddressToLibrary) {
        minTimeFromAddressToLibrary = timeFromAddressToLibrary;
        closestLibrary = library;
      }
    }
    return closestLibrary;
  }
}
