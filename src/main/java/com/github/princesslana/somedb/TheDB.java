package com.github.princesslana.somedb;

import java.util.stream.Stream;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;

/** Provides a static/singleton interface to a single database. */
public class TheDB {

  private static OneDB db;

  private TheDB() {}

  private static synchronized void initialize() {
    if (db == null) {
      db = new OneDB(new Config());
    }
  }

  /**
   * Initializes for use with a db with the provided name.
   *
   * @param name the db name
   * @throws IllegalStateException if the db is already initalized
   */
  public static void initialize(String name) {
    if (db != null) {
      throw new IllegalStateException("TheDB is already initialized");
    }
    db = new OneDB(name);
  }

  /**
   * Initializes for use with a db with the provided Config.
   *
   * @param config the db config
   * @throws IllegalStateException if the db is already initialized
   */
  public static void initialize(Config config) {
    if (db != null) {
      throw new IllegalStateException("TheDB is already initialized");
    }
    db = new OneDB(config);
  }

  private static OneDB getDB() {
    if (db == null) {
      initialize();
    }
    return db;
  }

  /**
   * Open a Handle to this database.
   *
   * @return an open Handle instance
   */
  public static Handle open() {
    return getDB().open();
  }

  /**
   * Use a Jdbi Handle.
   *
   * @param consumer the consumer that uses the Handle
   * @param <X> exeception that may be thrown by the consumer
   * @throws X if consumer does
   */
  public static <X extends Exception> void useHandle(HandleConsumer<X> consumer) throws X {
    getDB().useHandle(consumer);
  }

  /**
   * Execute a callback with a Jdbi Handle.
   *
   * @param callback the code to run with the Handle
   * @param <R> return type of callback
   * @param <X> exception that may be thrown by the callback
   * @return the result of the callback
   * @throws X if callback does
   */
  public static <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X {
    return getDB().withHandle(callback);
  }

  /**
   * Execute an SQL statement.
   *
   * @param sql the SQL to execute
   * @param params the parameters to include in the sql
   */
  public static void execute(String sql, Object... params) {
    useHandle(h -> h.execute(sql, params));
  }

  /**
   * Run an SQL query.
   *
   * @param mapper Function to convert from ResultSet to the result we want
   * @param sql the SQL to execute
   * @param params the parameters to include in the sql
   * @param <T> the return type of the mapper
   * @return a Stream of mapped results
   */
  public static <T> Stream<T> select(OneDB.RowMapper<T> mapper, String sql, Object... params) {
    return withHandle(
        h -> h.select(sql, params).map((rs, ctx) -> mapper.apply(rs)).list().stream());
  }
}
