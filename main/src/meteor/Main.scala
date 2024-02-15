package meteor

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Resource
import com.comcast.ip4s.*
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.RequestLogger
import org.typelevel.otel4s.java.OtelJava
import org.typelevel.otel4s.metrics.Meter
import org.typelevel.otel4s.trace.Tracer
import scribe.Scribe
import scribe.cats.effect
import org.http4s.server.middleware.Metrics
import cats.effect.std.Console
import concurrent.duration.DurationInt
import meteor.util.Otel4sMetrics

object Main extends IOApp {
  val helloWorldService = HttpRoutes
    .of[IO] { case GET -> Root / "hello" =>
      Ok("Hello, world!")
    }

  private def app = for {
    otel <- OtelJava.global[IO]
    given Meter[IO] <- otel.meterProvider.get("meteor")
    given Tracer[IO] <- otel.tracerProvider.get("meteor")
    server <- EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8011")
      .withHttpApp(
        Otel4sMetrics(prefix = "meteor")(helloWorldService.orNotFound)
      )
      .build
      .useForever
  } yield server

  override def run(args: List[String]): IO[ExitCode] = app
}
