scalaVersion := "2.10.2"

name := "EAI_DomainModel"

version := "1.0.0"

scalacOptions ++= Seq("-deprecation", "-feature")

// resolvers ++= Seq(
//   "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
//   "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
// )

// libraryDependencies ++= Seq(
//   "com.typesafe.akka" %% "akka-actor" % "2.2.3",
//   "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
//   "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test",
//   "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3-M1",
//   "org.scalatest" %% "scalatest" % "2.0.M6-SNAP22" % "test"
// )


  // "Sonatype Repo" at "https://oss.sonatype.org/content/repositories/releases/",

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.1.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.1.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.1.4" % "test",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3-M1",
  "ch.qos.logback" % "logback-classic" % "1.0.10",
  "org.scalatest" %% "scalatest" % "2.0" % "test"
)


