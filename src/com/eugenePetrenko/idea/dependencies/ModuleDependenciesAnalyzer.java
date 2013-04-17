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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 06.04.13 11:09
 */
public class ModuleDependenciesAnalyzer {
  /**
   * Performs references analysis for all modules
   *
   * @param indicator progress
   * @param app       application
   * @param project   project
   * @return set of module dependencies that could be removed
   */
  @NotNull
  public static ModulesDependencies processAllDependencies(@NotNull final ProgressIndicator indicator,
                                                           @NotNull final Application app,
                                                           @NotNull final Project project) {
    final Module[] modules = app.runReadAction(new Computable<Module[]>() {
      public Module[] compute() {
        return ModuleManager.getInstance(project).getSortedModules();
      }
    });

    return processModulesDependencies(indicator, app, modules, project);
  }

  /**
   * Performs references analysis for given module
   *
   * @param indicator progress
   * @param app       application
   * @param project   project
   * @param modules   modules
   * @return set of module dependencies that could be removed
   */
  @NotNull
  public static ModulesDependencies processModulesDependencies(@NotNull final ProgressIndicator indicator,
                                                               @NotNull Application app,
                                                               @NotNull Module[] modules,
                                                               @NotNull Project project) {
    final ModulesDependencies moduleUsages = ModuleDependenciesSearcher.collectionActualModulesDependencies(indicator, app, project, modules);
    final ModulesDependencies moduleRemovables = new ModulesDependencies();

    for (final Module module : modules) {
      final LibOrModuleSet toRemove = new LibOrModuleSet();
      final LibOrModuleSet actualUsages = moduleUsages.forModule(module);
      app.runReadAction(new Runnable() {
        public void run() {
          for (OrderEntry e : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (!actualUsages.contains(e)) {
              toRemove.addDependency(e);
            }
          }
        }
      });
      moduleRemovables.addAllRemoves(module, toRemove);
    }

    return moduleRemovables;
  }
}
