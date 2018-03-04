name := "data-source-simulator"

version := "0.1"

scalaVersion := "2.11.6"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test
)

libraryDependencies += "org.yaml" % "snakeyaml" % "1.20-SNAPSHOT"

libraryDependencies += "com.datastax.spark" %% "spark-cassandra-connector" % "2.0.7"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.2.1"


resolvers += "Sonatype-public" at "http://oss.sonatype.org/content/groups/public/"