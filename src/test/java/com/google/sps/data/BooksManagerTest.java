package com.google.sps.data;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.maps.errors.ApiException;

import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BooksManagerTest {
  private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private final BooksManager booksManager = new BooksManager();

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void testGetLibrariesForBook() {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity bookEntity = new Entity("Book");
    bookEntity.setProperty("bookId", "1234");
    datastore.put(bookEntity);
    Entity library1 = new Entity("LibraryStock", bookEntity.getKey());
    library1.setProperty("stock", 3);
    datastore.put(library1);
    Entity library2 = new Entity("LibraryStock", bookEntity.getKey());
    library2.setProperty("stock", 0);
    datastore.put(library2);
    Entity library3 = new Entity("LibraryStock", bookEntity.getKey());
    library3.setProperty("stock", 7);
    datastore.put(library3);
    Assert.assertEquals(Arrays.asList(library1, library3), booksManager.getLibrariesForBook("1234"));
  }

  @Test
  public void testRemoveBookFromLibrary()
      throws BadRequestException, ApiException, IOException, InterruptedException, DataNotFoundException {
    LibraryPoint libraryPoint = new LibraryPoint(3, 1, 4);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity bookEntity = new Entity("Book");
    bookEntity.setProperty("bookId", "1234");
    bookEntity.setProperty("totalStock", 8);
    datastore.put(bookEntity);
    Entity library = new Entity("LibraryStock", bookEntity.getKey());
    library.setProperty("libraryId", 4);
    library.setProperty("stock", 3);
    datastore.put(library);
    booksManager.removeBooksFromLibrary(libraryPoint, Arrays.asList("1234"));
    Entity expected = datastore.prepare(new Query("LibraryStock").setAncestor(bookEntity.getKey())
      .setFilter(new Query.FilterPredicate("libraryId", Query.FilterOperator.EQUAL, 4)))
      .asSingleEntity();
    Assert.assertEquals(2, ((Number) expected.getProperty("stock")).intValue());
  }

  @Test
  public void testRemoveFromtotalstock() throws BadRequestException, ApiException, IOException, InterruptedException,
      DataNotFoundException {
    LibraryPoint libraryPoint = new LibraryPoint(3, 1, 4);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity bookEntity = new Entity("Book");
    bookEntity.setProperty("bookId", "1234");
    bookEntity.setProperty("totalStock", 6);
    datastore.put(bookEntity);
    Entity library = new Entity("LibraryStock", bookEntity.getKey());
    library.setProperty("libraryId", 4);
    library.setProperty("stock", 3);
    datastore.put(library);
    booksManager.removeBooksFromLibrary(libraryPoint, Arrays.asList("1234"));
    Entity expected = datastore.prepare(new Query("Book")
      .setFilter(new Query.FilterPredicate("bookId", Query.FilterOperator.EQUAL, "1234")))
      .asSingleEntity();
    Assert.assertEquals(5, ((Number) expected.getProperty("totalStock")).intValue());
  }

  @Test
  public void testRemoveFromLibraryThatDoesNotHaveTheBook() throws BadRequestException, ApiException, IOException,
      InterruptedException, DataNotFoundException {
    LibraryPoint libraryPoint1 = new LibraryPoint(3, 1, 1);
    LibraryPoint libraryPoint2 = new LibraryPoint(2, 2, 2);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity bookEntity = new Entity("Book");
    bookEntity.setProperty("bookId", "1234");
    bookEntity.setProperty("totalStock", 6);
    datastore.put(bookEntity);
    Entity library = new Entity("LibraryStock", bookEntity.getKey());
    library.setProperty("libraryId", 1);
    library.setProperty("stock", 3);
    datastore.put(library);
    // Just the first library point has the book in its records.
    Assert.assertEquals(false, booksManager.removeBooksFromLibrary(libraryPoint2, Arrays.asList("1234")));
  }

  @Test
  public void testRemoveBookWithZeroTotalStock() throws BadRequestException, ApiException, IOException,
      InterruptedException, DataNotFoundException {
    LibraryPoint libraryPoint1 = new LibraryPoint(3, 1, 1);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity bookEntity = new Entity("Book");
    bookEntity.setProperty("bookId", "1234");
    bookEntity.setProperty("totalStock", 0);
    datastore.put(bookEntity);
    Entity library = new Entity("LibraryStock", bookEntity.getKey());
    library.setProperty("libraryId", 1);
    library.setProperty("stock", 3);
    datastore.put(library);
    Assert.assertEquals(false, booksManager.removeBooksFromLibrary(libraryPoint1, Arrays.asList("1234")));
  }

  @Test
  public void testRemoveBookWithZeroStockInLibrary() throws BadRequestException, ApiException, IOException,
      InterruptedException, DataNotFoundException {
    LibraryPoint libraryPoint1 = new LibraryPoint(3, 1, 1);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity bookEntity = new Entity("Book");
    bookEntity.setProperty("bookId", "1234");
    bookEntity.setProperty("totalStock", 6);
    datastore.put(bookEntity);
    Entity library = new Entity("LibraryStock", bookEntity.getKey());
    library.setProperty("libraryId", 1);
    library.setProperty("stock", 0);
    datastore.put(library);
    Assert.assertEquals(false, booksManager.removeBooksFromLibrary(libraryPoint1, Arrays.asList("1234")));
  }

  @Test
  public void testCheckIfDifferentLibraryStockRemainsTheSame() throws BadRequestException, ApiException, IOException,
      InterruptedException, DataNotFoundException {
    LibraryPoint libraryPoint1 = new LibraryPoint(3, 1, 1);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity bookEntity = new Entity("Book");
    bookEntity.setProperty("bookId", "1234");
    bookEntity.setProperty("totalStock", 17);
    datastore.put(bookEntity);
    Entity library = new Entity("LibraryStock", bookEntity.getKey());
    library.setProperty("libraryId", 1);
    library.setProperty("stock", 5);
    datastore.put(library);
    Entity library2 = new Entity("LibraryStock", bookEntity.getKey());
    library2.setProperty("libraryId", 2);
    library2.setProperty("stock", 12);
    datastore.put(library2);
    booksManager.removeBooksFromLibrary(libraryPoint1, Arrays.asList("1234"));
    Entity expected = datastore.prepare(new Query("LibraryStock").setAncestor(bookEntity.getKey())
      .setFilter(new Query.FilterPredicate("libraryId", Query.FilterOperator.EQUAL, 2)))
      .asSingleEntity();
    Assert.assertEquals(12, ((Number) expected.getProperty("stock")).intValue());
  }

  @Test
  public void testGetBookStock() {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity bookEntity = new Entity("Book");
    bookEntity.setProperty("bookId", "1234");
    bookEntity.setProperty("totalStock", 28);
    datastore.put(bookEntity);
    Assert.assertEquals(28, BooksManager.getBookStock("1234"));
  }

  @Test
  public void testBookStockForBookNotInDatabase() {
    Assert.assertEquals(0, BooksManager.getBookStock("1234"));
  }
}
