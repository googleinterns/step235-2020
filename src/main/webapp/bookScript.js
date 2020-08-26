const API_KEY = 'AIzaSyA4o8AqxFRAFJD_S5-cEXPlJMqVeGHhGLI';
// To get results from UK publishing houses.
const country = 'UK';

/**
 * Performs the GET request to the Google Books API.
 */

$(document).ready(function() {
  // When this button is clicked, the GET request is triggered.
  $('#search-button').click(function() {
    const URL = getRequestedURL();
    if (URL.length !== 0) {
      $.ajax({
        url: URL,
        type: 'GET',
        success: function(result) {
          addBooksGrid(result.items);
        },
        error: function(error) {
          alert(`Error ${error}. Please try again.`);
        },
      });
    }
  });
});

/**
 * Reads the fields completed by the user(title, author, ISBN) and computes the URL for the GET request
 * Books API. Also adds the API_KEY and the other useful queries.
 */

function getRequestedURL() {
  const title = document.getElementById('title');
  const author = document.getElementById('author');
  const isbn = document.getElementById('isbn');
  // Cannot display results for empty search queries.
  if (!title.value && !author.value && !isbn.value) {
    alert('Please complete at least one field for your search!');
    return '';
  }
  // The ISBN is unique for each book, so if the ISBN is set, the author and title are not
  // taken into account.
  if (isbn.value && (title.value || author.value)) {
    alert('Search by ISBN does not allow title/author fields');
    return '';
  }
  let url = 'https://www.googleapis.com/books/v1/volumes?q=';
  // Search by ISBN.
  if (isbn.value) {
    url += `isbn:${isbn.value}`;
  }
  // Search by title.
  if (title.value) {
    url += `intitle:${title.value}+`;
  }
  // Search by author.
  if (author.value) {
    url += `inauthor:${author.value}`;
  }
  // Set country code.
  url += `&country=${country}`;
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
  /* TODO [anamariamilcu] When database is ready, only display books
     that exist in database. */
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
  book.append(button);
  // TODO [anamariamilcu] Add onclick action to redirect to seeing details.
  return book;
}
