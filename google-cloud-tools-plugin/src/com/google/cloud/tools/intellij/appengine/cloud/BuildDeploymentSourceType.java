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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;
import com.intellij.remoteServer.impl.configuration.deployment.ModuleDeploymentSourceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;

/**
 * Base class for build system (e.g. maven or gradle) deployment source types.
 */
public abstract class BuildDeploymentSourceType extends ModuleDeploymentSourceType {

  @Override
  public void setBuildBeforeRunTask(
      @NotNull RunConfiguration configuration,
      @NotNull ModuleDeploymentSource source) {

    Module module = source.getModule();

    if(module == null) {
      return;
    }

    setConfiguration(configuration);

    RunManagerEx runManager = RunManagerEx.getInstanceEx(configuration.getProject());
    final List<? extends BeforeRunTask> buildTasks = getBuildTasks(runManager, configuration);

    if (hasBuildTask(buildTasks)) {
      for (BeforeRunTask task : buildTasks) {
        task.setEnabled(true);
      }
    } else {
      BeforeRunTask buildTask = createBuildTask(configuration.getProject(), module);
      List<BeforeRunTask> tasks = runManager.getBeforeRunTasks(configuration);
      tasks.add(buildTask);
      runManager.setBeforeRunTasks(configuration, tasks, true);
    }
  }

  @Override
  public void updateBuildBeforeRunOption(
      @NotNull JComponent runConfigurationEditorComponent,
      @NotNull Project project,
      @NotNull ModuleDeploymentSource source,
      boolean select) {

    final DataContext dataContext =
        DataManager.getInstance().getDataContext(runConfigurationEditorComponent);
    final ConfigurationSettingsEditorWrapper editor =
        ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(dataContext);

    Module module = source.getModule();

    if(module != null && editor != null) {
      BeforeRunTask buildTask = createBuildTask(project, module);

      if(buildTask != null) {
        List<BeforeRunTask> beforeRunTasks = editor.getStepsBeforeLaunch();
        if (select && !hasBuildTask(beforeRunTasks)) {
          editor.addBeforeLaunchStep(buildTask);
        }
      }
    }
  }

  @NotNull
  protected abstract List<? extends BeforeRunTask> getBuildTasks(
      RunManagerEx runManager,
      RunConfiguration configuration);

  @Nullable
  protected abstract BeforeRunTask createBuildTask(Project project, Module module);

  protected abstract boolean hasBuildTask(Collection<? extends BeforeRunTask> beforeRunTasks);

  /**
   * Manually set the deployment configuration so that its available immediately in the
   * deployment configuration dialog even if the user does not trigger any UI actions. This
   * prevents downstream npe's in {@link DeployToServerRunConfiguration#checkConfiguration()}
   *
   */
  @SuppressWarnings("unchecked")
  private void setConfiguration(@NotNull RunConfiguration configuration) {
    if(configuration instanceof DeployToServerRunConfiguration) {
      DeployToServerRunConfiguration deployRunConfiguration =
          ((DeployToServerRunConfiguration) configuration);
      deployRunConfiguration.setDeploymentConfiguration(new AppEngineDeploymentConfiguration());
    }
  }

}
