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
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 07.04.13 20:33
 */
public class ModulesDependencies {
  private final Map<String, LibOrModuleSet> myModuleToRemove = new TreeMap<String, LibOrModuleSet>();

  public void addRemoves(@NotNull final Module module, @Nullable final LibOrModuleSet set) {
    myModuleToRemove.put(module.getName(), set);
  }

  public void addAllRemoves(@NotNull ModulesDependencies model) {
    myModuleToRemove.putAll(model.myModuleToRemove);
  }

  @Nullable
  public LibOrModuleSet forModule(@NotNull Module module) {
    return myModuleToRemove.get(module.getName());
  }

  public boolean isEmpty() {
    if (myModuleToRemove.isEmpty()) return true;
    for (LibOrModuleSet val : myModuleToRemove.values()) {
      if (!val.isEmpty()) return false;
    }
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("RemoveModulesModel{\n");
    for (Map.Entry<String, LibOrModuleSet> e : myModuleToRemove.entrySet()) {
      if (e.getValue().isEmpty()) continue;

      sb.append("  ").append(e.getKey()).append(" =>\n");
      for (String line : e.getValue().toString().split("[\n\r]+")) {
        if (StringUtil.isEmptyOrSpaces(line)) continue;
        sb.append("    ").append(line).append("\n");
      }
    }
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ModulesDependencies that = (ModulesDependencies) o;

    final Set<String> allKeys = new HashSet<String>();
    allKeys.addAll(this.myModuleToRemove.keySet());
    allKeys.addAll(that.myModuleToRemove.keySet());

    for (String key : allKeys) {
      final LibOrModuleSet thisSet = this.myModuleToRemove.get(key);
      final LibOrModuleSet thatSet = that.myModuleToRemove.get(key);

      if ((thisSet == null || thisSet.isEmpty()) && (thatSet == null || thatSet.isEmpty())) continue;
      if (thisSet == null || thatSet == null) return false;
      if (!thisSet.equals(thatSet)) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return myModuleToRemove.hashCode();
  }
}
