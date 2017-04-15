/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.plugins;

import groovy.lang.MissingPropertyException;
import org.gradle.api.internal.BeanDynamicObject;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import java.util.Map;

public class ExtraPropertiesDynamicObjectAdapter extends BeanDynamicObject {
    private final ExtraPropertiesExtension extension;
    private final Class<?> delegateType;

    public ExtraPropertiesDynamicObjectAdapter(Class<?> delegateType, ExtraPropertiesExtension extension) {
        super(extension);
        this.delegateType = delegateType;
        this.extension = extension;
    }

    public boolean hasProperty(String name) {
        return super.hasProperty(name) || extension.has(name);
    }

    public Map<String, ?> getProperties() {
        return extension.getProperties();
    }

    @Override
    public void setProperty(String name, Object value) throws MissingPropertyException {
        if (!hasProperty(name)) {
            throw new MissingPropertyException(name, delegateType);
        }

        super.setProperty(name, value);
    }

    @Override
    public boolean isMayImplementMissingMethods() {
        return false;
    }

    @Override
    public boolean isMayImplementMissingProperties() {
        return false;
    }
}
