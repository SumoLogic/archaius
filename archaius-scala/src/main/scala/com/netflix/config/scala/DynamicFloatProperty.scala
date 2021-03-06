/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.config.scala

import com.netflix.config.DynamicPropertyFactory
import java.lang.{Float => jFloat}

/**
 * User: gorzell
 * Date: 8/10/12
 */
object DynamicFloatProperty {
  def apply(propertyName: String, defaultValue: Float) =
    new DynamicFloatProperty(propertyName, defaultValue)

  def apply(propertyName: String, defaultValue: Float, callback: Runnable) = {
    val p = new DynamicFloatProperty(propertyName, defaultValue)
    p.addCallback(callback)
    p
  }
}

class DynamicFloatProperty(
  override val propertyName: String,
  override val defaultValue: Float)
extends DynamicProperty[Float]
{
  override protected val box = new PropertyBox[Float, jFloat] {
    override val prop = DynamicPropertyFactory.getInstance().getFloatProperty(propertyName, defaultValue)
    def convert(jt: jFloat): Float = jt
  }
}
