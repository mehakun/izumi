package izumi.distage.model

import izumi.distage.model.definition.DIResource.DIResourceBase
import izumi.distage.model.effect.DIEffect
import izumi.distage.model.plan.OrderedPlan
import izumi.distage.model.provisioning.PlanInterpreter.{FailedProvision, FinalizerFilter}
import izumi.fundamentals.platform.functional.Identity
import izumi.reflect.TagK

/** Executes instructions in [[OrderedPlan]] to produce a [[Locator]] */
trait Producer {
  private[distage] def produceDetailedFX[F[_]: TagK: DIEffect](plan: OrderedPlan, filter: FinalizerFilter[F]): DIResourceBase[F, Either[FailedProvision[F], Locator]]
  private[distage] final def produceFX[F[_]: TagK: DIEffect](plan: OrderedPlan, filter: FinalizerFilter[F]): DIResourceBase[F, Locator] = {
    produceDetailedFX[F](plan, filter).evalMap(_.throwOnFailure())
  }

  final def produceCustomF[F[_]: TagK: DIEffect](plan: OrderedPlan): DIResourceBase[F, Locator] = {
    produceFX[F](plan, FinalizerFilter.all[F])
  }
  final def produceDetailedCustomF[F[_]: TagK: DIEffect](plan: OrderedPlan): DIResourceBase[F, Either[FailedProvision[F], Locator]] = {
    produceDetailedFX[F](plan, FinalizerFilter.all[F])
  }

  final def produceCustomIdentity(plan: OrderedPlan): DIResourceBase[Identity, Locator] =
    produceCustomF[Identity](plan)
  final def produceDetailedIdentity(plan: OrderedPlan): DIResourceBase[Identity, Either[FailedProvision[Identity], Locator]] =
    produceDetailedCustomF[Identity](plan)
}
