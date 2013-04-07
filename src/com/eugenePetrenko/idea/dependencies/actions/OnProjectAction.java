package com.eugenePetrenko.idea.dependencies.actions;

import com.eugenePetrenko.idea.dependencies.ModuleDependenciesAnalyzer;
import com.eugenePetrenko.idea.dependencies.RemoveModulesModel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Dependencies of All modules", true, BackgroundFromStartOption.getInstance()) {
      public void run(@NotNull final ProgressIndicator indicator) {
        final RemoveModulesModel result = ModuleDependenciesAnalyzer.processAllDependencies(indicator, ApplicationManager.getApplication(), project);
        PostAction.completeProcess(project, result);
      }
    });
  }
}
