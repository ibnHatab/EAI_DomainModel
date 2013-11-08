scalaVersion := "2.10.2"

name := "EAI_DomainModel"

version := "1.0.0"

scalacOptions ++= Seq("-deprecation", "-feature", "-language:postfixOps")

resolvers += "Akka Snapshots" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test",
//  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3-20130921-230908",
  "ch.qos.logback" % "logback-classic" % "1.0.10",
  "org.scalatest" %% "scalatest" % "2.0" % "test"
)
