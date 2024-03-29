ThisBuild / scalaVersion     := "2.13.13"
ThisBuild / version          := "0.0.1"
ThisBuild / organization     := "dev.tradex"
ThisBuild / organizationName := "tradex"

ThisBuild / evictionErrorLevel := Level.Warn
ThisBuild / scalafixDependencies += Dependencies.organizeImports

resolvers += Resolver.sonatypeRepo("snapshots")
// val scalafixCommonSettings = inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest))

lazy val root = (project in file("."))
  .settings(
    name := "tradeioz2"
  )
  .aggregate(core, tests, it)

lazy val core = (project in file("modules/core")).settings(
  name := "tradeioz2-core",
  commonSettings,
  consoleSettings,
  dependencies
)

lazy val tests = (project in file("modules/tests"))
  .settings(commonSettings: _*)
  .configs(IntegrationTest)
  .settings(
    name           := "tradeioz2-test-suite",
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    testDependencies
  )
  .dependsOn(core)

lazy val it = (project in file("modules/it"))
  .settings(commonSettings: _*)
  .configs(IntegrationTest)
  .settings(
    name           := "tradeioz2-it-suite",
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    Defaults.itSettings,
    itDependencies
  )
  .dependsOn(core % "it->test")

lazy val commonSettings = Seq(
  scalafmtOnCompile := true,
  scalacOptions ++= List("-Ymacro-annotations", "-Yrangepos", "-Wconf:cat=unused:info"),
  resolvers += Resolver.sonatypeRepo("snapshots")
)

lazy val consoleSettings = Seq(
  Compile / console / scalacOptions --= Seq("-Ywarn-unused", "-Ywarn-unused-import")
)

lazy val compilerOptions = {
  val commonOptions = Seq(
    "-unchecked",
    "-deprecation",
    "-encoding",
    "utf8",
    "-target:jvm-1.8",
    "-feature",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps",
    "-Ywarn-value-discard",
    "-Ymacro-annotations",
    "-Ywarn-unused:imports"
  )

  scalacOptions ++= commonOptions
}

lazy val dependencies =
  libraryDependencies ++= Dependencies.tradeioDependencies

lazy val testDependencies =
  libraryDependencies ++= Dependencies.testDependencies

lazy val itDependencies =
  libraryDependencies ++= Dependencies.testDependencies
