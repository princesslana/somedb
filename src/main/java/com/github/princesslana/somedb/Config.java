package com.github.princesslana.somedb;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Configuration information for a database. */
public class Config {

  private final String dbName;

  private final Path dataPath;

  /** Construct a Config with the default values. */
  public Config() {
    this("the", Paths.get("data"));
  }

  private Config(String dbName, Path dataPath) {
    this.dbName = dbName;
    this.dataPath = dataPath;
  }

  /**
   * Get the name to use for the database.
   *
   * @return the database name
   */
  public String getDbName() {
    return dbName;
  }

  /**
   * Construct a copy of this Config with the given db name.
   *
   * @param dbName the db name
   * @return the new Config
   */
  public Config withDbName(String dbName) {
    return new Config(dbName, dataPath);
  }

  /**
   * Get the folder where data will be stored.
   *
   * @return folder where data will be stored
   */
  public Path getDataPath() {
    return dataPath;
  }

  /**
   * Construct a copy of this Config with the given data path.
   *
   * @param first the data path
   * @param more the rest of the data path to join to first
   * @return the new Config
   */
  public Config withDataPath(String first, String... more) {
    return withDataPath(Paths.get(first, more));
  }

  /**
   * Construct a copy of this Config with the given data path.
   *
   * @param path the data path
   * @return the new Config
   */
  public Config withDataPath(Path path) {
    return new Config(dbName, path);
  }

  /**
   * Constructs a Config based upon the system environment. The uppercase name is used as a prefix
   * for any environment variables.
   *
   * <p>{name}_DB_PATH to set the data path.
   *
   * @param name the name for the db
   * @return the new Config
   */
  public static Config fromEnv(String name) {
    Config cfg = new Config().withDbName(name);

    var pathFromEnv = System.getenv(name.toUpperCase() + "_DB_PATH");
    if (pathFromEnv != null) {
      cfg = cfg.withDataPath(pathFromEnv);
    }

    return cfg;
  }
}
