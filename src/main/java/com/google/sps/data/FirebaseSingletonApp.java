package com.google.sps.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * Singleton class that will return the unique Firebase instance within the
 * entire app.
 */
public class FirebaseSingletonApp {
  private static FirebaseApp instance = initialize();

  /**
   * Initialize firebase SDK and return the FirebaseApp object
   */

  private static FirebaseApp initialize() {
    try {
      FirebaseOptions options = new FirebaseOptions.Builder()
          .setCredentials(GoogleCredentials.getApplicationDefault())
          .setDatabaseUrl("https://alphabooks-step-2020.firebaseio.com").build();

      return FirebaseApp.initializeApp(options, "AlphaBooks");
    } catch (IOException e) {
      System.out.println(e.getMessage());
      return null;
    }
  }
  // The only method that can be called outside the class.
  public static FirebaseApp getInstance() {
    return instance;
  }
}
