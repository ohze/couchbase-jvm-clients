resolvers += Resolver.jcenterRepo

addSbtPlugin("com.eed3si9n" % "sbt-assembly"          % "0.15.0")
addSbtPlugin("com.sandinh"  % "sbt-shade"             % "0.1.3")
addSbtPlugin("net.aichler"  % "sbt-jupiter-interface" % "0.8.3")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.4")
addSbtPlugin("com.jsuereth"   % "sbt-pgp"      % "2.0.1")

addSbtPlugin("org.scalameta" % "sbt-scalafmt"   % "2.4.0")
addSbtPlugin("com.sandinh"   % "sbt-checkstyle" % "3.2.2")

addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.4.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
