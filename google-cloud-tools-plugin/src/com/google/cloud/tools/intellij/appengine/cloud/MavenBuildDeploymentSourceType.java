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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

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
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.JComponent;

/**
 * A Maven build deployment source type providing an auto configured pre-deploy build step
 */
public class MavenBuildDeploymentSourceType extends ModuleDeploymentSourceType {

  private static final String MAVEN_TASK_INSTALL = "install";

  @Override
  public void setBuildBeforeRunTask(
      @NotNull RunConfiguration configuration,
      @NotNull ModuleDeploymentSource source) {
    Module module = source.getModule();

    if(module == null) {
      return;
    }

    if(configuration instanceof DeployToServerRunConfiguration) {
      DeployToServerRunConfiguration deployRunConfiguration =
          ((DeployToServerRunConfiguration) configuration);
      deployRunConfiguration.setDeploymentConfiguration(new AppEngineDeploymentConfiguration());
    }

    RunManagerEx runManager = RunManagerEx.getInstanceEx(configuration.getProject());
    final List<MavenBeforeRunTask> mavenBuildTasks =
        runManager.getBeforeRunTasks(configuration, MavenBeforeRunTasksProvider.ID);

    if (hasBuildTask(mavenBuildTasks)) {
      for (MavenBeforeRunTask task : mavenBuildTasks) {
        task.setEnabled(true);
      }
    } else {
      MavenProject mavenProject =
          MavenProjectsManager.getInstance(configuration.getProject()).findProject(module);

      if(mavenProject == null) {
        return;
      }

      MavenBeforeRunTask buildTask = createBuildTask(mavenProject.getFile().getPath());
      List<BeforeRunTask> tasks = runManager.getBeforeRunTasks(configuration);
      tasks.add(buildTask);
      runManager.setBeforeRunTasks(configuration, tasks, true);
    }
  }

  @Override
  public void updateBuildBeforeRunOption(@NotNull JComponent runConfigurationEditorComponent,
      @NotNull Project project, @NotNull ModuleDeploymentSource source, boolean select) {
    final DataContext dataContext =
        DataManager.getInstance().getDataContext(runConfigurationEditorComponent);
    final ConfigurationSettingsEditorWrapper editor =
        ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(dataContext);

    Module module = source.getModule();

    if(module != null && editor != null) {
      MavenProject mavenProject =
          MavenProjectsManager.getInstance(project).findProject(module);

      if(mavenProject != null) {
        MavenBeforeRunTask buildTask = createBuildTask(mavenProject.getFile().getPath());

        List<BeforeRunTask> beforeRunTasks = editor.getStepsBeforeLaunch();

        if (select && !hasBuildTask(beforeRunTasks)) {
          editor.addBeforeLaunchStep(buildTask);
        }
      }
    }
  }

  private MavenBeforeRunTask createBuildTask(String pathToPomFile) {
    MavenBeforeRunTask task = new MavenBeforeRunTask();

    task.setProjectPath(pathToPomFile);
    task.setGoal(MAVEN_TASK_INSTALL);
    task.setEnabled(true);

    return task;
  }

  private boolean hasBuildTask(Collection<? extends BeforeRunTask> beforeRunTasks) {
    return !Collections2.filter(beforeRunTasks, new Predicate<BeforeRunTask>() {
      @Override
      public boolean apply(@Nullable BeforeRunTask beforeRunTask) {
        return beforeRunTask != null
            && beforeRunTask instanceof MavenBeforeRunTask
            && MAVEN_TASK_INSTALL.equals(((MavenBeforeRunTask) beforeRunTask).getGoal());
      }
    }).isEmpty();
  }
}

