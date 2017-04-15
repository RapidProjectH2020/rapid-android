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

package org.gradle.internal.component.model;

import com.google.common.collect.Sets;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactMetaData;
import org.gradle.internal.component.external.model.ModuleComponentArtifactMetaData;
import org.gradle.util.CollectionUtils;

import java.util.*;

public abstract class AbstractModuleDescriptorBackedMetaData implements ComponentResolveMetaData {

    private ModuleVersionIdentifier moduleVersionIdentifier;
    private final ModuleDescriptor moduleDescriptor;
    private ComponentIdentifier componentIdentifier;
    private ModuleSource moduleSource;
    private boolean changing;
    private String status;
    private List<String> statusScheme = DEFAULT_STATUS_SCHEME;
    private List<DependencyMetaData> dependencies;
    private Map<String, DefaultConfigurationMetaData> configurations = new HashMap<String, DefaultConfigurationMetaData>();

    public AbstractModuleDescriptorBackedMetaData(ModuleVersionIdentifier moduleVersionIdentifier, ModuleDescriptor moduleDescriptor, ComponentIdentifier componentIdentifier) {
        this.moduleVersionIdentifier = moduleVersionIdentifier;
        this.moduleDescriptor = moduleDescriptor;
        this.componentIdentifier = componentIdentifier;
        status = moduleDescriptor.getStatus();
    }

    protected void copyTo(AbstractModuleDescriptorBackedMetaData copy) {
        copy.dependencies = dependencies;
        copy.changing = changing;
        copy.status = status;
        copy.statusScheme = statusScheme;
        copy.moduleSource = moduleSource;
    }

    @Override
    public String toString() {
        return componentIdentifier.getDisplayName();
    }

    public ModuleVersionIdentifier getId() {
        return moduleVersionIdentifier;
    }

    public void setId(ModuleVersionIdentifier moduleVersionIdentifier) {
        this.moduleVersionIdentifier = moduleVersionIdentifier;
    }

    public ModuleSource getSource() {
        return moduleSource;
    }

    public void setSource(ModuleSource source) {
        this.moduleSource = source;
    }

    public void setModuleSource(ModuleSource moduleSource) {
        this.moduleSource = moduleSource;
    }

    public ModuleDescriptor getDescriptor() {
        return moduleDescriptor;
    }

    public boolean isGenerated() {
        return moduleDescriptor.isDefault();
    }

    public boolean isChanging() {
        return changing;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getStatusScheme() {
        return statusScheme;
    }

    public ComponentIdentifier getComponentId() {
        return componentIdentifier;
    }

    public void setComponentId(ComponentIdentifier componentIdentifier) {
        this.componentIdentifier = componentIdentifier;
    }

    public void setChanging(boolean changing) {
        this.changing = changing;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatusScheme(List<String> statusScheme) {
        this.statusScheme = statusScheme;
    }

    public List<DependencyMetaData> getDependencies() {
        if (dependencies == null) {
            dependencies = populateDependenciesFromDescriptor();
        }
        return dependencies;
    }

     protected List<DependencyMetaData> populateDependenciesFromDescriptor() {
         List<DependencyMetaData> dependencies = new ArrayList<DependencyMetaData>();
         for (final DependencyDescriptor dependencyDescriptor : moduleDescriptor.getDependencies()) {
             dependencies.add(new DefaultDependencyMetaData(dependencyDescriptor));
         }
         return dependencies;
    }

    public void setDependencies(Iterable<? extends DependencyMetaData> dependencies) {
        this.dependencies = CollectionUtils.toList(dependencies);
        for (DefaultConfigurationMetaData configuration : configurations.values()) {
            configuration.dependencies = null;
        }
    }

    @Override
    public Set<String> getConfigurationNames() {
        return Sets.newHashSet(moduleDescriptor.getConfigurationsNames());
    }

    public DefaultConfigurationMetaData getConfiguration(final String name) {
        DefaultConfigurationMetaData configuration = configurations.get(name);
        if (configuration == null) {
            configuration = populateConfigurationFromDescriptor(name);
        }
        return configuration;
    }

    private DefaultConfigurationMetaData populateConfigurationFromDescriptor(String name) {
        Configuration descriptorConfiguration = moduleDescriptor.getConfiguration(name);
        if (descriptorConfiguration == null) {
            return null;
        }
        Set<String> hierarchy = new LinkedHashSet<String>();
        hierarchy.add(name);
        for (String parent : descriptorConfiguration.getExtends()) {
            hierarchy.addAll(getConfiguration(parent).hierarchy);
        }
        DefaultConfigurationMetaData configuration = new DefaultConfigurationMetaData(name, descriptorConfiguration, hierarchy);
        configurations.put(name, configuration);
        return configuration;
    }

    protected abstract Set<ComponentArtifactMetaData> getArtifactsForConfiguration(ConfigurationMetaData configuration);

    private class DefaultConfigurationMetaData implements ConfigurationMetaData {
        private final String name;
        private final Configuration descriptor;
        private final Set<String> hierarchy;
        private List<DependencyMetaData> dependencies;
        private Set<ComponentArtifactMetaData> artifacts;
        private LinkedHashSet<ExcludeRule> excludeRules;

        private DefaultConfigurationMetaData(String name, Configuration descriptor, Set<String> hierarchy) {
            this.name = name;
            this.descriptor = descriptor;
            this.hierarchy = hierarchy;
        }

        @Override
        public String toString() {
            return String.format("%s:%s", componentIdentifier.getDisplayName(), name);
        }

        public ComponentResolveMetaData getComponent() {
            return AbstractModuleDescriptorBackedMetaData.this;
        }

        public String getName() {
            return name;
        }

        public Set<String> getHierarchy() {
            return hierarchy;
        }

        public boolean isTransitive() {
            return descriptor.isTransitive();
        }

        public boolean isVisible() {
            return descriptor.getVisibility() == Configuration.Visibility.PUBLIC;
        }

        public List<DependencyMetaData> getDependencies() {
            if (dependencies == null) {
                dependencies = new ArrayList<DependencyMetaData>();
                for (DependencyMetaData dependency : AbstractModuleDescriptorBackedMetaData.this.getDependencies()) {
                    if (include(dependency)) {
                        dependencies.add(dependency);
                    }
                }
            }
            return dependencies;
        }

        private boolean include(DependencyMetaData dependency) {
            String[] moduleConfigurations = dependency.getModuleConfigurations();
            for (int i = 0; i < moduleConfigurations.length; i++) {
                String moduleConfiguration = moduleConfigurations[i];
                if (moduleConfiguration.equals("%") || hierarchy.contains(moduleConfiguration)) {
                    return true;
                }
                if (moduleConfiguration.equals("*")) {
                    boolean include = true;
                    for (int j = i + 1; j < moduleConfigurations.length && moduleConfigurations[j].startsWith("!"); j++) {
                        if (moduleConfigurations[j].substring(1).equals(getName())) {
                            include = false;
                            break;
                        }
                    }
                    if (include) {
                        return true;
                    }
                }
            }
            return false;
        }

        public Set<ExcludeRule> getExcludeRules() {
            if (excludeRules == null) {
                populateExcludeRulesFromDescriptor();
            }
            return excludeRules;
        }

        private void populateExcludeRulesFromDescriptor() {
            excludeRules = new LinkedHashSet<ExcludeRule>();
            for (ExcludeRule excludeRule : moduleDescriptor.getAllExcludeRules()) {
                for (String config : excludeRule.getConfigurations()) {
                    if (hierarchy.contains(config)) {
                        excludeRules.add(excludeRule);
                        break;
                    }
                }
            }
        }

        public Set<ComponentArtifactMetaData> getArtifacts() {
            if (artifacts == null) {
                artifacts = getArtifactsForConfiguration(this);
            }
            return artifacts;
        }

        public ModuleComponentArtifactMetaData artifact(IvyArtifactName artifact) {
            return new DefaultModuleComponentArtifactMetaData((org.gradle.api.artifacts.component.ModuleComponentIdentifier) getComponentId(), artifact);
        }
    }
}
