package izumi.distage.roles.bundled

import distage.TagK
import izumi.distage.model.definition.ModuleDef
import izumi.distage.roles.model.definition.RoleModuleDef
import izumi.fundamentals.platform.resources._

class BundledRolesModule[F[_]: TagK](version: String) extends ModuleDef with RoleModuleDef {
  makeRole[ConfigWriter[F]]
  makeRole[Help[F]]
  make[ArtifactVersion].named("launcher-version").fromValue(ArtifactVersion(version))

}

object BundledRolesModule {
  def apply[F[_]: TagK](version: String): BundledRolesModule[F] = {
    new BundledRolesModule(version)
  }
}
