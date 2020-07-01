# SomeDB


![Build](https://github.com/princesslana/somedb/workflows/Build/badge.svg?branch=master)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=princesslana_somedb&metric=sqale_index)](https://sonarcloud.io/dashboard?id=princesslana_somedb)
[![Discord](https://img.shields.io/discord/417389758470422538)](https://discord.gg/3aTVQtz)

An opinionated library that provides a simple way to run an embedded, file backed database.

It uses:

* [Flyway](https://flywaydb.org/) for schema migrations,
* [Apache Derby](http://db.apache.org/derby/) for the database, and
* [JDBI](https://jdbi.org/) for executing SQL.

## Installing

Currently SomeDB is available via [JitPack](https://jitpack.io/#princesslana/somedb)

To use with Maven add the JitPack repository to your pom.xml

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

Then add the dependency

```xml

<dependency>
  <groupId>com.github.princesslana</groupId>
  <artifactId>somedb</artifactId>
  <version>master-SNAPSHOT</version>
</dependency>
```

Instructions for Gradle can be found on [JitPack](https://jitpack.io/#princesslana/somedb)

## Getting Started

The following steps outline the quickest way to get a database setup within your application.

First, ensure that SomeDB is [Installed](#installing) in your project.

Next, we need to define how our database schema.
With Flyway we place our migrations on the classpath under the path `db/migration`.
If you are using Gradle or Maven this means they should be in
the `src/main/resources/db/migration` folder.

Flyway has a [naming scheme](https://flywaydb.org/documentation/migrations#naming) that should
be followed so that our migrations can be discovered.
For the example here our file could be called `V1__Create_favorite_color_table.sql`.
In this file we can place SQL that will be used to setup our database.

```sql
CREATE TABLE favorite_color (
  username VARCHAR(64),
  color    VARCHAR(64)
);
```

Now we are ready to use our database in our code.
As an optional (but recommended step) we can initialize our database.
If we don't initialize our database explicitly, it will be initialized on first use
using a default name.
We can use `TheDB.initialize` to perform this step.

```java
import com.github.princesslana.somedb.TheDB;

public class MyApp {
  public static void main(String[] args) {
    TheDB.initialize("my_cool_app");

    // ... more code ...
  }
}
```

We can insert data into our database by using the `execute` method.
Parameterized queries are supported using `?` as the placeholder.

```java
String name = ...
String color = ...

TheDB.execute("insert into favorite_color(username, color) values (?, ?)", name, color);
```

Querys can by run with the `select` method.
The `select` method must be provided with a way to convert a row from the `ResultSet`
into an object of our choosing.
Results are provided as Stream, meaning we are able to use the full range of Stream
functionality to, for example, get the first item or collect the results into a List.
As with `execute`, parameterized queries are supported using `?` as the placeholder.

```java
String name = ...

Stream<FavoriteColor> colors = TheDB.execute(
  rs -> new FavoriteColor(rs.getString("username"), rs.getString("color")),
  "select username, color from favorite_color where username = ?",
  name);
```

## About Singletons

To say that the Singleton design pattern is controversial would be an understatement.
SomeDB lets you choose whether you want to Singleton or not.
If you wish to Singleton you can use the `TheDB` class, which provides static methods
to execute SQL and access the database.
If you do not wish to Singleton you can create instances of the `OneDB` class.
Both classes provide the same functionality and a pretty much identical interface.

## Contact

For futher help, queries, or to contribute the best way to reach out is the
[Discord Projects Hub](https://discord.gg/3aTVQtz).

