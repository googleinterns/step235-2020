// Obtain the API_KEY from local file.
document.addEventListener('DOMContentLoaded', () => {
  fetch('apiKey.json').then(response => response.json()).then(jsonResponse => {
    let API_KEY = jsonResponse.apiKey;
    getBookDetails(API_KEY);
  });
  /**
   * Loads the Google Books API embedded library.
  */
  google.books.load();

  google.books.setOnLoadCallback(initializeViewer);
});


// Obtain the book ID from the query string. This is the Google Books API ID.
const ID = parseQueryString(window.location.search).id;

/**
 * Function that is automatically triggered when the user is redirected to the bookDetails page.
 * Extracts the Google Books API ID of the individual book from the URL and performs a GET request
 * with that ID to obtain a JSON with all the details about that specific book.
 */

function getBookDetails(API_KEY) {
  // To get results from UK publishing houses.
  const COUNTRY = 'UK';
  let URL = `https://www.googleapis.com/books/v1/volumes/${ID}`;
  URL += `?key=${API_KEY}`;
  URL += `&country=${COUNTRY}`;
  fetch(URL).then(response => {
    if (response.status === 200) {
      response.json().then(result => {
        displayDetails(result);
      });
    } else {
      alert(`Error ${response.status}. Please try again.`);
    }
  });
}


/**
 * Completes all the fields on the page with details from the JSON answer of the API.
 * @param {JSON} bookJSON 
 */

function displayDetails(bookJSON) {
  const volumeInfo = bookJSON.volumeInfo;
  document.getElementById('title-header').innerText = volumeInfo.title;
  document.getElementById('authors').innerText =
    `Authors: ${volumeInfo.authors}`;
  document.getElementById('published-date').innerText =
    `Published date: ${volumeInfo.publishedDate}`;
  document.getElementById('category').innerText =
    `Categories: ${volumeInfo.categories}`;
  document.getElementById('publisher').innerText =
    `Publishing house: ${volumeInfo.publisher}`;
  document.getElementById('page-count').innerText =
    `Page count: ${volumeInfo.pageCount}`;
  document.getElementById('description').innerText =
    volumeInfo.description.replaceAll('<br>', '');
  document.getElementById('identifiers').innerText =
    getStringIdentifiers(volumeInfo.industryIdentifiers);
}

/**
 * identifiers array contains objects like:
 * {
 *      "type": "ISBN_10",
 *      "identifier": "0415286514"
 * }
 * The function transform them into a string.    
 * @param {Array of JSONs} identifiers 
 */

function getStringIdentifiers(identifiers) {
  return identifiers.map(item => 
      `${item.type}: ${item.identifier}`
    )
    .join('\n');
}

/**
 * Loads the current book preview.
 */
function initializeViewer() {
  let viewer = new google.books.DefaultViewer(document.getElementById('viewer-container'));
  viewer.load(`${ID}`, failLoadingViewer);
}

/**
 * When no preview is found, this is the error callback.
 */
function failLoadingViewer() {
  let noPreviewDiv = document.createElement('p');
  noPreviewDiv.setAttribute('id', 'no-preview');
  noPreviewDiv.innerText = 'No preview available for this book.';
  document.getElementById('viewer-container').append(noPreviewDiv);
}
