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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
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
                                              @NotNull final RemoveModulesModel model) {
    //TODO: implement undo
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {

        boolean hasChangedModules = false;
        for (Module module : ModuleManager.getInstance(project).getModules()) {
          final LibOrModuleSet toRemove = model.forModule(module);
          if (toRemove == null || toRemove.isEmpty()) continue;

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
              hasChangedModules = true;
              model.commit();
            }
          } finally {
            //it's not allowed to dispose model after commit
            if (model.isWritable()) model.dispose();
          }
        }

        if (hasChangedModules) project.save();
      }
    });
  }
}