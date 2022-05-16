/*
 * Copyright 2022, Leanplum, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.leanplum.actions

import com.leanplum.ActionArgs
import com.leanplum.callbacks.ActionCallback
import com.leanplum.internal.ActionManager
import com.leanplum.internal.VarCache

data class ActionDefinition(
  val name: String,
  val kind: Int,
  val args: ActionArgs,
  val options: Map<String, Any>?,
  var presentHandler: ActionCallback?,
  var dismissHandler: ActionCallback?
) {
  val definitionMap: MutableMap<String, Any?>

  init {
    val values: Map<String, Any> = HashMap()
    val kinds: Map<String, String> = HashMap()
    val order: MutableList<String> = ArrayList()
    for (arg in args.value) {
      VarCache.updateValues(
        arg.name(),
        VarCache.getNameComponents(arg.name()),
        arg.defaultValue(),
        arg.kind(),
        values,
        kinds
      )
      order.add(arg.name())
    }
    definitionMap = HashMap()
    definitionMap["kind"] = kind
    definitionMap["values"] = values
    definitionMap["kinds"] = kinds
    definitionMap["order"] = order
    definitionMap["options"] = options
  }
}

data class Definitions(
  val actionDefinitions: MutableList<ActionDefinition> = mutableListOf(),
  val handlers: MutableMap<String, MutableList<ActionCallback>> = mutableMapOf(),
  var devModeActionDefinitionsFromServer: Map<String, Any?>? = null
) {
  fun findDefinition(definitionName: String?): ActionDefinition? {
    return actionDefinitions.firstOrNull { it.name == definitionName }
  }
}

fun ActionManager.getActionDefinitionMap(actionName: String?): Map<String, Any?>? {
  val defMap = definitions.findDefinition(actionName)?.definitionMap
  return defMap
}

fun ActionManager.getActionDefinitionMaps(): Map<String, Any?> {
  val result: MutableMap<String, Map<String, Any?>> = mutableMapOf()
  definitions.actionDefinitions.forEach {
    result.put(it.name, it.definitionMap)
  }
  return result
}

fun ActionManager.defineAction(definition: ActionDefinition) {
  with (definitions.actionDefinitions) {
    firstOrNull { it.name == definition.name }?.also { remove(it) }
    add(definition)
  }
}

fun ActionManager.onAction(name: String, callback: ActionCallback) {
  with(definitions) {
    handlers[name]?.add(callback) ?: {
      val list = mutableListOf<ActionCallback>()
      list.add(callback)
      handlers[name] = list
    }
  }
}

fun ActionManager.setDevModeActionDefinitionsFromServer(serverDefs: Map<String, Any?>?) {
  definitions.devModeActionDefinitionsFromServer = serverDefs
}

fun ActionManager.areLocalAndServerDefinitionsEqual(): Boolean {
  val localDefinitions = getActionDefinitionMaps()
  val serverDefinitions = definitions.devModeActionDefinitionsFromServer
  return areActionDefinitionsEqual(localDefinitions, serverDefinitions)
}

private fun areActionDefinitionsEqual(a: Map<String, Any?>?, b: Map<String, Any?>?): Boolean {
  if (a == null || b == null || a.size != b.size) {
    return false
  }
  for ((key, value) in a) {
    if (value == null || b[key] == null) {
      return false
    }
    val aItem = value as Map<String, Any>
    val bItem = b[key] as Map<String, Any>

    val aKind = aItem["kind"]
    val aValues = aItem["values"]
    val aKinds = aItem["kinds"]
    val aOptions = aItem["options"]
    if ((aKind != null && aKind != bItem["kind"])
      || (aValues != null && aValues != bItem["values"])
      || (aKinds != null && aKinds != bItem["kinds"])
      || ((aOptions == null) != (bItem["options"] == null))
      || (aOptions != null && aOptions == bItem["options"])) {
      return false
    }
  }
  return true
}
