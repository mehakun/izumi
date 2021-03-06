package izumi.distage.modules.platform

import java.util.concurrent.{Executors, ThreadPoolExecutor}

import distage.Id
import izumi.distage.model.definition.{DIResource, ModuleDef}
import izumi.functional.bio.{BIORunner, BIORunner3, BlockingIO, BlockingIO3, BlockingIOInstances}
import izumi.functional.bio.BIORunner.{FailureHandler, ZIORunner}
import zio.{Has, IO, Runtime, ZIO}
import zio.blocking.Blocking
import zio.internal.Executor
import zio.internal.tracing.TracingConfig

import scala.concurrent.ExecutionContext

object ZIOPlatformSupportModule extends ModuleDef {
  make[BIORunner3[ZIO]].using[ZIORunner]

  make[BlockingIO3[ZIO]].from(BlockingIOInstances.BlockingZIO3FromBlocking(_: zio.blocking.Blocking.Service))
  make[BlockingIO[IO]].from {
    implicit B: BlockingIO3[ZIO] => BlockingIO[IO]
  }

  make[zio.blocking.Blocking].from(Has(_: Blocking.Service))
  make[zio.blocking.Blocking.Service].from {
    blockingPool: ThreadPoolExecutor @Id("zio.io") =>
      new Blocking.Service {
        override val blockingExecutor: Executor = Executor.fromThreadPoolExecutor(_ => Int.MaxValue)(blockingPool)
      }
  }

  make[ZIORunner].from {
    (cpuPool: ThreadPoolExecutor @Id("zio.cpu"), handler: FailureHandler, tracingConfig: TracingConfig) =>
      BIORunner.createZIO(
        cpuPool = cpuPool,
        handler = handler,
        tracingConfig = tracingConfig,
      )
  }
  make[TracingConfig].fromValue(TracingConfig.enabled)
  make[FailureHandler].fromValue(FailureHandler.Default)
  make[Runtime[Any]].from((_: ZIORunner).runtime)

  make[ThreadPoolExecutor].named("zio.cpu").fromResource {
    () =>
      val coresOr2 = java.lang.Runtime.getRuntime.availableProcessors() max 2
      DIResource.fromExecutorService(Executors.newFixedThreadPool(coresOr2).asInstanceOf[ThreadPoolExecutor])
  }
  make[ThreadPoolExecutor].named("zio.io").fromResource {
    () =>
      DIResource.fromExecutorService(Executors.newCachedThreadPool().asInstanceOf[ThreadPoolExecutor])
  }

  make[ExecutionContext].named("zio.cpu").from(ExecutionContext.fromExecutor(_: ThreadPoolExecutor @Id("zio.cpu")))
  make[ExecutionContext].named("zio.io").from(ExecutionContext.fromExecutor(_: ThreadPoolExecutor @Id("zio.io")))
}
