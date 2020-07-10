package com.github.princesslana.somedb;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.flywaydb.core.Flyway;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generator;

/** Process the EnableJooqCodeGen annotation and run code gen for Jooq. */
@SupportedAnnotationTypes("com.github.princesslana.somedb.EnableJooqCodeGen")
public class JooqCodeGenAnnotationProcessor extends AbstractProcessor {

  private boolean hasRun = false;

  private DataSource getTemporaryDb() {
    try {
      var dbName =
          Files.createTempDirectory("somedb").toAbsolutePath().resolve("jooq.db").toString();

      var dataSource = new EmbeddedDataSource();
      dataSource.setDatabaseName(dbName);
      dataSource.setCreateDatabase("create");

      return dataSource;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Configuration getCodeGenerationConfiguration() {
    var db = new Database().withName("org.jooq.meta.derby.DerbyDatabase").withExcludes("flyway_.*");

    return new Configuration().withGenerator(new Generator().withDatabase(db));
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (hasRun) {
      return true;
    }

    var dataSource = getTemporaryDb();

    Flyway.configure()
        .dataSource(dataSource)
        .locations("filesystem:src/main/resources/db/migration")
        .load()
        .migrate();

    try {
      var generate = new GenerationTool();
      generate.setDataSource(dataSource);
      generate.run(getCodeGenerationConfiguration());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    hasRun = true;

    return true;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
}
