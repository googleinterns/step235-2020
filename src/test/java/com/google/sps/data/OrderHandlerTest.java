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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.maps.errors.ApiException;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.data.OrderHandler;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/** 
 * Class that tests the methods in OrderHandler.java.
 */
@RunWith(JUnit4.class)
public class OrderHandlerTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private OrderHandler orderHandler;
  private ArrayList<LibraryPoint> libraries;
  private List<String> bookIds;
  Point address;


  @Before
  public void setUp() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    helper.setUp();
    orderHandler = new OrderHandler(new ManhattanDistancePathFinder());
    putLibraryEntity(8, 1, 0);
    putLibraryEntity(7, 4, 1);
    putLibraryEntity(6, 6, 2);
    putLibraryEntity(0, 0, 3);
    Point address = new Point(3, 3);
    bookIds = Arrays.asList("book1", "book2", "book3", "book4");
  }

  private void putLibraryEntity(double lat, double lng, int id) {
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity library = new Entity("Library");
    library.setProperty("libraryLatitude", lat);
    library.setProperty("libraryLongitude", lng);
    library.setProperty("libraryId", id);
    ds.put(library);
  }

  private Entity getLibraryEntity(Entity book, int libraryId, int stock) {
    Key bookKey = book.getKey();
    Entity libraryStock = new Entity("LibraryStock", bookKey);
    libraryStock.setProperty("libraryId", libraryId);
    libraryStock.setProperty("stock", stock);
    return libraryStock;
  }

  private void addBookToDatastore(DatastoreService ds, String bookId, List<Integer> bookLibraries) {
    Entity book = new Entity("Book");
    book.setProperty("bookId", bookId);
    book.setProperty("totalStock", 5);
    ds.put(book);
    for (Integer library : bookLibraries) {
      ds.put(getLibraryEntity(book, library, 1));
    }
  }
  private void inintializeBooksDatastore() {
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    addBookToDatastore(ds, "book1", Arrays.asList(0, 2));
    addBookToDatastore(ds, "book2", Arrays.asList(0, 1));
    addBookToDatastore(ds, "book3", Arrays.asList(0, 2, 3));
    addBookToDatastore(ds, "book4", Arrays.asList());
  }

  private void createOrder(CourierStop library, CourierStop recipient, List<String> bookIds, String userId) {
    String orderKey = orderHandler.addOrderToDatastore((LibraryPoint)library.getPoint(), bookIds, userId, recipient.getPoint());
  }

  @Test
  public void testCreateLibrarypoint() throws BadRequestException, ApiException, IOException, InterruptedException,
      DataNotFoundException {
    LibraryPoint expected = new LibraryPoint(8, 1, 0);
    Assert.assertEquals(expected, orderHandler.createLibraryPoint(0));
  }

  @Test
  public void testInvalidOrder() throws InterruptedException, ApiException, IOException, DataNotFoundException, BadRequestException {
    inintializeBooksDatastore();
    Collection<String> expected = Arrays.asList("book4");
    // Book4 is not in stock, thus, it will be returned.
    Assert.assertEquals(expected, orderHandler.makeOrders("user1", new Point(3, 3), bookIds));
  }

  @Test
  public void testNoBooksOrdered() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    inintializeBooksDatastore();
    Collection<String> expected = Arrays.asList();
    // No books are ordered, thus no book is out of stock and no order is added to datastore.
    Assert.assertEquals(expected, orderHandler.makeOrders("user1", new Point(3, 3), Arrays.asList()));
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Assert.assertEquals(0, ds.prepare(new Query("Order")).countEntities(FetchOptions.Builder.withLimit(10)));
  }

  @Test
  public void testOneSuccesfulOrder() throws InterruptedException, ApiException, IOException, DataNotFoundException, BadRequestException {
    inintializeBooksDatastore();
    Collection<String> expected = Arrays.asList();
    // The ordered books are book1 and book2. Both books are in stock and the closest library for
    // both of them is Library2, thus, one order is created. 
    Assert.assertEquals(expected, orderHandler.makeOrders("user1", new Point(3, 3), Arrays.asList("book1", "book3")));
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Assert.assertEquals(1, ds.prepare(new Query("Order")).countEntities(FetchOptions.Builder.withLimit(10)));
  }

  @Test
  public void testTwoSuccesfulOrders() throws InterruptedException, ApiException, IOException, DataNotFoundException, BadRequestException {
    inintializeBooksDatastore();
    Collection<String> expected = Arrays.asList();
    Assert.assertEquals(expected, orderHandler.makeOrders("user1", new Point(3, 3), Arrays.asList("book1", "book2", "book3")));
    // The ordered books are book1, book2 and book3 and all of them are in stock. Library 2 is the
    // closest library that has book1 and book2. Library 1 is the closest one that has book2, thus
    // 2 orders are created.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Assert.assertEquals(2, ds.prepare(new Query("Order")).countEntities(FetchOptions.Builder.withLimit(10)));
  }

  @Test
  public void testGetClosestLibrary() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    inintializeBooksDatastore();
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    List<Entity> libraryEntities = ds.prepare(new Query("LibraryStock")).asList(FetchOptions.Builder.withDefaults());
    
    Entity closestLibrary = orderHandler.getClosestLibrary(libraryEntities, new Point(0, 1));
    // The closest library for (0, 1) is library 3 at point (0, 0).
    Assert.assertEquals(3, ((Number)closestLibrary.getProperty("libraryId")).intValue());
    closestLibrary = orderHandler.getClosestLibrary(libraryEntities, new Point(7, 1));
    // The closest library for (7, 1) is library 0 at point (8, 1).
    Assert.assertEquals(0, ((Number)closestLibrary.getProperty("libraryId")).intValue());
  }

  public List<String> getEntitiesKeyString(List<Entity> results) {
    List <String> resultsKeyStrings = new ArrayList<>();
    for (Entity order : results) {
      resultsKeyStrings.add(KeyFactory.keyToString(order.getKey()));
    }
    return resultsKeyStrings;
  }

  @Test
  public void testGetAvailableOrders() throws ApiException, BadRequestException, DataNotFoundException, IOException, InterruptedException {
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    String orderKey1 = orderHandler.addOrderToDatastore(new LibraryPoint(0, 0, 0), Arrays.asList("book1", "book2", "book3"), "user1", new Point(3, 3));
    String orderKey2 = orderHandler.addOrderToDatastore(new LibraryPoint(1, 1, 1), Arrays.asList("book1", "book3"), "user1", new Point(3, 3));
   
    List<String> expected = Arrays.asList(orderKey1, orderKey2); 
    Assert.assertEquals(expected, orderHandler.getAvailableOrders(new Point(3, 3).getArea()));
  }

  @Test
  public void testUpdateStatusForOrders() throws ApiException, BadRequestException, DataNotFoundException, EntityNotFoundException, IOException, InterruptedException {
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    String orderKey1 = orderHandler.addOrderToDatastore(new LibraryPoint(0, 0, 0), Arrays.asList("book1", "book2", "book3"), "user1", new Point(3, 3));
    String orderKey2 = orderHandler.addOrderToDatastore(new LibraryPoint(1, 1, 1), Arrays.asList("book1", "book3"), "user1", new Point(3, 3));
    String orderKey3 = orderHandler.addOrderToDatastore(new LibraryPoint(1, 1, 1), Arrays.asList("book1", "book3"), "user0", new Point(3, 3));
    // Change the status of orderKey1 and orderKey2 to ASSIGNED.
    orderHandler.updateStatusForOrders(Arrays.asList(orderKey1, orderKey2), OrderHandler.OrderStatus.ASSIGNED.toString());
    // Test that the status of the 2 orders has changed to ASSIGNED.
    List<String> actual = getEntitiesKeyString(ds.prepare(new Query("Order").
      setFilter(new Query.FilterPredicate(OrderHandler.OrderProperty.STATUS.label, Query.FilterOperator.EQUAL, OrderHandler.OrderStatus.ASSIGNED.toString())))
        .asList(FetchOptions.Builder.withDefaults()));
    Assert.assertEquals(Arrays.asList(orderKey1, orderKey2), actual);
  }

  @Test
  public void testGetProperty() throws ApiException, BadRequestException, DataNotFoundException, IOException, EntityNotFoundException, InterruptedException {
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    String orderKey1 = orderHandler.addOrderToDatastore(new LibraryPoint(0, 0, 0), Arrays.asList("book1", "book2", "book3"), "user1", new Point(3, 3));
    Assert.assertEquals(OrderHandler.OrderStatus.ADDED.toString(), (String)orderHandler.getProperty(orderKey1, OrderHandler.OrderProperty.STATUS.label));
    Assert.assertEquals("user1", (String)orderHandler.getProperty(orderKey1, OrderHandler.OrderProperty.USER_ID.label));
    Assert.assertEquals(0, ((Number)orderHandler.getProperty(orderKey1, OrderHandler.OrderProperty.LIBRARY_ID.label)).intValue());
    Assert.assertEquals(0.0, orderHandler.getProperty(orderKey1, OrderHandler.OrderProperty.LIBRARY_LAT.label));
    Assert.assertEquals(0.0, orderHandler.getProperty(orderKey1, OrderHandler.OrderProperty.LIBRARY_LNG.label));
    Assert.assertEquals(3.0, orderHandler.getProperty(orderKey1, OrderHandler.OrderProperty.RECIPIENT_LAT.label));
    Assert.assertEquals(3.0, orderHandler.getProperty(orderKey1, OrderHandler.OrderProperty.RECIPIENT_LNG.label));
    Assert.assertEquals(Arrays.asList("book1", "book2", "book3"), (List<String>)orderHandler.getProperty(orderKey1, OrderHandler.OrderProperty.BOOK_IDS.label));
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }
}
