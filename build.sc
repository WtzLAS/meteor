import mill._, scalalib._
import coursier.maven.MavenRepository
import $file.secrets

val additionalRepositories = Seq(
  MavenRepository("https://oss.sonatype.org/content/repositories/releases"),
  MavenRepository("https://s01.oss.sonatype.org/content/repositories/releases/")
)

object V {
  val catsEffect = "3.5.3"
  val http4s = "0.23.25"
  val otel4s = "0.4.0"
  val openTelemetry = "1.35.0"
  val scribe = "3.13.0"
  val jsoniter = "2.28.2"
}

object main extends ScalaModule {
  def scalaVersion = "3.3.1"

  def repositoriesTask = T.task {
    super.repositoriesTask() ++ additionalRepositories
  }

  def ivyDeps = Agg(
    ivy"org.typelevel::cats-effect:${V.catsEffect}",
    ivy"org.http4s::http4s-ember-client:${V.http4s}",
    ivy"org.http4s::http4s-ember-server:${V.http4s}",
    ivy"org.http4s::http4s-dsl:${V.http4s}",
    ivy"org.typelevel::otel4s-java:${V.otel4s}",
    ivy"com.outr::scribe:${V.scribe}",
    ivy"com.outr::scribe-slf4j2:${V.scribe}",
    ivy"com.outr::scribe-cats:${V.scribe}",
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:${V.jsoniter}"
  )

  def runIvyDeps = Agg(
    ivy"io.opentelemetry:opentelemetry-exporter-otlp:${V.openTelemetry}",
    ivy"io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:${V.openTelemetry}"
  )

  def compileIvyDeps = Agg(
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:${V.jsoniter}"
  )

  def forkArgs = Seq(
    "-Xmx2G",
    "-Dotel.java.global-autoconfigure.enabled=true",
    "-Dotel.service.name=meteor-test",
    "-Dotel.traces.exporter=none",
    "-Dotel.metrics.exporter=otlp",
    "-Dotel.logs.exporter=none",
    "-Dotel.exporter.otlp.metrics.default.histogram.aggregation=BASE2_EXPONENTIAL_BUCKET_HISTOGRAM",
    "-Dotel.exporter.otlp.endpoint=https://api.honeycomb.io/",
    s"-Dotel.exporter.otlp.headers=x-honeycomb-team=${secrets.honeycombApiKey},x-honeycomb-dataset=meteor-test"
  )
}
