import sbtorgpolicies.runnable.syntax._

lazy val root = (project in file("."))
  .settings(moduleName := "root")
  .settings(name := "freestyle")
  .settings(noPublishSettings: _*)
  .aggregate(allModules: _*)

lazy val core = module("core")
  .jsSettings(sharedJsSettings: _*)
  .settings(libraryDependencies ++= Seq(%("scala-reflect", scalaVersion.value)))
  .settings(
    wartremoverWarnings in (Test, compile) := Warts.unsafe,
    wartremoverWarnings in (Test, compile) ++= Seq(
      Wart.FinalCaseClass,
      Wart.ExplicitImplicitTypes),
    wartremoverWarnings in (Test, compile) -= Wart.NonUnitStatements
  )
  .crossDepSettings(
    commonDeps ++ Seq(
      %("cats-free", "1.0.0-MF"),
      %("shapeless", "2.3.2"),
      %("simulacrum", "0.11.0"),
      %("cats-laws", "1.0.0-MF") % "test"
    ): _*
  )
  .settings(libraryDependencies += "io.frees" %%% "iota-core" % "0.3.1")

lazy val coreJVM = core.jvm
lazy val coreJS  = core.js

lazy val tagless = module("tagless")
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)
  .settings(
    libraryDependencies += "com.kailuowang" %%% "mainecoon-core" % "0.4.0"
  )

lazy val taglessJVM = tagless.jvm
lazy val taglessJS  = tagless.js

lazy val tests = jvmModule("tests")
  .dependsOn(coreJVM % "compile->compile;test->test")
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= commonDeps ++ Seq(
      %("scala-reflect", scalaVersion.value),
      %%("pcplod") % "test",
      %%("monix-eval", "3.0.0-M1") % "test"
    ),
    fork in Test := true,
    javaOptions in Test ++= {
      val excludedScalacOptions: List[String] = List("-Yliteral-types", "-Ypartial-unification")
      val options = (scalacOptions in Test).value.distinct
        .filterNot(excludedScalacOptions.contains)
        .mkString(",")
      val cp = (fullClasspath in Test).value.map(_.data).filter(_.exists()).distinct.mkString(",")
      Seq(
        s"""-Dpcplod.settings=$options""",
        s"""-Dpcplod.classpath=$cp"""
      )
    }
  )

lazy val bench = jvmModule("bench")
  .dependsOn(jvmFreestyleDeps: _*)
  .settings(
    name := "bench",
    description := "freestyle benchmark"
  )
  .enablePlugins(JmhPlugin)
  .configs(Codegen)
  .settings(inConfig(Codegen)(Defaults.configSettings))
  .settings(classpathConfiguration in Codegen := Compile)
  .settings(noPublishSettings)
  .settings(libraryDependencies ++= Seq(%%("cats-free", "1.0.0-MF"), %%("scalacheck")))
  .settings(inConfig(Compile)(
    sourceGenerators += Def.task {
      val path = (sourceManaged in (Compile, compile)).value / "bench.scala"
      (runner in (Codegen, run)).value.run(
        "freestyle.bench.BenchBoiler",
        Attributed.data((fullClasspath in Codegen).value),
        path.toString :: Nil,
        streams.value.log)
      path :: Nil
    }
  ))

lazy val Codegen = sbt.config("codegen").hide

lazy val effects = module("effects")
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)
  .settings(libraryDependencies += "org.typelevel" %%% "cats-mtl-core" % "0.0.2")

lazy val effectsJVM = effects.jvm
lazy val effectsJS  = effects.js

lazy val async = (crossProject in file("modules/async/async"))
  .settings(name := "frees-async")
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val asyncJVM = async.jvm
lazy val asyncJS  = async.js

lazy val asyncCatsEffect = (crossProject in file("modules/async/cats-effect"))
  .dependsOn(core, async)
  .settings(name := "frees-async-cats-effect")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)
  .settings(libraryDependencies += "org.typelevel" %%% "cats-effect" % "0.4")

lazy val asyncCatsEffectJVM = asyncCatsEffect.jvm
lazy val asyncCatsEffectJS  = asyncCatsEffect.js

lazy val asyncGuava = (project in file("modules/async/guava"))
  .dependsOn(coreJVM, asyncJVM)
  .settings(name := "frees-async-guava")
  .settings(libraryDependencies ++= commonDeps ++ Seq(
    "com.google.guava" % "guava" % "22.0"
  ))

lazy val cache = module("cache")
  .dependsOn(core)
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps: _*)

lazy val cacheJVM = cache.jvm
lazy val cacheJS  = cache.js

lazy val config = jvmModule("config")
  .dependsOn(coreJVM)
  .settings(
    fixResources := {
      val testConf   = (resourceDirectory in Test).value / "application.conf"
      val targetFile = (classDirectory in (coreJVM, Compile)).value / "application.conf"
      if (testConf.exists) {
        IO.copyFile(
          testConf,
          targetFile
        )
      }
    },
    compile in Test := ((compile in Test) dependsOn fixResources).value
  )
  .settings(
    libraryDependencies ++= Seq(
      %("config", "1.2.1"),
      %%("classy-config-typesafe"),
      %%("classy-core")
    ) ++ commonDeps
  )

lazy val logging = module("logging")
  .dependsOn(core)
  .jvmSettings(
    libraryDependencies += %%("journal-core")
  )
  .jsSettings(
    libraryDependencies += %%%("slogging")
  )
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++ Seq("com.lihaoyi" %% "sourcecode" % "0.1.3"): _*)

lazy val loggingJVM = logging.jvm
lazy val loggingJS  = logging.js

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val jvmModules: Seq[ProjectReference] = Seq(
  coreJVM,
  taglessJVM,
  effectsJVM,
  asyncJVM,
  asyncCatsEffectJVM,
  asyncGuava,
  cacheJVM,
  config,
  loggingJVM
  // ,tests
)

lazy val jsModules: Seq[ProjectReference] = Seq(
  coreJS,
  taglessJS,
  effectsJS,
  asyncJS,
  asyncCatsEffectJS,
  cacheJS,
  loggingJS
)

lazy val allModules: Seq[ProjectReference] = jvmModules ++ jsModules

lazy val jvmFreestyleDeps: Seq[ClasspathDependency] =
  jvmModules.map(ClasspathDependency(_, None))

addCommandAlias("validateJVM", (toCompileTestList(jvmModules) ++ List("project root")).asCmd)
addCommandAlias("validateJS", (toCompileTestList(jsModules) ++ List("project root")).asCmd)
addCommandAlias(
  "validate",
  ";clean;compile;coverage;validateJVM;coverageReport;coverageAggregate;coverageOff")
