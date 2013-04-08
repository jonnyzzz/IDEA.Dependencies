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
