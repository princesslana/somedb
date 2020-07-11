package com.github.princesslana.somedb;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.sql.DataSource;
import javax.tools.Diagnostic.Kind;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.flywaydb.core.Flyway;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Target;

/** Process the EnableJooqCodeGen annotation and run code gen for Jooq. */
@SupportedAnnotationTypes("com.github.princesslana.somedb.EnableJooqCodeGen")
public class JooqCodeGenAnnotationProcessor extends AbstractProcessor {

  private boolean hasRun = false;

  private Messager messager;

  @Override
  public void init(ProcessingEnvironment env) {
    super.init(env);
    messager = env.getMessager();
  }

  private DataSource getTemporaryDb() throws IOException {
    var dbName = Files.createTempDirectory("somedb").toAbsolutePath().resolve("jooq.db").toString();

    var dataSource = new EmbeddedDataSource();
    dataSource.setDatabaseName(dbName);
    dataSource.setCreateDatabase("create");

    return dataSource;
  }

  private Configuration getCodeGenerationConfiguration(String packageName) {
    var db =
        new Database()
            .withName("org.jooq.meta.derby.DerbyDatabase")
            .withExcludes("flyway_.*")
            .withInputSchema("APP")
            .withOutputSchemaToDefault(true);

    var target = new Target().withPackageName(packageName);

    return new Configuration().withGenerator(new Generator().withDatabase(db).withTarget(target));
  }

  private void runCodeGen(EnableJooqCodeGen annotation) throws Exception {
    var dataSource = getTemporaryDb();

    Flyway.configure()
        .dataSource(dataSource)
        .locations("filesystem:src/main/resources/db/migration")
        .load()
        .migrate();

    try {
      var generate = new GenerationTool();
      generate.setDataSource(dataSource);
      generate.run(getCodeGenerationConfiguration(annotation.packageName()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (hasRun) {
      return true;
    }

    messager.printMessage(Kind.NOTE, "Generating Jooq classes...");

    var elements = roundEnv.getElementsAnnotatedWith(EnableJooqCodeGen.class);

    if (elements.size() > 1) {
      messager.printMessage(
          Kind.WARNING,
          "@EnableJooqCodeGen is present on more than one class. "
              + "This may result in undefined behavior");
    }

    for (var element : elements) {
      var annotation = element.getAnnotation(EnableJooqCodeGen.class);

      try {
        runCodeGen(annotation);
      } catch (Exception e) {
        messager.printMessage(Kind.ERROR, "Exception generating Jooq classes. " + e.getMessage());
      }
    }

    messager.printMessage(Kind.NOTE, "Jooq classes generated.");

    hasRun = true;
    return true;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
}
