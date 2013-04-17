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

package com.eugenePetrenko.idea.dependencies;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 08.04.13 0:08
 */
public class ModuleDependenciesUpdater {
  public static void updateModuleDependencies(@NotNull final Project project,
                                              @NotNull final ModulesDependencies model,
                                              @NotNull final ProgressIndicator indicator) {
    //TODO: implement undo
    final Module[] modules = ModuleManager.getInstance(project).getModules();
    if (modules.length == 0) return;

    final double total = modules.length;
    int current = 0;

    for (final Module module : modules) {
      indicator.setFraction(++current / total);
      indicator.setText(module.getName());

      runWriteAction(new Runnable() {
        public void run() {
          final LibOrModuleSet toRemove = model.forModule(module);
          if (toRemove == null || toRemove.isEmpty()) return;

          final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
          try {
            boolean containedChanges = false;
            for (OrderEntry e : model.getOrderEntries()) {
              if (toRemove.contains(e)) {
                model.removeOrderEntry(e);
                containedChanges = true;
              }
            }

            if (containedChanges) {
              model.commit();
            }
          } finally {
            //it's not allowed to dispose model after commit
            if (model.isWritable()) model.dispose();
          }
        }
      });
    }
  }

  private static void runWriteAction(@NotNull final Runnable action) {
    new WriteAction() {
      @Override
      protected void run(Result result) throws Throwable {
        action.run();
      }
    }.execute();
  }
}
