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
public class ShoppingCartHandlerTest {
  private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private ShoppingCartHandler cartHandler = new ShoppingCartHandler();

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void testGettingShoppingCart() {
    ArrayList<String> books = new ArrayList<>();
    // Test empty shopping cart.
    Assert.assertEquals(books, cartHandler.getUserShoppingCart("1234"));

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity cartEntity = new Entity("UserCart");
    cartEntity.setProperty("uid", "1234");
    books.add("book1");
    books.add("book2");
    cartEntity.setProperty("books", books);
    datastore.put(cartEntity);
    Assert.assertEquals(books, cartHandler.getUserShoppingCart("1234"));
  }

  @Test
  public void testAddingToShoppingCart() {
    cartHandler.addBookToShoppingCart("1234", "book1");
    Assert.assertEquals(Arrays.asList("book1"), cartHandler.getUserShoppingCart("1234"));
  }

  @Test
  public void testDeletingFromShoppingCart() {
    cartHandler.addBookToShoppingCart("1234", "book1");
    cartHandler.addBookToShoppingCart("1234", "book2");
    // Test deleting a single book
    cartHandler.deleteBookFromShoppingCart("1234", "book1");
    Assert.assertEquals(Arrays.asList("book2"), cartHandler.getUserShoppingCart("1234"));
    // Test deleting the second book.
    cartHandler.deleteBookFromShoppingCart("1234", "book2");
    Assert.assertEquals(new ArrayList<String>(), cartHandler.getUserShoppingCart("1234"));
  }
}
