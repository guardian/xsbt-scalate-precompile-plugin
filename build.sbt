// -*- mode: scala; -*-

sbtPlugin := true

name := "xsbt-scalate-precompile-plugin"

organization := "com.zentrope"

version := "1.3"

cleanFiles <<= (cleanFiles, baseDirectory) {
  (files, basedir) =>
    files ++ Seq(new File(basedir, "/project/target"))
}

libraryDependencies ++= Seq(
  "org.fusesource.scalate" % "scalate-core" % "1.4.1" % "compile",
  "org.slf4j" % "slf4j-nop" % "1.6.1" % "compile"
)

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := Some("Zentrope Repo" at "http://zentrope.com/maven")

// this breaks if you use a source (git:) dependency to include this plugin
//resolvers += Resolver.sftp("Zentrope Repo", "zentrope.com", "domains/zentrope.com/maven")
