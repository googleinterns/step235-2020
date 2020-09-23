package com.google.sps.data;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.maps.errors.ApiException;
import com.google.maps.PlacesApi;
import com.google.maps.model.Geometry;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.data.DatabaseHandler;
import com.google.sps.data.OrderHandler;

import java.io.File;
import java.io.IOException;
import java.lang.InterruptedException;
import java.net.URISyntaxException;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

/**
 * Class that tests the methods from DatabaseHandler.java.
 */
@RunWith(JUnit4.class)
public class DatabaseHandlerTest {
  private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private DatabaseHandler databaseHandler = spy(new DatabaseHandler());
  private final LatLng LONDON = new LatLng(51.509865, -0.1278);

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void testAddingBookToDatastore() {
    databaseHandler.addBookStockToDatastore("12345");
    Query query = new Query("Book").setFilter(new Query.FilterPredicate("bookId", Query.FilterOperator.EQUAL, "12345"));
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    Entity bookEntity = results.asSingleEntity();
    Long expectedTotalStock = (long) 185;
    // Check if total stock was added correctly to Datastore.
    Assert.assertEquals(expectedTotalStock, bookEntity.getProperty("totalStock"));
    Query librariesQuery = new Query("LibraryStock").setAncestor(bookEntity.getKey());
    int expectedNumberOfLibraries = 37;
    // Check if all children Libraries were added succesfully.
    Assert.assertEquals(expectedNumberOfLibraries,
        datastore.prepare(librariesQuery).asList(FetchOptions.Builder.withDefaults()).size());
    Query individualLibrariyQuery = new Query("LibraryStock").setAncestor(bookEntity.getKey())
        .setFilter(new Query.FilterPredicate("libraryId", Query.FilterOperator.EQUAL, 1));
    Long expectedNumberOfBooks = (long) 5;
    // Check if individual libraries have the correct number of books.
    Assert.assertEquals(expectedNumberOfBooks,
        datastore.prepare(individualLibrariyQuery).asSingleEntity().getProperty("stock"));
  }

  @Test
  public void testLoadingDataFromCSV() throws URISyntaxException, IOException {
    File csvFile = new File(this.getClass().getResource("/examples.csv").toURI());
    databaseHandler.loadBookStocksFromCSV(csvFile);
    Query query = new Query("Book").setFilter(new Query.FilterPredicate("bookId", Query.FilterOperator.EQUAL, "gqX7rQEACAAJ"));
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    // Check if book was added to datastore.
    Assert.assertEquals(1, datastore.prepare(query).asList(FetchOptions.Builder.withDefaults()).size());
  }

  @Test
  public void testLoadingDuplicateDataFromCSV() throws URISyntaxException, IOException {
    File csvFile = new File(this.getClass().getResource("/examples.csv").toURI());
    // Load CSV twice
    databaseHandler.loadBookStocksFromCSV(csvFile);
    databaseHandler.loadBookStocksFromCSV(csvFile);
    Query query = new Query("Book").setFilter(new Query.FilterPredicate("bookId", Query.FilterOperator.EQUAL, "gqX7rQEACAAJ"));
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    // Check if book was added just once to datastore.
    Assert.assertEquals(1, datastore.prepare(query).asList(FetchOptions.Builder.withDefaults()).size());
  }

  @Test(expected = DataNotFoundException.class)
  public void loadLibraryCoordinatesInvalidResponse() throws ApiException, DataNotFoundException, IOException, InterruptedException  {
    doReturn(null).when(databaseHandler).getLibraryLocationsFromPlacesApi(LONDON);
    databaseHandler.loadLibraryCoordinates(LONDON);
  }

  @Test
  public void loadLibraryCoordinates() throws ApiException, DataNotFoundException, IOException, InterruptedException  {
    PlacesSearchResponse response = new PlacesSearchResponse();
    response.results = new PlacesSearchResult[37];
    for (int i = 0; i < 37; ++ i) {
      response.results[i] = new PlacesSearchResult();
      response.results[i].geometry = new Geometry();
      response.results[i].geometry.location = new LatLng(0.1 * i, 0.1 * i);
    }
    doReturn(response).when(databaseHandler).getLibraryLocationsFromPlacesApi(LONDON);
    databaseHandler.loadLibraryCoordinates(LONDON);
    // Test that libraries are added to datastore.
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List<Entity> libraryEntities = datastore.prepare(new Query("Library").addSort(OrderHandler.OrderProperty.LIBRARY_ID.label, SortDirection.ASCENDING))
      .asList(FetchOptions.Builder.withDefaults());
    int index = 0;
    for (Entity libraryEntity : libraryEntities) {
      Assert.assertEquals(index, ((Number)libraryEntity.getProperty(OrderHandler.OrderProperty.LIBRARY_ID.label)).intValue());
      Assert.assertEquals((double)0.1 * index, (double)libraryEntity.getProperty(OrderHandler.OrderProperty.LIBRARY_LAT.label), 0.00001);
      Assert.assertEquals((double)0.1 * index, (double)libraryEntity.getProperty(OrderHandler.OrderProperty.LIBRARY_LNG.label), 0.00001);
      index += 1;
    }
  }
}
