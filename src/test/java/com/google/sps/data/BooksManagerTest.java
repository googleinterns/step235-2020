package com.google.sps.data;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.util.ArrayList;
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

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
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
