package com.eugenePetrenko.idea.dependencies;

import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 06.04.13 11:09
 */
public class ModuleDependenciesAnalyzer implements ProjectComponent {
  private static final Logger LOG = Logger.getInstance(ModuleDependenciesAnalyzer.class.getName());
  private final Project myProject;

  public ModuleDependenciesAnalyzer(@NotNull final Project project) {
    myProject = project;
  }

  public void projectOpened() {
    LOG.info("projectOpened");
  }

  public void projectClosed() {
    LOG.info("projectClosed");
  }

  public void initComponent() {
  }

  public void disposeComponent() {
  }

  @NotNull
  public String getComponentName() {
    return "jonnyzzz" + getClass().getSimpleName();
  }

  public void processProjectDependencies() {
    ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Analysis\u2026", true, BackgroundFromStartOption.getInstance()) {
      public void run(@NotNull final ProgressIndicator indicator) {
        final Application app = ApplicationManager.getApplication();

        final Module[] modules = app.runReadAction(new Computable<Module[]>() {
          public Module[] compute() {
            return ModuleManager.getInstance(myProject).getSortedModules();
          }
        });
        for (int i = 0; i < modules.length; i++) {
          final Module module = modules[i];

          indicator.setIndeterminate(false);
          indicator.checkCanceled();
          indicator.setText(module.getName());
          indicator.setFraction((double) i / (double) modules.length);

          processModuleDependencies(indicator, app, myProject, module);
        }
      }
    });
  }

  public void processModuleDependencies(@NotNull final Module module) {
    ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Analysis of module " + module.getName() + "\u2026", true, BackgroundFromStartOption.getInstance()) {
      public void run(@NotNull final ProgressIndicator indicator) {
        final Application app = ApplicationManager.getApplication();
        processModuleDependencies(indicator, app, myProject, module);
      }
    });
  }

  private void processModuleDependencies(@NotNull final ProgressIndicator indicator,
                                         @NotNull final Application app,
                                         @NotNull final Project project,
                                         @NotNull final Module module) {
    final LibOrModuleSet dependencies = new LibOrModuleSet();

    final PsiManager psiManager = PsiManager.getInstance(project);
    final ModuleRootManager roots = ModuleRootManager.getInstance(module);
    final ModuleFileIndex index = roots.getFileIndex();
    index.iterateContent(new ContentIterator() {
      public boolean processFile(@NotNull final VirtualFile fileOrDir) {
        indicator.checkCanceled();

        if (fileOrDir.isDirectory()) return true;
        if (ProjectCoreUtil.isProjectOrWorkspaceFile(fileOrDir)) return true;
        if (!index.isInContent(fileOrDir)) return true;

        app.runReadAction(new Runnable() {
          public void run() {
            final PsiFile psiFile = psiManager.findFile(fileOrDir);

            if (psiFile == null) return;
            if (!psiFile.isValid()) return;
            if (!ProblemHighlightFilter.shouldProcessFileInBatch(psiFile)) return;

            indicator.checkCanceled();
            indicator.setText2("" + ProjectUtil.calcRelativeToProjectPath(fileOrDir, myProject));

            psiFile.accept(new PsiRecursiveElementVisitor() {
              @Override
              public void visitElement(final PsiElement element) {
                super.visitElement(element);

                for (final PsiReference ref : element.getReferences()) {
                  final PsiElement resolved = ref.resolve();
                  if (resolved == null) continue;
                  if (resolved.getProject().isDefault()) continue;
                  if (!resolved.isValid()) continue;

                  final PsiFile file = resolved.getContainingFile();
                  if (file == null) continue;
                  if (!file.isValid()) continue;

                  final VirtualFile virtual = file.getVirtualFile();
                  if (virtual == null) continue;
                  if (!virtual.isValid()) continue;

                  final List<OrderEntry> refs = ProjectRootManager.getInstance(project).getFileIndex().getOrderEntriesForFile(virtual);
                  for (OrderEntry e : refs) {
                    dependencies.addDependency(e);
                  }
                }
              }
            });

            psiManager.dropResolveCaches();
            InjectedLanguageManager.getInstance(psiFile.getProject()).dropFileCaches(psiFile);
          }
        });

        return true;
      }
    });

    final LibOrModuleSet toRemove = new LibOrModuleSet();
    app.runReadAction(new Runnable() {
      public void run() {
        for (OrderEntry e : ModuleRootManager.getInstance(module).getOrderEntries()) {
          if (!dependencies.contains(e)) {
            toRemove.addDependency(e);
          }
        }
      }
    });

    app.invokeLater(new Runnable() {
      public void run() {
        Messages.showInfoMessage(myProject, "Analysis completed:\n" + toRemove, "Jonnyzzz");
      }
    });
  }
}
