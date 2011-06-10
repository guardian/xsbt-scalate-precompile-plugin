// -*- mode: scala; -*-

sbtPlugin := true

name := "xsbt-scalate-precompile-plugin"

organization := "com.zentrope"

version := "1.0"

cleanFiles <<= (cleanFiles, baseDirectory) {
  (files, basedir) =>
    files ++ Seq(new File(basedir, "/project/target"))
}

libraryDependencies ++= Seq(
  "org.fusesource.scalate" % "scalate-core" % "1.4.1" % "compile",
  "ch.qos.logback" %  "logback-classic" % "0.9.28" % "compile"
)

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := Some("Zentrope Repo" at "http://zentrope.com/maven")

resolvers += Resolver.sftp("Zentrope Repo", "zentrope.com", "domains/zentrope.com/maven")
