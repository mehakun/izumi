package izumi.logstage.distage

import izumi.distage.model.plan.{OrderedPlan, SemiPlan}
import izumi.distage.model.planning.PlanningObserver
import izumi.logstage.api.IzLogger

final class PlanningObserverLoggingImpl(
  log: IzLogger
) extends PlanningObserver {

  override def onPhase10PostGC(plan: SemiPlan): Unit = {
    log.trace(s"[onPhase10PostGC]:\n$plan")
  }
  override def onPhase90AfterForwarding(plan: OrderedPlan): Unit = {
    log.trace(s"[onPhase30AfterForwarding]:\n$plan")
  }
}
