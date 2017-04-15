/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.result;

import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.result.ComponentSelectionReason;
import org.gradle.internal.resolve.ModuleVersionResolveException;

public class DefaultInternalDependencyResult implements InternalDependencyResult {

    private final ComponentSelector requested;
    private final ModuleVersionIdentifier selected;
    private final ComponentSelectionReason reason;
    private ModuleVersionResolveException failure;

    public DefaultInternalDependencyResult(ComponentSelector requested,
                                           ModuleVersionIdentifier selected,
                                           ComponentSelectionReason reason,
                                           ModuleVersionResolveException failure) {
        assert requested != null;
        assert failure != null || selected != null;

        this.requested = requested;
        this.reason = reason;
        this.selected = selected;
        this.failure = failure;
    }

    public ComponentSelector getRequested() {
        return requested;
    }

    public ModuleVersionIdentifier getSelected() {
        return selected;
    }

    public ComponentSelectionReason getReason() {
        return reason;
    }

    public ModuleVersionResolveException getFailure() {
        return failure;
    }
}
