import sbt._
import Keys._
import Versions._

object Dependencies {
  object Zio {
    val zio            = "dev.zio" %% "zio"              % zioVersion
    val zioStreams     = "dev.zio" %% "zio-streams"      % zioVersion
    val zioMacros      = "dev.zio" %% "zio-macros"       % zioVersion
    val zioPrelude     = "dev.zio" %% "zio-prelude"      % zioPreludeVersion
    val zioQuery       = "dev.zio" %% "zio-query"        % zioQueryVersion
    val zioInteropCats = "dev.zio" %% "zio-interop-cats" % zioInteropCatsVersion
    val zioConfig      = "dev.zio" %% "zio-config"       % zioConfigVersion
    val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % zioConfigVersion
    val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
    val zioTest        = "dev.zio" %% "zio-test"         % zioVersion % "it,test"
    val zioTestSbt     = "dev.zio" %% "zio-test-sbt"     % zioVersion % "it,test"
  }

  // Scalafix rules
  val organizeImports = "com.github.liancheng" %% "organize-imports" % organizeImportsVersion

  def derevo(artifact: String): ModuleID    = "tf.tofu"           %% s"derevo-$artifact"    % derevoVersion
  def circe(artifact: String): ModuleID     = "io.circe"          %% s"circe-$artifact"     % circeVersion
  def http4s(artifact: String): ModuleID    = "org.http4s"        %% s"http4s-$artifact"    % http4sVersion
  def cormorant(artifact: String): ModuleID = "io.chrisdavenport" %% s"cormorant-$artifact" % cormorantVersion

  object Misc {
    val newtype    = "io.estatico"           %% "newtype"     % newtypeVersion
    val squants    = "org.typelevel"         %% "squants"     % squantsVersion
    val fs2Core    = "co.fs2"                %% "fs2-core"    % fs2Version
    val fs2IO      = "co.fs2"                %% "fs2-io"      % fs2Version
    val pureconfig = "com.github.pureconfig" %% "pureconfig"  % pureconfigVersion
    val flywayDb   = "org.flywaydb"           % "flyway-core" % flywayDbVersion
  }

  object Refined {
    val refinedCore      = "eu.timepit" %% "refined"           % refinedVersion
    val refinedCats      = "eu.timepit" %% "refined-cats"      % refinedVersion
    val refinedShapeless = "eu.timepit" %% "refined-shapeless" % refinedVersion
  }

  object Circe {
    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")
    val circeRefined = circe("refined")
  }

  object Derevo {
    val derevoCore          = derevo("core")
    val derevoCiris         = derevo("ciris")
    val derevoCirce         = derevo("circe")
    val derevoCirceMagnolia = derevo("circe-magnolia")
  }

  object Ciris {
    val cirisCore    = "is.cir" %% "ciris"            % cirisVersion
    val cirisEnum    = "is.cir" %% "ciris-enumeratum" % cirisVersion
    val cirisRefined = "is.cir" %% "ciris-refined"    % cirisVersion
    val cirisCirce   = "is.cir" %% "ciris-circe"      % cirisVersion
    val cirisSquants = "is.cir" %% "ciris-squants"    % cirisVersion
  }

  object Doobie {
    val doobieCore       = "org.tpolecat" %% "doobie-core"       % doobieVersion
    val doobieH2         = "org.tpolecat" %% "doobie-h2"         % doobieVersion
    val doobieHikari     = "org.tpolecat" %% "doobie-hikari"     % doobieVersion
    val doobiePostgres   = "org.tpolecat" %% "doobie-postgres"   % doobieVersion
    val doobieEnumeratum = "com.beachape" %% "enumeratum-doobie" % enumeratumDoobieVersion
  }

  // Runtime
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime

  object CompilerPlugin {
    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % betterMonadicForVersion
    )
    val kindProjector = compilerPlugin(
      "org.typelevel" %% "kind-projector" % kindProjectorVersion cross CrossVersion.full
    )
    val semanticDB = compilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % semanticDBVersion cross CrossVersion.full
    )
  }

  import CompilerPlugin._

  val commonDependencies: Seq[ModuleID] = Seq(
    Zio.zio, 
    Zio.zioConfig, 
    Zio.zioConfigTypesafe, 
    Zio.zioConfigMagnolia, 
    Zio.zioPrelude, 
    Zio.zioInteropCats, 
    Zio.zioStreams, 
    Zio.zioMacros, 
    Zio.zioQuery)

  val tradeioDependencies: Seq[ModuleID] =
    commonDependencies ++ Seq(kindProjector, betterMonadicFor, semanticDB) ++
      Seq(Misc.newtype, Misc.squants, Misc.pureconfig) ++ Seq(logback) ++ Seq(Misc.flywayDb) ++
      Seq(Derevo.derevoCore, Derevo.derevoCiris, Derevo.derevoCirceMagnolia) ++
      Seq(Refined.refinedCore, Refined.refinedCats, Refined.refinedShapeless) ++
      Seq(Ciris.cirisCore, Ciris.cirisEnum, Ciris.cirisRefined, Ciris.cirisCirce, Ciris.cirisSquants) ++
      Seq(Circe.circeCore, Circe.circeGeneric, Circe.circeParser, Circe.circeRefined) ++
      Seq(Doobie.doobieCore, Doobie.doobieHikari, Doobie.doobiePostgres, Doobie.doobieH2, Doobie.doobieEnumeratum)

  val testDependencies: Seq[ModuleID] =
    Seq(Zio.zioTest, Zio.zioTestSbt)
}
