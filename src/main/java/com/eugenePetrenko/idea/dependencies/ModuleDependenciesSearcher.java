/*
 * Copyright 2013-2013 Eugene Petrenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eugenePetrenko.idea.dependencies;

import com.eugenePetrenko.idea.dependencies.data.LibOrModuleSet;
import com.eugenePetrenko.idea.dependencies.data.ModulesDependencies;
import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.concurrency.JobLauncher;
import com.intellij.ide.util.DelegatingProgressIndicator;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intellij.psi.PsiReferenceService.Hints.NO_HINTS;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 17.04.13 10:47
 */
public class ModuleDependenciesSearcher {
  /**
   * Performs references analysis for given module
   *
   * @param indicator progress
   * @param project   project
   * @param module    module
   * @return set of module dependencies that could be removed
   */
  @NotNull
  public static LibOrModuleSet collectionActualModuleDependencies(@NotNull final ProgressIndicator indicator,
                                                                  @NotNull final Project project,
                                                                  @NotNull final Module module) {
    final LibOrModuleSet dependencies = new LibOrModuleSet();

    final PsiManager psiManager = PsiManager.getInstance(project);
    final ModuleRootManager roots = ModuleRootManager.getInstance(module);
    final ModuleFileIndex moduleIndex = roots.getFileIndex();
    final ProjectFileIndex projectIndex = ProjectRootManager.getInstance(project).getFileIndex();

    final List<VirtualFile> allFiles = new ArrayList<>(1000);
    moduleIndex.iterateContent(fileOrDir -> {
      indicator.checkCanceled();

      if (fileOrDir.isDirectory()) return true;
      if (ProjectCoreUtil.isProjectOrWorkspaceFile(fileOrDir)) return true;
      if (!moduleIndex.isInContent(fileOrDir)) return true;
      if (moduleIndex.isInSourceContent(fileOrDir) || moduleIndex.isInTestSourceContent(fileOrDir)) {
        allFiles.add(fileOrDir);
      }
      return true;
    });

    final double total = allFiles.size();
    final AtomicInteger current = new AtomicInteger();

    JobLauncher.getInstance().invokeConcurrentlyUnderProgress(
            allFiles,
            indicator,
            new Processor<>() {
              public boolean process(@NotNull final VirtualFile file) {
                indicator.checkCanceled();
                indicator.setFraction(current.incrementAndGet() / total);

                final Set<OrderEntry> oes = new HashSet<>(10);
                ApplicationManager.getApplication().runReadAction(new Runnable() {
                  public void run() {
                    final PsiFile psiFile = psiManager.findFile(file);

                    if (psiFile == null) return;
                    if (!psiFile.isValid()) return;
                    if (!ProblemHighlightFilter.shouldProcessFileInBatch(psiFile)) return;

                    indicator.checkCanceled();
                    indicator.setText2("" + ProjectUtil.calcRelativeToProjectPath(file, project));

                    psiFile.accept(new PsiRecursiveElementVisitor() {
                      @Override
                      public void visitElement(final @NotNull PsiElement element) {
                        super.visitElement(element);

                        for (final PsiReference ref : PsiReferenceService.getService().getReferences(element, NO_HINTS)) {
                          processResolvedElement(ref.resolve());
                        }
                      }

                      private void processResolvedElement(@Nullable PsiElement resolved) {
                        if (resolved == null) return;
                        if (resolved.getProject().isDefault()) return;
                        if (!resolved.isValid()) return;

                        if (resolved instanceof PsiClass) {
                          InheritanceUtil.processSupers((PsiClass) resolved, true, psiClass -> {
                            registerUsage(psiClass);
                            return true;
                          });
                          return;
                        }

                        registerUsage(resolved);
                      }

                      private void registerUsage(@NotNull PsiElement resolved) {
                        final PsiFile file = resolved.getContainingFile();
                        if (file == null) return;
                        if (!file.isValid()) return;

                        final VirtualFile virtual = file.getVirtualFile();
                        if (virtual == null) return;
                        if (!virtual.isValid()) return;

                        oes.addAll(projectIndex.getOrderEntriesForFile(virtual));
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

    return dependencies;
  }

  /**
     * Performs references analysis for given module
     *
     * @param indicator progress
     * @param project   project
     * @param modules   modules
     * @return set of module dependencies that could be removed
     */
    @NotNull
    public static ModulesDependencies collectionActualModulesDependencies(@NotNull final ProgressIndicator indicator,
                                                                          @NotNull final Project project,
                                                                          @NotNull final Module[] modules) {
      final ModulesDependencies result = new ModulesDependencies();
      final double length = modules.length;
      final double outerStep = 1.0 / length;

      for (int i = 0; i < modules.length; i++) {
        final Module module = modules[i];

        indicator.setIndeterminate(false);
        indicator.checkCanceled();
        indicator.setText(module.getName());

        final double outerFraction = (double) i / length;
        indicator.setFraction(outerFraction);

        DelegatingProgressIndicator subProgress = new DelegatingProgressIndicator(indicator) {
          @Override
          public double getFraction() {
            return (super.getFraction() - outerFraction) / outerStep;
          }

          @Override
          public void setFraction(double fraction) {
            super.setFraction(outerFraction + fraction * outerStep);
          }
        };
        result.addAll(module, collectionActualModuleDependencies(subProgress, project, module));
      }
      return result;
    }
}
