package com.eugenePetrenko.idea.dependencies;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

/**
 * Created 07.04.13 15:57
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class OnModuleAction extends AnAction {
  @Override
  public void actionPerformed(@Nullable final AnActionEvent anActionEvent) {
    if (anActionEvent == null) return;

    final Module module = anActionEvent.getData(LangDataKeys.MODULE);
    if (module == null) return;

    final Project project = module.getProject();
    final ModuleDependenciesAnalyzer component = project.getComponent(ModuleDependenciesAnalyzer.class);
    if (component == null) return;

    component.processModuleDependencies(module);
  }
}
