import sbt._
import Keys._

object SbtIdeaBuild extends Build {

  lazy val sbtIdea = Project("sbt-idea", file("."), settings = mainSettings)

  lazy val mainSettings: Seq[Project.Setting[_]] = Defaults.defaultSettings ++ Seq(
    sbtPlugin := true,
    organization := "com.github.mpeltonen",
    definedSbtPlugins := Set("org.sbtidea.SbtIdeaPlugin"),
    name := "sbt-idea",
    version := "1.1.0-M2-TYPESAFE",
    publishMavenStyle := false,
    publishTo <<= (version) { version: String =>
      val typesafeIvyReleases = Resolver.url("Typesafe Ivy Releases Repository", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns) 
      val typesafeIvySnapshot = Resolver.url("Typesafe Ivy Snapshots Repository", url("http://repo.typesafe.com/typesafe/ivy-snapshots/"))(Resolver.ivyStylePatterns) 
      val repo =  if (version.trim.endsWith("SNAPSHOT")) typesafeIvySnapshot
                          else typesafeIvyReleases
      Some(repo)
    },
    resolvers += Classpaths.typesafeSnapshots,
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    libraryDependencies ++= scriptedTestHelperDependencies
  )

  private def scriptedTestHelperDependencies = Seq(
    "commons-io" % "commons-io" % "2.0.1"
  )
}
