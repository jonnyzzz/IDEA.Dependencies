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

import com.intellij.openapi.roots.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Created 07.04.13 18:26
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class DependenciesFilter {
  public static final Predicate<OrderEntry> REMOVABLE_DEPENDENCY = input -> input != null && input.accept(new RootPolicy<>() {
    @Override
    public Boolean visitModuleOrderEntry(@NotNull ModuleOrderEntry moduleOrderEntry, Boolean value) {
      return true;
    }

    @Override
    public Boolean visitLibraryOrderEntry(@NotNull LibraryOrderEntry libraryOrderEntry, Boolean value) {
      final DependencyScope scope = libraryOrderEntry.getScope();
      return scope == DependencyScope.COMPILE || scope == DependencyScope.TEST;
    }
  }, false);
}
