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
package com.google.sps.data;

import com.google.maps.model.LatLng;
import com.google.sps.data.Point;
import com.google.maps.errors.ApiException;
import com.google.sps.data.MapsRequest;
import java.io.IOException;
import java.lang.InterruptedException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class that represents a library point in a delivery journey using its latitude and longitude.
 */
public class LibraryPoint extends Point {
  private ArrayList<String> booksToBorrow;
  private int libraryId;

  public LibraryPoint(double latitude, double longitude, int id) {
    super(latitude, longitude);
    booksToBorrow = new ArrayList<>();
    this.libraryId = id;
  }

  public int getLibraryId() {
    return libraryId;
  }
  public void setLibraryId(int libraryId) {
    this.libraryId = libraryId;
  }

  /**
   * Adds the Google Books ids of the books from bookIdsString to the books that must be borrowed
   * from this library during current delivery
   */
  public void addBooks(String bookIdsString) {
    String[] bookIds = bookIdsString.split("&");
    booksToBorrow.addAll(Arrays.asList(bookIds));
  }

  /**
   * Returns the Google Books ids of the books that must be borrowed from this library during
   * current delivery.
   */
  public ArrayList<String> getBooksToBorrow() {
    return booksToBorrow;
  }
}
