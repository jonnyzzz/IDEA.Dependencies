package com.eugenePetrenko.idea.depedencies;

import com.eugenePetrenko.idea.dependencies.ModulesDependencies;
import com.intellij.openapi.roots.libraries.Library;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 09.04.13 10:25
 */

public class AnalyzerTest extends AnalyzerTestCase {

  @TestFor(issues = "#4")
  public void testTransitiveClasses() throws Throwable {
    doTest(new AnalyzerTestAction() {
      @Override
      protected void testCode() throws Throwable {
        final ModuleBuilder m1 = module("m1", "transitiveClasses","a");
        final ModuleBuilder m2 = module("m2", "transitiveClasses","b");
        final ModuleBuilder m3 = module("m3", "transitiveClasses","c");
        final ModuleBuilder m4 = module("m4", "transitiveClasses","d");

        dep(m4, m3);
        dep(m4, m2);
        dep(m4, m1);

        dep(m3, m2);
        dep(m3, m1); //must not be extra extra

        dep(m2, m1);

        ModulesDependencies result = analyzeProject();
        System.out.println("result = " + result);

        //transitive dependencies must not be removed
        assertBuilder().assertActual(result);
      }
    });
  }

  @TestFor(issues = "#4")
  public void testTransitiveLibs() throws Throwable {
    doTest(new AnalyzerTestAction() {
      @Override
      protected void testCode() throws Throwable {
        final ModuleBuilder m1 = module("m1", "transitiveLibs", "a");
        final ModuleBuilder m2 = module("m2", "transitiveLibs", "b");

        Library ia = lib("ia", "transitiveLibs", "lib", "a.i");
        Library la = lib("la", "transitiveLibs", "lib", "a");
        Library lb = lib("lb", "transitiveLibs", "lib", "b");
        Library lc = lib("lc", "transitiveLibs", "lib", "c");

        m1.lib(lc);
        m2.lib(ia, la, lb);

        ModulesDependencies result = analyzeProject();
        System.out.println("result = " + result);

        //transitive dependencies must not be removed
        assertBuilder().assertActual(result);
      }
    });
  }

  @TestFor(issues = "#4")
  public void testUnusedLibraryIsRemoved() throws Throwable {
    doTest(new AnalyzerTestAction() {
      @Override
      protected void testCode() throws Throwable {
        final ModuleBuilder m1 = module("m1", "transitiveLibs", "a");

        Library ia = lib("ia", "transitiveLibs", "lib", "a.i");
        Library lc = lib("lc", "transitiveLibs", "lib", "c");

        m1.lib(lc);
        m1.lib(ia); //unused

        ModulesDependencies result = analyzeProject();
        System.out.println("result = " + result);

        assertBuilder().removes(m1, ia).assertActual(result);
      }
    });
  }

  @TestFor(issues = "#4")
  public void testUnusedModuleIsRemoved() throws Throwable {
    doTest(new AnalyzerTestAction() {
      @Override
      protected void testCode() throws Throwable {
        final ModuleBuilder m1 = module("m1", "transitiveClasses","a");
        final ModuleBuilder m2 = module("m2", "transitiveLibs", "b");

        dep(m2, m1); //should not be here

        ModulesDependencies result = analyzeProject();
        System.out.println("result = " + result);

        assertBuilder().removes(m2, m1).assertActual(result);
      }
    });
  }

  @TestFor(issues = "#1")
  public void testExportDependency() throws Throwable {
    doTest(new AnalyzerTestAction() {
      @Override
      protected void testCode() throws Throwable {
        final ModuleBuilder mE = module("mE", "exportClasses", "aExport");
        final ModuleBuilder mR = module("mR", "exportClasses", "a");
        final ModuleBuilder mB = module("mB", "exportClasses", "b");
        final ModuleBuilder mC = module("mC", "exportClasses", "c");
        final ModuleBuilder mD = module("mD", "exportClasses", "d");
        final ModuleBuilder mQ = module("mQ", "exportClasses", "q");

        dep(mR, mE, true); //this is exported dependency with no actual use inside

        dep(mB, mR); //uses both mR and mE
        dep(mC, mR); //uses only mR
        dep(mD, mR); //uses only mE
        dep(mQ, mR); //uses none


        ModulesDependencies result = analyzeProject();
        System.out.println("result = " + result);

        assertBuilder()
                .removes(mQ, mR)
                .assertActual(result);
      }
    });
  }
}
