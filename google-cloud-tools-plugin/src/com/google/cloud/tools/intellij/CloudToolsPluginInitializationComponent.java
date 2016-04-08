/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineCloudType;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineCloudType.UserSpecifiedPathDeploymentSourceType;
import com.google.cloud.tools.intellij.debugger.CloudDebugConfigType;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionStub;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerConfigurationType;

import org.jetbrains.annotations.NotNull;

/**
 * Performs runtime initialization for the GCT plugin.
 */
public class CloudToolsPluginInitializationComponent implements ApplicationComponent {

  @Override
  public void disposeComponent() {
    // Do nothing.
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "GoogleCloudToolsCore.InitializationComponent";
  }

  @Override
  public void initComponent() {
    CloudToolsPluginInfoService pluginInfoService = ServiceManager
        .getService(CloudToolsPluginInfoService.class);
    CloudToolsPluginConfigurationService pluginConfigurationService = ServiceManager
        .getService(CloudToolsPluginConfigurationService.class);
    if (pluginInfoService.shouldEnable(GctFeature.DEBUGGER)) {
      pluginConfigurationService
          .registerExtension(
              ConfigurationType.CONFIGURATION_TYPE_EP, new CloudDebugConfigType());
    }
    if (pluginInfoService.shouldEnable(GctFeature.APPENGINE_FLEX)) {
      unregisterIJAppEngineDeployment();

      AppEngineCloudType appEngineCloudType = new AppEngineCloudType();
      pluginConfigurationService.registerExtension(ServerType.EP_NAME, appEngineCloudType);
      pluginConfigurationService.registerExtension(DeploymentSourceType.EP_NAME,
          new UserSpecifiedPathDeploymentSourceType());
      pluginConfigurationService.registerExtension(ConfigurationType.CONFIGURATION_TYPE_EP,
          new DeployToServerConfigurationType(appEngineCloudType));
    }
    if (pluginInfoService.shouldEnableErrorFeedbackReporting()) {
      pluginConfigurationService
          .enabledGoogleFeedbackErrorReporting(pluginInfoService.getPluginId());
    }

  }

  private void unregisterIJAppEngineDeployment() {

    ActionManager actionManager = ActionManager.getInstance();
    AnAction uploadAppEngineAppAction = actionManager.getAction("AppEngine.UploadApplication");
    if (uploadAppEngineAppAction != null) {
      DefaultActionGroup toolsMenu = (DefaultActionGroup) actionManager.getAction("ToolsMenu");
      for (AnAction anAction : toolsMenu.getChildActionsOrStubs()) {
        if (anAction instanceof ActionStub) {
          ActionStub action = (ActionStub) anAction;
          if (action.getId().equals("AppEngine.UploadApplication")) {
            toolsMenu.remove(anAction);
          }
        }
      }
      actionManager.unregisterAction("AppEngine.UploadApplication");
    }

    ServerType[] extensions = Extensions.getRootArea().getExtensionPoint(ServerType.EP_NAME)
        .getExtensions();
    for (ServerType extension : extensions) {
      if (extension.getId().equals("google-app-engine")) {
        ConfigurationType[] configurations = Extensions.getRootArea()
            .getExtensionPoint(ConfigurationType.CONFIGURATION_TYPE_EP)
            .getExtensions();
        for (ConfigurationType configuration : configurations) {
          if (configuration instanceof DeployToServerConfigurationType) {
            if (((DeployToServerConfigurationType) configuration).getServerType()
                .equals(extension)) {
              Extensions.getRootArea().getExtensionPoint(ConfigurationType.CONFIGURATION_TYPE_EP)
                  .unregisterExtension(configuration);
            }
          }
        }
        Extensions.getRootArea().getExtensionPoint(ServerType.EP_NAME)
            .unregisterExtension(extension);
      }
    }
  }
}
