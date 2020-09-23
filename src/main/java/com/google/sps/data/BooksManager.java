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
import java.lang.Number;
import java.util.List;

/**
 * Class used for managing the stock of books. 
 */
public class BooksManager {

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
   * Returns the total stock for the given book or 0 if book is not in the database.
   */

  public static int getBookStock(String bookId) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query = new Query("Book").setFilter(new Query.FilterPredicate("bookId", Query.FilterOperator.EQUAL, bookId));
    PreparedQuery results = datastore.prepare(query);
    if (results.asSingleEntity() == null) {
      return 0;
    } else {
      return ((Long) results.asSingleEntity().getProperty("totalStock")).intValue();
    }
  }

  /** 
   * Returns a List of "Library" Entity objects that have bookId in stock.
   */

  public List<Entity> getLibrariesForBook(String bookId) {
    Key bookKey = getBookKeyFromDatastore(bookId);
    // User can place orders only after the books are checked to be in stock,
    // so we can consider we only work with books that are in stock somewhere.

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query libraryQuery = new Query("LibraryStock").setAncestor(bookKey)
        .setFilter(new Query.FilterPredicate("stock", Query.FilterOperator.NOT_EQUAL, 0));
    return datastore.prepare(libraryQuery).asList(FetchOptions.Builder.withDefaults());  
  }

  /** 
   * Removes one occurence for each book in bookIds from library. Also, will remove once
   * occurence from thte total stock for each book. 
   * This method is called after an order for renting books in bookIds
   * from library is added to datastore.
   * Return true if the removal was succesfully done, otherwise false.
   */

  public boolean removeBooksFromLibrary(LibraryPoint library, List<String> bookIds) {
    // TODO transaction
    for (String bookId: bookIds) {
      Key bookKey = getBookKeyFromDatastore(bookId);
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      
      Query libraryQuery = new Query("LibraryStock").setAncestor(bookKey)
          .setFilter(new Query.FilterPredicate("libraryId", Query.FilterOperator.EQUAL, (int)library.getLibraryId()));
      if (datastore.prepare(libraryQuery).asList(FetchOptions.Builder.withDefaults()).size() == 0) {
        // This means this library does not have the book in its records.
        // Cannot remove the book from a library like that one.
        return false;
      }     
      Entity result = datastore.prepare(libraryQuery).asSingleEntity();
      // Remove one occurence from library.
      if (((Number) result.getProperty("stock")).intValue() <= 0) {
        return false;
      }
      // Remove 1 from total stock.
      Query bookQuery = new Query("Book").setFilter(new Query.FilterPredicate("bookId", Query.FilterOperator.EQUAL, bookId));
      Entity bookResult = datastore.prepare(bookQuery).asSingleEntity();
      // Inconsistent state.
      if (bookResult.hasProperty("totalStock") == false) {
        return false;
      }
      if (((Number) bookResult.getProperty("totalStock")).intValue() <= 0) {
        return false;
      }
      // Proceed to do the removals only after all the checks are fine.
      result.setProperty("stock", ((Number) result.getProperty("stock")).intValue() - 1);
      datastore.put(result);
      bookResult.setProperty("totalStock", ((Number) bookResult.getProperty("totalStock")).intValue() - 1);
      datastore.put(bookResult);
    }
    return true;
  }
}
