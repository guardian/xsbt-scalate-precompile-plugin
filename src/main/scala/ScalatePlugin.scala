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
  val scalateTemplates = SettingKey[Seq[(File, String)]]("scalate-templates", "Full set of template files to process, with their resulting uris")

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

  def generateScalateSource(out: TaskStreams, outputDir: File, templateMappings: Seq[(File, String)]) = {
    // If we throw an exception here, it'll break the compile. Which is what
    // I want.
    val engine = new org.fusesource.scalate.TemplateEngine()
    engine.packagePrefix = ""

    for ( (template, uri) <- templateMappings) yield
      lazyGenerate(engine, template, uri, outputDir, out.log)
  }

  // recommended approach nowadays is for a plugin not to insert itself into settings automatically,
  // but allow user to add in as they require.
  //  - add "seq(com.zentrope.ScalatePlugin.scalatePrecompileSettings :_*)" to your build.sbt
  val scalatePrecompileSettings: Seq[Project.Setting[_]] = Seq (
    scalateFileTypes := TemplateEngine.templateTypes map { t => "*." + t : FileFilter } reduceLeft { _ || _ },
    scalateTemplateDirectories in Compile <<= (resourceDirectory in Compile) { Seq(_) },
    scalateTemplates in Compile <<= (scalateTemplateDirectories in Compile, scalateFileTypes) {
      (dirs, scalateFiles) => dirs ** scalateFiles x relativeTo(dirs)
    },
    sourceGenerators in Compile <+=
      (streams, sourceManaged in Compile, scalateTemplates in Compile) map generateScalateSource
  )

}
