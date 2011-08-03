sbtPlugin := true

// this plugin heavly based on https://github.com/Crias/xsbt-scalate-precompile-plugin
name := "xsbt-scalate-precompile-plugin"

organization := "com.gu"

version := "1.4-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.fusesource.scalate" % "scalate-core" % "1.4.1" % "compile",
  "ch.qos.logback" %  "logback-classic" % "0.9.28" % "compile"
)
