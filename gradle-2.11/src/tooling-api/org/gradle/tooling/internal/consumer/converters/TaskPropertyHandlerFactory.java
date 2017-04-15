/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.tooling.internal.consumer.converters;

import org.gradle.api.Action;
import org.gradle.tooling.internal.adapter.SourceObjectMapping;
import org.gradle.tooling.internal.consumer.versioning.VersionDetails;
import org.gradle.tooling.model.GradleTask;

import java.io.Serializable;

public class TaskPropertyHandlerFactory {
    public Action<SourceObjectMapping> forVersion(VersionDetails versionDetails) {
        return new ConsumerMapping(versionDetails);
    }

    private static class ConsumerMapping implements Action<SourceObjectMapping>, Serializable {
        private final VersionDetails versionDetails;

        public ConsumerMapping(VersionDetails versionDetails) {
            this.versionDetails = versionDetails;
        }

        public void execute(SourceObjectMapping mapping) {
            if (GradleTask.class.isAssignableFrom(mapping.getTargetType()) && !versionDetails.supportsTaskDisplayName()) {
                mapping.mixIn(GradleTaskDisplayNameMixInHandler.class);
            }
        }
    }
}
