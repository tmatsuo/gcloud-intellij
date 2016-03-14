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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfigurationExtension;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManagedVmRunConfigurationExtension
    extends DeployToServerRunConfigurationExtension {

  public ManagedVmRunConfigurationExtension() {
  }

  @Override
  protected void readExternal(@NotNull DeployToServerRunConfiguration runConfiguration,
      @NotNull Element element) throws InvalidDataException {

  }

  @Nullable
  @Override
  protected String getEditorTitle() {
    return "todo";
  }

  @Override
  protected boolean isApplicableFor(@NotNull DeployToServerRunConfiguration configuration) {
    return configuration.getServerType() instanceof ManagedVmCloudType;
  }

  @Override
  protected boolean isEnabledFor(@NotNull DeployToServerRunConfiguration applicableConfiguration,
      @Nullable RunnerSettings runnerSettings) {
    return false;
  }

  @Override
  protected void patchCommandLine(@NotNull DeployToServerRunConfiguration configuration,
      @Nullable RunnerSettings runnerSettings, @NotNull GeneralCommandLine cmdLine,
      @NotNull String runnerId) throws ExecutionException {

  }

  @Nullable
  @Override
  protected SettingsEditor createEditor(@NotNull DeployToServerRunConfiguration configuration) {
    return new DeployToManagedVmSettingsEditor(configuration.getServerType(), configuration.getDeploymentConfigurator(), configuration.getProject());
  }
}
