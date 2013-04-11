package com.eugenePetrenko.idea.depedencies;

import com.eugenePetrenko.idea.dependencies.LibOrModuleSet;
import com.eugenePetrenko.idea.dependencies.RemoveModulesModel;
import com.intellij.openapi.roots.libraries.Library;
import junit.framework.Assert;

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

        RemoveModulesModel result = analyzeProject();
        System.out.println("result = " + result);

        //transitive dependencies must not be removed
        Assert.assertTrue(result.isEmpty());
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

        RemoveModulesModel result = analyzeProject();
        System.out.println("result = " + result);

        //transitive dependencies must not be removed
        Assert.assertTrue(result.isEmpty());
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

        RemoveModulesModel result = analyzeProject();
        System.out.println("result = " + result);

        LibOrModuleSet gold = new LibOrModuleSet();
        gold.addDependency(ia);

        Assert.assertEquals(gold, result.forModule(m1.module()));
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

        RemoveModulesModel result = analyzeProject();
        System.out.println("result = " + result);

        LibOrModuleSet gold = new LibOrModuleSet();
        gold.addDependency(m1.module());

        Assert.assertEquals(gold, result.forModule(m2.module()));
      }
    });
  }
}
