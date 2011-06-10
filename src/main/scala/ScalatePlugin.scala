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

  private def targetFileName(template: File, outputdir: File) =
    new File(outputdir, "/%s.scala".format(template.getName.replaceAll("[.]", "_")))

  private def isTemplate(template: File) =
    TemplateEngine.templateTypes.filter(template.getName.endsWith(_)).size > 0

  private def needsCompiling(template: File, outputdir: File): Boolean = {
    // PROBLEM: Doesn't address the fact that dependent objects may have changed. I don't
    // know if SBT has a hook for this sort of thing. Solution might be to use the
    // scalate precompiler, and adjust the managedClasspath in the settings declaration
    // to contain the classDirectory.
    if (! isTemplate(template))
      return false
    val target = targetFileName(template, outputdir)
    if (! target.exists())
      return true
    return template.lastModified > target.lastModified
  }

  private def generate (engine: TemplateEngine, template: File, outputdir: File, log: Logger) = {
    log.info(" compiling template: " + template)
    val source = TemplateSource.fromFile(template, template.getName)
    source.engine = engine
    val code = engine.generateScala(source)
    IO.write(targetFileName(template, outputdir), code.source)
  }

  private def scalateLoggingConfigValue: Initialize[File] =
    (resourceDirectory in Compile) { (d) => new File(d, "/logback.xml") }

  def scalateTemplateDirectoriesValue: Initialize[Seq[File]] =
    (resourceDirectory in Compile) { (d) => Seq(d) }

  def scalateSourceGeneratorTask: Initialize[Task[Seq[File]]] = (streams,
                                                                 sourceManaged in Compile,
                                                                 scalateTemplateDirectories in Compile,
                                                                 scalateLoggingConfig in Compile) map {
    (out, outputDir, inputDir, logConfig) => {
      System.setProperty("logback.configurationFile", logConfig.toString)

      val engine = new org.fusesource.scalate.TemplateEngine()
      engine.packagePrefix = ""

      inputDir.foreach { dir =>
        dir.listFiles.filter(needsCompiling(_, outputDir)).foreach { template =>
          generate(engine, template, outputDir, out.log)
        }
      }
      outputDir.listFiles.toList
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
