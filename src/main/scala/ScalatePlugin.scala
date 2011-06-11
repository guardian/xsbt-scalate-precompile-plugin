package com.zentrope

import sbt._
import Keys._
import Project.Initialize

import org.fusesource.scalate.TemplateSource
import org.fusesource.scalate.TemplateEngine

import java.io.File

object ScalatePlugin extends Plugin {

  val scalateTemplateDirectories = SettingKey[Seq[File]]("scalate-template-directories",
                                                         "Locations of template files.")
  val scalateLoggingConfig = SettingKey[File]("scalate-logging-config",
                                              "Logback config to get rid of that infernal debug output.")

  private def scala(template: File, outputdir: File) =
    new File(outputdir, "/%s.scala".format(template.getName.replaceAll("[.]", "_")))

  private def recognized(template: File) =
    TemplateEngine.templateTypes.filter(template.getName.endsWith(_)).size > 0

  private def updated(template: File, scala: File) =
    (! scala.exists()) || (template.lastModified > scala.lastModified)

  private def changed(template: File, outputdir: File) =
    recognized(template) && updated(template, scala(template, outputdir))

  private def code(engine: TemplateEngine, template: File) = {
    val source = TemplateSource.fromFile(template, template.getName)
    source.engine = engine
    source
    engine.generateScala(source).source
  }

  private def generate (engine: TemplateEngine, template: File, outputdir: File, log: Logger) = {
    log.info(" compiling template: " + template)
    IO.write(scala(template, outputdir), code(engine, template))
  }

  private def scalateLoggingConfigValue: Initialize[File] =
    (resourceDirectory in Compile) { (d) => new File(d, "/logback.xml") }

  def scalateTemplateDirectoriesValue: Initialize[Seq[File]] =
    (resourceDirectory in Compile) { (d) => Seq(d) }

  def scalateSourceGeneratorTask: Initialize[Task[Seq[File]]] =
    (streams, sourceManaged in Compile, scalateTemplateDirectories in Compile, scalateLoggingConfig in Compile) map {

      (out, outputDir, inputDirs, logConfig) => {

        // If we throw an exception here, it'll break the compile. Which is what
        // I want.

        System.setProperty("logback.configurationFile", logConfig.toString)

        val engine = new org.fusesource.scalate.TemplateEngine()
        engine.packagePrefix = ""

        for (dir <- inputDirs)
          for (template <- dir.listFiles.filter(changed(_, outputDir)))
            generate(engine, template, outputDir, out.log)

        outputDir.listFiles match {
          case null => Seq()
          case (files) => files.toList
        }
      }
    }

  // Overriding settings means these expressions will be automatically interpolated
  // into the project's build.sbt

  override def settings = Seq (
    scalateLoggingConfig in Compile <<= scalateLoggingConfigValue,
    scalateTemplateDirectories in Compile <<= scalateTemplateDirectoriesValue,
    sourceGenerators in Compile <+= scalateSourceGeneratorTask
  )

}
