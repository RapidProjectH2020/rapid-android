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

package org.gradle.api.internal.artifacts;

import org.gradle.api.internal.artifacts.component.ComponentIdentifierFactory;
import org.gradle.api.internal.artifacts.component.DefaultComponentIdentifierFactory;
import org.gradle.api.internal.artifacts.ivyservice.DefaultIvyContextManager;
import org.gradle.api.internal.artifacts.ivyservice.IvyContextManager;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.*;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies.*;
import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.resource.transport.file.FileConnectorFactory;

class DependencyManagementGlobalScopeServices {
    IvyContextManager createIvyContextManager() {
        return new DefaultIvyContextManager();
    }

    ExcludeRuleConverter createExcludeRuleConverter() {
        return new DefaultExcludeRuleConverter();
    }

    ComponentIdentifierFactory createComponentIdentifierFactory() {
        return new DefaultComponentIdentifierFactory();
    }

    ExternalModuleIvyDependencyDescriptorFactory createExternalModuleDependencyDescriptorFactory(ExcludeRuleConverter excludeRuleConverter) {
        return new ExternalModuleIvyDependencyDescriptorFactory(excludeRuleConverter);
    }

    DependencyDescriptorFactory createDependencyDescriptorFactory(ExcludeRuleConverter excludeRuleConverter, ExternalModuleIvyDependencyDescriptorFactory descriptorFactory) {
        return new DefaultDependencyDescriptorFactory(
            new ProjectIvyDependencyDescriptorFactory(
                excludeRuleConverter),
            descriptorFactory);
    }

    DependenciesToModuleDescriptorConverter createDependenciesToModuleDescriptorConverter(DependencyDescriptorFactory dependencyDescriptorFactory,
                                                                                          ExcludeRuleConverter excludeRuleConverter) {
        return new DefaultDependenciesToModuleDescriptorConverter(dependencyDescriptorFactory, excludeRuleConverter);
    }

    ConfigurationComponentMetaDataBuilder createConfigurationComponentMetaDataBuilder(DependenciesToModuleDescriptorConverter dependenciesConverter) {
        return new DefaultConfigurationComponentMetaDataBuilder(dependenciesConverter);
    }

    ResourceConnectorFactory createFileConnectorFactory() {
        return new FileConnectorFactory();
    }
}
