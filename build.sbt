import com.lihaoyi.workbench.Plugin._

enablePlugins(ScalaJSPlugin)

workbenchSettings

name := "decloaking"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.9.0",
        "com.lihaoyi" %%% "scalatags" % "0.5.5"
        )

bootSnippet := "decloaking.Main().main(document.getElementById('decloaking-div'));"

updateBrowsers <<= updateBrowsers.triggeredBy(fastOptJS in Compile)
