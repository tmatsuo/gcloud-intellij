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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.impl.configuration.deployment.ModuleDeploymentSourceImpl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataService;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.Icon;

import icons.GradleIcons;

/**
 * A deployment source backed by the Gradle build system
 */
public class GradleBuildDeploymentSource extends ModuleDeploymentSourceImpl {

  private final Project project;

  public GradleBuildDeploymentSource(@NotNull ModulePointer pointer, @NotNull Project project) {
    super(pointer);
    this.project = project;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return String.format("Gradle build: %s", getModulePointer().getModuleName());
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return GradleIcons.Gradle;
  }

  @NotNull
  @Override
  public DeploymentSourceType<?> getType() {
    return DeploymentSourceType.EP_NAME.findExtension(GradleBuildDeploymentSourceType.class);
  }

  @Nullable
  @Override
  public File getFile() {
    ExternalProjectDataService dataService =
        (ExternalProjectDataService) ServiceManager.getService(
            ProjectDataManager.class).getDataService(ExternalProjectDataService.KEY);

    if (dataService == null || project.getBasePath() == null || getModule() == null) {
      return null;
    }

    ExternalProject rootExternalProject =
        dataService.getRootExternalProject(GradleConstants.SYSTEM_ID,
            new File(project.getBasePath()));

    if (rootExternalProject == null) {
      return null;
    }

    ExternalProject moduleExternalProject =
        dataService.findExternalProject(rootExternalProject, getModule());

    if (moduleExternalProject == null) {
      return null;
    }

    return findGradleBuildArtifact(moduleExternalProject.getBuildDir());
  }

  /**
   * Uses a best-effort approach to locate the Gradle build artifact.
   * Assumes the build artifact is located in the /libs folder under the root build directory.
   * Also assumes only a single build artifact. Will only return one artifact if multiple are found.
   */
  @Nullable
  private File findGradleBuildArtifact(@NotNull File buildDir) {
    File libDir = new File(buildDir, "libs");
    File[] buildFiles = libDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".jar") || name.endsWith(".war");
      }
    });

    return buildFiles.length > 0 ? buildFiles[0] : null;
  }

}
