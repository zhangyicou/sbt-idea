resolvers ++= Seq(
  Classpaths.typesafeSnapshots
)

libraryDependencies <+= sbtVersion("org.scala-sbt" % "scripted-plugin" % _)
