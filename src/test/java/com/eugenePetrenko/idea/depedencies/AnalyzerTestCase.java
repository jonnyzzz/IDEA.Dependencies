package com.eugenePetrenko.idea.depedencies;

import com.eugenePetrenko.idea.dependencies.AnalyzeStrategy;
import com.eugenePetrenko.idea.dependencies.ModuleDependenciesAnalyzer;
import com.eugenePetrenko.idea.dependencies.data.LibOrModuleSet;
import com.eugenePetrenko.idea.dependencies.data.ModulesDependencies;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
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
public abstract class AnalyzerTestCase extends TestCase {
  private final Lazy<File, IOException> myTestDataPath = new Lazy<>() {
    @NotNull
    @Override
    protected File compute() throws IOException {
      String home = PathManager.getResourceRoot(getClass(), "/" + getClass().getName().replace('.', '/') + ".class");
      if (home == null) throw new IOException("Failed to find test data root");
      if (home.startsWith("file://")) {
        home = home.substring("file://".length());
      }
      File file = new File(home).getCanonicalFile();
      while (file != null) {
        final File result = new File(file, "testData");
        if (result.isDirectory() && !file.getName().equals("test")) return result;
        file = file.getParentFile();
      }
      throw new IOException("Failed to find testData");
    }
  };

  @NotNull
  protected String testDataPath(@NotNull final String... path) {
    return testData(path).getPath();
  }

  @NotNull
  protected File testData(@NotNull final String... path) {
    try {
      File result = myTestDataPath.get();
      for (String s : path) {
        result = new File(result, s);
      }
      if (!result.exists()) throw new IOException("Failed to find path: " + result);
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  protected class ModuleBuilder {
    private final TestFixtureBuilder<IdeaProjectTestFixture> myHost;
    private final ModuleFixture myModule;

    public ModuleBuilder(@NotNull final TestFixtureBuilder<IdeaProjectTestFixture> host,
                         @NotNull final String name,
                         @NotNull final String[] path) throws Exception {
      myHost = host;
      final JavaModuleFixtureBuilder bld = host.addModule(JavaModuleFixtureBuilder.class);
      bld.setMockJdkLevel(JavaModuleFixtureBuilder.MockJdkLevel.jdk15);
      bld.addSourceContentRoot(testDataPath(path));
      myModule = bld.getFixture();
      myModule.setUp();

      WriteAction.run(() -> {
        final ModifiableModuleModel model = ModuleManager.getInstance(project()).getModifiableModel();
        try {
          model.renameModule(module(), name);
        } catch (ModuleWithNameAlreadyExists moduleWithNameAlreadyExists) {
          model.dispose();
          Assert.fail();
          return;
        }
        model.commit();
      });
    }

    @NotNull
    public Project project() {
      return myHost.getFixture().getProject();
    }

    @NotNull
    public Module module() {
      return myModule.getModule();
    }

    public void lib(@NotNull final Library... libs) {
      for (final Library lib : libs) {
        WriteAction.run(() -> {
          final ModifiableRootModel mod = ModuleRootManager.getInstance(module()).getModifiableModel();
          mod.addLibraryEntry(lib).setScope(DependencyScope.COMPILE);
          mod.commit();
        });
      }
    }
  }

  public abstract class AnalyzerTestAction {
    private final TestFixtureBuilder<IdeaProjectTestFixture> myHost;

    public AnalyzerTestAction() {
      myHost = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("aaa");
    }

    @NotNull
    public ModuleBuilder module(@NotNull String name, @NotNull String... path) throws Exception {
      return new ModuleBuilder(myHost, name, path);
    }

    public void dep(@NotNull ModuleBuilder from, @NotNull ModuleBuilder to) {
      dep(from, to, false);
    }

    public void dep(@NotNull ModuleBuilder from, @NotNull ModuleBuilder to, boolean export) {
      ModuleRootModificationUtil.addDependency(from.module(), to.module(), DependencyScope.COMPILE, export);
    }

    @NotNull
    public ModulesDependencies analyzeProject(@NotNull AnalyzeStrategy strategy) {
      return ModuleDependenciesAnalyzer.processAllDependencies(
              strategy,
              new EmptyProgressIndicator(),
              project()
      );
    }

    @NotNull
    public Project project() {
      return myHost.getFixture().getProject();
    }

    @NotNull
    public Library lib(@NotNull final String name, @NotNull String... path) {
      final String url = "file://" + testDataPath(path).replace("\\", "/") + "/";
      return WriteAction.compute(() -> {
        final Library lib = LibraryTablesRegistrar.getInstance().getLibraryTable(project()).createLibrary(name);
        final Library.ModifiableModel model = lib.getModifiableModel();
        model.addRoot(url, OrderRootType.CLASSES);
        model.commit();

        return lib;
      });
    }

    public final void doTheTest() throws Throwable {
      final JavaCodeInsightTestFixture java = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(myHost.getFixture());
      java.setUp();
      testCode();
    }


    public class ResultChecker {
      private final ModulesDependencies myExpected = new ModulesDependencies();

      @NotNull
      public ResultChecker removes(@NotNull ModuleBuilder from, @NotNull ModuleBuilder to) {
        LibOrModuleSet set = new LibOrModuleSet();
        set.addDependency(to.module());
        myExpected.addAll(from.module(), set);
        return this;
      }

      @NotNull
      public ResultChecker removes(@NotNull ModuleBuilder from, @NotNull Library to) {
        LibOrModuleSet set = new LibOrModuleSet();
        set.addDependency(to);
        myExpected.addAll(from.module(), set);
        return this;
      }

      public void assertActual(@NotNull ModulesDependencies actual) {
        Assert.assertEquals(myExpected, actual);
      }
    }

    @NotNull
    public ResultChecker assertBuilder() {
      return new ResultChecker();
    }

    protected abstract void testCode() throws Throwable;
  }


  protected void doTest(@NotNull AnalyzerTestAction action) throws Throwable {
    action.doTheTest();
  }

}
