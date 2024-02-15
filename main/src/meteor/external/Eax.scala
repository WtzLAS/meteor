package meteor.external

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.core.readFromStream
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import cats.effect.kernel.Resource
import org.typelevel.otel4s.metrics.Counter

case class Eax[F[_]](
    requests: Counter[F, Long]
)

object Eax {
    def create = {
        
    }
}