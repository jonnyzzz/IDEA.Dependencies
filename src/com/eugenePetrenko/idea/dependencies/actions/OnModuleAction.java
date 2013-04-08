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

package com.eugenePetrenko.idea.dependencies.actions;

import com.eugenePetrenko.idea.dependencies.ModuleDependenciesAnalyzer;
import com.eugenePetrenko.idea.dependencies.RemoveModulesModel;
import com.eugenePetrenko.idea.dependencies.ui.Comparators;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Created 07.04.13 15:57
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class OnModuleAction extends AnAction {

  public static final Function<Module,String> TO_STRING = new Function<Module, String>() {
    public String fun(Module module) {
      return module.getName();
    }
  };

  @Override
  public void update(AnActionEvent e) {
    super.update(e);

    if (e == null) return;
    final Module[] modules = e.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    e.getPresentation().setEnabled(null != modules && modules.length > 0 && e.getProject() != null);
  }

  @Override
  public void actionPerformed(@Nullable final AnActionEvent e) {
    if (e == null) return;

    final Module[] modules = e.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    if (modules == null) return;
    final Project project = e.getProject();
    if (project == null) return;

    Arrays.sort(modules, Comparators.MODULE_COMPARATOR);

    ProgressManager.getInstance().run(
            new Task.Backgroundable(
                    e.getProject(),
                    "Dependencies of [" + StringUtil.join(modules, TO_STRING, ", ") + "]",
                    true,
                    BackgroundFromStartOption.getInstance()) {

      public void run(@NotNull final ProgressIndicator indicator) {
        final RemoveModulesModel toRemove = ModuleDependenciesAnalyzer.processModulesDependencies(indicator, ApplicationManager.getApplication(), modules, myProject);
        PostAction.completeProcess(project, toRemove);
      }
    });
  }
}
