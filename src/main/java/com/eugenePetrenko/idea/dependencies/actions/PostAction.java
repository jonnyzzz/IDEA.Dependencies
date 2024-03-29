/*
 * Copyright 2013-2013 Eugene Petrenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eugenePetrenko.idea.dependencies.actions;

import com.eugenePetrenko.idea.dependencies.ModuleDependenciesUpdater;
import com.eugenePetrenko.idea.dependencies.data.ModulesDependencies;
import com.eugenePetrenko.idea.dependencies.ui.LibrariesSelectionDialog;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 08.04.13 0:22
 */
public class PostAction {
  public static void completeProcess(@NotNull final Project project, @NotNull final ModulesDependencies model) {
    final Application app = ApplicationManager.getApplication();

    if (model.isEmpty()) {
      app.invokeLater(() -> Notifications.Bus.notify(new Notification(
              "Unused Dependencies",
              "Remove unused dependencies",
              "No unused dependencies were detected",
              NotificationType.INFORMATION
      ), project));
      return;
    }

    app.invokeLater(() -> {
      LibrariesSelectionDialog dialog = new LibrariesSelectionDialog(project, model);
      dialog.show();
      if (!dialog.isOK()) return;

      final ModulesDependencies newModel = dialog.getModel();
      if (newModel.isEmpty()) return;


      WriteAction.run(() -> {
        ModuleDependenciesUpdater.updateModuleDependencies(project, newModel);
      });

      saveProjectAsync(app, project);
    });
  }

  private static void saveProjectAsync(@NotNull final Application app, @NotNull final Project project) {
    app.invokeLater(() -> app.runWriteAction(project::save));
  }
}
