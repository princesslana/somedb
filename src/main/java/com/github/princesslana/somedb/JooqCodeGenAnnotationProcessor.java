package com.github.princesslana.somedb;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Property;

/** Process the EnableJooqCodeGen annotation and run code gen for Jooq. */
@SupportedAnnotationTypes("com.github.princesslana.somedb.EnableJooqCodeGen")
public class JooqCodeGenAnnotationProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    var config =
        new Configuration()
            .withGenerator(
                new Generator()
                    .withDatabase(
                        new Database()
                            .withName("org.jooq.meta.extensions.ddl.DDLDatabase")
                            .withProperties(
                                new Property()
                                    .withKey("scripts")
                                    .withValue("src/main/resources/db/migration/*.sql"),
                                new Property().withKey("sort").withValue("flyway"),
                                new Property().withKey("unqualifiedSchema").withValue("none"),
                                new Property().withKey("defaultNameCase").withValue("as_is"))));

    try {
      GenerationTool.generate(config);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return true;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
}
