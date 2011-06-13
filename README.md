# XSBT Scalate Precompile Plugin 1.3

This XSBT >= 0.10.0 plugin precompiles your Scalate templates for you
as part of the normal compilation process. The plugin accomplishes
this by hooking SBT's generated-sources task list with a task that
generates Scala sources from Scalate Templates. SBT then compiles the
generated source in the normal way.

In order to use this plugin, just include it as a dependency in your
`project/plugins/build.sbt` file and precompilation will "just work".

## Design Choices

This plugin is not supported by the Scalate Project and thus only
supports the work-flows that I actually use (mainly because I don't
really know about any others). I write web apps using Scalatra and
Scalatra's Scalate support. These apps run stand-alone (rather than as
WAR files or some other container based strategy), so this sort of
plugin is perfect for me.

These are the following design choices:

 * Simply installing this plugin (see below) makes it "just work".

 * Precompilation is not optional (yet). The workflow should be to
   compile, then start your app, then see if it worked. My assumption
   is that you're running a stand-alone web app.

 * All templates are assumed to be in the resources directory
   (src/main/resources) as a flat list (no sub directories).

 * There's no infrastructure for operating within the testing scope.

 * Scalate templates are compiled when the `compile` task is invoked
   and deposited in the directory defined by the `managed-sources`
   setting.

 * The `clean` task removes the generated sources.

 * While the plugin only generates scala from a template when it
   detects that a template has changed, it does not detecte the need
   for regeneration due to dependencies on classes in your application
   (say, a User.scala object you iterate over for presentation).

 * The plugin only generates scala source from scalate templates and
   then allows SBT to compile the results. It doesn't use the
   precompiler supplied by the Scalate project.

If you can live with all that, then this plugin will work for you. If
not, send me suggestions, or agitate for the Scalate folks to
implement a new plugin for SBT >= 0.10.0.

## Install

Create a file called `/project/plugins/build.sbt' and add the following lines:

    resolvers ++= Seq (
      "zentrope" at "http://zentrope.com/maven"
    )

    libraryDependencies ++= Seq (
        "com.zentrope" %% "xsbt-scalate-precompile-plugin" % "1.2"
    )

## Tasks

None! Your Scalate templates are compiled whenever SBT invokes its
`compile` task as part of `run` or whatever.

## Settings

The following are the settings used by the plugin. You and change 'em
if you want:

    scalate-template-directories   ;; List of directories containing templates.

    scalate-logging-config         ;; Log back configuration for Scalate logging.

You can add new template source directories like:

    scalateTemplateDirectories in Compile <<= (scalateTemplateDirectories in Compile, baseDirectory) {
      (dirs, basedir) =>
        dirs ++ Seq(new File(basedir, "/path/to/somewhere"))
    }

You can also modify Scalate's default logging via a `logback.xml`
config file in the `src/main/resources` directory via the
logging-config setting. By default Scalate prints out a lot of debug
information which can become distracting after a while.

## Credits

TODO
