package com.eugenePetrenko.idea.dependencies;

import com.intellij.openapi.roots.*;
import com.intellij.util.containers.Predicate;
import org.jetbrains.annotations.Nullable;

/**
 * Created 07.04.13 18:26
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class DependenciesFilter {
  public static final Predicate<OrderEntry> REMOVABLE_DEPENDENCY = new Predicate<OrderEntry>() {
    public boolean apply(@Nullable OrderEntry input) {
      return input != null && input.accept(new RootPolicy<Boolean>(){
        @Override
        public Boolean visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Boolean value) {
          return true;
        }

        @Override
        public Boolean visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Boolean value) {
          final DependencyScope scope = libraryOrderEntry.getScope();
          return scope == DependencyScope.COMPILE || scope == DependencyScope.TEST;
        }
      }, false);
    }
  };

}
