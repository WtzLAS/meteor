package meteor.util

import org.http4s.HttpApp
import org.typelevel.otel4s.metrics.Meter
import cats.data.Kleisli
import cats.syntax.all.toFunctorOps
import cats.syntax.all.toFlatMapOps
import cats.syntax.all.catsSyntaxApplyOps
import cats.effect.kernel.MonadCancel
import scribe.Scribe
import cats.effect.kernel.Outcome
import org.typelevel.otel4s.Attribute
import cats.effect.kernel.Clock
import java.util.concurrent.TimeUnit

object Otel4sServerMetrics {
  def apply[F[_], E](prefix: String = "http4s")(
      app: HttpApp[F]
  )(using
      F: MonadCancel[F, E],
      scribeF: Scribe[F],
      meterF: Meter[F],
      clockF: Clock[F]
  ): HttpApp[F] = Kleisli { req =>
    for {
      requestsCounter <- meterF
        .counter(prefix + "_request_count")
        .withDescription("Total request count.")
        .withUnit("1")
        .create
      requestDurationsHistogram <- meterF
        .histogram(prefix + "_request_duration")
        .withDescription("Request duration.")
        .withUnit("ms")
        .create
      res <- F.bracketCase(clockF.monotonic)(_ => app(req))((startTime, oc) =>
        for {
          endTime <- clockF.monotonic
          attributes <- oc match
            case Outcome.Succeeded(fa) =>
              for {
                res <- fa
              } yield List(
                Attribute("method", req.method.name),
                Attribute("path", req.uri.path.toString),
                Attribute("outcome", "succeeded"),
                Attribute("status", res.status.code.toLong)
              )
            case Outcome.Errored(e) =>
              F.pure(
                List(
                  Attribute("method", req.method.name),
                  Attribute("path", req.uri.path.toString),
                  Attribute("outcome", "errored"),
                  Attribute("cause", e.getClass().getName())
                )
              )
            case Outcome.Canceled() =>
              F.pure(
                List(
                  Attribute("method", req.method.name),
                  Attribute("path", req.uri.path.toString),
                  Attribute("outcome", "canceled")
                )
              )
          _ <- requestsCounter.inc(
            attributes*
          )
          _ <- requestDurationsHistogram.record(
            (endTime - startTime).toUnit(TimeUnit.MILLISECONDS),
            attributes*
          )
        } yield ()
      )
    } yield res
  }
}
