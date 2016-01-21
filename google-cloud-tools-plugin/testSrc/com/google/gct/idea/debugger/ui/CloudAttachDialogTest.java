/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.debugger.ui;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.clouddebugger.model.Debuggee;
import com.google.gct.idea.debugger.CloudDebugProcessState;
import com.google.gct.idea.debugger.ProjectRepositoryValidator;
import com.google.gct.idea.debugger.SyncResult;
import com.google.gct.idea.elysium.ProjectSelector;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.google.gct.login.MockGoogleLogin;
import com.google.gdt.eclipse.login.common.GoogleLoginState;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.testFramework.PlatformTestCase;

import java.util.LinkedHashMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;

public class CloudAttachDialogTest extends PlatformTestCase {
  private static final String NO_LOGIN_WARNING = "You must be logged in to perform this action.";
  private static final String NO_PROJECT_ID_WARNING = "Please enter a Project ID.";
  private static final String NO_MODULES_WARNING = "No debuggable modules found.";
  private static final String SELECT_VALID_PROJECT_WARNING = "Please select a project with debuggable modules.";

  private static final String USER = "test@user.com";
  private static final String PASSWORD = "123";
  private CredentialedUser user;

  private ProjectSelector projectSelector;
  private CloudAttachDialog dialog;
  private ProjectDebuggeeBinding binding;
  private JComboBox moduleSelector;
  private JLabel warningHeader;
  private JLabel warningMessage;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mockCredentials();
  }

  public void testErrorWhenUserIsLoggedOut() {
    initDialog();
    mockLoggedOutUser();
    ValidationInfo error = dialog.doValidate();

    assertNotNull(error);
    assertEquals(NO_LOGIN_WARNING, error.message);
  }

  public void testNoProjectSelected() {
    initDialog();
    mockLoggedInUser();
    ValidationInfo error = getValidationError();

    assertNotNull(error);
    assertEquals(NO_PROJECT_ID_WARNING, error.message);
  }

  public void testNoModulesFound() {
    initDialog();
    mockLoggedInUser();
    selectEmptyProject();
    ValidationInfo error = getValidationError();

    assertNotNull(error);
    assertEquals(SELECT_VALID_PROJECT_WARNING, error.message);
    assertFalse(warningMessage.isVisible());
    assertFalse(warningHeader.isVisible());

    assertFalse(moduleSelector.isEnabled());
  }

  public void testDebuggableModuleSelected() {
    mockLoggedInUser();

    binding = mock(ProjectDebuggeeBinding.class);
    when(binding.buildResult(any(Project.class))).thenReturn(new CloudDebugProcessState());

    ProjectRepositoryValidator repositoryValidator = mock(ProjectRepositoryValidator.class);
    SyncResult syncResult = mockDebuggableSyncResult();
    when(repositoryValidator.checkSyncStashState()).thenReturn(syncResult);

    initDialog();
    dialog.setProjectRepositoryValidator(repositoryValidator);

    selectProjectWithDebuggableModules();
    ValidationInfo error = getValidationError();

    assertNull(error);

    assertFalse(warningMessage.isVisible());
    assertFalse(warningHeader.isVisible());

    assertTrue(moduleSelector.isEnabled());
  }

  /**
   * If an unidentified project is chosen, there should be a default module selected
   * in a disabled state with warning text
   *
   * Related to Issue #309 - if there is no module loaded (including no default module)
   * then this indicates that the async module loading is still in progress. We do not
   * want to display a warning to the user until the module loading is complete to avoid
   * the flashing warning message as described by this issue.
   */
  public void testUnknownProjectSelected() {
    initDialog();
    mockLoggedInUser();
    selectUnknownProject();

    ValidationInfo error = getValidationError();

    assertNotNull(error);

    assertFalse(moduleSelector.isEnabled());


    // TODO this currently fails, partially confirming issue #309
    // uncomment once bug is fixed
//    assertEquals(SELECT_VALID_PROJECT_WARNING, moduleSelector.getSelectedItem());
  }

  private void initDialog() {
    dialog = new CloudAttachDialog(this.getProject(), binding);
    projectSelector = dialog.getElysiumProjectSelector();
    moduleSelector = dialog.getDebuggeeTarget();
    warningHeader = dialog.getWarningHeader();
    warningMessage = dialog.getWarningMessage();
  }

  @SuppressWarnings("unchecked")
  private void selectEmptyProject() {
    projectSelector.setText("emptyProject");
    moduleSelector.setEnabled(false);
    moduleSelector.addItem(NO_MODULES_WARNING);
  }

  @SuppressWarnings("unchecked")
  private void selectProjectWithDebuggableModules() {
    String projectName = "projectWithDebuggableModules";
    projectSelector.setText(projectName);
    moduleSelector.setEnabled(true);

    DebugTarget debugTarget = new DebugTarget(new Debuggee(), projectName);
    moduleSelector.addItem(debugTarget);
  }

  private ValidationInfo getValidationError() {
    return dialog.doValidate();
  }

  private void selectUnknownProject() {
    String projectName = "unknownProject";
    projectSelector.setText(projectName);
  }

  private void mockCredentials() throws Exception {
    MockGoogleLogin googleLogin = new MockGoogleLogin();
    googleLogin.install();

    GoogleLoginState googleLoginState = mock(GoogleLoginState.class);
    Credential credential = mock(Credential.class);
    this.user = mock(CredentialedUser.class);
    LinkedHashMap<String, CredentialedUser> allusers = new LinkedHashMap<String, CredentialedUser>();

    when(this.user.getCredential()).thenReturn(credential);
    when(this.user.getEmail()).thenReturn(USER);
    when(this.user.getGoogleLoginState()).thenReturn(googleLoginState);
    when(googleLoginState.fetchAccessToken()).thenReturn(PASSWORD);
    when(GoogleLogin.getInstance().getAllUsers()).thenReturn(allusers);
    allusers.put(USER, this.user);
  }

  private void mockLoggedOutUser() {
    when(GoogleLogin.getInstance().isLoggedIn()).thenReturn(false);
  }

  private void mockLoggedInUser() {
    when(GoogleLogin.getInstance().isLoggedIn()).thenReturn(true);
  }

  /**
   * Creates a mock sync result representing a debuggable module selection
   * that doesn't need stash or sync
   */
  private SyncResult mockDebuggableSyncResult() {
    SyncResult syncResult = mock(SyncResult.class);
    when(syncResult.needsStash()).thenReturn(false);
    when(syncResult.needsSync()).thenReturn(false);
    when(syncResult.getTargetSyncSHA()).thenReturn(null);
    when(syncResult.hasRemoteRepository()).thenReturn(true);

    return syncResult;
  }

  @Override
  public void tearDown() throws Exception {
    dialog.close(0);
    super.tearDown();
  }
}
