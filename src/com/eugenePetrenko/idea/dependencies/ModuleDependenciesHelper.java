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
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 17.04.13 11:29
 */
public class ModuleDependenciesHelper {
  @NotNull
  public static Module[] includeExportDependencies(@NotNull final Project project,
                                                   @NotNull final Module[] modules) {
    return ApplicationManager.getApplication().runReadAction(new Computable<Module[]>() {
      public Module[] compute() {
        Set<Module> set = new HashSet<Module>();
        includeExportDependencies(project, Arrays.asList(modules), set);
        return set.toArray(new Module[set.size()]);
      }
    });
  }

  private static void includeExportDependencies(@NotNull final Project project,
                                                @NotNull final List<Module> modules,
                                                @NotNull final Set<Module> result) {
    final ModuleManager moduleManager = ModuleManager.getInstance(project);
    for (final Module module : modules) {
      //do not re-check visited modules
      if (!result.add(module)) continue;
      if (collectModuleExports(module).isEmpty()) continue;

      includeExportDependencies(
              project,
              moduleManager.getModuleDependentModules(module),
              result);
    }
  }

  public static void updateExportedDependenciesUsages(@NotNull final Project project,
                                                      @NotNull final Module[] modules,
                                                      @NotNull final ModulesDependencies deps) {
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      public void run() {
        final ModuleManager moduleManager = ModuleManager.getInstance(project);

        for (Module module : modules) {
          final LibOrModuleSet exports = collectModuleExports(module);
          if (exports.isEmpty()) continue;

          //take modules that depends on the module that has export dependency
          final LibOrModuleSet moduleDeps = deps.forModule(module);
          for (Module dependee : moduleManager.getModuleDependentModules(module)) {
            moduleDeps.addDependencies(deps.forModule(dependee).intersect(exports));
          }
        }
      }
    });
  }

  @NotNull
  private static LibOrModuleSet collectModuleExports(@NotNull final Module module) {
    final LibOrModuleSet exports = new LibOrModuleSet();
    ModuleRootManager.getInstance(module).processOrder(new RootPolicy<Void>(){
      @Override
      public Void visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Void value) {
        if (!moduleOrderEntry.isExported()) return null;
        exports.addDependency(moduleOrderEntry);
        return null;
      }

      @Override
      public Void visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Void value) {
        if (!libraryOrderEntry.isExported()) return null;
        exports.addDependency(libraryOrderEntry);
        return null;
      }
    }, null);
    return exports;
  }
}
