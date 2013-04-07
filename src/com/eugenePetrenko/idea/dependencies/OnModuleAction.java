package com.eugenePetrenko.idea.dependencies;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
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

/**
 * Created 07.04.13 15:57
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class OnModuleAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    super.update(e);

    e.getPresentation().setEnabled(null != e.getData(LangDataKeys.MODULE));
  }

  @Override
  public void actionPerformed(@Nullable final AnActionEvent anActionEvent) {
    if (anActionEvent == null) return;

    final Application app = ApplicationManager.getApplication();
    final Module module = anActionEvent.getData(LangDataKeys.MODULE);
    if (module == null) return;
    final Project project = module.getProject();
    final ModuleDependenciesAnalyzer component = app.getComponent(ModuleDependenciesAnalyzer.class);
    if (component == null) return;

    ProgressManager.getInstance().run(
            new Task.Backgroundable(
                    module.getProject(),
                    "Dependencies of [" + module.getName() + "]",
                    true,
                    BackgroundFromStartOption.getInstance()) {

      public void run(@NotNull final ProgressIndicator indicator) {
        final LibOrModuleSet toRemove = component.processModuleDependencies(indicator, app, myProject, module);
        app.invokeLater(new Runnable() {
          public void run() {
            Messages.showInfoMessage(project, "Analysis completed:\n" + toRemove, "Jonnyzzz");
          }
        });
      }
    });
  }
}
