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

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.maps.errors.ApiException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.sps.data.BadRequestException;
import com.google.sps.data.DataNotFoundException;
import com.google.sps.data.DeliverySlotManager;
import com.google.sps.data.FirebaseAuthentication;
import java.util.List;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static net.sourceforge.jwebunit.junit.JWebUnit.assertLinkPresent;
import static net.sourceforge.jwebunit.junit.JWebUnit.assertTitleEquals;
import static net.sourceforge.jwebunit.junit.JWebUnit.assertElementPresent;
import static net.sourceforge.jwebunit.junit.JWebUnit.beginAt;
import static net.sourceforge.jwebunit.junit.JWebUnit.setBaseUrl;
import static net.sourceforge.jwebunit.junit.JWebUnit.getPageSource;
import static net.sourceforge.jwebunit.junit.JWebUnit.setTestingEngineKey;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import net.sourceforge.jwebunit.util.TestingEngineRegistry;

/**
 * Tests the behaviour of requests sent to NewRequestServlet.java from the delivery-form inside
 * deliveryRequest.html
 */
@RunWith(JUnit4.class)
public final class NewRequestServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Mock
  FirebaseAuthentication firebaseAuth;

  @Before
  public void setUp() {
    helper.setUp();
    // initialize Mock objects
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  /**
   * Tests that when a date from the past is introduced for delivery-date, the servlet will send
   * HttpServletResponse.SC_BAD_REQUEST
   */
  @Test
  public void testPastDeliveryDate() throws IOException, ServletException, FirebaseAuthException {
    when(request.getParameter("idToken")).thenReturn("idToken0");
    when(request.getParameter("delivery-date")).thenReturn("2020-08-10");
    when(request.getParameter("start-time")).thenReturn("05:05");
    when(request.getParameter("end-time")).thenReturn("05:10");
    when(request.getParameter("timezone-offset-minutes")).thenReturn("180");

    NewRequestServlet requestServlet = new NewRequestServlet();
    when(firebaseAuth.getUserIdFromIdToken("idToken0")).thenReturn("user0");
    requestServlet.setFirebaseAuth(firebaseAuth);
    requestServlet.doPost(request, response);

    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid date!");
  }

  /**
   * Tests that when the start-time of the delivery request is set after end-time, the servlet will
   * send HttpServletResponse.SC_BAD_REQUEST
   */
  @Test
  public void testInvalidRequest() throws IOException, ServletException, FirebaseAuthException {
    when(request.getParameter("idToken")).thenReturn("idToken0");
    when(request.getParameter("delivery-date")).thenReturn("2020-09-26");
    when(request.getParameter("start-time")).thenReturn("05:15");
    when(request.getParameter("end-time")).thenReturn("05:10");
    when(request.getParameter("timezone-offset-minutes")).thenReturn("180");

    NewRequestServlet requestServlet = new NewRequestServlet();
    requestServlet.setFirebaseAuth(firebaseAuth);
    when(firebaseAuth.getUserIdFromIdToken("idToken0")).thenReturn("user0");
    requestServlet.doPost(request, response);
    // the date is valid, thus "Please enter a valid date!" shouldn't be sent
    verify(response, times(0)).sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid date!");
    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid time!");
  }

  /**
   * Tests that when correct data is sent to the servlet, no BAD_REQUEST errors are sent. 
   */
  @Test
  public void testNewRequestsAutomaticallyCreatesCourierProfile() throws IOException, ServletException, FirebaseAuthException {
    when(request.getParameter("delivery-date")).thenReturn("2020-10-26");
    when(request.getParameter("start-time")).thenReturn("05:15");
    when(request.getParameter("end-time")).thenReturn("15:10");
    when(request.getParameter("timezone-offset-minutes")).thenReturn("180");
    when(request.getParameter("latitude")).thenReturn("0");
    when(request.getParameter("longitude")).thenReturn("0");
    when(response.getWriter()).thenReturn(new PrintWriter(System.out));

    NewRequestServlet requestServlet = spy(new NewRequestServlet());
    when(request.getParameter("idToken")).thenReturn("idToken0");
    when(firebaseAuth.getUserIdFromIdToken("idToken0")).thenReturn("user0");
    requestServlet.setFirebaseAuth(firebaseAuth);
    requestServlet.doPost(request, response);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    // The user is a new user and it is added to datastore.
    Query userQuery = new Query("UserData")
          .setFilter(new Query.FilterPredicate("uid", Query.FilterOperator.EQUAL, "user0"));
    assertEquals(1, ds.prepare(userQuery).countEntities(FetchOptions.Builder.withLimit(10)));
    // TODO[ak47na]: update test to check that the delivery request is added to datastore after 
    // NewRequestServlet.java is refactored.
  }
}
