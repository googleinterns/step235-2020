/**
 * When the DOM is loaded, trigger the displayBookList callback after
 * the list is obtained from the servlet with a get request.
 */
document.addEventListener('DOMContentLoaded', async function () {
  let idToken = await getIdToken();
  fetch(`/order-history?idToken=${idToken}`).then(response => response.json()).then(bookIds => {
    console.log(bookIds);
    displayBookList(bookIds);
  });
});

/**
 * Create a DOM element for each book on the list and append it.
 * @param {Array of Strings} bookIds 
 */

function displayBookList(bookIds) {
  // The ul element that will contain the books.
  const listWrapper = document.getElementById('order-list');
  // If the user never ordered any books, print a message.
  if (bookIds.length === 0) {
    listWrapper.innerText = 'No books ordered in the past';
    return;
  }
  // Append each book as a li element.
  bookIds.forEach(async bookId => {
    let element = await createListElement(bookId);
    listWrapper.append(element);
  })
}

/**
 * Using the JSON obtained from Google Books API, display book's details
 * and also add 1 button that redirects the user to the detailed page of the book.
 * Will return the DOM element to be appended to the UL.
 * @param {String} bookId 
 */

async function createListElement(bookId) {
    let jsonBook = await getBookJSON(bookId);
    liElement = document.createElement('li');
    liElement.setAttribute('class', 'book-li');
    titleElement = document.createElement('p');
    titleElement.innerText = `${jsonBook.volumeInfo.title} by ${jsonBook.volumeInfo.authors}`;
    liElement.append(titleElement);
    detailsButton = document.createElement('button');
    detailsButton.innerText = 'View more';
    liElement.append(detailsButton);
    // Redirect to the details page of the book.
    detailsButton.addEventListener('click', function() {
      location.href = `/bookDetails.html?id=${bookId}`;
    });
    return liElement;
}
