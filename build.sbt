name := "AkkaHttpDnsInvestigation"

version := "1.0"

val settings = Seq(
  scalaVersion := "2.12.2"
)

lazy val commonDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.8",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

lazy val testDependencies = Seq(
  "org.scalatest" % "scalatest_2.12" % "3.0.3" % Test,
  "com.github.docker-java" % "docker-java" % "3.0.10" % Test
)


lazy val root = project.in(file("."))
  .aggregate(server, client, investigation)

lazy val server = project
  .settings(settings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .settings(packageName in Docker := "wojda/server")
  .settings(dockerUpdateLatest := true)
  .enablePlugins(JavaAppPackaging)

import com.typesafe.sbt.packager.docker._
lazy val client = project
  .settings(settings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .settings(daemonUser in Docker := "root")
  .settings(packageName in Docker := "wojda/akka-client")
  .settings(dockerCommands ++= Seq(Cmd("RUN", "echo networkaddress.cache.ttl=0 >> /etc/java-8-openjdk/security/java.security")))
  .settings(javaOptions in Universal ++= Seq("-Dsun.net.inetaddr.ttl=0"))
  .settings(dockerExposedPorts := Seq(8080))
  .settings(dockerUpdateLatest := true)
  .enablePlugins(JavaAppPackaging)

lazy val investigation = project
  .settings(settings: _*)
  .settings(libraryDependencies ++= commonDependencies ++ testDependencies)