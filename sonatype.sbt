// See https://github.com/xerial/sbt-sonatype#sbt-sonatype-plugin
//ThisBuild / organization := "com.couchbase.client" // sonatypeProfileName := organization.value
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / publishMavenStyle := true

ThisBuild / licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

val home = url("https://couchbase.com")
ThisBuild / homepage := Some(home)
ThisBuild / organizationName := "Couchbase, Inc."
ThisBuild / organizationHomepage := homepage.value
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/couchbase/couchbase-jvm-clients"),
    "scm:git:git://github.com/couchbase/couchbase-jvm-clients"
  )
)
ThisBuild / developers := List(
  Developer("daschl", "Michael Nitschinger", "michael.nitschinger@couchbase.com", home),
  Developer("programmatix", "Graham Pople", "graham.pople@couchbase.com", home),
  Developer("dnault", "David Nault", "david.nault@couchbase.com", home),
  Developer("avsej", "Sergey Avseyev", "sergey@couchbase.com", home)
)
