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
package org.gradle.language.nativeplatform.internal.incremental.sourceparser;

import org.gradle.api.specs.Spec;
import org.gradle.language.nativeplatform.internal.Include;
import org.gradle.language.nativeplatform.internal.IncludeType;
import org.gradle.language.nativeplatform.internal.SourceIncludes;
import org.gradle.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DefaultSourceIncludes implements SourceIncludes, Serializable {
    private final List<Include> allIncludes = new ArrayList<Include>();

    public void addAll(List<Include> includes) {
        this.allIncludes.addAll(includes);
    }

    public List<Include> getQuotedIncludes() {
        return CollectionUtils.filter(allIncludes, new Spec<Include>() {
            @Override
            public boolean isSatisfiedBy(Include element) {
                return element.getType() == IncludeType.QUOTED;
            }
        });
    }

    public List<Include> getSystemIncludes() {
        return CollectionUtils.filter(allIncludes, new Spec<Include>() {
            @Override
            public boolean isSatisfiedBy(Include element) {
                return element.getType() == IncludeType.SYSTEM;
            }
        });
    }

    public List<Include> getMacroIncludes() {
        return CollectionUtils.filter(allIncludes, new Spec<Include>() {
            @Override
            public boolean isSatisfiedBy(Include element) {
                return element.getType() == IncludeType.MACRO;
            }
        });
    }

    public List<Include> getIncludesAndImports() {
        return allIncludes;
    }

    public List<Include> getIncludesOnly() {
        return CollectionUtils.filter(allIncludes, new Spec<Include>() {
            @Override
            public boolean isSatisfiedBy(Include element) {
                return !element.isImport();
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultSourceIncludes that = (DefaultSourceIncludes) o;

        if (!allIncludes.equals(that.allIncludes)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return allIncludes.hashCode();
    }
}
