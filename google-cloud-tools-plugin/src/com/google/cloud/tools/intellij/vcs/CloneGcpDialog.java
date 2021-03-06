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

package com.google.cloud.tools.intellij.vcs;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.dvcs.ui.DvcsBundle;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The dialog that prompts the user to download (git clone) from a GCP project.
 */
public class CloneGcpDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(CloneGcpDialog.class);
  public static final String INVALID_FILENAME_CHARS = "[/\\\\?%*:|\"<>]";

  // Form controls
  private JPanel rootPanel;
  private ProjectSelector repositoryUrl;
  private TextFieldWithBrowseButton parentDirectory;
  private JTextField directoryName;
  private JLabel parentDirectoryLabel;

  @NotNull private String defaultDirectoryName = "";
  @NotNull private final Project project;

  /**
   * Initialize the dialog.
   */
  public CloneGcpDialog(@NotNull Project project) {
    super(project, true);
    this.project = project;
    parentDirectoryLabel.setText(DvcsBundle.message("clone.parent.dir"));
    init();
    initComponents();
    setTitle(GctBundle.message("clonefromgcp.title"));
    setOKButtonText(GctBundle.message("clonefromgcp.button"));
  }

  @Nullable
  public String getSourceRepositoryUrl() {
    return getCurrentUrlText();
  }

  @Nullable
  public String getParentDirectory() {
    return parentDirectory.getText();
  }

  @Nullable
  public String getDirectoryName() {
    return directoryName.getText();
  }

  @Nullable
  public String getGcpUserName() {
    CredentialedUser selectedUser = repositoryUrl.getSelectedUser();
    return selectedUser != null ? selectedUser.getEmail() : null;
  }

  private void initComponents() {
    FileChooserDescriptor fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    fcd.setShowFileSystemRoots(true);
    fcd.setTitle(GctBundle.message("clonefromgcp.destination.directory.title"));
    fcd.setDescription(GctBundle.message("clonefromgcp.destination.directory.description"));
    fcd.setHideIgnored(false);
    parentDirectory.addActionListener(new ComponentWithBrowseButton
          .BrowseFolderActionListener<JTextField>(
          fcd.getTitle(), fcd.getDescription(), parentDirectory,
          project, fcd, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
        @Override
        protected VirtualFile getInitialFile() {
          String text = getComponentText();
          if (text.length() == 0) {
            VirtualFile file = project.getBaseDir();
            if (file != null) {
              return file;
            }
          }
          return super.getInitialFile();
        }
      }
    );

    final DocumentListener updateOkButtonListener = new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        updateButtons();
      }
    };
    parentDirectory.getChildComponent().getDocument().addDocumentListener(updateOkButtonListener);
    parentDirectory.setText(ProjectUtil.getBaseDir());
    directoryName.getDocument().addDocumentListener(updateOkButtonListener);

    setOKActionEnabled(false);
  }

  @Override
  protected String getDimensionServiceKey() {
    return "GCPCloneDialog";
  }

  @Override
  protected String getHelpId() {
    return "reference.VersionControl.Git.CloneRepository";
  }

  /**
   * Check fields and display error in the wrapper if there is a problem.
   */
  private void updateButtons() {
    if (parentDirectory.getText().length() == 0 || directoryName.getText().length() == 0) {
      setErrorText(null);
      setOKActionEnabled(false);
      return;
    }
    File file = new File(parentDirectory.getText(), directoryName.getText());
    if (file.exists()) {
      setErrorText(GctBundle.message("clonefromgcp.destination.exists.error"));
      setOKActionEnabled(false);
      paintSelectionError();
      return;
    } else if (!file.getParentFile().exists()) {
      setErrorText(GctBundle.message("clonefromgcp.parent.missing.error"));
      setOKActionEnabled(false);
      paintSelectionError();
      return;
    }
    paintSelectionOk();
    setErrorText(null);
    setOKActionEnabled(true);
  }

  @Nullable
  private String getCurrentUrlText() {
    CredentialedUser selectedUser = repositoryUrl.getSelectedUser();

    if (selectedUser == null || Strings.isNullOrEmpty(repositoryUrl.getText())) {
      return null;
    }

    return GcpHttpAuthDataProvider.getGcpUrl(repositoryUrl.getText());
  }

  @SuppressWarnings("checkstyle:abbreviationaswordinname")
  private void createUIComponents() {
    repositoryUrl = new ProjectSelector();
    repositoryUrl.setMinimumSize(new Dimension(300, 0));
    repositoryUrl.getDocument().addDocumentListener(new DocumentAdapter() {
      @SuppressWarnings("ConstantConditions") // This suppresses an invalid nullref warning for
      // projectDescription.replaceAll.
      @Override
      protected void textChanged(DocumentEvent event) {
        if (defaultDirectoryName.equals(directoryName.getText())
            || directoryName.getText().length() == 0) {
          // modify field if it was unmodified or blank
          String projectDescription = repositoryUrl.getProjectDescription();
          if (!Strings.isNullOrEmpty(projectDescription)) {
            defaultDirectoryName = projectDescription.replaceAll(INVALID_FILENAME_CHARS, "");
            defaultDirectoryName = defaultDirectoryName.replaceAll("\\s", "");
          } else {
            defaultDirectoryName = "";
          }

          directoryName.setText(defaultDirectoryName);
        }
        updateButtons();
      }
    });
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return repositoryUrl;
  }

  @Override
  protected JComponent createCenterPanel() {
    return rootPanel;
  }

  /**
   * Default dialog state.
   */
  private void paintSelectionOk() {
    parentDirectory.setBackground(Color.getColor("ECECEC"));
    parentDirectoryLabel.setForeground(Color.BLACK);
  }

  /**
   * Activates when a user selection is incorrect.
   * <p></p>
   * Paints the "Parent Directory" label and textbox background red.
   */
  private void paintSelectionError() {
    parentDirectory.setBackground(Color.RED);
    parentDirectoryLabel.setForeground(Color.RED);
  }
}
