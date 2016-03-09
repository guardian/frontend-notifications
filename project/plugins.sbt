resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.0")

resolvers += "twitter-repo" at "https://maven.twttr.com"

addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % "3.16.3")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.8.3")
