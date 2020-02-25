import net.aichler.jupiter.sbt.JupiterPlugin
import sbt.Keys._
import sbt.{Def, _}
import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings
import com.etsy.sbt.checkstyle.CheckstylePlugin.autoImport.checkstyleSettings

object Helpers {
  def javaVersion: Int = System.getProperty("java.specification.version") match {
    case v if v.startsWith("1.") => v.substring(2).toInt
    case v                       => v.toInt
  }

  /** Add sourceDirectory / [scala-2, scala-3, scala-2.12, scala-2.13+,..] to unmanagedSourceDirectories
    * This setting don't have a Configuration scope axis
    * So, when use, we need (for `Compile` Configuration): `inConfig(Compile)(Seq(scalaSourceDirsSetting))` */
  lazy val scalaSourceDirsSetting = unmanagedSourceDirectories ++= {
    val sourceDir             = sourceDirectory.value
    val Some((m, n)) = CrossVersion.partialVersion(scalaVersion.value)
    val major                 = if (m == 0) 3 else m

    // https://docs.scala-lang.org/overviews/core/collections-migration-213.html#how-do-i-cross-build-my-project-against-scala-212-and-scala-213
    val coll213 = (major, n) match {
      case (2, n) if n >= 13 => Seq("2.13+")
      case (2, _)            => Seq("2.13-")
      case _                 => Nil
    }

    val all = Seq(major.toString, s"$m.$n") ++ coll213
    all.map(s => sourceDir / s"scala-$s")
  }

  lazy val commonScalaSourceDirsSetting: Seq[Def.Setting[_]] =
    inConfig(Compile)(Seq(scalaSourceDirsSetting)) ++
    inConfig(Test)(Seq(scalaSourceDirsSetting))

  /** Redefine the IntegrationTest Configuration to extend the Test configuration
    * instead of the Runtime Configuration (the default).
    * This will make everything in Test configuration available to IntegrationTest configuration
    * @see [[https://www.scala-sbt.org/1.x/docs/Scopes.html#Scoping+by+the+configuration+axis Scoping by the configuration axis]]
    * @see [[https://stackoverflow.com/q/20624574/457612 How to make “test” classes available in the “it” (integration test) configuration?]] */
  lazy val IntegrationTest = config("it").extend(Test, Optional)

  /** Integration test settings
    * @see [[https://github.com/maichler/sbt-jupiter-interface#integration-testing sbt-jupiter-interface Integration Testing]]
    * @see [[https://scalameta.org/scalafmt/docs/installation.html#enable-integrationtest scalafmt: Enable IntegrationTest]]
    * @see [[https://github.com/giabao/sbt-checkstyle-plugin#integration-tests sbt-checkstyle: Integration tests]] */
  lazy val itSettings: Seq[Setting[_]] = Defaults.itSettings ++
    checkstyleSettings(IntegrationTest) ++
    inConfig(IntegrationTest)(
      scalafmtConfigSettings ++
        JupiterPlugin.scopedSettings ++
        Seq(
          sourceDirectory := baseDirectory.value / "src" / "integrationTest",
          scalaSourceDirsSetting
        )
    )

  /** Extension methods for Attributed[File]
    * @param entry an item in [[sbt.Keys.Classpath]] */
  implicit class AttributedFileOps(val entry: Attributed[File]) extends AnyVal {

    /** Check if `entry` is a Classpath entry from ModuleID `m` */
    def isModule(m: ModuleID, scalaBinaryVersion: String): Boolean = {
      val name = m.name + "_" + scalaBinaryVersion
      entry.get(moduleID.key).exists { x =>
        x.organization == m.organization &&
        x.name == name
      }
    }
  }

  /** Extension methods for sbt.Project */
  implicit class ProjectOps(val p: Project) extends AnyVal {

    /** Config IntegrationTest */
    def itConfig(): Project = p.configs(IntegrationTest).settings(itSettings: _*)
  }
}
