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
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.EntityNotFoundException;
import java.lang.Number;
import java.util.ArrayList;
import java.util.List;

public class BooksManager {
  /**
   * Returns true if there is at least one library that has the book with bookId in stock and false
   * otherwise.
   */
  protected static boolean isBookInStore(String bookId) {
    List<Entity> results = getLibrariesForBook(bookId, /** setLimit = */true);
    return (results.size() > 0);
  }

  /**
   * Returns the Key of book bookId from datastore or null if there is no book with bookId.
   */
  private static Key getBookKeyFromDatastore(String bookId) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query bookQuery = new Query("Book")
        .setFilter(new Query.FilterPredicate("bookId", Query.FilterOperator.EQUAL, bookId))
        .setKeysOnly();
    PreparedQuery result = datastore.prepare(bookQuery);
    if (result.countEntities(FetchOptions.Builder.withDefaults()) == 0) {
      return null;
    }
    return result.asSingleEntity().getKey();
  }
  
  /** 
   * Returns a List of "Library" Entity objects that have bookId in stock.
   */
  protected static List<Entity> getLibrariesForBook(String bookId, boolean setLimit) {
    Key bookKey = getBookKeyFromDatastore(bookId);
    if (bookKey == null) {
      // bookId is not in the datastore
      // TODO[ak47na]: handle case when bookId is not valid
      return new ArrayList<>();
    }

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query libraryQuery = new Query("Library").setAncestor(bookKey)
        .setFilter(new Query.FilterPredicate("stock", Query.FilterOperator.NOT_EQUAL, 0));
    return datastore.prepare(libraryQuery).asList(FetchOptions.Builder.withDefaults());
  }

  /** 
   * Updates the stock of library in datastore by removing one piece for each book in bookIds. This
   * method is called after an order for renting books in bookIds from library is added to datastore.  
   */
  protected static void removeBooksFromLibrary(LibraryPoint library, List<String> bookIds) {
    for (String bookId: bookIds) {
      Key bookKey = getBookKeyFromDatastore(bookId);
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      
      Query libraryQuery = new Query("Library").setAncestor(bookKey)
          .setFilter(new Query.FilterPredicate("libraryId", Query.FilterOperator.EQUAL, (int)library.getLibraryId()));
      
      Entity result = datastore.prepare(libraryQuery).asSingleEntity();
      result.setProperty("stock", ((Number) result.getProperty("stock")).intValue() - 1);
    }
  }
}
