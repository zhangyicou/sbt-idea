resolvers ++= Seq(
  Classpaths.typesafeSnapshots,
  "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
)

definedSbtPlugins:= Set("org.sbtidea.SbtIdeaPlugin")

