package com.eugenePetrenko.idea.dependencies;

import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 06.04.13 11:09
 */
public class ModuleDependenciesAnalyzer implements ApplicationComponent {
  private static final Logger LOG = Logger.getInstance(ModuleDependenciesAnalyzer.class.getName());

  public void initComponent() {
  }

  public void disposeComponent() {
  }

  @NotNull
  public String getComponentName() {
    return "jonnyzzz" + getClass().getSimpleName();
  }

  /**
   * Performs references analysis for all modules
   * @param indicator progress
   * @param app application
   * @param project project
   * @return set of module dependencies that could be removed
   */
  @NotNull
  public Map<Module, LibOrModuleSet> processAllDependencies(@NotNull final ProgressIndicator indicator,
                                                  @NotNull final Application app,
                                                  @NotNull final Project project) {
    final Module[] modules = app.runReadAction(new Computable<Module[]>() {
      public Module[] compute() {
        return ModuleManager.getInstance(project).getSortedModules();
      }
    });

    final Map<Module, LibOrModuleSet> result = new HashMap<Module, LibOrModuleSet>();
    for (int i = 0; i < modules.length; i++) {
      final Module module = modules[i];

      indicator.setIndeterminate(false);
      indicator.checkCanceled();
      indicator.setText(module.getName());
      indicator.setFraction((double) i / (double) modules.length);

      result.put(module, processModuleDependencies(indicator, app, project, module));
    }

    return result;
  }



    /**
     * Performs references analysis for given module
     * @param indicator progress
     * @param app application
     * @param project project
     * @param module module
     * @return set of module dependencies that could be removed
     */
  @NotNull
  public LibOrModuleSet processModuleDependencies(@NotNull final ProgressIndicator indicator,
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
            indicator.setText2("" + ProjectUtil.calcRelativeToProjectPath(fileOrDir, project));

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

    return toRemove;
  }
}
