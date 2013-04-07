package com.eugenePetrenko.idea.dependencies;

import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.concurrency.JobLauncher;
import com.intellij.ide.util.DelegatingProgressIndicator;
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
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
   *
   * @param indicator progress
   * @param app       application
   * @param project   project
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
      final double outerFraction = (double) i / (double) modules.length;
      final double outerStep = 1.0 / modules.length;
      indicator.setFraction(outerFraction);

      DelegatingProgressIndicator subProgress = new DelegatingProgressIndicator(indicator){
        @Override
        public double getFraction() {
          return (super.getFraction() - outerFraction) * outerStep;
        }

        @Override
        public void setFraction(double fraction) {
          super.setFraction(outerFraction + fraction / outerStep);
        }
      };
      result.put(module, processModuleDependencies(subProgress, app, project, module));
    }

    return result;
  }


  /**
   * Performs references analysis for given module
   *
   * @param indicator progress
   * @param app       application
   * @param project   project
   * @param module    module
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
    final ModuleFileIndex moduleIndex = roots.getFileIndex();
    final ProjectFileIndex projectIndex = ProjectRootManager.getInstance(project).getFileIndex();

    final List<VirtualFile> allFiles = new ArrayList<VirtualFile>(1000);
    moduleIndex.iterateContent(new ContentIterator() {
      public boolean processFile(@NotNull final VirtualFile fileOrDir) {
        indicator.checkCanceled();

        if (fileOrDir.isDirectory()) return true;
        if (ProjectCoreUtil.isProjectOrWorkspaceFile(fileOrDir)) return true;
        if (!moduleIndex.isInContent(fileOrDir)) return true;

        allFiles.add(fileOrDir);
        return true;
      }
    });

    final double total = allFiles.size();
    final AtomicInteger current = new AtomicInteger();

    JobLauncher.getInstance().invokeConcurrentlyUnderProgress(
            allFiles,
            indicator,
            true,
            new Processor<VirtualFile>() {
              public boolean process(@NotNull final VirtualFile file) {
                indicator.checkCanceled();
                indicator.setFraction(current.incrementAndGet() / total);

                final Set<OrderEntry> oes = new HashSet<OrderEntry>(10);
                app.runReadAction(new Runnable() {
                  public void run() {
                    final PsiFile psiFile = psiManager.findFile(file);

                    if (psiFile == null) return;
                    if (!psiFile.isValid()) return;
                    if (!ProblemHighlightFilter.shouldProcessFileInBatch(psiFile)) return;

                    indicator.checkCanceled();
                    indicator.setText2("" + ProjectUtil.calcRelativeToProjectPath(file, project));

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


                          oes.addAll(projectIndex.getOrderEntriesForFile(virtual));
                        }
                      }
                    });

                    psiManager.dropResolveCaches();
                    InjectedLanguageManager.getInstance(psiFile.getProject()).dropFileCaches(psiFile);
                  }
                });

                synchronized (dependencies) {
                  dependencies.addDependencies(oes);
                }
                return true;
              }
            }
    );

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
