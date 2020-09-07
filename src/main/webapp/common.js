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
var APIKey = "";

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
  let idToken = await getIdToken();
  if (document.getElementById('idToken')) {
    document.getElementById('idToken').value = idToken;
  } else {
    // /TODO[ak47na]: handle error when idToken field is not loaded
    console.log('Error');
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
 * If checkboxId element is checked, the function will update the address box addressId to user's
 * current address so that user can check its location accuracy.
 */
function updateAddressFromGeolocation(checkboxId, addressId) {
  addressBox = document.getElementById(addressId);
  if (document.getElementById(checkboxId).checked) {
    // update the address box to show the address found using Geolocation
    if (navigator.geolocation) {
        // if location was retrieved successfully, call setLatLng, else alert in geolocationError
        getLocation().then((position) => {
          setLatLng(position);
          getAddressFromLatLng(addressId)
        }).catch((error) => geolocationError(error));
    } else {
      // clear the address box if location is not supported 
      addressBox.value = ""; 
      alert("Geolocation is not supported by this browser!");
    }
  }
}

function getLocation() {
  // getCurrentPosition immediately returns when called; then asynchronously attempts to
  // obtain the current location of the device;
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
      alert("Please allow access to your location!");
      break;
    case error.POSITION_UNAVAILABLE:
      alert("Location information is unavailable!");
      break;
    case error.UNKNOWN_ERROR:
      alert("Error occured!");
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
function getAddressFromLatLng(addressId) {
  addressBox = document.getElementById(addressId);
  geocodingUrl = `https://maps.googleapis.com/maps/api/geocode/json?latlng=${document.getElementById("latitude").value},${document.getElementById("longitude").value}&key=${APIKey}`;
  fetch(geocodingUrl).then(response => response.json()).then((result) => {
    // return the best address found 
    addressBox.value = result.results[0].formatted_address;
  });
}

/**
 * Method that builds for every page with a "menu-container div" a navigable menu.
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
  let idToken = await getIdToken();
  fetch(`/user-data?idToken=${idToken}`).then(response => response.json()).then(userInfo => {
    if (userInfo.isCourier === true) {
      // Only display this page for users that are set in the database as couriers.
      see_journeys.innerText = 'See journeys';
      see_journeys.href = 'journeys.html';
      menuElement.append(see_journeys);
    }
  });
}
