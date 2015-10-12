/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gct.idea.git;

import com.google.gct.idea.elysium.SelectUserDialog;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.google.gct.login.MockGoogleLogin;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.testFramework.LightIdeaTestCase;
import com.intellij.util.AuthData;
import git4idea.DialogManager;
import org.mockito.Mockito;
import org.picocontainer.defaults.DefaultPicoContainer;

import java.util.LinkedHashMap;

import static org.mockito.Mockito.when;

/**
 * Tests for {@link com.google.gct.idea.git.GcpHttpAuthDataProvider}.
 */
public class GcpHttpAuthProviderTest extends LightIdeaTestCase {
  public static final String GOOGLE_URL = "https://source.developers.google.com";
  private static final String USER = "user@gmail.com";
  private static final String PASSWORD = "123";
  private static final String CACHE_KEY = "com.google.gct.idea.git.username";

  private TestDialogManager myDialogManager;
  private MockGoogleLogin myGoogleLogin;
  private boolean myDialogShown;
  private MockProject myProject;


  @Override
  protected final void setUp() throws Exception {
    super.setUp();

    Disposable disposable = new SimpleDisposable();
    myProject = new MockProject(new DefaultPicoContainer(), disposable);

    /*MockComponentManager componentManager = (MockComponentManager) ourProject; //= new MockComponentManager(null, disposable);
    componentManager.registerService(DialogManager.class, TestDialogManager.class);*/
    myProject.registerService(DialogManager.class, TestDialogManager.class);
    myDialogManager = (TestDialogManager) ServiceManager.getService(DialogManager.class);

    myGoogleLogin = new MockGoogleLogin();
    myGoogleLogin.install();

    GoogleLoginState googleLoginState = Mockito.mock(GoogleLoginState.class);
    CredentialedUser user = Mockito.mock(CredentialedUser.class);
    LinkedHashMap<String, CredentialedUser> allUsers = new LinkedHashMap();

    when(user.getEmail()).thenReturn(USER);
    when(user.getGoogleLoginState()).thenReturn(googleLoginState);
    when(googleLoginState.fetchAccessToken()).thenReturn(PASSWORD);
    when(GoogleLogin.getInstance().getAllUsers()).thenReturn(allUsers);
    allUsers.put(USER, user);

    PropertiesComponent.getInstance(myProject).unsetValue(CACHE_KEY);
    //PropertiesComponent.getInstance(ourProject).unsetValue(CACHE_KEY);
    //GcpHttpAuthDataProvider.setCurrentProject(ourProject);
    GcpHttpAuthDataProvider.setCurrentProject(myProject);

    myDialogShown = false;

    myDialogManager.registerDialogHandler(SelectUserDialog.class, new TestDialogHandler<SelectUserDialog>() {
      @Override
      public int handleDialog(SelectUserDialog dialog) {
        dialog.setSelectedUser(USER);
        myDialogShown = true;
        return 0;
      }
    });
  }

  @Override
  protected final void tearDown() throws Exception {
    myGoogleLogin.cleanup();
    PropertiesComponent.getInstance(myProject).unsetValue(CACHE_KEY);
//    PropertiesComponent.getInstance(ourProject).unsetValue(CACHE_KEY);
    GcpHttpAuthDataProvider.setCurrentProject(null);
    myDialogManager.cleanup();
    super.tearDown();
  }

  public void testOnlyForGcp() {
    GcpHttpAuthDataProvider authDataProvider = new GcpHttpAuthDataProvider();
    AuthData result = authDataProvider.getAuthData("http://someotherurl.myurl.com");

    assertNull(result);
  }

  public void testForGcpPrompt() {
    GcpHttpAuthDataProvider authDataProvider = new GcpHttpAuthDataProvider();
    AuthData result = authDataProvider.getAuthData(GOOGLE_URL);

    assertTrue(myDialogShown);
    assertEquals(USER, result.getLogin());
    assertEquals(PASSWORD, result.getPassword());
    assertEquals(USER, PropertiesComponent.getInstance(myProject).getValue(CACHE_KEY));
  }

  public void testForCachedState() {
    PropertiesComponent.getInstance(ourProject).setValue(CACHE_KEY, USER);

    GcpHttpAuthDataProvider authDataProvider = new GcpHttpAuthDataProvider();
    AuthData result = authDataProvider.getAuthData(GOOGLE_URL);

    assertTrue(!myDialogShown);
    assertEquals(USER, result.getLogin());
    assertEquals(PASSWORD, result.getPassword());
    assertEquals(USER, PropertiesComponent.getInstance(ourProject).getValue(CACHE_KEY));
  }

  public void testForInvalidCachedState() {
    PropertiesComponent.getInstance(ourProject).setValue(CACHE_KEY, "invalidusername");

    GcpHttpAuthDataProvider authDataProvider = new GcpHttpAuthDataProvider();
    AuthData result = authDataProvider.getAuthData(GOOGLE_URL);

    assertTrue(myDialogShown);
    assertEquals(USER, result.getLogin());
    assertEquals(PASSWORD, result.getPassword());
    assertEquals(USER, PropertiesComponent.getInstance(ourProject).getValue(CACHE_KEY));
  }

  private static class SimpleDisposable implements Disposable {
    @Override
    public void dispose() {
    }
  }
}
