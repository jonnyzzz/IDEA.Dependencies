package com.eugenePetrenko.idea.depedencies;

import com.eugenePetrenko.idea.dependencies.RemoveModulesModel;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 09.04.13 10:25
 */

public class AnalyzerTest extends AnalyzerTestCase {

  public void testTransitiveClasses() throws Throwable {
    doTest(new AnalyzerTestAction() {
      @Override
      protected void testCode() throws Throwable {
        final ModuleBuilder m1 = module("m1", "transitiveClasses","a");
        final ModuleBuilder m2 = module("m2", "transitiveClasses","b");
        final ModuleBuilder m3 = module("m3", "transitiveClasses","c");

        dep(m3, m2);
        dep(m3, m1); //extra
        dep(m2, m1);

        RemoveModulesModel result = analyzeProject();
        System.out.println("result = " + result);
      }
    });
  }
}
