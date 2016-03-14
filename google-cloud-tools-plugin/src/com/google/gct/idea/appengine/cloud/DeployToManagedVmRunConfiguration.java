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

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfigurationExtensionsManager;

import org.jetbrains.annotations.NotNull;


public class DeployToManagedVmRunConfiguration
    extends DeployToServerRunConfiguration<ManagedVmServerConfiguration, ManagedVmDeploymentConfiguration> {

  private final ServerType myServerType;
  private final DeploymentConfigurator myDeploymentConfigurator;

  public DeployToManagedVmRunConfiguration(Project project,
      ConfigurationFactory factory, String name,
      ServerType<ManagedVmServerConfiguration> serverType,
      DeploymentConfigurator<ManagedVmDeploymentConfiguration, ManagedVmServerConfiguration> deploymentConfigurator) {
    super(project, factory, name, serverType, deploymentConfigurator);
    myServerType = serverType;
    myDeploymentConfigurator = deploymentConfigurator;
  }

  @NotNull
  @Override
  public SettingsEditor<DeployToServerRunConfiguration> getConfigurationEditor() {
    SettingsEditor commonEditor
        = new DeployToManagedVmSettingsEditor(myServerType, myDeploymentConfigurator, getProject());

    SettingsEditorGroup<DeployToManagedVmRunConfiguration> group = new SettingsEditorGroup<DeployToManagedVmRunConfiguration>();
    group.addEditor("Deployment", commonEditor);
    DeployToServerRunConfigurationExtensionsManager.getInstance().appendEditors(this, group);
    return commonEditor;
  }
}
