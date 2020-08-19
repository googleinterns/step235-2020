package com.google.sps.data;

public class UserData {
  // User's unique identifier in Firebase.
  private final String uid;
  // User's address that is editable.
  private String address;
  // This will be set to true the first time the user makes a delivery request. This way,
  // only previous signed up couriers will be able to see "See journeys" page.
  private boolean isCourier;

  public UserData(String uid) {
    this.uid = uid;
    this.address = "";
    this.isCourier = false;
  }

  public UserData(String uid, String address, boolean isCourier) {
    this.uid = uid;
    this.address = address;
    this.isCourier = isCourier;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public void setIsCourier(boolean isCourier) {
    this.isCourier = isCourier;
  }
}