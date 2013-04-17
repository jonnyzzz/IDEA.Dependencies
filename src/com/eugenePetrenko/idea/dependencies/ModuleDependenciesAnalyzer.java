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

import com.intellij.ide.util.DelegatingProgressIndicator;
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
    final ModulesDependencies result = new ModulesDependencies();
    final double length = (double) modules.length;
    final double outerStep = 1.0 / length;

    for (int i = 0; i < modules.length; i++) {
      final Module module = modules[i];

      indicator.setIndeterminate(false);
      indicator.checkCanceled();
      indicator.setText(module.getName());

      final double outerFraction = (double) i / length;
      indicator.setFraction(outerFraction);

      DelegatingProgressIndicator subProgress = new DelegatingProgressIndicator(indicator) {
        @Override
        public double getFraction() {
          return (super.getFraction() - outerFraction) / outerStep;
        }

        @Override
        public void setFraction(double fraction) {
          super.setFraction(outerFraction + fraction * outerStep);
        }
      };
      result.addAllRemoves(processModuleDependencies(subProgress, app, project, module));
    }

    return result;
  }


  /**
   * Performs references analysis for given module
   *
   * @param indicator progress
   * @param app       application
   * @param project   project
   * @param module    module
   * @return set of module dependencies that could be removed
   */
  @NotNull
  public static ModulesDependencies processModuleDependencies(@NotNull final ProgressIndicator indicator,
                                                              @NotNull final Application app,
                                                              @NotNull final Project project,
                                                              @NotNull final Module module) {
    final LibOrModuleSet actualUsages = ModuleDependenciesSearcher.processModuleDependencies(indicator, app, project, module);

    final LibOrModuleSet toRemove = new LibOrModuleSet();
    app.runReadAction(new Runnable() {
      public void run() {
        for (OrderEntry e : ModuleRootManager.getInstance(module).getOrderEntries()) {
          if (DependenciesFilter.REMOVABLE_DEPENDENCY.apply(e) && !actualUsages.contains(e)) {
            toRemove.addDependency(e);
          }
        }
      }
    });

    final ModulesDependencies removeModulesModel = new ModulesDependencies();
    removeModulesModel.addRemoves(module, toRemove);
    return removeModulesModel;
  }
}
