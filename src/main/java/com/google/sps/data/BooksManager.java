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
   * Returns true if there is at least one library that has the book with bookId in stock and false
   * otherwise.
   */
  protected static boolean isBookInStore(String bookId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the Key of book bookId from datastore or null if there is no book with bookId.
   */
  private static Key getBookKeyFromDatastore(String bookId) {
    throw new UnsupportedOperationException("Method will return the Key of book with id bookId");
  }

  /** 
   * Returns a List of "Library" Entity objects that have bookId in stock.
   */
  protected static List<Entity> getLibrariesForBook(String bookId, boolean setLimit) {
    throw new UnsupportedOperationException("Method will return a List of LibraryEntities that contain bookId.");
  }

  /** 
   * Removes one occurence for each book in bookIds from library. This method is called after an
   * order for renting books in bookIds from library is added to datastore.  
   */
  protected static void removeBooksFromLibrary(LibraryPoint library, List<String> bookIds) {
    throw new UnsupportedOperationException("Method will remove bookIds from library in datastore.");
  }
}
