import sbt._
import Keys._

object Dependencies {
  object V {
    val crossScalaVersions = Seq("2.13.1", "2.12.10", "2.11.12")
    val scalaVersion       = "2.13.1"
    val reactor            = "3.3.1.RELEASE"
    val slf4j              = "1.7.30"
    val netty              = "4.1.45.Final"
    val nettyBoringssl     = "2.0.28.Final"
    val jctools            = "2.1.2" // 3.0.0
    val jackson            = "2.10.1" // 2.10.2
    val snappy             = "0.4"
    val latencyutils       = "2.0.3"
    val log4jSlf4j         = "2.13.0"
    // jupiter-interface's version should be same as sbt-jupiter-interface's version in project/plugins.sbt
    val jupiterInterface = "0.8.3"
    val junit            = "5.5.2"
    val mockito          = "3.2.4"
    val assertj          = "3.14.0"
    val testcontainers   = "1.12.4"
    val CouchbaseMock    = "73e493d259"
    val awaitility       = "4.0.1"
    val opentracing      = "0.33.0"
    val opentelemetry    = "0.2.0"
    val scalaCollCompat  = "2.1.3"
    val jsoniter         = "0.9.23"
    val jsoniterScala    = "2.1.6"
    val json4s           = "3.6.7"
    val jawnAst          = "0.14.3"
    val upickle          = "0.9.8"
    val circe            = "0.12.3"
    val playJson         = "2.8.1"
    val reactorScala     = "0.5.0"
    val scalaJava8Compat = "0.9.0"
    val scalacheck       = "1.14.0"
  }

  def reactor(name: String): ModuleID =
    "io.projectreactor" % s"reactor-$name" % V.reactor
  def netty(name: String, version: String = V.netty): ModuleID =
    "io.netty" % s"netty-$name" % version
  def jacksonModule(name: String): ModuleID =
    "com.fasterxml.jackson.module" % s"jackson-module-$name" % V.jackson

  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % V.jackson
  val jacksonScala    = jacksonModule("scala") cross CrossVersion.binary

  val slf4jApi     = "org.slf4j"                % "slf4j-api"        % V.slf4j
  val jctools      = "org.jctools"              % "jctools-core"     % V.jctools
  val snappy       = "org.iq80.snappy"          % "snappy"           % V.snappy
  val latencyutils = "org.latencyutils"         % "LatencyUtils"     % V.latencyutils
  val log4jSlf4j   = "org.apache.logging.log4j" % "log4j-slf4j-impl" % V.log4jSlf4j

  // cannot use `% jupiterVersion.value` here
  val jupiterInterface = "net.aichler" % "jupiter-interface" % V.jupiterInterface
  def junitJupiter(name: String): ModuleID =
    "org.junit.jupiter" % s"junit-jupiter-$name" % V.junit

  val mockitoCore    = "org.mockito"          % "mockito-core"   % V.mockito
  val assertjCore    = "org.assertj"          % "assertj-core"   % V.assertj
  val testcontainers = "org.testcontainers"   % "testcontainers" % V.testcontainers
  val CouchbaseMock  = "com.github.Couchbase" % "CouchbaseMock"  % V.CouchbaseMock
  val awaitility     = "org.awaitility"       % "awaitility"     % V.awaitility

  def opentracing(name: String): ModuleID =
    "io.opentracing" % s"opentracing-$name" % V.opentracing
  def opentelemetry(name: String): ModuleID =
    "io.opentelemetry" % s"opentelemetry-$name" % V.opentelemetry

  val scalaCollCompat = "org.scala-lang.modules" %% "scala-collection-compat" % V.scalaCollCompat
  // don't need this. jsoniter-scala is not depended on jsoniter
  // val jsoniter = "com.jsoniter"  % "jsoniter"  % V.jsoniter
  val jawnAst = "org.typelevel" %% "jawn-ast" % V.jawnAst
  def jsoniterScala(name: String): ModuleID =
    "com.github.plokhotnyuk.jsoniter-scala" %% s"jsoniter-scala-$name" % V.jsoniterScala
  def json4s(name: String): ModuleID = "org.json4s"             %% s"json4s-$name"            % V.json4s
  def circe(name: String): ModuleID  = "io.circe"               %% s"circe-$name"             % V.circe
  val playJson                       = "com.typesafe.play"      %% "play-json"                % V.playJson
  val reactorScala                   = "io.projectreactor"      %% "reactor-scala-extensions" % V.reactorScala
  val scalaJava8Compat               = "org.scala-lang.modules" %% "scala-java8-compat"       % V.scalaJava8Compat
  val scalacheck                     = "org.scalacheck"         %% "scalacheck"               % V.scalacheck

  val coreIoShadedDeps = Seq(
    netty("codec-http"),
    netty("transport-native-kqueue"),
    netty("transport-native-epoll"),
    netty("tcnative-boringssl-static", V.nettyBoringssl),
    jctools,
    jacksonDatabind,
    jacksonModule("afterburner"),
    snappy,
    latencyutils
  )
  val coreIoDeps = Seq(
    reactor("core"),
    slf4jApi        % Optional,
    reactor("test") % Test
  )

  val rootTestDeps: Seq[ModuleID] = Seq(
    junitJupiter("api"),
    junitJupiter("params"),
    junitJupiter("engine"),
    mockitoCore
  ) :+ jupiterInterface

  val testUtilsDeps: Seq[ModuleID] = rootTestDeps ++ Seq(
    // junitJupiter("api"),
    assertjCore,
    testcontainers,
    CouchbaseMock,
    jacksonDatabind,
    awaitility,
    log4jSlf4j
  )

  /** upickle is `% Optional` in scala-implicits but not in scala-client */
  private def upickle = Def.setting {
    val v = CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => "0.7.4"
      case _             => V.upickle
    }
    "com.lihaoyi" %% "upickle" % v
  }

  private def scalaModuleOptionalDeps = Def.setting {
    {
      Seq(
        // jsoniter, // don't need this. jsoniter-scala is not depended on jsoniter
        json4s("native"),
        json4s("jackson"),
        jawnAst
        // upickle(scalaVersion)
      ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11)) =>
          Seq(
            circe("core") withRevision "0.11.2",
            circe("generic") withRevision "0.11.2",
            circe("parser") withRevision "0.11.2",
            playJson withRevision "2.7.4"
          )
        case _ =>
          Seq(
            circe("core"),
            circe("generic"),
            circe("parser"),
            playJson
          )
      })
    }.map(_ % Optional)
  }

  def scalaImplicitsDeps = Def.setting {
    Seq(
      scalaCollCompat,
      jsoniterScala("core"),
      jsoniterScala("macros") % Provided,
      upickle.value           % Optional
    ) ++ scalaModuleOptionalDeps.value
  }

  def scalaClientDeps = Def.setting {
    Seq(
      reactorScala,
      jsoniterScala("core"),
      jsoniterScala("macros"), // FIXME should be `% Provided`
      scalaJava8Compat,
      scalaCollCompat,
      scalacheck      % Test,
      jacksonDatabind % Test,
      upickle.value,
      // note: In pom.xml, some deps are declared both `% Optional` % `% Test`
      //, ex: json4s-jackson, jawn-ast, jackson-module-scala, circe-core, play-json
      // which caused maven warnings.
      // We re-declare here in sbt as `% Optional` only
      jacksonScala % Optional
    ) ++ scalaModuleOptionalDeps.value
  }
}
