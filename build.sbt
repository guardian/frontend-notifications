resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

lazy val messageWorker = (project in file("./messageworker"))
  .settings(
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(
      "joda-time" % "joda-time" % "2.9.2",
      "com.google.gcm" % "gcm-server" % "1.0.0",
      "com.amazonaws" % "aws-java-sdk" % "1.10.50",
      "com.amazonaws" % "amazon-kinesis-client" % "1.6.1"
    ),
    routesGenerator := InjectedRoutesGenerator
  )
  .enablePlugins(PlayScala)
