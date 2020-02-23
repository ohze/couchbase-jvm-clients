import sbt.{Def, _}
import Keys._
import dotty.tools.sbtplugin.DottyPlugin.autoImport._

object Dependencies {
  object V {
    val scala3         = "0.22.0-RC1" // dotty
    val scala213       = "2.13.1"
    val scala: String  = scala3 // default for scalaVersion SettingKey
    val crossScala     = Seq(scala3, scala213, "2.12.10")
    val reactor        = "3.3.2.RELEASE"
    val slf4j          = "1.7.30"
    val netty          = "4.1.45.Final"
    val nettyBoringssl = "2.0.28.Final"
    val jctools        = "2.1.2" // 3.0.0
    val jackson        = "2.10.1" // 2.10.2
    val snappy         = "0.4"
    val latencyutils   = "2.0.3"
    val log4jSlf4j     = "2.13.0"
    // jupiter-interface's version should be same as sbt-jupiter-interface's version in project/plugins.sbt
    val jupiterInterface = "0.8.3"
    val junit            = "5.6.0"
    val mockito          = "3.2.4"
    val assertj          = "3.14.0"
    val testcontainers   = "1.12.4"
    val CouchbaseMock    = "73e493d259"
    val awaitility       = "4.0.1"
    val opentracing      = "0.33.0"
    val opentelemetry    = "0.2.0"
    val scalaCollCompat  = "2.1.4"
    // val jsoniter         = "0.9.23"
    val jsoniterScala    = "2.1.7"
    val json4s           = "3.6.7"
    val jawn             = "1.0.0"
    val upickle          = "0.9.9"
    val circe            = "0.13.0"
    val playJson         = "2.8.1"
    val reactorScala     = "0.5.1"
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
  val jawnAst = "org.typelevel" %% "jawn-ast" % V.jawn
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

  /** @note
    *  + In scala 2 version, upickle is `% Optional` in scala-implicits but not in scala-client
    *  + In scala 3 version, upickle is Optional in both scala-implicits and scala-client */
  val upickle = "com.lihaoyi" %% "upickle" % V.upickle

  private def scalaModuleCommonDeps = Def.setting {
    val sv = scalaVersion.value

    // Only need scala-collection-compat for scala < 2.13
    val compat = CrossVersion.partialVersion(sv) match {
      case Some((2, v)) if v < 13 => Seq(scalaCollCompat)
      case _                      => Nil
    }

    val optionalDeps = Seq(
      circe("core"),
      circe("generic"),
      // we don't need circe-parser which is depends on circe-jawn
      circe("jawn")
    ) ++ Seq(
      //        circe("parser") intransitive (),
      json4s("native"),
      json4s("jackson"),
      jawnAst,
      playJson
    ).map(_.withDottyCompat(sv))

    compat ++ optionalDeps.map(_ % Optional)
  }

  def scalaImplicitsDeps = Def.setting {
    val sv = scalaVersion.value

    scalaModuleCommonDeps.value ++ Seq(
      // jsoniterScala("core"),
      upickle % Optional
    ).map(_.withDottyCompat(sv))
  }

  def scalaClientDeps = Def.setting {
    val sv            = scalaVersion.value
    val upickleConfig = if (isDotty.value) Optional else Compile

    scalaModuleCommonDeps.value ++ Seq(
      jacksonDatabind % Test
    ) ++ Seq(
      reactorScala,
      // This dependency is Optional because users only need this if they use
      // com.couchbase.client.scala.implicits.Codec.codec[T]
      // `intransitive` so that `scala-client` don't depends on scala-compiler
      jsoniterScala("macros") % Optional intransitive (),
      // Note: Only used in com.couchbase.client.scala.manager.analytics.AnalyticsDataset
      jsoniterScala("core"),
      // jsoniter, // don't need this. jsoniter-scala is not depended on jsoniter
      scalaJava8Compat,
      scalacheck % Test,
      upickle    % upickleConfig,
      // note: In pom.xml, some deps are declared both `% Optional` % `% Test`
      //, ex: json4s-jackson, jawn-ast, jackson-module-scala, circe-core, play-json
      // which caused maven warnings.
      // We re-declare here in sbt as `% Optional` only
      jacksonScala % Optional
    ).map(_.withDottyCompat(sv))
  }
}
