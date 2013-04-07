package com.eugenePetrenko.idea.dependencies;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;

import static com.intellij.openapi.actionSystem.PlatformDataKeys.PROJECT;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 06.04.13 11:16
 */
public class OnProjectAction extends AnAction {
  @Override
  public void update(AnActionEvent e) {
    super.update(e);

    e.getPresentation().setEnabled(null != e.getData(PROJECT));
  }

  @Override
  public void actionPerformed(@Nullable final AnActionEvent anActionEvent) {
    if (anActionEvent == null) return;

    final Application app = ApplicationManager.getApplication();
    final Project project = anActionEvent.getData(PROJECT);
    if (project == null) return;

    final ModuleDependenciesAnalyzer component = app.getComponent(ModuleDependenciesAnalyzer.class);
    if (component == null) return;

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Dependencies\u2026", true, BackgroundFromStartOption.getInstance()) {
      public void run(@NotNull final ProgressIndicator indicator) {
        final Application app = ApplicationManager.getApplication();
        final Map<Module,LibOrModuleSet> result = component.processAllDependencies(indicator, app, project);

        for (Iterator<Map.Entry<Module, LibOrModuleSet>> it = result.entrySet().iterator(); it.hasNext(); ) {
          if (it.next().getValue().isEmpty()) it.remove();
        }

        if (result.isEmpty()) {
          app.invokeLater(new Runnable() {
            public void run() {
              Messages.showInfoMessage(project, "No problems detected", "Jonnyzzz");
            }
          });
        } else {
          app.invokeLater(new Runnable() {
            public void run() {
              Messages.showInfoMessage(project, "Detected not-used dependencies\n" + result, "Jonnyzzz");
            }
          });
        }
      }
    });
  }
}
