import net.aichler.jupiter.sbt.JupiterPlugin
import sbt.Keys._
import sbt._
import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings
import com.etsy.sbt.checkstyle.CheckstylePlugin.autoImport.checkstyleSettings

object Helpers {
  def javaVersion: Int = System.getProperty("java.specification.version") match {
    case v if v.startsWith("1.") => v.substring(2).toInt
    case v                       => v.toInt
  }

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
          sourceDirectory := baseDirectory.value / "src" / "integrationTest"
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
