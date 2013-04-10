package com.eugenePetrenko.idea.depedencies;

import org.jetbrains.annotations.NotNull;

/**
* Created by Eugene Petrenko (eugene.petrenko@gmail.com)
* Date: 10.04.13 9:47
*/
public abstract class Lazy<T, E extends Throwable> {
  private T myValue;
  @NotNull
  protected abstract T compute() throws E;

  public synchronized T get() throws E{
    if (myValue == null) {
      myValue = compute();
    }
    return myValue;
  }
}
