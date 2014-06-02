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
import com.eugenePetrenko.idea.dependencies.data.ModulesDependencies;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.eugenePetrenko.idea.dependencies.AnalyzeStrategies.WITH_EXPORT_DEPENDENCIES;
import static com.intellij.openapi.actionSystem.PlatformDataKeys.PROJECT;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 06.04.13 11:16
 */
public class OnProjectAction extends AnAction {
  @Override
  public void update(AnActionEvent e) {
    super.update(e);

    if (e == null) return;
    e.getPresentation().setEnabled(null != e.getData(PROJECT));
  }

  @Override
  public void actionPerformed(@Nullable final AnActionEvent anActionEvent) {
    if (anActionEvent == null) return;
    final Project project = anActionEvent.getData(PROJECT);
    if (project == null) return;

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Dependencies of All Modules", true, BackgroundFromStartOption.getInstance()) {
      public void run(@NotNull final ProgressIndicator indicator) {
        final ModulesDependencies result = ModuleDependenciesAnalyzer.processAllDependencies(WITH_EXPORT_DEPENDENCIES, indicator, project);
        PostAction.completeProcess(project, result);
      }
    });
  }
}
