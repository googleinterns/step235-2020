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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.*;

/**
 * Class that uses FirebaseApp to manage users.
 */
public class FirebaseAuthentication {
  private FirebaseApp firebaseApp;

  public FirebaseAuthentication(FirebaseApp firebaseApp) {
    this.firebaseApp = firebaseApp;
  }

  public FirebaseApp getFirebaseApp() {
    return firebaseApp;
  }
  
  public String getUserIdFromIdToken(String idToken) throws FirebaseAuthException {
    FirebaseToken decodedToken = FirebaseAuth.getInstance(firebaseApp).verifyIdToken(idToken);
    return decodedToken.getUid();
  }

  public UserRecord getUserRecordFromUserId(String uid) throws FirebaseAuthException {
    return FirebaseAuth.getInstance(firebaseApp).getUser(uid);
  }

//   public UserData getUserFromUserId(String uid) {
      
//   }
}
