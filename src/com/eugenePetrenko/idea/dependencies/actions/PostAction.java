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

import com.eugenePetrenko.idea.dependencies.RemoveModulesModel;
import com.eugenePetrenko.idea.dependencies.ui.LibrariesSelectionDialog;
import com.eugenePetrenko.idea.dependencies.ModuleDependenciesUpdater;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 08.04.13 0:22
 */
public class PostAction {
  public static void completeProcess(@NotNull final Project project, @NotNull final RemoveModulesModel model) {
    final Application app = ApplicationManager.getApplication();

    if (model.isEmpty()) {
      app.invokeLater(new Runnable() {
        public void run() {
          Messages.showInfoMessage(project, "No unused dependencies were detected", "Jonnyzzz");
        }
      });
      return;
    }

    app.invokeLater(new Runnable() {
      public void run() {
        LibrariesSelectionDialog dialog = new LibrariesSelectionDialog(project, model);
        dialog.show();
        if (!dialog.isOK()) return;

        final RemoveModulesModel newModel = dialog.getModel();
        if (newModel.isEmpty()) return;
        ModuleDependenciesUpdater.updateModuleDependencies(project, newModel);

        Messages.showInfoMessage(project, "Unused dependencies were removed", "Jonnyzzz");
      }
    });
  }
}
