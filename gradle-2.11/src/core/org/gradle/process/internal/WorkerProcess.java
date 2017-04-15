/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.process.internal;

import org.gradle.messaging.remote.ObjectConnection;
import org.gradle.process.ExecResult;

/**
 * A Java child process that performs some worker action. You can send and receive messages to/from the worker action
 * using a supplied {@link org.gradle.messaging.remote.ObjectConnection}.
 */
public interface WorkerProcess {
    void start();

    /**
     * The connection to the worker. Call {@link org.gradle.messaging.remote.ObjectConnection#connect()} to complete the connection.
     */
    ObjectConnection getConnection();

    ExecResult waitForStop();
}
