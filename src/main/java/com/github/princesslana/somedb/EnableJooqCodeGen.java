package com.github.princesslana.somedb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Add to any class to enable jooq code generation. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface EnableJooqCodeGen {
  /**
   * Name of the package for generated files.
   *
   * @return name of the package to place generated files
   */
  public String packageName();
}
