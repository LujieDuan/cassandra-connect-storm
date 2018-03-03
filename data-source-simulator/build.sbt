name := "data-source-simulator"

version := "0.1"

scalaVersion := "2.12.4"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test
)

libraryDependencies += "org.yaml" % "snakeyaml" % "1.20-SNAPSHOT"


resolvers += "Sonatype-public" at "http://oss.sonatype.org/content/groups/public/"