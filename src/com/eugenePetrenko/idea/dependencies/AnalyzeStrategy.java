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
 * Dependency processing interface,
 * implementation may assume it is called under Read (or even Write) lock
 */
public interface AnalyzeStrategy {
  /**
   * called at the very beginning to expand modules that are searched
   * @param project project
   * @param initial user-selected modules set
   * @return expanded modules set
   */
  @NotNull
  Module[] collectAllModules(@NotNull Project project, @NotNull Module[] initial);

  /**
   * called to post-process collected actual dependencies.
   * For example to update exported module dependencies in results
   * @param project project
   * @param modules expanded set of modules
   * @param deps actual collected dependencies
   */
  void updateDetectedDependencies(@NotNull Project project, @NotNull Module[] modules, @NotNull ModulesDependencies deps);

  /**
   * called for filter out unsupported dependencies
   * @param entry dependency entry
   * @return true if dependency is accepted, false otherwise
   */
  boolean isSupportedDependency(@NotNull OrderEntry entry);
}
