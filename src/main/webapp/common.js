/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License'); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Common methods for both the app page.
 */

/**
 * Add menu when DOM is loaded.
 */
document.addEventListener('DOMContentLoaded', () => {
  if(document.getElementsByClassName('menu-container').length) {
    addMenu();
  }
});

/**
 * Set the idToken input value so that it can be sent to the server
 */
async function setIdToken() {
  const idToken = await getIdToken();
  if (document.getElementById('idToken')) {
    document.getElementById('idToken').value = idToken;
  } else {
    // /TODO[ak47na]: handle error when idToken field is not loaded
    console.log('Error');
  }
}


/**
 * Set the value of timezone-offset-minutes element from the dateId element
 */
function setTimezoneOffsetFromDate(dateId) {
  date = document.getElementById(dateId);
  if (date) {
    // update timezone-offset-minutes from the date of dateId 
    timezone = new Date(date.value).getTimezoneOffset();
    document.getElementById('timezone-offset-minutes').value = timezone;
  } else {
    //TODO[ak47na]: handle error when dateId input element is not loaded
    alert('Please select a date!');
  }
}

/**
 * @param {string} queryString The full query string.
 * @return {!Object<string, string>} The parsed query parameters.
 */
function parseQueryString(queryString) {
  // Remove first character if it is ? or #.
  if (queryString.length && (queryString.charAt(0) == '#' || queryString.charAt(0) == '?')) {
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
 * Get the idToken used in the Java Backend to access Firebase data.
 * Return it as a promise.
 */

function getIdToken() {
  return new Promise(function(resolve, reject) {
    firebase.auth().onAuthStateChanged(function(user) {
      if (user) {
        user.getIdToken(true).then(function(idToken) {
          resolve(idToken);
        });
      } else {
        // Redirect the user to home page where he can log in.
        window.location.pathname = '/index.html';
        reject(Error('You are not signed in'));
      }
    });
  });
}

/**
 * Method that displays in the address box the old address, so it would be easier for
 * users to edit it.
 */

async function displayCurrentAddress() {
  let idToken = await getIdToken();
  const addressBox = document.getElementById('address');
  fetch(`/user-data?idToken=${idToken}`).then(response => response.json()).then(userInfo => {
    addressBox.value = userInfo.address;
  });
}

/**
 * If geolocationButtonId element is checked, the function will update the address box addressId to
 * user's current address so that user can check its location accuracy. Otherwise, the latitude and 
 * longitude input elements will be disabled.
 */
function updateStartAddress(geolocationButtonId, addressId) {
  geolocationButton = document.getElementById(geolocationButtonId);
  addressBox = document.getElementById(addressId);
  if (geolocationButton.checked) {
    updateAddressBoxUsingGeolocation(addressBox);
  } else {
    // Allow the user to set the address.
    addressBox.value = ''; 
    disableCoordinates(document.getElementById('latitude'),
      document.getElementById('longitude'));
  }
}

/**
 * Disables latitude and longitude hidden input elements.
 */
function disableCoordinates(latElem, lngElem) {
  latElem.disabled = true;
  lngElem.disabled = true;
}

/**
 * Updates the address box to show the address found using Geolocation.
 */
async function updateAddressBoxUsingGeolocation(addressBox) {
  if (navigator.geolocation) {
    // If location was retrieved successfully, call setLatLng, else alert in geolocationError.
    getLocation().then((position) => {
      setLatLng(position);
      getAddressFromLatLng(addressBox);
    }).catch((error) => geolocationError(error));
  } else {
    // Clear the address box if Geolocation is not supported. 
    addressBox.value = ''; 
    alert('Geolocation is not supported by this browser!');
  }
}

/**
 * Returns a promise that resolves with user's coordinates and rejects if geolocation fails.
 */
function getLocation() {
  // GetCurrentPosition immediately returns when called; then asynchronously attempts to
  // obtain the current location of the device.
  return new Promise(function(resolve, reject) {
          navigator.geolocation.getCurrentPosition(resolve, reject);
  });
}

/**
 * Alerts user about the error message.
 */
function geolocationError(error) {
  switch(error.code) {
    case error.PERMISSION_DENIED:
      alert('Please allow access to your location!');
      break;
    case error.POSITION_UNAVAILABLE:
      alert('Location information is unavailable!');
      break;
    case error.UNKNOWN_ERROR:
      alert('Error occured!');
      break;
  }
}

/**
 * Sets latitude and longitude elements to user's coordinates using Geolocation API 
 */
function setLatLng(position) {
  document.getElementById('latitude').value = position.coords.latitude;
  document.getElementById('longitude').value = position.coords.longitude;
}

/**
 * Returns user's address using reverse geocoding on latitude and longitude elements
 */
function getAddressFromLatLng(addressBox) {
  // Obtain GoogleMapsApiKey from local file.
  fetch('apiKey.json').then(response => response.json()).then(jsonResponse => {
    GoogleMapsApiKey = jsonResponse.apiKey;
    geocodingUrl = `https://maps.googleapis.com/maps/api/geocode/json?latlng=${document.getElementById('latitude').value},${document.getElementById('longitude').value}&key=${GoogleMapsApiKey}`;
    fetch(geocodingUrl).then(response => response.json()).then((result) => {
      if (result.status != 'OK') {
        // there was an error when getting the address from the coordinates
        alert('Location information is unavailable!');
        addressBox.value = '';
      } else {
        // return the best address found 
        addressBox.value = result.results[0].formatted_address;
      }
    });
  });
}

/**
 * Fetches user's journeys from Java servlet and displayes them on journeys.html page.
 */
async function displayDeliverySlots() {
  let idToken = await getIdToken();
  fetch(`/new-delivery-request?idToken=${idToken}`).then(response => { 
    if (response.status != 200) {
      // There was an error when requesting the delivery slots from the server.
      throw new Error('Unable to get delivery slots!');
    } else {
      return response.json();
    }
  }).then(slots => {
    // Display each delivery slot as a list element.
    const deliverySlotsContainer = document.getElementById('delivery-slots-container');
    for (const slot in slots) {
      deliverySlotElem = createUlElement(`Delivery slot with ID: ${slots[slot].slotId}`);
      slotTime = createListElement(`Starts at ${slots[slot].startTime} and ends at ${slots[slot].endTime}`);
      slotAddress = createListElement(`The starting point is ${slots[slot].startPoint.latitude}, ${slots[slot].startPoint.longitude}`);
      // Show the start, end times and starting point of delivery slot as list elements. 
      // TODO[ak47na]: improve the way delivery slots are shown (e.g. add line breaks between them).
      deliverySlotElem.appendChild(slotTime);
      deliverySlotElem.appendChild(slotAddress);
      deliverySlotsContainer.appendChild(deliverySlotElem);
    }
  }).catch(error => {
    alert(error);
  });
}

/**
 * Fetches user's delivery slots from Java servlet and displayes them on deliverySlots.html page.
 */
async function displayJourneys() {
  let idToken = await getIdToken();
  fetch(`/see-journeys?idToken=${idToken}`).then(response => { 
    if (response.status != 200) {
      // There was an error when requesting the journeys from the server.
      throw new Error('Unable to get delivery journeys!');
    } else {
      return response.json();
    }
  }).then(journeys => {
    // Display each journey as a list element.
    const journeysContainer = document.getElementById('journeys-container');
    for (const index in journeys) {
      journeyProperties = journeys[index].propertyMap;
      journeyElem = createUlElement(`Journey with ID: ${journeys[index].key.id}`);
      journeyTime = createListElement(`Starts at ${journeyProperties.startDate} and ends at ${journeyProperties.endDate}`);
      journeyElem.appendChild(journeyTime);
      // Create Array object from waypoints string.
      waypoints = JSON.parse(journeyProperties.waypoints);
      // List the waypoints of the journey in the order they must be visited.
      for (const waypoint in waypoints) {
        // Add the current waypoint to the delivery journey.
        pointElem = createUlElement(`Visit point with latitude ${waypoints[waypoint].point.latitude} and longitude ${waypoints[waypoint].point.longitude}`);
        // Add the orders that must be picked up/delivered at the current point.
        for (const orderKey in waypoints[waypoint].orderKeys) {
          orderKeyElem = createListElement(`order ${waypoints[waypoint].orderKeys[orderKey]}`);
          pointElem.appendChild(orderKeyElem);
        }
        // Add the point to the journey.
        journeyElem.appendChild(pointElem);
      }
      journeyElem.appendChild(createViewOnMapButton(waypoints));
      journeyElem.appendChild(createDeleteButton('Mark Journey as complete', journeyElem));
      journeysContainer.appendChild(journeyElem);
    }
  }).catch(error => {
    alert(error);
  });
}

/**
 * Given an array of waypoints, it creates a button that onclick, creates a map and displays the
 * waypoints as markers.
 */
function createViewOnMapButton(waypoints) {
  const button = document.createElement('button');
  button.innerText = 'View journey map';
  button.onclick = addMarkersAndDetailsToMap(waypoints);
  return button;
}

/** 
 * Returns a button with text buttonText that deletes toDeletElem from DOM.
 */
function createDeleteButton(buttonText, toDeleteElem) {
  const button = document.createElement('button');
  button.innerText = buttonText;
  // Delete toDeleteElem if button is clicked
  button.onclick = function() {
    toDeleteElem.parentNode.removeChild(toDeleteElem);
  }
  return button;
}

/** 
 * Returns HTML list element with text. 
 */
function createListElement(text) {
  const liElement = document.createElement('li');
  liElement.innerText = text;
  return liElement;
}

/** 
 * Returns HTML ul element with text. 
 */
function createUlElement(text) {
  const ulElement = document.createElement('ul');
  ulElement.innerText = text;
  return ulElement;
}

/**
 * Method that builds for every page with a 'menu-container div' a navigable menu.
 */
async function addMenu() {
  const menuElement = document.getElementsByClassName('menu-container')[0];
  const header = document.createElement('h3');
  header.innerText = 'MENU';
  menuElement.append(header);
  const home = document.createElement('a');
  home.innerText = 'Home';
  home.href = 'loggedIn.html';
  menuElement.append(home);
  const cart = document.createElement('a');
  cart.innerText = 'See your cart';
  cart.href = 'cart.html';
  menuElement.append(cart);
  const address = document.createElement('a');
  address.innerText = 'Set/Edit address';
  address.href = 'address.html';
  menuElement.append(address);
  const orderHistory = document.createElement('a');
  orderHistory.innerText = 'The books you read';
  orderHistory.href = 'orderHistory.html';
  menuElement.append(orderHistory);
  const courier_header = document.createElement('h3');
  courier_header.setAttribute('id', 'courier-header');
  courier_header.innerText = 'BECOME A COURIER';
  menuElement.append(courier_header);
  const delivery_request = document.createElement('a');
  delivery_request.innerText = 'Request a delivery slot!';
  delivery_request.href = 'deliveryRequest.html';
  menuElement.append(delivery_request);
  const see_journeys = document.createElement('a');
  const view_delivery_slots = document.createElement('a');
  let idToken = await getIdToken();
  fetch(`/user-data?idToken=${idToken}`).then(response => response.json()).then(userInfo => {
    if (userInfo.isCourier === true) {
      courier_header.innerText = 'COURIER SECTION';
      // Only display this page for users that are set in the database as couriers.
      see_journeys.innerText = 'See journeys';
      see_journeys.href = 'journeys.html';
      menuElement.append(see_journeys);

      view_delivery_slots.innerText = 'View Delivery Slots';
      view_delivery_slots.href = 'deliverySlots.html';
      menuElement.append(view_delivery_slots);
    }
  });
}

/**
 * Make a request to Google Books API to obtain the JSON with details about the
 * book with given ID to be able to print the title and author of the book on
 * the shopping cart page.
 * @param {String} id 
 */

async function getBookJSON(id) {
  // To get results from UK publishing houses.
  const COUNTRY = 'UK';
  const URL = `https://www.googleapis.com/books/v1/volumes/${id}?country=${COUNTRY}`;
  const response = await fetch(URL);
  const json = await response.json();
  return json;
}
