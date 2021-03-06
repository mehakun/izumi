package izumi.distage.roles.launcher

import distage.{Injector, TagK}
import izumi.distage.framework.services.IntegrationChecker
import izumi.distage.framework.services.RoleAppPlanner.AppStartupPlans
import izumi.distage.model.Locator
import izumi.distage.model.definition.DIResource.DIResourceBase
import izumi.distage.model.definition.Id
import izumi.distage.model.effect.{DIEffect, DIEffectRunner}
import izumi.distage.model.provisioning.PlanInterpreter.FinalizerFilter
import izumi.distage.model.recursive.Bootloader
import izumi.fundamentals.platform.functional.Identity

trait AppResourceProvider[F[_]] {
  def makeAppResource(): DIResourceBase[Identity, PreparedApp[F]]
}

object AppResourceProvider {

  final case class FinalizerFilters[F[_]](
    filterF: FinalizerFilter[F],
    filterId: FinalizerFilter[Identity],
  )
  object FinalizerFilters {
    def all[F[_]]: FinalizerFilters[F] = FinalizerFilters[F](FinalizerFilter.all, FinalizerFilter.all)
  }

  class Impl[F[_]: TagK](
    integrationChecker: IntegrationChecker[F],
    entrypoint: RoleAppEntrypoint[F],
    filters: FinalizerFilters[F],
    appPlan: AppStartupPlans,
    bootloader: Bootloader @Id("roleapp"),
  ) extends AppResourceProvider[F] {
    def makeAppResource(): DIResourceBase[Identity, PreparedApp[F]] = {
      appPlan
        .injector
        .produceFX[Identity](appPlan.runtime, filters.filterId)
        .map {
          runtimeLocator =>
            val runner = runtimeLocator.get[DIEffectRunner[F]]
            implicit val F: DIEffect[F] = runtimeLocator.get[DIEffect[F]]

            PreparedApp(prepareMainResource(runtimeLocator)(F), runner, F)
        }
    }

    private def prepareMainResource(runtimeLocator: Locator)(implicit F: DIEffect[F]): DIResourceBase[F, Locator] = {
      bootloader
        .injectorFactory
        .inherit(runtimeLocator)
        .produceFX[F](appPlan.app.shared, filters.filterF)
        .flatMap {
          sharedLocator =>
            Injector
              .inherit(sharedLocator)
              .produceFX[F](appPlan.app.side, filters.filterF)
              .evalTap {
                integrationLocator =>
                  integrationChecker.checkOrFail(appPlan.app.sideRoots1, appPlan.app.sideRoots2, integrationLocator)
              }
              .flatMap {
                _ => // we don't need integration locator
                  Injector
                    .inherit(sharedLocator)
                    .produceFX[F](appPlan.app.primary, filters.filterF)
                    .evalTap(entrypoint.runTasksAndRoles(_, F))
              }
        }
    }
  }

}
