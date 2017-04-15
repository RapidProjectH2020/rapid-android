/*
 * Copyright 2011 the original author or authors.
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

import org.gradle.api.Nullable
import org.gradle.api.artifacts.ModuleVersionIdentifier

/**
 * Single entry module library
 */
class SingleEntryModuleLibrary extends ModuleLibrary {

    /**
     * Module version of the library, if any.
     */
    @Nullable
    ModuleVersionIdentifier moduleVersion

    /**
     * Creates single entry module library
     *
     * @param library a path to jar or class folder in idea format
     * @param javadoc paths to javadoc jars or javadoc folders
     * @param source paths to source jars or source folders
     * @param scope scope
     * @return
     */
    SingleEntryModuleLibrary(FilePath library, Set<FilePath> javadoc, Set<FilePath> source, String scope) {
        super([library] as Set, javadoc, source, [] as Set, scope)
    }

    /**
     * Creates single entry module library
     *
     * @param library a path to jar or class folder in idea format
     * @param javadoc path to javadoc jars or javadoc folders
     * @param source paths to source jars or source folders
     * @param scope scope
     * @return
     */
    SingleEntryModuleLibrary(FilePath library, @Nullable FilePath javadoc, @Nullable FilePath source, String scope) {
        super([library] as Set, javadoc ? [javadoc] : [], source ? [source] : [], [] as Set, scope)
    }

    /**
     * Creates single entry module library
     *
     * @param library a path to jar or class folder in Path format
     * @param scope scope
     * @return
     */
    SingleEntryModuleLibrary(FilePath library, String scope) {
        this(library, [] as Set, [] as Set, scope)
    }

    /**
     * Returns a single jar or class folder
     */
    File getLibraryFile() {
        this.classes.iterator().next().file
    }

    /**
     * Returns a single javadoc jar or javadoc folder
     */
    File getJavadocFile() {
        if (javadoc.size() > 0) {
            return this.javadoc.iterator().next().file
        } else {
            return null
        }
    }

    /**
     * Returns a single source jar or source folder
     */
    File getSourceFile() {
        if (sources.size() > 0) {
            return this.sources.iterator().next().file
        } else {
            return null
        }
    }
}
