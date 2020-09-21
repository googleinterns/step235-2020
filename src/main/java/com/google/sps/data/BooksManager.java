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

/**
 * Class used for managing the stock of books. 
 */
public class BooksManager {

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
  public List<Entity> getLibrariesForBook(String bookId, boolean setLimit) {
    throw new UnsupportedOperationException("Method will return a List of LibraryEntities that contain bookId.");
  }

  /** 
   * Removes one occurence for each book in bookIds from library. This method is called after an
   * order for renting books in bookIds from library is added to datastore.  
   */
  public void removeBooksFromLibrary(LibraryPoint library, List<String> bookIds) {
    throw new UnsupportedOperationException("Method will remove bookIds from library in datastore.");
  }
}
