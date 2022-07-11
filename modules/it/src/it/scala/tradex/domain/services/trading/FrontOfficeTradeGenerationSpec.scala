package tradex.domain.services.trading

import zio.ZIO
import zio.test._
import zio.test.Assertion._
import scala.io.Source
import io.circe.parser.parse
import tradex.domain.model.trade.GenerateTradeFrontOfficeInput

object FrontOfficeTradeGenerationSpec {
  def loadFile(name: String) =
    ZIO.acquireReleaseWith(ZIO.attempt(Source.fromResource(name)))(src => ZIO.succeed(src.close())) { in =>
      ZIO.succeed(in.mkString)
    }

  def loadFileAndParse(name: String) =
    loadFile(name)
      .map(
        parse(_)
          .flatMap(_.as[List[GenerateTradeFrontOfficeInput]])
      )
      .absolve

  def spec = suite("FrontOfficeTradeGenerationSpec")(
    test("parse") {
      for {
        json <- loadFileAndParse("trading/fo-trades-messages.json")
      } yield (assertTrue(json.size == 6))
    }
  )
}
