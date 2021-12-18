package tradex

import java.time.LocalDateTime

import squants.market._
import zio.prelude._

import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.auto._

package object domain {

  def today                  = LocalDateTime.now
  final val ZERO_BIG_DECIMAL = BigDecimal(0)

  implicit val moneyContext = defaultMoneyContext

  object NewtypeRefinedOps {
    import io.estatico.newtype.Coercible
    import io.estatico.newtype.ops._

    final class NewtypeRefinedPartiallyApplied[A] {
      def apply[T, P](raw: T)(implicit
          c: Coercible[Refined[T, P], A],
          v: Validate[T, P]
      ): Validation[String, A] =
        refineV[P](raw)
          .fold(s => Validation.fail(s), v => Validation.succeed(v.coerce[A]))
    }

    def validate[A]: NewtypeRefinedPartiallyApplied[A] =
      new NewtypeRefinedPartiallyApplied[A]
  }
}
