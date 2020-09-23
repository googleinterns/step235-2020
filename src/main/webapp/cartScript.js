/**
 * When the DOM is loaded, trigger the displayBookList callback after
 * the list is obtained from the servlet with a get request.
 */
document.addEventListener('DOMContentLoaded', async function () {
  let idToken = await getIdToken();
  fetch(`/shopping-cart?idToken=${idToken}`).then(response => response.json()).then(bookIds => {
    console.log(bookIds);
    document.getElementById('order-button').disabled = true;
    displayBookList(bookIds);
    // Add callback for placing an order.
    document.getElementById('order-button').addEventListener('click', placeOrder);
  });
});

/**
 * Create a DOM element for each book on the list and append it.
 * @param {Array of Strings} bookIds 
 */

function displayBookList(bookIds) {
  // The ul element that will contain the books.
  const listWrapper = document.getElementById('shopping-list');
  // If the cart is empty, print a message.
  if (bookIds.length === 0) {
    listWrapper.innerText = 'No items added to cart';
    return;
  }
  // Enable order button.
  document.getElementById('order-button').disabled = false;
  // Append each book as a li element.
  bookIds.forEach(async bookId => {
    let element = await createListElement(bookId);
    listWrapper.append(element);
  })
}

/**
 * Using the JSON obtained from Google Books API, display book's details
 * and also add 2 buttons: one that removes the book from the cart and one
 * that redirects the user to the detailed page of the book.
 * Will return the DOM element to be appended to the UL.
 * @param {String} bookId 
 */

async function createListElement(bookId) {
    let jsonBook = await getBookJSON(bookId);
    let idToken = await getIdToken();
    liElement = document.createElement('li');
    liElement.setAttribute('class', 'book-li');
    titleElement = document.createElement('p');
    titleElement.innerText = `${jsonBook.volumeInfo.title} by ${jsonBook.volumeInfo.authors}`;
    liElement.append(titleElement);
    detailsButton = document.createElement('button');
    detailsButton.innerText = 'View more';
    liElement.append(detailsButton);
    removeButton = document.createElement('button');
    removeButton.setAttribute('class', 'remove-button');
    removeButton.innerText = 'Remove book';
    removeButton.addEventListener('click', function () {
      let url = `/shopping-cart?idToken=${idToken}&bookId=${bookId}`;
      fetch(url, {method: 'DELETE'}).then(result => {
        //Check if the book was deleted succesfully.
        if (result.status === 200) {
          // Remove the book container only if the request was succesfull.
          liElement.remove();
        }
      });
    });
    // Redirect to the details page of the book.
    detailsButton.addEventListener('click', function() {
      location.href = `/bookDetails.html?id=${bookId}`;
    });
    liElement.append(removeButton);
    return liElement;
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
  let URL = `https://www.googleapis.com/books/v1/volumes/${id}`;
  URL += `?country=${COUNTRY}`;
  const response = await fetch(URL);
  const json = await response.json();
  return json;
}

/**
 * Will trigger the servlet that adds the current order to the datastore.
 * Firstly, will check if the user set his addres as an oder cannot be placed
 * otherwise.
 */

async function placeOrder() {
  // Check if current addres exists.
  let idToken = await getIdToken();
  let userInfo = await fetch(`/user-data?idToken=${idToken}`).then(response => response.json());
  if (userInfo.address = "") {
    alert("Please set your address before placing an order!");
    // Stop if there is no existing address.
    return;
  }
  fetch(`/place-order?idToken=${idToken}`, {method: 'POST'}).then(response => {
    if (response.status === 200) {
      alert('Order successfully placed');
    } else {
      alert(`Error ${response.status}. Please try again. Your address might not be correct.`)
      
    }
  });
}
