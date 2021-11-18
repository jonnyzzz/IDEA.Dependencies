package com.eugenePetrenko.idea.depedencies;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created 10.04.13 19:43
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TestFor {
  @NotNull
  Class[] testForClass() default {};
  String[] issues() default {};
}
