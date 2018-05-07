import sbt.Keys.libraryDependencies
import sbt._

object Dependencies {

  val akkaVersion       = "2.5.12"
  val dropwizardVersion = "4.0.2"
  val kamonVersion      = "1.1.0"
  val scalatestVersion  = "3.0.5"

  val scalaTest = "org.scalatest" %% "scalatest" % scalatestVersion % Test

  val core = Seq(libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream"         % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    scalaTest
  ))

  val dropwizard = Seq(libraryDependencies ++= Seq(
    "io.dropwizard.metrics" % "metrics-core" % dropwizardVersion,
    scalaTest
  ))

  val kamon = Seq(libraryDependencies ++= Seq(
    "io.kamon" %% "kamon-core"    % kamonVersion,
    "io.kamon" %% "kamon-testkit" % kamonVersion % Test,
    scalaTest
  ))

  val docs = Seq(libraryDependencies ++= Seq(
    "com.typesafe.akka"     %% "akka-slf4j"       % akkaVersion,
    "org.slf4j"             %  "slf4j-api"        % "1.7.16" % Runtime,
    "ch.qos.logback"        %  "logback-classic"  % "1.1.5"  % Runtime,
    "io.dropwizard.metrics" %  "metrics-graphite" % dropwizardVersion,
    "io.kamon"              %% "kamon-statsd"     % "1.0.0"
  ))

}