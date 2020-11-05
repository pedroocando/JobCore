import play.PlayJava
import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

name := """JobCore"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.10.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "mysql" % "mysql-connector-java" % "5.1.46",
  "com.google.guava" % "guava" % "15.0",
  "net.vz.mongodb.jackson" %% "play-mongo-jackson-mapper" % "1.1.0",
  "org.reflections" % "reflections" % "0.9.7.RC1",
  "javax.activation" % "activation" % "1.1",
  "javax.mail" % "mail" % "1.4.7",
  "com.sun.xml.messaging.saaj" % "saaj-impl" % "1.3",
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "com.typesafe.akka" %% "akka-contrib" % "2.3.3",
  "com.typesafe.akka" %% "akka-remote" % "2.3.4",
  "be.objectify"  %% "deadbolt-java"     % "2.3.0-RC1",
  "com.feth"      %% "play-authenticate" % "0.6.5-SNAPSHOT"
)

resolvers ++= Seq(
  "Maven1 Repository" at "http://insecure.repo1.maven.org/maven2/net/vz/mongodb/jackson/play-mongo-jackson-mapper_2.10/1.1.0/",
  "Apache" at "http://insecure.repo1.maven.org/maven2/",
  "jBCrypt Repository" at "http://insecure.repo1.maven.org/maven2/org/",
  "play-easymail (release)" at "https://joscha.github.io/play-easymail/repo/releases/",
  "play-easymail (snapshot)" at "https://joscha.github.io/play-easymail/repo/snapshots/",
  Resolver.url("Objectify Play Repository", url("https://schaloner.github.io/releases/"))(Resolver.ivyStylePatterns),
  "play-authenticate (release)" at "https://joscha.github.io/play-authenticate/repo/releases/",
  "play-authenticate (snapshot)" at "https://joscha.github.io/play-authenticate/repo/snapshots/"
)


