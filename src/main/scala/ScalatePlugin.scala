package com.zentrope

import sbt._
import Keys._
import Project.Initialize

import org.fusesource.scalate.TemplateSource
import org.fusesource.scalate.TemplateEngine

import java.io.File

object ScalatePlugin extends Plugin {


  val scalateTemplateDirectories = SettingKey[Seq[File]]("scalate-template-directories", "Directories containing template files")
  val scalateFileTypes = SettingKey[FileFilter]("scalate-file-types", "File types processable by Scalate")
  val scalateTemplates = SettingKey[Seq[(File, String)]]("scalate-templates", "Full set of template files to process")
  val scalateLoggingConfig = SettingKey[File]("scalate-logging-config", "Logback config to get rid of that infernal debug output")

  private def scala(uri: String, outputdir: File) = outputdir / (uri + ".scala")

  private def updated(template: File, scala: File) = (!scala.exists()) || (template.lastModified > scala.lastModified)

  private def code(engine: TemplateEngine, template: File, uri: String) = {
    val source = TemplateSource.fromFile(template, uri)
    source.engine = engine
    engine.generateScala(source).source
  }

  private def lazyGenerate(engine: TemplateEngine, template: File, uri: String, outputdir: File, log: Logger) = {
    val scalaFile = scala(uri, outputdir)
    if (updated(template, scalaFile)) {
      log.info(" compiling template: " + uri)
      IO.write(scalaFile, code(engine, template, uri))
    }
    scalaFile
  }

  def scalateSourceGeneratorTask: Initialize[Task[Seq[File]]] =
    (streams, sourceManaged in Compile, scalateTemplates in Compile, scalateLoggingConfig in Compile) map {
      (out, outputDir, templates, logConfig) => generateScalateSource(out, outputDir, templates, logConfig)
    }

  def generateScalateSource(out: TaskStreams, outputDir: File, templateMappings: Seq[(File, String)], logConfig : File) = {
    // If we throw an exception here, it'll break the compile. Which is what
    // I want.
    System.setProperty("logback.configurationFile", logConfig.toString)

    val engine = new org.fusesource.scalate.TemplateEngine()
    engine.packagePrefix = ""

    for ( (template, uri) <- templateMappings) yield
      lazyGenerate(engine, template, uri, outputDir, out.log)
  }

  // Overriding settings means these expressions will be automatically interpolated
  // into the project's build.sbt

  override def settings = Seq (
    scalateLoggingConfig in Compile <<= (resourceDirectory in Compile) { _ / "logback.xml" },
    scalateFileTypes := TemplateEngine.templateTypes map { t => "*." + t : FileFilter } reduceLeft { _ || _ },
    scalateTemplateDirectories in Compile <<= (resourceDirectory in Compile) { Seq(_) },
    scalateTemplates in Compile <<= (scalateTemplateDirectories in Compile, scalateFileTypes) {
      (dirs, scalateFiles) => dirs ** scalateFiles x relativeTo(dirs)
    },
    sourceGenerators in Compile <+= scalateSourceGeneratorTask
  )

}
