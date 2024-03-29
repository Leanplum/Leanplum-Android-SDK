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

/**
 * Use static factory methods [MessageDisplayChoice.show], [MessageDisplayChoice.discard], and
 * [MessageDisplayChoice.delay] as a result to [MessageDisplayController.shouldDisplayMessage].
 */
data class MessageDisplayChoice private constructor(val type: Type, val delaySeconds: Int = 0) {
  enum class Type {
    SHOW,
    DISCARD,
    DELAY
  }

  companion object {
    @JvmStatic
    fun show() = MessageDisplayChoice(Type.SHOW)

    @JvmStatic
    fun discard() = MessageDisplayChoice(Type.DISCARD)

    @JvmStatic
    fun delay(delaySeconds: Int) = MessageDisplayChoice(Type.DELAY, delaySeconds)

    /**
     * When actions are delayed indefinitely use [LeanplumActions.triggerDelayedMessages] to trigger
     * them again.
     */
    @JvmStatic
    fun delayIndefinitely() = delay(0)
  }
}
