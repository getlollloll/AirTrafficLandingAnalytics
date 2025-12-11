ThisBuild / version := "0.1.0-SNAPSHOT"

name := "airtrafficlanding"      // or "TRAFFIC-LANDING-ANALYTICS"

scalaVersion := "2.13.16"

// Where unmanaged jars (like tableparser-core_2.13-1.0.jar) live
unmanagedBase := baseDirectory.value / "lib"

// Automatically add all jars from lib/ to the compile classpath
Compile / unmanagedJars ++= (baseDirectory.value / "lib" ** "*.jar").classpath

libraryDependencies ++= Seq(
  // Spark (batch, later)
  "org.apache.spark" %% "spark-core" % "3.5.1" % "provided",
  "org.apache.spark" %% "spark-sql"  % "3.5.1" % "provided",

  // Akka (for streaming & HTTP later)
  "com.typesafe.akka" %% "akka-actor-typed" % "2.8.5",
  "com.typesafe.akka" %% "akka-stream"      % "2.8.5",
  "com.typesafe.akka" %% "akka-http"        % "10.5.3",

  "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.8.5" % Test,

  // Logging (needed for LazyLogging)
  "ch.qos.logback" % "logback-classic" % "1.5.16",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",

  // Tests
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,

  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",

)

// Main entry point
Compile / mainClass := Some("air.app.Run")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-encoding", "utf8"
)