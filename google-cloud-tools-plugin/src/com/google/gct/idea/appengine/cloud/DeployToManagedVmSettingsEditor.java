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

package com.google.gct.idea.appengine.cloud;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerSettingsEditor;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.util.ui.FormBuilder;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public class DeployToManagedVmSettingsEditor
    extends DeployToServerSettingsEditor<ManagedVmServerConfiguration, ManagedVmDeploymentConfiguration> {

  public DeployToManagedVmSettingsEditor(ServerType<ManagedVmServerConfiguration> type,
      DeploymentConfigurator<ManagedVmDeploymentConfiguration, ManagedVmServerConfiguration> deploymentConfigurator,
      Project project) {
    super(type, deploymentConfigurator, project);
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return FormBuilder.createFormBuilder()
        .addLabeledComponent("Server:", new ComboboxWithBrowseButton())
        .addLabeledComponent("Deployment:", new ComboboxWithBrowseButton(new ComboBox()))
        .getPanel();
  }
}
