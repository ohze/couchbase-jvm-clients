### Couchbase JVM Clients
# For scala-3 (`dotty`)

This repository contains the third generation of the Couchbase SDKs on the JVM ("3.0").

## Diff against the [official sdks](https://github.com/couchbase/couchbase-jvm-clients)
+ Available for scala 3 (dotty)
+ Instead of `"com.couchbase.client" %% "scala-client"`, you need `"com.sandinh" %% "couchbase-scala-client"`
+ Internal changes:
  - Don't shade `scala-implicits` module into `scala-client`'s .jar when publish
  - Split `scala-implicits` into `scala-implicits` & `scala-macro`.
    `scala-macro` contains only 1 public class `com.couchbase.client.scala.implicits.Codec`
  - `Codec` method `def codec[T]: Codec[T]`:
    - In scala 2: Same as the official `scala-client` lib: Use `jsoniter_scala` + scala 2's macro to auto generate the `Codec[T]`
    - In dotty: Generate the codec based on an implicit `circe.Codec`:
      `def codec[T: circe.Decoder : circe.Encoder]: Codec[T] = new CirceBasedCodec[T]`
+ `scala-client`'s dependency change:
  - `com.couchbase.client:scala-client`:
    - For dotty: NA
    - For scala 2.x: Depend on `jsoniter_scala-core`, `jsoniter_scala-macros`, `scala-refect`, `upickle`
  - `com.sandinh:couchbase-scala-client`:
    - For dotty:
      - Depend on `circe-jawn` & `circe-generic`.
        Note: dotty version of `circe-generic` don't depend on `shapeless`
      - Do NOT depend on `jsoniter_scala-core`, `jsoniter_scala-macros`, `scala-refect`, `upickle`
    - For scala 2.x:
      - Depend on `jsoniter_scala-core`, `jsoniter_scala-macros`, `scala-refect`
      - Do not depend on `upickle`
      - Do NOT publish to maven central. Pls use the official one

## Overview

This repository contains the following projects:

 - `core-io`: the core library for all language bindings
 - `java-client`: the Java language binding
 - `scala-client`: the Scala language binding

You'll also find utility libraries and integration components at the toplevel (i.e for tracing).
 
Documentation is now available for [Java](https://docs.couchbase.com/java-sdk/3.0/hello-world/start-using-sdk.html) 
and [Scala](https://docs.couchbase.com/scala-sdk/1.0/start-using-sdk.html)
 
## Usage

Stable [releases are pushed](https://search.maven.org/search?q=g:com.sandinh%20couchbase-) to maven central.

For Java:

```xml
<dependencies>
    <dependency>
        <groupId>com.sandinh</groupId>
        <artifactId>couchbase-java-client</artifactId>
        <version>3.0.1</version>
    </dependency>
</dependencies>
```

For Scala:

```xml
<dependencies>
    <dependency>
        <groupId>com.sandinh</groupId>
        <artifactId>couchbase-scala-client_2.12</artifactId>
        <version>1.0.1</version>
    </dependency>
</dependencies>
```
or if you use sbt:
```sbt
libraryDependencies += "com.sandinh" %% "couchbase-scala-client" % "1.0.1"
```

## Build / Test
You can always also just build it from source:
+ Clone git repository
```
$ git clone https://github.com/couchbase/couchbase-jvm-clients.git
$ cd couchbase-jvm-clients
$ ./sbt
```

+ [Run sbt](https://www.scala-sbt.org/1.x/docs/Running.html)
```shell script
cd couchbase-jvm-clients
./sbt
```
(We bundled [sbt-extra](https://github.com/paulp/sbt-extras) script at [./sbt]
 so you don't need to [install sbt](https://www.scala-sbt.org/1.x/docs/Setup.html).
 Of course if you have installed sbt, you can run `sbt` command instead of `./sbt`)
We use sbt for all couchbase-jvm-clients' sub projects including the java-only projects such as `core-io`, `java-client`.

+ In sbt shell, run:
    - `compile` or `test` or `it:test` to compile/ test/ integration test all sub project.
    - `core-io / compile` to compile only `core-io` sub-project.
    - `projects` to list all sub-projects
    -  `project scala-client` to switch to `scala-client` sub-project. All subsequence command will be run in this active project.
    - `+test` to test for all versions of Scala defined in the crossScalaVersions setting of the active project.
       For example scala-client / crossScalaVersions includes 2.13.1, 2.12.10, 2.11.12.
    - `testOnly com.couchbase.client.core.CoreTest` to test only the test in CoreTest test spec.
       (sbt support tab completion, so you can `testOnly <tab>` to see all test spec names)
    - `clean; compile` to clean and then compile (`cmd1; cmd2` is similar to run `cmd1`, and then run `cmd2`)
    - `publishLocal` or `publishM2` to publish to ivy or maven local repository.
    - `test:checkstyle` or `test:scalafmt` to [checkstyle](http://checkstyle.sourceforge.net/)/ [scalafmt](https://scalameta.org/scalafmt/) your java/ scala test files.
We recommend you to skim through the [Getting Started with sbt](https://www.scala-sbt.org/1.x/docs/Getting-Started.html) document.

### Branches & Release Trains

Since this monorepo houses different versions of different artifacts, release train names have been chosen
to identify a collection of releases that belong to the same train.

These trains are named after historic computers for your delight.

Tags in each branch are named `branchname-ga` for the initial GA release, and then subsequently `branchname-sr-n` for
each service release. See the tag information for specifics of what's in there.

 - [Colossus](https://en.wikipedia.org/wiki/Colossus_computer) (Initial Release 2020-01-10)

| Release Train | Java-Client | Scala-Client | Core-Io | Tracing-Opentelemetry | Tracing-Opentracing |
| ------------- | ----------- | ------------ | ------- | --------------------- | ------------------- |
| colossus      | 3.0.x       | 1.0.x        | 2.0.x   | 0.2.x                 | 0.2.x               |

### Testing Infos

To cover all tests, the suite needs to be run against the following topologies, but by default it
runs against the mock. Recommended topologies:

 - 1 node, no replica
 - 2 nodes, 1 replica
 - 2 nodes, 2 replicas
 
## IDE Configuration

### IntelliJ
Scala code is automatically formatted on compile with the tool `scalafmt`.  To make IntelliJ use the same settings:

Editor -> Code Style -> Scala, change formatter to scalafmt
and check [Reformat on file save](https://scalameta.org/scalafmt/docs/installation.html#format-on-save)

(`scalafmtAll` can be used from sbt shell to force reformat. See also other [scalafmt* sbt tasks](https://scalameta.org/scalafmt/docs/installation.html#task-keys))
