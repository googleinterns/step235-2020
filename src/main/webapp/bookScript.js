/**
 * Performs the GET request to the Google Books API.
 */

document.addEventListener('DOMContentLoaded', () => {
  // Disable button initially
  if (document.getElementById('search-button')) {
    document.getElementById('search-button').disabled = true;
    // Obtain the API_KEY from local file.
    fetch('apiKey.json').then(response => response.json()).then(jsonResponse => {
      apikey = jsonResponse.apiKey;
      document.getElementById('search-button').disabled = false;
      // When this button is clicked, the GET request is triggered.
      document.getElementById('search-button').addEventListener('click', function() {
        const URL = getRequestedURL(apikey);
        if (URL.length !== 0) {
          fetch(URL).then(response => {
            if (response.status === 200) {
              response.json().then(result => {
                const bookList = extractBookList(result);
                if (bookList !== null) {
                  addBooksGrid(bookList);
                }
              });
            } else {
              alert(`Error ${response.status}. Please try again.`);
            }
          });
        }
      });
    });
  }
});

/**
 * Function that ensures that when the list of books returned by Books API is null,
 * the app notifies the user.
 * @param {JSON} response 
 */

function extractBookList(response) {
  if (response.hasOwnProperty('items')) {
    return response.items;
  } else {
    alert('No results found. Please try different search queries, ISBN might not exist.')
    return null;
  }
}

/**
 * Reads the fields completed by the user(title, author, ISBN) and computes the URL for the GET request
 * Books API. Also adds the API_KEY and the other useful queries.
 */

function getRequestedURL(API_KEY) {
  const title = document.getElementById('title');
  const author = document.getElementById('author');
  const isbn = document.getElementById('isbn');
  const MAX_RESULTS = 30;
  return processURL(title.value, author.value, isbn.value, API_KEY, MAX_RESULTS);
}

function processURL(title, author, isbn, API_KEY, MAX_RESULTS) {
  // To get results from UK publishing houses.
  const COUNTRY = 'UK';
  // Cannot display results for empty search queries.
  if (!title && !author && !isbn) {
    alert('Please complete at least one field for your search!');
    return '';
  }
  // The ISBN is unique for each book, so if the ISBN is set, the author and title are not
  // taken into account.
  if (isbn && (title || author)) {
    alert('Search by ISBN does not allow title/author fields');
    return '';
  }
  let url = 'https://www.googleapis.com/books/v1/volumes?q=';
  // Search by ISBN.
  if (isbn) {
    url += `isbn:${isbn}`;
  }
  // Search by title.
  if (title) {
    url += `intitle:${title}+`;
  }
  // Search by author.
  if (author) {
    url += `inauthor:${author}`;
  }
  // Set COUNTRY code.
  url += `&country=${COUNTRY}`;
  // Set how many books should be displayed.
  url += `&maxResults=${MAX_RESULTS}`;
  url += `&key=${API_KEY}`;
  return url;
}

/**
 * Appends each created book div to the main page.
 * @param {Array} bookList 
 */

function addBooksGrid(bookList) {
  const bookGrid = document.getElementById('book-grid');
  // Safely remove the old contents.
  bookGrid.innerHTML = '';
  // Add every book to the main page grid.
  bookList.forEach(book => bookGrid.append(createBookElement(book)));
}

/**
 * From a JSON with all the details about a books, builds the individual div with image,
 * title, authors and a button that will redirect users to a detailed page.
 * @param {JSON} bookJSON 
 */

function createBookElement(bookJSON) {
  let book = document.createElement('div');
  book.setAttribute('class', 'book-item');
  let cover = document.createElement('div');
  cover.setAttribute('class', 'cover');
  let image = document.createElement('img');
  if (bookJSON.volumeInfo.hasOwnProperty('imageLinks')) {
    image.src = bookJSON.volumeInfo.imageLinks.thumbnail;
  } else {
    image.alt = 'No image available';
  }
  cover.append(image);
  book.append(cover);
  let title = document.createElement('p');
  title.setAttribute('class', 'title');
  title.innerHTML = bookJSON.volumeInfo.title;
  book.append(title);
  let authors = document.createElement('p');
  authors.setAttribute('class', 'authors');
  authors.innerHTML = bookJSON.volumeInfo.authors;
  book.append(authors);
  let button = document.createElement('button');
  button.textContent = 'View Details';
  button.addEventListener('click', function() {
    location.href = `/bookDetails.html?id=${bookJSON.id}`;
  });
  book.append(button);
  return book;
}
