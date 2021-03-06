package izumi.distage.roles.launcher

import izumi.distage.model.Locator
import izumi.distage.model.definition.DIResource.DIResourceBase
import izumi.distage.model.effect.{DIEffect, DIEffectRunner}

final case class PreparedApp[F[_]](
  app: DIResourceBase[F, Locator],
  runner: DIEffectRunner[F],
  effect: DIEffect[F],
) {
  def run(): Unit = {
    runner.run(app.use(_ => effect.unit)(effect))
  }
}
