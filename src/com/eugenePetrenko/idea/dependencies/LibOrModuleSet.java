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

  public boolean contains(@Nullable OrderEntry e) {
    if (e == null) return false;
    return e.accept(CONTAINS, false);
  }

  public void addDependencies(@NotNull Collection<? extends OrderEntry> es) {
    for (OrderEntry e : es) {
      addDependency(e);
    }
  }

  public void addDependency(@NotNull OrderEntry e) {
    e.accept(ADD, null);
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
      Library lib1 = libraryOrderEntry.getLibrary();
      if (lib1 == null) return null;
      String name = lib1.getName();
      if (name == null) return null;
      myLibs.add(name);

      return null;
    }

    @Override
    public Void visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Void value) {
      final Module module = moduleOrderEntry.getModule();
      if (module == null) return null;
      final String name = module.getName();
      myModules.add(name);
      return null;
    }
  };

  private final RootPolicy<Boolean> CONTAINS = new RootPolicy<Boolean>(){
    @Override
    public Boolean visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Boolean value) {
      final Library lib = libraryOrderEntry.getLibrary();
      return lib != null && myLibs.contains(lib.getName());
    }

    @Override
    public Boolean visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Boolean value) {
      final Module mod = moduleOrderEntry.getModule();
      return mod != null && myModules.contains(mod.getName());
    }
  };
}