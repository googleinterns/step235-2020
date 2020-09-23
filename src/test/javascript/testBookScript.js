describe('Testing bookScript.js - processURL', () => {
  let apiKey;
  beforeEach(function () {
    apiKey = 'xyz';
  })

  it('test if it parses the correct URL when author and title are set', () => {
    let title = 'Title';
    let author = 'Author';
    let isbn = null;
    let url = 'https://www.googleapis.com/books/v1/volumes?q=intitle:Title+inauthor:Author&country=UK&maxResults=20&key=xyz';
    expect(processURL(title, author, isbn, apiKey, 20)).toEqual(url);
  })

  it('test if it parses the correct URL when only isbn is set', () => {
    let title = null;
    let author = null;
    let isbn = '12345';
    let url = 'https://www.googleapis.com/books/v1/volumes?q=isbn:12345&country=UK&maxResults=20&key=xyz';
    expect(processURL(title, author, isbn, apiKey, 20)).toEqual(url);
  })

  it('test if it parses the correct URL when author and title and isbn are set', () => {
    let title = 'Title';
    let author = 'Author';
    let isbn = '12345';
    expect(processURL(title, author, isbn, apiKey, 20)).toEqual('');
  })

  it('test if it parses the correct URL when nothing is set', () => {
    let title = null;
    let author = null;
    let isbn = null;
    expect(processURL(title, author, isbn, apiKey, 20)).toEqual('');
  })

})

describe('Testing detailScript.js - getStringIdentifiers', () => {

  it('test if it parses correctly an array', () => {
    let identifiers = [
      {
        "type": "ISBN_10",
        "identifier": "0871293870"
      },
      {
        "type": "ISBN_13",
        "identifier": "9780871293879"
      }
    ];
    expect(getStringIdentifiers(identifiers)).toEqual('ISBN_10: 0871293870\nISBN_13: 9780871293879');
  })
})

describe('Testing bookScript.js - extractBookList', () => {
  it('returns null when reponse does not have any items', () => {
    let response = {
      "kind": "books#volumes",
      "totalItems": 0
    };
    expect(extractBookList(response)).toEqual(null);
  });
});

describe('Testing common.js - parseQueryString', () => {
  it('parses query strings', () => {
    const testData = '?param1=value1&param2=value2';
    const result = parseQueryString(testData);

    expect(result).toEqual({
      param1: 'value1',
      param2: 'value2',
    });
  });

  it('parses query strings that are empty', () => {
    const testData = '';
    const result = parseQueryString(testData);

    expect(result).toEqual({});
  });
});

describe('Testing common.js - getBookJSON', () => {
  it('getting the correct info about a book', async () => {
    const result = await getBookJSON('OBM3AAAAIAAJ');

    expect(result.volumeInfo.title).toEqual('The Sign of Four');
    expect(result.volumeInfo.pageCount).toEqual(283);
    expect(result.volumeInfo.publisher).toEqual('Spencer Blacket');
  });
});
