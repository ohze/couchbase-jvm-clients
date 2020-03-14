import Dependencies._
import Helpers.{IntegrationTest, _}
import sbtassembly.shadeplugin.ResourceTransformer.{Discard, Rename}
import sbtassembly.shadeplugin.ShadePlugin.autoImport.shadeKeys.artifactIdSuffix
import sbtassembly.shadeplugin.ShadePluginUtils._

Global / bloopExportJarClassifiers := Some(Set("sources"))

// TODO remove
ThisBuild / updateOptions := updateOptions.value.withLatestSnapshots(false)
ThisBuild / offline := true
ThisBuild / Test / publishArtifact := false
ThisBuild / organization := "com.sandinh"
// those settings should be set at the end of project definition
def ensurePrefix(prefix: String)(s: String): String = if (s.startsWith(prefix)) s else prefix + s
val overridePublishSettings = Seq(
  name ~= ensurePrefix("couchbase-"),
  version := {
    val v      = version.value
    val sha    = git.gitHeadCommit.value.get
    val suffix = if (git.gitUncommittedChanges.value) "-SNAPSHOT" else ""
    v.stripSuffix("SNAPSHOT") + sha.take(7) + suffix
  },
  developers += Developer(
    "thanhbv",
    "Bui Viet Thanh",
    "thanhbv@sandinh.net",
    url("https://sandinh.com")
  )
)

ThisBuild / trackInternalDependencies := TrackLevel.TrackIfMissing

lazy val checkstyleSettings = Seq(
  checkstyleSeverityLevel := Some(CheckstyleSeverityLevel.Error),
  checkstyleConfigLocation := (ThisBuild / baseDirectory).value / "config" / "checkstyle" / "checkstyle-basic.xml",
  checkstyleHeaderFile := (ThisBuild / baseDirectory).value / "config" / "checkstyle" / "checkstyle-header.txt"
)
lazy val commonSettings = checkstyleSettings ++ Seq(
  Compile / doc / sources := Nil, // TODO remove
  javacOptions ++= Seq("-encoding", "UTF-8"),
  Compile / compile / javacOptions ++= (javaVersion match {
    case v if v >= 9 => Seq("--release", "8")
    case _           => Seq("-source", "1.8", "-target", "1.8")
  }),
  // format: off
  Compile / doc / javacOptions ++= Seq(
    "-source", "1.8",
    "-Xdoclint:none",
    "-quiet",
    "-stylesheetfile", ((ThisBuild / baseDirectory).value / "config" / "javadoc" / "style.css").getPath,
    "-link", "https://projectreactor.io/docs/core/release/api/"
  ),
  // format: on
  resolvers += Resolver.jcenterRepo,
  Test / parallelExecution := false,
  IntegrationTest / parallelExecution := false
)

lazy val javaModuleSettings = commonSettings ++ Seq(
  autoScalaLibrary := false, // exclude scala-library from dependencies
  crossPaths := false        // drop off Scala suffix from artifact names and publish path
)

lazy val scalaModuleSettings = commonSettings ++ commonScalaSourceDirsSetting ++ Seq(
  logLevel := Level.Error, // TODO remove this
  version := "1.1.0-SNAPSHOT",
  scalaVersion := V.scala,
  crossScalaVersions := V.crossScala,
  scalacOptions ++= Seq("-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation", "-feature"),
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    // Required for Scala 2.11 build, this enables support for Java SAM.
    // This is on by default in Scala 2.12+
    case Some((2, 11)) => Seq("-Xexperimental")
    case _             => Nil
  })
)

val commonAssemblySettings = inTask(assembly)(
  Seq(
    test := {},
    assemblyOption ~= {
      _.copy(includeScala = false)
    },
    assemblyJarName := s"${name.value}${artifactIdSuffix.value}-${version.value}.jar"
  )
) ++ Seq(
  // change, ex `scala-client_2.13-1.1.0.jar` to `original-scala-client_2.13-1.1.0.jar` in the same parent directory.
  // This is to avoid conflict (same name) with `assembly / assemblyOutputPath`.
  // We can just change assemblyJarName in commonAssemblySettings
  // but change here make the paths exactly same as when using maven-shade-plugin :)
  Compile / packageBin / artifactPath := {
    val p = (Compile / packageBin / artifactPath).value.toPath
    p.resolveSibling(s"original-${p.getFileName}").toFile
  }
)

val coreIoDepsAssemblySettings = commonAssemblySettings ++ inTask(assembly)(
  Seq(
    assemblyShadeRules := Seq(
      "io.netty",
      "com.fasterxml.jackson",
      "org.iq80.snappy",
      "org.LatencyUtils",
      "org.HdrHistogram",
      "org.jctools"
    ).map { p =>
      ShadeRule.rename(s"$p.**" -> s"com.couchbase.client.core.deps.@0").inAll
    },
    shadeResourceTransformers += Rename(
      "libnetty_tcnative_linux_x86_64.so"   -> "libcom_couchbase_client_core_deps_netty_tcnative_linux_x86_64.so",
      "libnetty_tcnative_osx_x86_64.jnilib" -> "libcom_couchbase_client_core_deps_netty_tcnative_osx_x86_64.jnilib",
      "netty_tcnative_windows_x86_64.dll"   -> "com_couchbase_client_core_deps_netty_tcnative_windows_x86_64.dll"
    ).inDir("META-INF/native"),
    assemblyMergeStrategy := {
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
      // https://stackoverflow.com/a/55557287/457612
      case "module-info.class" => MergeStrategy.discard
      case x                   => assemblyMergeStrategy.value(x)
    }
  )
)

lazy val `core-io-deps` = project
  .disablePlugins(ScalafmtPlugin, CheckstylePlugin)
  .settings(javaModuleSettings ++ coreIoDepsAssemblySettings)
  .settings(
    description := "Couchbase JVM Core IO Dependencies shaded for sanity's sake",
    version := "1.1.0-SNAPSHOT",
    libraryDependencies := coreIoShadedDeps,
    publish / skip := true,
    exportToInternal := TrackLevel.TrackIfMissing,
    // https://www.scala-sbt.org/1.x/docs/Howto-Classpaths.html#Use+packaged+jars+on+classpaths+instead+of+class+directories
    // exportJars so in dependent projects, we can compute assemblyExcludedJars based on this Project / artifactPath
    exportJars := true
  )

val coreIoAssemblySettings = commonAssemblySettings ++ inTask(assembly)(
  Seq(
    // shade only core-io-deps and selfJar (core-io)
    assemblyExcludedJars := {
      val cp           = fullClasspath.value
      val depJar       = (`core-io-deps` / assembly / assemblyOutputPath).value
      val selfJar      = (Compile / packageBin / artifactPath).value
      val includedJars = Set(depJar, selfJar)
      cp.filterNot { entry =>
        includedJars contains entry.data
      }
    },
    shadeResourceTransformers += Discard(
      "com.fasterxml.jackson.core.JsonFactory",
      "com.fasterxml.jackson.core.ObjectCodec",
      "com.fasterxml.jackson.databind.Module"
    ).inDir("META-INF/services")
  )
)

lazy val `core-io` = project
  .settings(javaModuleSettings ++ coreIoAssemblySettings: _*)
  .enableAssemblyPublish()
  .settings(
    description := "The official Couchbase JVM Core IO Library",
    version := "2.1.0-SNAPSHOT",
    libraryDependencies ++= coreIoDeps,
    exportJars := true,
    Compile / unmanagedJars += Def.taskDyn {
      val jar = (`core-io-deps` / assembly / assemblyOutputPath).value
      if (jar.exists()) Def.task { jar }
      else `core-io-deps` / assembly
    }.value
  )
  .itConfig()
  .dependsOn(`test-utils` % Test)
  .settings(overridePublishSettings: _*)

lazy val `java-client` = project
  .disablePlugins(AssemblyPlugin, ScalafmtPlugin)
  .settings(javaModuleSettings: _*)
  .settings(
    publish / skip := true,
    description := "The official Couchbase Java SDK",
    version := "3.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      jacksonDatabind % Optional
    )
  )
  .itConfig()
  .dependsOn(`core-io`, `test-utils` % Test)

lazy val `java-examples` = project
  .disablePlugins(AssemblyPlugin, ScalafmtPlugin)
  .settings(javaModuleSettings: _*)
  .settings(
    description := "Examples for the Couchbase Java SDK",
    version := "1.1.0-SNAPSHOT",
    libraryDependencies += log4jSlf4j,
    publish / skip := true
  )
  .dependsOn(`java-client`)

lazy val `test-utils` = project
  .disablePlugins(AssemblyPlugin, ScalafmtPlugin)
  .settings(javaModuleSettings: _*)
  .settings(
    description := "Couchbase (Integration) Test Utilities",
    version := "1.1.0-SNAPSHOT",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= testUtilsDeps,
    publish / skip := true
  )

lazy val `tracing-opentracing` = project
  .disablePlugins(AssemblyPlugin, ScalafmtPlugin)
  .settings(javaModuleSettings: _*)
  .settings(
    publish / skip := true,
    description := "Provides interoperability with OpenTracing",
    version := "0.3.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      opentracing("api"),
      opentracing("mock") % Test
    )
  )
  .itConfig()
  .dependsOn(`core-io`, `java-client` % Test, `test-utils` % Test)

lazy val `tracing-opentelemetry` = project
  .disablePlugins(AssemblyPlugin)
  .settings(javaModuleSettings: _*)
  .settings(
    publish / skip := true,
    description := "Provides interoperability with OpenTelemetry",
    version := "0.3.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      opentelemetry("api"),
      opentelemetry("sdk")                % Test,
      opentelemetry("exporters-inmemory") % Test
    ),
    // tracing-opentelemetry need scala only when test
    scalaVersion := V.scala,
    scalacOptions ++= Seq("-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation", "-feature"),
    Test / unmanagedSourceDirectories := Seq((Test / javaSource).value, (Test / scalaSource).value)
  )
  .itConfig()
  .dependsOn(`core-io`, `java-client` % Test, `test-utils` % Test, `scala-client` % "test->it")

lazy val `scala-implicits` = project
  .disablePlugins(AssemblyPlugin)
  .settings(scalaModuleSettings: _*)
  .settings(
    description := "The official Couchbase Scala SDK (Implicits)",
    libraryDependencies ++= scalaImplicitsDeps.value,
    exportJars := true
  )
  .dependsOn(`core-io`, `test-utils` % Test)
  .settings(overridePublishSettings: _*)

lazy val `scala-macro` = project
  .disablePlugins(AssemblyPlugin, CheckstylePlugin)
  .settings(scalaModuleSettings: _*)
  .settings(
    exportJars := true,
    libraryDependencies += {
      // use circe because jsoniter-scala is not dotty ready yet
      if (isDotty.value) circe("jawn")
      else jsoniterScala("macros")
    }
  )
  .settings(munitSettings: _*)
  .dependsOn(`scala-implicits`)
  .settings(overridePublishSettings: _*)

val scalaClientAssemblySettings = commonAssemblySettings ++ inTask(assembly)(
  Seq(
    assemblyShadeRules := Seq(
      "scala.compat.java8",
      "scala.concurrent.java8"
    ).map { p =>
      ShadeRule.rename(s"$p.**" -> s"com.couchbase.client.scala.deps.@0").inAll
    },
    // shade scala-java8-compat, and selfJar (scala-client)
    assemblyExcludedJars := {
      val cp      = fullClasspath.value
      val selfJar = (Compile / packageBin / artifactPath).value
      val sv      = scalaBinaryVersion.value
      cp.filterNot { entry =>
        entry.isModule(scalaJava8Compat, sv) ||
        entry.data == selfJar
      }
    },
    shadeResourceTransformers += Discard(
      "com.fasterxml.jackson.core.JsonFactory"
    ).inDir("META-INF/services")
  )
)

lazy val `scala-client` = project
  .settings(scalaModuleSettings ++ scalaClientAssemblySettings: _*)
  .enableAssemblyPublish()
  .settings(
    description := "The official Couchbase Scala SDK",
    libraryDependencies ++= scalaClientDeps.value
  )
  .itConfig()
  .dependsOn(`core-io`, `scala-implicits`, `test-utils` % Test)
  // TODO scala-client on scala 2.x do NOT depends on scala-compiler but with this setting `.dependsOn(`scala-macro`)`
  // , we make scala-client <- jsoniter-scala-macros <- scala-compiler
  // Note: scala-client IS depended on scala-reflect but this is not declared in the official pom.xml (bug)
  .dependsOn(`scala-macro`)
  .removePomDependsOn(scalaJava8Compat)
  .settings(overridePublishSettings: _*)

lazy val `scala-examples` = project
  .disablePlugins(AssemblyPlugin, CheckstylePlugin)
  .settings(scalaModuleSettings: _*)
  .settings(
    description := "Examples for the Couchbase Scala SDK",
    // resolvers += "oss.sonatype.org-snapshot" at "https://oss.jfrog.org/artifactory/oss-snapshot-local",
    libraryDependencies ++= Seq(
      opentelemetry("sdk"),
      opentelemetry("exporters-inmemory"),
      log4jSlf4j
    ),
    publish / skip := true
  )
  .dependsOn(`scala-client`, `tracing-opentelemetry`)

// This is used to filter projects in QUICK_TEST_MODE
// ex, run `sbt -DSBT_PL='!scala-client,!scala-implicits'` to filter out scala-client and scala-implicits
// @see Jenkinsfile
val moduleFilterOut: ProjectReference => Boolean = {
  val pl = System
    .getProperty("SBT_PL", "")
    .split(',')
    .map(_.trim.stripPrefix("!").trim)

  p => pl.contains(p.asInstanceOf[LocalProject].project)
}

lazy val aggregated = Seq[ProjectReference](
  `core-io-deps`,
  `core-io`,
  `java-client`,
  `java-examples`,
  `scala-implicits`,
  `scala-macro`,
  `scala-client`,
  `scala-examples`,
  `test-utils`,
  `tracing-opentracing`,
  `tracing-opentelemetry`
).filterNot(moduleFilterOut)

lazy val root = (project in file("."))
  .disablePlugins(AssemblyPlugin, ScalafmtPlugin, CheckstylePlugin)
  .settings(
    name := "couchbase-jvm-clients",
    description := "Couchbase JVM Client Parent",
    publish / skip := true
  )
  .aggregate(aggregated: _*)
  .settings(overridePublishSettings: _*)
