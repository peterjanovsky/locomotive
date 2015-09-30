name := "locomotive"

lazy val locomotive = (project in file("."))

scalaVersion := "2.11.7"

organization := "com.pjanof"

resolvers ++= Seq(
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= {
  val akkaV  = "2.3.12"
  val sprayV = "1.3.3"

  Seq(
      "com.typesafe.scala-logging"  %%  "scala-logging"                       % "3.1.0"
    , "org.scalaz"                  %%  "scalaz-core"                         % "7.1.3"
    , "com.couchbase.client"        %   "couchbase-client"                    % "1.4.9"
    , "com.typesafe.akka"           %%  "akka-http-spray-json-experimental"   % "1.0"
    , "joda-time"                   %   "joda-time"                           % "2.8.2"
    , "org.scalatest"               %%  "scalatest"                           % "2.2.4" % "test")
}

// run options
scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

// test options
fork := true

javaOptions in Test += "-Dconfig.file=src/test/resources/reference.conf"
