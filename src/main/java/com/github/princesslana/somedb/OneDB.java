package com.github.princesslana.somedb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A single database. */
public class OneDB {

  private static final Logger LOG = LoggerFactory.getLogger(OneDB.class);

  private final Config config;

  private Jdbi jdbi;

  /**
   * Creates a database with the given name, reading the Config from the system environment.
   *
   * @param name the db name
   */
  public OneDB(String name) {
    this(Config.fromEnv(name));
  }

  /**
   * Creates a database with the provided config.
   *
   * @param config the configuration for this database
   */
  public OneDB(Config config) {
    this.config = config;
  }

  /**
   * Initialize this database. It is not required to call initialize, as it will be performed the
   * first time that it is needed. However, sometimes it is desierable to have initialization
   * performed at a known time (e.g., on startup).
   */
  public synchronized void initialize() {
    if (jdbi == null) {
      LOG.info("Initializing database {}...", config.getDbName());

      var dbName = config.getDataPath().resolve(config.getDbName() + ".db").toString();

      var dataSource = new EmbeddedDataSource();
      dataSource.setDatabaseName(dbName);
      dataSource.setCreateDatabase("create");

      Flyway.configure().dataSource(dataSource).load().migrate();

      jdbi = Jdbi.create(dataSource);
    }
  }

  /**
   * Provides access to the Jdbi instance. The methods on OneDB are designed to cover the most
   * common use cases. The Jdbi instance is available if they do not fit your needs.
   *
   * @return the Jdbi instance
   */
  public synchronized Jdbi getJdbi() {
    if (jdbi == null) {
      initialize();
    }
    return jdbi;
  }

  /**
   * Open a Handle to this database.
   *
   * @return an open Handle instance
   */
  public Handle open() {
    return getJdbi().open();
  }

  /**
   * Use a Jdbi Handle.
   *
   * @param consumer the consumer that uses the Handle
   * @param <X> exeception that may be thrown by the consumer
   * @throws X if consumer does
   */
  public <X extends Exception> void useHandle(HandleConsumer<X> consumer) throws X {
    getJdbi().useHandle(consumer);
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
  public <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X {
    return getJdbi().withHandle(callback);
  }

  /**
   * Execute an SQL statement.
   *
   * @param sql the SQL to execute
   * @param params the parameters to include in the sql
   */
  public void execute(String sql, Object... params) {
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
  public <T> Stream<T> select(RowMapper<T> mapper, String sql, Object... params) {
    return withHandle(
        h -> h.select(sql, params).map((rs, ctx) -> mapper.apply(rs)).list().stream());
  }

  /**
   * Functional interface for mapping from a ResultSet to T.
   *
   * @param <T> the result type of the mapping
   */
  public static interface RowMapper<T> {
    /**
     * Run the mapper for the provided ResultSet.
     *
     * @param rs the result set
     * @return the mapped value
     * @throws SQLException if there is an error with the ResultSet
     */
    T apply(ResultSet rs) throws SQLException;
  }
}
