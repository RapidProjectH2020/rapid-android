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

package org.gradle.api.plugins

import org.gradle.api.Incubating
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.internal.project.ProjectInternal

/**
 * A {@link Plugin} which package a Java project as a distribution including the JAR and runtime dependencies.
 */
@Incubating
class JavaLibraryDistributionPlugin implements Plugin<ProjectInternal> {
    private Project project

    public void apply(ProjectInternal project) {
        this.project = project
        project.pluginManager.apply(JavaPlugin)
        project.pluginManager.apply(DistributionPlugin)
        def contents = project.distributions[DistributionPlugin.MAIN_DISTRIBUTION_NAME].contents
        def jar = project.tasks[JavaPlugin.JAR_TASK_NAME]
        contents.with {
            from(jar)
            from(project.file("src/dist"))
            into("lib") {
                from(project.configurations.runtime)
            }
        }
    }
}
