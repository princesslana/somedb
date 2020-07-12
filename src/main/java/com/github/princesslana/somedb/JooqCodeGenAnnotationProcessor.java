package com.github.princesslana.somedb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.sql.DataSource;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
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

  private Filer filer;

  @Override
  public void init(ProcessingEnvironment env) {
    super.init(env);
    messager = env.getMessager();
    filer = env.getFiler();
  }

  private static void touch(FileObject f) throws IOException {
    try (var w = f.openWriter()) {}
  }

  private Path getTargetDirectory() throws IOException {
    // We do this by creating a resource in the root of the output.
    // We call touch to avoid a warning about not closing it.
    // This does leave an empty file in the root of the generated sources
    var fileInTargetDirectory =
        filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "somedb.jooq");

    var targetDirectory = Paths.get(fileInTargetDirectory.toUri()).getParent();

    touch(fileInTargetDirectory);

    return targetDirectory;
  }

  private DataSource getTemporaryDb() throws IOException {
    var dbName = Files.createTempDirectory("somedb").toAbsolutePath().resolve("jooq.db").toString();

    var dataSource = new EmbeddedDataSource();
    dataSource.setDatabaseName(dbName);
    dataSource.setCreateDatabase("create");

    return dataSource;
  }

  private Configuration getCodeGenerationConfiguration(String packageName) throws IOException {
    var db =
        new Database()
            .withName("org.jooq.meta.derby.DerbyDatabase")
            .withExcludes("flyway_.*")
            .withInputSchema("APP")
            .withOutputSchemaToDefault(true);

    var target =
        new Target().withPackageName(packageName).withDirectory(getTargetDirectory().toString());

    return new Configuration().withGenerator(new Generator().withDatabase(db).withTarget(target));
  }

  private void runCodeGen(EnableJooqCodeGen annotation) throws Exception {
    var dataSource = getTemporaryDb();

    Flyway.configure()
        .dataSource(dataSource)
        .locations("filesystem:src/main/resources/db/migration")
        .load()
        .migrate();

    // We do this so that the compiler knows that we have generated source files.
    // These files will always be genereated by Jooq regardless of schema.
    // We don't do this for our schema specific files (we don't know what they are until
    // after codegen), which is ok because they get implicitly compiled.
    // We do get a warning from the compiler because of this though.
    for (var name : Set.of(".Tables", ".Indexes", ".DefaultSchema", ".DefaultCatalog")) {
      try {
        touch(filer.createSourceFile(annotation.packageName() + name));
      } catch (FilerException e) {
        // this is expected if the files exist from a previous run
      }
    }

    var generate = new GenerationTool();
    generate.setDataSource(dataSource);
    generate.run(getCodeGenerationConfiguration(annotation.packageName()));
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (hasRun) {
      return false;
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
    return false;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
}
