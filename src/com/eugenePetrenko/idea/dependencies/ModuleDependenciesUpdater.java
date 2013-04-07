package com.eugenePetrenko.idea.dependencies;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 08.04.13 0:08
 */
public class ModuleDependenciesUpdater {
  public static void updateModuleDependencies(@NotNull final Project project,
                                              @NotNull final RemoveModulesModel model) {
    //TODO: implement undo
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {

        boolean hasChangedModules = false;
        for (Module module : ModuleManager.getInstance(project).getModules()) {
          final LibOrModuleSet toRemove = model.forModule(module);
          if (toRemove == null || toRemove.isEmpty()) continue;

          final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
          try {
            boolean containedChanges = false;
            for (OrderEntry e : model.getOrderEntries()) {
              if (toRemove.contains(e)) {
                model.removeOrderEntry(e);
                containedChanges = true;
              }
            }

            if (containedChanges) {
              hasChangedModules = true;
              model.commit();
            }
          } finally {
            //it's not allowed to dispose model after commit
            if (model.isWritable()) model.dispose();
          }
        }

        project.save();
      }
    });
  }


}
