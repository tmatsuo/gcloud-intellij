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

import com.google.common.base.Preconditions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Performs the deployment of ManagedVM based applications to GCP.
 */
class DoManagedVmDeployment implements Runnable {

  private LoggingHandler loggingHandler;
  private File deploymentArtifactPath;
  private File appYamlPath;
  private File dockerFilePath;
  private AppEngineHelper appEngineHelper;
  private DeploymentOperationCallback callback;
  private DeploymentArtifactType artifactType;

  DoManagedVmDeployment(
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull File deploymentArtifactPath,
      @NotNull File appYamlPath,
      @NotNull File dockerFilePath,
      @NotNull DeploymentOperationCallback callback) {
    this.appEngineHelper = appEngineHelper;
    this.loggingHandler = loggingHandler;
    this.deploymentArtifactPath = deploymentArtifactPath;
    this.callback = callback;
    this.appYamlPath = appYamlPath;
    this.dockerFilePath = dockerFilePath;
    this.artifactType = DeploymentArtifactType.typeForPath(deploymentArtifactPath);
  }

  public void run() {
    Preconditions.checkState(deploymentArtifactPath != null);
    File stagingDirectory;
    try {
      stagingDirectory = FileUtil.createTempDirectory(
          "gae-mvm" /* prefix */,
          null /* suffix */,
          true  /* deleteOnExit */);
      consoleLogLn(
          "Created temporary staging directory: " + stagingDirectory.getAbsolutePath(),
          loggingHandler
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      File destination = new File(stagingDirectory, "target" + artifactType);
      FileUtil.copy(deploymentArtifactPath, destination);
      // We do this because on linux if the umask is set to only owner readable the docker
      // container will not be able to read the package at execution time.
      destination.setReadable(true /* readable */, false /* ownerOnly */);
      consoleLogLn("Copied target artifact %s to %s", loggingHandler,
          deploymentArtifactPath.getAbsolutePath(), destination.getAbsolutePath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      File destination = new File(stagingDirectory, "app.yaml");
      FileUtil.copy(appYamlPath, destination);
      consoleLogLn("Copied app.yaml %s to %s", loggingHandler,
          appYamlPath.getAbsolutePath(), destination.getAbsolutePath());

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      File destination = new File(stagingDirectory, "Dockerfile");
      FileUtil.copy(dockerFilePath, destination);
      consoleLogLn("Copied Dockerfile %s to %s", loggingHandler,
          dockerFilePath.getAbsolutePath(), destination.getAbsolutePath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    GeneralCommandLine commandLine = new GeneralCommandLine(
        appEngineHelper.getGcloudCommandPath().getAbsolutePath());
    commandLine.addParameters("preview", "app", "deploy", "app.yaml", "--promote", "--quiet");
    commandLine.addParameter("--project=" + appEngineHelper.getProjectId());
    commandLine.withWorkDirectory(stagingDirectory);
    consoleLogLn("Working directory set to: " + stagingDirectory.getAbsolutePath(),
        loggingHandler);
    commandLine.withParentEnvironmentType(ParentEnvironmentType.CONSOLE);
    Process process = null;
    try {
      consoleLogLn("Executing: " + commandLine.getCommandLineString(),
          loggingHandler
      );
      process = commandLine.createProcess();
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
    final ProcessHandler processHandler = new OSProcessHandler(process,
        commandLine.getCommandLineString());
    loggingHandler.attachToProcess(processHandler);
    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(ProcessEvent event) {
        if (event.getExitCode() == 0) {
          callback.succeeded(new DeploymentRuntime() {
            @Override
            public boolean isUndeploySupported() {
              return false;
            }

            @Override
            public void undeploy(@NotNull UndeploymentTaskCallback callback) {
              throw new UnsupportedOperationException();
            }
          });
        } else {
          callback.errorOccurred("Deployment failed with exit code: " + event.getExitCode());
        }
      }
    });
    processHandler.startNotify();
  }

  private void consoleLogLn(String message, LoggingHandler deploymentLoggingHandler,
      String... arguments) {
    deploymentLoggingHandler.print(String.format(message + "\n", (Object[]) arguments));
  }
}
