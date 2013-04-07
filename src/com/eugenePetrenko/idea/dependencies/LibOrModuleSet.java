package com.eugenePetrenko.idea.dependencies;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  public void addDependency(@Nullable Module module) {
    if (module == null) return;
    String name = module.getName();
    myModules.add(name);
  }

  public void addDependency(@NotNull ModuleOrderEntry module) {
    addDependency(module.getModule());
  }

  public void addDependency(@NotNull LibraryOrderEntry lib) {
    addDependency(lib.getLibrary());
  }

  public void addDependency(@Nullable Library lib) {
    if (lib == null) return;
    String name = lib.getName();
    if (name == null) return;
    myLibs.add(name);
  }

  public boolean contains(@Nullable Module m) {
    return m != null && myModules.contains(m.getName());
  }

  public boolean contains(@Nullable ModuleOrderEntry m) {
    return m != null && contains(m.getModule());
  }

  public boolean contains(@Nullable Library l) {
    return l != null && myLibs.contains(l.getName());
  }

  public boolean contains(@Nullable LibraryOrderEntry l) {
    return l != null && contains(l.getLibrary());
  }

  public boolean contains(@Nullable OrderEntry e) {
    if (e instanceof ModuleOrderEntry) {
      return contains((ModuleOrderEntry)e);
    }

    if (e instanceof LibraryOrderEntry) {
      return contains((LibraryOrderEntry)e);
    }
    return false;
  }

  public void addDependency(@NotNull OrderEntry e) {
    if (e instanceof ModuleOrderEntry) {
      addDependency((ModuleOrderEntry)e);
    }

    if (e instanceof LibraryOrderEntry) {
      addDependency((LibraryOrderEntry)e);
    }
  }

  @Override
  public String toString() {
    return "LibOrModuleSet{" +
            "Modules=" + StringUtil.join(myModules, "\n") +
            ", Libs=" + StringUtil.join(myLibs, "\n") +
            '}';
  }
}
