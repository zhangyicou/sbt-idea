import sbt._
import Keys._

object SbtIdeaBuild extends Build {

  lazy val sbtIdea = Project("sbt-idea", file("."), settings = mainSettings)


  lazy val mainSettings: Seq[Project.Setting[_]] = Defaults.defaultSettings ++ ScriptedPlugin.scriptedSettings ++ Seq(
    sbtPlugin := true,
    organization := "com.typesafe.sbtidea",
    name := "sbt-idea",
    version := "1.1.1",
    publishMavenStyle := false,
    publishTo <<= (version) { version: String =>
      val typesafeIvyReleases = Resolver.url("Typesafe Ivy Releases Repository", url("https://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns) 
      val typesafeIvySnapshot = Resolver.url("Typesafe Ivy Snapshots Repository", url("https://typesafe.artifactoryonline.com/typesafe/ivy-snapshots/"))(Resolver.ivyStylePatterns) 
      val repo =  if (version.trim.endsWith("SNAPSHOT")) typesafeIvySnapshot
                          else typesafeIvyReleases
      Some(repo)
    },
    publishArtifact in Test := false,
    pomIncludeRepository := (_ => false),
    pomExtra := extraPom,
    resolvers += Classpaths.typesafeSnapshots,
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    libraryDependencies ++= scriptedTestHelperDependencies
  )

  private def scriptedTestHelperDependencies = Seq(
    "commons-io" % "commons-io" % "2.0.1"
  )

  def extraPom = (
    <url>http://your.project.url</url>
    <licenses>
      <license>
        <name>BSD-style</name>
        <url>http://www.opensource.org/licenses/BSD-3-Clause</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:mpeltonen/sbt-idea.git</url>
      <connection>scm:git:git@github.com:mpeltonen/sbt-idea.git</connection>
    </scm>
    <developers>
      <developer>
      <id>mpeltonen</id>
      <name>Mikko Peltonen</name>
      <url>http://github.com/mpeltonen</url>
    </developer>
  </developers>)
}
