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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import org.jetbrains.annotations.NotNull;

/**
* Created by Eugene Petrenko (eugene.petrenko@gmail.com)
* Date: 17.04.13 12:19
*/
public enum AnalyzeStrategies implements AnalyzeStrategy {
  WITH_EXPORT_DEPENDENCIES {
    @NotNull
    public Module[] collectAllModules(@NotNull Project project, @NotNull Module[] initial) {
      return ModuleDependenciesHelper.includeExportDependencies(project, initial);
    }

    public void updateDetectedDependencies(@NotNull Project project, @NotNull Module[] modules, @NotNull ModulesDependencies deps) {
      ModuleDependenciesHelper.updateExportedDependenciesUsages(project, modules, deps);
    }

    public boolean isSupportedDependency(@NotNull OrderEntry entry) {
      //all supported deps are included
      return true;
    }
  },

  SKIP_EXPORT_DEPENDENCIES {
    @NotNull
    public Module[] collectAllModules(@NotNull Project project, @NotNull Module[] initial) {
      return initial;
    }

    public void updateDetectedDependencies(@NotNull Project project, @NotNull Module[] modules, @NotNull ModulesDependencies deps) {
      //NOP
    }

    public boolean isSupportedDependency(@NotNull OrderEntry entry) {
      return !ModuleDependenciesHelper.isExportDependency(entry);
    }
  }
}
