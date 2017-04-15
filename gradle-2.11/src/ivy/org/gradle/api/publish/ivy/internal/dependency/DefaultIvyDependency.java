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

package org.gradle.api.publish.ivy.internal.dependency;

import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExcludeRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultIvyDependency implements IvyDependencyInternal {
    private final String organisation;
    private final String module;
    private final String revision;
    private final String confMapping;
    private final List<DependencyArtifact> artifacts = new ArrayList<DependencyArtifact>();
    private final List<ExcludeRule> excludeRules = new ArrayList<ExcludeRule>();

    public DefaultIvyDependency(String organisation, String module, String revision, String confMapping) {
        this.organisation = organisation;
        this.module = module;
        this.revision = revision;
        this.confMapping = confMapping;
    }

    public DefaultIvyDependency(String organisation, String module, String revision, String confMapping, Collection<DependencyArtifact> artifacts) {
        this(organisation, module, revision, confMapping);
        this.artifacts.addAll(artifacts);
    }

    public DefaultIvyDependency(String organisation, String module, String revision, String confMapping, Collection<DependencyArtifact> artifacts, Collection<ExcludeRule> excludeRules) {
        this(organisation, module, revision, confMapping, artifacts);
        this.excludeRules.addAll(excludeRules);
    }

    public String getOrganisation() {
        return organisation;
    }

    public String getModule() {
        return module;
    }

    public String getRevision() {
        return revision;
    }

    public String getConfMapping() {
        return confMapping;
    }

    public Iterable<DependencyArtifact> getArtifacts() {
        return artifacts;
    }

    public Iterable<ExcludeRule> getExcludeRules() {
        return excludeRules;
    }
}
