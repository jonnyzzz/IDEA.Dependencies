package com.eugenePetrenko.idea.dependencies.ui;

import com.intellij.openapi.module.Module;

import java.util.Comparator;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 08.04.13 0:45
 */
public class Comparators {
  public static final Comparator<Module> MODULE_COMPARATOR = new Comparator<Module>() {
    public int compare(Module o1, Module o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

}
