package com.eugenePetrenko.idea.dependencies;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 07.04.13 20:33
 */
public class RemoveModulesModel {
  private final Map<String, LibOrModuleSet> myModuleToRemove = new TreeMap<String, LibOrModuleSet>();

  public void addRemoves(@NotNull final Module module, @Nullable final LibOrModuleSet set) {
    if (set == null || set.isEmpty()) return;
    myModuleToRemove.put(module.getName(), set);
  }

  public void addAllRemoves(@NotNull RemoveModulesModel model) {
    myModuleToRemove.putAll(model.myModuleToRemove);
  }

  @Nullable
  public LibOrModuleSet forModule(@NotNull Module module) {
    return myModuleToRemove.get(module.getName());
  }

  public boolean isEmpty() {
    return myModuleToRemove.isEmpty();
  }
}
