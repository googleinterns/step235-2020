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


package com.google.sps;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static net.sourceforge.jwebunit.junit.JWebUnit.assertLinkPresent;
import static net.sourceforge.jwebunit.junit.JWebUnit.assertTitleEquals;
import static net.sourceforge.jwebunit.junit.JWebUnit.assertElementPresent;
import static net.sourceforge.jwebunit.junit.JWebUnit.beginAt;
import static net.sourceforge.jwebunit.junit.JWebUnit.setBaseUrl;
import static net.sourceforge.jwebunit.junit.JWebUnit.getPageSource;
import static net.sourceforge.jwebunit.junit.JWebUnit.setTestingEngineKey;
import net.sourceforge.jwebunit.util.TestingEngineRegistry;

/** */
@RunWith(JUnit4.class)
public final class WebPageTest {

  @Before
  public void setUp() {
    setTestingEngineKey(TestingEngineRegistry.TESTING_ENGINE_HTMLUNIT); 
    //setBaseUrl("http://localhost:8080/");
    setBaseUrl("https://8080-5e8b56cb-2da4-4a7e-a2f3-2a45e97e0db8.europe-west4.cloudshell.dev");
  }

  @Test
  public void testMainPageForSignedOutUser() {
    beginAt("index.html");
    //System.out.println(getPageSource());
  }
}
