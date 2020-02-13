// See https://github.com/xerial/sbt-sonatype#sbt-sonatype-plugin
sonatypeProfileName := "com.couchbase.client"
publishTo := sonatypePublishToBundle.value
publishMavenStyle := true

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

val home = url("https://couchbase.com")
homepage := Some(home)
organizationName := "Couchbase, Inc."
organizationHomepage := homepage.value
scmInfo := Some(
  ScmInfo(
    url("https://github.com/couchbase/couchbase-jvm-clients"),
    "scm:git:git://github.com/couchbase/couchbase-jvm-clients"
  )
)

developers := List(
  Developer("daschl", "Michael Nitschinger", "michael.nitschinger@couchbase.com", home),
  Developer("programmatix", "Graham Pople", "graham.pople@couchbase.com", home),
  Developer("dnault", "David Nault", "david.nault@couchbase.com", home),
  Developer("avsej", "Sergey Avseyev", "sergey@couchbase.com", home)
)
