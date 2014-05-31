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

import com.eugenePetrenko.idea.dependencies.data.LibOrModuleSet;
import com.eugenePetrenko.idea.dependencies.data.ModulesDependencies;
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
                                              @NotNull final ModulesDependencies model) {
    //TODO: implement undo
    final Module[] modules = ModuleManager.getInstance(project).getModules();
    if (modules.length == 0) return;

    for (final Module module : modules) {
      final LibOrModuleSet toRemove = model.forModule(module);
      if (toRemove.isEmpty()) return;

      final ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
      try {
        boolean containedChanges = false;
        for (OrderEntry e : rootModel.getOrderEntries()) {
          if (toRemove.contains(e)) {
            rootModel.removeOrderEntry(e);
            containedChanges = true;
          }
        }

        if (containedChanges) {
          rootModel.commit();
        }
      } finally {
        //it's not allowed to dispose model after commit
        if (rootModel.isWritable()) rootModel.dispose();
      }
    }
  }
}
