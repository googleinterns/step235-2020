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
public class OrderHistoryTest {
  private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private OrderHistory orderHistory = new OrderHistory();

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void testGettingHistoryForUserWithNohistory() {
    Assert.assertEquals(Arrays.asList(), orderHistory.getUserOrderHistory("1234"));
  }

  @Test
  public void testGettingUserHistory() {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity historyEntity = new Entity("UserHistory");
    historyEntity.setProperty("uid", "1234");
    ArrayList<String> books = new ArrayList<>();
    books.add("book1");
    books.add("book2");
    historyEntity.setProperty("books", books);
    datastore.put(historyEntity);
    Assert.assertEquals(Arrays.asList("book1", "book2"), orderHistory.getUserOrderHistory("1234"));
  }

  @Test
  public void testAddingToUserHistory() {
    orderHistory.addBooksToOrderHistory("1234", Arrays.asList("book1", "book2"));
    Assert.assertEquals(Arrays.asList("book1", "book2"), orderHistory.getUserOrderHistory("1234"));
    orderHistory.addBooksToOrderHistory("1234", Arrays.asList("book5", "book7"));
    Assert.assertEquals(Arrays.asList("book1", "book2", "book5", "book7"), orderHistory.getUserOrderHistory("1234"));
  }

  @Test
  public void testAddingDuplicateToUserHistory() {
    orderHistory.addBooksToOrderHistory("1234", Arrays.asList("book1", "book2"));
    Assert.assertEquals(Arrays.asList("book1", "book2"), orderHistory.getUserOrderHistory("1234"));
    orderHistory.addBooksToOrderHistory("1234", Arrays.asList("book5", "book2"));
    Assert.assertEquals(Arrays.asList("book1", "book2", "book5"), orderHistory.getUserOrderHistory("1234"));
  }
}
