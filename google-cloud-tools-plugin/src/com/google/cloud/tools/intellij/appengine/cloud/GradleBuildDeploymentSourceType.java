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
import com.google.common.collect.ImmutableList;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.execution.GradleBeforeRunTaskProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A Gradle build deployment source type providing an auto configured pre-deploy build step
 */
public class GradleBuildDeploymentSourceType extends BuildDeploymentSourceType {

  private static final String GRADLE_TASK_BUILD = "build";

  @NotNull
  @Override
  protected List<ExternalSystemBeforeRunTask> getBuildTasks(
      RunManagerEx runManager,
      RunConfiguration configuration) {
    return runManager.getBeforeRunTasks(configuration, GradleBeforeRunTaskProvider.ID);
  }

  @Nullable
  @Override
  protected BeforeRunTask createBuildTask(Project project, Module module) {
    ExternalSystemBeforeRunTask task =
        new ExternalSystemBeforeRunTask(
            GradleBeforeRunTaskProvider.ID, GradleConstants.SYSTEM_ID);

    ExternalSystemTaskExecutionSettings taskSettings = task.getTaskExecutionSettings();
    taskSettings.setExternalProjectPath(getPathToGradleBuildFile(module));
    taskSettings.setTaskNames(ImmutableList.of(GRADLE_TASK_BUILD));
    task.setEnabled(true);

    return task;
  }

  @Override
  protected boolean hasBuildTask(Collection<? extends BeforeRunTask> beforeRunTasks) {
    return !Collections2.filter(beforeRunTasks, new Predicate<BeforeRunTask>() {
      @Override
      public boolean apply(@Nullable BeforeRunTask beforeRunTask) {
        return beforeRunTask != null
            && beforeRunTask instanceof ExternalSystemBeforeRunTask
            && ((ExternalSystemBeforeRunTask) beforeRunTask).getTaskExecutionSettings()
            .getTaskNames().contains(GRADLE_TASK_BUILD);
      }
    }).isEmpty();
  }

  private String getPathToGradleBuildFile(Module module) {
    return new File(module.getModuleFilePath()).getParent();
  }
}
