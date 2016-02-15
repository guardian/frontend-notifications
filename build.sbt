import com.twitter.scrooge.ScroogeSBT

scalaVersion := "2.11.7"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.apache.thrift" % "libthrift" % "0.9.2",
  "com.twitter" %% "finagle-thrift" % "6.33.0",
  "com.gu" %% "content-api-client" % "7.24"
)



lazy val common = (project in file("./common"))
  .settings(
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(
      "joda-time" % "joda-time" % "2.9.2",
      "com.amazonaws" % "aws-java-sdk" % "1.10.20",
      "com.google.gcm" % "gcm-server" % "1.0.0"
    )
  )
  .enablePlugins(PlayScala)


lazy val capiEventWorker = (project in file("./capieventworker"))
  .dependsOn(common)
  .settings(
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(
      "joda-time" % "joda-time" % "2.9.2",
      "com.amazonaws" % "aws-java-sdk" % "1.10.20",
      "com.amazonaws" % "amazon-kinesis-client" % "1.6.1",
      "com.gu" %% "content-api-client" % "7.24"
    ),
    routesGenerator := InjectedRoutesGenerator
  )
  .enablePlugins(PlayScala)
  .settings(ScroogeSBT.newSettings: _*)
  .settings(
    scalaVersion := "2.11.7",
    scroogeThriftDependencies in Compile := Seq("content-api-client_2.11", "content-atom-model_2.11", "content-api-models_2.11", "story-packages-model_2.11"),
    resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    resolvers += "Guardian GitHub Repository" at "http://guardian.github.io/maven/repo-releases",
    scroogeThriftSourceFolder in Compile <<= baseDirectory {
      base => base / "src/main/resources"
    },
    libraryDependencies ++= Seq(
      "com.gu" %% "content-api-client" % "7.24",
      "com.twitter" %% "scrooge-core" % "3.20.0"
    )
  )

lazy val messageWorker = (project in file("./messageworker"))
  .dependsOn(common)
  .settings(
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(
      "joda-time" % "joda-time" % "2.9.2",
      "com.amazonaws" % "aws-java-sdk" % "1.10.20"
    ),
    routesGenerator := InjectedRoutesGenerator
  )
  .enablePlugins(PlayScala)
