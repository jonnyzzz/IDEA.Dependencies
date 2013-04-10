package com.eugenePetrenko.idea.depedencies;

import com.eugenePetrenko.idea.dependencies.ModuleDependenciesAnalyzer;
import com.eugenePetrenko.idea.dependencies.RemoveModulesModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 10.04.13 9:51
 */
public class AnalyzerTestCase extends TestCase {
  private final Lazy<File, IOException> myTestDataPath = new Lazy<File, IOException>() {
    @NotNull
    @Override
    protected File compute() throws IOException {
      String home = PathManager.getResourceRoot(getClass(), "/" + getClass().getName().replace('.', '/') + ".class");
      if (home == null) throw new IOException("Failed to find test data root");
      if (home.startsWith("file://")){
        home = home.substring("file://".length());
      }
      File file = new File(home).getCanonicalFile();
      while (file != null) {
        final File result = new File(file, "testData");
        if (result.isDirectory()) return result;
        file = file.getParentFile();
      }
      throw new IOException("Failed to find testData");
    }
  };

  @NotNull
  protected String testDataPath(@NotNull final String... path) throws IOException {
    return testData(path).getPath();
  }

  @NotNull
  protected File testData(@NotNull final String... path) throws IOException {
    File result = myTestDataPath.get();
    for (String s : path) {
      result = new File(result, s);
    }
    if (!result.exists()) throw new IOException("Failed to find path: " + result);
    return result;
  }

  protected class ModuleBuilder {
    private final TestFixtureBuilder<IdeaProjectTestFixture> myHost;
    private final JavaModuleFixtureBuilder myModule;

    public ModuleBuilder(@NotNull final TestFixtureBuilder<IdeaProjectTestFixture> host) throws Exception {
      myHost = host;
      myModule = myHost.addModule(JavaModuleFixtureBuilder.class);
      myModule.getFixture().setUp();
      myModule.setMockJdkLevel(JavaModuleFixtureBuilder.MockJdkLevel.jdk15);
    }

    @NotNull
    public Project project() {
      return myHost.getFixture().getProject();
    }

    @NotNull
    public Module module() {
      return myModule.getFixture().getModule();
    }

    @NotNull
    public ModuleBuilder name(@NotNull final String name) {
      new WriteAction<Void>(){
        @Override
        protected void run(Result<Void> result) throws Throwable {
          final ModifiableModuleModel model = ModuleManager.getInstance(project()).getModifiableModel();
                    try {
                      model.renameModule(module(), name);
                    } catch (ModuleWithNameAlreadyExists moduleWithNameAlreadyExists) {
                      model.dispose();
                      Assert.fail();
                      return;
                    }
                    model.commit();
        }
      }.execute();
      return this;
    }

    public ModuleBuilder source(@NotNull String... path) throws IOException {
      myModule.addSourceContentRoot(testDataPath(path));
      return this;
    }
  }

  public abstract class AnalyzerTestAction {
    private final TestFixtureBuilder<IdeaProjectTestFixture> myHost;

    public AnalyzerTestAction() {
      myHost = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("aaa");
    }

    @NotNull
    public ModuleBuilder module(@NotNull String name, @NotNull String... path) throws Exception {
      ModuleBuilder builder = new ModuleBuilder(myHost);
      builder.name(name);
      builder.source(path);
      return builder;
    }

    public void dep(@NotNull ModuleBuilder from, @NotNull ModuleBuilder to) {
      ModuleRootModificationUtil.addDependency(from.module(), to.module());
    }

    @NotNull
    public RemoveModulesModel analyzeProject() {
      return ModuleDependenciesAnalyzer.processAllDependencies(
              new EmptyProgressIndicator(),
              ApplicationManager.getApplication(),
              project()
      );
    }

    @NotNull
    public Project project() {
      return myHost.getFixture().getProject();
    }

    public final void doTheTest() throws Throwable {
      final JavaCodeInsightTestFixture java = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(myHost.getFixture());
      java.setUp();
      try {
        testCode();
      } finally {
        java.tearDown();
      }
    }

    protected abstract void testCode() throws Throwable;
  }


  protected void doTest(@NotNull AnalyzerTestAction action) throws Throwable {
    action.doTheTest();
  }

}
