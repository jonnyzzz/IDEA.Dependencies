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
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.roots.libraries.Library;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
* Created 07.04.13 15:54
*
* @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
*/
public class LibOrModuleSet {
  private final Set<String> myModules = new TreeSet<String>();
  private final Set<String> myLibs = new TreeSet<String>();

  public boolean contains(@Nullable Module mod) {
    return mod != null && myModules.contains(mod.getName());
  }

  public boolean contains(@Nullable Library lib) {
    if (lib == null) return false;
    final String name = lib.getName();
    return name != null && myLibs.contains(name);
  }

  public boolean contains(@Nullable OrderEntry e) {
    if (e == null) return false;
    return e.accept(CONTAINS, false);
  }

  public void addDependencies(@NotNull Collection<? extends OrderEntry> es) {
    for (OrderEntry e : es) {
      addDependency(e);
    }
  }

  public void addDependencies(@NotNull LibOrModuleSet deps) {
    myLibs.addAll(deps.myLibs);
    myModules.addAll(deps.myModules);
  }

  public void addDependency(@NotNull OrderEntry e) {
    if (!DependenciesFilter.REMOVABLE_DEPENDENCY.apply(e)) return;
    e.accept(ADD, null);
  }

  public void addDependency(@Nullable final Library lib) {
    if (lib == null) return;
    final String name = lib.getName();
    if (name == null) return;
    myLibs.add(name);
  }

  public boolean removeDependency(@Nullable final Library lib) {
    if (lib == null) return true;
    String name = lib.getName();
    if (name == null) return true;
    myLibs.remove(name);
    return false;
  }

  public boolean addDependency(@Nullable Module module) {
    if (module == null) return true;
    final String name = module.getName();
    myModules.add(name);
    return false;
  }

  public boolean removeDependency(@Nullable Module module) {
    if (module == null) return true;
    final String name = module.getName();
    myModules.remove(name);
    return false;
  }

  public void removeDependency(@NotNull OrderEntry e) {
    e.accept(REMOVE, null);
  }

  public boolean isEmpty() {
    return myLibs.isEmpty() && myModules.isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("LibOrModuleSet{\n");

    if (!myModules.isEmpty()) {
      sb.append("  Modules:\n");
      for (String m : myModules) {
        sb.append("    ").append(m).append("\n");
      }
    }

    if (!myLibs.isEmpty()) {
      sb.append("  Libraries:\n");
      for (String m : myLibs) {
        sb.append("    ").append(m).append("\n");
      }
    }
    sb.append("}");
    return sb.toString();
  }

  private final RootPolicy<Void> ADD = new RootPolicy<Void>(){
    @Override
    public Void visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Void value) {
      addDependency(libraryOrderEntry.getLibrary());
      return null;
    }

    @Override
    public Void visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Void value) {
      addDependency(moduleOrderEntry.getModule());
      return null;
    }
  };

  private final RootPolicy<Void> REMOVE = new RootPolicy<Void>(){
    @Override
    public Void visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Void value) {
      removeDependency(libraryOrderEntry.getLibrary());
      return null;
    }

    @Override
    public Void visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Void value) {
      removeDependency(moduleOrderEntry.getModule());
      return null;
    }
  };

  private final RootPolicy<Boolean> CONTAINS = new RootPolicy<Boolean>(){
    @Override
    public Boolean visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Boolean value) {
      return contains(libraryOrderEntry.getLibrary());
    }

    @Override
    public Boolean visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Boolean value) {
      return contains(moduleOrderEntry.getModule());
    }
  };

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final LibOrModuleSet that = (LibOrModuleSet) o;
    return myLibs.equals(that.myLibs) && myModules.equals(that.myModules);
  }

  @Override
  public int hashCode() {
    int result = myModules.hashCode();
    result = 31 * result + myLibs.hashCode();
    return result;
  }
}
