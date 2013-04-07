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
import com.intellij.openapi.project.DumbModeAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiFileEx;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

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

  public void processDependencies() {
    final ModuleManager moduleManager = ModuleManager.getInstance(myProject);

    for (Module module : moduleManager.getSortedModules()) {
      for (OrderEntry dep : ModuleRootManager.getInstance(module).getOrderEntries()) {
        if (dep instanceof ModuleOrderEntry) {
          System.out.println(module.getName() + " -> " + dep.getPresentableName());
        }
        if (dep instanceof LibraryOrderEntry) {
          DependencyScope scope = ((LibraryOrderEntry) dep).getScope();
          System.out.println(module.getName() + " -> lib:" + dep.getPresentableName() + " [" + scope.getDisplayName() + "]");
        }
      }
      System.out.println();

      ProgressManager.getInstance().runProcess(

              new Task.Backgroundable(myProject, "Analyzer", true, BackgroundFromStartOption.getInstance()) {
                @Override
                public boolean shouldStartInBackground() {
                  return true;
                }

                @Override
                public DumbModeAction getDumbModeAction() {
                  return DumbModeAction.WAIT;
                }

                @Override
                public String getProcessId() {
                  return "jonnyzzz";
                }

                public void run(@NotNull final ProgressIndicator indicator) {
                  final Application app = ApplicationManager.getApplication();
                  app.runReadAction(new Runnable() {
                    public void run() {
                      final Module[] modules = ModuleManager.getInstance(myProject).getSortedModules();
                      for (int i = 0; i < modules.length; i++) {
                        final Module module = modules[i];

                        indicator.checkCanceled();
                        indicator.setText(module.getName());
                        indicator.setFraction((double) i / (double) modules.length);

                        app.executeOnPooledThread(new Runnable() {
                          public void run() {
                            analyzeModuleDependencies(module);
                          }
                        });
                      }
                    }
                  });
                }
              });
    }
  }

  private void analyzeModuleDependencies(@NotNull final Module module) {
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    psiManager.startBatchFilesProcessingMode();

    try {
      final Set<Module> myVisitedModules = new HashSet<Module>();
      final Set<Library> myVisitedLibraries = new HashSet<Library>();

      final ModuleRootManager roots = ModuleRootManager.getInstance(module);
      final ModuleFileIndex index = roots.getFileIndex();
      index.iterateContent(new ContentIterator() {
        public boolean processFile(@NotNull final VirtualFile fileOrDir) {
          final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
          indicator.checkCanceled();

          if (fileOrDir.isDirectory()) return true;
          if (ProjectCoreUtil.isProjectOrWorkspaceFile(fileOrDir)) return true;
          if (!index.isInContent(fileOrDir)) return true;

          final PsiFile psiFile = psiManager.findFile(fileOrDir);
          if (psiFile == null) return true;
          if (!psiFile.isValid()) return true;
          if (!ProblemHighlightFilter.shouldProcessFileInBatch(psiFile)) return true;

          indicator.setText2("" + ProjectUtil.calcRelativeToProjectPath(fileOrDir, myProject));

          psiFile.putUserData(PsiFileEx.BATCH_REFERENCE_PROCESSING, Boolean.TRUE);
          psiFile.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
              super.visitElement(element);

              for (PsiReference ref : element.getReferences()) {
                final PsiElement resolved = ref.resolve();
                if (resolved == null) continue;
                final PsiFile resolvedFile = resolved.getContainingFile();
                if (resolvedFile == null) continue;

                ///
                continue;


              }
            }
          });
          psiFile.putUserData(PsiFileEx.BATCH_REFERENCE_PROCESSING, null);


          psiFile.accept(new PsiRecursiveElementVisitor(){

          });
          psiManager.dropResolveCaches();
          InjectedLanguageManager.getInstance(psiFile.getProject()).dropFileCaches(psiFile);


          return true;
        }
      });
    } finally {
      psiManager.finishBatchFilesProcessingMode();
    }

  }
}
