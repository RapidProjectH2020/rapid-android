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

package org.gradle.plugins.ide.idea.model

import org.gradle.api.JavaVersion

/**
 * Java language level used by IDEA projects.
 */
class IdeaLanguageLevel {

    String level

    IdeaLanguageLevel(Object version) {
        if (version instanceof String && version =~ /^JDK_/) {
            level = version
            return
        }
        level = JavaVersion.toVersion(version).name().replaceFirst("VERSION", "JDK")
    }

    boolean equals(o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
            return false
        }
        IdeaLanguageLevel that = (IdeaLanguageLevel) o
        if (level != that.level) {
            return false
        }
        return true
    }

    int hashCode() {
        return (level != null ? level.hashCode() : 0)
    }
}
