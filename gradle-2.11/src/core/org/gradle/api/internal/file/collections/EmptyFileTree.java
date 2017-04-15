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
package org.gradle.api.internal.file.collections;

import org.gradle.api.Buildable;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.file.FileSystemSubset;
import org.gradle.api.tasks.TaskDependency;

import java.util.Collection;
import java.util.Collections;

/**
 * An empty file collection which is used to mix in some build dependencies.
 */
public class EmptyFileTree implements MinimalFileTree, Buildable, LocalFileTree {
    private final TaskDependency buildDependencies;

    public EmptyFileTree(TaskDependency buildDependencies) {
        this.buildDependencies = buildDependencies;
    }

    public TaskDependency getBuildDependencies() {
        return buildDependencies;
    }

    public String getDisplayName() {
        return "dependencies mix-in file collection";
    }

    public Collection<DirectoryFileTree> getLocalContents() {
        return Collections.emptyList();
    }

    public void visit(FileVisitor visitor) {
    }

    @Override
    public void registerWatchPoints(FileSystemSubset.Builder builder) {
    }

    @Override
    public void visitTreeOrBackingFile(FileVisitor visitor) {
    }
}
