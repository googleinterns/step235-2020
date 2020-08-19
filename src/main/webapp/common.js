/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Common methods for both the app page.
 */

/**
 * Set the idToken input value so that it can be sent to the server
 */
function setIdToken() {
  if (document.getElementById("idToken")) {
    firebase.auth().currentUser.getIdToken(/* forceRefresh */ false).then(function(idToken) {
      document.getElementById("idToken").value = idToken;
    });
  } else {
    //TODO[ak47na]: handle error when idToken field is not loaded
    console.log("Error");
  }
}

/**
 * Set the value of timezone-offset element from the dateId element
 */
function setTimezoneOffsetFromDate(dateId) {
  date = document.getElementById(dateId);
  if (date) {
    // update timezone-offset from the date of dateId 
    timezone = new Date(date.value).getTimezoneOffset();
    document.getElementById('timezone-offset').value = timezone;
  } else {
    //TODO[ak47na]: handle error when dateId input element is not loaded
    alert("Please select a date!");
  }
}
 
/**
 * @param {string} queryString The full query string.
 * @return {!Object<string, string>} The parsed query parameters.
 */
function parseQueryString(queryString) {
  // Remove first character if it is ? or #.
  if (queryString.length &&
    (queryString.charAt(0) == '#' || queryString.charAt(0) == '?')) {
    queryString = queryString.substring(1);
  }
  var config = {};
  var pairs = queryString.split('&');
  for (var i = 0; i < pairs.length; i++) {
    var pair = pairs[i].split('=');
    if (pair.length == 2) {
      config[pair[0]] = pair[1];
    }
  }
  return config;
}

/**
 * Method that displays in the address box the old address, so it would be easier for
 * users to edit it.
 */

function displayCurrentAddress() {
  const addressBox = document.getElementById('address');
  fetch('/user-data').then(response => response.json()).then((userInfo) => {
    addressBox.value = userInfo.address;
  });
}

/**
 * Method that builds for every page with a "menu-container div" a navigable menu.
 */

function addMenu() {
  const menuElement = document.getElementsByClassName('menu-container')[0];
  const header = document.createElement('h3');
  header.innerText = 'MENU';
  menuElement.append(header);
  const home = document.createElement('a');
  home.innerText = 'Home';
  home.href = 'loggedIn.html';
  menuElement.append(home);
  const address = document.createElement('a');
  address.innerText = 'Set/Edit address';
  address.href = 'address.html';
  menuElement.append(address);
  const bookshelves = document.createElement('a');
  bookshelves.innerText = 'See your bookshelves';
  bookshelves.href = 'bookshelves.html';
  menuElement.append(bookshelves);
  const delivery_request = document.createElement('a');
  delivery_request.innerText = 'Request a delivery!';
  delivery_request.href = 'deliveryRequest.html';
  menuElement.append(delivery_request);
  const see_journeys = document.createElement('a');
  let displayJourneys;
  fetch('/user-data').then(response => response.json()).then((userInfo) => {
    console.log(userInfo);
    displayJourneys = userInfo.isCourier;
  });
  // Only display this page for users that are set in the database as couriers.
  if (displayJourneys) {
    see_journeys.innerText = 'See journeys';
    see_journeys.href = 'journeys.html';
    menuElement.append(see_journeys);
  }
}
