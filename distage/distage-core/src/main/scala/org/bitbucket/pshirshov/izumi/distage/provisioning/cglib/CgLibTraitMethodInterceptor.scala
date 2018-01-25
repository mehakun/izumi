package org.bitbucket.pshirshov.izumi.distage.provisioning.cglib

import java.lang.reflect.Method

import net.sf.cglib.proxy.{MethodInterceptor, MethodProxy}
import org.bitbucket.pshirshov.izumi.distage.model.exceptions.MissingRefException
import org.bitbucket.pshirshov.izumi.distage.provisioning.ProvisioningContext
import org.bitbucket.pshirshov.izumi.distage.provisioning.strategies.TraitIndex

import scala.collection.mutable

protected[distage] class CgLibTraitMethodInterceptor
(
  index: TraitIndex
  , context: ProvisioningContext
) extends MethodInterceptor {

  private val fields = mutable.HashMap[String, Any]()

  override def intercept(o: scala.Any, method: Method, objects: Array[AnyRef], methodProxy: MethodProxy): AnyRef = {
    //premature optimization, all our methods are parameterless
    if (method.getParameterTypes.length == 0 && index.methods.contains(method)) {
      val wireWith = index.methods(method).wireWith

      context.fetchKey(wireWith) match {
        case Some(v) =>
          v.asInstanceOf[AnyRef]

        case None =>
          throw new MissingRefException(s"Cannot return $wireWith from ${method.getName}, it's not available in the context o_O", Set(wireWith), None)
      }

    } else if (index.getters.contains(method.getName)) {
      fields.synchronized {
        val field = index.getters(method.getName).name
        fields.getOrElse(field, throw new NullPointerException(s"Field $field was not initialized for $o"))
      }.asInstanceOf[AnyRef]
    } else if (index.setters.contains(method.getName)) {
      fields.synchronized {
        fields.put(index.setters(method.getName).name, objects.head)
      }.asInstanceOf[AnyRef]
    } else {
      CglibTools.invokeExistingMethod(o, method, objects)
    }
  }

}
