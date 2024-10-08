/**
 * Copyright (c) 2019 Cotta & Cush Limited.
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
package com.cottacush.android.hiddencam

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * A [LifecycleOwner] to manage the Lifecycle of the camera engine.
 */
internal class HiddenCamLifeCycleOwner : LifecycleOwner {

    override val lifecycle = LifecycleRegistry(this)

    init {
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    fun start() {
        lifecycle.currentState = Lifecycle.State.STARTED
    }

    fun stop() {
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    fun tearDown() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
    }
}
