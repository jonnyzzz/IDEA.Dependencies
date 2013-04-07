package com.eugenePetrenko.idea.dependencies.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

/**
 * Created 07.04.13 17:58
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class TestAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    final LibrariesSelectionDialog dialog = new LibrariesSelectionDialog(project);
    dialog.show();
  }
}
