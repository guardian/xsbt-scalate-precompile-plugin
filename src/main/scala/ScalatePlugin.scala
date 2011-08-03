package com.zentrope

import sbt._
import Keys._
import Project.Initialize

import org.fusesource.scalate.TemplateSource
import org.fusesource.scalate.TemplateEngine

import java.io.File

object ScalatePlugin extends Plugin {


  val scalateTemplateDirectories = SettingKey[Seq[File]]("scalate-template-directories", "Directories containing template files")
  val scalateTemplates = SettingKey[PathFinder]("scalate-templates", "Full set of template files to process")
  val scalateLoggingConfig = SettingKey[File]("scalate-logging-config", "Logback config to get rid of that infernal debug output")
  val scalateFileTypes = SettingKey[FileFilter]("scalate-file-types", "File types processable by Scalate")

  private def scala(template: File, outputdir: File) =
    new File(outputdir, "/%s.scala".format(template.getName.replaceAll("[.]", "_")))

  private def updated(template: File, scala: File) =
    (! scala.exists()) || (template.lastModified > scala.lastModified)

  private def code(engine: TemplateEngine, template: File) = {
    val source = TemplateSource.fromFile(template, template.getName)
    source.engine = engine
    engine.generateScala(source).source
  }

  private def lazyGenerate(engine: TemplateEngine, template: File, outputdir: File, log: Logger) = {
    val scalaFile = scala(template, outputdir)
    if (updated(template, scalaFile)) {
      log.info(" compiling template: " + template)
      IO.write(scalaFile, code(engine, template))
    }
    scalaFile
  }

  def scalateSourceGeneratorTask: Initialize[Task[Seq[File]]] =
    (streams, sourceManaged in Compile, scalateTemplates in Compile, scalateLoggingConfig in Compile) map {
      (out, outputDir, inputDirs, logConfig) => generateScalateSource(out, outputDir, inputDirs, logConfig)
    }

  def generateScalateSource(out: TaskStreams, outputDir: File, inputFiles: PathFinder, logConfig : File) = {
    out.log.info("generate scalate source: out: %s in: %s" format (outputDir, inputFiles.get) )
    // If we throw an exception here, it'll break the compile. Which is what
    // I want.

    System.setProperty("logback.configurationFile", logConfig.toString)

    val engine = new org.fusesource.scalate.TemplateEngine()
    engine.packagePrefix = ""

    for (template <- inputFiles.get) yield
      lazyGenerate(engine, template, outputDir, out.log)
  }

  // Overriding settings means these expressions will be automatically interpolated
  // into the project's build.sbt

  override def settings = Seq (
    scalateLoggingConfig in Compile <<= (resourceDirectory in Compile) { _ / "logback.xml" },
    scalateFileTypes := TemplateEngine.templateTypes map { t => "*." + t : FileFilter } reduceLeft { _ || _ },
    scalateTemplateDirectories in Compile <<= (resourceDirectory in Compile) { Seq(_) },
    scalateTemplates in Compile <<= (scalateTemplateDirectories in Compile, scalateFileTypes) {
      (dirs, scalateFiles) => dirs ** scalateFiles
    },
    sourceGenerators in Compile <+= scalateSourceGeneratorTask
  )

}
