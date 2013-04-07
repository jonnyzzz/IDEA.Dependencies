package com.eugenePetrenko.idea.dependencies;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 06.04.13 11:16
 */
public class ModuleDependenciesAction extends AnAction {
  @Override
  public void update(@Nullable AnActionEvent e) {
    super.update(e);

    //TODO: implement me
  }

  @Override
  public void actionPerformed(@Nullable final AnActionEvent anActionEvent) {
    if (anActionEvent == null) return;

    final Project project = anActionEvent.getData(PlatformDataKeys.PROJECT);
    if (project == null) return;

    final ModuleDependenciesAnalyzer component = project.getComponent(ModuleDependenciesAnalyzer.class);
    if (component == null) return;

    component.processDependencies();
  }

}
